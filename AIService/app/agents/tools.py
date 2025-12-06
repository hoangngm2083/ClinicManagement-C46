from langchain.tools import tool
from typing import List, Dict, Any, Optional
import logging
from datetime import datetime, timedelta
from ..services.clinic_api import ClinicAPIService
from ..rag.pgvector_store import PGVectorStore
from ..config.settings import settings

logger = logging.getLogger(__name__)

# Global instances (will be initialized in the agent)
clinic_api: Optional[ClinicAPIService] = None
vector_store: Optional[PGVectorStore] = None


def init_tools(clinic_api_instance: ClinicAPIService, vector_store_instance: PGVectorStore):
    """Initialize global tool instances"""
    global clinic_api, vector_store
    clinic_api = clinic_api_instance
    vector_store = vector_store_instance


@tool
async def search_doctor_info(query: str) -> str:
    """
    Tìm kiếm thông tin bác sĩ theo tên, chuyên khoa, hoặc mô tả.
    Sử dụng tool này khi người dùng hỏi về bác sĩ cụ thể hoặc chuyên khoa.

    Args:
        query: Từ khóa tìm kiếm (tên bác sĩ, chuyên khoa, etc.)

    Returns:
        Thông tin chi tiết về bác sĩ phù hợp
    """
    if not clinic_api or not vector_store:
        return "Lỗi: Tools chưa được khởi tạo"

    try:
        # If query is empty or very general, get all doctors from API
        if not query or query.strip() == "" or query.lower() in ['all', 'tất cả', 'tất cả bác sĩ']:
            doctors = await clinic_api.get_doctors(role=0)
            if doctors:
                doctors_info = []
                for doctor in doctors[:10]:  # Limit to 10 doctors
                    doctors_info.append(f"""
                    🔹 Bác sĩ: {doctor.get('name', 'N/A')}
                    📧 Email: {doctor.get('email', 'N/A')}
                    📞 Điện thoại: {doctor.get('phone', 'N/A')}
                    🏥 Chuyên khoa: {doctor.get('departmentName', 'N/A')}
                    📝 Mô tả: {doctor.get('description', 'Không có mô tả')}
                    ✅ Trạng thái: {'Đang hoạt động' if doctor.get('active', True) else 'Tạm nghỉ'}
                    """.strip())
                return "\n\n".join(doctors_info)
            else:
                return "Hiện tại không có thông tin về bác sĩ nào trong phòng khám."

        # Search in PGVector store for semantic search
        search_results = vector_store.similarity_search("doctors", query, n_results=5)

        if search_results:
            # Use vector search results
            doctors_info = []
            for result_tuple in search_results:
                metadata, similarity_score = result_tuple
                doctors_info.append(f"""
                🔹 Bác sĩ: {metadata.get('name', 'N/A')}
                📧 Email: {metadata.get('email', 'N/A')}
                📞 Điện thoại: {metadata.get('phone', 'N/A')}
                🏥 Chuyên khoa: {metadata.get('department', 'N/A')}
                📝 Mô tả: {metadata.get('description', 'Không có mô tả')}
                ✅ Trạng thái: {'Đang hoạt động' if metadata.get('active', True) else 'Tạm nghỉ'}
                🎯 Độ liên quan: {similarity_score:.3f}
                """.strip())

            return "\n\n".join(doctors_info)
        else:
            # Fallback to API search
            doctors = await clinic_api.get_doctors(keyword=query, role=0)
            if not doctors:
                return "Không tìm thấy bác sĩ phù hợp với từ khóa tìm kiếm."

            doctors_info = []
            for doctor in doctors[:5]:
                doctors_info.append(f"""
                🔹 Bác sĩ: {doctor.get('name', 'N/A')}
                📧 Email: {doctor.get('email', 'N/A')}
                📞 Điện thoại: {doctor.get('phone', 'N/A')}
                🏥 Chuyên khoa: {doctor.get('departmentName', 'N/A')}
                📝 Mô tả: {doctor.get('description', 'Không có mô tả')}
                ✅ Trạng thái: {'Đang hoạt động' if doctor.get('active', True) else 'Tạm nghỉ'}
                """.strip())

            return "\n\n".join(doctors_info)

    except Exception as e:
        logger.error(f"Error in search_doctor_info: {e}")
        return f"Lỗi khi tìm kiếm bác sĩ: {str(e)}"


