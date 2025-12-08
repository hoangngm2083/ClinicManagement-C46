package com.clinic.c46.MedicalPackageService.infrastructure.adapter.web.controller;

import com.clinic.c46.CommonService.query.staff.GetIdOfAllDepartmentQuery;
import com.clinic.c46.MedicalPackageService.application.service.MedicalPackageService;
import com.clinic.c46.MedicalPackageService.application.service.MedicalServiceService;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalPackageCommand;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@RequestMapping("/migrate")
@RequiredArgsConstructor
@Slf4j
public class MigrateController {

  private final QueryGateway queryGateway;
  private final MedicalServiceService medicalServiceService;
  private final MedicalPackageService medicalPackageService;
  private final ObjectMapper objectMapper = new ObjectMapper();

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
      // Select services based on package type - more intelligent selection
      Set<String> selectedServiceIds = selectServicesForSpecificPackage(allServiceIds, pkg);

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

  private Set<String> selectServicesForSpecificPackage(List<String> allServiceIds, MedicalPackageData pkg) {
    // Define service mappings for each package type
    Map<String, List<String>> packageServiceMappings = getPackageServiceMappings();

    List<String> preferredServices = packageServiceMappings.getOrDefault(pkg.name, new ArrayList<>());

    Set<String> selectedIds = new HashSet<>();

    // First, try to select preferred services for this package
    List<String> availablePreferredServices = new ArrayList<>();
    for (int i = 0; i < preferredServices.size() && availablePreferredServices.size() < pkg.serviceCount; i++) {
      // Find service IDs that match the preferred service names
      for (String serviceId : allServiceIds) {
        // Note: In a real implementation, you would need to map service names to IDs
        // For now, we'll use a simplified approach based on service count
        if (availablePreferredServices.size() < pkg.serviceCount && !availablePreferredServices.contains(serviceId)) {
          availablePreferredServices.add(serviceId);
          break;
        }
      }
    }

    // If we don't have enough preferred services, fill with random services
    if (availablePreferredServices.size() < pkg.serviceCount) {
      List<String> remainingServices = new ArrayList<>(allServiceIds);
      remainingServices.removeAll(availablePreferredServices);

      Collections.shuffle(remainingServices);

      int remainingCount = pkg.serviceCount - availablePreferredServices.size();
      for (int i = 0; i < remainingCount && i < remainingServices.size(); i++) {
        availablePreferredServices.add(remainingServices.get(i));
      }
    }

    // Take only the required number of services
    for (int i = 0; i < pkg.serviceCount && i < availablePreferredServices.size(); i++) {
      selectedIds.add(availablePreferredServices.get(i));
    }

    return selectedIds;
  }

