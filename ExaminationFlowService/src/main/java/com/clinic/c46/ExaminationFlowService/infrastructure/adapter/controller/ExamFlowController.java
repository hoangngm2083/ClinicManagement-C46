package com.clinic.c46.ExaminationFlowService.infrastructure.adapter.controller;

import com.clinic.c46.CommonService.query.staff.GetIdOfAllDepartmentQuery;
import com.clinic.c46.ExaminationFlowService.application.command.InitialRedisQueueCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/exam-flow")
public class ExamFlowController {

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;

    @Autowired
    public ExamFlowController(QueryGateway queryGateway, CommandGateway commandGateway) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
    }

    @PostMapping("/queue/init")
    public CompletableFuture<ResponseEntity<String>> initialRedisQueues() {
        CompletableFuture<List<String>> departmentIdsFuture = queryGateway.query(new GetIdOfAllDepartmentQuery(),
                ResponseTypes.multipleInstancesOf(String.class));

        return departmentIdsFuture.thenCompose(departmentIds -> {
            if (departmentIds == null || departmentIds.isEmpty()) {
                return CompletableFuture.completedFuture(
                        ResponseEntity.ok("Không tìm thấy Department ID nào để khởi tạo Queue."));
            }

            InitialRedisQueueCommand command = new InitialRedisQueueCommand(departmentIds);

            return commandGateway.send(command)
                    .thenApply(result -> ResponseEntity.ok(
                            "Đã gửi lệnh khởi tạo Redis Queues thành công cho " + departmentIds.size() + " departments."))
                    .exceptionally(ex -> ResponseEntity.internalServerError()
                            .body("Lỗi khi gửi lệnh khởi tạo: " + ex.getMessage()));
        });
    }
}