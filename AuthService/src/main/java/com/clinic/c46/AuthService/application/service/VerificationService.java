package com.clinic.c46.AuthService.application.service;

import org.springframework.stereotype.Service;

@Service
public class VerificationService {
    private final int MAX = 999999;
    private final int MIN = 100000;
    private final int RANGE = MAX - MIN + 1;

    public String generateOTP() {
        return String.valueOf(Math.random() * this.RANGE);
    }
}
