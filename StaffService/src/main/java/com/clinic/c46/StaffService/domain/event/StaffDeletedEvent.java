package com.clinic.c46.StaffService.domain.event;


import lombok.Builder;

@Builder
public record StaffDeletedEvent(String staffId) {
}
