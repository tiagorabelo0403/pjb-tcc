package com.tcc.pjb.backend.core.comunicacao.institucional.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.AutorizacaoCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.application.InstitutionalCommunicationAuditApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalWorkflowApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDelegationAssignment;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDraftManifestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDelegationAssignmentStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDraftManifestationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusDelegacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusMinutaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoFluxoDelegacaoInstitucional;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstitutionalWorkflowApplicationServiceTest {

    private InstitutionalInboxApplicationService inboxApplicationService;
    private AutorizacaoCaixaInstitucionalService autorizacaoCaixaInstitucionalService;
    private InstitutionalInboxStateRepository inboxStateRepository;
    private InstitutionalDelegationAssignmentStateRepository delegationRepository;
    private InstitutionalDraftManifestationStateRepository draftRepository;
    private InstitutionalCommunicationAuditApplicationService auditService;
    private CurrentUserService currentUserService;
    private UsuarioRepository usuarioRepository;
    private OutboxPublisher outboxPublisher;
    private InstitutionalWorkflowApplicationService service;
    private Usuario actor;
    private InstitutionalInboxItem inbox;

    @BeforeEach
    void setUp() {
        inboxApplicationService = mock(InstitutionalInboxApplicationService.class);
        autorizacaoCaixaInstitucionalService = mock(AutorizacaoCaixaInstitucionalService.class);
        inboxStateRepository = mock(InstitutionalInboxStateRepository.class);
        delegationRepository = mock(InstitutionalDelegationAssignmentStateRepository.class);
        draftRepository = mock(InstitutionalDraftManifestationStateRepository.class);
        auditService = mock(InstitutionalCommunicationAuditApplicationService.class);
        currentUserService = mock(CurrentUserService.class);
        usuarioRepository = mock(UsuarioRepository.class);
        outboxPublisher = mock(OutboxPublisher.class);
        service = new InstitutionalWorkflowApplicationService(inboxApplicationService, autorizacaoCaixaInstitucionalService, inboxStateRepository, delegationRepository, draftRepository, auditService, currentUserService, usuarioRepository, outboxPublisher);
        actor = new Usuario();
        actor.setId(10L);
        actor.setNome("Titular");
        when(currentUserService.getRequired()).thenReturn(actor);
        Instant now = Instant.now();
        inbox = new InstitutionalInboxItem("inbox-1", "exp-1", 77L, "000077", "UNI", "UNI", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA, TipoComunicacaoJudicial.INTIMACAO_PESSOAL_DEFENSOR, "CX", "CX", "DOMICILIO_JUDICIAL", StatusComunicacaoInstitucional.DISPONIBILIZADA, null, false, null, null, now, null, null, null, now.plusSeconds(3600), now.plusSeconds(7200), now, List.of(), "hash");
        when(inboxApplicationService.loadVisible("exp-1")).thenReturn(inbox);
        when(inboxStateRepository.findByExpedicaoUuid("exp-1")).thenReturn(Optional.of(inbox));
        when(delegationRepository.findByExpedicaoUuid("exp-1")).thenReturn(List.of());
        var allowed = new com.tcc.pjb.backend.core.comunicacao.institucional.access.ResultadoAutorizacaoCaixaInstitucional(true, "UNI", "CX", CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, List.of(), List.of());
        when(autorizacaoCaixaInstitucionalService.autorizar(eq("UNI"), eq("CX"), any())).thenReturn(allowed);
        when(delegationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldRejectDelegationToCurrentUser() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(actor));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.delegar("exp-1", 10L, EnumSet.of(CapacidadeCaixaInstitucional.PREPARAR_MINUTA), 4, "teste"));
        assertEquals("delegacao_para_o_proprio_usuario_nao_eh_permitida", ex.getMessage());
        verify(delegationRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateActiveSubstitutionForSameUser() {
        Usuario substitute = new Usuario();
        substitute.setId(20L);
        substitute.setNome("Substituto");
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(substitute));
        Instant now = Instant.now();
        when(delegationRepository.findByExpedicaoUuid("exp-1")).thenReturn(List.of(new InstitutionalDelegationAssignment(
                "a1", "exp-1", 77L, "UNI", "CX", 10L, 20L,
                TipoFluxoDelegacaoInstitucional.SUBSTITUICAO,
                EnumSet.of(CapacidadeCaixaInstitucional.PREPARAR_MINUTA),
                StatusDelegacaoInstitucional.ATIVA,
                "ativa", now.minusSeconds(60), now.plusSeconds(3600), now.minusSeconds(60), now.minusSeconds(60), "hash"
        )));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.substituir("exp-1", 20L, 12, "teste"));
        assertEquals("ja_existe_atribuicao_ativa_equivalente_para_o_usuario_informado", ex.getMessage());
    }

    @Test
    void shouldRejectDraftSubmissionToSameAuthor() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(actor));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.submeterMinuta("exp-1", "draft-1", 10L, "encaminhar"));
        assertEquals("submissao_de_minuta_exige_aprovador_distinto_do_submissor", ex.getMessage());
    }

    @Test
    void shouldCreateNewDraftIdentityAfterTerminalHistory() {
        Instant now = Instant.now();
        InstitutionalDraftManifestation rejected = new InstitutionalDraftManifestation(
                "draft-old", "exp-1", 77L, "UNI", "CX", 10L, 20L,
                StatusMinutaInstitucional.REJEITADA,
                "Titulo antigo", "Conteudo antigo", "obs",
                now.minusSeconds(120), now.minusSeconds(90), now.minusSeconds(60), now.minusSeconds(60), "hash-old"
        );
        when(draftRepository.findByExpedicaoUuid("exp-1")).thenReturn(List.of(rejected));

        InstitutionalDraftManifestation created = service.criarOuAtualizarMinuta("exp-1", "Nova minuta", "Novo conteudo", "obs nova");

        assertNotEquals("draft-old", created.draftId());
        assertEquals(StatusMinutaInstitucional.RASCUNHO, created.status());
        verify(draftRepository).save(any(InstitutionalDraftManifestation.class));
    }

    @Test
    void shouldRejectEditingDraftWhenApprovalIsPending() {
        Instant now = Instant.now();
        InstitutionalDraftManifestation pending = new InstitutionalDraftManifestation(
                "draft-pending", "exp-1", 77L, "UNI", "CX", 10L, 20L,
                StatusMinutaInstitucional.EM_APROVACAO,
                "Titulo", "Conteudo", "obs",
                now.minusSeconds(120), now.minusSeconds(90), null, now.minusSeconds(90), "hash-pending"
        );
        when(draftRepository.findByExpedicaoUuid("exp-1")).thenReturn(List.of(pending));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.criarOuAtualizarMinuta("exp-1", "Nova", "Conteudo", "obs"));

        assertEquals("minuta_em_aprovacao_nao_pode_ser_editada", ex.getMessage());
        verify(draftRepository, never()).save(any());
    }

    @Test
    void shouldRejectApprovalByActorDifferentFromDesignatedApprover() {
        Usuario approver = new Usuario();
        approver.setId(20L);
        approver.setNome("Aprovador");
        Instant now = Instant.now();
        InstitutionalDraftManifestation pending = new InstitutionalDraftManifestation(
                "draft-pending", "exp-1", 77L, "UNI", "CX", 10L, 20L,
                StatusMinutaInstitucional.EM_APROVACAO,
                "Titulo", "Conteudo", "obs",
                now.minusSeconds(120), now.minusSeconds(90), null, now.minusSeconds(90), "hash-pending"
        );
        when(draftRepository.findByDraftId("draft-pending")).thenReturn(Optional.of(pending));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.aprovarMinuta("exp-1", "draft-pending", "ok", false));

        assertEquals("somente_o_aprovador_designado_pode_revisar_a_minuta", ex.getMessage());
        verify(draftRepository, never()).save(any());
    }

}
