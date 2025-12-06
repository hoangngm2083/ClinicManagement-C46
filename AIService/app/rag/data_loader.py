from typing import List, Dict, Any
import logging
from ..services.clinic_api import ClinicAPIService
from ..config.settings import settings
from .pgvector_store import PGVectorStore

logger = logging.getLogger(__name__)


class DataLoader:
    def __init__(self, clinic_api: ClinicAPIService, vector_store: PGVectorStore):
        self.clinic_api = clinic_api
        self.vector_store = vector_store

    async def load_all_doctors(self) -> List[Dict[str, Any]]:
        """Load all doctors from StaffService"""
        try:
            logger.info("Loading doctors data...")
            doctors = await self.clinic_api.get_doctors(role=0)

            # Prepare data for PGVector
            ids = []
            documents = []
            metadatas = []

            for doctor in doctors:
                doc_id = f"doctor_{doctor['id']}"
                content = f"""
                Bác sĩ {doctor.get('name', '')}.
                Email: {doctor.get('email', '')}.
                Điện thoại: {doctor.get('phone', '')}.
                Chuyên khoa: {doctor.get('departmentName', 'Chưa xác định')}.
                Mô tả: {doctor.get('description', 'Không có mô tả')}.
                Trạng thái: {'Đang hoạt động' if doctor.get('active', True) else 'Tạm nghỉ'}.
                """

                ids.append(doc_id)
                documents.append(content.strip())
                metadatas.append({
                    "type": "doctor",
                    "id": doctor["id"],
                    "name": doctor.get("name", ""),
                    "department": doctor.get("departmentName", ""),
                    "email": doctor.get("email", ""),
                    "phone": doctor.get("phone", ""),
                    "active": doctor.get("active", True)
                })

            if ids:
                await self.vector_store.add_documents("doctors", documents, metadatas, ids)

            logger.info(f"Loaded {len(doctors)} doctors")
            return doctors
        except Exception as e:
            logger.warning(f"Could not load doctors from API: {e}")
            # Return empty list instead of raising exception
            return []

    async def load_all_packages(self) -> List[Dict[str, Any]]:
        """Load all medical packages from MedicalPackageService"""
        try:
            logger.info("Loading medical packages data...")
            packages = await self.clinic_api.get_medical_packages()

            # Prepare data for PGVector
            ids = []
            documents = []
            metadatas = []

            for package in packages:
                doc_id = f"package_{package['id']}"
                content = f"""
                Gói khám: {package.get('name', '')}.
                Mô tả: {package.get('description', 'Không có mô tả')}.
                Giá: {package.get('price', 0)} VND.
                """

                ids.append(doc_id)
                documents.append(content.strip())
                metadatas.append({
                    "type": "medical_package",
                    "id": package["id"],
                    "name": package.get("name", ""),
                    "price": package.get("price", 0),
                    "description": package.get("description", "")
                })

            if ids:
                await self.vector_store.add_documents("medical_packages", documents, metadatas, ids)

            logger.info(f"Loaded {len(packages)} packages")
            return packages
        except Exception as e:
            logger.warning(f"Could not load packages from API: {e}")
            # Return empty list instead of raising exception
            return []

    async def load_clinic_processes(self) -> List[Dict[str, Any]]:
        """Load clinic processes (static data)"""
        try:
            logger.info("Loading clinic processes...")
            processes = [
                {
                    "id": "general_exam_process",
                    "title": "Quy trình khám tổng quát",
                    "content": """
                    Quy trình khám tổng quát tại phòng khám bao gồm:
                    1. Đăng ký và tiếp đón: Khách hàng đến quầy lễ tân đăng ký thông tin cá nhân
                    2. Khám lâm sàng: Bác sĩ hỏi bệnh sử và khám tổng thể
                    3. Chỉ định xét nghiệm: Nếu cần, bác sĩ sẽ chỉ định các xét nghiệm cần thiết
                    4. Tư vấn và kê đơn: Bác sĩ tư vấn kết quả và đưa ra phác đồ điều trị
                    5. Thanh toán và hẹn tái khám: Hoàn tất thủ tục và hướng dẫn tái khám nếu cần
                    """
                },
                {
                    "id": "booking_process",
                    "title": "Cách đặt lịch khám",
                    "content": """
                    Cách đặt lịch khám tại phòng khám:
                    1. Chọn gói khám phù hợp với triệu chứng
                    2. Kiểm tra lịch trống của bác sĩ
                    3. Điền thông tin cá nhân đầy đủ
                    4. Xác nhận đặt lịch và nhận mã booking
                    5. Nhận email xác nhận và hướng dẫn đến khám
                    """
                },
                {
                    "id": "emergency_process",
                    "title": "Quy trình xử lý cấp cứu",
                    "content": f"""
                    Trong trường hợp cấp cứu:
                    1. Gọi ngay hotline: {settings.clinic_hotline} hoặc đến trực tiếp phòng khám
                    2. Nhân viên y tế sẽ tiếp đón và đánh giá mức độ khẩn cấp
                    3. Ưu tiên khám và điều trị ngay lập tức
                    4. Thông báo cho người nhà về tình trạng bệnh nhân
                    """
                },
                {
                    "id": "payment_process",
                    "title": "Quy trình thanh toán",
                    "content": """
                    Quy trình thanh toán tại phòng khám:
                    1. Thanh toán trước khi khám (đối với dịch vụ đặt lịch)
                    2. Thanh toán tại quầy thu ngân sau khi khám (đối với khách vãng lai)
                    3. Chấp nhận thanh toán bằng tiền mặt, thẻ tín dụng, chuyển khoản
                    4. Cung cấp hóa đơn điện tử sau thanh toán
                    """
                }
            ]

            # Prepare data for PGVector
            ids = []
            documents = []
            metadatas = []

            for process in processes:
                doc_id = f"process_{process['id']}"
                content = process.get('content', '')

                ids.append(doc_id)
                documents.append(content)
                metadatas.append({
                    "type": "process",
                    "id": process["id"],
                    "title": process.get("title", "")
                })

            if ids:
                await self.vector_store.add_documents("clinic_processes", documents, metadatas, ids)

            logger.info(f"Loaded {len(processes)} processes")
            return processes
        except Exception as e:
            logger.error(f"Error loading processes: {e}")
            raise

    async def load_faq_data(self) -> List[Dict[str, Any]]:
        """Load FAQ data (static data)"""
        try:
            logger.info("Loading FAQ data...")
            faqs = [
                {
                    "id": "working_hours",
                    "question": "Phòng khám mở cửa từ mấy giờ?",
                    "answer": "Phòng khám mở cửa từ 7:00 - 17:00 các ngày trong tuần từ thứ 2 đến thứ 6. Thứ 7 và Chủ nhật mở cửa từ 7:00 - 12:00.",
                    "category": "general"
                },
                {
                    "id": "booking_cancellation",
                    "question": "Làm thế nào để hủy lịch khám?",
                    "answer": "Để hủy lịch khám, vui lòng liên hệ hotline hoặc gửi email ít nhất 24 giờ trước giờ hẹn. Phí hủy lịch sẽ được áp dụng nếu hủy trong vòng 24 giờ.",
                    "category": "booking"
                },
                {
                    "id": "insurance",
                    "question": "Phòng khám có chấp nhận bảo hiểm không?",
                    "answer": "Phòng khám chấp nhận các loại bảo hiểm y tế theo quy định của Bộ Y tế. Vui lòng mang thẻ bảo hiểm và CMND/CCCD khi đến khám.",
                    "category": "payment"
                },
                {
                    "id": "preparation",
                    "question": "Cần chuẩn bị gì trước khi đến khám?",
                    "answer": "Vui lòng mang theo CMND/CCCD, thẻ bảo hiểm (nếu có), và các kết quả xét nghiệm cũ. Nên nhịn ăn 8-12 tiếng nếu có chỉ định xét nghiệm máu.",
                    "category": "preparation"
                },
                {
                    "id": "test_results",
                    "question": "Khi nào có kết quả xét nghiệm?",
                    "answer": "Kết quả xét nghiệm thường có trong 24-48 giờ. Một số xét nghiệm đặc biệt có thể mất 3-7 ngày. Chúng tôi sẽ thông báo qua SMS hoặc email.",
                    "category": "results"
                },
                {
                    "id": "children_exam",
                    "question": "Khám cho trẻ em như thế nào?",
                    "answer": "Phòng khám có khu vực riêng dành cho trẻ em với không gian vui chơi. Bác sĩ nhi khoa chuyên nghiệp sẽ thăm khám và tư vấn cho trẻ.",
                    "category": "specialties"
                }
            ]

            # Prepare data for PGVector
            ids = []
            documents = []
            metadatas = []

            for faq in faqs:
                doc_id = f"faq_{faq['id']}"
                content = f"""
                Câu hỏi: {faq.get('question', '')}
                Trả lời: {faq.get('answer', '')}
                """

                ids.append(doc_id)
                documents.append(content.strip())
                metadatas.append({
                    "type": "faq",
                    "id": faq["id"],
                    "question": faq.get("question", ""),
                    "category": faq.get("category", "")
                })

            if ids:
                await self.vector_store.add_documents("faq", documents, metadatas, ids)

            logger.info(f"Loaded {len(faqs)} FAQs")
            return faqs
        except Exception as e:
            logger.error(f"Error loading FAQs: {e}")
            raise

    async def load_initial_data(self):
        """Load all initial data for the AI service"""
        try:
            logger.info("Starting initial data load...")

            # Don't use async with - clinic_api is a long-lived shared instance
            await self.load_all_doctors()
            await self.load_all_packages()
            await self.load_clinic_processes()
            await self.load_faq_data()

            logger.info("Initial data load completed successfully")
        except Exception as e:
            logger.error(f"Error in initial data load: {e}")
            # Don't raise exception - allow service to start with empty data

    async def sync_doctors(self):
        """Sync doctor data periodically"""
        try:
            logger.info("Syncing doctor data...")
            # Don't use async with - clinic_api is a long-lived shared instance
            doctors = await self.clinic_api.get_doctors(role=0)

            # Delete existing and reload
            self.vector_store.delete_collection("doctors")
            await self.load_all_doctors()
            logger.info("Doctor data synced")
        except Exception as e:
            logger.error(f"Error syncing doctors: {e}")
            raise

    async def sync_packages(self):
        """Sync medical package data periodically"""
        try:
            logger.info("Syncing package data...")
            # Don't use async with - clinic_api is a long-lived shared instance
            packages = await self.clinic_api.get_medical_packages()

            # Delete existing and reload
            self.vector_store.delete_collection("medical_packages")
            await self.load_all_packages()
            logger.info("Package data synced")
        except Exception as e:
            logger.error(f"Error syncing packages: {e}")
            raise
