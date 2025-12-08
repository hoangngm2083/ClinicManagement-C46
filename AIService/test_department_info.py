#!/usr/bin/env python3
"""
Test script for department info functionality
"""
import asyncio
import sys
import os
sys.path.append('/app')

from app.services.clinic_api import ClinicAPIService
from app.config.settings import settings

async def test_department_info():
    """Test the department info functionality"""

    # Initialize service
    clinic_api = ClinicAPIService()

    print("🧪 Testing Department Info Functionality\n")

    try:
        # Test 1: Get all departments
        print("Test 1: Get all departments")
        departments = await clinic_api.get_departments()
        if departments:
            print(f"✅ Found {len(departments)} departments")
            for dept in departments[:3]:  # Show first 3
                print(f"  - {dept.get('name', 'N/A')}: {dept.get('description', 'No desc')[:50]}...")
        else:
            print("❌ No departments found")

        print()

        # Test 2: Get doctors in a department (if departments exist)
        if departments:
            first_dept = departments[0]
            dept_id = first_dept.get('id')
            dept_name = first_dept.get('name', 'Unknown')

            print(f"Test 2: Get doctors in department '{dept_name}'")
            doctors = await clinic_api.get_doctors(department_id=dept_id)
            if doctors:
                print(f"✅ Found {len(doctors)} doctors in {dept_name}")
                for doctor in doctors[:2]:  # Show first 2 doctors
                    print(f"  - {doctor.get('name', 'N/A')} ({doctor.get('email', 'N/A')})")
            else:
                print(f"❌ No doctors found in {dept_name}")

        print("\n✅ Department info functionality tests completed!")

    except Exception as e:
        print(f"❌ Error testing department info: {e}")
        import traceback
        traceback.print_exc()

    finally:
        await clinic_api.client.aclose()

if __name__ == "__main__":
    asyncio.run(test_department_info())