@tool
async def check_available_slots(date: str, shift: Optional[str] = None, medical_package: Optional[str] = None) -> str:
    """
    Kiểm tra các slot khám còn trống trong ngày cụ thể.
    Sử dụng tool này khi người dùng muốn đặt lịch hoặc kiểm tra availability.

    Args:
        date: Ngày cần kiểm tra (có thể là text tiếng Việt hoặc format khác)
        shift: Buổi khám (MORNING hoặc AFTERNOON, mặc định None để lấy cả hai)
        medical_package: Tên gói khám (tùy chọn để filter)

    Returns:
        Danh sách slot trống
    """
    if not clinic_api:
        return "Lỗi: Tools chưa được khởi tạo"

    try:
        # Parse and format date to Java LocalDate format (yyyy-MM-dd)
        formatted_date = _parse_and_format_date(date)
        if not formatted_date:
            return f"Không thể hiểu ngày '{date}'. Vui lòng cung cấp ngày theo định dạng dd/mm/yyyy hoặc mô tả như 'ngày mai', 'thứ 2 tuần sau', etc."
        # Get packages with keyword search (server handles None/empty keyword)
        packages = await clinic_api.get_medical_packages(keyword=medical_package)

        if not packages:
            return f"Không tìm thấy gói khám phù hợp với '{medical_package}'. Vui lòng kiểm tra lại tên gói khám."

        available_slots = []

        # Get slots for the specific date using date range (same date for both from/to)
        for package in packages:
            try:
                slots = await clinic_api.get_available_slots(
                    package['id'],
                    date_from=formatted_date,
                    date_to=formatted_date
                )

                for slot in slots:
                    slot_date = slot.get('date', '')
                    slot_shift = slot.get('shift', '')

                    # Filter by shift if specified (convert to numeric for comparison)
                    shift_numeric = None
                    if shift:
                        if shift.upper() == 'MORNING':
                            shift_numeric = 0
                        elif shift.upper() == 'AFTERNOON':
                            shift_numeric = 1

                    if shift_numeric is not None and slot_shift != shift_numeric:
                        continue

                    # Check if slot has remaining capacity
                    remaining = slot.get('remainingQuantity', 0)
                    if remaining > 0:
                        available_slots.append({
                            'package_name': package.get('name', 'N/A'),
                            'date': slot_date,
                            'shift': slot_shift,
                            'remaining': remaining,
                            'slot_id': slot.get('slotId', ''),
                            'price': package.get('price', 0)
                        })
            except Exception as e:
                logger.warning(f"Error getting slots for package {package['id']}: {e}")
                continue

        if not available_slots:
            return f"Không có slot trống nào vào ngày {date} cho tiêu chí đã chọn."

        # Group by shift
        morning_slots = [s for s in available_slots if s['shift'] == 0]
        afternoon_slots = [s for s in available_slots if s['shift'] == 1]

        result = [f"📅 Slot trống ngày {date}:"]
        result.append("")

        if morning_slots:
            result.append("🌅 Buổi sáng (8:00-12:00):")
            for slot in morning_slots[:5]:  # Limit to 5 per shift
                result.append(f"  • {slot['package_name']} - Còn {slot['remaining']} chỗ - {slot['price']:,} VND")
            result.append("")

        if afternoon_slots:
            result.append("🌇 Buổi chiều (13:00-17:00):")
            for slot in afternoon_slots[:5]:  # Limit to 5 per shift
                result.append(f"  • {slot['package_name']} - Còn {slot['remaining']} chỗ - {slot['price']:,} VND")

        return "\n".join(result)

    except Exception as e:
        logger.error(f"Error in check_available_slots: {e}")
        return f"Lỗi khi kiểm tra slot trống: {str(e)}"


def _parse_and_format_date(date_input: str) -> Optional[str]:
    """
    Parse date from various formats and return Java LocalDate format (yyyy-MM-dd)

    Args:
        date_input: Date in various formats (dd/mm/yyyy, dd-mm-yyyy, Vietnamese text, etc.)

    Returns:
        Date in yyyy-MM-dd format or None if parsing fails
    """
    import re
    from datetime import datetime, timedelta

    try:
        date_input = date_input.strip().lower()

        # Handle Vietnamese date expressions
        today = datetime.now()

        # "ngày mai" -> tomorrow
        if "ngày mai" in date_input or "mai" == date_input:
            target_date = today + timedelta(days=1)
            return target_date.strftime("%Y-%m-%d")

        # "hôm nay" -> today
        if "hôm nay" in date_input or "hôm nay" == date_input or "today" in date_input:
            return today.strftime("%Y-%m-%d")

        # "thứ [number] tuần sau" -> next week weekday
        week_match = re.search(r'thứ\s*(\d+)\s*tuần\s*sau', date_input)
        if week_match:
            weekday = int(week_match.group(1))
            if 2 <= weekday <= 8:  # Monday = 0, Sunday = 6 in Python, but Vietnamese uses 2-8
                viet_to_python_weekday = {2: 0, 3: 1, 4: 2, 5: 3, 6: 4, 7: 5, 8: 6}
                python_weekday = viet_to_python_weekday.get(weekday, 0)

                days_ahead = python_weekday - today.weekday()
                if days_ahead <= 0:
                    days_ahead += 7

                target_date = today + timedelta(days=days_ahead + 7)  # Next week
                return target_date.strftime("%Y-%m-%d")

        # "thứ [number] này" -> this week weekday
        week_this_match = re.search(r'thứ\s*(\d+)\s*này', date_input)
        if week_this_match:
            weekday = int(week_this_match.group(1))
            if 2 <= weekday <= 8:
                viet_to_python_weekday = {2: 0, 3: 1, 4: 2, 5: 3, 6: 4, 7: 5, 8: 6}
                python_weekday = viet_to_python_weekday.get(weekday, 0)

                days_ahead = python_weekday - today.weekday()
                if days_ahead <= 0:
                    days_ahead += 7

                target_date = today + timedelta(days=days_ahead)
                return target_date.strftime("%Y-%m-%d")

        # Direct date formats: dd/mm/yyyy, dd-mm-yyyy, yyyy-mm-dd
        date_patterns = [
            r'(\d{1,2})[/-](\d{1,2})[/-](\d{4})',  # dd/mm/yyyy or dd-mm-yyyy
            r'(\d{4})[/-](\d{1,2})[/-](\d{1,2})',  # yyyy/mm/dd or yyyy-mm-dd
        ]

        for pattern in date_patterns:
            match = re.search(pattern, date_input)
            if match:
                groups = match.groups()
                if len(groups) == 3:
                    # Determine format based on first group length
                    if len(groups[0]) == 4:  # yyyy-mm-dd format
                        year, month, day = int(groups[0]), int(groups[1]), int(groups[2])
                    else:  # dd-mm-yyyy format
                        day, month, year = int(groups[0]), int(groups[1]), int(groups[2])

                    # Validate date
                    try:
                        datetime(year, month, day)
                        return f"{year:04d}-{month:02d}-{day:02d}"
                    except ValueError:
                        continue

        # If no pattern matches, try direct parsing with common formats
        formats_to_try = [
            "%d/%m/%Y", "%d-%m-%Y", "%Y-%m-%d",
            "%d/%m/%y", "%d-%m-%y", "%Y/%m/%d"
        ]

        for fmt in formats_to_try:
            try:
                parsed_date = datetime.strptime(date_input, fmt)
                return parsed_date.strftime("%Y-%m-%d")
            except ValueError:
                continue

        return None

    except Exception as e:
        logger.error(f"Error parsing date '{date_input}': {e}")
        return None


