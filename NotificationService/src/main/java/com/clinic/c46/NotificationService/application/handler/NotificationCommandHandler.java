package com.clinic.c46.NotificationService.application.handler;

import com.clinic.c46.CommonService.command.notification.SendExamResultEmailCommand;
import com.clinic.c46.CommonService.command.notification.SendInvoiceEmailCommand;
import com.clinic.c46.CommonService.command.notification.SendOTPVerificationCommand;
import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import com.clinic.c46.CommonService.dto.InvoiceDetailsDto;
import com.clinic.c46.CommonService.query.examination.GetExamDetailsByIdQuery;
import com.clinic.c46.CommonService.query.invoice.GetInvoiceDetailsByIdQuery;
import com.clinic.c46.NotificationService.application.service.email.EmailContentType;
import com.clinic.c46.NotificationService.application.service.email.EmailSender;
import com.clinic.c46.NotificationService.application.service.email.EmailTemplateFactory;
import com.clinic.c46.NotificationService.application.service.email.FormTemplateParser;
import com.clinic.c46.NotificationService.application.service.email.variables.EmailTemplate;
import com.clinic.c46.NotificationService.application.service.email.variables.EmailVerificationTemplateVariables;
import com.clinic.c46.NotificationService.application.service.email.variables.ExamResultTemplateVariables;
import com.clinic.c46.NotificationService.application.service.email.variables.InvoiceTemplateVariables;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandHandler {

    private final EmailSender emailSender;
    private final EmailTemplateFactory emailTemplateFactory;
    private final QueryGateway queryGateway;
    private final FormTemplateParser formTemplateParser;

    @CommandHandler
    public void handle(SendOTPVerificationCommand command) {
        EmailVerificationTemplateVariables vars = EmailVerificationTemplateVariables.builder()
                .callbackUrl(command.callbackUrl())
                .build();
        String html = emailTemplateFactory.renderTemplate(EmailTemplate.EMAIL_VERIFICATION, vars);
        emailSender.sendEmail(command.to(), EmailTemplate.EMAIL_VERIFICATION.getSubject(), html, EmailContentType.HTML);
    }

    @CommandHandler
    public void handle(SendInvoiceEmailCommand command) {
        log.info("[NotificationCommandHandler] Handling SendInvoiceEmailCommand for invoice: {}", command.invoiceId());

        try {
            // Query invoice details
            GetInvoiceDetailsByIdQuery query = new GetInvoiceDetailsByIdQuery(command.invoiceId());
            Optional<InvoiceDetailsDto> invoiceOpt = queryGateway.query(query,
                            ResponseTypes.optionalInstanceOf(InvoiceDetailsDto.class))
                    .join();

            if (invoiceOpt.isEmpty()) {
                log.warn("[NotificationCommandHandler] Invoice not found: {}", command.invoiceId());
                return;
            }
            InvoiceDetailsDto invoice = invoiceOpt.get();
            // Build template variables
            List<InvoiceTemplateVariables.InvoiceItem> items = invoice.medicalPackages()
                    .stream()
                    .map(pkg -> InvoiceTemplateVariables.InvoiceItem.builder()
                            .name(pkg.name())
                            .price(String.format("%,.0f VNĐ", pkg.price()))
                            .build())
                    .collect(Collectors.toList());

            InvoiceTemplateVariables variables = InvoiceTemplateVariables.builder()
                    .patientName(invoice.patientName())
                    .invoiceId(invoice.invoiceId())
                    .paymentDate(LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    .totalAmount(String.format("%,.0f VNĐ", invoice.totalAmount()))
                    .items(items)
                    .build();


            // Render template
            String htmlContent = emailTemplateFactory.renderTemplate(EmailTemplate.INVOICE_RECEIPT, variables);

            // Send email
            emailSender.sendEmail(command.recipientEmail(), EmailTemplate.INVOICE_RECEIPT.getSubject(), htmlContent,
                    EmailContentType.HTML);

            log.info("[NotificationCommandHandler] Invoice email sent successfully to: {}", command.recipientEmail());

        } catch (Exception e) {
            log.error("[NotificationCommandHandler] Failed to send invoice email for: {}", command.invoiceId(), e);
        }
    }

    @CommandHandler
    public void handle(SendExamResultEmailCommand command) {


        try {


            GetExamDetailsByIdQuery query = new GetExamDetailsByIdQuery(command.examinationId());
            Optional<ExamDetailsDto> examOpt = queryGateway.query(query,
                            ResponseTypes.optionalInstanceOf(ExamDetailsDto.class))
                    .join();

            if (examOpt.isEmpty()) {
                log.warn("[NotificationCommandHandler] Exam not found: {}", command.examinationId());
                return;
            }

            ExamDetailsDto exam = examOpt.get();

            // Build list of result items with service name, doctor name, and parsed content
            List<ExamResultTemplateVariables.ResultItem> resultItems = exam.results()
                    .stream()
                    .map(result -> ExamResultTemplateVariables.ResultItem.builder()
                            .serviceName(result.serviceName())
                            .doctorName(result.doctorName())
                            .resultHtmlContent(formTemplateParser.parse(result.serviceFormTemplate(), result.data()))
                            .build())
                    .collect(Collectors.toList());

            ExamResultTemplateVariables variables = ExamResultTemplateVariables.builder()
                    .patientName(exam.patientName())
                    .examinationId(exam.id())
                    .completionDate(LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    .resultItems(resultItems)
                    .build();


            // Render template
            String htmlContent = emailTemplateFactory.renderTemplate(EmailTemplate.EXAM_RESULT, variables);

            // Send email
            emailSender.sendEmail(exam.patientEmail(), EmailTemplate.EXAM_RESULT.getSubject(), htmlContent,
                    EmailContentType.HTML);

            log.info("[NotificationCommandHandler] Exam result email sent successfully to: {}", exam.patientEmail());

        } catch (Exception e) {
            log.error("[NotificationCommandHandler] Failed to send exam result email for: {}", command.examinationId(),
                    e);
        }
    }
}
