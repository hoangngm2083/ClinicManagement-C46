package com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.view;

import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "exam_view")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Slf4j
@AllArgsConstructor
public class ExamView extends BaseView implements Serializable {

    @Id
    private String id;
    private String patientId;
    private String patientName;
    private String patientEmail;
    private String medicalFormId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "exam_result_view", joinColumns = @JoinColumn(name = "exam_id"))
    @Builder.Default
    private Set<ResultView> results = new HashSet<>();


    public void addResultView(ResultView resultView) {
        if (this.results.contains(resultView)) {
            log.info("[examination.view.ExamView.addResultView.existed]: {}", resultView.toString());
            return;
        }
        this.results.add(resultView);
    }

    public void removeResultView(String serviceId) {
        this.results.removeIf(r -> r.getServiceId()
                .equals(serviceId));
    }

    public boolean containsResult(ResultView resultView) {
        return this.results.stream()
                .anyMatch(result -> result.equals(resultView));
    }

    public boolean containsServiceId(String serviceId) {
        return this.results.stream()
                .anyMatch(result -> result.getServiceId()
                        .equals(serviceId));
    }


}
