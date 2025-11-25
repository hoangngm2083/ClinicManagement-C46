package com.clinic.c46.AuthService.domain.event;

import lombok.Builder;

@Builder
public record AccountCreatedEvent(
        String accountId,
        String accountName,
        String staffId,
        String role) {
}
