package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.financeiro.DepositoRecursal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositoRecursalRepository extends JpaRepository<DepositoRecursal, Long> {
    List<DepositoRecursal> findByProcessoIdOrderByCreatedAtDesc(Long processoId);
}
