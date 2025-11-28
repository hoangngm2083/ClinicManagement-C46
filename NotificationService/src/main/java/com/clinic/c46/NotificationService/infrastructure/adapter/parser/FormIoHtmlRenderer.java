package com.clinic.c46.NotificationService.infrastructure.adapter.parser;

import com.clinic.c46.NotificationService.application.service.email.FormTemplateParser;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * FormIoHtmlRenderer - Modern, production-ready renderer for Form.io schemas
 * 
 * ✅ Improvements over old parser:
 * 1. Supports nested components (panels, columns, fieldsets)
 * 2. Type-specific renderers (text, textarea, number, date, select, file, image)
 * 3. XSS protection via HTML escaping
 * 4. Clean modular architecture
 * 5. CSS class standardization for consistent PDF rendering
 * 6. Proper layout handling (panels, columns, containers)
 */
@Component("formIoHtmlRenderer")
@Slf4j
public class FormIoHtmlRenderer implements FormTemplateParser {

    private static final String CONTAINER_CLASS = "exam-result-container";
    private static final String FIELD_CLASS = "form-field";
    private static final String LABEL_CLASS = "field-label";
    private static final String VALUE_CLASS = "field-value";
    private static final String PANEL_CLASS = "form-panel";
    private static final String PANEL_TITLE_CLASS = "panel-title";
    private static final String COLUMNS_CLASS = "form-columns";
    private static final String COLUMN_CLASS = "form-column";
    private static final String IMAGE_CLASS = "medical-image";
    private static final String FILE_LINK_CLASS = "file-link";
    private static final String STATIC_CONTENT_CLASS = "static-content";

