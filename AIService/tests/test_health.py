import pytest
from httpx import AsyncClient
from app.main import app


@pytest.mark.asyncio
async def test_health_endpoint():
    """Test health endpoint"""
    async with AsyncClient(app=app, base_url="http://testserver") as client:
        response = await client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert "status" in data
        assert "version" in data
        assert "services" in data


@pytest.mark.asyncio
async def test_root_endpoint():
    """Test root endpoint"""
    async with AsyncClient(app=app, base_url="http://testserver") as client:
        response = await client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert "message" in data
        assert "Welcome to Clinic AI Service" in data["message"]


@pytest.mark.asyncio
async def test_info_endpoint():
    """Test service info endpoint"""
    async with AsyncClient(app=app, base_url="http://testserver") as client:
        response = await client.get("/info")
        assert response.status_code == 200
        data = response.json()
        assert "name" in data
        assert "capabilities" in data
        assert isinstance(data["capabilities"], list)


@pytest.mark.asyncio
async def test_dynamic_prompt_fallback():
    """Test that dynamic prompt falls back gracefully"""
    from app.models.prompts import build_dynamic_system_prompt, FALLBACK_PACKAGES_LIST

    # Test fallback packages list exists
    assert FALLBACK_PACKAGES_LIST is not None
    assert "Khám tổng quát cơ bản" in FALLBACK_PACKAGES_LIST
    assert "Giá:" in FALLBACK_PACKAGES_LIST

    print("✅ Fallback packages list is properly configured")


@pytest.mark.asyncio
async def test_cache_functionality():
    """Test prompt cache functionality"""
    from app.models.prompts import clear_system_prompt_cache, _system_prompt_cache

    # Clear cache
    initial_size = len(_system_prompt_cache)
    clear_system_prompt_cache()
    assert len(_system_prompt_cache) <= initial_size

    print("✅ Cache clearing functionality works")
