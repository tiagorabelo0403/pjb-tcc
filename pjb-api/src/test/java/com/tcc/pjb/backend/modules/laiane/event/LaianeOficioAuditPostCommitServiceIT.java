package com.tcc.pjb.backend.modules.laiane.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaEventoComportamental;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * F5 (plano de melhoria v3): o unit test original mockava AuditoriaInteligenteService e so provava
 * verify(auditoria).registrarEventoImutavelJustificado(args) -- passa mesmo que persistirEvento()
 * grave um evento com dados errados, porque o mock nunca chega a persistir nada de verdade. O
 * efeito real de LaianeOficioAuditPostCommitService.on() so e observavel via AuditoriaRepository
 * (colaborador real), entao vira IT.
 */
class LaianeOficioAuditPostCommitServiceIT extends PjbIntegrationTestBase {

    @Autowired
    LaianeOficioAuditPostCommitService handler;

    @Autowired
    AuditoriaRepository auditoriaRepository;

    @Test
    void on_eventoValido_persisteEventoDeAuditoriaComOsDadosDoEvento() {
        String referenciaId = "it-" + UUID.randomUUID();
        var event = new LaianeOficioAuditEvent(
                "MP_OFICIO_CRIADO",
                referenciaId,
                "tipo=OFICIO_REQUISITORIO;destinoId=null",
                "Fluxo institucional"
        );

        handler.on(event);

        Page<AuditoriaEventoComportamental> found =
                auditoriaRepository.search(referenciaId, "MP_OFICIO_CRIADO", null, Pageable.unpaged());
        assertThat(found.getContent()).hasSize(1);
        AuditoriaEventoComportamental persisted = found.getContent().get(0);
        assertThat(persisted.getAcao()).isEqualTo("MP_OFICIO_CRIADO");
        assertThat(persisted.getReferenciaId()).isEqualTo(referenciaId);
        assertThat(persisted.getDetalhes()).isEqualTo("tipo=OFICIO_REQUISITORIO;destinoId=null");
        assertThat(persisted.getJustificativa()).isEqualTo("Fluxo institucional");
        assertThat(persisted.getHashIntegridade()).isNotBlank();
    }

    @Test
    void on_acaoNula_naoPersisteNadaParaAReferenciaInformada() {
        String referenciaId = "it-" + UUID.randomUUID();

        handler.on(new LaianeOficioAuditEvent(null, referenciaId, "det", "just"));

        Page<AuditoriaEventoComportamental> found =
                auditoriaRepository.search(referenciaId, null, null, Pageable.unpaged());
        assertThat(found.getContent()).isEmpty();
    }

    @Test
    void on_referenciaIdNula_naoPersisteNadaParaAAcaoInformada() {
        String acaoUnica = "MP_OFICIO_CRIADO_TEST_" + UUID.randomUUID();

        handler.on(new LaianeOficioAuditEvent(acaoUnica, null, "det", "just"));

        Page<AuditoriaEventoComportamental> found =
                auditoriaRepository.search(null, acaoUnica, null, Pageable.unpaged());
        assertThat(found.getContent()).isEmpty();
    }

    @Test
    void on_eventoNulo_naoLancaExcecao() {
        // Sem evento nao ha dado de dominio para usar como discriminador de busca no ledger
        // compartilhado (base sem rollback entre testes); a unica observacao real possivel aqui
        // e que a guard clause absorve o null sem propagar excecao.
        handler.on(null);
    }
}
