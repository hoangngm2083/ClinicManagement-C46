package com.clinic.c46.ExaminationFlowService.infrastructure.config.ws;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    // TODO: sửa lại command gateway -> gửi command verify token
    // Giả định JwtService của bạn có phương thức để xác thực và trích xuất ID
//    private final JwtService jwtService;

    // @Autowired qua constructor
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Chỉ xử lý thông điệp CONNECT (thông điệp đầu tiên khi thiết lập WS)
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

//            // 1. Lấy JWT từ Header STOMP (hoặc Query parameter nếu bạn dùng HandshakeInterceptor)
//            // Client thường gửi JWT qua header "Authorization" trong frame CONNECT
            String token = accessor.getFirstNativeHeader("Authorization");
//
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
//
//            if (token != null && jwtService.validateToken(token)) {
//
//                // 2. Trích xuất Doctor ID (Username) từ JWT
//                String doctorId = jwtService.extractUsername(token);

            // 3. Tạo đối tượng Authentication (Principal)
            Authentication auth = new UsernamePasswordAuthenticationToken(token, // doctorId,
                    null, // Không cần credentials/password
                    null  // Danh sách quyền hạn (Authorities)
            );

            // 4. Gắn Principal vào Session
            accessor.setUser(auth);
//
//                // Sau bước này, principal.getName() sẽ trả về doctorId
//            } else {
//                // Nếu token không hợp lệ, bạn có thể ném ngoại lệ hoặc từ chối kết nối
//                // throw new MessagingException("Unauthorized");
        }
        return message;
    }


}
