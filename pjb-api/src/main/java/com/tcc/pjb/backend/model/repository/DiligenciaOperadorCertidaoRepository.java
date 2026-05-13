package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;

@Repository
public interface DiligenciaOperadorCertidaoRepository extends JpaRepository<DiligenciaOperadorCertidao, Long> {

    Optional<DiligenciaOperadorCertidao> findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                   TelemetriaOperacionalCanal canal,
                                                                                                                   String diligenceReference);

    List<DiligenciaOperadorCertidao> findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                   TelemetriaOperacionalCanal canal,
                                                                                                                   String diligenceReference);

    List<DiligenciaOperadorCertidao> findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(Long operatorUserId,
                                                                                                                   TelemetriaOperacionalCanal canal,
                                                                                                                   String diligenceReference);

    long countByOperatorUserIdAndCanalAndDiligenceReference(Long operatorUserId,
                                                            TelemetriaOperacionalCanal canal,
                                                            String diligenceReference);

    long countByOperatorUserIdAndCanalAndCreatedAtAfter(Long operatorUserId,
                                                        TelemetriaOperacionalCanal canal,
                                                        java.time.Instant createdAfter);
}
