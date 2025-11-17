package com.clinic.c46.ExaminationFlowService.infrastructure.config.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Autowired
    private AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Gắn Interceptor vào kênh đến (Inbound) để xử lý xác thực
        registration.interceptors(authChannelInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Entrypoint cho Pub/Sub (Server -> Client)
        // Đây là các kênh dùng để gửi cập nhật trạng thái chung của hàng đợi tới nhiều người dùng (tất cả bác sĩ trong phòng).
        config.enableSimpleBroker("/topic", "/queue");
        // Entrypoint cho Commands (Client -> Server)
        // Đây là các lệnh mà Bác sĩ/Nhân viên gửi để thay đổi trạng thái của hệ thống.
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // HTTP Handshake entrypoint (GET)
        registry.addEndpoint("/ws/exam-workflow")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Là một thư viện JavaScript cung cấp khả năng hỗ trợ dự phòng (fallback options) cho các trình duyệt hoặc mạng không hỗ trợ hoàn toàn WebSocket (ví dụ: sử dụng HTTP Long Polling hoặc Iframe).
    }
}
