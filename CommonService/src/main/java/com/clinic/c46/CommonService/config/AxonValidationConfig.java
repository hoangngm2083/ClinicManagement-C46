package com.clinic.c46.CommonService.config;

import jakarta.validation.ValidatorFactory;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.messaging.interceptors.BeanValidationInterceptor;
import org.axonframework.queryhandling.QueryBus;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AxonValidationConfig {

    public AxonValidationConfig(CommandBus commandBus, QueryBus queryBus, ValidatorFactory validator) {
        commandBus.registerDispatchInterceptor(new BeanValidationInterceptor<>(validator));
        queryBus.registerDispatchInterceptor(new BeanValidationInterceptor<>(validator));
    }
}