@tool
async def recommend_medical_packages(symptoms: str) -> str:
    """
    Đề xuất gói khám phù hợp dựa trên triệu chứng của bệnh nhân với AI analysis.
    Sử dụng tool này khi người dùng mô tả triệu chứng và cần tư vấn gói khám chi tiết.

    Args:
        symptoms: Mô tả triệu chứng của bệnh nhân

    Returns:
        Danh sách gói khám được đề xuất với phân tích chi tiết
    """
    if not clinic_api:
        return "Lỗi: Tools chưa được khởi tạo"

    try:
        # Use the enhanced API's recommendation method
        recommendations = await clinic_api.get_package_recommendations(symptoms)

        if not recommendations:
            return f"""❌ Không tìm thấy gói khám phù hợp với triệu chứng "{symptoms}".

💡 **Khuyến nghị:**
- Vui lòng mô tả chi tiết hơn về triệu chứng (mức độ đau, thời gian xuất hiện, các triệu chứng kèm theo)
- Hoặc gọi hotline {settings.clinic_hotline} để được tư vấn trực tiếp từ bác sĩ
- Bạn cũng có thể đặt lịch khám tổng quát để được kiểm tra toàn diện"""

        result = [f"🔍 **Phân tích triệu chứng và đề xuất gói khám:**"]
        result.append(f"📋 *Triệu chứng mô tả:* {symptoms}")
        result.append("")

        # Group recommendations by urgency
        high_urgency = [p for p in recommendations if p.get('_urgency') == 'high']
        medium_urgency = [p for p in recommendations if p.get('_urgency') == 'medium']
        low_urgency = [p for p in recommendations if p.get('_urgency') == 'low']

        def format_package(package, index, is_primary=False):
            """Format a single package recommendation"""
            lines = []

            # Package header with priority indicator
            if is_primary:
                lines.append(f"⭐ **{index}. {package.get('name', 'N/A')}** (Đề xuất chính)")
            else:
                lines.append(f"{index}. 📦 {package.get('name', 'N/A')}")

            # Price
            price = package.get('price', 0)
            if price > 0:
                lines.append(f"   💰 **Giá:** {price:,} VND")
            else:
                lines.append("   💰 **Giá:** Liên hệ")

            # Description (truncated)
            desc = package.get('description', 'Không có mô tả')
            if len(desc) > 150:
                desc = desc[:150] + "..."
            lines.append(f"   📝 **Mô tả:** {desc}")

            # Matched symptoms (if available)
            matched = package.get('_matched_symptoms', [])
            if matched:
                lines.append(f"   🎯 **Lý do phù hợp:** {', '.join(matched[:3])}")

            # Urgency indicator
            urgency = package.get('_urgency', 'low')
            if urgency == 'high':
                lines.append("   ⚠️ **Mức độ khẩn cấp:** CAO - Nên khám sớm")
            elif urgency == 'medium':
                lines.append("   🟡 **Mức độ khẩn cấp:** TRUNG BÌNH")
            else:
                lines.append("   🟢 **Mức độ khẩn cấp:** THẤP")

            # Urgent note
            urgent_note = package.get('_urgent_note')
            if urgent_note:
                lines.append(f"   🚨 **Lưu ý quan trọng:** {urgent_note}")

            # Reason for secondary packages
            reason = package.get('_reason')
            if reason:
                lines.append(f"   💡 **Gợi ý bổ sung:** {reason}")

            return "\n".join(lines)

        # Display recommendations with clinical analysis
        result.append("## 📋 **PHÂN TÍCH TRIỆU CHỨNG**\n")

        # Show clinical insights if available
        if recommendations and '_possible_conditions' in recommendations[0]:
            primary_rec = recommendations[0]
            possible_conditions = primary_rec.get('_possible_conditions', [])
            recommended_specialties = primary_rec.get('_recommended_specialties', [])
            red_flags = primary_rec.get('_red_flags', [])
            confidence_level = primary_rec.get('_confidence_level', 'Unknown')

            result.append(f"**🔍 Chuyên khoa gợi ý:** {', '.join(recommended_specialties) if recommended_specialties else 'Tổng quát'}")
            result.append(f"**📊 Độ tin cậy:** {confidence_level}")

            if possible_conditions:
                result.append(f"**🎯 Có thể liên quan đến:** {', '.join(possible_conditions[:3])}")

            if red_flags:
                result.append("")
                result.append("**🚨 CẢNH BÁO QUAN TRỌNG:**")
                for flag in red_flags:
                    result.append(f"• {flag}")
                result.append("")

        result.append("## 💊 **ĐỀ XUẤT GÓI KHÁM**\n")

        # Display recommendations with clinical reasoning
        for i, package in enumerate(recommendations, 1):
            # Enhanced package formatting with clinical info
            clinical_reasoning = package.get('_clinical_reasoning', '')
            urgency_justification = package.get('_urgency_justification', '')
            confidence_level = package.get('_confidence_level', 'Thấp')
            specialty_match = package.get('_specialty_match', False)

            # Package header with clinical indicators
            header_icon = "⭐" if i == 1 else "📦"
            specialty_indicator = "🏥" if specialty_match else ""
            confidence_indicator = "🎯" if confidence_level == "Cao" else "⚡" if confidence_level == "Trung bình" else "❓"

            result.append(f"{i}. {header_icon} {specialty_indicator} {confidence_indicator} **{package.get('name', 'N/A')}**")

            # Price
            price = package.get('price', 0)
            if price > 0:
                result.append(f"   💰 **Giá:** {price:,} VND")
            else:
                result.append("   💰 **Giá:** Liên hệ")

            # Clinical reasoning (most important)
            if clinical_reasoning:
                result.append(f"   🩺 **Lý do đề xuất:** {clinical_reasoning}")

            # Urgency information
            if urgency_justification:
                result.append(f"   ⏰ **Khuyến nghị thời gian:** {urgency_justification}")

            # Description (truncated for clinical focus)
            desc = package.get('description', 'Không có mô tả')
            if len(desc) > 100:
                desc = desc[:100] + "..."
            result.append(f"   📝 **Chi tiết:** {desc}")

            # Confidence level
            result.append(f"   ✅ **Độ tin cậy:** {confidence_level}")

            # Urgent notes
            urgent_note = package.get('_urgent_note')
            if urgent_note:
                result.append(f"   🚨 **LƯU Ý KHẨN CẤP:** {urgent_note}")

            # Red flag notes
            red_flag_notes = package.get('_red_flag_notes')
            if red_flag_notes:
                result.append("   ⚠️ **TRIỆU CHỨNG CẦN CHÚ Ý:**")
                for note in red_flag_notes:
                    result.append(f"      • {note}")

            result.append("")

        # General recommendations
        result.append("💡 **Khuyến nghị chung:**")
        result.append("• Hãy mô tả chi tiết hơn về triệu chứng để có đề xuất chính xác hơn")
        result.append("• Có thể kết hợp nhiều gói khám để kiểm tra toàn diện")
        result.append("• Đến khám sớm giúp phát hiện và điều trị kịp thời")
        result.append("")
        result.append(f"📞 **Cần hỗ trợ thêm?** Gọi hotline {settings.clinic_hotline} hoặc để lại thông tin để chúng tôi liên hệ tư vấn.")

        return "\n".join(result)

    except Exception as e:
        logger.error(f"Error in recommend_medical_packages: {e}")
        return f"Lỗi khi đề xuất gói khám: {str(e)}\n\nVui lòng thử lại hoặc liên hệ hotline để được hỗ trợ."


