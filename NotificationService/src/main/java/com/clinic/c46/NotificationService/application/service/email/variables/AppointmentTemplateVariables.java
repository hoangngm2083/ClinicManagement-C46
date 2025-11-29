package com.clinic.c46.NotificationService.application.service.email.variables;

import lombok.Builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
public record AppointmentTemplateVariables(
        String patientName,
        String appointmentId,
        String appointmentDate,
        int shift,
        String appointmentState,
        String medicalPackageName,
        List<ServiceItem> services) implements EmailTemplateVariables {

    @Builder
    public record ServiceItem(String name) {
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("patientName", patientName);
        map.put("appointmentId", appointmentId);
        map.put("appointmentDate", appointmentDate);
        map.put("shift", shift);
        map.put("appointmentState", appointmentState);
        map.put("medicalPackageName", medicalPackageName);
        map.put("services", services.stream().map(service -> {
            Map<String, String> serviceMap = new HashMap<>();
            serviceMap.put("name", service.name());
            return serviceMap;
        }).collect(Collectors.toList()));
        return map;
    }
}
