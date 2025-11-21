package com.clinic.c46.StaffService.application.service;


import com.clinic.c46.CommonService.query.examinationFlow.GetQueueSizeQuery;
import com.clinic.c46.StaffService.domain.command.CreateDepartmentCommand;
import com.clinic.c46.StaffService.domain.command.DeleteDepartmentCommand;
import com.clinic.c46.StaffService.domain.query.GetAllStaffIdOfDepQuery;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    @Override
    public void create(CreateDepartmentCommand cmd) {
        commandGateway.sendAndWait(cmd);
    }

    @Override
    public void delete(DeleteDepartmentCommand cmd) {
        List<String> staffIds = queryGateway.query(new GetAllStaffIdOfDepQuery(cmd.departmentId()),
                        ResponseTypes.multipleInstancesOf(String.class))
                .join();

        if (!staffIds.isEmpty()) {
            throw new RuntimeException("Không thể xóa vì phòng này vẫn còn nhân viên liên kết");
        }

        Optional<Long> size = queryGateway.query(new GetQueueSizeQuery(cmd.departmentId()),
                        ResponseTypes.optionalInstanceOf(Long.class))
                .join();

        if (size.isEmpty() || size.get() > 0) {
            throw new RuntimeException("Không thể xóa vì hàng đợi của phòng này vẫn còn hồ sơ!");
        }

        commandGateway.sendAndWait(cmd);
    }
}
