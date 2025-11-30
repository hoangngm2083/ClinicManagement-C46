package com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.factory;

import com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.template.EmailTemplate;
import com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.template.variables.EmailTemplateVariables;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
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
