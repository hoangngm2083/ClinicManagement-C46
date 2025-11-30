package com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.parser;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

//@Component
@Slf4j
public class FormIoTemplateParserImpl implements FormTemplateParser {

    public String parse(JsonNode formTemplate, JsonNode resultData) {
        if (formTemplate == null || resultData == null) {
            return "";
        }

        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<div class='exam-result-container'>");

        JsonNode components = formTemplate.get("components");
        if (components != null && components.isArray()) {
            for (JsonNode component : components) {
                processComponent(component, resultData, htmlBuilder);
            }
        }

        htmlBuilder.append("</div>");
        return htmlBuilder.toString();
    }

    private void processComponent(JsonNode component, JsonNode resultData, StringBuilder htmlBuilder) {
        String type = component.has("type") ? component.get("type")
                .asText() : "";
        String key = component.has("key") ? component.get("key")
                .asText() : "";
        String label = component.has("label") ? component.get("label")
                .asText() : "";

        // Skip if no key or data
        if (key.isEmpty() || !resultData.has(key)) {
            // Handle layout components or static text if needed, but for now focus on data
            // fields
            if ("htmlelement".equals(type) || "content".equals(type)) {
                if (component.has("content")) {
                    htmlBuilder.append("<div class='static-content'>")
                            .append(component.get("content")
                                    .asText())
                            .append("</div>");
                }
                return;
            }
            // If it's a container/layout, we might need to recurse, but for simplicity
            // let's stick to flat fields first
            // or check if it has 'components'
            if (component.has("components")) {
                JsonNode subComponents = component.get("components");
                if (subComponents.isArray()) {
                    for (JsonNode sub : subComponents) {
                        processComponent(sub, resultData, htmlBuilder);
                    }
                }
                return;
            }
            return;
        }

        JsonNode valueNode = resultData.get(key);
        String value = valueNode.asText();

        htmlBuilder.append("<div class='result-item'>");
        htmlBuilder.append("<span class='label'>")
                .append(label)
                .append(": </span>");
        htmlBuilder.append("<span class='value'>")
                .append(value)
                .append("</span>");
        htmlBuilder.append("</div>");
    }
}
