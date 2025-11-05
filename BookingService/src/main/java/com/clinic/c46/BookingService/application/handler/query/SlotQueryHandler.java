package com.clinic.c46.BookingService.application.handler.query;


import com.clinic.c46.BookingService.application.handler.query.dto.SlotDto;
import com.clinic.c46.BookingService.application.port.out.SlotRepository;
import com.clinic.c46.BookingService.domain.query.GetAllSlotOfPackageQuery;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SlotQueryHandler {

    private final SlotRepository slotRepository;

    @QueryHandler
    public List<SlotDto> handle(GetAllSlotOfPackageQuery query) {
        List<SlotDto> slotDTOs = new ArrayList<>();

        slotDTOs.add(SlotDto.builder()
                .slotId("8804d7e6-c779-4729-8095-d1102049648e")
                .medicalPackageId(query.medicalPackageId())
                .date(LocalDate.of(2025, 11, 20))
                .shift(1)
                .build());

        slotDTOs.add(SlotDto.builder()
                .slotId("a44120b6-327b-4482-8db1-08e18ac97057")
                .medicalPackageId(query.medicalPackageId())
                .date(LocalDate.of(2025, 11, 20))
                .shift(0)
                .build());

        return slotDTOs;
    }


//        LocalDate date = query.date();
//        Shift shift = query.shift();
//        String medicalPackageId = query.medicalPackageId();
//
//        List<?> result;
//
//        if (date != null && shift != null && medicalPackageId != null) {
//            result = Collections.singletonList(
//                    slotRepository.findAllByDateAndShiftAndMedicalPackageId(date, shift, medicalPackageId));
//        } else if (date != null && shift != null) {
//            result = Collections.singletonList(slotRepository.findAllByDateAndShift(date, shift));
//        } else if (date != null) {
//            result = Collections.singletonList(slotRepository.findAllByDate(date));
//        }
//        else
//        else {
//            result = slotRepository.findAll();
//        }
//
//        return result.stream()
//                .map(entity -> SlotDto.builder()
//                        .slotId(entity.getSlotId())
//                        .date(entity.getDate())
//                        .shift(entity.getShift())
//                        .medicalPackageId(entity.getMedicalPackageId())
//                        .locked(entity.isLocked())
//                        .build())
//                .collect(Collectors.toList());

}
