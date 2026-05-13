package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;

@Repository
public interface DiligenciaOperadorCheckpointEventoRepository extends JpaRepository<DiligenciaOperadorCheckpointEvento, Long> {

    List<DiligenciaOperadorCheckpointEvento> findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(Long operatorUserId,
                                                                                                                           TelemetriaOperacionalCanal canal,
                                                                                                                           String diligenceReference);

    Optional<DiligenciaOperadorCheckpointEvento> findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(Long operatorUserId,
                                                                                                                             TelemetriaOperacionalCanal canal,
                                                                                                                             String diligenceReference);

    long countByOperatorUserIdAndCanalAndDiligenceReference(Long operatorUserId,
                                                            TelemetriaOperacionalCanal canal,
                                                            String diligenceReference);

    long countByOperatorUserIdAndCanalAndCreatedAtAfter(Long operatorUserId,
                                                        TelemetriaOperacionalCanal canal,
                                                        java.time.Instant createdAfter);

    List<DiligenciaOperadorCheckpointEvento> findByOperatorUserIdAndCanalAndWorkItemIdInOrderByOccurredAtDesc(Long operatorUserId,
                                                                                                               TelemetriaOperacionalCanal canal,
                                                                                                               List<Long> workItemIds);
}
