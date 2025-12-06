#!/usr/bin/env python3
"""
Development runner for Clinic AI Service
"""
import uvicorn
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

if __name__ == "__main__":
    port = int(os.getenv("AI_SERVICE_PORT", 8000))

    print("🚀 Starting Clinic AI Service...")
    print(f"📡 Service will be available at: http://localhost:{port}")
    print(f"📚 API Documentation at: http://localhost:{port}/docs")
    print(f"🔍 Health check at: http://localhost:{port}/health")

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=port,
        reload=True,
        log_level="info",
        reload_dirs=["app"]
    )
