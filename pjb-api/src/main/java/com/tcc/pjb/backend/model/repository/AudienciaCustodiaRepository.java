package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.criminal.AudienciaCustodia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudienciaCustodiaRepository extends JpaRepository<AudienciaCustodia, Long> {
    List<AudienciaCustodia> findByStatusOrderByPrazoLimite24hAsc(String status);
}
