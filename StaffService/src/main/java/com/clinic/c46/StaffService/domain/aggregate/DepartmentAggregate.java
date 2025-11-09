package com.clinic.c46.StaffService.domain.aggregate;

import com.clinic.c46.CommonService.event.staff.DepartmentCreatedEvent;
import com.clinic.c46.StaffService.domain.command.CreateDepartmentCommand;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
@NoArgsConstructor()
public class DepartmentAggregate {

    @AggregateIdentifier
    private String departmentId;
    private String name;
    private String description;

    @CommandHandler
    public DepartmentAggregate(CreateDepartmentCommand cmd) {
        AggregateLifecycle.apply(DepartmentCreatedEvent.builder()
                .departmentId(cmd.departmentId())
                .departmentName(cmd.name())
                .description(cmd.description())
                .build());
    }

    @EventSourcingHandler
    public void on(DepartmentCreatedEvent event) {
        this.departmentId = event.departmentId();
        this.name = event.departmentName();
        this.description = event.description();
    }
}

