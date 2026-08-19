package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;

public interface DiligenciaOperadorEncerramentoRepository extends JpaRepository<DiligenciaOperadorEncerramento, Long> {

    Optional<DiligenciaOperadorEncerramento> findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndIdempotencyKey(Long operatorUserId,
                                                                                                                     TelemetriaOperacionalCanal canal,
                                                                                                                     String diligenceReference,
                                                                                                                     String idempotencyKey);

    Optional<DiligenciaOperadorEncerramento> findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                       TelemetriaOperacionalCanal canal,
                                                                                                                       String diligenceReference);

    List<DiligenciaOperadorEncerramento> findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                     TelemetriaOperacionalCanal canal,
                                                                                                                     String diligenceReference);

    long countByOperatorUserIdAndCanalAndDiligenceReference(Long operatorUserId,
                                                            TelemetriaOperacionalCanal canal,
                                                            String diligenceReference);

    long countByOperatorUserIdAndCanalAndCreatedAtAfter(Long operatorUserId,
                                                        TelemetriaOperacionalCanal canal,
                                                        java.time.Instant createdAfter);

    List<DiligenciaOperadorEncerramento> findByOperatorUserIdAndCanalAndCreatedAtAfterOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                            TelemetriaOperacionalCanal canal,
                                                                                                            java.time.Instant createdAfter);

    List<DiligenciaOperadorEncerramento> findByOperatorUserIdAndCanalAndWorkItemIdInOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                          TelemetriaOperacionalCanal canal,
                                                                                                          List<Long> workItemIds);
}
