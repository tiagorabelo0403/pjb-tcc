package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorMalhaInstitucionalDispatch;

public interface DiligenciaOperadorMalhaInstitucionalDispatchRepository extends JpaRepository<DiligenciaOperadorMalhaInstitucionalDispatch, Long> {

    Optional<DiligenciaOperadorMalhaInstitucionalDispatch> findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndChainIdempotencyKey(Long operatorUserId,
                                                                                                                                        TelemetriaOperacionalCanal canal,
                                                                                                                                        String diligenceReference,
                                                                                                                                        String chainIdempotencyKey);

    Optional<DiligenciaOperadorMalhaInstitucionalDispatch> findByReplayToken(String replayToken);

    Optional<DiligenciaOperadorMalhaInstitucionalDispatch> findByOutboxEventId(UUID outboxEventId);

    Optional<DiligenciaOperadorMalhaInstitucionalDispatch> findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                                    TelemetriaOperacionalCanal canal,
                                                                                                                                    String diligenceReference);

    List<DiligenciaOperadorMalhaInstitucionalDispatch> findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                                   TelemetriaOperacionalCanal canal,
                                                                                                                                   String diligenceReference);

    List<DiligenciaOperadorMalhaInstitucionalDispatch> findTop100ByOperatorUserIdAndCanalAndCreatedAtAfterOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                                TelemetriaOperacionalCanal canal,
                                                                                                                                Instant createdAfter);

    long countByOperatorUserIdAndCanalAndCreatedAtAfter(Long operatorUserId,
                                                        TelemetriaOperacionalCanal canal,
                                                        Instant createdAfter);

    long countByOperatorUserIdAndCanalAndDispatchStatusAndCreatedAtAfter(Long operatorUserId,
                                                                         TelemetriaOperacionalCanal canal,
                                                                         String dispatchStatus,
                                                                         Instant createdAfter);
}
