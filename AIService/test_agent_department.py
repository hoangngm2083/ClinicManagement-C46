#!/usr/bin/env python3
"""
Test script for agent department consultation functionality
"""
import asyncio
import sys
import os
sys.path.append('/app')

from app.agents.agent import create_clinic_agent
from app.services.clinic_api import ClinicAPIService
from app.rag.pgvector_store import PGVectorStore
from app.config.settings import settings

async def test_agent_department_queries():
    """Test agent with department-related queries"""

    print("🤖 Testing Agent Department Consultation\n")

    # Initialize services
    clinic_api = ClinicAPIService()
    vector_store = PGVectorStore()

    try:
        # Create agent
        agent = await create_clinic_agent(clinic_api, vector_store)

        # Test queries
        test_queries = [
            "Phòng khám có những khoa nào?",
            "Khoa Pediatrics có những bác sĩ nào?",
            "Tôi muốn khám khoa tim mạch",
            "Khoa Neurology chuyên khám gì?"
        ]

        for i, query in enumerate(test_queries, 1):
            print(f"\n🧪 Test {i}: {query}")
            print("-" * 50)

            try:
                # Run agent
                response = await agent.ainvoke({"input": query})
                print(response["output"][:500] + "..." if len(response["output"]) > 500 else response["output"])
            except Exception as e:
                print(f"❌ Error: {e}")

            print()

    except Exception as e:
        print(f"❌ Setup Error: {e}")
        import traceback
        traceback.print_exc()

    finally:
        await clinic_api.client.aclose()

if __name__ == "__main__":
    asyncio.run(test_agent_department_queries())