@tool
async def create_booking(patient_info: str, slot_id: str) -> str:
    """
    Tạo lịch hẹn khám mới cho bệnh nhân.
    Sử dụng tool này sau khi đã xác nhận thông tin bệnh nhân và slot trống.

    Args:
        patient_info: Thông tin bệnh nhân (định dạng: "name:Nguyễn Văn A,email:a@example.com,phone:0123456789")
        slot_id: ID của slot đã chọn

    Returns:
        Kết quả tạo booking
    """
    if not clinic_api:
        return "Lỗi: Tools chưa được khởi tạo"

    try:
        # Parse patient info
        info_parts = patient_info.split(',')
        patient_data = {}
        for part in info_parts:
            if ':' in part:
                key, value = part.strip().split(':', 1)
                patient_data[key.strip()] = value.strip()

        required_fields = ['name', 'email', 'phone']
        missing_fields = [field for field in required_fields if field not in patient_data]

        if missing_fields:
            return f"Thiếu thông tin bắt buộc: {', '.join(missing_fields)}. Vui lòng cung cấp đầy đủ."

        # Generate fingerprint for booking
        import uuid
        fingerprint = str(uuid.uuid4())

        booking_id = await clinic_api.create_booking(
            slot_id=slot_id,
            name=patient_data['name'],
            email=patient_data['email'],
            phone=patient_data['phone'],
            fingerprint=fingerprint
        )

        return f"""✅ Đặt lịch thành công!

🎫 Mã booking: {booking_id}
👤 Bệnh nhân: {patient_data['name']}
📧 Email: {patient_data['email']}
📞 Điện thoại: {patient_data['phone']}

📩 Bạn sẽ nhận được email xác nhận trong giây lát với thông tin chi tiết về lịch hẹn.
💡 Vui lòng đến trước 15 phút để hoàn tất thủ tục.

Nếu cần thay đổi lịch hẹn, vui lòng liên hệ hotline hoặc gửi email."""

    except Exception as e:
        logger.error(f"Error in create_booking: {e}")
        return f"Lỗi khi tạo lịch hẹn: {str(e)}. Vui lòng thử lại hoặc liên hệ hotline."


