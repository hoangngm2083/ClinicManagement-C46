package com.clinic.c46.MedicalPackageService.domain.view;

import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Convert;
import com.fasterxml.jackson.databind.JsonNode;
import com.clinic.c46.CommonService.converter.JsonNodeConverter;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "medical_service")
public class MedicalServiceView extends BaseView {

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
