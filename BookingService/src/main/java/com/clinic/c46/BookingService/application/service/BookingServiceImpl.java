package com.clinic.c46.BookingService.application.service;

import com.clinic.c46.BookingService.domain.command.LockSlotCommand;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final CommandGateway commandGateway;

    private final QueryGateway queryGateway;

    @Override
    public void lockSlot(LockSlotCommand cmd) {

        commandGateway.send(cmd);

    }

}
