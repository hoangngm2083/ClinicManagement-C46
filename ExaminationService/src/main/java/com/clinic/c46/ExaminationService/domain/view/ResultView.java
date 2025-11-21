package com.clinic.c46.ExaminationService.domain.view;

import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.Column;
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
    private String data;
    private String pdfUrl;
    private String status;
    private String doctorName;
}
