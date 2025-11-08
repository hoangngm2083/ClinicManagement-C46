package com.clinic.c46.NotificationService.application.service;

import com.clinic.c46.NotificationService.domain.type.EmailTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailTemplateFactoryImpl implements EmailTemplateFactory {

    private final TemplateEngine templateEngine;

    public String renderTemplate(EmailTemplate template, EmailTemplateVariables variables) {
        Context context = new Context();
        context.setVariables(variables.toMap());

        String templatePath = "email/" + template.getFileName(); // e.g. "email/email-verification"
        return templateEngine.process(templatePath, context);
    }
}
