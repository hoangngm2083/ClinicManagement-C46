package com.clinic.c46.ExaminationService.application.handler;

import com.clinic.c46.CommonService.command.notification.SendExamResultEmailCommand;
import com.clinic.c46.CommonService.event.examination.ExaminationCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExamEventHandler {

    private final CommandGateway commandGateway;

    @EventHandler
    public void on(ExaminationCompletedEvent event) {
        log.info("[ExamEventHandler] Examination completed: {}. Sending email to: {}", event.examinationId(),
                event.patientEmail());

        if (event.patientEmail() == null || event.patientEmail().isEmpty()) {
            log.warn("[ExamEventHandler] Patient email is missing for examination: {}", event.examinationId());
            return;
        }

        String notificationId = UUID.randomUUID().toString();
        SendExamResultEmailCommand command = SendExamResultEmailCommand.builder()
                .notificationId(notificationId)
                .examinationId(event.examinationId())
                .recipientEmail(event.patientEmail())
                .build();

        commandGateway.send(command)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("[ExamEventHandler] Failed to send email command for examination: {}",
                                event.examinationId(), throwable);
                    } else {
                        log.info("[ExamEventHandler] Sent email command for examination: {}", event.examinationId());
                    }
                });
    }
}
