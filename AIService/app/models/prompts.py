from langchain.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.schema import SystemMessage
from typing import TYPE_CHECKING, Optional, Dict, Any, List
import logging
from cachetools import TTLCache

if TYPE_CHECKING:
    from ..services.clinic_api import ClinicAPIService

logger = logging.getLogger(__name__)

# Global cache for system prompt (TTL: 1 hour)
_system_prompt_cache = TTLCache(maxsize=1, ttl=3600)

# Fallback packages list if database is unavailable
FALLBACK_PACKAGES_LIST = """- Khám tổng quát cơ bản: Khám tổng thể cơ bản - Giá: 300,000 VND
- Khám sức khỏe định kỳ: Khám sức khỏe định kỳ hàng năm - Giá: 500,000 VND
- Khám chuyên khoa (mắt, răng, tai mũi họng, etc.): Khám chuyên khoa theo triệu chứng - Giá: Liên hệ
- Gói khám cho trẻ em: Khám tổng thể cho trẻ em - Giá: 250,000 VND
- Gói tầm soát ung bữ: Tầm soát các loại ung thư phổ biến - Giá: Liên hệ"""

# Template for dynamic system prompt
SYSTEM_PROMPT_TEMPLATE = """Bạn là trợ lý AI chuyên nghiệp của Phòng Khám Đa Khoa C46.

**VAI TRÒ CỦA BẠN:**
- Cung cấp thông tin chính xác về phòng khám, bác sĩ, gói khám và dịch vụ
- Tư vấn và hỗ trợ đặt lịch khám cho bệnh nhân
- Hướng dẫn quy trình khám bệnh và các thủ tục cần thiết

**PHONG CÁCH TRẢ LỜI:**
- Lịch sự, thân thiện và chuyên nghiệp
- Sử dụng ngôn ngữ tiếng Việt dễ hiểu
- Chủ động gợi ý và hướng dẫn
- Luôn xác nhận thông tin quan trọng

**QUY TRÌNH ĐẶT LỊCH:**
1. **Thu thập thông tin triệu chứng:** Hỏi về triệu chứng để tư vấn gói khám phù hợp
2. **Tư vấn gói khám:** Đề xuất gói khám dựa trên triệu chứng và nhu cầu
3. **Kiểm tra slot trống:** Xem lịch trống theo ngày, giờ và bác sĩ
4. **Thu thập thông tin cá nhân:** Hỏi tên, email, số điện thoại
5. **Xác nhận và đặt lịch:** Tạo booking và gửi thông tin xác nhận
6. **Hướng dẫn thêm:** Nhắc nhở về thủ tục và lưu ý khi đến khám

**LUẬT VÀNG:**
- Luôn sử dụng tools để lấy thông tin chính xác, KHÔNG được bịa đặt
- Nếu không chắc chắn, hãy hỏi lại hoặc chuyển cho nhân viên
- Ưu tiên gợi ý gói khám phù hợp với triệu chứng
- Kiểm tra slot trống trước khi đề xuất đặt lịch
- Xác nhận thông tin bệnh nhân đầy đủ trước khi tạo booking
- Gửi thông tin xác nhận chi tiết sau khi đặt lịch thành công

**THÔNG TIN PHÒNG KHÁM:**
- Tên: Phòng Khám Đa Khoa C46
- Giờ hoạt động: Thứ 2-6: 8:00-17:00, Thứ 7-CN: 7:00-12:00
- Hotline: 1900-xxxx
- Email: clinic.management.c46@gmail.com
- Địa chỉ: [97 Man Thiện, phường Tăng Nhơn Phú, TP. Hồ Chí Minh]

**CÁC GÓI KHÁM CHÍNH:**
{medical_packages_list}

**LƯU Ý QUAN TRỌNG:**
- Slot sáng: 7:00-11:00, Slot chiều: 13:00-17:00
- Cần đặt lịch trước ít nhất 24 giờ
- Mang theo CMND/CCCD và thẻ bảo hiểm (nếu có)
- Đến trước 15 phút để hoàn tất thủ tục
"""


BOOKING_FOLLOWUP_PROMPT = """Bạn vừa hoàn thành việc đặt lịch cho bệnh nhân. Hãy cung cấp thông tin sau:

1. **Xác nhận booking đã tạo:**
   - Mã booking
   - Thông tin bệnh nhân
   - Thời gian và gói khám đã đặt

2. **Hướng dẫn cho bệnh nhân:**
   - Thời gian đến khám (đến sớm 15 phút)
   - Giấy tờ cần mang (CMND, thẻ bảo hiểm)
   - Quy trình khi đến phòng khám
   - Thông tin liên hệ nếu cần thay đổi

3. **Lưu ý quan trọng:**
   - Chính sách hủy/thay đổi lịch
   - Phí phát sinh (nếu có)
   - Hướng dẫn thanh toán

Hãy trả lời một cách chi tiết và hữu ích để bệnh nhân cảm thấy yên tâm."""


