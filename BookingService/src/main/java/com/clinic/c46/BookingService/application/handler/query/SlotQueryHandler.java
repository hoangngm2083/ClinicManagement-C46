package com.clinic.c46.BookingService.application.handler.query;


import com.clinic.c46.BookingService.application.repository.SlotViewRepository;
import com.clinic.c46.BookingService.domain.query.FindSlotByIdQuery;
import com.clinic.c46.BookingService.domain.query.GetAllSlotOfPackageQuery;
import com.clinic.c46.BookingService.domain.view.SlotView;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.SlotResponse;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.SlotsPagedResponse;
import com.clinic.c46.CommonService.query.BaseQueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotQueryHandler extends BaseQueryHandler {

    private final SlotViewRepository slotRepository;

    @QueryHandler
    public SlotsPagedResponse handle(GetAllSlotOfPackageQuery query) {
        int page = Math.max(query.page(), 0);

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);

        Page<SlotView> slotPage = slotRepository.findAllByMedicalPackageId(query.medicalPackageId(), pageable);

        List<SlotResponse> slots = slotPage.getContent().stream()
                .map(entity -> SlotResponse.builder()
                        .slotId(entity.getSlotId())
                        .medicalPackageId(query.medicalPackageId())
                        .shift(entity.getShift())
                        .maxQuantity(entity.getMaxQuantity())
                        .remainingQuantity(entity.getRemainingQuantity())
                        .date(entity.getDate())
                        .build())
                .toList();

        return SlotsPagedResponse.builder()
                .content(slots)
                .page(page)
                .size(slots.size())
                .total((int) slotPage.getTotalElements())
                .totalPages(slotPage.getTotalPages())
                .build();
    }



    @QueryHandler
    public SlotView handle(FindSlotByIdQuery query) {
        return slotRepository.findById(query.slotId())
                .orElse(null);

    }


}
