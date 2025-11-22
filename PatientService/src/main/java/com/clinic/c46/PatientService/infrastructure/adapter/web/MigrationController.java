package com.clinic.c46.PatientService.infrastructure.adapter.web;

import com.clinic.c46.PatientService.application.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/migration")
public class MigrationController {

    private final PatientService patientService;

    // Common email for all test patients
    private static final String TEST_EMAIL = "n21dccn034@student.ptithcm.edu.vn";

    @PostMapping("/patients/seed")
    public ResponseEntity<String> seedPatients() {
        // Create test patients with common email
        patientService.createPatient("Nguyễn Văn A", TEST_EMAIL, "0123456789");
        patientService.createPatient("Trần Thị B", TEST_EMAIL, "0987654321");
        patientService.createPatient("Phạm Văn C", TEST_EMAIL, "0912345678");
        patientService.createPatient("Lê Thị D", TEST_EMAIL, "0945678901");
        patientService.createPatient("Hoàng Văn E", TEST_EMAIL, "0934567890");

        return ResponseEntity.ok("Patients seeded successfully with email: " + TEST_EMAIL);
    }
}
