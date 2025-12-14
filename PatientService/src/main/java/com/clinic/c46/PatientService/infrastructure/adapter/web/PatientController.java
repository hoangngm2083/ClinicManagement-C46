package com.clinic.c46.PatientService.infrastructure.adapter.web;

import com.clinic.c46.CommonService.dto.PatientDto;
import com.clinic.c46.PatientService.application.service.PatientService;
import com.clinic.c46.PatientService.domain.view.PatientView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patient")
public class PatientController {
    private final PatientService patientService;

    // --- COMMAND ---
    @PostMapping
    public ResponseEntity<Map<String, String>> createPatient(@RequestBody PatientView request) {
        String patientId = patientService.createPatient(request.getName(), request.getEmail(), request.getPhone());
        return ResponseEntity.ok(Map.of("patientId", patientId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePatient(@PathVariable String id) {
        patientService.deletePatient(id);
        return ResponseEntity.ok("Patient deleted successfully");
    }

    // --- QUERY ---
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<PatientDto>> getPatientById(@PathVariable String id) {
        return patientService.getPatientById(id)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<List<PatientDto>>> getAllPatients() {
        return patientService.getAllPatients()
                .thenApply(ResponseEntity::ok);
    }
}
