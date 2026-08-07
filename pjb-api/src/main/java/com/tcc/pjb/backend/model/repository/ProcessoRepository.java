package com.tcc.pjb.backend.model.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Repository
public interface ProcessoRepository extends JpaRepository<Processo, Long>, JpaSpecificationExecutor<Processo> {

    
    Optional<Processo> findByNumeroProcesso(String numeroProcesso);

    
    Optional<Processo> findByNumeroUnificado(String numeroUnificado);

    boolean existsByNumeroProcesso(String numeroProcesso);

    boolean existsByNumeroUnificado(String numeroUnificado);

    @EntityGraph(attributePaths = {"jurisdicao", "usuario"})
    @Query("""
            SELECT p FROM Processo p
            WHERE p.numeroUnificado IN :numeros
               OR p.numeroProcesso IN :numerosProcesso
            """)
    List<Processo> findByNumeroUnificadoInOrNumeroProcessoIn(@Param("numeros") Collection<String> numeros,
                                                             @Param("numerosProcesso") Collection<String> numerosProcesso);

    default List<Processo> findAllByAnyNumeroIn(Collection<String> numeros) {
        return findByNumeroUnificadoInOrNumeroProcessoIn(numeros, numeros);
    }

    default List<Processo> findAllByNumeroUnificadoInOrNumeroProcessoIn(Collection<String> numeros,
                                                                        Collection<String> numerosProcesso) {
        return findByNumeroUnificadoInOrNumeroProcessoIn(numeros, numerosProcesso);
    }

    
    @EntityGraph(attributePaths = {"usuario", "jurisdicao"})
    @Query("SELECT p FROM Processo p WHERE p.id = :id")
    Optional<Processo> findProcessoCompletoById(@Param("id") Long id);


    @EntityGraph(attributePaths = {"usuario", "jurisdicao"})
    @Query("SELECT p FROM Processo p WHERE p.id = :id")
    Optional<Processo> findContextoArquiteturalById(@Param("id") Long id);


