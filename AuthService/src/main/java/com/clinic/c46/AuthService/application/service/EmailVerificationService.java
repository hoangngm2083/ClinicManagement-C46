package com.clinic.c46.AuthService.application.service;

public interface EmailVerificationService {
    String generateOTP();

    boolean validateOTP(String otpClient, String otpServer);

    String buildCallbackUrl(String verificationId, String otp);

}
