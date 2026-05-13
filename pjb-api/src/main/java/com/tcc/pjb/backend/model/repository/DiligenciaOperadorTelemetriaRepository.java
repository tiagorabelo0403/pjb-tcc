package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorTelemetria;

@Repository
public interface DiligenciaOperadorTelemetriaRepository extends JpaRepository<DiligenciaOperadorTelemetria, Long> {

    Optional<DiligenciaOperadorTelemetria> findTopByOperatorUserIdAndCanalOrderByCapturadoEmDesc(Long operatorUserId,
                                                                                                  TelemetriaOperacionalCanal canal);

    List<DiligenciaOperadorTelemetria> findByOperatorUserIdAndCanalOrderByCapturadoEmDesc(Long operatorUserId,
                                                                                           TelemetriaOperacionalCanal canal,
                                                                                           Pageable pageable);

    Optional<DiligenciaOperadorTelemetria> findTopByOperatorUserIdAndCanalAndCapturadoEmAfterOrderByCapturadoEmDesc(Long operatorUserId,
                                                                                                                     TelemetriaOperacionalCanal canal,
                                                                                                                     Instant capturedAfter);

    long countByOperatorUserIdAndCanal(Long operatorUserId,
                                       TelemetriaOperacionalCanal canal);

    long countByOperatorUserIdAndCanalAndCapturadoEmAfter(Long operatorUserId,
                                                          TelemetriaOperacionalCanal canal,
                                                          Instant capturedAfter);
}
