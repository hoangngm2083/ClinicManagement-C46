#!/usr/bin/env python3
"""
Test script for clinical analysis and medical recommendations
"""
import asyncio
import sys
import os
sys.path.append('/app')

from app.services.medical_analyzer import MedicalSymptomAnalyzer, UrgencyLevel, SymptomCategory
from app.services.clinic_api import ClinicAPIService
from app.config.settings import settings

async def test_clinical_analysis():
    """Test the enhanced clinical analysis system"""

    print("🩺 Testing Clinical Symptom Analysis\n")

    analyzer = MedicalSymptomAnalyzer()

    # Test cases with various symptoms
    test_cases = [
        {
            "symptoms": "đau ngực trái, khó thở, mồ hôi lạnh",
            "expected_category": SymptomCategory.CARDIOVASCULAR,
            "expected_urgency": UrgencyLevel.HIGH,
            "description": "Acute coronary syndrome symptoms"
        },
        {
            "symptoms": "đau đầu đột ngột dữ dội, nôn mửa, cổ cứng",
            "expected_category": SymptomCategory.NEUROLOGICAL,
            "expected_urgency": UrgencyLevel.CRITICAL,
            "description": "Meningitis or stroke symptoms"
        },
        {
            "symptoms": "ho ra máu, sụt cân, mệt mỏi kéo dài",
            "expected_category": SymptomCategory.RESPIRATORY,
            "expected_urgency": UrgencyLevel.HIGH,
            "description": "Possible lung cancer symptoms"
        },
        {
            "symptoms": "đau bụng dữ dội, nôn ói, sốt cao",
            "expected_category": SymptomCategory.GASTROINTESTINAL,
            "expected_urgency": UrgencyLevel.HIGH,
            "description": "Acute abdomen symptoms"
        },
        {
            "symptoms": "mụn trứng cá, da dầu, rụng tóc",
            "expected_category": SymptomCategory.DERMATOLOGICAL,
            "expected_urgency": UrgencyLevel.LOW,
            "description": "Acne vulgaris symptoms"
        },
        {
            "symptoms": "đau răng, sưng lợi, mủ răng",
            "expected_category": SymptomCategory.DENTAL,
            "expected_urgency": UrgencyLevel.MEDIUM,
            "description": "Dental abscess symptoms"
        }
    ]

    for i, test_case in enumerate(test_cases, 1):
        print(f"Test {i}: {test_case['description']}")
        print(f"Symptoms: {test_case['symptoms']}")

        try:
            analysis = analyzer.analyze_symptoms(test_case['symptoms'])

            print("✅ Analysis Results:")
            print(f"   Category: {analysis.primary_category.value}")
            print(f"   Urgency: {analysis.urgency_level.value}")
            print(".2%")
            print(f"   Related symptoms: {', '.join(analysis.related_symptoms[:3])}")
            print(f"   Possible conditions: {', '.join(analysis.possible_conditions[:3])}")
            print(f"   Recommended specialties: {', '.join(analysis.recommended_specialties)}")

            if analysis.red_flags:
                print(f"   ⚠️ Red flags: {len(analysis.red_flags)} detected")

            # Validate expectations
            category_match = analysis.primary_category == test_case['expected_category']
            urgency_match = analysis.urgency_level == test_case['expected_urgency']

            print(f"   🎯 Category match: {'✅' if category_match else '❌'}")
            print(f"   🎯 Urgency match: {'✅' if urgency_match else '❌'}")

        except Exception as e:
            print(f"❌ Error: {e}")

        print("\n" + "="*60 + "\n")

async def test_integration_with_packages():
    """Test integration with medical package recommendations"""

    print("🔗 Testing Integration with Package Recommendations\n")

    try:
        # Initialize services
        clinic_api = ClinicAPIService()
        analyzer = MedicalSymptomAnalyzer()

        # Test with a complex symptom
        symptoms = "đau ngực khi gắng sức, khó thở khi nằm, sưng chân"
        print(f"Testing with symptoms: {symptoms}")

        # Get clinical analysis
        analysis = analyzer.analyze_symptoms(symptoms)
        print("📊 Clinical Analysis:")
        print(f"   Category: {analysis.primary_category.value}")
        print(f"   Urgency: {analysis.urgency_level.value}")
        print(".2%")

        # Get available packages
        packages = await clinic_api.get_medical_packages()
        print(f"   Available packages: {len(packages)}")

        # Get recommendations
        recommendations = await clinic_api.get_package_recommendations(symptoms)
        print(f"   Generated recommendations: {len(recommendations)}")

        if recommendations:
            top_rec = recommendations[0]
            print("🏆 Top Recommendation:")
            print(f"   Package: {top_rec.get('name', 'N/A')}")
            print(f"   Clinical reasoning: {top_rec.get('_clinical_reasoning', 'N/A')}")
            print(f"   Urgency justification: {top_rec.get('_urgency_justification', 'N/A')}")
            print(f"   Confidence level: {top_rec.get('_confidence_level', 'N/A')}")

        await clinic_api.client.aclose()

    except Exception as e:
        print(f"❌ Integration test error: {e}")

if __name__ == "__main__":
    print("🧪 Starting Clinical Analysis Tests...\n")

    # Run clinical analysis tests
    asyncio.run(test_clinical_analysis())

    # Run integration tests
    asyncio.run(test_integration_with_packages())

    print("✅ All tests completed!")
