package com.clinic.c46.NotificationService.infrastructure.adapter.handler.command;

import com.clinic.c46.CommonService.command.notification.RemindAppointmentCommand;
import com.clinic.c46.CommonService.command.notification.SendAppointmentInfoCommand;
import com.clinic.c46.CommonService.command.notification.SendExamResultEmailCommand;
import com.clinic.c46.CommonService.command.notification.SendInvoiceEmailCommand;
import com.clinic.c46.CommonService.command.notification.SendOTPVerificationCommand;
import com.clinic.c46.CommonService.dto.ExamDetailsDto;
import com.clinic.c46.CommonService.dto.InvoiceDetailsDto;
import com.clinic.c46.CommonService.query.examination.GetExamDetailsByIdQuery;
import com.clinic.c46.CommonService.query.invoice.GetInvoiceDetailsByIdQuery;
import com.clinic.c46.CommonService.query.appointment.GetAppointmentDetailsByIdQuery;
import com.clinic.c46.CommonService.dto.AppointmentDetailsDto;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.factory.EmailTemplateFactory;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.parser.FormTemplateParser;
import com.clinic.c46.NotificationService.application.service.notification.NotificationSenderService;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.template.EmailTemplate;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.template.variables.EmailVerificationTemplateVariables;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.template.variables.ExamResultTemplateVariables;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.template.variables.InvoiceTemplateVariables;
import com.clinic.c46.NotificationService.infrastructure.adapter.exception.DataNotFoundRetryableException;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.template.variables.AppointmentReminderTemplateVariables;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.template.variables.AppointmentTemplateVariables;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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

    private final NotificationSenderService notificationSenderService;
    private final EmailTemplateFactory emailTemplateFactory;
    private final QueryGateway queryGateway;
    private final FormTemplateParser formTemplateParser;

    @CommandHandler
    public void handle(SendOTPVerificationCommand command) {
        EmailVerificationTemplateVariables vars = EmailVerificationTemplateVariables.builder()
                .callbackUrl(command.callbackUrl())
                .build();
        String html = emailTemplateFactory.renderTemplate(EmailTemplate.EMAIL_VERIFICATION, vars);
        notificationSenderService.sendEmail(command.to(), command.to(), EmailTemplate.EMAIL_VERIFICATION.getSubject(), html);
    }

    @CommandHandler
    @Retryable(retryFor = DataNotFoundRetryableException.class, maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(maxDelayExpression = "${retry.maxDelay}"))
    public void handle(SendInvoiceEmailCommand command) {
        log.info("[NotificationCommandHandler] Handling SendInvoiceEmailCommand for invoice: {}", command.invoiceId());

        try {
            // Query invoice details
            GetInvoiceDetailsByIdQuery query = new GetInvoiceDetailsByIdQuery(command.invoiceId());
            Optional<InvoiceDetailsDto> invoiceOpt = queryGateway.query(query,
                            ResponseTypes.optionalInstanceOf(InvoiceDetailsDto.class))
                    .join();

            if (invoiceOpt.isEmpty()) {
                log.warn("[NotificationCommandHandler] Invoice not found: {}, will retry", command.invoiceId());
                throw new DataNotFoundRetryableException("Invoice not found: " + command.invoiceId());
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
            notificationSenderService.sendEmail(command.recipientEmail(), command.recipientEmail(), EmailTemplate.INVOICE_RECEIPT.getSubject(), htmlContent);

            log.info("[NotificationCommandHandler] Invoice email sent successfully to: {}", command.recipientEmail());

        } catch (Exception e) {
            log.error("[NotificationCommandHandler] Failed to send invoice email for: {}", command.invoiceId(), e);
        }
    }

    @CommandHandler
    @Retryable(retryFor = DataNotFoundRetryableException.class, maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(maxDelayExpression = "${retry.maxDelay}"))
    public void handle(SendExamResultEmailCommand command) {


        try {


            GetExamDetailsByIdQuery query = new GetExamDetailsByIdQuery(command.examinationId());
            Optional<ExamDetailsDto> examOpt = queryGateway.query(query,
                            ResponseTypes.optionalInstanceOf(ExamDetailsDto.class))
                    .join();

            if (examOpt.isEmpty()) {
                log.warn("[NotificationCommandHandler] Exam not found: {}, will retry", command.examinationId());
                throw new DataNotFoundRetryableException("Exam not found: " + command.examinationId());
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
            notificationSenderService.sendEmail(exam.patientEmail(), exam.patientEmail(), EmailTemplate.EXAM_RESULT.getSubject(), htmlContent);

            log.info("[NotificationCommandHandler] Exam result email sent successfully to: {}", exam.patientEmail());

        } catch (Exception e) {
            log.error("[NotificationCommandHandler] Failed to send exam result email for: {}", command.examinationId(),
                    e);
        }
    }

    @CommandHandler
    @Retryable(retryFor = DataNotFoundRetryableException.class, maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(maxDelayExpression = "${retry.maxDelay}"))
    public void handle(SendAppointmentInfoCommand command) {
        log.info("[NotificationCommandHandler] Handling SendAppointmentInfoCommand for appointment: {}",
                command.appointmentId());

        try {
            // Query appointment details
            GetAppointmentDetailsByIdQuery query = new GetAppointmentDetailsByIdQuery(command.appointmentId());
            Optional<AppointmentDetailsDto> appointmentOpt = queryGateway.query(query,
                            ResponseTypes.optionalInstanceOf(AppointmentDetailsDto.class))
                    .join();

            if (appointmentOpt.isEmpty()) {
                log.warn("[NotificationCommandHandler] Appointment not found: {}, will retry", command.appointmentId());
                throw new DataNotFoundRetryableException("Appointment not found: " + command.appointmentId());
            }

            AppointmentDetailsDto appointmentDetails = appointmentOpt.get();

            // Build list of service items
            List<AppointmentTemplateVariables.ServiceItem> serviceItems = appointmentDetails.getServices()
                    .stream()
                    .map(service -> AppointmentTemplateVariables.ServiceItem.builder()
                            .name(service.getName())
                            .build())
                    .collect(Collectors.toList());

            // Build template variables
            AppointmentTemplateVariables variables = AppointmentTemplateVariables.builder()
                    .patientName(appointmentDetails.getPatientName())
                    .appointmentId(appointmentDetails.getId())
                    .appointmentDate(appointmentDetails.getDate()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .shift(appointmentDetails.getShift())
                    .appointmentState(appointmentDetails.getState())
                    .medicalPackageName(appointmentDetails.getMedicalPackageName())
                    .services(serviceItems)
                    .build();

            // Render template
            String htmlContent = emailTemplateFactory.renderTemplate(
                    EmailTemplate.APPOINTMENT_CONFIRMATION, variables);

            // Send email
            notificationSenderService.sendEmail(appointmentDetails.getPatientEmail(),
                    appointmentDetails.getPatientEmail(), EmailTemplate.APPOINTMENT_CONFIRMATION.getSubject(), htmlContent);

            log.info("[NotificationCommandHandler] Appointment email sent successfully to: {}",
                    appointmentDetails.getPatientEmail());

        } catch (Exception e) {
            log.error("[NotificationCommandHandler] Failed to send appointment email for: {}",
                    command.appointmentId(), e);
        }
    }

    @CommandHandler
    @Retryable(retryFor = DataNotFoundRetryableException.class, maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(maxDelayExpression = "${retry.maxDelay}"))
    public void handle(RemindAppointmentCommand command) {
        log.info("[NotificationCommandHandler] Handling RemindAppointmentCommand for appointment: {}",
                command.appointmentId());

        try {
            // Query appointment details
            GetAppointmentDetailsByIdQuery query = new GetAppointmentDetailsByIdQuery(command.appointmentId());
            Optional<AppointmentDetailsDto> appointmentOpt = queryGateway.query(query,
                            ResponseTypes.optionalInstanceOf(AppointmentDetailsDto.class))
                    .join();

            if (appointmentOpt.isEmpty()) {
                log.warn("[NotificationCommandHandler] Appointment not found: {}, will retry", command.appointmentId());
                throw new DataNotFoundRetryableException("Appointment not found: " + command.appointmentId());
            }

            AppointmentDetailsDto reminderAppointment = appointmentOpt.get();

            // Build template variables (preparation instructions are now in HTML template)
            AppointmentReminderTemplateVariables variables = AppointmentReminderTemplateVariables.builder()
                    .patientName(reminderAppointment.getPatientName())
                    .appointmentDate(reminderAppointment.getDate()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .shift(reminderAppointment.getShift())
                    .medicalPackageName(reminderAppointment.getMedicalPackageName())
                    .build();

            // Render template
            String htmlContent = emailTemplateFactory.renderTemplate(
                    EmailTemplate.APPOINTMENT_REMINDER, variables);

            // Send email
            notificationSenderService.sendEmail(reminderAppointment.getPatientEmail(),
                    reminderAppointment.getPatientEmail(), EmailTemplate.APPOINTMENT_REMINDER.getSubject(), htmlContent);

            log.info("[NotificationCommandHandler] Appointment reminder email sent successfully to: {}",
                    reminderAppointment.getPatientEmail());

        } catch (Exception e) {
            log.error("[NotificationCommandHandler] Failed to send appointment reminder email for: {}",
                    command.appointmentId(), e);
        }
    }

    @Recover
    public void recover(DataNotFoundRetryableException e, SendInvoiceEmailCommand command) {
        log.error("[NotificationCommandHandler] Failed to send invoice email after retries for invoiceId: {}, reason: {}",
                command.invoiceId(), e.getMessage(), e);
    }

    @Recover
    public void recover(DataNotFoundRetryableException e, SendExamResultEmailCommand command) {
        log.error("[NotificationCommandHandler] Failed to send exam result email after retries for examinationId: {}, reason: {}",
                command.examinationId(), e.getMessage(), e);
    }

    @Recover
    public void recover(DataNotFoundRetryableException e, SendAppointmentInfoCommand command) {
        log.error("[NotificationCommandHandler] Failed to send appointment info email after retries for appointmentId: {}, reason: {}",
                command.appointmentId(), e.getMessage(), e);
    }

    @Recover
    public void recover(DataNotFoundRetryableException e, RemindAppointmentCommand command) {
        log.error("[NotificationCommandHandler] Failed to send appointment reminder email after retries for appointmentId: {}, reason: {}",
                command.appointmentId(), e.getMessage(), e);
    }
}
