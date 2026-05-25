package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;

@Repository
public interface UnidadeJudiciariaCompetenciaRepository extends JpaRepository<UnidadeJudiciariaCompetencia, Long> {

    Optional<UnidadeJudiciariaCompetencia> findByCodigo(String codigo);

    java.util.List<UnidadeJudiciariaCompetencia> findAllByTribunalCodigo(String tribunalCodigo);

    java.util.List<UnidadeJudiciariaCompetencia> findAllByTribunalCodigoAndComarcaIgnoreCase(String tribunalCodigo, String comarca);

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
