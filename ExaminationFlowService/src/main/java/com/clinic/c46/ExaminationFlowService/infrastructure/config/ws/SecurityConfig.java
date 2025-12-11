package com.clinic.c46.ExaminationFlowService.infrastructure.config.ws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // TODO: 2-layer security
    // private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // @Autowired via constructor

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http.csrf(AbstractHttpConfigurer::disable)
//                .authorizeHttpRequests(auth -> auth
//                        // Enable WebSocket Handshake
//                        .requestMatchers("/ws/exam-workflow/**")
//                        .permitAll()
//
//                        // Enable REST APIs (Trust Gateway)
//                        .requestMatchers("/**")
//                        .permitAll()
//
//                        // ==========================================================================================
//                        // 1. Unlock Swagger/OpenAPI paths (REQUIRED)
//                        // ==========================================================================================
//                        .requestMatchers(
//                                // OpenAPI file main path (JSON/YAML)
//                                "/v3/api-docs/**",
//
//                                // Static files and UI (CSS/JS)
//                                "/swagger-ui/**", "/swagger-ui.html",
//
//                                // Other Swagger resources (depending on version)
//                                "/webjars/**")
//                        .permitAll() // <--- Allow public access
//
//                        // ====================================================================
//
//                        // Block all remaining requests
//
//                        .anyRequest()
//                        .denyAll());
//
//        return http.build();
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Vô hiệu hóa toàn bộ CSRF nếu bạn không cần bảo vệ cho các API REST (KHÔNG NÊN DÙNG)
                // http.csrf(AbstractHttpConfigurer::disable)

                // 1. CHỈ VÔ HIỆU HÓA CSRF CHO ENDPOINT CỦA WEBSOCKET
                .csrf(csrf -> csrf
                        // Vô hiệu hóa CSRF cho tất cả các API REST (/**)
                        .ignoringRequestMatchers("/**")
                        // Đảm bảo vẫn bỏ qua cho WebSocket
                        .ignoringRequestMatchers("/ws/exam-workflow/**"))
                // 2. Vô hiệu hóa Http Basic và Form Login mặc định (nếu không dùng)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // 3. CẤP QUYỀN TRUY CẬP CHO CHÍNH ENDPOINT ĐÃ BỎ QUA CSRF
                        .requestMatchers("/ws/exam-workflow/**")
                        .permitAll()

                        // Cấp quyền cho các API REST và Swagger (giữ nguyên)
                        .requestMatchers("/**", "/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/swagger-resources/**", "/webjars/**")
                        .permitAll()

                        // 4. CHẶN TẤT CẢ CÁC REQUEST KHÁC
                        .anyRequest()
                        .denyAll());

        return http.build();
    }

//    @Bean
//    public CorsFilter corsFilter() {
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        CorsConfiguration config = new CorsConfiguration();
//
//        // THAY ĐỔI QUAN TRỌNG 1: Dùng addAllowedOriginPattern thay vì addAllowedOrigin
//        // Điều này cho phép dùng "*" ngay cả khi allowCredentials = true
//        config.addAllowedOriginPattern("*");
//
//        config.addAllowedHeader("*");
//        config.addAllowedMethod("*");
//
//        // THAY ĐỔI QUAN TRỌNG 2: Bắt buộc phải là true cho SockJS/WebSocket
//        config.setAllowCredentials(true);
//
//        source.registerCorsConfiguration("/**", config);
//        return new CorsFilter(source);
//    }
//
//    @Bean
//    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsFilter corsFilter) {
//        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(corsFilter);
//        registration.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE); // Quan trọng: Đảm bảo chạy đầu tiên
//        return registration;
//    }

}