@tool
async def get_clinic_info(query: str) -> str:
    """
    Truy vấn thông tin chung về phòng khám từ knowledge base.
    Sử dụng tool này cho câu hỏi về giờ mở cửa, quy trình, chính sách, etc.

    Args:
        query: Câu hỏi về thông tin phòng khám

    Returns:
        Thông tin liên quan từ knowledge base
    """
    if not vector_store:
        return "Lỗi: Tools chưa được khởi tạo"

    try:
        # Search across different collections
        process_results = vector_store.similarity_search("clinic_processes", query, n_results=2)
        faq_results = vector_store.similarity_search("faq", query, n_results=2)

        results = []

        # Add process results
        for (metadata, similarity_score) in process_results:
            results.append(f"📋 {metadata.get('title', 'Thông tin quy trình')}:\n{metadata.get('content', '').strip()}")

        # Add FAQ results
        for (metadata, similarity_score) in faq_results:
            question = metadata.get('question', '')
            answer = metadata.get('answer', '')
            results.append(f"❓ {question}\n💡 {answer}")

        if not results:
            return f"""Không tìm thấy thông tin cụ thể. Đây là một số thông tin chung về phòng khám:

🏥 **Giờ hoạt động:**
{settings.clinic_working_hours}

📞 **Liên hệ:**
- Hotline: {settings.clinic_hotline}
- Email: {settings.clinic_email}

💡 Để được hỗ trợ chi tiết hơn, vui lòng mô tả cụ thể câu hỏi của bạn."""

        return "\n\n".join(results)

    except Exception as e:
        logger.error(f"Error in get_clinic_info: {e}")
        return f"Lỗi khi truy vấn thông tin: {str(e)}"


@tool
async def get_doctor_schedule(doctor_name: Optional[str] = None, month: Optional[int] = None, year: Optional[int] = None) -> str:
    """
    Lấy lịch làm việc của bác sĩ theo tháng.
    Sử dụng tool này khi người dùng hỏi về lịch làm việc của bác sĩ cụ thể.

    Args:
        doctor_name: Tên bác sĩ (tùy chọn)
        month: Tháng (1-12, mặc định là tháng hiện tại)
        year: Năm (mặc định là năm hiện tại)

    Returns:
        Lịch làm việc của bác sĩ
    """
    if not clinic_api:
        return "Lỗi: Tools chưa được khởi tạo"

    try:
        from datetime import datetime

        now = datetime.now()
        target_month = month or now.month
        target_year = year or now.year

        if not (1 <= target_month <= 12):
            return "Tháng phải nằm trong khoảng 1-12."

        if not (2000 <= target_year <= 2100):
            return f"Năm {target_year} không hợp lệ."

        # Get schedule for the month
        schedule_data = await clinic_api.get_doctor_schedule(target_month, target_year)

        if not schedule_data:
            return f"Không có thông tin lịch làm việc cho tháng {target_month}/{target_year}."

        # Filter by doctor name if specified
        if doctor_name:
            filtered_schedule = [
                doc for doc in schedule_data
                if doctor_name.lower() in doc.get('name', '').lower()
            ]
            if not filtered_schedule:
                return f"Không tìm thấy bác sĩ có tên '{doctor_name}' trong tháng {target_month}/{target_year}."
            schedule_data = filtered_schedule

        result = [f"📅 Lịch làm việc tháng {target_month}/{target_year}:"]
        result.append("")

        for doctor in schedule_data[:10]:  # Limit to 10 doctors
            result.append(f"👨‍⚕️ Bác sĩ: {doctor.get('name', 'N/A')}")
            result.append(f"🏥 Khoa: {doctor.get('departmentName', 'N/A')}")

            # Note: Schedule details would need to be expanded based on actual API response
            result.append("📋 Lịch: Thứ 2 - Thứ 6 (7:00-17:00), Thứ 7 (7:00-12:00)")
            result.append("")

        return "\n".join(result)

    except Exception as e:
        logger.error(f"Error in get_doctor_schedule: {e}")
        return f"Lỗi khi lấy lịch làm việc: {str(e)}"


