package com.clinic.c46.CommonService.event.auth;

import lombok.Builder;

@Builder
public record EmailVerificationFailedEvent(String verificationId, String email, String reason) {
}
