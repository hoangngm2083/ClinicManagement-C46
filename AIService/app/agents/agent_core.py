from langchain.agents import create_openai_functions_agent, AgentExecutor
from langchain_openai import ChatOpenAI
from langchain.memory import ConversationBufferWindowMemory
from langchain.callbacks.base import BaseCallbackHandler
import logging
from typing import List, Dict, Any, Optional
from .tools import init_tools
from ..services.clinic_api import ClinicAPIService
from ..rag.vector_store import VectorStore
from ..models.prompts import create_agent_prompt
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


class ClinicAgent:
    """Main AI Agent for Clinic Management"""

    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.openai_model,
            temperature=settings.openai_temperature,
            openai_api_key=settings.openai_api_key,
            max_tokens=1000
        )

        self.memory = ConversationBufferWindowMemory(
            memory_key="chat_history",
            return_messages=True,
            max_token_limit=settings.memory_max_tokens,
            k=settings.memory_window_size
        )

        self.tools = []
        self.agent_executor = None
        self.callback_handler = ClinicAgentCallbackHandler()

        logger.info("ClinicAgent initialized")

    async def initialize(self, clinic_api: ClinicAPIService, vector_store: VectorStore):
        """Initialize agent with services and tools"""
        # Initialize global tool instances
        init_tools(clinic_api, vector_store)

        # Import tools after initialization
        from .tools import (
            search_doctor_info,
            check_available_slots,
            recommend_medical_packages,
            create_booking,
            get_clinic_info,
            get_doctor_schedule,
            find_earliest_available_slot
        )

        self.tools = [
            search_doctor_info,
            check_available_slots,
            recommend_medical_packages,
            create_booking,
            get_clinic_info,
            get_doctor_schedule,
            find_earliest_available_slot
        ]

        # Create agent prompt with dynamic content from database
        from ..models.prompts import create_agent_prompt
        prompt = await create_agent_prompt(clinic_api)

        # Create agent
        agent = create_openai_functions_agent(
            llm=self.llm,
            tools=self.tools,
            prompt=prompt
        )

        # Create agent executor
        self.agent_executor = AgentExecutor(
            agent=agent,
            tools=self.tools,
            memory=self.memory,
            verbose=True,
            max_iterations=5,
            early_stopping_method="generate",
            callbacks=[self.callback_handler],
            handle_parsing_errors=True
        )

        logger.info("ClinicAgent fully initialized with tools and executor")

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

        try:
            # Run agent
            response = await self.agent_executor.arun(input=user_input)

            # Extract suggested actions from response (basic implementation)
            suggested_actions = self._extract_suggested_actions(response)

            result = {
                "response": response,
                "suggested_actions": suggested_actions,
                "session_id": session_id,
                "timestamp": self._get_timestamp()
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
                "timestamp": self._get_timestamp()
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

    def _get_timestamp(self) -> str:
        """Get current timestamp"""
        from datetime import datetime
        return datetime.now().isoformat()

    def clear_memory(self, session_id: Optional[str] = None):
        """Clear conversation memory"""
        self.memory.clear()
        logger.info(f"Memory cleared for session {session_id}")

    def get_memory_variables(self) -> Dict[str, Any]:
        """Get current memory variables"""
        return self.memory.load_memory_variables({})

    async def get_conversation_history(self, session_id: Optional[str] = None) -> List[Dict[str, Any]]:
        """Get conversation history"""
        try:
            memory_vars = self.memory.load_memory_variables({})
            history = memory_vars.get("chat_history", [])

            # Convert to serializable format
            serializable_history = []
            for message in history:
                serializable_history.append({
                    "type": message.__class__.__name__,
                    "content": message.content,
                    "timestamp": self._get_timestamp()
                })

            return serializable_history

        except Exception as e:
            logger.error(f"Error getting conversation history: {e}")
            return []


class AgentManager:
    """Manager for handling multiple agent instances"""

    def __init__(self):
        self.agents: Dict[str, ClinicAgent] = {}
        self.default_agent = ClinicAgent()

    async def initialize_default_agent(self, clinic_api: ClinicAPIService, vector_store: VectorStore):
        """Initialize the default agent"""
        await self.default_agent.initialize(clinic_api, vector_store)
        logger.info("Default agent initialized")

    def get_agent(self, session_id: Optional[str] = None) -> ClinicAgent:
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
