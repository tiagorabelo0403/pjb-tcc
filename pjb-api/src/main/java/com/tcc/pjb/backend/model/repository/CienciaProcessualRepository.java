package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusCiencia;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicaoCiencia;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CienciaProcessualRepository extends JpaRepository<CienciaProcessual, Long> {

    List<CienciaProcessual> findByProcessoIdAndStatus(Long processoId, StatusCiencia status);

    List<CienciaProcessual> findByUsuarioIdAndStatus(Long usuarioId, StatusCiencia status);

    Optional<CienciaProcessual> findByAtoProcessualIdAndUsuarioId(Long atoId, Long usuarioId);

    @Query("""
            SELECT c FROM CienciaProcessual c
            WHERE c.status = 'PENDENTE'
              AND c.dataExpiracao < :agora
            ORDER BY c.dataExpiracao ASC
            """)
    List<CienciaProcessual> findPendentesExpirados(
            @Param("agora") Instant agora, Pageable pageable);

    @Query("""
            SELECT c FROM CienciaProcessual c
            WHERE c.processo.id = :processoId
              AND c.grauJurisdicao = :grau
            ORDER BY c.dataDisponibilizacao DESC
            """)
    List<CienciaProcessual> findByProcessoIdAndGrau(
            @Param("processoId") Long processoId,
            @Param("grau") GrauJurisdicaoCiencia grau);

    @Query("""
            SELECT c FROM CienciaProcessual c
            WHERE c.processo.id = :processoId
              AND c.poloProcessual = :polo
              AND c.status = 'PENDENTE'
            """)
    List<CienciaProcessual> findPendentesByProcessoAndPolo(
            @Param("processoId") Long processoId,
            @Param("polo") String polo);

    long countByProcessoIdAndStatus(Long processoId, StatusCiencia status);

    @Query("""
            SELECT c FROM CienciaProcessual c
            WHERE c.processo.vara = :vara
              AND c.status = 'PENDENTE'
              AND c.dataExpiracao <= :ateData
            ORDER BY c.dataExpiracao ASC
            """)
    List<CienciaProcessual> findPendentesPorVaraAteData(
            @Param("vara") String vara,
            @Param("ateData") Instant ateData,
            Pageable pageable);
}
