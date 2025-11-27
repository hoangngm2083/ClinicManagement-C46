package com.clinic.c46.NotificationService.application.service.email;

public interface EmailSender {

    void sendEmail(String to, String subject, String content, EmailContentType emailContentType);
}
