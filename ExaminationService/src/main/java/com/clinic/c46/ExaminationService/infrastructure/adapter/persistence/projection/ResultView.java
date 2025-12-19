package com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projection;

import com.clinic.c46.CommonService.converter.JsonNodeConverter;
import com.clinic.c46.CommonService.domain.BaseView;
import com.clinic.c46.ExaminationService.domain.valueObject.ResultStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "serviceId", callSuper = false)
public class ResultView extends BaseView implements Serializable {
    private String doctorId;
    private String serviceId;
    @Column(columnDefinition = "TEXT")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode data;
    @Column(columnDefinition = "TEXT")
    private String pdfUrl;
    private ResultStatus status;
    private String doctorName;
}
