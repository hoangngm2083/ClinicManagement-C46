package com.clinic.c46.BookingService.application.handler.query;


import com.clinic.c46.BookingService.application.dto.SlotResponse;
import com.clinic.c46.BookingService.application.dto.SlotsPagedResponse;
import com.clinic.c46.BookingService.application.port.out.SlotRepository;
import com.clinic.c46.BookingService.domain.query.GetAllSlotOfPackageQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlotQueryHandler {

    private final SlotRepository slotRepository;

    @QueryHandler
    public SlotsPagedResponse handle(GetAllSlotOfPackageQuery query) {
        List<SlotResponse> slots = slotRepository.findAllByMedicalPackageId(query.medicalPackageId())
                .stream()
                .map(entity -> SlotResponse.builder()
                        .slotId(entity.getSlotId())
                        .medicalPackageId(query.medicalPackageId())
                        .shift(entity.getShift())
                        .maxQuantity(entity.getMaxQuantity())
                        .remainingQuantity(entity.getRemainingQuantity())
                        .date(entity.getDate())
                        .build())
                .collect(Collectors.toList());

        return SlotsPagedResponse.builder()
                .content(slots)
                .page(0)
                .size(slots.size())
                .total(slots.size())
                .build();
    }


}
