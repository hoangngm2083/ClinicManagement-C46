package com.clinic.c46.AuthService.infrastructure.adapter.in.web;


import com.clinic.c46.AuthService.domain.event.EmailVerificationPatientRepliedEvent;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/otp")

public class OtpVerificationController {

    private final EventGateway eventGateway;

    @GetMapping("/email")
    public ResponseEntity<String> emailOtpReceive(@RequestParam String verificationId, @RequestParam String code) {
        eventGateway.publish(EmailVerificationPatientRepliedEvent.builder()
                .verificationCode(code)
                .verificationId(verificationId)
                .build());
        return ResponseEntity.ok()
                .body("Đã nhận mã OTP");
    }


}