    @EntityGraph(attributePaths = {"jurisdicao", "usuario"})
    @Query("""
            SELECT p FROM Processo p
            LEFT JOIN p.jurisdicao j
            WHERE (:excludeId IS NULL OR p.id <> :excludeId)
              AND (:tribunal IS NULL OR :tribunal = ''
                    OR UPPER(p.tribunal) = UPPER(:tribunal)
                    OR UPPER(j.codigo) = UPPER(:tribunal)
                    OR UPPER(j.sigla) = UPPER(:tribunal))
              AND (:classe IS NULL OR :classe = '' OR UPPER(p.classeProcessual) = UPPER(:classe))
              AND (:assunto IS NULL OR :assunto = ''
                    OR LOWER(COALESCE(p.assunto, '')) LIKE LOWER(CONCAT('%', :assunto, '%'))
                    OR LOWER(:assunto) LIKE LOWER(CONCAT('%', COALESCE(p.assunto, ''), '%')))
            ORDER BY COALESCE(p.dataUltimaMovimentacao, p.dataAtualizacao, p.dataCriacao) DESC, p.id DESC
            """)
    List<Processo> findComparableCases(@Param("excludeId") Long excludeId,
                                       @Param("tribunal") String tribunal,
                                       @Param("classe") String classe,
                                       @Param("assunto") String assunto,
                                       Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "jurisdicao"})
    @Query("""
            SELECT p FROM Processo p
            WHERE p.ramoDireito IS NOT NULL
              AND p.statusProcesso NOT IN :statusIgnorados
            ORDER BY p.id ASC
            """)
    Slice<Processo> findAllForPrazoScan(@Param("statusIgnorados") Collection<StatusProcesso> statusIgnorados,
                                        Pageable pageable);


    @EntityGraph(attributePaths = {"usuario", "jurisdicao"})
    @Query("""
            SELECT p FROM Processo p
            WHERE p.ramoDireito IS NOT NULL
              AND p.statusProcesso NOT IN :statusIgnorados
              AND p.id > :lastProcessoId
              AND UPPER(p.tribunal) = UPPER(:tribunalCodigo)
            ORDER BY p.id ASC
            """)
    Slice<Processo> findDataJudFeedBatch(@Param("lastProcessoId") Long lastProcessoId,
                                         @Param("tribunalCodigo") String tribunalCodigo,
                                         @Param("statusIgnorados") Collection<StatusProcesso> statusIgnorados,
                                         Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "jurisdicao"})
    @Query("""
            SELECT p FROM Processo p
            WHERE p.statusProcesso NOT IN :statusIgnorados
              AND p.usuario IS NOT NULL
              AND p.usuario.cpf IS NOT NULL
            ORDER BY p.id ASC
            """)
    Slice<Processo> findAllForVitalMonitor(@Param("statusIgnorados") Collection<StatusProcesso> statusIgnorados,
                                           Pageable pageable);

    
    boolean existsByJurisdicao_Id(Long jurisdicaoId);

    
    
    @Query("SELECT COUNT(p) FROM Processo p WHERE p.jurisdicao.id = :jurisdicaoId AND p.statusProcesso <> 'ARQUIVADO'")
    long countByJurisdicaoIdAndStatusAtivo(@Param("jurisdicaoId") Long jurisdicaoId);

    long countByStatusProcesso(StatusProcesso status);

    @Query("""
            SELECT COUNT(p) FROM Processo p
            WHERE p.statusProcesso = :recursalStatus
               OR p.faseAtual IN :recursalPhases
            """)
    long countRecursais(@Param("recursalStatus") StatusProcesso recursalStatus,
                        @Param("recursalPhases") Collection<FaseProcessual> recursalPhases);

    @Query(value = """
            SELECT COALESCE(p.ramo_direito, 'NAO_CLASSIFICADO') AS ramo,
                   COUNT(*) AS total
            FROM tb_processo p
            GROUP BY COALESCE(p.ramo_direito, 'NAO_CLASSIFICADO')
            ORDER BY ramo ASC
            """, nativeQuery = true)
    List<Object[]> countPorRamo();

    @Query(value = """
            SELECT COALESCE(p.status_processo, 'NAO_CLASSIFICADO') AS status,
                   COUNT(*) AS total
            FROM tb_processo p
            GROUP BY COALESCE(p.status_processo, 'NAO_CLASSIFICADO')
            ORDER BY status ASC
            """, nativeQuery = true)
    List<Object[]> countPorStatus();

    

    
    @Query("SELECT DISTINCT u.cpf FROM Processo p JOIN p.usuario u WHERE p.statusProcesso <> 'ARQUIVADO' AND u.cpf IS NOT NULL")
    List<String> findDistinctCpfByStatusNotArquivado();

    
    @EntityGraph(attributePaths = {"usuario", "jurisdicao"})
    @Query("SELECT p FROM Processo p LEFT JOIN p.usuario u WHERE (p.parteAutoraCpf = :cpf OR p.parteReuCpf = :cpf OR u.cpf = :cpf)")
    List<Processo> findAllByPartesCpf(@Param("cpf") String cpf);

    
    @EntityGraph(attributePaths = {"jurisdicao"})
    @Query("""
            SELECT p FROM Processo p
            WHERE (:cpf IS NULL OR :cpf = '' OR p.parteAutoraCpf = :cpf OR p.parteReuCpf = :cpf)
              AND (:nome IS NULL OR :nome = '' OR LOWER(p.parteAutoraNome) LIKE LOWER(CONCAT('%', :nome, '%'))
                   OR LOWER(p.parteReuNome) LIKE LOWER(CONCAT('%', :nome, '%')))
              AND (:numero IS NULL OR :numero = '' OR p.numeroUnificado = :numero OR p.numeroProcesso = :numero)
            ORDER BY p.dataUltimaMovimentacao DESC NULLS LAST, p.id DESC
            """)
    Page<Processo> searchQuick(@Param("cpf") String cpf,
                              @Param("nome") String nome,
                              @Param("numero") String numero,
                              Pageable pageable);

    


    @EntityGraph(attributePaths = {"jurisdicao"})
    @Query("""
            SELECT p FROM Processo p
              JOIN p.jurisdicao j
              LEFT JOIN p.usuario u
            WHERE (:cpf IS NULL OR :cpf = '' OR p.parteAutoraCpf = :cpf OR p.parteReuCpf = :cpf OR u.cpf = :cpf)
              AND (:numero IS NULL OR :numero = '' OR p.numeroUnificado = :numero OR p.numeroProcesso = :numero)
              AND (:uf IS NULL OR :uf = '' OR j.estado = :uf)
              AND (:status IS NULL OR p.statusProcesso = :status)
            ORDER BY p.dataUltimaMovimentacao DESC NULLS LAST, p.id DESC
            """)
    Page<Processo> searchCidadao(@Param("cpf") String cpf,
                                @Param("numero") String numero,
                                @Param("uf") String uf,
                                @Param("status") StatusProcesso status,
                                Pageable pageable);

    
    @EntityGraph(attributePaths = {"jurisdicao"})
    @Query("SELECT p FROM Processo p JOIN p.jurisdicao j " +
            "WHERE (:uf IS NULL OR j.estado = :uf) " +
            "AND (:comarca IS NULL OR j.comarca = :comarca) " +
            "AND p.statusProcesso <> 'ARQUIVADO'")
    Page<Processo> findForMagistradoDashboard(@Param("uf") String uf,
                                             @Param("comarca") String comarca,
                                             Pageable pageable);


    @Query(value = """
            SELECT
                p.ramo_direito AS ramo,
                COUNT(*) AS total,
                SUM(CASE WHEN p.status_processo IN ('JULGADO','ARQUIVADO','TRANSITO_EM_JULGADO','SENTENCA_PROFERIDA')
                         THEN 1 ELSE 0 END) AS julgados,
                SUM(CASE WHEN p.fase_atual = 'RECURSAL'
                          OR p.status_processo = 'RECURSO_INTERPOSTO'
                         THEN 1 ELSE 0 END) AS recursais,
                SUM(CASE WHEN p.status_processo NOT IN ('JULGADO','ARQUIVADO','TRANSITO_EM_JULGADO')
                         THEN 1 ELSE 0 END) AS ativos,
                AVG(EXTRACT(EPOCH FROM (
                        COALESCE(p.data_ultima_movimentacao, NOW())
                        - COALESCE(p.data_criacao, p.data_distribuicao)
                )) / 86400.0) AS tempo_medio_dias
            FROM tb_processo p
            WHERE p.ramo_direito = :ramo
            GROUP BY p.ramo_direito
            """, nativeQuery = true)
    List<Object[]> agregadosPorRamo(@Param("ramo") String ramo);

    @Query(value = """
            SELECT
                COUNT(*) AS total_tribunal,
                SUM(CASE WHEN p.status_processo NOT IN ('JULGADO','ARQUIVADO','TRANSITO_EM_JULGADO')
                         THEN 1 ELSE 0 END) AS ativos_tribunal,
                SUM(CASE WHEN (UPPER(COALESCE(p.resultado_final, '')) LIKE '%ACORDO%'
                               OR UPPER(COALESCE(p.resultado_final, '')) LIKE '%CONCILIACAO%'
                               OR UPPER(COALESCE(p.resultado_final, '')) LIKE '%MEDIACAO%'
                               OR (UPPER(COALESCE(p.resultado_final, '')) LIKE '%HOMOLOG%'
                                   AND UPPER(COALESCE(p.resultado_final, '')) LIKE '%ACORDO%'))
                         THEN 1 ELSE 0 END) AS com_acordo,
                SUM(CASE WHEN (UPPER(COALESCE(p.classe_processual, '')) LIKE '%TUTELA%'
                               OR UPPER(COALESCE(p.assunto, '')) LIKE '%TUTELA%'
                               OR UPPER(COALESCE(p.assunto, '')) LIKE '%LIMINAR%'
                               OR UPPER(COALESCE(p.assunto, '')) LIKE '%URG%'
                               OR UPPER(COALESCE(p.assunto, '')) LIKE '%CAUTELAR%')
                         THEN 1 ELSE 0 END) AS com_tutela
            FROM tb_processo p
            JOIN jurisdicoes j ON j.id = p.jurisdicao_id
            WHERE p.ramo_direito = :ramo
              AND (:tribunal IS NOT NULL AND :tribunal <> '')
              AND (UPPER(j.codigo) = UPPER(:tribunal) OR UPPER(j.sigla) = UPPER(:tribunal))
            """, nativeQuery = true)
    List<Object[]> agregadosPorRamoETribunal(@Param("ramo") String ramo,
                                             @Param("tribunal") String tribunal);



    @EntityGraph(attributePaths = {"usuario", "jurisdicao"})
    @Query("""
            SELECT p FROM Processo p
            WHERE p.connectorSystem IS NOT NULL
              AND (p.connectorProtocolReference IS NULL OR p.connectorProtocolReference = '')
              AND (p.connectorSubmissionStatus IS NULL OR p.connectorSubmissionStatus IN :statuses)
              AND (p.connectorSubmissionAttempts IS NULL OR p.connectorSubmissionAttempts < :maxAttempts)
            ORDER BY p.id ASC
            """)
    Page<Processo> findConnectorReplayCandidates(@Param("statuses") List<String> statuses,
                                                 @Param("maxAttempts") Integer maxAttempts,
                                                 Pageable pageable);


    @EntityGraph(attributePaths = {"usuario", "jurisdicao"})
    @Query("""
            SELECT p FROM Processo p
            WHERE p.connectorSystem IS NOT NULL
              AND p.connectorProtocolReference IS NOT NULL
              AND p.connectorProtocolReference <> ''
              AND (p.connectorSyncStatus IS NULL OR p.connectorSyncStatus IN :statuses)
              AND (p.connectorSyncAttempts IS NULL OR p.connectorSyncAttempts < :maxAttempts)
            ORDER BY p.id ASC
            """)
    Page<Processo> findConnectorSyncReplayCandidates(@Param("statuses") List<String> statuses,
                                                     @Param("maxAttempts") Integer maxAttempts,
                                                     Pageable pageable);

    @Query("SELECT p.id, p.numeroUnificado FROM Processo p WHERE p.id IN :ids")
    List<Object[]> findNumeroUnificadoByIds(@Param("ids") List<Long> ids);


