package com.clinic.c46.AuthService.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Integer accountId;
    
    @Column(name = "account_name", unique = true, nullable = false)
    private String accountName;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "staff_id")
    private String staffId;
    
    @Builder.Default
    @Column(name = "role", nullable = false)
    private String role = "USER";
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
}
