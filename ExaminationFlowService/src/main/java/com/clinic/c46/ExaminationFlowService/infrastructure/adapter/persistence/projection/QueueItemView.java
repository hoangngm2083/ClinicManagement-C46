package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.persistence.projection;


import com.clinic.c46.CommonService.domain.BaseView;
import com.clinic.c46.ExaminationFlowService.domain.aggregate.QueueItemStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Entity
@Table(name = "queue_item")
@SuperBuilder
@Getter
@Setter
public class QueueItemView extends BaseView {
    @Id
    private String id;
    private String medicalFormId;
    private String serviceId;
    private String queueId; // Department ID
    private String staffId;
    private QueueItemStatus status; // WAITING, IN_PROGRESS, COMPLETED
}