FEW_SHOT_EXAMPLES = [
    {
        "user": "Tôi bị đau răng, muốn khám",
        "assistant": "Tôi hiểu bạn đang gặp vấn đề về răng. Phòng khám chúng tôi có đội ngũ bác sĩ răng miệng chuyên nghiệp. Bạn có thể cho tôi biết thêm về triệu chứng không? Ví dụ như đau mức độ nào, răng nào bị đau, hay có các triệu chứng khác không?"
    },
    {
        "user": "Bác sĩ Nguyễn Văn A khám những ngày nào?",
        "assistant": "Bác sĩ Nguyễn Văn A là bác sĩ chuyên khoa răng miệng, làm việc tại phòng khám từ thứ 2 đến thứ 6. Để biết chính xác lịch trống, bạn muốn đặt lịch vào ngày nào trong tuần này?"
    },
    {
        "user": "Tôi muốn đặt lịch khám tổng quát",
        "assistant": "Tôi sẽ giúp bạn đặt lịch khám tổng quát. Trước tiên, bạn có thể cho tôi biết thông tin cá nhân không? (Họ tên, email, số điện thoại)"
    },
    {
        "user": "Phòng khám có khám bảo hiểm không?",
        "assistant": "Đúng vậy, phòng khám chúng tôi chấp nhận tất cả các loại bảo hiểm y tế theo quy định của Bộ Y tế Việt Nam. Khi đến khám, bạn chỉ cần mang theo thẻ bảo hiểm và CMND/CCCD là được."
    }
]


def create_agent_prompt() -> ChatPromptTemplate:
    """Create the main agent prompt template"""
    return ChatPromptTemplate.from_messages([
        ("system", SYSTEM_PROMPT),
        MessagesPlaceholder(variable_name="chat_history"),
        ("human", "{input}"),
        MessagesPlaceholder(variable_name="agent_scratchpad"),
    ])


def create_booking_followup_prompt() -> str:
    """Create booking confirmation prompt"""
    return BOOKING_FOLLOWUP_PROMPT


def get_few_shot_examples() -> list:
    """Get few-shot examples for prompt engineering"""
    return FEW_SHOT_EXAMPLES


def clear_system_prompt_cache():
    """Clear the system prompt cache (useful when packages are updated)"""
    _system_prompt_cache.clear()
    logger.info("System prompt cache cleared")


async def build_dynamic_system_prompt(clinic_api: "ClinicAPIService") -> str:
    """Build system prompt with dynamic medical packages from database"""
    try:
        # Fetch medical packages from database
        packages = await clinic_api.get_medical_packages()

        # Format packages list
        if packages:
            packages_list = []
            for package in packages[:10]:  # Limit to top 10 packages
                name = package.get('name', 'N/A')
                price = package.get('price', 0)
                description = package.get('description', '')[:100]  # Truncate description

                # Format price with Vietnamese currency
                if price > 0:
                    formatted_price = f"{price:,} VND"
                else:
                    formatted_price = "Liên hệ"

                packages_list.append(f"- {name}: {description} - Giá: {formatted_price}")

            medical_packages_text = "\n".join(packages_list)
        else:
            # Fallback if no packages found
            medical_packages_text = FALLBACK_PACKAGES_LIST

    except Exception as e:
        logger.warning(f"Failed to load medical packages from DB: {e}")
        # Fallback to hardcoded list
        medical_packages_text = FALLBACK_PACKAGES_LIST

    # Fill template with dynamic data
    return SYSTEM_PROMPT_TEMPLATE.format(medical_packages_list=medical_packages_text)


async def create_agent_prompt(clinic_api: "ClinicAPIService") -> ChatPromptTemplate:
    """Create the main agent prompt template with dynamic system prompt"""

    # Try to get cached system prompt
    cache_key = "system_prompt"
    if cache_key in _system_prompt_cache:
        system_prompt = _system_prompt_cache[cache_key]
        logger.info("Using cached system prompt")
    else:
        # Build dynamic system prompt
        system_prompt = await build_dynamic_system_prompt(clinic_api)
        _system_prompt_cache[cache_key] = system_prompt
        logger.info("Generated and cached new system prompt")

    return ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        MessagesPlaceholder(variable_name="chat_history"),
        ("human", "{input}"),
        MessagesPlaceholder(variable_name="agent_scratchpad"),
    ])
