package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.judicial.MniRemessa;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MniRemessaRepository extends JpaRepository<MniRemessa, Long> {

    Optional<MniRemessa> findByProcessoIdAndTribunalDestinoAndMotivo(Long processoId, String tribunalDestino, String motivo);

    @Query("""
            select r from MniRemessa r
            where r.status in (com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa.PENDING, com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa.FAILED)
              and (r.proximoRetryEm is null or r.proximoRetryEm <= :now)
            order by r.id asc
            """)
    java.util.List<MniRemessa> findRetryCandidates(@Param("now") Instant now);
}
