package com.clinic.c46.MedicalPackageService.domain.view;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table( name = "department")
public class DepartmentView {
    @Id
    private String id;
    private String name;
}