@tool
async def find_earliest_available_slot(medical_package: Optional[str] = None, max_days_ahead: int = 7) -> str:
    """
    Tìm slot khám sớm nhất có thể cho gói khám cụ thể.
    Tool này sẽ tự động kiểm tra từ ngày mai trở đi, ưu tiên buổi sáng trước, sau đó buổi chiều.
    Sử dụng tool này khi người dùng muốn khám "càng sớm càng tốt" hoặc "sớm nhất có thể".

    Args:
        medical_package: Tên gói khám (tùy chọn, nếu không có sẽ tìm cho tất cả gói)
        max_days_ahead: Số ngày tối đa để tìm kiếm (mặc định 7 ngày)

    Returns:
        Thông tin về slot sớm nhất tìm được
    """
    if not clinic_api:
        return "Lỗi: Tools chưa được khởi tạo"
    
    try:
        from datetime import datetime, timedelta

        # Lấy gói khám với keyword search nếu có
        packages = await clinic_api.get_medical_packages(keyword=medical_package)

        if not packages:
            return f"Không tìm thấy gói khám phù hợp với '{medical_package}'. Vui lòng kiểm tra lại tên gói khám."

        # Tìm gói khám chính xác nhất dựa trên tên (case-insensitive match)
        target_package = None
        medical_package_lower = medical_package.lower().strip()

        # First, try exact match
        for package in packages:
            if package.get('name', '').lower().strip() == medical_package_lower:
                target_package = package
                break

        # If no exact match, try partial match
        if not target_package:
            for package in packages:
                package_name_lower = package.get('name', '').lower().strip()
                if medical_package_lower in package_name_lower or package_name_lower in medical_package_lower:
                    target_package = package
                    break

        # If still no match, use the first package as fallback
        if not target_package:
            target_package = packages[0]
            logger.warning(f"No exact match found for '{medical_package}', using first available package: {target_package.get('name')}")

        # Bắt đầu từ ngày mai (vì cần đặt trước 24h)
        current_date = datetime.now().date()
        earliest_slot = None
        earliest_date = None

        # Calculate date range and get all slots in one go
        date_from = current_date  # Start from today
        date_to = current_date + timedelta(days=max_days_ahead)

        date_from_str = date_from.strftime("%Y-%m-%d")
        date_to_str = date_to.strftime("%Y-%m-%d")

        # Chỉ tìm slot cho gói khám được chọn
        for shift in [0, 1]:
            try:
                slots = await clinic_api.get_available_slots(
                    target_package['id'],
                    date_from=date_from_str,
                    date_to=date_to_str
                )

                for slot in slots:
                    slot_date = slot.get('date', '')
                    slot_shift = slot.get('shift', '')

                    # Parse date if it's a string
                    if isinstance(slot_date, str):
                        try:
                            slot_date_obj = datetime.fromisoformat(slot_date).date()
                            slot_date_str = slot_date_obj.strftime("%Y-%m-%d")
                        except:
                            slot_date_str = str(slot_date)
                    else:
                        slot_date_str = str(slot_date)

                    if slot_shift == shift:
                        remaining = slot.get('remainingQuantity', 0)
                        if remaining > 0:
                            slot_date_obj = datetime.strptime(slot_date_str, "%Y-%m-%d").date()
                            if not earliest_slot or slot_date_obj < earliest_date:
                                earliest_slot = {
                                    'package_name': target_package.get('name', 'N/A'),
                                    'date': slot_date_str,
                                    'shift': slot_shift,
                                    'remaining': remaining,
                                    'slot_id': slot.get('slotId', ''),
                                    'price': target_package.get('price', 0)
                                }
                                earliest_date = slot_date_obj

            except Exception as e:
                logger.warning(f"Error getting slots for package {target_package['id']}: {e}")
                continue

            if earliest_slot:
                break
        
        if not earliest_slot:
            return f"Không tìm thấy slot trống trong {max_days_ahead} ngày tới cho gói khám đã chọn. Vui lòng thử lại sau hoặc liên hệ hotline {settings.clinic_hotline} để được hỗ trợ."
        
        # Format kết quả
        shift_name = "🌅 Buổi sáng (8:00-12:00)" if earliest_slot['shift'] == 0 else "🌇 Buổi chiều (13:00-17:00)"
        date_formatted = earliest_date.strftime("%d/%m/%Y")
        
        result = f"""✅ Tìm thấy slot sớm nhất:
        
📅 Ngày: {date_formatted}
⏰ {shift_name}
📦 Gói khám: {earliest_slot['package_name']}
💰 Giá: {earliest_slot['price']:,} VND
🎫 Còn {earliest_slot['remaining']} chỗ trống

Bạn có muốn đặt lịch cho slot này không? Nếu có, vui lòng cung cấp thông tin:
- Họ tên
- Email
- Số điện thoại"""
        
        return result
    
    except Exception as e:
        logger.error(f"Error in find_earliest_available_slot: {e}", exc_info=True)
        return f"Lỗi khi tìm slot sớm nhất: {str(e)}"


