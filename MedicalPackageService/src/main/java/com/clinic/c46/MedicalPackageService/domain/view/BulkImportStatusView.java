package com.clinic.c46.MedicalPackageService.domain.view;

import com.clinic.c46.CommonService.domain.BaseView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "bulk_import_status")
public class BulkImportStatusView extends BaseView {

    @Id
    private String bulkId;

    @Column(nullable = false, length = 50)
    private String entityType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String importFileUrl;

    @Column(nullable = false, length = 20)
    private String status;

    private Integer totalRows;
    private Integer successfulRows;
    private Integer failedRows;

    @Column(columnDefinition = "TEXT")
    private String resultCsvUrl;
}
