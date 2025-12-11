package com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projector;

import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.CommonService.event.examination.ExaminationCompletedEvent;
import com.clinic.c46.CommonService.event.examination.ExaminationCreatedEvent;
import com.clinic.c46.CommonService.event.examination.ResultAddedEvent;
import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.patient.GetPatientByIdQuery;
import com.clinic.c46.ExaminationService.domain.event.ExaminationDeletedEvent;
import com.clinic.c46.ExaminationService.domain.event.ResultRemovedEvent;
import com.clinic.c46.ExaminationService.domain.event.ResultStatusUpdatedEvent;
import com.clinic.c46.ExaminationService.domain.valueObject.ExaminationStatus;
import com.clinic.c46.ExaminationService.domain.valueObject.ResultStatus;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projection.DoctorRepView;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projection.ExamView;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.projection.ResultView;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.repository.DoctorRepViewRepository;
import com.clinic.c46.ExaminationService.infrastructure.adapter.persistence.repository.ExamViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExaminationViewProjector {

    private final ExamViewRepository examViewRepository;
    private final DoctorRepViewRepository doctorViewRepository;
    private final QueryGateway queryGateway;

    @EventHandler
    public void on(ExaminationCreatedEvent event) {
        log.info("[examination.projection.ExaminationCreatedEvent]: {}", event.examinationId());
        ExamView view = ExamView.builder()
                .id(event.examinationId())
                .patientId(event.patientId())
                .medicalFormId(event.medicalFormId())
                .status(ExaminationStatus.PENDING)
                .build();

        GetPatientByIdQuery query = new GetPatientByIdQuery(event.patientId());
        PatientDto patient = queryGateway.query(query, ResponseTypes.instanceOf(PatientDto.class))
                .join();

        if (patient == null) {
            log.warn("[examination.projection.ExaminationCreatedEvent.patient-not-found] patientId: {}",
                    event.patientId());
            throw new IllegalStateException(
                    "[examination.projection.ExaminationCreatedEvent.patient-not-found] patientId: "
                            + event.patientId());
        }

        view.setPatientName(patient.name());
        view.setPatientEmail(patient.email());
        view.markCreated();
        examViewRepository.save(view);
    }

    @EventHandler
    public void on(ResultAddedEvent event) {
        log.info("[examination.projection.ResultAddedEvent] {}", event.serviceId());
        examViewRepository.findById(event.examinationId())
                .filter(view -> !view.containsServiceId(event.serviceId()))
                .ifPresent(view -> addResultToView(view, event));
    }

    private void addResultToView(ExamView view, ResultAddedEvent event) {
        DoctorRepView doctorRepView = getDoctorOrThrow(event.doctorId());
        ResultView resultView = buildResultView(event, doctorRepView.getName());
        resultView.markCreated();
        view.addResultView(resultView);
        view.markUpdated();
        examViewRepository.save(view);
    }

    private DoctorRepView getDoctorOrThrow(String doctorId) {
        return doctorViewRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Bác sĩ (" + doctorId + ")"));
    }

    private ResultView buildResultView(ResultAddedEvent event, String doctorName) {
        return ResultView.builder()
                .serviceId(event.serviceId())
                .doctorId(event.doctorId())
                .data(event.data())
                .pdfUrl(null)
                .status(ResultStatus.CREATED)
                .doctorName(doctorName)
                .build();
    }

    @EventHandler
    public void on(ResultRemovedEvent event) {
        log.info("[examination.projection.ResultRemovedEvent] {}", event.toString());

        examViewRepository.findById(event.examId())
                .ifPresent(view -> {
                    view.removeResultView(event.serviceId());
                    view.markUpdated();
                    examViewRepository.save(view);
                });
    }

    @EventHandler
    public void on(ResultStatusUpdatedEvent event) {
        log.debug("[examination.projection.ResultStatusUpdatedEvent] {}", event.examId());
        examViewRepository.findById(event.examId())
                .ifPresent(view -> {
                    view.getResults()
                            .stream()
                            .filter(r -> r.getServiceId()
                                    .equals(event.serviceId()))
                            .findFirst()
                            .ifPresent(resultView -> {
                                resultView.setStatus(event.newStatus());
                                view.markUpdated();
                                examViewRepository.save(view);
                            });
                });
    }

    @EventHandler
    public void on(ExaminationDeletedEvent event) {
        log.debug("Handling ExaminationDeletedEvent: {}", event.examId());
        examViewRepository.findById(event.examId())
                .ifPresent(view -> {
                    view.markDeleted();
                    examViewRepository.save(view);
                });
    }

    @EventHandler
    public void on(ExaminationCompletedEvent event) {
        log.info("Handling ExaminationCompletedEvent: {}", event.examinationId());
        examViewRepository.findById(event.examinationId())
                .ifPresent(view -> {
                    view.setStatus(ExaminationStatus.valueOf(event.status()));
                    view.markUpdated();
                    examViewRepository.save(view);
                });
    }
}
