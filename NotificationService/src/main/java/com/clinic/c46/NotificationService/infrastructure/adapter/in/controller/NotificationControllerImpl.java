package com.clinic.c46.NotificationService.infrastructure.adapter.in.controller;

import com.clinic.c46.NotificationService.application.port.in.NotificationController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationControllerImpl implements NotificationController {
}