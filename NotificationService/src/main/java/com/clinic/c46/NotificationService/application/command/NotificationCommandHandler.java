package com.clinic.c46.NotificationService.application.command;

import com.clinic.c46.NotificationService.application.port.out.EmailSender;
import com.clinic.c46.NotificationService.application.port.out.SSEPublisher;
import com.clinic.c46.NotificationService.application.port.out.ZaloSender;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class NotificationCommandHandler {
    private final SSEPublisher emitterHandler;
    private final EmailSender emailSender;
    private final ZaloSender zaloSender;
    private final CommandGateway commandGateway;
    private final EventGateway eventGateway;

}
