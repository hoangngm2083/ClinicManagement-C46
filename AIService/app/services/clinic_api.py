import httpx
from typing import List, Dict, Optional, Any
import logging
from ..config.settings import settings

logger = logging.getLogger(__name__)


class ClinicAPIService:
    def __init__(self, base_url: Optional[str] = None):
        self.base_url = base_url or settings.clinic_api_base_url
        self.client = httpx.AsyncClient(
            timeout=httpx.Timeout(10.0, connect=5.0),
            headers={"Content-Type": "application/json"}
        )
        logger.info(f"Initialized ClinicAPIService with base_url: {self.base_url}")

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.client.aclose()

    async def _get(self, endpoint: str, params: Optional[Dict] = None) -> Dict[str, Any]:
        """Generic GET request"""
        url = f"{self.base_url}{endpoint}"
        try:
            response = await self.client.get(url, params=params)
            response.raise_for_status()
            return response.json()
        except httpx.HTTPError as e:
            logger.error(f"HTTP error for {url}: {e}")
            raise

    async def _post(self, endpoint: str, data: Dict[str, Any], headers: Optional[Dict] = None) -> Dict[str, Any]:
        """Generic POST request"""
        url = f"{self.base_url}{endpoint}"
        try:
            response = await self.client.post(url, json=data, headers=headers)
            response.raise_for_status()
            return response.json()
        except httpx.HTTPError as e:
            logger.error(f"HTTP error for {url}: {e}")
            raise

    async def get_doctors(self,
                         keyword: Optional[str] = None,
                         department_id: Optional[str] = None,
                         role: Optional[int] = None,
                         page: int = 1) -> List[Dict[str, Any]]:
        """Lấy danh sách bác sĩ với filter"""
        params = {"page": page}
        if keyword:
            params["keyword"] = keyword
        if department_id:
            params["departmentId"] = department_id
        if role is not None:
            params["role"] = role

        response = await self._get("/api/staff", params)
        return response.get("data", {}).get("content", [])

    async def get_doctor_by_id(self, doctor_id: str) -> Optional[Dict[str, Any]]:
        """Lấy thông tin chi tiết bác sĩ theo ID"""
        try:
            response = await self._get(f"/api/staff/{doctor_id}")
            return response
        except httpx.HTTPError:
            return None

    async def get_doctor_schedule(self, month: int, year: int) -> List[Dict[str, Any]]:
        """Lấy lịch làm việc của bác sĩ theo tháng"""
        params = {"month": month, "year": year}
        response = await self._get("/api/staff/schedule", params)
        return response

    async def get_departments(self) -> List[Dict[str, Any]]:
        """Lấy danh sách khoa phòng"""
        response = await self._get("/api/department")
        return response.get("data", [])

    async def get_medical_packages(self,
                                 keyword: Optional[str] = None,
                                 page: int = 1) -> List[Dict[str, Any]]:
        """Lấy danh sách gói khám"""
        params = {"page": page}
        if keyword:
            params["keyword"] = keyword

        response = await self._get("/api/medical-package", params)
        # MedicalPackageService trả về trực tiếp {content: [...], page: ..., total: ...}
        # Không có wrap trong "data" như các service khác
        if "content" in response:
            packages = response.get("content", [])
        else:
            # Fallback cho trường hợp có wrap trong "data" (backward compatible)
            packages = response.get("data", {}).get("content", [])
        
        # Normalize field names: medicalPackageId -> id để consistent với code khác
        normalized_packages = []
        for package in packages:
            normalized = dict(package)
            if "medicalPackageId" in normalized and "id" not in normalized:
                normalized["id"] = normalized.pop("medicalPackageId")
            normalized_packages.append(normalized)
        
        return normalized_packages

    async def get_medical_package_by_id(self, package_id: str) -> Optional[Dict[str, Any]]:
        """Lấy thông tin chi tiết gói khám theo ID"""
        try:
            response = await self._get(f"/api/medical-package/{package_id}")
            return response
        except httpx.HTTPError:
            return None

    async def get_available_slots(self,
                                medical_package_id: str,
                                date_from: Optional[str] = None,
                                date_to: Optional[str] = None,
                                page: int = 0) -> List[Dict[str, Any]]:
        """Lấy danh sách slot trống theo gói khám với khoảng thời gian"""
        params = {
            "medicalPackageId": medical_package_id,
            "page": page
        }
        if date_from:
            params["dateFrom"] = date_from
        if date_to:
            params["dateTo"] = date_to

        response = await self._get("/api/slot", params)
        return response.get("content", [])

    async def create_booking(self,
                           slot_id: str,
                           name: str,
                           email: str,
                           phone: str,
                           fingerprint: str) -> str:
        """Tạo booking mới"""
        booking_data = {
            "slotId": slot_id,
            "name": name,
            "email": email,
            "phone": phone
        }

        headers = {"Fingerprint": fingerprint}
        response = await self._post("/api/booking", booking_data, headers)
        return response.get("bookingId")

    async def get_booking_status(self, booking_id: str) -> Optional[Dict[str, Any]]:
        """Lấy trạng thái booking"""
        try:
            response = await self._get(f"/api/booking/{booking_id}/status")
            return response.get("bookingStatus")
        except httpx.HTTPError:
            return None

    async def get_all_slots_next_week(self) -> List[Dict[str, Any]]:
        """Lấy tất cả slot trong tuần tới"""
        from datetime import datetime, timedelta

        all_slots = []
        packages = await self.get_medical_packages()

        start_date = datetime.now().date()
        end_date = start_date + timedelta(days=7)

        for package in packages:
            try:
                slots = await self.get_available_slots(package["id"])
                for slot in slots:
                    slot_date = datetime.fromisoformat(slot["date"]).date()
                    if start_date <= slot_date <= end_date:
                        slot["package_name"] = package["name"]
                        slot["package_description"] = package["description"]
                        all_slots.append(slot)
            except Exception as e:
                logger.warning(f"Error getting slots for package {package['id']}: {e}")
                continue

        return all_slots

    async def search_doctors_by_specialty(self, specialty: str) -> List[Dict[str, Any]]:
        """Tìm bác sĩ theo chuyên khoa"""
        doctors = await self.get_doctors()
        return [doc for doc in doctors if specialty.lower() in (doc.get("description", "").lower() or "")]

    async def get_package_recommendations(self, symptoms: str) -> List[Dict[str, Any]]:
        """Đề xuất gói khám dựa trên triệu chứng"""
        packages = await self.get_medical_packages()
        symptom_keywords = {
            'răng': ['khám răng', 'niềng răng', 'trám răng', 'răng miệng'],
            'mắt': ['khám mắt', 'nhãn khoa', 'mắt'],
            'tim mạch': ['tim mạch', 'tim', 'mạch máu'],
            'tổng quát': ['tổng quát', 'thường xuyên', 'kiểm tra sức khỏe'],
            'phụ khoa': ['phụ khoa', 'sản phụ khoa', 'bệnh phụ nữ'],
            'nam khoa': ['nam khoa', 'bệnh nam giới'],
            'tiêu hóa': ['tiêu hóa', 'dạ dày', 'ruột'],
            'thần kinh': ['thần kinh', 'não', 'đau đầu'],
            'cơ xương khớp': ['cơ xương khớp', 'gãy xương', 'thoát vị'],
            'da liễu': ['da liễu', 'da', 'nám', 'mụn']
        }

        recommendations = []
        symptoms_lower = symptoms.lower()

        for package in packages:
            package_name = package.get("name", "").lower()
            package_desc = package.get("description", "").lower()

            for symptom_type, keywords in symptom_keywords.items():
                if symptoms_lower in symptom_type or any(k in symptoms_lower for k in keywords):
                    if any(k in package_name or k in package_desc for k in keywords):
                        recommendations.append(package)
                        break

        return recommendations[:5]  # Top 5 recommendations
