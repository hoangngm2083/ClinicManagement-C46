package com.clinic.c46.MedicalPackageService.domain.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Convert;
import com.fasterxml.jackson.databind.JsonNode;
import com.clinic.c46.CommonService.converter.JsonNodeConverter;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "medical_service")
public class MedicalServiceView {

    @Id
    private String id;
    private String name;
    private String description;
    private String departmentName;
    private String departmentId;
    private int processingPriority;
    @Column(columnDefinition = "TEXT")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode formTemplate;
}
