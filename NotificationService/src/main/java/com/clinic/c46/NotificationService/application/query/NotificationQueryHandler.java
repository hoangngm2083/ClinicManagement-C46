package com.clinic.c46.NotificationService.application.query;

import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class NotificationQueryHandler {
    private final QueryGateway queryGateway;
}
