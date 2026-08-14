package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.competencia.Tribunal;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;

@Repository
public interface UnidadeJudiciariaCompetenciaRepository extends JpaRepository<UnidadeJudiciariaCompetencia, Long> {

    Optional<UnidadeJudiciariaCompetencia> findByCodigo(String codigo);

    @Override
    @EntityGraph(attributePaths = {"tribunal", "comarca", "especialidades", "classesTpu", "assuntosTpu"})
    List<UnidadeJudiciariaCompetencia> findAll();

    @Override
    @EntityGraph(attributePaths = {"tribunal", "comarca", "especialidades", "classesTpu", "assuntosTpu"})
    Optional<UnidadeJudiciariaCompetencia> findById(Long id);

    java.util.List<UnidadeJudiciariaCompetencia> findAllByTribunal(Tribunal tribunal);

    java.util.List<UnidadeJudiciariaCompetencia> findAllByTribunalAndComarca(Tribunal tribunal, Comarca comarca);

    @Modifying(flushAutomatically = false, clearAutomatically = false)
    @Query(value = """
            update tb_unidade_judiciaria_competencia
               set processos_ativos = processos_ativos + 1,
                   distribuicoes_ultimas_24h = distribuicoes_ultimas_24h + 1,
                   ultima_distribuicao_em = :agora,
                   atualizado_em = :agora,
                   versao = coalesce(versao, 0) + 1
             where id = :id
            """, nativeQuery = true)
    int registrarDistribuicaoAplicada(@Param("id") Long id, @Param("agora") Instant agora);
}
