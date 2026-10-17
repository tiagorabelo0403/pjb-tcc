package com.tcc.pjb.backend.service.intelligence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.magistratura.lotacao.MagistradoLotacaoAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.ChatService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.notification.NotificationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JudgeAgreementApprovalServiceTest {

    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final InstitutionalActorRoutingService routingService = mock(InstitutionalActorRoutingService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ChatService chatService = mock(ChatService.class);
    private final MagistradoLotacaoAccessService lotacaoAccess = mock(MagistradoLotacaoAccessService.class);
    private final JudgeAgreementApprovalService service = new JudgeAgreementApprovalService(
            workItemRepository, routingService, notificationService, chatService, lotacaoAccess);

    private final Processo processo = Processo.builder().id(80L).numeroUnificado("0000080-11.2026.8.06.0001").build();
    private final PropostaAcordo proposta = PropostaAcordo.builder().id(81L).processo(processo).valorAcordo(new BigDecimal("5000.00")).build();

    @Test
    void pedidoDeHomologacaoSoNotificaMagistradosLotadosNaUnidadeDoProcesso() {
        prepararFila();
        Usuario lotado = usuario(9L, TipoUsuario.JUIZ, true);
        when(lotacaoAccess.magistradosLotadosNaUnidadeDoProcesso(processo)).thenReturn(List.of(lotado));

        var resposta = service.requestApproval(processo, proposta, null, null, "Resumo");

        verify(notificationService).notifyUser(eq(lotado), eq(processo), anyString(), anyString(), anyString());
        verify(notificationService, org.mockito.Mockito.times(1)).notifyUser(any(), any(), anyString(), anyString(), anyString());
        assertThat(resposta).isNotNull();
    }

    @Test
    void semMagistradoLotadoNinguemENotificadoEAFilaDoGabineteContinua() {
        prepararFila();
        when(lotacaoAccess.magistradosLotadosNaUnidadeDoProcesso(processo)).thenReturn(List.of());

        service.requestApproval(processo, proposta, null, null, "Resumo");

        verify(notificationService, never()).notifyUser(any(), any(), anyString(), anyString(), anyString());
        verify(workItemRepository, org.mockito.Mockito.atLeastOnce()).save(any(WorkItem.class));
    }

    @Test
    void notificaNoMaximoSeisMagistradosHabilitados() {
        prepararFila();
        List<Usuario> habilitados = java.util.stream.LongStream.rangeClosed(1, 8).mapToObj(id -> usuario(id, TipoUsuario.JUIZ, true)).toList();
        when(lotacaoAccess.magistradosLotadosNaUnidadeDoProcesso(processo)).thenReturn(habilitados);

        service.requestApproval(processo, proposta, null, null, "Resumo");

        verify(notificationService, org.mockito.Mockito.times(6)).notifyUser(any(), eq(processo), anyString(), anyString(), anyString());
        verify(notificationService, never()).notifyUser(eq(habilitados.get(6)), any(), anyString(), anyString(), anyString());
    }

    private void prepararFila() {
        when(routingService.gabineteDecision(80L, "HOMOLOGACAO_ACORDO")).thenReturn(new InstitutionalActorRoutingService.InstitutionalRoute(
                "GABINETE", "gabinete:80", TipoUsuario.JUIZ, "GABINETE", "topologia", "homologacao", Map.of()));
        when(workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(eq(80L), anyString(), any())).thenReturn(Optional.empty());
        when(workItemRepository.findAllByProcesso(80L)).thenReturn(List.of());
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Usuario usuario(Long id, TipoUsuario tipo, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoUsuario(tipo);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
