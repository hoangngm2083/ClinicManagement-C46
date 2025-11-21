package com.clinic.c46.CommonService.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.axonframework.config.ConfigurationScopeAwareProvider;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.SimpleDeadlineManager;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.spring.messaging.unitofwork.SpringTransactionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@PropertySource("classpath:application-common.properties")
@EnableScheduling
public class CommonAxonConfig {
    @Bean
    @Primary
    public Serializer serializer(ObjectMapper objectMapper) {
        // Tạo một bản copy của ObjectMapper để cấu hình độc lập
        ObjectMapper copyObjectMapper = objectMapper.copy();

        // **QUAN TRỌNG:** Thêm Default Typing để Jackson thêm trường "@class"
        // vào JSON/Payload cho các đối tượng và List.
        // Điều này giúp Axon biết kiểu Generic thực sự của các phần tử trong List.
        return JacksonSerializer.builder()
                .objectMapper(copyObjectMapper)
                .defaultTyping()
                .lenientDeserialization()
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "axon.deadline.enabled", havingValue = "true", matchIfMissing = true)
    public DeadlineManager deadlineManager(SpringTransactionManager transactionManager,
            org.axonframework.config.Configuration configuration) {

        return SimpleDeadlineManager.builder()
                .scopeAwareProvider(new ConfigurationScopeAwareProvider(configuration))
                .transactionManager(transactionManager)
                .build();
    }


}
