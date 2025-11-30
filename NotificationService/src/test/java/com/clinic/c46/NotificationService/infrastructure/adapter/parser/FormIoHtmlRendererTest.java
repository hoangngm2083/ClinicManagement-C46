package com.clinic.c46.NotificationService.infrastructure.adapter.parser;

import com.clinic.c46.NotificationService.infrastructure.adapter.observer.email.parser.FormIoHtmlRenderer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FormIoHtmlRenderer
 * Demonstrating all supported Form.io component types
 */
class FormIoHtmlRendererTest {

    private FormIoHtmlRenderer renderer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        renderer = new FormIoHtmlRenderer();
        objectMapper = new ObjectMapper();
    }

    /**
     * Helper method to unescape HTML for assertion
     * This allows us to compare Vietnamese text that may have been escaped
     */
    private void assertContainsText(String html, String expectedText) {
        String unescaped = StringEscapeUtils.unescapeHtml4(html);
        assertThat(unescaped).contains(expectedText);
    }

    @Test
    void testBasicTextField() throws Exception {
        String templateJson = """
            {
              "components": [
                {
                  "type": "text",
                  "key": "patientName",
                  "label": "Tên bệnh nhân"
                }
              ]
            }
            """;

        String dataJson = """
            {
              "patientName": "Nguyễn Văn A"
            }
            """;

        JsonNode template = objectMapper.readTree(templateJson);
        JsonNode data = objectMapper.readTree(dataJson);

        String html = renderer.parse(template, data);

        assertThat(html).contains("exam-result-container");
        assertThat(html).contains("form-field");
        assertThat(html).contains("field-label");
        assertContainsText(html, "Tên bệnh nhân");
        assertContainsText(html, "Nguyễn Văn A");
    }

    @Test
    void testTextareaWithNewlines() throws Exception {
        String templateJson = """
            {
              "components": [
                {
                  "type": "textarea",
                  "key": "description",
                  "label": "Mô tả"
                }
              ]
            }
            """;

        String dataJson = """
            {
              "description": "Dòng 1\\nDòng 2\\nDòng 3"
            }
            """;

        JsonNode template = objectMapper.readTree(templateJson);
        JsonNode data = objectMapper.readTree(dataJson);

        String html = renderer.parse(template, data);

        assertThat(html).contains("<br>");
        assertContainsText(html, "Dòng 1");
        assertContainsText(html, "Dòng 2");
        assertContainsText(html, "Dòng 3");
    }

    @Test
    void testXSSProtection() throws Exception {
        String templateJson = """
            {
              "components": [
                {
                  "type": "text",
                  "key": "maliciousInput",
                  "label": "Test XSS"
                }
              ]
            }
            """;

        String dataJson = """
            {
              "maliciousInput": "<script>alert('XSS')</script>"
            }
            """;

        JsonNode template = objectMapper.readTree(templateJson);
        JsonNode data = objectMapper.readTree(dataJson);

        String html = renderer.parse(template, data);

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("&lt;/script&gt;");
    }

    @Test
    void testPanelWithNestedComponents() throws Exception {
        String templateJson = """
            {
              "components": [
                {
                  "type": "panel",
                  "title": "Thông tin bệnh nhân",
                  "components": [
                    {
                      "type": "text",
                      "key": "name",
                      "label": "Họ tên"
                    },
                    {
                      "type": "number",
                      "key": "age",
                      "label": "Tuổi"
                    }
                  ]
                }
              ]
            }
            """;

        String dataJson = """
            {
              "name": "Nguyễn Văn B",
              "age": 30
            }
            """;

        JsonNode template = objectMapper.readTree(templateJson);
        JsonNode data = objectMapper.readTree(dataJson);

        String html = renderer.parse(template, data);

        assertThat(html).contains("form-panel");
        assertThat(html).contains("panel-title");
        assertContainsText(html, "Thông tin bệnh nhân");
        assertContainsText(html, "Nguyễn Văn B");
        assertThat(html).contains("30");
    }

    @Test
    void testColumnsLayout() throws Exception {
        String templateJson = """
            {
              "components": [
                {
                  "type": "columns",
                  "columns": [
                    {
                      "components": [
                        {
                          "type": "text",
                          "key": "leftField",
                          "label": "Trái"
                        }
                      ]
                    },
                    {
                      "components": [
                        {
                          "type": "text",
                          "key": "rightField",
                          "label": "Phải"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """;

        String dataJson = """
            {
              "leftField": "Giá trị trái",
              "rightField": "Giá trị phải"
            }
            """;

        JsonNode template = objectMapper.readTree(templateJson);
        JsonNode data = objectMapper.readTree(dataJson);

        String html = renderer.parse(template, data);

        assertThat(html).contains("form-columns");
        assertThat(html).contains("form-column");
        assertContainsText(html, "Giá trị trái");
        assertContainsText(html, "Giá trị phải");
    }

    @Test
    void testFileRendering() throws Exception {
        String templateJson = """
            {
              "components": [
                {
                  "type": "file",
                  "key": "xrayImage",
                  "label": "Ảnh X-Quang"
                }
              ]
            }
            """;

        String dataJson = """
            {
              "xrayImage": "https://example.com/xray.png"
            }
            """;

        JsonNode template = objectMapper.readTree(templateJson);
        JsonNode data = objectMapper.readTree(dataJson);

        String html = renderer.parse(template, data);

        assertThat(html).contains("<img");
        assertThat(html).contains("medical-image");
        assertThat(html).contains("https://example.com/xray.png");
    }

    @Test
    void testCheckboxRendering() throws Exception {
        String templateJson = """
            {
              "components": [
                {
                  "type": "checkbox",
                  "key": "agreementChecked",
                  "label": "Đồng ý điều khoản"
                },
                {
                  "type": "checkbox",
                  "key": "agreementUnchecked",
                  "label": "Không đồng ý"
                }
              ]
            }
            """;

        String dataJson = """
            {
              "agreementChecked": true,
              "agreementUnchecked": false
            }
            """;

        JsonNode template = objectMapper.readTree(templateJson);
        JsonNode data = objectMapper.readTree(dataJson);

        String html = renderer.parse(template, data);

        assertThat(html).contains("✓ Có");
        assertThat(html).contains("✗ Không");
    }

    @Test
    void testEmptyTemplate() throws Exception {
        String templateJson = """
            {
              "components": []
            }
            """;

        String dataJson = "{}";

        JsonNode template = objectMapper.readTree(templateJson);
        JsonNode data = objectMapper.readTree(dataJson);

        String html = renderer.parse(template, data);

        assertThat(html).contains("exam-result-container");
        assertThat(html).doesNotContain("form-field");
    }

    @Test
    void testNullInputs() {
        String html = renderer.parse(null, null);
        assertThat(html).isEmpty();
    }

    @Test
    void testComplexNestedStructure() throws Exception {
        String templateJson = """
            {
              "components": [
                {
                  "type": "panel",
                  "title": "Kết quả xét nghiệm máu",
                  "components": [
                    {
                      "type": "columns",
                      "columns": [
                        {
                          "components": [
                            {
                              "type": "number",
                              "key": "wbc",
                              "label": "WBC"
                            }
                          ]
                        },
                        {
                          "components": [
                            {
                              "type": "number",
                              "key": "rbc",
                              "label": "RBC"
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "type": "textarea",
                      "key": "notes",
                      "label": "Ghi chú"
                    }
                  ]
                }
              ]
            }
            """;

        String dataJson = """
            {
              "wbc": 7.2,
              "rbc": 4.8,
              "notes": "Kết quả bình thường\\nKhông cần xét nghiệm thêm"
            }
            """;

        JsonNode template = objectMapper.readTree(templateJson);
        JsonNode data = objectMapper.readTree(dataJson);

        String html = renderer.parse(template, data);

        assertThat(html).contains("form-panel");
        assertThat(html).contains("panel-title");
        assertContainsText(html, "Kết quả xét nghiệm máu");
        assertThat(html).contains("form-columns");
        assertThat(html).contains("7.2");
        assertThat(html).contains("4.8");
        assertContainsText(html, "Kết quả bình thường");
        assertContainsText(html, "Không cần xét nghiệm thêm");
    }
}
