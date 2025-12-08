from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import logging
import uvicorn
from contextlib import asynccontextmanager

from .config.settings import settings
from .services.clinic_api import ClinicAPIService
from .rag.pgvector_store import PGVectorStore
from .rag.data_loader import DataLoader
from .rag.data_sync import DataSyncService
from .agents.langgraph_agent import AgentManager
from .utils.helpers import setup_logging

# Setup logging
setup_logging()
logger = logging.getLogger(__name__)

# API prefix configuration
api_prefix = settings.ai_api_prefix

# Global instances
clinic_api: Optional[ClinicAPIService] = None
vector_store: Optional[PGVectorStore] = None
data_loader: Optional[DataLoader] = None
data_sync_service: Optional[DataSyncService] = None
agent_manager: Optional[AgentManager] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager"""
    global clinic_api, vector_store, data_loader, data_sync_service, agent_manager

    logger.info("Starting AI Service...")

    try:
        # Initialize services
        clinic_api = ClinicAPIService()
        vector_store = PGVectorStore()
        data_loader = DataLoader(clinic_api, vector_store)
        data_sync_service = DataSyncService(clinic_api, data_loader)
        agent_manager = AgentManager()

        # Initialize default agent
        await agent_manager.initialize_default_agent(clinic_api, vector_store)

        # Load initial data
        logger.info("Loading initial data...")
        await data_loader.load_initial_data()

        # Start data sync service for periodic updates
        logger.info("Starting data sync service...")
        await data_sync_service.start()
        logger.info("Data sync service started")

        logger.info("AI Service started successfully")

    except Exception as e:
        logger.error(f"Error during startup: {e}")
        raise

    yield

    # Cleanup
    logger.info("Shutting down AI Service...")
    try:
        # Stop data sync service
        if data_sync_service:
            await data_sync_service.stop()
            logger.info("Data sync service stopped")
        
        if clinic_api and clinic_api.client:
            await clinic_api.client.aclose()
            logger.info("Clinic API client closed")
    except Exception as e:
        logger.error(f"Error during cleanup: {e}")


# Create FastAPI app
app = FastAPI(
    title="Clinic AI Service",
    description="AI-powered chatbot for clinic management and appointment booking",
    version="1.0.0",
    lifespan=lifespan
)

# Add CORS middleware
# app.add_middleware(
#     CORSMiddleware,
#     allow_origins=["*"],  # Configure appropriately for production
#     allow_credentials=True,
#     allow_methods=["*"],
#     allow_headers=["*"],
# )


# Pydantic models
class ChatRequest(BaseModel):
    message: str = Field(..., description="User's message")
    session_id: Optional[str] = Field(None, description="Session identifier for conversation tracking")


class SuggestedAction(BaseModel):
    action: str = Field(..., description="Action identifier")
    label: str = Field(..., description="Human-readable label")
    description: Optional[str] = Field(None, description="Action description")


class ChatResponse(BaseModel):
    response: str = Field(..., description="AI assistant's response")
    suggested_actions: List[str] = Field(default_factory=list, description="Suggested next actions")
    session_id: Optional[str] = Field(None, description="Session identifier")
    timestamp: str = Field(..., description="Response timestamp")
    error: Optional[str] = Field(None, description="Error message if any")


class HealthResponse(BaseModel):
    status: str = Field(..., description="Service health status")
    version: str = Field(..., description="Service version")
    services: Dict[str, bool] = Field(..., description="Status of dependent services")


@app.get(f"{api_prefix}/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    try:
        # Check vector store
        vector_store_healthy = vector_store.health_check() if vector_store else False

        # Check clinic API
        clinic_api_healthy = False
        if clinic_api:
            try:
                # Don't use async with - clinic_api is a long-lived global instance
                # Simple health check - try to get doctors
                await clinic_api.get_doctors(page=1)
                clinic_api_healthy = True
            except Exception as e:
                logger.warning(f"Clinic API health check failed: {e}")
                clinic_api_healthy = False

        # Overall health
        overall_healthy = vector_store_healthy and clinic_api_healthy

        return HealthResponse(
            status="healthy" if overall_healthy else "unhealthy",
            version="1.0.0",
            services={
                "vector_store": vector_store_healthy,
                "clinic_api": clinic_api_healthy,
                "agent": agent_manager is not None
            }
        )

    except Exception as e:
        logger.error(f"Health check error: {e}")
        return HealthResponse(
            status="unhealthy",
            version="1.0.0",
            services={
                "vector_store": False,
                "clinic_api": False,
                "agent": False
            }
        )


@app.post(f"{api_prefix}/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    Main chat endpoint for AI assistant interaction

    This endpoint accepts user messages and returns AI-generated responses
    with optional suggested actions.
    """
    if not agent_manager:
        raise HTTPException(status_code=503, detail="AI service not initialized")

    try:
        # Run agent
        result = await agent_manager.run_agent(
            user_input=request.message,
            session_id=request.session_id
        )

        return ChatResponse(**result)

    except Exception as e:
        logger.error(f"Chat error: {e}")
        raise HTTPException(status_code=500, detail="Internal server error")