@EntityGraph(attributePaths = {"usuario", "jurisdicao", "equipe"})
@Query("""
        SELECT p FROM Processo p
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(COALESCE(p.numeroUnificado, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.numeroProcesso, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.numero, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.assunto, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.classeProcessual, '')) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR p.statusProcesso = :status)
          AND (:ramoDireito IS NULL OR p.ramoDireito = :ramoDireito)
        ORDER BY COALESCE(p.dataUltimaMovimentacao, p.dataAtualizacao, p.dataCriacao) DESC, p.id DESC
        """)
Page<Processo> searchWorkspaceVisible(@Param("search") String search,
                                      @Param("status") StatusProcesso status,
                                      @Param("ramoDireito") com.tcc.pjb.backend.model.entity.enums.RamoDireito ramoDireito,
                                      Pageable pageable);

@EntityGraph(attributePaths = {"usuario", "jurisdicao", "equipe"})
@Query("SELECT p FROM Processo p WHERE p.id = :id")
Optional<Processo> findWorkspaceScopedById(@Param("id") Long id);


@EntityGraph(attributePaths = {"usuario", "jurisdicao", "equipe"})
@Query("SELECT p FROM Processo p WHERE p.id = :id")
Optional<Processo> findMagistraturaActsScopedById(@Param("id") Long id);



    default Optional<Processo> findByNumero(String numero) {
        return findByNumeroProcesso(numero).or(() -> findByNumeroUnificado(numero));
    }


    default Optional<Processo> findContextoCompletoById(Long id) {
        return findContextoArquiteturalById(id)
                .or(() -> findProcessoCompletoById(id))
                .or(() -> findById(id));
    }

    interface JurisdicaoLoadProjection {
        Long getJurisdicaoId();
        Long getTotalAtivos();
        Long getAtrasoEstrutural();
    }

    @Query(value = """
            SELECT
                p.jurisdicao_id AS jurisdicaoId,
                SUM(CASE WHEN p.status_processo IS NOT NULL
                              AND p.status_processo <> 'ARQUIVADO'
                         THEN 1 ELSE 0 END) AS totalAtivos,
                SUM(CASE WHEN p.data_ultima_movimentacao IS NOT NULL
                              AND p.data_ultima_movimentacao < :staleThreshold
                         THEN 1 ELSE 0 END) AS atrasoEstrutural
            FROM tb_processo p
            WHERE p.jurisdicao_id IS NOT NULL
            GROUP BY p.jurisdicao_id
            """, nativeQuery = true)
    List<JurisdicaoLoadProjection> computeLoadByJurisdicao(@Param("staleThreshold") LocalDateTime staleThreshold);

    @Query("""
            SELECT p.id FROM Processo p
            LEFT JOIN p.usuario u
            WHERE u.cpf = :cpf
            ORDER BY COALESCE(p.dataUltimaMovimentacao, p.dataAtualizacao, p.dataCriacao) DESC, p.id DESC
            """)
    List<Long> findIdsByAdvogadoCpf(@Param("cpf") String cpf, Pageable pageable);

    @Query("""
            SELECT p.id FROM Processo p
            LEFT JOIN p.usuario u
            WHERE p.parteAutoraCpf = :cpf OR p.parteReuCpf = :cpf OR u.cpf = :cpf
            ORDER BY COALESCE(p.dataUltimaMovimentacao, p.dataAtualizacao, p.dataCriacao) DESC, p.id DESC
            """)
    List<Long> findIdsByCidadaoCpf(@Param("cpf") String cpf, Pageable pageable);

    @Query("""
            SELECT p.id FROM Processo p
            WHERE p.equipe.id IN :equipeIds
            ORDER BY COALESCE(p.dataUltimaMovimentacao, p.dataAtualizacao, p.dataCriacao) DESC, p.id DESC
            """)
    List<Long> findIdsByEquipeIds(@Param("equipeIds") List<Long> equipeIds, Pageable pageable);

    @EntityGraph(attributePaths = {"jurisdicao"})
    @Query("""
            SELECT p FROM Processo p
            LEFT JOIN p.usuario u
            WHERE (p.parteAutoraCpf = :cpf OR p.parteReuCpf = :cpf OR u.cpf = :cpf)
            ORDER BY p.dataUltimaMovimentacao DESC NULLS LAST, p.id DESC
            """)
    Page<Processo> findByCidadaoCpf(@Param("cpf") String cpf, Pageable pageable);

    @EntityGraph(attributePaths = {"jurisdicao"})
    @Query("""
            SELECT p FROM Processo p
            LEFT JOIN p.usuario u
            WHERE u.cpf = :cpf
            ORDER BY p.dataUltimaMovimentacao DESC NULLS LAST, p.id DESC
            """)
    Page<Processo> findByAdvogadoCpf(@Param("cpf") String cpf, Pageable pageable);

    Page<Processo> findByVaraAndStatusProcesso(String vara, StatusProcesso statusProcesso, Pageable pageable);

    @EntityGraph(attributePaths = {"jurisdicao"})
    @Query("""
            SELECT p FROM Processo p
            LEFT JOIN p.usuario u
            WHERE (p.parteAutoraCpf = :doc OR p.parteReuCpf = :doc OR u.cpf = :doc)
            ORDER BY p.dataUltimaMovimentacao DESC NULLS LAST, p.id DESC
            """)
    Page<Processo> findByClienteCpfCnpj(@Param("doc") String doc, Pageable pageable);

    @EntityGraph(attributePaths = {"jurisdicao"})
    @Query("""
            SELECT p FROM Processo p JOIN p.jurisdicao j
            WHERE (:comarca IS NULL OR j.comarca = :comarca)
              AND (:uf IS NULL OR j.estado = :uf)
            ORDER BY p.dataUltimaMovimentacao DESC NULLS LAST, p.id DESC
            """)
    Page<Processo> findByComarcaAndUf(@Param("comarca") String comarca, @Param("uf") String uf, Pageable pageable);

    @Query("""
            SELECT COUNT(p) FROM Processo p JOIN p.jurisdicao j
            WHERE (:uf IS NULL OR j.estado = :uf)
            """)
    long countByUf(@Param("uf") String uf);

    @Query("""
            SELECT COUNT(p) FROM Processo p JOIN p.jurisdicao j
            WHERE (:uf IS NULL OR j.estado = :uf)
              AND (:comarca IS NULL OR j.comarca = :comarca)
            """)
    long countByUfAndComarca(@Param("uf") String uf, @Param("comarca") String comarca);

    @Query("""
            SELECT COUNT(p) FROM Processo p JOIN p.jurisdicao j
            WHERE (:uf IS NULL OR j.estado = :uf)
              AND p.statusProcesso <> 'ARQUIVADO'
              AND p.faseAtual IS NOT NULL
            """)
    long countByUfAndFaseAtivaNotArchived(@Param("uf") String uf);

    @Query("""
            SELECT p FROM Processo p
            WHERE (:vara IS NULL OR :vara = '' OR p.vara = :vara)
              AND (:comarca IS NULL OR :comarca = '' OR p.comarca = :comarca)
              AND (:uf IS NULL OR :uf = '' OR p.uf = :uf)
              AND (:rito IS NULL OR p.rito = :rito)
              AND (:faseAtual IS NULL OR p.faseAtual = :faseAtual)
              AND (:statusProcesso IS NULL OR p.statusProcesso = :statusProcesso)
            ORDER BY p.vara ASC, p.id ASC
            """)
    Page<Processo> findParaPainelOrganizacao(
            @Param("vara") String vara,
            @Param("comarca") String comarca,
            @Param("uf") String uf,
            @Param("rito") RitoProcessual rito,
            @Param("faseAtual") FaseProcessual faseAtual,
            @Param("statusProcesso") StatusProcesso statusProcesso,
            Pageable pageable);

}
