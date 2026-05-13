package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorFormalizacaoProcessual;

public interface DiligenciaOperadorFormalizacaoProcessualRepository extends JpaRepository<DiligenciaOperadorFormalizacaoProcessual, Long> {

    Optional<DiligenciaOperadorFormalizacaoProcessual> findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndIdempotencyKey(Long operatorUserId,
                                                                                                                               TelemetriaOperacionalCanal canal,
                                                                                                                               String diligenceReference,
                                                                                                                               String idempotencyKey);

    Optional<DiligenciaOperadorFormalizacaoProcessual> findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                             TelemetriaOperacionalCanal canal,
                                                                                                                             String diligenceReference);

    List<DiligenciaOperadorFormalizacaoProcessual> findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                               TelemetriaOperacionalCanal canal,
                                                                                                                               String diligenceReference);

    long countByOperatorUserIdAndCanalAndDiligenceReference(Long operatorUserId,
                                                            TelemetriaOperacionalCanal canal,
                                                            String diligenceReference);

    long countByOperatorUserIdAndCanalAndCreatedAtAfter(Long operatorUserId,
                                                        TelemetriaOperacionalCanal canal,
                                                        java.time.Instant createdAfter);
}
