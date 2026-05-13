package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpedicaoJudicialRepository extends JpaRepository<ExpedicaoJudicial, Long> {

    Optional<ExpedicaoJudicial> findByExpedicaoUuid(String expedicaoUuid);


    List<ExpedicaoJudicial> findTop100ByDestinatarioDocumentoOrderByExpedidaEmDesc(String destinatarioDocumento);

    List<ExpedicaoJudicial> findTop200ByProcessoIdInOrderByExpedidaEmDesc(Collection<Long> processoIds);

    List<ExpedicaoJudicial> findByDestinatarioDocumentoAndStatusNotOrderByExpedidaEmDesc(
            String destinatarioDocumento,
            ExpedicaoJudicial.StatusExpedicao status
    );

    long countByStatus(ExpedicaoJudicial.StatusExpedicao status);

    long countByStatusIn(Collection<ExpedicaoJudicial.StatusExpedicao> statuses);

    long countByEvasaoDetectadaTrue();

    long countByEscalonadoParaJuizTrue();

    List<ExpedicaoJudicial> findByStatusAndPresuncaoEntregaEmLessThanEqual(
            ExpedicaoJudicial.StatusExpedicao status,
            Instant limite
    );

    List<ExpedicaoJudicial> findByInstanciaExpedidoraIgnoreCase(String instanciaExpedidora);

    List<ExpedicaoJudicial> findByModalidade(ModalidadeExpedicaoJudicial modalidade);

    @Query("""
            select count(e)
            from ExpedicaoJudicial e
            where e.destinatarioDocumento = :documento
              and e.frustradaEm >= :desde
              and e.motivoFrustracao is not null
            """)
    long contarFrustracoesRecentes(@Param("documento") String documento, @Param("desde") Instant desde);

    @Query("""
            select e
            from ExpedicaoJudicial e
            where e.destinatarioDocumento = :documento
              and e.status in :statusesEntregues
              and e.modalidade not in :modalidadesFisicas
            order by e.expedidaEm desc
            """)
    List<ExpedicaoJudicial> buscarHistoricoDigitalConfirmado(
            @Param("documento") String documento,
            @Param("statusesEntregues") Collection<ExpedicaoJudicial.StatusExpedicao> statusesEntregues,
            @Param("modalidadesFisicas") Collection<ModalidadeExpedicaoJudicial> modalidadesFisicas,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
            select (count(e) > 0)
            from ExpedicaoJudicial e
            where e.destinatarioDocumento = :documento
              and e.status in :statusesEntregues
              and e.modalidade not in :modalidadesFisicas
            """)
    boolean existsHistoricoDigitalConfirmado(
            @Param("documento") String documento,
            @Param("statusesEntregues") Collection<ExpedicaoJudicial.StatusExpedicao> statusesEntregues,
            @Param("modalidadesFisicas") Collection<ModalidadeExpedicaoJudicial> modalidadesFisicas
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ExpedicaoJudicial e
               set e.status = :statusPresumido,
                   e.prazoRespostaInicioEm = :agora
             where e.status = :statusExpedida
               and e.presuncaoEntregaEm is not null
               and e.presuncaoEntregaEm <= :agora
            """)
    int atualizarPresuncoesPendentes(
            @Param("agora") Instant agora,
            @Param("statusExpedida") ExpedicaoJudicial.StatusExpedicao statusExpedida,
            @Param("statusPresumido") ExpedicaoJudicial.StatusExpedicao statusPresumido
    );

    @Query("""
            select e
            from ExpedicaoJudicial e
            where e.evasaoDetectada = true
              and e.escalonadoParaJuiz = false
              and e.status <> :statusCancelada
            order by e.expedidaEm asc
            """)
    List<ExpedicaoJudicial> findEvasoesNaoEscalonadas(@Param("statusCancelada") ExpedicaoJudicial.StatusExpedicao statusCancelada);

    default Optional<ExpedicaoJudicial> findUltimaEntregaDigitalConfirmada(String documento) {
        return buscarHistoricoDigitalConfirmado(
                documento,
                statusesEntregues(),
                modalidadesFisicas(),
                PageRequest.of(0, 1)
        ).stream().findFirst();
    }

    default boolean jaFoiServadoDigitalmente(String documento) {
        return existsHistoricoDigitalConfirmado(documento, statusesEntregues(), modalidadesFisicas());
    }

    default int atualizarPresuncoesPendentes(Instant agora) {
        return atualizarPresuncoesPendentes(
                agora,
                ExpedicaoJudicial.StatusExpedicao.EXPEDIDA,
                ExpedicaoJudicial.StatusExpedicao.PRESUMIDA_ENTREGUE
        );
    }

    default List<ExpedicaoJudicial> findEvasoesNaoEscalonadas() {
        return findEvasoesNaoEscalonadas(ExpedicaoJudicial.StatusExpedicao.CANCELADA);
    }

    private static Set<ExpedicaoJudicial.StatusExpedicao> statusesEntregues() {
        return EnumSet.of(
                ExpedicaoJudicial.StatusExpedicao.ENTREGUE_CONFIRMADA,
                ExpedicaoJudicial.StatusExpedicao.LIDA_CONFIRMADA,
                ExpedicaoJudicial.StatusExpedicao.PRESUMIDA_ENTREGUE
        );
    }

    private static Set<ModalidadeExpedicaoJudicial> modalidadesFisicas() {
        return EnumSet.of(
                ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA,
                ModalidadeExpedicaoJudicial.CORREIO_AR_DIGITAL,
                ModalidadeExpedicaoJudicial.EDITAL_DOU_DJE
        );
    }
}
