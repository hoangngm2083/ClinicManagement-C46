package com.clinic.c46.ExaminationFlowService.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI examinationFlowServiceOpenAPI() {
        Server server = new Server();
        server.setUrl("/");
        server.setDescription("Examination Flow Service API");

        Contact contact = new Contact();
        contact.setName("Clinic Management Team");
        contact.setEmail("support@clinic.com");

        License license = new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html");

        Info info = new Info()
                .title("Examination Flow Service API")
                .version("1.0.0")
                .contact(contact)
                .description("API documentation for Examination Flow Service - manages patient queues, medical forms, and examination workflow")
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}
