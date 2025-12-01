package com.clinic.c46.NotificationService.infrastructure.adapter.strategy.email.template.variables;

import lombok.Builder;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
public record InvoiceTemplateVariables(
        String patientName,
        String invoiceId,
        String paymentDate,
        String totalAmount,
        List<InvoiceItem> items) implements EmailTemplateVariables {
    @Builder
    public record InvoiceItem(String name, String price) {
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("patientName", patientName);
        map.put("invoiceId", invoiceId);
        map.put("paymentDate", paymentDate);
        map.put("totalAmount", totalAmount);
        map.put("items", items.stream().map(item -> {
            Map<String, String> itemMap = new HashMap<>();
            itemMap.put("name", item.name());
            itemMap.put("price", item.price());
            return itemMap;
        }).collect(Collectors.toList()));
        return map;
    }
}
