package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.eleitoral.ProcessoZonaEleitoral;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessoZonaEleitoralRepository extends JpaRepository<ProcessoZonaEleitoral, Long> {
}
