package com.clinic.c46.StaffService.infrastructure.adapter.web.controller;

import com.clinic.c46.CommonService.type.Shift;
import com.clinic.c46.StaffService.domain.command.CreateDepartmentCommand;
import com.clinic.c46.StaffService.domain.command.CreateStaffCommand;
import com.clinic.c46.StaffService.domain.command.RequestDayOffCommand;
import com.clinic.c46.StaffService.domain.enums.Role;
import com.clinic.c46.StaffService.domain.valueObject.DayOff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/migrate")
@RequiredArgsConstructor
public class MigrationController {

    private final CommandGateway commandGateway;

    @PostMapping("/mock-data")
    public CompletableFuture<ResponseEntity<MigrationResponse>> migrateMockData() {
        log.info("Starting migration of mock data...");

        return migrateDepartments().thenCompose(migrationResult -> migrateStaff(migrationResult.departmentIds()))
                .thenCompose(migrationResult -> migrateDayOffs(migrationResult.staffIds()))
                .thenApply(result -> {
                    log.info("Migration completed successfully");
                    return ResponseEntity.ok(new MigrationResponse("Migration completed successfully",
                            result.departmentIds()
                                    .size(), result.staffIds()
                            .size(), result.dayOffRequests()));
                })
                .exceptionally(ex -> {
                    log.error("Migration failed: {}", ex.getMessage());
                    return ResponseEntity.internalServerError()
                            .body(new MigrationResponse("Migration failed: " + ex.getMessage(), 0, 0, 0));
                });
    }

    private CompletableFuture<MigrationResult> migrateDepartments() {
        log.info("Creating departments...");

        List<CreateDepartmentCommand> departmentCommands = Arrays.asList(CreateDepartmentCommand.builder()
                .departmentId(UUID.randomUUID()
                        .toString())
                .name("Reception")
                .description("Heart and cardiovascular department")
                .build(), CreateDepartmentCommand.builder()
                .departmentId(UUID.randomUUID()
                        .toString())
                .name("Neurology")
                .description("Brain and nervous system department")
                .build(), CreateDepartmentCommand.builder()
                .departmentId(UUID.randomUUID()
                        .toString())
                .name("Pediatrics")
                .description("Children healthcare department")
                .build(), CreateDepartmentCommand.builder()
                .departmentId(UUID.randomUUID()
                        .toString())
                .name("Orthopedics")
                .description("Bone and joint department")
                .build(), CreateDepartmentCommand.builder()
                .departmentId(UUID.randomUUID()
                        .toString())
                .name("Emergency")
                .description("Emergency and critical care department")
                .build());

        // Send all department commands and collect their IDs
        List<CompletableFuture<String>> departmentFutures = departmentCommands.stream()
                .map(command -> commandGateway.send(command)
                        .thenApply(result -> {
                            log.info("Department created: {}", command.departmentId());
                            return command.departmentId();
                        }))
                .toList();

        return CompletableFuture.allOf(departmentFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> departmentFutures.stream()
                        .map(CompletableFuture::join)
                        .toList())
                .thenApply(departmentIds -> new MigrationResult(departmentIds, Collections.emptyList(), 0));
    }

