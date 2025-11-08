package com.clinic.c46.NotificationService.application.service;

import com.clinic.c46.NotificationService.domain.type.EmailContentType;

public interface EmailSender {

    void sendEmail(String to, String subject, String content, EmailContentType emailContentType);
}
