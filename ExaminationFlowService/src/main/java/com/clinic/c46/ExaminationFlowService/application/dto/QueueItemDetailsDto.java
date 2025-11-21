package com.clinic.c46.ExaminationFlowService.application.dto;


public record QueueItemDetailsDto(String queueItemId, String medicalFormId, String serviceId, String queueId,
                                  String staffId, String status) {
}
