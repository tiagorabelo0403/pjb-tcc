package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorTelemetry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JudicialConnectorTelemetryRepository extends JpaRepository<JudicialConnectorTelemetry, UUID> {

    List<JudicialConnectorTelemetry> findAllByCreatedAtAfterOrderByCreatedAtDesc(Instant createdAt);

    List<JudicialConnectorTelemetry> findTop200ByConnectorSystemAndCreatedAtAfterOrderByCreatedAtDesc(JudicialSystem connectorSystem, Instant createdAt);
}
