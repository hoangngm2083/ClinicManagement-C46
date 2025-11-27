package com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projection;

import com.clinic.c46.CommonService.domain.BaseView;
import com.clinic.c46.CommonService.converter.JsonNodeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "medical_service_rep")
@EqualsAndHashCode(callSuper = false, of = "id")
@NoArgsConstructor
@Setter
@Getter
@SuperBuilder
public class ServiceRepView extends BaseView {
    @Id
    private String id;
    
    private String name;
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode formTemplate;
}
