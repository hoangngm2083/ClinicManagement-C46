package com.clinic.c46.NotificationService.domain.type;

import com.clinic.c46.NotificationService.application.service.EmailTemplateVariables;
import com.clinic.c46.NotificationService.application.dto.EmailVerificationTemplateVariables;
import com.clinic.c46.NotificationService.application.dto.ExamResultTemplateVariables;
import com.clinic.c46.NotificationService.application.dto.InvoiceTemplateVariables;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailTemplate {
    EMAIL_VERIFICATION("email-verification", "Xác thực email", EmailVerificationTemplateVariables.class),
    INVOICE_RECEIPT("invoice-receipt", "Hóa đơn thanh toán", InvoiceTemplateVariables.class),
    EXAM_RESULT("exam-result", "Kết quả khám bệnh", ExamResultTemplateVariables.class);

    private final String fileName;
    private final String subject;
    private final Class<? extends EmailTemplateVariables> variableClass;
}