  private Map<String, List<String>> getPackageServiceMappings() {
    Map<String, List<String>> mappings = new HashMap<>();

    // Gói khám sức khỏe cơ bản
    mappings.put("Gói Khám Sức Khỏe Cơ Bản", Arrays.asList(
        "Khám Nội Tổng Quát",
        "Xét Nghiệm Máu Tổng Quát",
        "Xét Nghiệm Nước Tiểu"
    ));

    // Gói khám sức khỏe tiêu chuẩn
    mappings.put("Gói Khám Sức Khỏe Tiêu Chuẩn", Arrays.asList(
        "Khám Nội Tổng Quát",
        "Khám Tim Mạch",
        "Khám Sản Phụ Khoa",
        "Xét Nghiệm Máu Tổng Quát",
        "Xét Nghiệm Nước Tiểu",
        "Siêu Âm Bụng Tổng Quát",
        "Điện Tim",
        "Chụp X-Quang Ngực"
    ));

    // Gói khám sức khỏe cao cấp
    mappings.put("Gói Khám Sức Khỏe Cao Cấp", Arrays.asList(
        "Khám Nội Tổng Quát",
        "Khám Tim Mạch",
        "Khám Nội Tiết",
        "Khám Sản Phụ Khoa",
        "Khám Mắt",
        "Khám Tai Mũi Họng",
        "Khám Da Liễu",
        "Khám Răng Hàm Mặt",
        "Xét Nghiệm Máu Tổng Quát",
        "Xét Nghiệm Nước Tiểu",
        "Siêu Âm Bụng Tổng Quát",
        "Điện Tim"
    ));

    // Gói khám nhi đồng
    mappings.put("Gói Khám Sức Khỏe Nhi Đồng", Arrays.asList(
        "Khám Nhi Khoa",
        "Khám Tai Mũi Họng Trẻ Em",
        "Xét Nghiệm Máu Tổng Quát",
        "Xét Nghiệm Nước Tiểu"
    ));

    // Gói khám phụ nữ
    mappings.put("Gói Khám Phụ Nữ", Arrays.asList(
        "Khám Sản Phụ Khoa",
        "Siêu Âm Thai",
        "Xét Nghiệm Máu Tổng Quát",
        "Xét Nghiệm Nước Tiểu"
    ));

    // Gói khám tim mạch
    mappings.put("Gói Khám Tim Mạch", Arrays.asList(
        "Khám Tim Mạch",
        "Điện Tim",
        "Xét Nghiệm Máu Tổng Quát",
        "Chụp X-Quang Ngực",
        "Siêu Âm Bụng Tổng Quát"
    ));

    // Gói khám nội tiết - đái tháo đường
    mappings.put("Gói Khám Nội Tiết - Đái Tháo Đường", Arrays.asList(
        "Khám Nội Tiết",
        "Xét Nghiệm Máu Tổng Quát",
        "Xét Nghiệm Nước Tiểu",
        "Siêu Âm Bụng Tổng Quát"
    ));

    // Gói khám tiêu hóa
    mappings.put("Gói Khám Tiêu Hóa", Arrays.asList(
        "Khám Nội Tổng Quát",
        "Nội Soi Tiêu Hóa",
        "Siêu Âm Bụng Tổng Quát",
        "Xét Nghiệm Máu Tổng Quát",
        "Xét Nghiệm Nước Tiểu"
    ));

    // Gói khám mắt cơ bản
    mappings.put("Gói Khám Mắt Cơ Bản", Arrays.asList(
        "Khám Mắt"
    ));

    // Gói khám tai mũi họng
    mappings.put("Gói Khám Tai Mũi Họng", Arrays.asList(
        "Khám Tai Mũi Họng",
        "Khám Tai Mũi Họng Trẻ Em"
    ));

    // Gói khám da liễu
    mappings.put("Gói Khám Da Liễu", Arrays.asList(
        "Khám Da Liễu"
    ));

    // Gói khám răng hàm mặt cơ bản
    mappings.put("Gói Khám Răng Hàm Mặt Cơ Bản", Arrays.asList(
        "Khám Răng Hàm Mặt"
    ));

    return mappings;
  }

  private JsonNode parseJsonTemplate(String jsonString) {
    try {
      return objectMapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      log.error("Error parsing JSON template", e);
      return objectMapper.createObjectNode(); // Return empty object node as fallback
    }
  }

