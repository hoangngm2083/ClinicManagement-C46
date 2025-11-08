package com.clinic.c46.AuthService.domain.event;

import lombok.Builder;

@Builder
public record EmailVerificationPatientRepliedEvent(String verificationCode, String verificationId) {
}
