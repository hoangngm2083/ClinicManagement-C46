"""
Medical Symptom Analyzer with Enhanced Professional Accuracy
Implements ML-based symptom analysis and medical knowledge integration
"""
import re
import logging
from typing import Dict, List, Any, Optional, Tuple
from dataclasses import dataclass
from enum import Enum

logger = logging.getLogger(__name__)

class UrgencyLevel(Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"

class SymptomCategory(Enum):
    CARDIOVASCULAR = "cardiovascular"
    RESPIRATORY = "respiratory"
    GASTROINTESTINAL = "gastrointestinal"
    NEUROLOGICAL = "neurological"
    MUSCULOSKELETAL = "musculoskeletal"
    DERMATOLOGICAL = "dermatological"
    OPHTHALMOLOGICAL = "ophthalmological"
    OTOLARYNGOLOGICAL = "otolaryngological"
    DENTAL = "dental"
    ENDOCRINE = "endocrine"
    UROLOGICAL = "urological"
    GYNECOLOGICAL = "gynecological"
    PEDIATRIC = "pediatric"
    GENERAL = "general"

@dataclass
class SymptomAnalysis:
    """Structured analysis of symptoms"""
    primary_category: SymptomCategory
    urgency_level: UrgencyLevel
    confidence_score: float
    related_symptoms: List[str]
    possible_conditions: List[str]
    recommended_specialties: List[str]
    red_flags: List[str]  # Symptoms requiring immediate attention

@dataclass
class MedicalRecommendation:
    """Medical package recommendation with clinical reasoning"""
    package_id: str
    package_name: str
    relevance_score: float
    clinical_reasoning: str
    urgency_justification: str
    specialty_match: bool
    confidence_level: str

class MedicalSymptomAnalyzer:
    """
    Advanced medical symptom analyzer with clinical knowledge integration
    """

    def __init__(self):
        self.symptom_patterns = self._load_symptom_patterns()
        self.medical_knowledge = self._load_medical_knowledge()
        self.clinical_guidelines = self._load_clinical_guidelines()

    def _load_symptom_patterns(self) -> Dict[str, Any]:
        """Load comprehensive symptom patterns with medical accuracy"""
        return {
            # Critical symptoms requiring immediate attention
            'critical': {
                'patterns': [
                    r'không thể thở|khó thở nặng|ngừng thở',
                    r'đau ngực nặng|đau ngực như bị đè|nắn|bóp nghẹt',
                    r'mất ý thức|ngất xỉu đột ngột|co giật liên tục',
                    r'chảy máu nhiều|chảy máu không cầm được',
                    r'đau đầu đột ngột dữ dội|đau đầu như vỡ đầu',
                    r'sốt cao trên 40độ|sốt kèm co giật',
                    r'vết thương hở sâu|vết thương chảy máu nhiều',
                    r'ngộ độc thực phẩm nặng|nôn mửa không dừng'
                ],
                'urgency': UrgencyLevel.CRITICAL,
                'category': SymptomCategory.GENERAL,
                'red_flags': ['Cần cấp cứu ngay lập tức']
            },

            # High urgency cardiovascular symptoms
            'cardiovascular_high': {
                'patterns': [
                    r'đau ngực trái|đau ngực lan ra cánh tay',
                    r'nhịp tim nhanh bất thường|nhịp tim chậm dưới 50',
                    r'huyết áp cao đột ngột|huyết áp thấp đột ngột',
                    r'sưng chân kèm đau ngực|sưng cổ chân đột ngột',
                    r'khó thở khi nằm|đánh trống ngực'
                ],
                'urgency': UrgencyLevel.HIGH,
                'category': SymptomCategory.CARDIOVASCULAR,
                'possible_conditions': ['Acute coronary syndrome', 'Heart failure', 'Arrhythmia'],
                'specialties': ['Cardiology', 'Emergency Medicine']
            },

            # Neurological symptoms
            'neurological': {
                'patterns': [
                    r'đau đầu migraine|đau nửa đầu',
                    r'chóng mặt quay cuồng|vertigo',
                    r'mờ mắt|mất thị lực|mờ một mắt',
                    r'tê bì nửa người|tê nửa mặt',
                    r'run tay chân|co giật nhẹ',
                    r'mất cảm giác|mất sức cơ'
                ],
                'urgency': UrgencyLevel.HIGH,
                'category': SymptomCategory.NEUROLOGICAL,
                'possible_conditions': ['Stroke', 'Migraine', 'Neuropathy', 'Multiple sclerosis'],
                'specialties': ['Neurology', 'Neurosurgery']
            },

            # Respiratory symptoms
            'respiratory': {
                'patterns': [
                    r'ho ra máu|ho ra đờm lẫn máu',
                    r'khó thở khi gắng sức|khó thở khi nằm',
                    r'ho kéo dài trên 3 tuần|ho mạn tính',
                    r'thở khò khè|tiếng rít khi thở',
                    r'đau ngực khi thở sâu|đau ngực khi ho'
                ],
                'urgency': UrgencyLevel.MEDIUM,
                'category': SymptomCategory.RESPIRATORY,
                'possible_conditions': ['Pneumonia', 'COPD', 'Asthma', 'Lung cancer'],
                'specialties': ['Pulmonology', 'Respiratory Medicine']
            },

            # Gastrointestinal symptoms
            'gastrointestinal': {
                'patterns': [
                    r'đau bụng dữ dội|đau quặn bụng',
                    r'nôn ra máu|nôn ra thức ăn cũ',
                    r'tiêu chảy ra máu|phân đen',
                    r'vàng da kèm đau bụng|vàng mắt',
                    r'không thể nuốt|nuốt nghẹn|ợ ra máu',
                    r'táo bón kéo dài|mất cảm giác muốn đi vệ sinh'
                ],
                'urgency': UrgencyLevel.MEDIUM,
                'category': SymptomCategory.GASTROINTESTINAL,
                'possible_conditions': ['Acute abdomen', 'GI bleeding', 'Cholecystitis', 'Pancreatitis'],
                'specialties': ['Gastroenterology', 'General Surgery']
            },

            # Dental symptoms
            'dental': {
                'patterns': [
                    r'đau răng nhức nhối|đau răng không ngủ được',
                    r'răng lung lay|răng gãy vỡ',
                    r'sưng lợi|sưng má|sưng hàm',
                    r'mủ răng|miệng hôi|hôi miệng nặng',
                    r'viêm nha chu| lợi chảy máu khi đánh răng'
                ],
                'urgency': UrgencyLevel.MEDIUM,
                'category': SymptomCategory.DENTAL,
                'possible_conditions': ['Dental abscess', 'Periodontitis', 'Tooth fracture', 'Pericoronitis'],
                'specialties': ['Dentistry', 'Oral Surgery']
            },

            # Dermatological symptoms
            'dermatological': {
                'patterns': [
                    r'ngứa toàn thân|ngứa không thể chịu đựng',
                    r'ban đỏ lan rộng|mẩn ngứa đỏ',
                    r'phồng rộp nước|loét da lan rộng',
                    r'thay đổi sắc tố da|u hắc tố',
                    r'vết loét không lành|mụn cóc lạ'
                ],
                'urgency': UrgencyLevel.LOW,
                'category': SymptomCategory.DERMATOLOGICAL,
                'possible_conditions': ['Contact dermatitis', 'Psoriasis', 'Eczema', 'Skin cancer'],
                'specialties': ['Dermatology']
            },

            # Ophthalmological symptoms
            'ophthalmological': {
                'patterns': [
                    r'mờ mắt đột ngột|mất thị lực nhanh',
                    r'thay đổi thị lực|thị lực giảm nhanh',
                    r'đau mắt đỏ|đau mắt kèm mờ',
                    r'thay đổi đồng tử|mờ đục thủy tinh thể',
                    r'thay đổi màu sắc nhìn|ánh sáng lạ'
                ],
                'urgency': UrgencyLevel.MEDIUM,
                'category': SymptomCategory.OPHTHALMOLOGICAL,
                'possible_conditions': ['Retinal detachment', 'Glaucoma', 'Cataract', 'Macular degeneration'],
                'specialties': ['Ophthalmology']
            },

            # ENT symptoms
            'ent': {
                'patterns': [
                    r'đau tai dữ dội|điếc đột ngột',
                    r'chảy máu mũi không cầm được',
                    r'nuốt nghẹn|đau họng nặng',
                    r'chóng mặt quay cuồng kèm nôn',
                    r'nghẹt mũi kéo dài|thay đổi giọng nói'
                ],
                'urgency': UrgencyLevel.MEDIUM,
                'category': SymptomCategory.OTOLARYNGOLOGICAL,
                'possible_conditions': ['Acute otitis media', 'Epistaxis', 'Tonsillitis', 'Vestibular disorders'],
                'specialties': ['Otolaryngology', 'ENT']
            }
        }

    def _load_medical_knowledge(self) -> Dict[str, Any]:
        """Load medical knowledge base for better reasoning"""
        return {
            'symptom_clusters': {
                'cardiac_cluster': ['đau ngực', 'khó thở', 'mệt mỏi', 'sưng chân', 'đánh trống ngực'],
                'respiratory_cluster': ['ho', 'khó thở', 'đau ngực khi thở', 'sốt', 'mệt mỏi'],
                'gi_cluster': ['đau bụng', 'buồn nôn', 'tiêu chảy', 'táo bón', 'chán ăn'],
                'neuro_cluster': ['đau đầu', 'chóng mặt', 'tê bì', 'yếu cơ', 'mờ mắt'],
                'dental_cluster': ['đau răng', 'sưng lợi', 'mủ răng', 'hôi miệng', 'đau hàm']
            },

            'clinical_correlations': {
                'chest_pain': {
                    'cardiac': ['đau ngực trái', 'lan ra cánh tay', 'kèm mồ hôi', 'cảm giác nghẹt thở'],
                    'respiratory': ['đau khi thở sâu', 'ho', 'sốt', 'khạc đờm'],
                    'musculoskeletal': ['đau khi cử động', 'có thể chỉ định vị trí đau chính xác']
                }
            },
            'specialty_mapping': {
                'Cardiology': 'tim mạch',
                'Emergency Medicine': 'cấp cứu',
                'Pulmonology': 'hô hấp',
                'Gastroenterology': 'tiêu hóa',
                'Neurology': 'thần kinh',
                'Dermatology': 'da liễu',
                'Ophthalmology': 'mắt',
                'ENT': 'tai mũi họng',
                'Dentistry': 'răng',
                'Endocrinology': 'nội tiết',
                'Gynecology': 'sản phụ khoa',
                'Urology': 'nam khoa',
                'Pediatrics': 'nhi khoa',
                'General Medicine': 'tổng quát'
            }
        }

    def _load_clinical_guidelines(self) -> Dict[str, Any]:
        """Load clinical guidelines for decision support"""
        return {
            'red_flags': {
                'immediate_emergency': [
                    'khó thở nặng', 'đau ngực nặng', 'mất ý thức', 'chảy máu nhiều',
                    'sốt cao kèm co giật', 'đau bụng dữ dội kèm nôn ói nhiều'
                ],
                'urgent_attention': [
                    'đau đầu đột ngột dữ dội', 'mờ mắt đột ngột', 'yếu nửa người',
                    'ho ra máu', 'tiêu chảy ra máu', 'vàng da nhanh'
                ]
            },

            'specialty_routing': {
                SymptomCategory.CARDIOVASCULAR: ['tim mạch', 'cấp cứu'],
                SymptomCategory.NEUROLOGICAL: ['thần kinh', 'phẫu thuật thần kinh'],
                SymptomCategory.RESPIRATORY: ['hô hấp', 'nội khoa'],
                SymptomCategory.GASTROINTESTINAL: ['tiêu hóa', 'phẫu thuật tổng quát'],
                SymptomCategory.DENTAL: ['răng', 'phẫu thuật miệng hàm mặt'],
                SymptomCategory.DERMATOLOGICAL: ['da liễu'],
                SymptomCategory.OPHTHALMOLOGICAL: ['mắt'],
                SymptomCategory.OTOLARYNGOLOGICAL: ['tai mũi họng'],
                SymptomCategory.ENDOCRINE: ['nội tiết'],
                SymptomCategory.GYNECOLOGICAL: ['sản phụ khoa'],
                SymptomCategory.UROLOGICAL: ['nam khoa'],
                SymptomCategory.PEDIATRIC: ['nhi khoa'],
                SymptomCategory.GENERAL: ['tổng quát']
            }
        }

    def analyze_symptoms(self, symptom_text: str) -> SymptomAnalysis:
        """
        Comprehensive symptom analysis with medical expertise

        Args:
            symptom_text: Patient's symptom description

        Returns:
            Structured analysis with clinical insights
        """
        symptom_text = symptom_text.lower().strip()

        # Initialize analysis
        analysis = SymptomAnalysis(
            primary_category=SymptomCategory.GENERAL,
            urgency_level=UrgencyLevel.LOW,
            confidence_score=0.0,
            related_symptoms=[],
            possible_conditions=[],
            recommended_specialties=[],
            red_flags=[]
        )

        # Check for critical symptoms first
        critical_matches = self._check_patterns(symptom_text, self.symptom_patterns['critical']['patterns'])
        if critical_matches:
            analysis.urgency_level = UrgencyLevel.CRITICAL
            analysis.red_flags = self.symptom_patterns['critical']['red_flags']
            analysis.confidence_score = 0.95
            return analysis

        # Analyze against all symptom categories
        category_scores = {}
        all_matched_patterns = []

        for category_name, category_data in self.symptom_patterns.items():
            if category_name == 'critical':
                continue

            matches = self._check_patterns(symptom_text, category_data['patterns'])
            if matches:
                score = len(matches) / len(category_data['patterns'])  # Pattern density score
                category_scores[category_name] = score
                all_matched_patterns.extend(matches)

                # Update analysis with highest scoring category
                if score > analysis.confidence_score:
                    analysis.primary_category = category_data['category']
                    analysis.urgency_level = category_data['urgency']
                    analysis.confidence_score = min(score, 0.9)  # Cap at 0.9 for uncertainty

                    analysis.possible_conditions = category_data.get('possible_conditions', [])
                    analysis.recommended_specialties = self.clinical_guidelines['specialty_routing'].get(category_data['category'], [])

        # Extract related symptoms from clusters
        analysis.related_symptoms = self._extract_related_symptoms(symptom_text)

        # Check for red flags in clinical guidelines
        analysis.red_flags = self._check_red_flags(symptom_text)

        # Boost confidence if multiple related symptoms present
        if len(analysis.related_symptoms) > 2:
            analysis.confidence_score = min(analysis.confidence_score + 0.1, 0.95)

        return analysis

    def _check_patterns(self, text: str, patterns: List[str]) -> List[str]:
        """Check text against regex patterns"""
        matches = []
        for pattern in patterns:
            if re.search(pattern, text, re.IGNORECASE):
                matches.append(pattern)
        return matches

    def _extract_related_symptoms(self, symptom_text: str) -> List[str]:
        """Extract related symptoms based on medical knowledge"""
        related = []

        for cluster_name, symptoms in self.medical_knowledge['symptom_clusters'].items():
            cluster_matches = [s for s in symptoms if s in symptom_text]
            if cluster_matches:
                related.extend(cluster_matches)

        return list(set(related))  # Remove duplicates

    def _check_red_flags(self, symptom_text: str) -> List[str]:
        """Check for red flag symptoms requiring special attention"""
        red_flags = []

        for flag in self.clinical_guidelines['red_flags']['immediate_emergency']:
            if flag in symptom_text:
                red_flags.append(f"🚨 KHẨN CẤP: {flag}")

        for flag in self.clinical_guidelines['red_flags']['urgent_attention']:
            if flag in symptom_text:
                red_flags.append(f"⚠️ CẦN CHÚ Ý: {flag}")

        return red_flags

    def recommend_medical_packages(
        self,
        analysis: SymptomAnalysis,
        available_packages: List[Dict[str, Any]]
    ) -> List[MedicalRecommendation]:
        """
        Generate clinically-informed package recommendations

        Args:
            analysis: Symptom analysis result
            available_packages: List of available medical packages

        Returns:
            Ranked list of medical recommendations
        """
        recommendations = []

        for package in available_packages:
            recommendation = self._score_package_relevance(package, analysis)
            if recommendation.relevance_score > 0.1:  # Minimum threshold
                recommendations.append(recommendation)

        # Sort by clinical relevance and urgency
        recommendations.sort(key=lambda x: (
            -x.relevance_score,
            -self._urgency_priority(x.urgency_justification)
        ), reverse=True)

        return recommendations[:5]  # Top 5 recommendations

    def _score_package_relevance(
        self,
        package: Dict[str, Any],
        analysis: SymptomAnalysis
    ) -> MedicalRecommendation:
        """Score how relevant a package is for the analyzed symptoms"""

        package_name = package.get('name', '').lower()
        package_desc = package.get('description', '').lower()

        relevance_score = 0.0
        clinical_reasoning_parts = []
        urgency_parts = []

        # Specialty matching (high weight)
        package_specialties = self._extract_package_specialties(package_name, package_desc)
        specialty_match = any(
            specialty.lower() in package_name or specialty.lower() in package_desc
            for specialty in analysis.recommended_specialties
        )

        if specialty_match:
            relevance_score += 0.4
            clinical_reasoning_parts.append("Chuyên khoa phù hợp với triệu chứng")
        else:
            # For high urgency cases, give general packages higher relevance
            if analysis.urgency_level in [UrgencyLevel.HIGH, UrgencyLevel.CRITICAL]:
                if 'tổng quát' in package_name or 'cơ bản' in package_name:
                    relevance_score += 0.3
                    clinical_reasoning_parts.append("Khám tổng quát cấp thiết để đánh giá triệu chứng khẩn cấp")
                else:
                    # Other packages get minimal score for high urgency
                    relevance_score += 0.05
                    clinical_reasoning_parts.append("Có thể bổ sung để kiểm tra toàn diện")
            else:
                # For lower urgency, general packages still get some relevance
                if 'tổng quát' in package_name or 'cơ bản' in package_name:
                    relevance_score += 0.15
                    clinical_reasoning_parts.append("Khám tổng quát để đánh giá ban đầu")

        # Symptom keyword matching
        symptom_keywords = self._get_category_keywords(analysis.primary_category)
        keyword_matches = []

        for keyword in symptom_keywords:
            if keyword in package_name or keyword in package_desc:
                keyword_matches.append(keyword)
                relevance_score += 0.2

        if keyword_matches:
            clinical_reasoning_parts.append(f"Liên quan đến: {', '.join(keyword_matches[:3])}")

        # Urgency consideration
        if analysis.urgency_level in [UrgencyLevel.HIGH, UrgencyLevel.CRITICAL]:
            urgency_parts.append("Khuyến nghị khám sớm do mức độ khẩn cấp cao")
        elif analysis.urgency_level == UrgencyLevel.MEDIUM:
            urgency_parts.append("Nên khám trong thời gian sớm")
        else:
            urgency_parts.append("Có thể sắp xếp theo lịch phù hợp")

        # Confidence adjustment
        relevance_score *= analysis.confidence_score

        # Create clinical reasoning
        clinical_reasoning = "; ".join(clinical_reasoning_parts) if clinical_reasoning_parts else "Dựa trên phân tích triệu chứng"
        urgency_justification = "; ".join(urgency_parts) if urgency_parts else "Đánh giá theo mức độ khẩn cấp"

        # Determine confidence level
        if relevance_score > 0.7:
            confidence_level = "Cao"
        elif relevance_score > 0.4:
            confidence_level = "Trung bình"
        else:
            confidence_level = "Thấp"

        return MedicalRecommendation(
            package_id=package.get('id', ''),
            package_name=package.get('name', 'N/A'),
            relevance_score=round(relevance_score, 3),
            clinical_reasoning=clinical_reasoning,
            urgency_justification=urgency_justification,
            specialty_match=specialty_match,
            confidence_level=confidence_level
        )

    def _extract_package_specialties(self, name: str, description: str) -> List[str]:
        """Extract medical specialties mentioned in package (Vietnamese)"""
        specialties = []
        specialty_keywords = {
            'tim mạch': ['tim', 'mạch', 'trái tim', 'tim mạch', 'cardio', 'cardiovascular'],
            'răng': ['răng', 'hàm', 'răng miệng', 'dental', 'nha khoa'],
            'mắt': ['mắt', 'thị lực', 'nhãn khoa', 'ophthalmo', 'ophthalmology'],
            'da liễu': ['da', 'liễu', 'da liễu', 'dermat', 'dermatology'],
            'thần kinh': ['thần kinh', 'não', 'thần kinh học', 'neuro', 'neurology'],
            'tiêu hóa': ['tiêu hóa', 'dạ dày', 'ruột', 'gan', 'gastro', 'gastroenterology'],
            'hô hấp': ['phổi', 'hô hấp', 'phế quản', 'respiratory', 'pulmonology'],
            'nội tiết': ['nội tiết', 'hormone', 'đái tháo đường', 'endocrine'],
            'tai mũi họng': ['tai', 'mũi', 'họng', 'tai mũi họng', 'ent', 'otorhinolaryngology'],
            'sản phụ khoa': ['phụ khoa', 'sản phụ khoa', 'bầu bí', 'gynecology', 'obstetrics'],
            'nam khoa': ['nam khoa', 'tiết niệu', 'urology', 'andrology'],
            'cơ xương khớp': ['cơ xương khớp', 'chỉnh hình', 'orthopedics'],
            'nhi khoa': ['nhi', 'trẻ em', 'pediatrics'],
            'tổng quát': ['tổng quát', 'cơ bản', 'general', 'internal medicine']
        }

        text = f"{name} {description}".lower()
        for specialty, keywords in specialty_keywords.items():
            if any(kw in text for kw in keywords):
                specialties.append(specialty)

        return specialties

    def _get_category_keywords(self, category: SymptomCategory) -> List[str]:
        """Get relevant keywords for a symptom category (Vietnamese)"""
        keyword_map = {
            SymptomCategory.CARDIOVASCULAR: ['tim mạch', 'tim', 'mạch', 'trái tim', 'huyết áp', 'nhịp tim', 'đánh trống ngực'],
            SymptomCategory.RESPIRATORY: ['phổi', 'hô hấp', 'ho', 'khó thở', 'đờm', 'phế quản', 'hen suyễn'],
            SymptomCategory.GASTROINTESTINAL: ['tiêu hóa', 'dạ dày', 'ruột', 'đau bụng', 'tiêu chảy', 'táo bón', 'ợ nóng', 'nôn'],
            SymptomCategory.NEUROLOGICAL: ['thần kinh', 'não', 'đau đầu', 'migraine', 'chóng mặt', 'co giật', 'mất cảm giác', 'yếu cơ'],
            SymptomCategory.MUSCULOSKELETAL: ['cơ xương khớp', 'gãy xương', 'thoát vị', 'đau khớp', 'đau lưng', 'chỉnh hình'],
            SymptomCategory.DERMATOLOGICAL: ['da liễu', 'da', 'mụn', 'ngứa', 'eczema', 'viêm da', 'nám'],
            SymptomCategory.OPHTHALMOLOGICAL: ['mắt', 'thị lực', 'đau mắt', 'mờ mắt', 'đỏ mắt', 'nhãn khoa'],
            SymptomCategory.OTOLARYNGOLOGICAL: ['tai mũi họng', 'tai', 'mũi', 'họng', 'điếc', 'nghẹt mũi', 'đau họng'],
            SymptomCategory.DENTAL: ['răng', 'hàm', 'nha khoa', 'đau răng', 'sưng lợi', 'mủ răng'],
            SymptomCategory.ENDOCRINE: ['nội tiết', 'tiểu đường', 'hormone', 'khát nước', 'sụt cân', 'mệt mỏi'],
            SymptomCategory.UROLOGICAL: ['tiết niệu', 'thận', 'bàng quang', 'tiểu khó', 'tiểu nhiều', 'nam khoa'],
            SymptomCategory.GYNECOLOGICAL: ['phụ khoa', 'sản phụ khoa', 'kinh nguyệt', 'bầu bí', 'vô sinh'],
            SymptomCategory.PEDIATRIC: ['nhi khoa', 'trẻ em', 'trẻ nhỏ', 'tiêm chủng', 'phát triển'],
            SymptomCategory.GENERAL: ['tổng quát', 'cơ bản', 'định kỳ', 'kiểm tra sức khỏe', 'thường xuyên']
        }

        return keyword_map.get(category, [])

    def _urgency_priority(self, urgency_text: str) -> int:
        """Convert urgency text to priority score"""
        if 'cao' in urgency_text.lower():
            return 3
        elif 'trung bình' in urgency_text.lower():
            return 2
        elif 'thấp' in urgency_text.lower():
            return 1
        return 0
