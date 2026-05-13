package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.federalismo.FederacaoEventoOutbox;
import com.tcc.pjb.backend.model.entity.federalismo.StatusEventoOutboxFederacao;

public interface FederacaoEventoOutboxRepository extends JpaRepository<FederacaoEventoOutbox, UUID> {

    Optional<FederacaoEventoOutbox> findByIdempotencyKey(String idempotencyKey);

    List<FederacaoEventoOutbox> findTop200ByStatusAndProximaTentativaEmLessThanEqualOrderByPrioridadeDescCriadoEmAsc(
            StatusEventoOutboxFederacao status,
            Instant proximaTentativaEm
    );

    long countByTribunalCodigoAndStatus(String tribunalCodigo, StatusEventoOutboxFederacao status);
}
