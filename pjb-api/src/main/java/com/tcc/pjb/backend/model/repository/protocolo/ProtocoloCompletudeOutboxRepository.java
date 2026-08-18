package com.tcc.pjb.backend.model.repository.protocolo;

import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloCompletudeOutboxEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProtocoloCompletudeOutboxRepository extends JpaRepository<ProtocoloCompletudeOutboxEntity, UUID> {

    @Query("SELECT o FROM ProtocoloCompletudeOutboxEntity o WHERE o.processado = false ORDER BY o.criadoEm LIMIT 50")
    List<ProtocoloCompletudeOutboxEntity> findPendentes();
}
