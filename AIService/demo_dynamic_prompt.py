#!/usr/bin/env python3
"""
Demo script to test dynamic system prompt functionality
"""
import asyncio
import os
import sys
from pathlib import Path

# Add the app directory to the Python path
sys.path.insert(0, str(Path(__file__).parent / "app"))

from app.services.clinic_api import ClinicAPIService
from app.models.prompts import build_dynamic_system_prompt, create_agent_prompt
from app.config.settings import settings


async def demo_dynamic_prompt():
    """Demo dynamic prompt generation"""

    print("🚀 Demo: Dynamic System Prompt với dữ liệu từ Database")
    print("=" * 60)

    # Initialize clinic API (mock data for demo)
    clinic_api = ClinicAPIService()

    try:
        # Build dynamic system prompt
        print("📝 Đang tạo system prompt động...")
        system_prompt = await build_dynamic_system_prompt(clinic_api)
        print("✅ System prompt đã được tạo thành công!")
        print()

        # Show the dynamic packages section
        print("📦 Phần 'CÁC GÓI KHÁM CHÍNH' trong system prompt:")
        print("-" * 50)

        # Extract the packages section
        start_marker = "**CÁC GÓI KHÁM CHÍNH:**"
        end_marker = "**LƯU Ý QUAN TRỌNG:**"

        start_idx = system_prompt.find(start_marker)
        end_idx = system_prompt.find(end_marker)

        if start_idx != -1 and end_idx != -1:
            packages_section = system_prompt[start_idx:end_idx].strip()
            print(packages_section)
        else:
            print("❌ Không tìm thấy phần packages trong prompt")

        print()
        print("🎯 Kết quả:")
        print("- System prompt được tạo động từ database")
        print("- Danh sách gói khám được cập nhật real-time")
        print("- Có fallback khi database không khả dụng")
        print("- Cache 1 giờ để tối ưu performance")

    except Exception as e:
        print(f"❌ Lỗi khi tạo dynamic prompt: {e}")

    finally:
        if hasattr(clinic_api, '_client') and clinic_api._client:
            await clinic_api._client.aclose()


async def demo_agent_prompt():
    """Demo agent prompt creation"""

    print("\n🤖 Demo: Agent Prompt Creation")
    print("=" * 40)

    clinic_api = ClinicAPIService()

    try:
        # Create agent prompt
        print("🔧 Đang tạo agent prompt...")
        prompt_template = await create_agent_prompt(clinic_api)
        print("✅ Agent prompt đã được tạo!")

        # Show prompt structure
        print("📋 Cấu trúc prompt:")
        print(f"- System message: {len(prompt_template.messages[0].content)} ký tự")
        print(f"- Chat history placeholder: {prompt_template.messages[1].variable_name}")
        print(f"- Human input placeholder: {prompt_template.input_variables}")
        print(f"- Agent scratchpad: {prompt_template.messages[3].variable_name}")

    except Exception as e:
        print(f"❌ Lỗi khi tạo agent prompt: {e}")

    finally:
        if hasattr(clinic_api, '_client') and clinic_api._client:
            await clinic_api._client.aclose()


if __name__ == "__main__":
    print("AI Service - Dynamic Prompt Demo")
    print("=================================")

    # Run demos
    asyncio.run(demo_dynamic_prompt())
    asyncio.run(demo_agent_prompt())

    print("\n🎉 Demo hoàn thành!")
    print("\n💡 Để chạy AI Service với dynamic prompt:")
    print("   1. Đảm bảo microservices đang chạy")
    print("   2. Set environment variables trong .env")
    print("   3. Chạy: python run.py")
    print("   4. Test: curl http://localhost:8000/chat -d '{\"message\":\"Hello\"}'")