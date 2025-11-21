package com.clinic.c46.AuthService.infrastructure.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfoResponse {
    private Integer accountId;
    private String accountName;
    private String staffId;
    private String role;
}