@app.get(f"{api_prefix}/chat/history/{{session_id}}")
async def get_chat_history(session_id: str):
    """Get conversation history for a session"""
    if not agent_manager:
        raise HTTPException(status_code=503, detail="AI service not initialized")

    try:
        agent = agent_manager.get_agent(session_id)
        history = await agent.get_conversation_history(session_id)
        return {"session_id": session_id, "history": history}

    except Exception as e:
        logger.error(f"Error getting chat history: {e}")
        raise HTTPException(status_code=500, detail="Error retrieving chat history")


@app.delete(f"{api_prefix}/chat/session/{{session_id}}")
async def clear_session(session_id: str):
    """Clear conversation session"""
    if not agent_manager:
        raise HTTPException(status_code=503, detail="AI service not initialized")

    try:
        agent_manager.cleanup_session(session_id)
        return {"message": f"Session {session_id} cleared successfully"}

    except Exception as e:
        logger.error(f"Error clearing session: {e}")
        raise HTTPException(status_code=500, detail="Error clearing session")


@app.post(f"{api_prefix}/admin/clear-cache")
async def clear_cache():
    """Clear system prompt cache (admin endpoint)"""
    try:
        from .models.prompts import clear_system_prompt_cache
        clear_system_prompt_cache()
        return {"message": "Cache cleared successfully"}
    except Exception as e:
        logger.error(f"Error clearing cache: {e}")
        raise HTTPException(status_code=500, detail="Error clearing cache")


@app.get(f"{api_prefix}/info")
async def get_service_info():
    """Get service information"""
    return {
        "name": "Clinic AI Service",
        "version": "1.0.0",
        "description": "AI-powered chatbot for clinic management",
        "capabilities": [
            "doctor_information_search",
            "appointment_booking",
            "medical_package_recommendations",
            "clinic_information_queries",
            "schedule_checking"
        ],
        "supported_languages": ["vi"],
        "rate_limit": f"{settings.rate_limit_requests} requests per {settings.rate_limit_window_seconds} seconds"
    }


@app.post(f"{api_prefix}/admin/clear-prompt-cache")
async def clear_prompt_cache():
    """Clear system prompt cache (admin only)"""
    if not agent_manager:
        raise HTTPException(status_code=503, detail="AI service not initialized")

    try:
        from .models.prompts import clear_system_prompt_cache
        clear_system_prompt_cache()

        return {"message": "System prompt cache cleared successfully"}

    except Exception as e:
        logger.error(f"Error clearing prompt cache: {e}")
        raise HTTPException(status_code=500, detail="Error clearing prompt cache")


@app.get(f"{api_prefix}/admin/prompt-preview")
async def preview_system_prompt():
    """Preview current system prompt (admin only)"""
    if not agent_manager:
        raise HTTPException(status_code=503, detail="AI service not initialized")

    try:
        from .models.prompts import build_dynamic_system_prompt
        if clinic_api:
            prompt = await build_dynamic_system_prompt(clinic_api)
            return {"system_prompt": prompt}
        else:
            raise HTTPException(status_code=503, detail="Clinic API not initialized")

    except Exception as e:
        logger.error(f"Error previewing prompt: {e}")
        raise HTTPException(status_code=500, detail="Error previewing prompt")


@app.get(f"{api_prefix}/admin/sync-status")
async def get_sync_status():
    """Get data sync service status (admin only)"""
    if not data_sync_service:
        raise HTTPException(status_code=503, detail="Data sync service not initialized")
    
    try:
        status = data_sync_service.get_sync_status()
        return status
    except Exception as e:
        logger.error(f"Error getting sync status: {e}")
        raise HTTPException(status_code=500, detail="Error getting sync status")


@app.post(f"{api_prefix}/admin/sync-now")
async def trigger_manual_sync():
    """Manually trigger data sync (admin only)"""
    if not data_sync_service:
        raise HTTPException(status_code=503, detail="Data sync service not initialized")
    
    try:
        result = await data_sync_service.manual_sync_all()
        return result
    except Exception as e:
        logger.error(f"Error triggering manual sync: {e}")
        raise HTTPException(status_code=500, detail="Error triggering manual sync")


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "Welcome to Clinic AI Service",
        "docs": "/docs",
        "health": f"{api_prefix}/health",
        "chat": f"{api_prefix}/chat"
    }


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=settings.ai_service_port,
        reload=True,
        log_level="info"
    )
