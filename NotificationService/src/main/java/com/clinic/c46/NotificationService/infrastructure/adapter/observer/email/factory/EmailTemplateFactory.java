package com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.factory;

import com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.template.EmailTemplate;
import com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.template.variables.EmailTemplateVariables;

public interface EmailTemplateFactory {
    String renderTemplate(EmailTemplate template, EmailTemplateVariables variables);
}
