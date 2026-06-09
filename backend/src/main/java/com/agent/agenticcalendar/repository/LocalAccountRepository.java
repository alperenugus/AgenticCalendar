package com.agent.agenticcalendar.repository;

import com.agent.agenticcalendar.model.LocalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalAccountRepository extends JpaRepository<LocalAccount, Long> {

    Optional<LocalAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
