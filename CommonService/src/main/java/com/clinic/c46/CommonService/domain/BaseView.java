package com.clinic.c46.CommonService.domain;

import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseView {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    protected void markCreated() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    protected void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    protected void markDeleted() {
        this.updatedAt = LocalDateTime.now();
        this.deletedAt = LocalDateTime.now();
    }
}

