package com.clinic.c46.BookingService.application.dto;

import com.clinic.c46.CommonService.dto.BasePagedResponse;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor(force = true)
@SuperBuilder
public class SlotsPagedResponse extends BasePagedResponse<SlotResponse> {
}

