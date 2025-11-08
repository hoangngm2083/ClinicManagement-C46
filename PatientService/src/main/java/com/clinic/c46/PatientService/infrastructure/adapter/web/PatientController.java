package com.clinic.c46.PatientService.infrastructure.adapter.web;

import com.clinic.c46.PatientService.application.service.PatientService;
import com.clinic.c46.PatientService.domain.view.PatientView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patient")
public class PatientController {
    private final PatientService patientService;
    // --- COMMAND ---
    @PostMapping
    public ResponseEntity<String> createPatient(@RequestBody PatientView request) {
        patientService.createPatient(request.getName(), request.getEmail(), request.getPhone());
        return ResponseEntity.ok("Patient created successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePatient(@PathVariable String id) {
        patientService.deletePatient(id);
        return ResponseEntity.ok("Patient deleted successfully");
    }

    // --- QUERY ---
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<PatientView>> getPatientById(@PathVariable String id) {
        return patientService.getPatientById(id)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<List<PatientView>>> getAllPatients() {
        return patientService.getAllPatients()
                .thenApply(ResponseEntity::ok);
    }
}
