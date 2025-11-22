package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.controller;

import com.clinic.c46.CommonService.query.staff.GetIdOfAllDepartmentQuery;
import com.clinic.c46.MedicalPackageService.application.service.MedicalPackageService;
import com.clinic.c46.MedicalPackageService.application.service.MedicalServiceService;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/migrate")
@RequiredArgsConstructor
@Slf4j
public class MigrateController {

    private final QueryGateway queryGateway;
    private final MedicalServiceService medicalServiceService;
    private final MedicalPackageService medicalPackageService;

    @PostMapping("/database")
    public ResponseEntity<String> migrateDatabase() {
        try {
            log.info("Starting database migration...");

            // Step 1: Query all department IDs
            CompletableFuture<List<String>> departmentIds = queryGateway.query(new GetIdOfAllDepartmentQuery(),
                    ResponseTypes.multipleInstancesOf(String.class));

            List<String> departments = departmentIds.get();
            log.info("Retrieved {} departments", departments.size());

            if (departments.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("No departments found in the system");
            }

            // Step 2: Create Medical Services based on each department and store their IDs
            List<String> allServiceIds = new ArrayList<>();
            for (String departmentId : departments) {
                List<String> serviceIds = createMedicalServicesForDepartment(departmentId);
                allServiceIds.addAll(serviceIds);
            }

            // Step 3: Create Medical Packages with actual service IDs from database
            createMedicalPackages(allServiceIds);

            log.info("Database migration completed successfully");
            return ResponseEntity.ok("Database migration completed successfully");

        } catch (Exception e) {
            log.error("Database migration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database migration failed: " + e.getMessage());
        }
    }

    private List<String> createMedicalServicesForDepartment(String departmentId) {
        log.info("Creating medical services for department: {}", departmentId);

        // Sample medical services data
        List<MedicalServiceData> services = getDefaultMedicalServices();
        List<String> createdServiceIds = new ArrayList<>();

        for (MedicalServiceData service : services) {
            String serviceId = UUID.randomUUID()
                    .toString();
            CreateMedicalServiceCommand cmd = CreateMedicalServiceCommand.builder()
                    .medicalServiceId(serviceId)
                    .name(service.name)
                    .description(service.description)
                    .departmentId(departmentId)
                    .processingPriority(service.processingPriority)
                    .formTemplate(service.formTemplate)
                    .build();

            medicalServiceService.create(cmd);
            createdServiceIds.add(serviceId);
            log.debug("Created medical service: {} for department: {} with id: {}", service.name, departmentId,
                    serviceId);
        }

        return createdServiceIds;
    }

    private void createMedicalPackages(List<String> allServiceIds) {
        log.info("Creating medical packages with {} available services", allServiceIds.size());

        // Sample medical packages data
        List<MedicalPackageData> packages = getDefaultMedicalPackages();

        for (MedicalPackageData pkg : packages) {
            // Select services from actual created services
            Set<String> selectedServiceIds = selectServicesForPackage(allServiceIds, pkg.serviceCount);

            CreateMedicalPackageCommand cmd = CreateMedicalPackageCommand.builder()
                    .medicalPackageId(UUID.randomUUID()
                            .toString())
                    .name(pkg.name)
                    .description(pkg.description)
                    .price(pkg.price)
                    .image(pkg.image)
                    .serviceIds(selectedServiceIds)
                    .build();

            medicalPackageService.createPackage(cmd);
            log.debug("Created medical package: {} with {} services", pkg.name, selectedServiceIds.size());
        }
    }

    private Set<String> selectServicesForPackage(List<String> allServiceIds, int requestedCount) {
        Set<String> selectedIds = new HashSet<>();
        int count = Math.min(requestedCount, allServiceIds.size());

        // Tạo bản sao để không thay đổi danh sách gốc
        List<String> copyList = new ArrayList<>(allServiceIds);

        // Trộn ngẫu nhiên
        Collections.shuffle(copyList);

        // Lấy count phần tử đầu tiên sau khi trộn
        for (int i = 0; i < count; i++) {
            selectedIds.add(copyList.get(i));
        }

        return selectedIds;
    }

    private List<MedicalServiceData> getDefaultMedicalServices() {
        return Arrays.asList(new MedicalServiceData("Khám Tổng Quát", "Khám sức khỏe tổng quát cho bệnh nhân", 1, """ 
                  {
                  "components": [
                    { "key": "description", "label": "Mô tả", "type": "textarea" },
                    { "key": "image1", "label": "Ảnh siêu âm 1", "type": "file" }
                  ]
                }
                """), new MedicalServiceData("Xét Nghiệm Máu", "Xét nghiệm máu để chẩn đoán bệnh", 2, """ 
                  {
                  "components": [
                    { "key": "description", "label": "Mô tả", "type": "textarea" },
                    { "key": "image1", "label": "Ảnh siêu âm 1", "type": "file" }
                  ]
                }
                """), new MedicalServiceData("Siêu Âm", "Khám bằng siêu âm các cơ quan trong", 3, """ 
                  {
                  "components": [
                    { "key": "description", "label": "Mô tả", "type": "textarea" },
                    { "key": "image1", "label": "Ảnh siêu âm 1", "type": "file" }
                  ]
                }
                """), new MedicalServiceData("Chụp X-Quang", "Chụp hình X-quang chẩn đoán", 4, """ 
                  {
                  "components": [
                    { "key": "description", "label": "Mô tả", "type": "textarea" },
                    { "key": "image1", "label": "Ảnh siêu âm 1", "type": "file" }
                  ]
                }
                """), new MedicalServiceData("Điện Tim", "Ghi điện tim chẩn đoán bệnh tim", 5, """ 
                  {
                  "components": [
                    { "key": "description", "label": "Mô tả", "type": "textarea" },
                    { "key": "image1", "label": "Ảnh siêu âm 1", "type": "file" }
                  ]
                }
                """));
    }

    private List<MedicalPackageData> getDefaultMedicalPackages() {
        return Arrays.asList(
                new MedicalPackageData("Gói Khám Cơ Bản", "Gói khám sức khỏe cơ bản với các dịch vụ cần thiết",
                        new BigDecimal("500000"), "basic_package.jpg", 3),
                new MedicalPackageData("Gói Khám Tiêu Chuẩn", "Gói khám tiêu chuẩn bao gồm đầy đủ dịch vụ",
                        new BigDecimal("1000000"), "standard_package.jpg", 4),
                new MedicalPackageData("Gói Khám Toàn Diện", "Gói khám toàn diện với tất cả các dịch vụ khám chẩn đoán",
                        new BigDecimal("1500000"), "premium_package.jpg", 5));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class MedicalServiceData {
        private String name;
        private String description;
        private int processingPriority;
        private String formTemplate;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class MedicalPackageData {
        private String name;
        private String description;
        private BigDecimal price;
        private String image;
        private int serviceCount;
    }
}
