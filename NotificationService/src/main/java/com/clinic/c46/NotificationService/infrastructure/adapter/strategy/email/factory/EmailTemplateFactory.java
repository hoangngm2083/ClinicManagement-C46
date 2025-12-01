package com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.factory;

import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.template.EmailTemplate;
import com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.template.variables.EmailTemplateVariables;

public interface EmailTemplateFactory {
    String renderTemplate(EmailTemplate template, EmailTemplateVariables variables);
}