  private List<MedicalServiceData> getDefaultMedicalServices() {
    return Arrays.asList(
        // Nội khoa (Internal Medicine)
        new MedicalServiceData("Khám Nội Tổng Quát", "Khám và tư vấn các bệnh lý nội khoa", 1, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Lý do khám",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "reason",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Mô tả triệu chứng, bệnh sử..."
                     },
                     {
                       "label": "Khám lâm sàng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "physicalExam",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Mạch, huyết áp, thân nhiệt, khám bộ phận..."
                     },
                     {
                       "label": "Chẩn đoán",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "diagnosis",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Kế hoạch điều trị",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "treatmentPlan",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),
        new MedicalServiceData("Khám Tim Mạch", "Khám chuyên khoa tim mạch và huyết áp", 2, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Triệu chứng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "symptoms",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Đau ngực, khó thở, mệt mỏi..."
                     },
                     {
                       "label": "Huyết áp",
                       "tableView": true,
                       "key": "bloodPressure",
                       "type": "number",
                       "input": true,
                       "placeholder": "120/80 mmHg"
                     },
                     {
                       "label": "Nhịp tim",
                       "tableView": true,
                       "key": "heartRate",
                       "type": "number",
                       "input": true,
                       "placeholder": "bpm"
                     },
                     {
                       "label": "Siêu âm tim",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "echoImage",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Chẩn đoán",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "diagnosis",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),
        new MedicalServiceData("Khám Nội Tiết", "Khám và điều trị các bệnh nội tiết", 3, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Triệu chứng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "symptoms",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Khát nhiều, tiểu nhiều, mệt mỏi..."
                     },
                     {
                       "label": "Đường huyết lúc đói",
                       "tableView": true,
                       "key": "bloodSugar",
                       "type": "number",
                       "input": true,
                       "placeholder": "mg/dL"
                     },
                     {
                       "label": "Hormone test",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "hormoneTest",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Siêu âm tuyến giáp",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "thyroidUltraSound",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Chẩn đoán",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "diagnosis",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),

        // Sản phụ khoa (Obstetrics & Gynecology)
        new MedicalServiceData("Khám Sản Phụ Khoa", "Khám phụ khoa định kỳ và tư vấn sức khỏe sinh sản", 4, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Lý do khám",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "reason",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Định kỳ, triệu chứng bất thường..."
                     },
                     {
                       "label": "Khám phụ khoa",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "gynecologicalExam",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Khám bụng, khám tầng sinh môn..."
                     },
                     {
                       "label": "Siêu âm phụ khoa",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "gynecologyUltraSound",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Pap smear",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "papSmear",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Chẩn đoán",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "diagnosis",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),
        new MedicalServiceData("Siêu Âm Thai", "Siêu âm thai nhi định kỳ theo tháng", 5, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Tuổi thai",
                       "tableView": true,
                       "key": "gestationalAge",
                       "type": "number",
                       "input": true,
                       "placeholder": "tuần"
                     },
                     {
                       "label": "Hình ảnh siêu âm",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "ultraSoundImage",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Đo đạc thai nhi",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "fetalMeasurements",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "CRL, BPD, HC, AC, FL..."
                     },
                     {
                       "label": "Nhận xét về thai nhi",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "fetalAssessment",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Khuyến cáo",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "recommendations",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),

        // Nhi khoa (Pediatrics)
        new MedicalServiceData("Khám Nhi Khoa", "Khám sức khỏe định kỳ cho trẻ em", 6, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Tuổi của trẻ",
                       "tableView": true,
                       "key": "childAge",
                       "type": "number",
                       "input": true,
                       "placeholder": "tháng tuổi"
                     },
                     {
                       "label": "Triệu chứng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "symptoms",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Phát triển thể chất",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "physicalDevelopment",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Cân nặng, chiều cao, vòng đầu..."
                     },
                     {
                       "label": "Khám lâm sàng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "clinicalExam",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Tiêm chủng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "vaccinations",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Chẩn đoán",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "diagnosis",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),
        new MedicalServiceData("Khám Tai Mũi Họng Trẻ Em", "Khám chuyên khoa tai mũi họng cho trẻ", 7, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Triệu chứng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "symptoms",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Đau họng, nghẹt mũi, ho..."
                     },
                     {
                       "label": "Khám tai",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "earExam",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Khám mũi",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "noseExam",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Khám họng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "throatExam",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Chẩn đoán",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "diagnosis",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),

        // Răng hàm mặt (Dentistry)
        new MedicalServiceData("Khám Răng Hàm Mặt", "Khám răng miệng tổng quát", 8, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Triệu chứng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "symptoms",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Đau răng, sưng lợi..."
                     },
                     {
                       "label": "Khám răng miệng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "oralExam",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Răng sữa/thường, sâu răng, viêm lợi..."
                     },
                     {
                       "label": "X-quang răng",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "dentalXray",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Kế hoạch điều trị",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "treatmentPlan",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),
        new MedicalServiceData("Chỉnh Nha", "Tư vấn và điều trị chỉnh hình răng mặt", 9, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Khuyết tật răng mặt",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "malocclusion",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Phân tích khớp cắn",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "biteAnalysis",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Chụp X-quang panorama",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "panoramicXray",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Kế hoạch chỉnh nha",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "orthodonticPlan",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),

        // Mắt (Ophthalmology)
        new MedicalServiceData("Khám Mắt", "Khám thị lực và mắt tổng quát", 10, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Triệu chứng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "symptoms",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Mờ mắt, đau mắt, đỏ mắt..."
                     },
                     {
                       "label": "Thị lực",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "visionTest",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Thị lực 2 mắt, kính áp tròng..."
                     },
                     {
                       "label": "Khám mắt ngoài",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "externalEyeExam",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Khám đáy mắt",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "fundusExam",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Chẩn đoán",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "diagnosis",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),

        // Tai mũi họng (ENT)
        new MedicalServiceData("Khám Tai Mũi Họng", "Khám chuyên khoa tai mũi họng", 11, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Triệu chứng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "symptoms",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Đau họng, nghẹt mũi, chóng mặt..."
                     },
                     {
                       "label": "Khám tai",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "earExam",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Khám mũi",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "noseExam",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Vẹo vách ngăn, polyp mũi..."
                     },
                     {
                       "label": "Khám họng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "throatExam",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Viêm họng, amidan..."
                     },
                     {
                       "label": "Nội soi tai mũi họng",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "endoscopyImage",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Chẩn đoán",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "diagnosis",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),

        // Da liễu (Dermatology)
        new MedicalServiceData("Khám Da Liễu", "Khám và điều trị các bệnh về da", 12, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Triệu chứng",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "symptoms",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Ngứa, phát ban, mụn..."
                     },
                     {
                       "label": "Khám da",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "skinExam",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Mô tả tổn thương da, vị trí..."
                     },
                     {
                       "label": "Ảnh tổn thương da",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "skinLesionImage",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Da niêm mạc",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "mucosaExam",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Chẩn đoán",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "diagnosis",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),

        // Xét nghiệm (Laboratory)
        new MedicalServiceData("Xét Nghiệm Máu Tổng Quát", "Xét nghiệm máu toàn diện đánh giá sức khỏe", 13, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Chỉ số công thức máu",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "cbc",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Hồng cầu, bạch cầu, tiểu cầu..."
                     },
                     {
                       "label": "Đường huyết",
                       "tableView": true,
                       "key": "bloodSugar",
                       "type": "number",
                       "input": true,
                       "placeholder": "mg/dL"
                     },
                     {
                       "label": "Cholesterol toàn phần",
                       "tableView": true,
                       "key": "totalCholesterol",
                       "type": "number",
                       "input": true,
                       "placeholder": "mg/dL"
                     },
                     {
                       "label": "Triglycerid",
                       "tableView": true,
                       "key": "triglycerides",
                       "type": "number",
                       "input": true,
                       "placeholder": "mg/dL"
                     },
                     {
                       "label": "Ure, Creatinin",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "renalFunction",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Men gan (ALT, AST)",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "liverEnzymes",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),
        new MedicalServiceData("Xét Nghiệm Nước Tiểu", "Phân tích nước tiểu", 14, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Màu sắc",
                       "tableView": true,
                       "key": "color",
                       "type": "text",
                       "input": true
                     },
                     {
                       "label": "Tỷ trọng",
                       "tableView": true,
                       "key": "specificGravity",
                       "type": "number",
                       "input": true
                     },
                     {
                       "label": "Protein",
                       "tableView": true,
                       "key": "protein",
                       "type": "text",
                       "input": true
                     },
                     {
                       "label": "Glucose",
                       "tableView": true,
                       "key": "glucose",
                       "type": "text",
                       "input": true
                     },
                     {
                       "label": "Ketone",
                       "tableView": true,
                       "key": "ketone",
                       "type": "text",
                       "input": true
                     },
                     {
                       "label": "Bạch cầu",
                       "tableView": true,
                       "key": "leukocytes",
                       "type": "text",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),

        // Chẩn đoán hình ảnh (Radiology)
        new MedicalServiceData("Chụp X-Quang Ngực", "Chụp X-quang phổi và tim", 15, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Chỉ định chụp",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "indication",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Hình ảnh X-quang",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "xrayImage",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Mô tả hình ảnh",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "imageDescription",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Mô tả tổn thương, cấu trúc..."
                     },
                     {
                       "label": "Kết luận",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "conclusion",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),
        new MedicalServiceData("Siêu Âm Bụng Tổng Quát", "Siêu âm bụng đánh giá các cơ quan trong ổ bụng", 16, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Chỉ định siêu âm",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "indication",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Gan",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "liver",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Tụy",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "pancreas",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Lách",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "spleen",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Thận",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "kidneys",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Hình ảnh siêu âm",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "ultrasoundImage",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Kết luận",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "conclusion",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),

        // Khám chuyên khoa khác
        new MedicalServiceData("Điện Tim", "Điện tâm đồ chẩn đoán bệnh tim mạch", 17, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Chỉ định ECG",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "indication",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhịp tim",
                       "tableView": true,
                       "key": "heartRate",
                       "type": "number",
                       "input": true,
                       "placeholder": "bpm"
                     },
                     {
                       "label": "Rhythm",
                       "tableView": true,
                       "key": "rhythm",
                       "type": "text",
                       "input": true,
                       "placeholder": "Sinus, AF, etc."
                     },
                     {
                       "label": "ST-T changes",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "stChanges",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Hình ảnh ECG",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "ecgImage",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Kết luận",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "conclusion",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)),
        new MedicalServiceData("Nội Soi Tiêu Hóa", "Nội soi dạ dày - tá tràng", 18, parseJsonTemplate("""
              {
                   "components": [
                     {
                       "label": "Chỉ định nội soi",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "indication",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Chuẩn bị",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "preparation",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Nhịn ăn, thuốc tiền mê..."
                     },
                     {
                       "label": "Mô tả nội soi",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "endoscopicFindings",
                       "type": "textarea",
                       "input": true,
                       "placeholder": "Dạ dày, hang vị, môn vị..."
                     },
                     {
                       "label": "Sinh thiết",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "biopsy",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Hình ảnh nội soi",
                       "image": true,
                       "tableView": false,
                       "webcam": false,
                       "key": "endoscopyImage",
                       "type": "file",
                       "input": true
                     },
                     {
                       "label": "Kết luận",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "conclusion",
                       "type": "textarea",
                       "input": true
                     },
                     {
                       "label": "Nhận xét của bác sĩ",
                       "autoExpand": false,
                       "tableView": true,
                       "key": "doctorNote",
                       "type": "textarea",
                       "input": true
                     }
                   ],
                   "type": "form",
                   "tags": [],
                   "owner": null
                 }
            """)));
  }

  private List<MedicalPackageData> getDefaultMedicalPackages() {
    return Arrays.asList(
        new MedicalPackageData("Gói Khám Sức Khỏe Cơ Bản",
            "Gói khám tổng quát cho người lớn khỏe mạnh, bao gồm khám nội tổng quát và các xét nghiệm cơ bản",
            new BigDecimal("850000"), "basic_health_check.jpg", 4),

        new MedicalPackageData("Gói Khám Sức Khỏe Tiêu Chuẩn",
            "Gói khám toàn diện cho người lớn, bao gồm khám chuyên khoa nội, sản phụ khoa (nữ), xét nghiệm máu và nước tiểu, siêu âm bụng, điện tim",
            new BigDecimal("1850000"), "standard_health_check.jpg", 8),

        new MedicalPackageData("Gói Khám Sức Khỏe Cao Cấp",
            "Gói khám VIP với đầy đủ các chuyên khoa, xét nghiệm chuyên sâu, chẩn đoán hình ảnh và tư vấn dinh dưỡng",
            new BigDecimal("3200000"), "premium_health_check.jpg", 12),

        new MedicalPackageData("Gói Khám Sức Khỏe Nhi Đồng",
            "Gói khám chuyên biệt cho trẻ em từ 1-12 tuổi, bao gồm khám nhi khoa, tai mũi họng trẻ em và các xét nghiệm phù hợp",
            new BigDecimal("750000"), "pediatric_health_check.jpg", 4),

        new MedicalPackageData("Gói Khám Phụ Nữ",
            "Gói khám chuyên sâu cho phụ nữ, bao gồm khám sản phụ khoa, siêu âm phụ khoa, Pap smear và tư vấn sức khỏe sinh sản",
            new BigDecimal("1200000"), "women_health_check.jpg", 6),

        new MedicalPackageData("Gói Khám Tim Mạch",
            "Gói khám chuyên khoa tim mạch, bao gồm khám tim mạch, điện tim, siêu âm tim và các xét nghiệm liên quan",
            new BigDecimal("1500000"), "cardiovascular_check.jpg", 6),

        new MedicalPackageData("Gói Khám Nội Tiết - Đái Tháo Đường",
            "Gói khám chuyên sâu về nội tiết và đái tháo đường, bao gồm xét nghiệm đường huyết, HbA1c, lipid máu và tư vấn điều trị",
            new BigDecimal("950000"), "diabetes_check.jpg", 5),

        new MedicalPackageData("Gói Khám Tiêu Hóa",
            "Gói khám tiêu hóa toàn diện, bao gồm nội soi dạ dày, siêu âm bụng và các xét nghiệm gan mật",
            new BigDecimal("2200000"), "gastrointestinal_check.jpg", 7),

        new MedicalPackageData("Gói Khám Mắt Cơ Bản",
            "Gói khám mắt tổng quát, bao gồm đo thị lực, khám mắt ngoài và khám đáy mắt",
            new BigDecimal("450000"), "basic_eye_check.jpg", 3),

        new MedicalPackageData("Gói Khám Tai Mũi Họng",
            "Gói khám tai mũi họng, bao gồm khám tai mũi họng và nội soi tai mũi họng",
            new BigDecimal("600000"), "ent_check.jpg", 4),

        new MedicalPackageData("Gói Khám Da Liễu",
            "Gói khám da liễu, bao gồm tư vấn da liễu và các xét nghiệm da chuyên biệt",
            new BigDecimal("550000"), "dermatology_check.jpg", 3),

        new MedicalPackageData("Gói Khám Răng Hàm Mặt Cơ Bản",
            "Gói khám răng miệng cơ bản, bao gồm khám răng hàm mặt và tư vấn vệ sinh răng miệng",
            new BigDecimal("400000"), "basic_dental_check.jpg", 2));
  }

  @lombok.Data
  @lombok.AllArgsConstructor
  private static class MedicalServiceData {
    private String name;
    private String description;
    private int processingPriority;
    private JsonNode formTemplate;
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
