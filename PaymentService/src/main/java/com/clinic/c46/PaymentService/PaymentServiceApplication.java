package com.clinic.c46.PaymentService;

import com.clinic.c46.CommonService.config.CommonAxonConfig;
import com.clinic.c46.CommonService.config.CommonServiceConfig;
import com.clinic.c46.CommonService.exception.BaseGlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import({BaseGlobalExceptionHandler.class, CommonAxonConfig.class, CommonServiceConfig.class})
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}
