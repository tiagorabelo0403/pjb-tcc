package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorCryptographicFailureEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JudicialConnectorCryptographicFailureEventRepository extends JpaRepository<JudicialConnectorCryptographicFailureEvent, UUID> {
    List<JudicialConnectorCryptographicFailureEvent> findTop100ByConnectorSystemOrderByCreatedAtDesc(JudicialSystem connectorSystem);
    List<JudicialConnectorCryptographicFailureEvent> findTop200ByCreatedAtAfterOrderByCreatedAtDesc(Instant createdAt);
}
