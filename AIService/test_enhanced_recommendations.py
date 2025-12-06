#!/usr/bin/env python3
"""
Test script for enhanced medical package recommendations
"""
import asyncio
import sys
import os
sys.path.append('/app')

from app.services.clinic_api import ClinicAPIService
from app.config.settings import settings

async def test_recommendations():
    """Test the enhanced recommendation system"""

    # Initialize service
    clinic_api = ClinicAPIService()

    # Test cases
    test_cases = [
        {
            "symptoms": "đau răng, răng số 6 bị sâu",
            "expected_urgency": "medium",
            "description": "Răng miệng - trung bình"
        },
        {
            "symptoms": "đau đầu dữ dội, chóng mặt, tầm nhìn mờ",
            "expected_urgency": "high",
            "description": "Đau đầu cấp tính - cao"
        },
        {
            "symptoms": "ho, sốt, mệt mỏi",
            "expected_urgency": "medium",
            "description": "Cảm cúm - trung bình"
        },
        {
            "symptoms": "đau bụng, buồn nôn, tiêu chảy",
            "expected_urgency": "medium",
            "description": "Tiêu hóa - trung bình"
        },
        {
            "symptoms": "mụn nhiều, da khô, ngứa",
            "expected_urgency": "low",
            "description": "Da liễu - thấp"
        }
    ]

    print("🧪 Testing Enhanced Medical Package Recommendations\n")

    for i, test_case in enumerate(test_cases, 1):
        print(f"Test {i}: {test_case['description']}")
        print(f"Symptoms: {test_case['symptoms']}")

        try:
            recommendations = await clinic_api.get_package_recommendations(test_case['symptoms'])

            if not recommendations:
                print("❌ No recommendations found")
                continue

            print(f"✅ Found {len(recommendations)} recommendations")

            # Check structure
            has_urgency = any('urgency' in pkg.get('_urgency', '') for pkg in recommendations)
            has_matched_symptoms = any(pkg.get('_matched_symptoms') for pkg in recommendations)

            print(f"   - Urgency classification: {'✅' if has_urgency else '❌'}")
            print(f"   - Matched symptoms: {'✅' if has_matched_symptoms else '❌'}")

            # Show top recommendation details
            top_pkg = recommendations[0]
            print(f"   - Top recommendation: {top_pkg.get('name', 'N/A')}")
            print(f"   - Urgency: {top_pkg.get('_urgency', 'unknown')}")
            print(f"   - Matched: {', '.join(top_pkg.get('_matched_symptoms', []))}")

        except Exception as e:
            print(f"❌ Error: {e}")

        print()

    await clinic_api.client.aclose()

if __name__ == "__main__":
    asyncio.run(test_recommendations())
