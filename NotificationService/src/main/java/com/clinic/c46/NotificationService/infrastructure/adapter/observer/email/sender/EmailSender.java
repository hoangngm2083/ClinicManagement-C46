package com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.sender;

import com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.EmailContentType;

public interface EmailSender {

    void sendEmail(String to, String subject, String content, EmailContentType emailContentType);
}