    @Override
    public String parse(JsonNode formTemplate, JsonNode resultData) {
        if (formTemplate == null || resultData == null) {
            log.warn("FormTemplate or ResultData is null, returning empty string");
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class='").append(CONTAINER_CLASS).append("'>");

        JsonNode components = formTemplate.get("components");
        if (components != null && components.isArray()) {
            parseComponents(components, resultData, html);
        } else {
            log.warn("No 'components' array found in formTemplate");
        }

        html.append("</div>");
        return html.toString();
    }

    /**
     * Recursively parse components array
     * Supports nested structures like panels, columns, containers
     */
    private void parseComponents(JsonNode components, JsonNode resultData, StringBuilder html) {
        for (JsonNode component : components) {
            processComponent(component, resultData, html);
        }
    }

    /**
     * Main component processor - routes to appropriate renderer based on type
     */
    private void processComponent(JsonNode component, JsonNode resultData, StringBuilder html) {
        String type = getComponentProperty(component, "type");
        
        switch (type) {
            case "panel":
                renderPanel(component, resultData, html);
                break;
            case "columns":
                renderColumns(component, resultData, html);
                break;
            case "fieldset":
            case "container":
                renderContainer(component, resultData, html);
                break;
            case "htmlelement":
            case "content":
                renderStaticContent(component, html);
                break;
            case "file":
                renderFile(component, resultData, html);
                break;
            default:
                renderBasicField(component, resultData, html);
                break;
        }
    }

    /**
     * Render Panel component (section with title)
     */
    private void renderPanel(JsonNode component, JsonNode resultData, StringBuilder html) {
        String title = getComponentProperty(component, "title");
        String label = getComponentProperty(component, "label");
        
        html.append("<div class='").append(PANEL_CLASS).append("'>");
        
        // Panel title
        String panelTitle = !title.isEmpty() ? title : label;
        if (!panelTitle.isEmpty()) {
            html.append("<div class='").append(PANEL_TITLE_CLASS).append("'>")
                .append(escapeHtml(panelTitle))
                .append("</div>");
        }
        
        // Panel content (nested components)
        JsonNode subComponents = component.get("components");
        if (subComponents != null && subComponents.isArray()) {
            parseComponents(subComponents, resultData, html);
        }
        
        html.append("</div>");
    }

    /**
     * Render Columns layout component
     */
    private void renderColumns(JsonNode component, JsonNode resultData, StringBuilder html) {
        JsonNode columns = component.get("columns");
        if (columns == null || !columns.isArray()) {
            return;
        }
        
        html.append("<div class='").append(COLUMNS_CLASS).append("'>");
        
        for (JsonNode column : columns) {
            html.append("<div class='").append(COLUMN_CLASS).append("'>");
            
            JsonNode columnComponents = column.get("components");
            if (columnComponents != null && columnComponents.isArray()) {
                parseComponents(columnComponents, resultData, html);
            }
            
            html.append("</div>");
        }
        
        html.append("</div>");
    }

    /**
     * Render Container/Fieldset component
     */
    private void renderContainer(JsonNode component, JsonNode resultData, StringBuilder html) {
        String label = getComponentProperty(component, "label");
        
        html.append("<div class='form-container'>");
        
        if (!label.isEmpty()) {
            html.append("<div class='container-label'>")
                .append(escapeHtml(label))
                .append("</div>");
        }
        
        JsonNode subComponents = component.get("components");
        if (subComponents != null && subComponents.isArray()) {
            parseComponents(subComponents, resultData, html);
        }
        
        html.append("</div>");
    }

    /**
     * Render static HTML content
     */
    private void renderStaticContent(JsonNode component, StringBuilder html) {
        String content = getComponentProperty(component, "content");
        if (!content.isEmpty()) {
            html.append("<div class='").append(STATIC_CONTENT_CLASS).append("'>")
                .append(content) // Content may contain HTML, don't escape
                .append("</div>");
        }
    }

    /**
     * Render File/Image component
     */
    private void renderFile(JsonNode component, JsonNode resultData, StringBuilder html) {
        String key = getComponentProperty(component, "key");
        String label = getComponentProperty(component, "label");
        
        if (key.isEmpty() || !resultData.has(key)) {
            return;
        }

        JsonNode valueNode = resultData.get(key);
        
        // Skip if no value
        if (valueNode.isNull() || (valueNode.isArray() && valueNode.isEmpty())) {
            return;
        }

        html.append("<div class='").append(FIELD_CLASS).append("'>");
        
        if (!label.isEmpty()) {
            html.append("<label class='").append(LABEL_CLASS).append("'>")
                .append(escapeHtml(label))
                .append(":</label>");
        }
        
        html.append("<div class='").append(VALUE_CLASS).append("'>");
        
        // Handle array of files
        if (valueNode.isArray()) {
            for (JsonNode fileNode : valueNode) {
                renderFileItem(fileNode, html);
            }
        } else {
            renderFileItem(valueNode, html);
        }
        
        html.append("</div>");
        html.append("</div>");
    }

    /**
     * Render individual file item (image or download link)
     */
    private void renderFileItem(JsonNode fileNode, StringBuilder html) {
        String url = "";
        String name = "";
        String type = "";
        
        if (fileNode.isTextual()) {
            // Simple string URL
            url = fileNode.asText();
            name = extractFileNameFromUrl(url);
        } else if (fileNode.isObject()) {
            // File object with metadata
            url = getComponentProperty(fileNode, "url");
            name = getComponentProperty(fileNode, "name");
            type = getComponentProperty(fileNode, "type");
            
            if (url.isEmpty() && fileNode.has("storage") && fileNode.get("storage").asText().equals("url")) {
                url = getComponentProperty(fileNode, "storage");
            }
        }
        
        if (url.isEmpty()) {
            return;
        }
        
        // Check if it's an image
        if (isImageFile(url, type)) {
            html.append("<img src='").append(escapeHtml(url))
                .append("' alt='").append(escapeHtml(name))
                .append("' class='").append(IMAGE_CLASS).append("' />");
        } else {
            // Render as download link
            html.append("<a href='").append(escapeHtml(url))
                .append("' class='").append(FILE_LINK_CLASS)
                .append("' target='_blank'>")
                .append(escapeHtml(name.isEmpty() ? "Download File" : name))
                .append("</a>");
        }
    }

    /**
     * Render basic input field (text, textarea, number, date, select, etc.)
     */
    private void renderBasicField(JsonNode component, JsonNode resultData, StringBuilder html) {
        String key = getComponentProperty(component, "key");
        String label = getComponentProperty(component, "label");
        String type = getComponentProperty(component, "type");
        
        // Skip if no key
        if (key.isEmpty()) {
            return;
        }
        
        // Skip if no data for this field
        if (!resultData.has(key)) {
            return;
        }

        JsonNode valueNode = resultData.get(key);
        
        // Skip null values
        if (valueNode.isNull()) {
            return;
        }

        html.append("<div class='").append(FIELD_CLASS).append("'>");
        
        // Render label
        if (!label.isEmpty()) {
            html.append("<label class='").append(LABEL_CLASS).append("'>")
                .append(escapeHtml(label))
                .append(":</label>");
        }
        
        // Render value based on type
        html.append("<div class='").append(VALUE_CLASS).append("'>");
        
        switch (type) {
            case "textarea":
                renderTextarea(valueNode, html);
                break;
            case "number":
                renderNumber(valueNode, html);
                break;
            case "date":
            case "datetime":
                renderDate(valueNode, html);
                break;
            case "select":
            case "radio":
                renderSelect(valueNode, component, html);
                break;
            case "checkbox":
                renderCheckbox(valueNode, html);
                break;
            default:
                renderText(valueNode, html);
                break;
        }
        
        html.append("</div>");
        html.append("</div>");
    }

    /**
     * Render text input value
     */
    private void renderText(JsonNode valueNode, StringBuilder html) {
        String value = valueNode.asText("");
        html.append(escapeHtml(value));
    }

    /**
     * Render textarea value (preserve newlines)
     */
    private void renderTextarea(JsonNode valueNode, StringBuilder html) {
        String value = valueNode.asText("");
        // Convert newlines to <br> for HTML display
        String htmlValue = escapeHtml(value).replace("\n", "<br>");
        html.append(htmlValue);
    }

    /**
     * Render number value
     */
    private void renderNumber(JsonNode valueNode, StringBuilder html) {
        if (valueNode.isNumber()) {
            html.append(valueNode.asText());
        } else {
            html.append(escapeHtml(valueNode.asText("")));
        }
    }

    /**
     * Render date value with formatting
     */
    private void renderDate(JsonNode valueNode, StringBuilder html) {
        String dateStr = valueNode.asText("");
        
        try {
            // Try to parse and format the date
            LocalDate date = LocalDate.parse(dateStr);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            html.append(date.format(formatter));
        } catch (DateTimeParseException e) {
            // If parsing fails, just display the raw value
            html.append(escapeHtml(dateStr));
        }
    }

    /**
     * Render select/radio value (try to get label from options)
     */
    private void renderSelect(JsonNode valueNode, JsonNode component, StringBuilder html) {
        String value = valueNode.asText("");
        
        // Try to find the label from options
        JsonNode data = component.get("data");
        if (data != null && data.has("values")) {
            JsonNode values = data.get("values");
            if (values.isArray()) {
                for (JsonNode option : values) {
                    String optionValue = getComponentProperty(option, "value");
                    if (optionValue.equals(value)) {
                        String label = getComponentProperty(option, "label");
                        if (!label.isEmpty()) {
                            html.append(escapeHtml(label));
                            return;
                        }
                    }
                }
            }
        }
        
        // Fallback to raw value
        html.append(escapeHtml(value));
    }

    /**
     * Render checkbox value
     */
    private void renderCheckbox(JsonNode valueNode, StringBuilder html) {
        boolean checked = valueNode.asBoolean(false);
        html.append(checked ? "✓ Có" : "✗ Không");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get component property safely
     */
    private String getComponentProperty(JsonNode component, String property) {
        if (component == null || !component.has(property)) {
            return "";
        }
        JsonNode node = component.get(property);
        return node.isNull() ? "" : node.asText("");
    }

    /**
     * Escape HTML to prevent XSS
     */
    private String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return StringEscapeUtils.escapeHtml4(text);
    }

    /**
     * Check if file is an image based on URL or MIME type
     */
    private boolean isImageFile(String url, String mimeType) {
        if (!mimeType.isEmpty() && mimeType.startsWith("image/")) {
            return true;
        }
        
        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") ||
               lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif") ||
               lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".svg") ||
               lowerUrl.endsWith(".bmp");
    }

    /**
     * Extract filename from URL
     */
    private String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }
        
        return url;
    }
}
