package com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.template.variables;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

@Builder
public record AppointmentReminderTemplateVariables(
        String patientName,
        String appointmentDate,
        int shift,
        String medicalPackageName) implements EmailTemplateVariables {

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("patientName", patientName);
        map.put("appointmentDate", appointmentDate);
        map.put("shift", shift);
        map.put("medicalPackageName", medicalPackageName);
        return map;
    }
}
