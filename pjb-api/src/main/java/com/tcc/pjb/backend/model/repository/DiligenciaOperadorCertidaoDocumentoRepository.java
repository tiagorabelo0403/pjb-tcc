package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidaoDocumento;

public interface DiligenciaOperadorCertidaoDocumentoRepository extends JpaRepository<DiligenciaOperadorCertidaoDocumento, Long> {

    boolean existsByCertidaoIdAndDocumentoId(Long certidaoId, UUID documentoId);

    List<DiligenciaOperadorCertidaoDocumento> findByCertidaoIdOrderByCreatedAtDesc(Long certidaoId);

    long countByCertidaoId(Long certidaoId);
}
