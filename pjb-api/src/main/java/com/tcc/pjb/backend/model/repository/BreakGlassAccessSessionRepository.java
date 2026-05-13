package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.security.BreakGlassAccessSession;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BreakGlassAccessSessionRepository extends JpaRepository<BreakGlassAccessSession, UUID> {

    List<BreakGlassAccessSession> findTop50ByProcessoIdOrderByCreatedAtDesc(Long processoId);
}
