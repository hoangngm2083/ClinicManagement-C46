package com.clinic.c46.CommonService.command.auth;


import lombok.Builder;

@Builder
public record VerifyPhoneCommand(String causalId, String phone) {
}
