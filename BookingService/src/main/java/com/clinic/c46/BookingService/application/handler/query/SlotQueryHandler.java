package com.clinic.c46.BookingService.application.handler.query;


import com.clinic.c46.BookingService.application.repository.SlotViewRepository;
import com.clinic.c46.BookingService.domain.query.ExistsBySlotIdQuery;
import com.clinic.c46.BookingService.domain.query.FindSlotByIdQuery;
import com.clinic.c46.BookingService.domain.query.GetAllSlotOfPackageQuery;
import com.clinic.c46.BookingService.domain.view.SlotView;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.SlotResponse;
import com.clinic.c46.BookingService.infrastructure.adapter.in.web.dto.SlotsPagedResponse;
import com.clinic.c46.CommonService.helper.PageAndSortHelper;
import com.clinic.c46.CommonService.helper.SortDirection;
import com.clinic.c46.CommonService.query.BaseQueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotQueryHandler extends BaseQueryHandler {

    private final SlotViewRepository slotRepository;
    private final PageAndSortHelper pageAndSortHelper;

    @QueryHandler
    public SlotsPagedResponse handle(GetAllSlotOfPackageQuery query) {

        Pageable pageable = pageAndSortHelper.buildPageable(1, 50, "date", SortDirection.ASC);

        Page<SlotView> slotPage = slotRepository.findAllByMedicalPackageIdAndDateBetween(query.medicalPackageId(), query.dateFrom(), query.dateTo(), pageable);

        return pageAndSortHelper.toPaged(slotPage, entity -> SlotResponse.builder()
                .slotId(entity.getSlotId())
                .medicalPackageId(query.medicalPackageId())
                .shift(entity.getShift())
                .maxQuantity(entity.getMaxQuantity())
                .remainingQuantity(entity.getRemainingQuantity())
                .date(entity.getDate())
                .build(), SlotsPagedResponse::new);

    }


    @QueryHandler
    public SlotView handle(FindSlotByIdQuery query) {
        return slotRepository.findById(query.slotId())
                .orElse(null);

    }

    @QueryHandler
    public Boolean handle(ExistsBySlotIdQuery query) {
        return slotRepository.existsById(query.slotId());
    }


}
