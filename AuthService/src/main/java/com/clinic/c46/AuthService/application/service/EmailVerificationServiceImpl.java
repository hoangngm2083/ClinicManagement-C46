package com.clinic.c46.AuthService.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service

public class EmailVerificationServiceImpl implements EmailVerificationService {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int LOWER = 100_000;       // inclusive
    private static final int UPPER_EXCLUSIVE = 1_000_000; // exclusive

    @Value("${app.public-base-url}")
    private String publicBaseUrl;


    @Override
    public String generateOTP() {
        int otp = secureRandom.nextInt(UPPER_EXCLUSIVE - LOWER) + LOWER;
        return Integer.toString(otp);
    }

    @Override
    public boolean validateOTP(String otpClient, String otpServer) {
        return otpServer.equals(otpClient);
    }

    @Override
    public String buildCallbackUrl(String verificationId, String otp) {
        return publicBaseUrl + "/email-verification?" + "verificationId=" + verificationId + "&code=" + otp;

    }


}