package com.clinic.c46.StaffService.domain.view;


import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "department")
@Getter
@SuperBuilder
@NoArgsConstructor
public class DepartmentView extends BaseView {
    @Id
    private String id;
    private String name;
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String description;


    public DepartmentView(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.setCreatedAt(LocalDateTime.now());
        this.setUpdatedAt(LocalDateTime.now());
    }

    public void update(String name, String description) {
        if (name != null && !name.isEmpty() && !name.equals(this.name)) {
            this.name = name;
        }

        if (description != null && !description.isEmpty() && !description.equals(this.description)) {
            this.description = description;
        }
        this.setUpdatedAt(LocalDateTime.now());
    }

}
