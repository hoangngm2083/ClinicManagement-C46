package com.clinic.c46.CommonService.command.auth;


import lombok.Builder;

@Builder
public record VerifyEmailCommand(String verificationId, String email) {
}