@tool
async def list_all_available_slots(medical_package: Optional[str] = None, days_ahead: int = 7, time_period: Optional[str] = None) -> str:
    """
    Liệt kê tất cả slot khám còn trống trong khoảng thời gian chỉ định.
    Sử dụng tool này khi người dùng muốn xem danh sách đầy đủ các slot trống.

    Args:
        medical_package: Tên gói khám (tùy chọn để filter)
        days_ahead: Số ngày muốn kiểm tra (mặc định 7 ngày)
        time_period: Khoảng thời gian đặc biệt ("this_week", "next_week", etc.) - sẽ override days_ahead

    Returns:
        Danh sách tất cả slot trống theo thứ tự thời gian
    """
    if not clinic_api:
        return "Lỗi: Tools chưa được khởi tạo"

    try:
        # Get packages with keyword search (server handles None/empty keyword)
        packages = await clinic_api.get_medical_packages(keyword=medical_package)

        if not packages:
            return f"Không tìm thấy gói khám phù hợp với '{medical_package}'. Vui lòng kiểm tra lại tên gói khám."

        # Calculate days_ahead based on time_period
        if time_period == "this_week":
            # Tính số ngày từ hôm nay đến cuối tuần (bao gồm hôm nay)
            current_date = datetime.now().date()
            # weekday() returns 0=Monday, 6=Sunday
            days_to_end_of_week = 6 - current_date.weekday()
            days_ahead = days_to_end_of_week + 1  # +1 để bao gồm cả hôm nay

        # Calculate date range (từ hôm nay đến days_ahead ngày sau)
        current_date = datetime.now().date()
        date_from = current_date  # Hôm nay
        date_to = current_date + timedelta(days=days_ahead)

        date_from_str = date_from.strftime("%Y-%m-%d")
        date_to_str = date_to.strftime("%Y-%m-%d")

        all_available_slots = []

        # Get slots for all matching packages in the date range
        for package in packages:
            try:
                slots = await clinic_api.get_available_slots(
                    package['id'],
                    date_from=date_from_str,
                    date_to=date_to_str
                )

                for slot in slots:
                    slot_date = slot.get('date', '')
                    slot_shift = slot.get('shift', '')

                    # Parse date if it's a string
                    if isinstance(slot_date, str):
                        try:
                            slot_date_obj = datetime.fromisoformat(slot_date).date()
                            slot_date_str = slot_date_obj.strftime("%Y-%m-%d")
                        except:
                            slot_date_str = str(slot_date)
                    else:
                        slot_date_str = str(slot_date)

                    # Check if slot has remaining capacity and is in valid shift
                    remaining = slot.get('remainingQuantity', 0)
                    if remaining > 0 and slot_shift in [0, 1]:
                        all_available_slots.append({
                            'package_name': package.get('name', 'N/A'),
                            'date': slot_date_str,
                            'shift': slot_shift,
                            'remaining': remaining,
                            'slot_id': slot.get('slotId', ''),
                            'price': package.get('price', 0)
                        })

            except Exception as e:
                logger.warning(f"Error getting slots for package {package['id']}: {e}")
                continue

        if not all_available_slots:
            return f"Không tìm thấy slot trống nào trong {days_ahead} ngày tới cho gói khám đã chọn. Vui lòng thử lại sau hoặc liên hệ hotline {settings.clinic_hotline} để được hỗ trợ."

        # Sort by date and shift (sáng trước chiều)
        all_available_slots.sort(key=lambda x: (x['date'], x['shift']))

        # Format kết quả
        result = f"📅 **Danh sách slot trống trong {days_ahead} ngày tới:**\n\n"

        current_date = None
        for slot in all_available_slots:
            if current_date != slot['date']:
                current_date = slot['date']
                date_obj = datetime.strptime(slot['date'], "%Y-%m-%d")
                result += f"🗓️ **{date_obj.strftime('%d/%m/%Y')}**:\n"

            shift_name = "🌅 Sáng (8:00-12:00)" if slot['shift'] == 0 else "🌇 Chiều (13:00-17:00)"
            result += f"  • {shift_name} - {slot['package_name']} - Còn {slot['remaining']} chỗ - {slot['price']:,} VND\n"

        result += f"\n💡 Tổng cộng: {len(all_available_slots)} slot trống\n"
        result += "Để đặt lịch, vui lòng chọn slot cụ thể và cung cấp thông tin cá nhân."

        return result

    except Exception as e:
        logger.error(f"Error in list_all_available_slots: {e}", exc_info=True)
        return f"Lỗi khi liệt kê slot trống: {str(e)}"


