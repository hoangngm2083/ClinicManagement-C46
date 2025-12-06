import logging
from typing import Dict, Any, Optional, List, TypedDict, Annotated
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage, SystemMessage
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnableConfig
from langchain_openai import ChatOpenAI
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode
from langgraph.checkpoint.memory import MemorySaver
import json
from datetime import datetime

from .tools import init_tools, search_doctor_info, check_available_slots, recommend_medical_packages, list_medical_packages, create_booking, get_clinic_info, get_doctor_schedule, find_earliest_available_slot, list_all_available_slots, get_department_info
from ..services.clinic_api import ClinicAPIService
from ..rag.pgvector_store import PGVectorStore
from ..models.prompts import build_dynamic_system_prompt
from ..config.settings import settings

logger = logging.getLogger(__name__)


# Define the state for our graph
class AgentState(TypedDict):
    messages: Annotated[List[BaseMessage], add_messages]


class LangGraphAgent:
    """AI Agent using LangGraph with proper state management"""

    def __init__(self):
        self.llm = ChatOpenAI(
            model=settings.openai_model,
            temperature=settings.openai_temperature,
            openai_api_key=settings.openai_api_key,
            max_tokens=1000
        )

        self.tools = []
        self.system_prompt = None
        self.graph = None
        self.memory = MemorySaver()

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
            list_medical_packages,
            create_booking,
            get_clinic_info,
            get_doctor_schedule,
            find_earliest_available_slot,
            list_all_available_slots,
            get_department_info
        ]

        # Build system prompt
        self.system_prompt = await build_dynamic_system_prompt(clinic_api)

        # Create the LangGraph
        self.graph = self._create_graph()

        logger.info("LangGraphAgent initialized with tools and graph")

    def _create_graph(self) -> StateGraph:
        """Create the LangGraph with proper state management"""
        # Create prompt template
        prompt = ChatPromptTemplate.from_messages([
            ("system", self.system_prompt),
            ("placeholder", "{messages}"),
        ])

        # Bind tools to LLM
        llm_with_tools = self.llm.bind_tools(self.tools)

        def agent_node(state: AgentState) -> Dict[str, Any]:
            """Agent node that processes messages and decides on actions"""
            try:
                # Format the prompt with current messages
                formatted_prompt = prompt.invoke({"messages": state["messages"]})

                # Get response from LLM
                response = llm_with_tools.invoke(formatted_prompt)

                # Return updated messages
                return {"messages": [response]}
            except Exception as e:
                logger.error(f"Error in agent_node: {e}")
                # Return error message
                error_msg = AIMessage(content=f"Xin lỗi, có lỗi xảy ra: {str(e)}")
                return {"messages": [error_msg]}

        def should_continue(state: AgentState) -> str:
            """Determine if we should continue with tool execution or end"""
            messages = state["messages"]
            last_message = messages[-1]

            # Check if the last message has tool calls
            if isinstance(last_message, AIMessage) and hasattr(last_message, 'tool_calls') and last_message.tool_calls:
                return "tools"
            else:
                return END

        # Create the graph
        graph = StateGraph(AgentState)

        # Add nodes
        graph.add_node("agent", agent_node)
        graph.add_node("tools", ToolNode(self.tools))

        # Add edges
        graph.add_edge(START, "agent")
        graph.add_conditional_edges("agent", should_continue)
        graph.add_edge("tools", "agent")

        # Compile with memory
        compiled_graph = graph.compile(checkpointer=self.memory)

        return compiled_graph


    async def run(self, user_input: str, session_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Run agent with user input using LangGraph

        Args:
            user_input: User's message
            session_id: Session identifier for conversation tracking

        Returns:
            Dict containing response and metadata
        """
        if not self.graph:
            raise ValueError("Agent not initialized. Call initialize() first.")

        if not session_id:
            session_id = f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        try:
            # Create human message
            human_message = HumanMessage(content=user_input)

            # Configure thread for memory
            config = RunnableConfig(
                configurable={"thread_id": session_id},
                recursion_limit=50
            )

            # Run the graph
            result = await self.graph.ainvoke(
                {"messages": [human_message]},
                config
            )

            # Extract final answer from messages
            messages = result.get("messages", [])
            final_answer = ""

            # Get the last AI message
            for msg in reversed(messages):
                if isinstance(msg, AIMessage) and msg.content and not msg.tool_calls:
                    final_answer = msg.content
                    break

            # If no AI message found, try the last message
            if not final_answer and messages:
                last_msg = messages[-1]
                if hasattr(last_msg, "content"):
                    final_answer = last_msg.content

            # Extract suggested actions from response
            suggested_actions = self._extract_suggested_actions(final_answer)

            # Count tool calls
            tool_calls = sum(1 for msg in messages if hasattr(msg, 'tool_calls') and msg.tool_calls)

            result_dict = {
                "response": final_answer,
                "suggested_actions": suggested_actions,
                "session_id": session_id,
                "timestamp": datetime.now().isoformat(),
                "tool_calls": tool_calls
            }

            logger.info(f"Agent response generated for session {session_id}")
            return result_dict

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
        """Clear conversation memory in LangGraph"""
        if session_id and self.memory:
            # Clear specific thread memory
            if hasattr(self.memory, 'storage'):
                self.memory.storage.pop(session_id, None)
            logger.info(f"Memory cleared for session {session_id}")
        elif not session_id and self.memory:
            # Clear all memories
            if hasattr(self.memory, 'storage'):
                self.memory.storage.clear()
            logger.info("All memories cleared")

    async def get_conversation_history(self, session_id: Optional[str] = None) -> List[Dict[str, Any]]:
        """Get conversation history from LangGraph memory"""
        try:
            if not session_id or not self.memory:
                return []

            # Get stored messages from memory storage
            history = []
            if hasattr(self.memory, 'storage') and session_id in self.memory.storage:
                thread_data = self.memory.storage[session_id]
                # LangGraph stores messages in different structure
                if 'channels' in thread_data:
                    channels = thread_data['channels']
                    if 'messages' in channels:
                        messages_data = channels['messages']
                        # messages_data might be a list or dict with different structure
                        if isinstance(messages_data, list):
                            messages = messages_data
                        elif isinstance(messages_data, dict) and 'messages' in messages_data:
                            messages = messages_data['messages']
                        else:
                            messages = []

                        for message in messages:
                            if hasattr(message, 'content'):
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
