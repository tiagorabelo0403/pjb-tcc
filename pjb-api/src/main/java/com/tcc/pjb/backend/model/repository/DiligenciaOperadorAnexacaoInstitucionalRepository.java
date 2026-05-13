package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorAnexacaoInstitucional;

public interface DiligenciaOperadorAnexacaoInstitucionalRepository extends JpaRepository<DiligenciaOperadorAnexacaoInstitucional, Long> {

    Optional<DiligenciaOperadorAnexacaoInstitucional> findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndChainIdempotencyKey(Long operatorUserId,
                                                                                                                                     TelemetriaOperacionalCanal canal,
                                                                                                                                     String diligenceReference,
                                                                                                                                     String chainIdempotencyKey);


    Optional<DiligenciaOperadorAnexacaoInstitucional> findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                               TelemetriaOperacionalCanal canal,
                                                                                                                               String diligenceReference);

    List<DiligenciaOperadorAnexacaoInstitucional> findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                               TelemetriaOperacionalCanal canal,
                                                                                                                               String diligenceReference);

    List<DiligenciaOperadorAnexacaoInstitucional> findTop100ByOperatorUserIdAndCanalAndCreatedAtAfterOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                            TelemetriaOperacionalCanal canal,
                                                                                                                            Instant createdAfter);

    long countByOperatorUserIdAndCanalAndDiligenceReference(Long operatorUserId,
                                                            TelemetriaOperacionalCanal canal,
                                                            String diligenceReference);

    long countByOperatorUserIdAndCanal(Long operatorUserId,
                                       TelemetriaOperacionalCanal canal);

    long countByOperatorUserIdAndCanalAndCreatedAtAfter(Long operatorUserId,
                                                        TelemetriaOperacionalCanal canal,
                                                        Instant createdAfter);

    @Query("select count(distinct a.processoId) from DiligenciaOperadorAnexacaoInstitucional a where a.operatorUserId = :operatorUserId and a.canal = :canal and a.createdAt >= :createdAfter")
    long countDistinctProcessoByOperatorAndCanalSince(@Param("operatorUserId") Long operatorUserId,
                                                      @Param("canal") TelemetriaOperacionalCanal canal,
                                                      @Param("createdAfter") Instant createdAfter);
}
