package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.competencia.ProcessoDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.competencia.StatusDistribuicaoCompetencia;

@Repository
public interface ProcessoDistribuicaoCompetenciaRepository extends JpaRepository<ProcessoDistribuicaoCompetencia, Long> {

    long countByUnidade_IdAndCriadoEmAfter(Long unidadeId, Instant criadoEm);

    Optional<ProcessoDistribuicaoCompetencia> findTopByProcesso_IdOrderByIdDesc(Long processoId);

    Optional<ProcessoDistribuicaoCompetencia> findTopByRequestHashOrderByIdDesc(String requestHash);

    long countByStatus(StatusDistribuicaoCompetencia status);

    @Query("""
            select d.unidade.id, count(d)
            from ProcessoDistribuicaoCompetencia d
            where d.criadoEm >= :criadoEm
              and d.unidade.id in :unidadeIds
            group by d.unidade.id
            """)
    List<Object[]> contarRecentesPorUnidade(@Param("unidadeIds") Collection<Long> unidadeIds,
                                            @Param("criadoEm") Instant criadoEm);
}