    private CompletableFuture<MigrationResult> migrateStaff(List<String> departmentIds) {
        log.info("Creating staff members for {} departments...", departmentIds.size());

        List<CreateStaffCommand> staffCommands = Arrays.asList(
                // Cardiology staff
                CreateStaffCommand.builder()
                        .staffId(UUID.randomUUID()
                                .toString())
                        .name("Dr. John Smith")
                        .email("john.smith@clinic.com")
                        .phone("+1234567890")
                        .description("Senior Cardiologist with 15 years experience")
                        .image("john_smith.jpg")
                        .role(Role.MANAGER)
                        .eSignature("john_smith_signature.png")
                        .departmentId(departmentIds.get(0))
                        .build(), CreateStaffCommand.builder()
                        .staffId(UUID.randomUUID()
                                .toString())
                        .name("Dr. Sarah Johnson")
                        .email("sarah.johnson@clinic.com")
                        .phone("+1234567891")
                        .description("Cardiologist specializing in pediatric cardiology")
                        .image("sarah_johnson.jpg")
                        .role(Role.DOCTOR)
                        .eSignature("sarah_johnson_signature.png")
                        .departmentId(departmentIds.get(0))
                        .build(),

                // Neurology staff
                CreateStaffCommand.builder()
                        .staffId(UUID.randomUUID()
                                .toString())
                        .name("Dr. Michael Brown")
                        .email("michael.brown@clinic.com")
                        .phone("+1234567892")
                        .description("Neurologist with expertise in stroke treatment")
                        .image("michael_brown.jpg")
                        .role(Role.DOCTOR)
                        .eSignature("michael_brown_signature.png")
                        .departmentId(departmentIds.get(1))
                        .build(), CreateStaffCommand.builder()
                        .staffId(UUID.randomUUID()
                                .toString())
                        .name("Dr. Emily Davis")
                        .email("emily.davis@clinic.com")
                        .phone("+1234567893")
                        .description("Neurosurgeon specializing in brain surgery")
                        .image("emily_davis.jpg")
                        .role(Role.DOCTOR)
                        .eSignature("emily_davis_signature.png")
                        .departmentId(departmentIds.get(1))
                        .build(),

                // Pediatrics staff
                CreateStaffCommand.builder()
                        .staffId(UUID.randomUUID()
                                .toString())
                        .name("Dr. Robert Wilson")
                        .email("robert.wilson@clinic.com")
                        .phone("+1234567894")
                        .description("Pediatrician with 20 years experience")
                        .image("robert_wilson.jpg")
                        .role(Role.DOCTOR)
                        .eSignature("robert_wilson_signature.png")
                        .departmentId(departmentIds.get(2))
                        .build(),

                // Orthopedics staff
                CreateStaffCommand.builder()
                        .staffId(UUID.randomUUID()
                                .toString())
                        .name("R. Lisa Anderson")
                        .email("lisa.anderson@clinic.com")
                        .phone("+1234567895")
                        .description("Orthopedic surgeon specializing in sports injuries")
                        .image("lisa_anderson.jpg")
                        .role(Role.RECEPTIONIST)
                        .eSignature("lisa_anderson_signature.png")
                        .departmentId(departmentIds.get(0))
                        .build(),

                // Emergency staff
                CreateStaffCommand.builder()
                        .staffId(UUID.randomUUID()
                                .toString())
                        .name("R. David Miller")
                        .email("david.miller@clinic.com")
                        .phone("+1234567896")
                        .description("Emergency medicine specialist")
                        .image("david_miller.jpg")
                        .role(Role.RECEPTIONIST)
                        .eSignature("david_miller_signature.png")
                        .departmentId(departmentIds.get(0))
                        .build());

        // Send all staff commands and collect their IDs
        List<CompletableFuture<String>> staffFutures = staffCommands.stream()
                .map(command -> commandGateway.send(command)
                        .thenApply(result -> {
                            log.info("Staff created: {}", command.staffId());
                            return command.staffId();
                        }))
                .toList();

        return CompletableFuture.allOf(staffFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> staffFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                .thenApply(staffIds -> new MigrationResult(departmentIds, staffIds, 0));
    }

    private CompletableFuture<MigrationResult> migrateDayOffs(List<String> staffIds) {
        log.info("Creating day off requests for {} staff members...", staffIds.size());

        if (staffIds.isEmpty()) {
            return CompletableFuture.completedFuture(new MigrationResult(Collections.emptyList(), staffIds, 0));
        }

        // Create day off requests for some staff members
        List<CompletableFuture<String>> dayOffFutures = new ArrayList<>();

        // Staff 0: Multiple day offs
        Set<DayOff> staff0DayOffs = Set.of(new DayOff(LocalDate.now()
                .plusDays(5), Shift.MORNING, "Family event"), new DayOff(LocalDate.now()
                .plusDays(10), Shift.AFTERNOON, "Medical appointment"));
        dayOffFutures.add(commandGateway.send(new RequestDayOffCommand(staffIds.get(0), staff0DayOffs))
                .thenApply(result -> {
                    log.info("Day offs created for staff: {}", staffIds.get(0));
                    return staffIds.get(0);
                }));

        // Staff 1: Single day off
        Set<DayOff> staff1DayOffs = Set.of(new DayOff(LocalDate.now()
                .plusDays(7), Shift.MORNING, "Vacation"));
        dayOffFutures.add(commandGateway.send(new RequestDayOffCommand(staffIds.get(1), staff1DayOffs))
                .thenApply(result -> {
                    log.info("Day offs created for staff: {}", staffIds.get(1));
                    return staffIds.get(1);
                }));

        // Staff 3: Multiple day offs in different shifts
        Set<DayOff> staff3DayOffs = Set.of(new DayOff(LocalDate.now()
                .plusDays(3), Shift.MORNING, "Training session"), new DayOff(LocalDate.now()
                .plusDays(4), Shift.AFTERNOON, "Training session"), new DayOff(LocalDate.now()
                .plusDays(15), Shift.MORNING, "Conference"));
        dayOffFutures.add(commandGateway.send(new RequestDayOffCommand(staffIds.get(3), staff3DayOffs))
                .thenApply(result -> {
                    log.info("Day offs created for staff: {}", staffIds.get(3));
                    return staffIds.get(3);
                }));

        return CompletableFuture.allOf(dayOffFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> new MigrationResult(Collections.emptyList(), staffIds, dayOffFutures.size()));
    }

    // Record classes for response and intermediate results
    public record MigrationResponse(String message, int departmentsCreated, int staffCreated, int dayOffRequests) {
    }

    private record MigrationResult(List<String> departmentIds, List<String> staffIds, int dayOffRequests) {
    }
}