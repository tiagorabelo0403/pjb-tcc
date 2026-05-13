package com.tcc.pjb.backend.modules.atendimento.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadPolicy;

public interface AtendimentoThreadPolicyRepository extends JpaRepository<AtendimentoThreadPolicy, Long> {
}
