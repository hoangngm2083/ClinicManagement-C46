from langchain.tools import tool
from typing import List, Dict, Any, Optional
import logging
from datetime import datetime, timedelta
from ..services.clinic_api import ClinicAPIService
from ..rag.pgvector_store import PGVectorStore

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
        # Search in PGVector store for semantic search
        search_results = vector_store.similarity_search("doctors", query, n_results=5)

        if search_results:
            # Use vector search results
            doctors_info = []
            for (metadata, similarity_score) in search_results:
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
            doctors = await clinic_api.get_doctors(keyword=query)
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
        date: Ngày cần kiểm tra (định dạng YYYY-MM-DD)
        shift: Buổi khám (MORNING hoặc AFTERNOON, mặc định None để lấy cả hai)
        medical_package: Tên gói khám (tùy chọn để filter)

    Returns:
        Danh sách slot trống
    """
    if not clinic_api:
        return "Lỗi: Tools chưa được khởi tạo"

    try:
        # Get all packages first
        packages = await clinic_api.get_medical_packages()

        available_slots = []
        for package in packages:
            # Filter by package name if specified
            if medical_package and medical_package.lower() not in package.get('name', '').lower():
                continue

            try:
                slots = await clinic_api.get_available_slots(package['id'])
                for slot in slots:
                    slot_date = slot.get('date', '')
                    slot_shift = slot.get('shift', '')

                    # Filter by date
                    if str(slot_date) != date:
                        continue

                    # Filter by shift if specified
                    if shift and slot_shift != shift.upper():
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
        morning_slots = [s for s in available_slots if s['shift'] == 'MORNING']
        afternoon_slots = [s for s in available_slots if s['shift'] == 'AFTERNOON']

        result = [f"📅 Slot trống ngày {date}:"]
        result.append("")

        if morning_slots:
            result.append("🌅 Buổi sáng (7:00-11:00):")
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


@tool
async def recommend_medical_packages(symptoms: str) -> str:
    """
    Đề xuất gói khám phù hợp dựa trên triệu chứng của bệnh nhân.
    Sử dụng tool này khi người dùng mô tả triệu chứng và cần tư vấn gói khám.

    Args:
        symptoms: Mô tả triệu chứng của bệnh nhân

    Returns:
        Danh sách gói khám được đề xuất
    """
    if not clinic_api:
        return "Lỗi: Tools chưa được khởi tạo"

    try:
        # Use the API's recommendation method
        recommendations = await clinic_api.get_package_recommendations(symptoms)

        if not recommendations:
            return f"""Không tìm thấy gói khám phù hợp với triệu chứng "{symptoms}".
Vui lòng mô tả chi tiết hơn về triệu chứng hoặc liên hệ trực tiếp với phòng khám để được tư vấn."""

        result = [f"💊 Gói khám đề xuất cho triệu chứng: {symptoms}"]
        result.append("")

        for i, package in enumerate(recommendations, 1):
            result.append(f"{i}. 📦 {package.get('name', 'N/A')}")
            result.append(f"   💰 Giá: {package.get('price', 0):,} VND")
            result.append(f"   📝 Mô tả: {package.get('description', 'Không có mô tả')[:200]}...")
            result.append("")

        result.append("💡 Khuyến nghị: Nên đến khám sớm để được chẩn đoán chính xác.")
        return "\n".join(result)

    except Exception as e:
        logger.error(f"Error in recommend_medical_packages: {e}")
        return f"Lỗi khi đề xuất gói khám: {str(e)}"


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
            return """Không tìm thấy thông tin cụ thể. Đây là một số thông tin chung về phòng khám:

🏥 **Giờ hoạt động:**
- Thứ 2 - Thứ 6: 7:00 - 17:00
- Thứ 7 - Chủ nhật: 7:00 - 12:00

📞 **Liên hệ:**
- Hotline: 1900-xxxx
- Email: info@clinic.com

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
