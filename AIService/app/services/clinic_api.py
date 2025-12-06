import httpx
from typing import List, Dict, Optional, Any
import logging
from ..config.settings import settings
from .medical_analyzer import MedicalSymptomAnalyzer, SymptomAnalysis, MedicalRecommendation

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
        return response.get("content", [])

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
        doctors = await self.get_doctors(role=0)
        return [doc for doc in doctors if specialty.lower() in (doc.get("description", "").lower() or "")]

    async def get_package_recommendations(self, symptoms: str) -> List[Dict[str, Any]]:
        """
        Enhanced medical package recommendations using clinical analysis

        Args:
            symptoms: Patient's symptom description

        Returns:
            List of recommended packages with clinical reasoning
        """
        try:
            # Get available packages
            packages = await self.get_medical_packages()

            # Initialize medical analyzer (could be cached in production)
            analyzer = MedicalSymptomAnalyzer()

            # Perform comprehensive symptom analysis
            analysis = analyzer.analyze_symptoms(symptoms)

            logger.info(f"Symptom analysis - Category: {analysis.primary_category.value}, "
                       f"Urgency: {analysis.urgency_level.value}, "
                       f"Confidence: {analysis.confidence_score:.2f}")

            # Get clinically-informed recommendations
            medical_recommendations = analyzer.recommend_medical_packages(analysis, packages)

            # Convert to format compatible with existing tool
            recommendations = []
            for rec in medical_recommendations:
                # Find the original package data
                original_package = next((p for p in packages if p.get('id') == rec.package_id), None)
                if original_package:
                    package_with_analysis = dict(original_package)
                    package_with_analysis['_score'] = rec.relevance_score
                    package_with_analysis['_urgency'] = analysis.urgency_level.value
                    package_with_analysis['_confidence'] = analysis.confidence_score
                    package_with_analysis['_clinical_reasoning'] = rec.clinical_reasoning
                    package_with_analysis['_urgency_justification'] = rec.urgency_justification
                    package_with_analysis['_specialty_match'] = rec.specialty_match
                    package_with_analysis['_confidence_level'] = rec.confidence_level
                    package_with_analysis['_possible_conditions'] = analysis.possible_conditions
                    package_with_analysis['_recommended_specialties'] = analysis.recommended_specialties
                    package_with_analysis['_red_flags'] = analysis.red_flags
                    package_with_analysis['_related_symptoms'] = analysis.related_symptoms

                    recommendations.append(package_with_analysis)

            # Add urgent notes based on analysis
            if recommendations:
                main_rec = recommendations[0]

                # Critical symptoms
                if analysis.urgency_level.value == 'critical':
                    main_rec['_urgent_note'] = "🚨 KHẨN CẤP: Triệu chứng này có thể đe dọa tính mạng. Hãy đến cơ sở y tế gần nhất ngay lập tức hoặc gọi cấp cứu 115!"

                # High urgency symptoms
                elif analysis.urgency_level.value == 'high':
                    main_rec['_urgent_note'] = "⚠️ CẦN CHÚ Ý: Triệu chứng này cần được thăm khám sớm trong vòng 24-48 giờ để tránh biến chứng."

                # Red flags present
                if analysis.red_flags:
                    red_flag_notes = [flag for flag in analysis.red_flags]
                    if red_flag_notes:
                        main_rec['_red_flag_notes'] = red_flag_notes

            logger.info(f"Generated {len(recommendations)} clinical recommendations")
            return recommendations

        except Exception as e:
            logger.error(f"Error in enhanced get_package_recommendations: {e}", exc_info=True)
            # Fallback to basic keyword matching
            return await self._fallback_recommendations(symptoms)

    async def _fallback_recommendations(self, symptoms: str) -> List[Dict[str, Any]]:
        """Fallback method using basic keyword matching"""
        logger.warning("Using fallback recommendation method")

        packages = await self.get_medical_packages()
        symptoms_lower = symptoms.lower()

        # Basic keyword matching
        basic_keywords = {
            'răng': ['răng', 'hàm', 'dental'],
            'mắt': ['mắt', 'thị lực'],
            'tim mạch': ['tim', 'mạch', 'huyết áp'],
            'tiêu hóa': ['dạ dày', 'ruột', 'tiêu hóa'],
            'da liễu': ['da', 'mụn', 'ngứa'],
            'thần kinh': ['đau đầu', 'chóng mặt'],
            'tổng quát': ['tổng quát', 'cơ bản']
        }

        scored_packages = []
        for package in packages:
            package_text = f"{package.get('name', '')} {package.get('description', '')}".lower()
            score = 0

            for category, keywords in basic_keywords.items():
                if any(kw in symptoms_lower for kw in keywords):
                    if any(kw in package_text for kw in keywords):
                        score += 1

            if score > 0:
                package_with_score = dict(package)
                package_with_score['_score'] = score
                package_with_score['_urgency'] = 'medium'
                package_with_score['_confidence'] = 0.5
                package_with_score['_fallback'] = True
                scored_packages.append(package_with_score)

        return sorted(scored_packages, key=lambda x: x.get('_score', 0), reverse=True)[:5]
