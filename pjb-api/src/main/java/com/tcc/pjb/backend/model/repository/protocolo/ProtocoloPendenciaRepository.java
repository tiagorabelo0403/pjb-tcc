package com.tcc.pjb.backend.model.repository.protocolo;

import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloPendencia;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProtocoloPendenciaRepository extends JpaRepository<ProtocoloPendencia, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    @Query("""
            SELECT p FROM ProtocoloPendencia p
            WHERE p.protocoloId = :protocoloId
              AND p.status IN ('PENDENTE_DOCUMENTACAO', 'EM_VALIDACAO')
            """)
    Optional<ProtocoloPendencia> findAtivaByProtocoloId(@Param("protocoloId") Long protocoloId);

    List<ProtocoloPendencia> findByStatus(ProtocoloCompletudeStatus status);
}
