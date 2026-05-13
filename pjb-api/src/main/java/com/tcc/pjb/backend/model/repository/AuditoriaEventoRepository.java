package com.tcc.pjb.backend.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.AuditoriaEvento;

@Repository
public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {
}
