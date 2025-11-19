package com.clinic.c46.ExaminationService.application.listener;

import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.CommonService.event.examination.ExaminationCreatedEvent;
import com.clinic.c46.CommonService.query.patient.GetPatientByIdQuery;
import com.clinic.c46.ExaminationService.application.repository.DoctorRepViewRepository;
import com.clinic.c46.ExaminationService.application.repository.ExamViewRepository;
import com.clinic.c46.ExaminationService.domain.event.*;
import com.clinic.c46.ExaminationService.domain.valueObject.MedicalResult;
import com.clinic.c46.ExaminationService.domain.view.DoctorRepView;
import com.clinic.c46.ExaminationService.domain.view.ExamView;
import com.clinic.c46.ExaminationService.domain.view.ResultView;
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
public class ExaminationViewProjection {

    private final ExamViewRepository examViewRepository;
    private final DoctorRepViewRepository doctorViewRepository;
    private final QueryGateway queryGateway;


    @EventHandler
    public void on(ExaminationCreatedEvent event) {
        log.info("[examination.projection.ExaminationCreatedEvent]: {}", event.examinationId());
        ExamView view = ExamView.builder()
                .id(event.examinationId())
                .patientId(event.patientId())
                .build();


        GetPatientByIdQuery query = new GetPatientByIdQuery(event.patientId());
        PatientDto patient = queryGateway.query(query, ResponseTypes.instanceOf(PatientDto.class))
                .join();

        if (patient == null) {
            log.warn("[examination.projection.ExaminationCreatedEvent.patient-not-found] patientId: {}",
                    event.patientId());
            throw new IllegalStateException(
                    "[examination.projection.ExaminationCreatedEvent.patient-not-found] patientId: " + event.patientId());
        }

        view.setPatientName(patient.name());
        view.setPatientEmail(patient.email());
        view.markCreated();
        examViewRepository.save(view);
    }

    @EventHandler
    public void on(ResultAddedEvent event) {
        log.info("[examination.projection.ResultAddedEvent] {}", event.medicalResult()
                .getServiceId());

        examViewRepository.findById(event.examinationId())
                .filter(view -> !view.containsServiceId(event.medicalResult()
                        .getServiceId()))
                .ifPresent(view -> addResultToView(view, event));
    }

    private void addResultToView(ExamView view, ResultAddedEvent event) {
        MedicalResult medicalResult = event.medicalResult();
        DoctorRepView doctorRepView = getDoctorOrThrow(medicalResult.getDoctorId());
        ResultView resultView = buildResultView(medicalResult, doctorRepView.getName());
        view.addResultView(resultView);
        view.markUpdated();
        examViewRepository.save(view);
    }

    private DoctorRepView getDoctorOrThrow(String doctorId) {
        return doctorViewRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("[examination.projection.ResultAddedEvent.doctor-not-found] doctorId: %s",
                                doctorId)));
    }

    private ResultView buildResultView(MedicalResult medicalResult, String doctorName) {
        return ResultView.builder()
                .serviceId(medicalResult.getServiceId())
                .doctorId(medicalResult.getDoctorId())
                .data(medicalResult.getData())
                .pdfUrl(medicalResult.getPdfUrl())
                .status(medicalResult.getStatus())
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
}
