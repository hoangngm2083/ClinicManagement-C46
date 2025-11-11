package com.clinic.c46.StaffService.application.service;

import com.clinic.c46.StaffService.application.dto.CreateStaffRequest;
import com.clinic.c46.StaffService.application.dto.RequestDayOffsRequest;
import com.clinic.c46.StaffService.application.dto.UpdateStaffRequest;

import java.util.concurrent.CompletableFuture;

public interface StaffService {
    CompletableFuture<String> createStaff(CreateStaffRequest request);

    CompletableFuture<Void> updateStaff(String staffId, UpdateStaffRequest request);

    CompletableFuture<Void> requestDayOff(String staffId, RequestDayOffsRequest request);

    CompletableFuture<Void> deleteStaff(String staffId);
}
