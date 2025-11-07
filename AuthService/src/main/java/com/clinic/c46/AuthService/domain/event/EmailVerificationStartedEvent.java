package com.clinic.c46.AuthService.domain.event;


import lombok.Builder;

@Builder
public record EmailVerificationStartedEvent(String verificationId, String email, String code) {
}
