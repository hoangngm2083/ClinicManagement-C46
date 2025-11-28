package com.clinic.c46.NotificationService.application.service.email;

import com.clinic.c46.NotificationService.application.service.email.variables.EmailTemplate;
import com.clinic.c46.NotificationService.application.service.email.variables.EmailTemplateVariables;

public interface EmailTemplateFactory {
    String renderTemplate(EmailTemplate template, EmailTemplateVariables variables);
}
