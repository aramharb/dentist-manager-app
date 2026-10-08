package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.ClientAccount;

public interface ClientAccountRepository extends JpaRepository<ClientAccount, Long> {
    Optional<ClientAccount> findByPhone(String phone);
    boolean existsByPhone(String phone);
}
