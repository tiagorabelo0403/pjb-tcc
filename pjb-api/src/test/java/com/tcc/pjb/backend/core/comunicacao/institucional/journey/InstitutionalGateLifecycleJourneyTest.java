package com.tcc.pjb.backend.core.comunicacao.institucional.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.audit.application.InstitutionalCommunicationAuditApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.application.InstitutionalCommunicationGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateStatus;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.infrastructure.InstitutionalGateStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InstitutionalGateLifecycleJourneyTest {

    @Test
    void shouldCreateBlockAdvanceOnScienceAndReleaseOnFulfillment() {
        InstitutionalGateStateRepository repository = Mockito.mock(InstitutionalGateStateRepository.class);
        InstitutionalCommunicationAuditApplicationService audit = Mockito.mock(InstitutionalCommunicationAuditApplicationService.class);
        doNothing().when(audit).registrarGateCriado(any(), any());
        doNothing().when(audit).registrarGateTransicao(any(), any(), any(), any());
        doNothing().when(audit).registrarGateLiberado(any(), any(), any(), any());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InstitutionalCommunicationGateApplicationService service = new InstitutionalCommunicationGateApplicationService(repository, audit);
        Instant now = Instant.parse("2026-03-20T15:30:00Z");
        InstitutionalInboxItem item = new InstitutionalInboxItem(
                "inbox-1",
                "exp-1",
                900L,
                "0000900-11.2026.8.26.0001",
                "MP-SP-FAM-1G",
                "MP/SP",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                TipoComunicacaoJudicial.INTIMACAO_PESSOAL_DEFENSOR,
                "CAIXA-PRINCIPAL",
                "CAIXA-PRINCIPAL",
                "PJB_INBOX",
                StatusComunicacaoInstitucional.DISPONIBILIZADA,
                "GATE_MP_INTERESSE_INCAPAZ",
                true,
                null,
                null,
                now,
                null,
                null,
                null,
                now.plusSeconds(3600),
                now.plusSeconds(7200),
                now,
                List.of("seed"),
                "hash-seed"
        );

        InstitutionalGateState criado = service.criarSeNecessario(item).orElseThrow();
        when(repository.findByExpedicaoUuid(item.expedicaoUuid())).thenReturn(Optional.of(criado));

        Usuario actor = Mockito.mock(Usuario.class);
        when(actor.getId()).thenReturn(55L);
        when(actor.getTipoUsuario()).thenReturn(TipoUsuario.SERVIDOR);

        InstitutionalGateState aguardandoCumprimento = service.marcarCiencia(item, actor, "ciência validada").orElseThrow();
        when(repository.findByExpedicaoUuid(item.expedicaoUuid())).thenReturn(Optional.of(aguardandoCumprimento));
        InstitutionalGateState liberado = service.marcarCumprimento(item, actor, "parecer juntado").orElseThrow();

        assertEquals(InstitutionalGateStatus.AGUARDANDO_CIENCIA, criado.status());
        assertTrue(criado.bloqueado());
        assertEquals(InstitutionalGateStatus.AGUARDANDO_CUMPRIMENTO, aguardandoCumprimento.status());
        assertTrue(aguardandoCumprimento.bloqueado());
        assertEquals(InstitutionalGateStatus.LIBERADO, liberado.status());
        assertTrue(!liberado.bloqueado());
    }
}
