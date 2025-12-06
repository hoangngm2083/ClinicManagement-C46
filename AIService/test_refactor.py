#!/usr/bin/env python3
"""
Test script for the refactored AIService with LangGraph and PGVector
"""

import asyncio
import sys
from pathlib import Path

# Add the app directory to the Python path
sys.path.insert(0, str(Path(__file__).parent / "app"))

from app.config.settings import settings
from app.rag.pgvector_store import PGVectorStore
from app.services.clinic_api import ClinicAPIService


async def test_pgvector_connection():
    """Test PGVectorStore connection"""
    print("🔍 Testing PGVectorStore connection...")

    try:
        vector_store = PGVectorStore()
        health = vector_store.health_check()

        if health:
            print("✅ PGVectorStore connection successful")

            # Test collection operations
            collections = ["doctors", "medical_packages", "clinic_processes", "faq"]

            for collection in collections:
                exists = vector_store.collection_exists(collection)
                print(f"   Collection '{collection}': {'exists' if exists else 'not exists'}")

                if exists:
                    stats = vector_store.get_collection_stats(collection)
                    print(f"   Stats: {stats}")

            return True
        else:
            print("❌ PGVectorStore health check failed")
            return False

    except Exception as e:
        print(f"❌ PGVectorStore connection failed: {e}")
        return False


async def test_clinic_api_connection():
    """Test ClinicAPIService connection"""
    print("🔍 Testing ClinicAPIService connection...")

    try:
        clinic_api = ClinicAPIService()

        # Test basic connectivity
        async with clinic_api:
            doctors = await clinic_api.get_doctors(page=1, keyword="")
            packages = await clinic_api.get_medical_packages(page=1, keyword="")

        print(f"✅ ClinicAPIService connection successful")
        print(f"   Doctors available: {len(doctors)}")
        print(f"   Packages available: {len(packages)}")

        return True

    except Exception as e:
        print(f"❌ ClinicAPIService connection failed: {e}")
        return False


async def test_langgraph_agent():
    """Test LangGraphAgent initialization"""
    print("🔍 Testing LangGraphAgent initialization...")

    try:
        from app.agents.langgraph_agent import LangGraphAgent

        agent = LangGraphAgent()
        print("✅ LangGraphAgent created successfully")

        # Test initialization (without actual API calls)
        # Note: This would require running microservices
        print("   Note: Full initialization requires running microservices")

        return True

    except Exception as e:
        print(f"❌ LangGraphAgent initialization failed: {e}")
        return False


async def main():
    """Run all tests"""
    print("🚀 Testing Refactored AIService")
    print("=" * 50)

    results = []

    # Test PGVector connection
    pgvector_ok = await test_pgvector_connection()
    results.append(("PGVector Connection", pgvector_ok))

    print()

    # Test Clinic API connection
    clinic_api_ok = await test_clinic_api_connection()
    results.append(("Clinic API Connection", clinic_api_ok))

    print()

    # Test LangGraph Agent
    langgraph_ok = await test_langgraph_agent()
    results.append(("LangGraph Agent", langgraph_ok))

    print()
    print("📊 Test Results Summary:")
    print("-" * 30)

    all_passed = True
    for test_name, passed in results:
        status = "✅ PASS" if passed else "❌ FAIL"
        print(f"{test_name}: {status}")
        if not passed:
            all_passed = False

    print()
    if all_passed:
        print("🎉 All basic tests passed! AIService refactor is ready for integration testing.")
    else:
        print("⚠️ Some tests failed. Please check the errors above.")

    return all_passed


if __name__ == "__main__":
    success = asyncio.run(main())
    sys.exit(0 if success else 1)
