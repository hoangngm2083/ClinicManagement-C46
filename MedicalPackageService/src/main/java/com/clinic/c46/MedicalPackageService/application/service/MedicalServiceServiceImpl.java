package com.clinic.c46.MedicalPackageService.application.service;

import com.clinic.c46.CommonService.exception.ResourceNotFoundException;
import com.clinic.c46.CommonService.query.medicalPackage.ExistsServiceByIdQuery;
import com.clinic.c46.MedicalPackageService.domain.command.CreateMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.DeleteMedicalServiceCommand;
import com.clinic.c46.MedicalPackageService.domain.command.UpdateMedicalServiceInfoCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.clinic.c46.MedicalPackageService.application.dto.MedicalServiceExportDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalServiceServiceImpl implements MedicalServiceService {
    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    @Override
    public void create(CreateMedicalServiceCommand cmd) {
        log.debug("=== CreateMedicalServiceCommand: {}", cmd);
        validateDepartmentExists(cmd.departmentId());
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void update(UpdateMedicalServiceInfoCommand cmd) {
        log.debug("=== UpdateMedicalServiceInfoCommand: {}", cmd);
        validateServiceExists(cmd.medicalServiceId());
        validateDepartmentExists(cmd.departmentId());
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void delete(DeleteMedicalServiceCommand cmd) {
        log.debug("=== DeleteMedicalServiceCommand: {}", cmd);
        validateServiceExists(cmd.medicalServiceId());
        commandGateway.sendAndWait(cmd);
    }

    private void validateServiceExists(String serviceId) {
        try {
            Boolean exists = queryGateway.query(new ExistsServiceByIdQuery(serviceId),
                    ResponseTypes.instanceOf(Boolean.class)).join();
            if (Boolean.FALSE.equals(exists)) {
                log.warn("Medical service not found: {}", serviceId);
                throw new ResourceNotFoundException("Dịch vụ y tế");
            }
        } catch (Exception e) {
            if (e instanceof ResourceNotFoundException) {
                throw e;
            }
            log.error("Error validating service existence: {}", serviceId, e);
            throw new RuntimeException("Lỗi khi kiểm tra dịch vụ tồn tại", e);
        }
    }

    private void validateDepartmentExists(String departmentId) {
        if (departmentId == null || departmentId.isEmpty()) return;

        try {
            Boolean exists = queryGateway.query(new com.clinic.c46.CommonService.query.department.ExistsDepartmentByIdQuery(departmentId),
                    ResponseTypes.instanceOf(Boolean.class)).join();

            if (Boolean.FALSE.equals(exists)) {
                log.warn("Department not found: {}", departmentId);
                throw new ResourceNotFoundException("Phòng ban");
            }
        } catch (Exception e) {
            if (e instanceof ResourceNotFoundException) {
                throw e;
            }
            log.error("Error validating department existence: {}", departmentId, e);
            throw new RuntimeException("Lỗi khi kiểm tra phòng ban tồn tại", e);
        }
    }


    @Override
    public java.util.concurrent.CompletableFuture<byte[]> exportServices(com.clinic.c46.MedicalPackageService.domain.query.GetAllMedicalServicesQuery query) {
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(MedicalServiceExportDTO.class))
            .thenApply(this::generateServiceCsv);
    }

    private byte[] generateServiceCsv(java.util.List<MedicalServiceExportDTO> exportList) {
        try (java.io.StringWriter writer = new java.io.StringWriter()) {
            ColumnPositionMappingStrategy<MedicalServiceExportDTO> strategy = new ColumnPositionMappingStrategy<>();
            strategy.setType(MedicalServiceExportDTO.class);
            String[] columns = new String[]{"id", "name", "description", "departmentName", "departmentId", "processingPriority", "formTemplate", "createdAt", "updatedAt", "deletedAt"};
            strategy.setColumnMapping(columns);

            StatefulBeanToCsv<MedicalServiceExportDTO> beanToCsv =
                    new StatefulBeanToCsvBuilder<MedicalServiceExportDTO>(writer)
                            .withMappingStrategy(strategy)
                            .withQuotechar(com.opencsv.CSVWriter.DEFAULT_QUOTE_CHARACTER)
                            .build();

            // Manually write header if needed, but beanToCsv with mapping strategy might not write named headers correctly by default.
            // Actually, for named headers with specific order, we might need a custom strategy or just write header manually.
            
            // Standard OpenCSV doesn't make it easy to HAVE BOTH named headers AND specific order without custom strategy.
            // Let's use a simpler approach: HeaderColumnNameMappingStrategy with a custom comparator if available, 
            // OR just write the header line first and then use the position strategy.
            
            // Let's try the simplest robust way:
            writer.write("id,name,description,departmentName,departmentId,processingPriority,formTemplate,createdAt,updatedAt,deletedAt\n");
            
            beanToCsv.write(exportList);
            return writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error generating CSV", e);
        }
    }
}
