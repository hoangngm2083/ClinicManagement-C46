#!/usr/bin/env python3
"""
Script to check and fix vector store and clinic API connections
"""
import asyncio
import sys
import os

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.rag.pgvector_store import PGVectorStore
from app.services.clinic_api import ClinicAPIService
from app.config.settings import settings


async def test_vector_store():
    """Test vector store connection"""
    print("=" * 60)
    print("🔍 Testing Vector Store Connection")
    print("=" * 60)
    
    try:
        vs = PGVectorStore()
        health = vs.health_check()
        print(f"✅ Vector Store Health: {health}")
        print(f"✅ DB Available: {vs.db_available}")
        
        if not health:
            print("\n⚠️  Vector Store is not healthy")
            print("Possible issues:")
            print("  1. pgvector extension not installed in PostgreSQL")
            print("  2. Database connection failed")
            print("  3. Table not created")
            
            # Try to check connection
            try:
                import psycopg2
                conn = psycopg2.connect(
                    host=settings.postgres_host,
                    port=settings.postgres_port,
                    database=settings.postgres_db,
                    user=settings.postgres_user,
                    password=settings.postgres_password
                )
                cursor = conn.cursor()
                cursor.execute("SELECT extname FROM pg_extension WHERE extname = 'vector';")
                result = cursor.fetchone()
                if result:
                    print("  ✅ pgvector extension is installed")
                else:
                    print("  ❌ pgvector extension is NOT installed")
                    print("  💡 Run: CREATE EXTENSION vector; in PostgreSQL")
                conn.close()
            except Exception as e:
                print(f"  ❌ Connection test failed: {e}")
        else:
            print("✅ Vector Store is working correctly!")
            
        return health
    except Exception as e:
        print(f"❌ Error testing vector store: {e}")
        return False


async def test_clinic_api():
    """Test clinic API connection"""
    print("\n" + "=" * 60)
    print("🔍 Testing Clinic API Connection")
    print("=" * 60)
    
    try:
        async with ClinicAPIService() as api:
            print(f"✅ ClinicAPIService initialized")
            print(f"   Base URL: {api.base_url}")
            
            # Test getting doctors
            try:
                doctors = await api.get_doctors(page=1)
                print(f"✅ Get doctors: SUCCESS")
                print(f"   Doctors count: {len(doctors)}")
                if doctors:
                    print(f"   Sample doctor: {doctors[0].get('name', 'N/A')}")
                return True
            except Exception as e:
                print(f"❌ Get doctors failed: {e}")
                print(f"   Error type: {type(e).__name__}")
                
                # Test basic connection
                try:
                    import httpx
                    async with httpx.AsyncClient(timeout=5.0) as client:
                        response = await client.get(f"{api.base_url}/api/staff?page=1")
                        print(f"   Direct HTTP test: Status {response.status_code}")
                        if response.status_code == 200:
                            print("   ✅ API Gateway is accessible")
                        else:
                            print(f"   ⚠️  API Gateway returned {response.status_code}")
                except Exception as http_e:
                    print(f"   ❌ HTTP connection failed: {http_e}")
                
                return False
    except Exception as e:
        print(f"❌ Error testing clinic API: {e}")
        return False


async def main():
    """Run all connection tests"""
    print("\n" + "=" * 60)
    print("🔧 AI Service Connection Diagnostics")
    print("=" * 60)
    print()
    
    # Test vector store
    vector_ok = await test_vector_store()
    
    # Test clinic API
    api_ok = await test_clinic_api()
    
    # Summary
    print("\n" + "=" * 60)
    print("📊 Summary")
    print("=" * 60)
    print(f"Vector Store:  {'✅ OK' if vector_ok else '❌ FAILED'}")
    print(f"Clinic API:    {'✅ OK' if api_ok else '❌ FAILED'}")
    print()
    
    if vector_ok and api_ok:
        print("🎉 All connections are working!")
        return 0
    else:
        print("⚠️  Some connections need attention")
        print("\n💡 Fix suggestions:")
        if not vector_ok:
            print("  1. Install pgvector extension in PostgreSQL:")
            print("     docker exec postgres psql -U booking -d vector_db -c 'CREATE EXTENSION vector;'")
        if not api_ok:
            print("  2. Check API Gateway is running:")
            print("     docker-compose ps api-gateway")
            print("  3. Check network connectivity from ai-service to api-gateway")
        return 1


if __name__ == "__main__":
    exit_code = asyncio.run(main())
    sys.exit(exit_code)