@tool
async def get_department_info(department_name: Optional[str] = None) -> str:
    """
    Tư vấn thông tin về phòng ban/khoa của phòng khám.
    Sử dụng tool này khi người dùng hỏi về phòng ban, khoa khám, hoặc chuyên khoa của phòng khám.

    Args:
        department_name: Tên phòng ban cụ thể (tùy chọn, nếu không có sẽ liệt kê tất cả)

    Returns:
        Thông tin chi tiết về phòng ban/khoa
    """
    if not clinic_api or not vector_store:
        return "Lỗi: Tools chưa được khởi tạo"

    try:
        # Get departments from API
        departments = await clinic_api.get_departments()

        if not departments:
            return "Hiện tại chưa có thông tin về phòng ban của phòng khám."

        if department_name:
            # Search for specific department
            target_dept = None
            for dept in departments:
                if department_name.lower() in dept.get('name', '').lower():
                    target_dept = dept
                    break

            if not target_dept:
                return f"Không tìm thấy phòng ban có tên '{department_name}'. Vui lòng kiểm tra lại tên phòng ban."

            # Get doctors in this department
            doctors = await clinic_api.get_doctors(department_id=target_dept.get('id'))

            result = [f"🏥 **THÔNG TIN PHÒNG BAN: {target_dept.get('name', 'N/A')}**\n"]

            # Department info
            result.append(f"📋 **Mô tả:** {target_dept.get('description', 'Chưa có mô tả chi tiết')}")
            result.append(f"👨‍⚕️ **Số bác sĩ:** {len(doctors) if doctors else 0}")
            result.append("")

            # List doctors if available
            if doctors:
                result.append("👨‍⚕️ **BÁC SĨ TRONG KHOA:**\n")
                for i, doctor in enumerate(doctors[:5], 1):  # Limit to 5 doctors
                    result.append(f"{i}. 🔹 {doctor.get('name', 'N/A')}")
                    result.append(f"   📧 {doctor.get('email', 'N/A')}")
                    result.append(f"   📞 {doctor.get('phone', 'N/A')}")
                    result.append(f"   📝 {doctor.get('description', 'Không có mô tả')[:100]}...")
                    result.append("")
            else:
                result.append("Hiện tại chưa có thông tin bác sĩ trong khoa này.")

            result.append("💡 **Khuyến nghị:**")
            result.append("- Nếu bạn có triệu chứng liên quan, hãy mô tả để được tư vấn gói khám phù hợp")
            result.append("- Có thể đặt lịch khám trực tiếp với bác sĩ trong khoa")

            return "\n".join(result)
        else:
            # List all departments
            result = [f"🏥 **DANH SÁCH PHÒNG BAN/KHOA CỦA PHÒNG KHÁM**\n"]
            result.append(f"Chúng tôi có {len(departments)} phòng ban chuyên khoa:\n")

            for i, dept in enumerate(departments, 1):
                dept_name = dept.get('name', 'N/A')
                dept_desc = dept.get('description', 'Chưa có mô tả')[:150]
                if len(dept_desc) == 150:
                    dept_desc += "..."

                result.append(f"{i}. 🏥 **{dept_name}**")
                result.append(f"   📋 {dept_desc}")
                result.append("")

            result.append("💡 **Hướng dẫn:**")
            result.append("- Hãy cho tôi biết bạn quan tâm đến khoa nào")
            result.append("- Hoặc mô tả triệu chứng để tôi tư vấn khoa phù hợp")
            result.append("- Bạn cũng có thể hỏi về bác sĩ trong từng khoa")

            return "\n".join(result)

    except Exception as e:
        logger.error(f"Error in get_department_info: {e}")
        return f"Lỗi khi lấy thông tin phòng ban: {str(e)}"


@tool
async def list_medical_packages(keyword: Optional[str] = None) -> str:
    """
    Liệt kê chi tiết các gói khám có sẵn với thông tin giá và dịch vụ.
    Sử dụng tool này khi người dùng muốn xem các gói khám tổng quát hoặc không có triệu chứng cụ thể.

    Args:
        keyword: Từ khóa tìm kiếm gói khám (tùy chọn, để filter)

    Returns:
        Danh sách chi tiết các gói khám có sẵn
    """
    if not clinic_api:
        return "Lỗi: Tools chưa được khởi tạo"

    try:
        # Get packages with keyword search if provided
        packages = await clinic_api.get_medical_packages(keyword=keyword)

        if not packages:
            return f"❌ Không tìm thấy gói khám phù hợp với từ khóa '{keyword}'.\n\n💡 **Khuyến nghị:**\n- Vui lòng kiểm tra lại tên gói khám\n- Hoặc liên hệ hotline {settings.clinic_hotline} để được tư vấn"

        result = [f"📋 **DANH SÁCH GÓI KHÁM CÓ SẴN**\n"]
        result.append(f"Chúng tôi có {len(packages)} gói khám phù hợp:\n")

        for i, package in enumerate(packages, 1):
            # Package name
            name = package.get('name', 'N/A')
            result.append(f"{i}. 📦 **{name}**")

            # Price
            price = package.get('price', 0)
            if price > 0:
                result.append(f"   💰 **Giá:** {price:,} VND")
            else:
                result.append("   💰 **Giá:** Liên hệ")

            # Description (truncated if too long)
            description = package.get('description', 'Không có mô tả chi tiết')
            if len(description) > 200:
                description = description[:200] + "..."
            result.append(f"   📝 **Dịch vụ bao gồm:** {description}")

            result.append("")  # Empty line between packages

        result.append("💡 **Hướng dẫn tiếp theo:**")
        result.append("• Hãy cho tôi biết bạn muốn đăng ký gói nào")
        result.append("• Hoặc mô tả triệu chứng cụ thể để tôi tư vấn gói phù hợp hơn")
        result.append("• Bạn cũng có thể hỏi về slot trống cho gói đã chọn")
        result.append("")
        result.append(f"📞 **Cần hỗ trợ?** Gọi hotline {settings.clinic_hotline}")

        return "\n".join(result)

    except Exception as e:
        logger.error(f"Error in list_medical_packages: {e}")
        return f"Lỗi khi lấy danh sách gói khám: {str(e)}\n\nVui lòng thử lại hoặc liên hệ hotline để được hỗ trợ."
