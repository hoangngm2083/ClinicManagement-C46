package com.clinic.c46.StaffService.infrastructure.adapter.web.controller;


import com.clinic.c46.StaffService.application.dto.CreateDepartmentRequest;
import com.clinic.c46.StaffService.application.dto.DepartmentDTO;
import com.clinic.c46.StaffService.application.dto.DepartmentsPagedDTO;
import com.clinic.c46.StaffService.application.service.DepartmentService;
import com.clinic.c46.StaffService.domain.command.CreateDepartmentCommand;
import com.clinic.c46.StaffService.domain.command.DeleteDepartmentCommand;
import com.clinic.c46.StaffService.domain.query.GetAllDepartmentsQuery;
import com.clinic.c46.StaffService.domain.query.GetDepartmentByIdQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/department")
@RequiredArgsConstructor
@Validated
public class DepartmentController {

    private final DepartmentService departmentService;
    private final QueryGateway queryGateway;

    @GetMapping
    public ResponseEntity<DepartmentsPagedDTO> getAll(@RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "0") int size,
            @RequestParam(value = "keyword", required = false) String keyword) {

        GetAllDepartmentsQuery query = GetAllDepartmentsQuery.builder()
                .page(page)
                .size(size)
                .keyword(keyword)
                .build();

        DepartmentsPagedDTO departmentsPagedDTO = queryGateway.query(query,
                        ResponseTypes.instanceOf(DepartmentsPagedDTO.class))
                .join();
        return ResponseEntity.ok(departmentsPagedDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDTO> getById(@PathVariable String id) {

        GetDepartmentByIdQuery query = GetDepartmentByIdQuery.builder()
                .departmentId(id)
                .build();

        DepartmentDTO departmentDTO = queryGateway.query(query, ResponseTypes.instanceOf(DepartmentDTO.class))
                .join();
        return ResponseEntity.ok(departmentDTO);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@Valid @RequestBody CreateDepartmentRequest requestBody) {


        String departmentId = UUID.randomUUID()
                .toString();

        CreateDepartmentCommand cmd = CreateDepartmentCommand.builder()
                .departmentId(departmentId)
                .description(requestBody.description())
                .name(requestBody.name())
                .build();
        departmentService.create(cmd);

        return ResponseEntity.created(URI.create("/department/" + departmentId))
                .body(Map.of("departmentId", departmentId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        DeleteDepartmentCommand cmd = DeleteDepartmentCommand.builder()
                .departmentId(id)
                .build();
        departmentService.delete(cmd);
        return ResponseEntity.ok(Map.of("departmentId", id, "status", "deleted"));
    }

}
