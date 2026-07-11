package com.tcc.pjb.backend.diagnostic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.DreamOutboxJpaEntity;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.DreamOutboxJpaRepository;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeEventoTipo;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloCompletudeOutboxEntity;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloCompletudeOutboxRepository;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoAuditoriaEvento;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoPropostaStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoPropostaTipo;
import com.tcc.pjb.backend.modules.acordo.infrastructure.persistence.AcordoAuditoriaEntity;
import com.tcc.pjb.backend.modules.acordo.infrastructure.persistence.AcordoAuditoriaJpaRepository;
import com.tcc.pjb.backend.modules.acordo.infrastructure.persistence.AcordoPropostaEntity;
import com.tcc.pjb.backend.modules.acordo.infrastructure.persistence.AcordoPropostaJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class JsonbBindingDiagnosticIT extends PjbIntegrationTestBase {

    @Autowired
    private ProtocoloCompletudeOutboxRepository outboxRepo;

    @Autowired
    private DreamOutboxJpaRepository dreamRepo;

    @Autowired
    private AcordoAuditoriaJpaRepository acordoAuditoriaRepo;

    @Autowired
    private AcordoPropostaJpaRepository acordoPropostaRepo;

    @Test
    void protocoloCompletudeOutbox_persisteSemErroDeBindingJsonb() {
        ProtocoloCompletudeOutboxEntity e = new ProtocoloCompletudeOutboxEntity();
        e.setId(UUID.randomUUID());
        e.setProtocoloId(1L);
        e.setTipo(ProtocoloCompletudeEventoTipo.PROTOCOLO_PENDENTE_DOCUMENTACAO);
        e.setPayload("{\"teste\":true}");
        e.setProcessado(false);
        e.setTentativas(0);
        e.setCriadoEm(Instant.now());
        outboxRepo.saveAndFlush(e);
    }

    @Test
    void dreamOutbox_falhaPorForeignKeyNaoPorBindingJsonb() {
        DreamOutboxJpaEntity e = new DreamOutboxJpaEntity();
        e.setId(UUID.randomUUID());
        e.setDreamId(UUID.randomUUID());
        e.setPayload("{\"teste\":true}");
        e.setProcessado(false);
        e.setCriadoEm(Instant.now());
        assertThatThrownBy(() -> dreamRepo.saveAndFlush(e))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("dream_outbox_dream_id_fkey")
                .hasMessageNotContaining("is of type jsonb");
    }

    @Test
    void acordoAuditoria_falhaPorForeignKeyNaoPorBindingJsonb() {
        AcordoAuditoriaEntity e = new AcordoAuditoriaEntity();
        e.setSessaoId(1L);
        e.setEvento(AcordoAuditoriaEvento.ABERTURA);
        e.setDetalhesJson("{\"teste\":true}");
        e.setCreatedAt(Instant.now());
        assertThatThrownBy(() -> acordoAuditoriaRepo.saveAndFlush(e))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_aca_sessao")
                .hasMessageNotContaining("is of type jsonb");
    }

    @Test
    void acordoProposta_falhaPorForeignKeyNaoPorBindingJsonb() {
        AcordoPropostaEntity e = new AcordoPropostaEntity();
        e.setSessaoId(1L);
        e.setAutorId(1L);
        e.setTipo(AcordoPropostaTipo.FORMAL);
        e.setTermosJson("{\"teste\":true}");
        e.setValidadeAte(Instant.now().plusSeconds(3600));
        e.setStatus(AcordoPropostaStatus.PENDENTE);
        e.setCriadaPorIa(false);
        e.setRevisadaPorHumano(false);
        e.setCreatedAt(Instant.now());
        assertThatThrownBy(() -> acordoPropostaRepo.saveAndFlush(e))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_acpr_sessao")
                .hasMessageNotContaining("is of type jsonb");
    }
}
