package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorSecuritySession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JudicialConnectorSecuritySessionRepository extends JpaRepository<JudicialConnectorSecuritySession, UUID> {
    List<JudicialConnectorSecuritySession> findTop300ByCreatedAtAfterOrderByCreatedAtDesc(Instant createdAt);
    List<JudicialConnectorSecuritySession> findTop300ByTribunalCodigoIgnoreCaseAndCreatedAtAfterOrderByCreatedAtDesc(String tribunalCodigo, Instant createdAt);
    List<JudicialConnectorSecuritySession> findTop100ByOrderByCreatedAtDesc();
    List<JudicialConnectorSecuritySession> findTop100ByTribunalCodigoIgnoreCaseOrderByCreatedAtDesc(String tribunalCodigo);
}
