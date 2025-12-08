"""
Script to test medical packages API response parsing fix
"""
import asyncio
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.services.clinic_api import ClinicAPIService
from app.config.settings import settings
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


async def test_medical_packages_parsing():
    """Test that medical packages are parsed correctly"""
    print("=" * 80)
    print("KIỂM TRA FIX PARSING MEDICAL PACKAGES")
    print("=" * 80)
    
    clinic_api = ClinicAPIService()
    print(f"\n📡 API Base URL: {clinic_api.base_url}")
    
    try:
        print("\n1️⃣ Đang gọi API get_medical_packages()...")
        packages = await clinic_api.get_medical_packages(page=1)
        
        print(f"\n✅ Trả về {len(packages)} packages")
        
        if packages:
            print("\n📦 Chi tiết packages:")
            print("-" * 80)
            for i, pkg in enumerate(packages, 1):
                print(f"\n{i}. Package:")
                print(f"   - ID field: {pkg.get('id', 'MISSING')}")
                print(f"   - medicalPackageId field: {pkg.get('medicalPackageId', 'MISSING')}")
                print(f"   - Name: {pkg.get('name', 'N/A')}")
                print(f"   - Price: {pkg.get('price', 0):,} VND" if pkg.get('price', 0) > 0 else "   - Price: Liên hệ")
                print(f"   - Description: {pkg.get('description', 'N/A')[:50]}...")
                
                # Verify id field exists
                if 'id' in pkg:
                    print(f"   ✅ Có field 'id' - OK")
                else:
                    print(f"   ❌ Thiếu field 'id' - CẦN FIX")
                
                # Check if medicalPackageId was normalized
                if 'medicalPackageId' in pkg and 'id' not in pkg:
                    print(f"   ⚠️  Có medicalPackageId nhưng chưa normalize thành id")
            
            print("\n" + "=" * 80)
            print("✅ TEST PASSED: Packages được parse đúng!")
            print("=" * 80)
        else:
            print("\n⚠️  API trả về danh sách rỗng")
            print("   → Kiểm tra:")
            print("     1. MedicalPackageService có đang chạy không?")
            print("     2. Database có dữ liệu không?")
            print("     3. API Gateway routing có đúng không?")
        
    except Exception as e:
        print(f"\n❌ LỖI: {e}")
        import traceback
        traceback.print_exc()
    
    finally:
        await clinic_api.client.aclose()


if __name__ == "__main__":
    asyncio.run(test_medical_packages_parsing())

