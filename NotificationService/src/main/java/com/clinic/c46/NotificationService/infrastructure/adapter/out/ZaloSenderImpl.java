package com.clinic.c46.NotificationService.infrastructure.adapter.out;

import com.clinic.c46.NotificationService.application.port.out.ZaloSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ZaloSenderImpl implements ZaloSender {
//    private final WebClient webClient = WebClient.builder()
//            .baseUrl("https://openapi.zalo.me/v3.0")
//            .build();
//
//    public Mono<Void> sendMessage(String accessToken, String userId, String message) {
//        return webClient.post()
//                .uri("/oa/message")
//                .header("access_token", accessToken)
//                .bodyValue(Map.of(
//                        "recipient", Map.of("user_id", userId),
//                        "message", Map.of("text", message)
//                                 ))
//                .retrieve()
//                .bodyToMono(Void.class);
//    }
}

