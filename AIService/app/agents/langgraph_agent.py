import logging
from typing import Dict, Any, Optional, List, TypedDict, Annotated
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, SystemMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.runnables import RunnableConfig
from langchain_openai import ChatOpenAI
from langchain.memory import ConversationBufferWindowMemory
from langchain.agents import create_openai_functions_agent, AgentExecutor
from langchain.callbacks.base import BaseCallbackHandler
import json
from datetime import datetime

from .tools import init_tools, search_doctor_info, check_available_slots, recommend_medical_packages, create_booking, get_clinic_info, get_doctor_schedule, find_earliest_available_slot, list_all_available_slots
from ..services.clinic_api import ClinicAPIService
from ..rag.pgvector_store import PGVectorStore
from ..models.prompts import build_dynamic_system_prompt
from ..config.settings import settings

logger = logging.getLogger(__name__)


class ClinicAgentCallbackHandler(BaseCallbackHandler):
    """Custom callback handler for logging agent actions"""

    def on_tool_start(self, serialized: Dict[str, Any], input_str: str, **kwargs: Any) -> None:
        logger.info(f"Tool started: {serialized.get('name', 'Unknown')} with input: {input_str}")

    def on_tool_end(self, output: str, **kwargs: Any) -> None:
        logger.info(f"Tool completed with output length: {len(output)}")

    def on_agent_action(self, action, **kwargs):
        logger.info(f"Agent action: {action.tool} with input: {action.tool_input}")


