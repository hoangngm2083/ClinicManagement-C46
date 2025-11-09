package com.clinic.c46.StaffService;

import com.clinic.c46.CommonService.config.CommonAxonConfig;
import com.clinic.c46.CommonService.exception.BaseGlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({CommonAxonConfig.class, BaseGlobalExceptionHandler.class})
public class StaffServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StaffServiceApplication.class, args);
    }

}
