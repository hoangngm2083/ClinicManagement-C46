import logging
import logging.config
from typing import Dict, Any
import sys
from pathlib import Path


def setup_logging():
    """Setup logging configuration"""
    logging_config = {
        "version": 1,
        "disable_existing_loggers": False,
        "formatters": {
            "default": {
                "format": "%(asctime)s - %(name)s - %(levelname)s - %(message)s",
                "datefmt": "%Y-%m-%d %H:%M:%S"
            },
            "detailed": {
                "format": "%(asctime)s - %(name)s - %(levelname)s - %(funcName)s:%(lineno)d - %(message)s",
                "datefmt": "%Y-%m-%d %H:%M:%S"
            }
        },
        "handlers": {
            "console": {
                "class": "logging.StreamHandler",
                "formatter": "default",
                "level": "INFO",
                "stream": sys.stdout
            },
            "file": {
                "class": "logging.handlers.RotatingFileHandler",
                "formatter": "detailed",
                "level": "DEBUG",
                "filename": "/app/logs/ai_service.log",
                "maxBytes": 10 * 1024 * 1024,  # 10MB
                "backupCount": 5
            }
        },
        "root": {
            "level": "INFO",
            "handlers": ["console"]  # Only use console handler in Docker
        },
        "loggers": {
            "app": {
                "level": "DEBUG",
                "handlers": ["console", "file"],
                "propagate": False
            },
            "langchain": {
                "level": "INFO",
                "handlers": ["console", "file"],
                "propagate": False
            },
            "httpx": {
                "level": "WARNING",
                "handlers": ["console"],
                "propagate": False
            }
        }
    }

    # Create logs directory
    logs_dir = Path("/app/logs")
    logs_dir.mkdir(exist_ok=True)

    # Apply configuration
    logging.config.dictConfig(logging_config)


def format_currency(amount: float) -> str:
    """Format currency in VND"""
    return f"{amount:,.0f} VND"


def parse_patient_info(info_string: str) -> Dict[str, str]:
    """
    Parse patient info from string format: "name:Nguyen Van A,email:a@example.com,phone:0123456789"

    Args:
        info_string: Patient info in string format

    Returns:
        Dict with parsed patient information
    """
    info_parts = info_string.split(',')
    patient_data = {}

    for part in info_parts:
        if ':' in part:
            key, value = part.strip().split(':', 1)
            patient_data[key.strip()] = value.strip()

    return patient_data


def validate_patient_info(patient_data: Dict[str, str]) -> Dict[str, str]:
    """
    Validate patient information

    Args:
        patient_data: Patient data dictionary

    Returns:
        Dict with validation errors (empty if valid)
    """
    errors = {}

    # Required fields
    required_fields = ['name', 'email', 'phone']
    for field in required_fields:
        if field not in patient_data or not patient_data[field]:
            errors[field] = f"{field} is required"

    # Email validation (basic)
    if 'email' in patient_data and patient_data['email']:
        if '@' not in patient_data['email'] or '.' not in patient_data['email']:
            errors['email'] = "Invalid email format"

    # Phone validation (basic)
    if 'phone' in patient_data and patient_data['phone']:
        phone = patient_data['phone'].replace(' ', '').replace('-', '')
        if not phone.isdigit() or len(phone) < 10:
            errors['phone'] = "Invalid phone number format"

    return errors


def format_slot_info(slot: Dict[str, Any]) -> str:
    """Format slot information for display"""
    shift_name = {
        'MORNING': 'Buổi sáng (7:00-11:00)',
        'AFTERNOON': 'Buổi chiều (13:00-17:00)'
    }.get(slot.get('shift', ''), slot.get('shift', 'N/A'))

    return f"""
📅 Ngày: {slot.get('date', 'N/A')}
🕐 Ca: {shift_name}
🏥 Gói khám: {slot.get('package_name', 'N/A')}
👥 Còn trống: {slot.get('remaining', 0)} chỗ
💰 Giá: {format_currency(slot.get('price', 0))}
    """.strip()


def format_doctor_info(doctor: Dict[str, Any]) -> str:
    """Format doctor information for display"""
    return f"""
👨‍⚕️ Bác sĩ: {doctor.get('name', 'N/A')}
📧 Email: {doctor.get('email', 'N/A')}
📞 Điện thoại: {doctor.get('phone', 'N/A')}
🏥 Chuyên khoa: {doctor.get('departmentName', 'N/A')}
📝 Mô tả: {doctor.get('description', 'Không có mô tả')}
✅ Trạng thái: {'Đang hoạt động' if doctor.get('active', True) else 'Tạm nghỉ'}
    """.strip()


def format_package_info(package: Dict[str, Any]) -> str:
    """Format medical package information for display"""
    return f"""
📦 Gói khám: {package.get('name', 'N/A')}
💰 Giá: {format_currency(package.get('price', 0))}
📝 Mô tả: {package.get('description', 'Không có mô tả')}
    """.strip()


def extract_booking_info_from_response(response: str) -> Dict[str, Any]:
    """Extract booking information from agent response"""
    booking_info = {
        'booking_id': None,
        'patient_name': None,
        'appointment_date': None,
        'appointment_time': None,
        'package': None
    }

    lines = response.split('\n')
    for line in lines:
        line = line.strip()
        if 'Mã booking:' in line:
            booking_info['booking_id'] = line.split(':', 1)[1].strip()
        elif '👤 Bệnh nhân:' in line:
            booking_info['patient_name'] = line.split(':', 1)[1].strip()

    return booking_info


def create_success_response(message: str, data: Dict[str, Any] = None) -> Dict[str, Any]:
    """Create standardized success response"""
    response = {
        "success": True,
        "message": message,
        "timestamp": get_current_timestamp()
    }
    if data:
        response["data"] = data
    return response


def create_error_response(message: str, error_code: str = None, details: Dict[str, Any] = None) -> Dict[str, Any]:
    """Create standardized error response"""
    response = {
        "success": False,
        "message": message,
        "timestamp": get_current_timestamp()
    }
    if error_code:
        response["error_code"] = error_code
    if details:
        response["details"] = details
    return response


def get_current_timestamp() -> str:
    """Get current timestamp in ISO format"""
    from datetime import datetime
    return datetime.now().isoformat()


def sanitize_input(text: str) -> str:
    """Sanitize user input to prevent injection attacks"""
    # Basic sanitization - remove potentially dangerous characters
    import re
    # Remove script tags and other dangerous content
    text = re.sub(r'<[^>]+>', '', text)
    # Remove excessive whitespace
    text = ' '.join(text.split())
    return text.strip()


def truncate_text(text: str, max_length: int = 500) -> str:
    """Truncate text to maximum length"""
    if len(text) <= max_length:
        return text
    return text[:max_length - 3] + "..."


def calculate_age(birth_date: str) -> int:
    """Calculate age from birth date (YYYY-MM-DD)"""
    from datetime import datetime
    try:
        birth = datetime.strptime(birth_date, '%Y-%m-%d')
        today = datetime.now()
        age = today.year - birth.year - ((today.month, today.day) < (birth.month, birth.day))
        return age
    except ValueError:
        return 0
