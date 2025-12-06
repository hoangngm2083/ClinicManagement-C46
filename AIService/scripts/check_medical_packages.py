"""
Script to check medical packages data from API and compare with fallback data
"""
import asyncio
import sys
import os
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.services.clinic_api import ClinicAPIService
from app.config.settings import settings
from app.models.prompts import FALLBACK_PACKAGES_LIST, build_dynamic_system_prompt
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


async def check_medical_packages():
    """Check medical packages from API"""
    print("=" * 80)
    print("KIỂM TRA DỮ LIỆU GÓI KHÁM")
    print("=" * 80)
    
    # Initialize clinic API
    clinic_api = ClinicAPIService()
    print(f"\n📡 API Base URL: {clinic_api.base_url}")
    print(f"🔗 Endpoint: {clinic_api.base_url}/api/medical-package")
    
    try:
        # Try to get packages from API
        print("\n1️⃣ Đang lấy dữ liệu từ API...")
        packages = await clinic_api.get_medical_packages(page=1)
        
        print(f"\n✅ API trả về {len(packages)} gói khám")
        
        if packages:
            print("\n📦 Danh sách gói khám từ API:")
            print("-" * 80)
            for i, pkg in enumerate(packages[:10], 1):  # Show first 10
                print(f"{i}. {pkg.get('name', 'N/A')}")
                print(f"   ID: {pkg.get('id', 'N/A')}")
                print(f"   Giá: {pkg.get('price', 0):,} VND" if pkg.get('price', 0) > 0 else "   Giá: Liên hệ")
                print(f"   Mô tả: {pkg.get('description', 'Không có mô tả')[:100]}...")
                print()
        else:
            print("\n⚠️  API trả về danh sách rỗng!")
            print("   → Hệ thống sẽ sử dụng FALLBACK_PACKAGES_LIST")
        
        # Check what build_dynamic_system_prompt returns
        print("\n2️⃣ Kiểm tra system prompt được tạo...")
        system_prompt = await build_dynamic_system_prompt(clinic_api)
        
        # Extract packages section
        if "CÁC GÓI KHÁM CHÍNH:" in system_prompt:
            start_idx = system_prompt.find("CÁC GÓI KHÁM CHÍNH:")
            end_idx = system_prompt.find("**LƯU Ý QUAN TRỌNG:", start_idx)
            packages_section = system_prompt[start_idx:end_idx] if end_idx > 0 else system_prompt[start_idx:]
            
            print("\n📝 Phần gói khám trong system prompt:")
            print("-" * 80)
            print(packages_section)
            
            # Check if using fallback
            if "Khám tổng quát cơ bản: Khám tổng thể cơ bản - Giá: 300,000 VND" in packages_section:
                print("\n⚠️  PHÁT HIỆN: Đang sử dụng FALLBACK_PACKAGES_LIST!")
                print("   → Dữ liệu không phải từ database thực tế")
            else:
                print("\n✅ Đang sử dụng dữ liệu từ API")
        
        # Compare with fallback
        print("\n3️⃣ So sánh với FALLBACK_PACKAGES_LIST:")
        print("-" * 80)
        print("FALLBACK data:")
        print(FALLBACK_PACKAGES_LIST)
        
        if not packages:
            print("\n❌ VẤN ĐỀ: API không trả về dữ liệu!")
            print("   → Cần kiểm tra:")
            print("     1. MedicalPackageService có đang chạy không?")
            print("     2. API Gateway có route đúng không?")
            print("     3. Database có dữ liệu không?")
            print("     4. Có cần chạy migration để tạo test data không?")
        
    except Exception as e:
        print(f"\n❌ LỖI khi gọi API: {e}")
        print(f"   Loại lỗi: {type(e).__name__}")
        print("\n   → Hệ thống sẽ sử dụng FALLBACK_PACKAGES_LIST")
        print("\n   Cần kiểm tra:")
        print("     1. API Gateway có đang chạy không?")
        print("     2. MedicalPackageService có đang chạy không?")
        print("     3. Network connectivity giữa các services")
        print("     4. API endpoint có đúng không?")
    
    finally:
        await clinic_api.client.aclose()
    
    print("\n" + "=" * 80)


if __name__ == "__main__":
    asyncio.run(check_medical_packages())

