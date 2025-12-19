package com.clinic.c46.MedicalPackageService.domain.view;

import com.clinic.c46.CommonService.converter.JsonNodeConverter;
import com.clinic.c46.CommonService.domain.BaseView;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    @Column(columnDefinition = "TEXT")
    private String description;
    private String departmentName;
    private String departmentId;
    private int processingPriority;
    @Column(columnDefinition = "TEXT")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode formTemplate;
}
