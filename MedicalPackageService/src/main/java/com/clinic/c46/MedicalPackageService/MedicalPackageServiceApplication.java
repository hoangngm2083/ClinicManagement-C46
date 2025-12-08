package com.clinic.c46.MedicalPackageService;

import com.clinic.c46.CommonService.config.CommonAxonConfig;
import com.clinic.c46.CommonService.config.CommonRetryConfig;
import com.clinic.c46.CommonService.config.CommonServiceConfig;
import com.clinic.c46.CommonService.exception.BaseGlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({BaseGlobalExceptionHandler.class, CommonAxonConfig.class, CommonServiceConfig.class, CommonRetryConfig.class})
public class MedicalPackageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalPackageServiceApplication.class, args);
    }

}
