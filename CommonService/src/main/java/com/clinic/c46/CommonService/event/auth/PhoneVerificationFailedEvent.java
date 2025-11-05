package com.clinic.c46.CommonService.event.auth;

import lombok.Builder;

@Builder
public record PhoneVerificationFailedEvent(String causalId, String phone, String reason) {
}
