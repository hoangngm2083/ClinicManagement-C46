import logging
from typing import Dict, Any, Optional, List, TypedDict, Annotated
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.runnables import RunnableConfig
from langchain_openai import ChatOpenAI
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver
from langgraph.prebuilt import ToolExecutor, create_agent_executor
import json
from datetime import datetime

from .tools import init_tools, search_doctor_info, check_available_slots, recommend_medical_packages, create_booking, get_clinic_info, get_doctor_schedule
from ..services.clinic_api import ClinicAPIService
from ..rag.pgvector_store import PGVectorStore
from ..models.prompts import build_dynamic_system_prompt
from ..config.settings import settings

logger = logging.getLogger(__name__)


# Using built-in LangGraph state management


class LangGraphAgent:
    """AI Agent using LangGraph with memory"""

    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.openai_model,
            temperature=settings.openai_temperature,
            openai_api_key=settings.openai_api_key,
            max_tokens=1000
        )

        self.tools = []
        self.agent_executor = None
        self.memory = MemorySaver()
        self.system_prompt = None

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
            get_doctor_schedule
        ]

        # Build system prompt
        self.system_prompt = await build_dynamic_system_prompt(clinic_api)

        # Create prompt template
        from langchain.agents import create_openai_functions_agent
        prompt = ChatPromptTemplate.from_messages([
            ("system", self.system_prompt),
            MessagesPlaceholder(variable_name="chat_history", optional=True),
            ("human", "{input}"),
            MessagesPlaceholder(variable_name="agent_scratchpad"),
        ])

        # Create LangChain agent first
        agent = create_openai_functions_agent(self.llm, self.tools, prompt)

        # Create LangGraph executor
        self.agent_executor = create_agent_executor(agent, self.tools)

        logger.info("LangGraphAgent fully initialized with tools and executor")

    # Using built-in LangGraph agent executor, no custom methods needed

    async def run(self, user_input: str, session_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Run agent with user input

        Args:
            user_input: User's message
            session_id: Session identifier for conversation tracking

        Returns:
            Dict containing response and metadata
        """
        if not self.agent_executor:
            raise ValueError("Agent not initialized. Call initialize() first.")

        if not session_id:
            session_id = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        try:
            # Configure thread for memory
            config = RunnableConfig(
                configurable={"thread_id": session_id},
                recursion_limit=settings.langgraph_recursion_limit
            )

            # Run the agent executor
            response = await self.agent_executor.ainvoke(
                {"input": user_input, "chat_history": []},
                config
            )

            # Extract final answer
            final_answer = response.get("output", "")

            # Extract suggested actions from response
            suggested_actions = self._extract_suggested_actions(final_answer)

            result = {
                "response": final_answer,
                "suggested_actions": suggested_actions,
                "session_id": session_id,
                "timestamp": datetime.now().isoformat(),
                "tool_calls": len(response.get("intermediate_steps", []))
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
        if session_id and self.memory:
            # Clear specific thread memory
            self.memory.storage.pop(session_id, None)
        logger.info(f"Memory cleared for session {session_id}")

    async def get_conversation_history(self, session_id: Optional[str] = None) -> List[Dict[str, Any]]:
        """Get conversation history"""
        try:
            if not session_id or not self.memory:
                return []

            # Get thread history from memory
            config = RunnableConfig(configurable={"thread_id": session_id})
            history = []

            # Get stored messages (this is a simplified approach)
            # In practice, you'd need to access the memory storage directly
            if hasattr(self.memory, 'storage') and session_id in self.memory.storage:
                thread_data = self.memory.storage[session_id]
                if 'channel_values' in thread_data and 'messages' in thread_data['channel_values']:
                    messages = thread_data['channel_values']['messages']
                    for message in messages:
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
