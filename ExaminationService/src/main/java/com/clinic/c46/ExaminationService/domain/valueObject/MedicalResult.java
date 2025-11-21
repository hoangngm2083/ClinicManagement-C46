package com.clinic.c46.ExaminationService.domain.valueObject;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(of = "serviceId")
public class MedicalResult {
    private String doctorId;
    private String serviceId;
    private String data;
    private String pdfUrl;
    private String status;
}