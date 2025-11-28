package com.clinic.c46.CommonService.command.examination;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
public record AddResultCommand(@TargetAggregateIdentifier String examId, String doctorId, String serviceId,
        JsonNode data) {

}
