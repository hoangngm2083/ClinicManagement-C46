package com.clinic.c46.NotificationService.application.command;

import com.clinic.c46.CommonService.command.notification.SendOTPVerificationCommand;
import com.clinic.c46.NotificationService.application.service.EmailSender;
import com.clinic.c46.NotificationService.application.service.EmailTemplateFactory;
import com.clinic.c46.NotificationService.application.service.EmailVerificationTemplateVariables;
import com.clinic.c46.NotificationService.domain.type.EmailContentType;
import com.clinic.c46.NotificationService.domain.type.EmailTemplate;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class NotificationCommandHandler {
    private final EmailSender emailSender;
    private final EmailTemplateFactory emailTemplateService;

    @CommandHandler
    public void handle(SendOTPVerificationCommand command) {
        EmailVerificationTemplateVariables vars = EmailVerificationTemplateVariables.builder()
                .callbackUrl(command.callbackUrl())
                .build();
        String html = emailTemplateService.renderTemplate(EmailTemplate.VERIFY_EMAIL, vars);
        emailSender.sendEmail(command.to(), EmailTemplate.VERIFY_EMAIL.getSubject(), html, EmailContentType.HTML);
    }
}
