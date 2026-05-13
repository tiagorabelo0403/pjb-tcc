package com.tcc.pjb.backend.modules.atendimento.repository;

import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoTosAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtendimentoTosAcceptanceRepository extends JpaRepository<AtendimentoTosAcceptance, Long> {
}
