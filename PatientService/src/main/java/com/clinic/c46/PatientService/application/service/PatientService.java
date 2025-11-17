package com.clinic.c46.PatientService.application.service;
import com.clinic.c46.CommonService.command.patient.CreatePatientCommand;
import com.clinic.c46.CommonService.command.patient.DeletePatientCommand;
import com.clinic.c46.CommonService.query.patient.GetAllPatientsQuery;
import com.clinic.c46.CommonService.query.patient.GetPatientByIdQuery;
import com.clinic.c46.PatientService.domain.view.PatientView;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    // COMMAND
    public void createPatient(String name, String email, String phone) {
        CreatePatientCommand cmd = CreatePatientCommand.builder()
                .patientId(UUID.randomUUID().toString())
                .name(name)
                .email(email)
                .phone(phone)
                .build();
        commandGateway.sendAndWait(cmd);
    }

    public void deletePatient(String id) {
        DeletePatientCommand cmd = DeletePatientCommand.builder()
                .patientId(id)
                .build();
        commandGateway.sendAndWait(cmd);
    }

    // QUERY
    public CompletableFuture<PatientView> getPatientById(String id) {
        return queryGateway.query(
                new GetPatientByIdQuery(id),
                ResponseTypes.instanceOf(PatientView.class)
                                 );
    }

    public CompletableFuture<List<PatientView>> getAllPatients() {
        return queryGateway.query(
                new GetAllPatientsQuery(),
                ResponseTypes.multipleInstancesOf(PatientView.class)
                                 );
    }
}
