package com.clinic.c46.NotificationService.application.service;

import com.clinic.c46.NotificationService.domain.type.EmailTemplate;

public interface EmailTemplateFactory {
    String renderTemplate(EmailTemplate template, EmailTemplateVariables variables);
}