class LangGraphAgent:
    """AI Agent using LangChain AgentExecutor with session-based memory"""

    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.openai_model,
            temperature=settings.openai_temperature,
            openai_api_key=settings.openai_api_key,
            max_tokens=1000
        )

        self.tools = []
        self.system_prompt = None
        self.callback_handler = ClinicAgentCallbackHandler()

        # Session-specific memories
        self.session_memories: Dict[str, ConversationBufferWindowMemory] = {}

        logger.info("LangGraphAgent initialized")

    async def initialize(self, clinic_api: ClinicAPIService, vector_store: PGVectorStore):
        """Initialize agent with services and tools"""
        # Initialize global tool instances
        init_tools(clinic_api, vector_store)

        # Import tools after initialization
        self.tools = [
            search_doctor_info,
            check_available_slots,
            recommend_medical_packages,
            create_booking,
            get_clinic_info,
            get_doctor_schedule,
            find_earliest_available_slot,
            list_all_available_slots
        ]

        # Build system prompt
        self.system_prompt = await build_dynamic_system_prompt(clinic_api)

        logger.info("LangGraphAgent initialized with tools and system prompt")

    def _get_or_create_memory(self, session_id: str) -> ConversationBufferWindowMemory:
        """Get or create memory for a specific session"""
        if session_id not in self.session_memories:
            self.session_memories[session_id] = ConversationBufferWindowMemory(
                memory_key="chat_history",
                return_messages=True,
                max_token_limit=settings.memory_max_tokens,
                k=settings.memory_window_size
            )
            logger.info(f"Created new memory for session {session_id}")
        return self.session_memories[session_id]

    def _create_agent_executor(self, memory: ConversationBufferWindowMemory):
        """Create agent executor with given memory"""
        # Create agent prompt with dynamic content
        prompt = ChatPromptTemplate.from_messages([
            ("system", self.system_prompt),
            MessagesPlaceholder(variable_name="chat_history", optional=True),
            ("human", "{input}"),
            MessagesPlaceholder(variable_name="agent_scratchpad"),
        ])

        # Create agent
        agent = create_openai_functions_agent(
            llm=self.llm,
            tools=self.tools,
            prompt=prompt
        )

        # Create agent executor with session-specific memory
        agent_executor = AgentExecutor(
            agent=agent,
            tools=self.tools,
            memory=memory,
            verbose=True,
            max_iterations=5,
            early_stopping_method="generate",
            callbacks=[self.callback_handler],
            handle_parsing_errors=True
        )

        return agent_executor


    async def run(self, user_input: str, session_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Run agent with user input

        Args:
            user_input: User's message
            session_id: Session identifier for conversation tracking

        Returns:
            Dict containing response and metadata
        """
        if not self.system_prompt:
            raise ValueError("Agent not initialized. Call initialize() first.")

        if not session_id:
            session_id = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        try:
            # Get or create memory for this session
            memory = self._get_or_create_memory(session_id)

            # Create agent executor with this session's memory
            agent_executor = self._create_agent_executor(memory)

            # Run agent
            result = await agent_executor.ainvoke({"input": user_input})

            # Extract the final answer from the result
            response = result.get("output", "")

            # Extract suggested actions from response
            suggested_actions = self._extract_suggested_actions(response)

            result = {
                "response": response,
                "suggested_actions": suggested_actions,
                "session_id": session_id,
                "timestamp": datetime.now().isoformat(),
                "tool_calls": 0  # Would need to track this differently
            }

            logger.info(f"Agent response generated for session {session_id}")
            return result

        except Exception as e:
            logger.error(f"Error running agent: {e}")
            return {
                "response": "Xin lỗi, có lỗi xảy ra khi xử lý yêu cầu của bạn. Vui lòng thử lại hoặc liên hệ hotline để được hỗ trợ.",
                "suggested_actions": ["contact_support"],
                "session_id": session_id,
                "error": str(e),
                "timestamp": datetime.now().isoformat()
            }

    def _extract_suggested_actions(self, response: str) -> List[str]:
        """Extract suggested actions from agent response"""
        actions = []

        # Basic action extraction based on keywords
        response_lower = response.lower()

        if any(word in response_lower for word in ["đặt lịch", "booking", "lịch hẹn"]):
            actions.append("book_appointment")

        if any(word in response_lower for word in ["gói khám", "package", "tư vấn"]):
            actions.append("view_packages")

        if any(word in response_lower for word in ["bác sĩ", "doctor", "chuyên khoa"]):
            actions.append("view_doctors")

        if any(word in response_lower for word in ["slot", "trống", "available", "lịch"]):
            actions.append("check_schedule")

        if any(word in response_lower for word in ["liên hệ", "hotline", "hỗ trợ"]):
            actions.append("contact_support")

        # Default action if no specific actions found
        if not actions:
            actions.append("general_help")

        return actions

    def clear_memory(self, session_id: Optional[str] = None):
        """Clear conversation memory"""
        if session_id and session_id in self.session_memories:
            # Clear specific session memory
            self.session_memories[session_id].clear()
            del self.session_memories[session_id]
            logger.info(f"Memory cleared for session {session_id}")
        elif not session_id:
            # Clear all memories
            self.session_memories.clear()
            logger.info("All memories cleared")

    async def get_conversation_history(self, session_id: Optional[str] = None) -> List[Dict[str, Any]]:
        """Get conversation history"""
        try:
            if not session_id or session_id not in self.session_memories:
                return []

            memory = self.session_memories[session_id]
            memory_vars = memory.load_memory_variables({})

            # Convert to serializable format
            history = []
            for message in memory_vars.get("chat_history", []):
                history.append({
                    "type": message.__class__.__name__,
                    "content": message.content,
                    "timestamp": datetime.now().isoformat()
                })

            return history

        except Exception as e:
            logger.error(f"Error getting conversation history: {e}")
            return []


class AgentManager:
    """Manager for handling multiple agent instances"""

    def __init__(self):
        self.agents: Dict[str, LangGraphAgent] = {}
        self.default_agent = LangGraphAgent()

    async def initialize_default_agent(self, clinic_api: ClinicAPIService, vector_store: PGVectorStore):
        """Initialize the default agent"""
        await self.default_agent.initialize(clinic_api, vector_store)
        logger.info("Default LangGraph agent initialized")

    def get_agent(self, session_id: Optional[str] = None) -> LangGraphAgent:
        """Get agent for session (currently returns default agent)"""
        # For now, return default agent
        # In future, can implement session-specific agents
        return self.default_agent

    async def run_agent(self, user_input: str, session_id: Optional[str] = None) -> Dict[str, Any]:
        """Run agent for given session"""
        agent = self.get_agent(session_id)
        return await agent.run(user_input, session_id)

    def cleanup_session(self, session_id: str):
        """Cleanup session data"""
        if session_id in self.agents:
            del self.agents[session_id]
            logger.info(f"Session {session_id} cleaned up")
