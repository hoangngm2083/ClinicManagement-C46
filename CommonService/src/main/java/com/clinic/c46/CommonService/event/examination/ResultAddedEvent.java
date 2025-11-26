package com.clinic.c46.CommonService.event.examination;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

@Builder
public record ResultAddedEvent(String examinationId, String doctorId, String serviceId, JsonNode data) {

}
