package com.clinic.c46.PatientService;

import com.clinic.c46.CommonService.config.CommonAxonConfig;
import com.clinic.c46.CommonService.config.CommonServiceConfig;
import com.clinic.c46.CommonService.exception.BaseGlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({BaseGlobalExceptionHandler.class, CommonAxonConfig.class, CommonServiceConfig.class})
public class PatientServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientServiceApplication.class, args);
    }

}
