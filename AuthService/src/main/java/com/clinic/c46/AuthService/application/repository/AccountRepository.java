package com.clinic.c46.AuthService.application.repository;

import com.clinic.c46.AuthService.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    
    Optional<Account> findByAccountNameAndIsDeletedFalse(String accountName);
    
    boolean existsByAccountName(String accountName);
}
