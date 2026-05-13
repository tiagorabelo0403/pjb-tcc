package com.tcc.pjb.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.EquipeContexto;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementIntelligenceService;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.inovacao.batna.FacilitadorBatnaService;
import com.tcc.pjb.backend.model.dto.AcordoHomologadoEvent;
import com.tcc.pjb.backend.model.dto.PdfGenerationResult;
import com.tcc.pjb.backend.model.entity.AcordoHomologado;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.AcordoHomologadoRepository;
import com.tcc.pjb.backend.model.repository.ModeloContratoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.model.repository.ChatMensagemRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.engine.FinancialValidatorEngine;
import com.tcc.pjb.backend.service.engine.LegalRhythmEngine;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import com.tcc.pjb.backend.service.intelligence.AgreementChatLedgerService;
import com.tcc.pjb.backend.service.intelligence.JudgeAgreementApprovalService;
import com.tcc.pjb.backend.service.intelligence.ProcessOutcomePredictionService;
import com.tcc.pjb.backend.service.notification.NotificationService;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AcordoServiceTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PropostaAcordoRepository propostaAcordoRepository = mock(PropostaAcordoRepository.class);
    private final ChatMensagemRepository chatMensagemRepository = mock(ChatMensagemRepository.class);
    private final AcordoHomologadoRepository acordoHomologadoRepository = mock(AcordoHomologadoRepository.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final PdfGeneratorService pdfGeneratorService = mock(PdfGeneratorService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final DomainEventPublisher domainEventPublisher = mock(DomainEventPublisher.class);
    private final ChatService chatService = mock(ChatService.class);
    private final UiHistoryService uiHistoryService = mock(UiHistoryService.class);
    private final AgreementChatLedgerService agreementChatLedgerService = mock(AgreementChatLedgerService.class);

    private final AcordoService service = new AcordoService(
            processoRepository,
            usuarioRepository,
            mock(ModeloContratoRepository.class),
            propostaAcordoRepository,
            chatMensagemRepository,
            acordoHomologadoRepository,
            mock(ProfileEngine.class),
            pdfGeneratorService,
            notificationService,
            auditService,
            domainEventPublisher,
            chatService,
            mock(AcordoSuggestionPipelineAsyncService.class),
            uiHistoryService,
            mock(ProcessoRitoSnapshotService.class),
            mock(SettlementIntelligenceService.class),
            mock(ProcessMaterialDossierService.class),
            mock(ProcessMaterialStrategyService.class),
            mock(SettlementAdvisoryService.class),
            mock(ProcessOutcomePredictionService.class),
            mock(JudgeAgreementApprovalService.class),
            workItemRepository,
            agreementChatLedgerService,
            mock(LegalRhythmEngine.class),
            mock(FinancialValidatorEngine.class),
            mock(FacilitadorBatnaService.class)
    );

    @AfterEach
    void tearDown() {
        EquipeContexto.clear();
    }

    @Test
    void shouldHomologarAcordoJudicialAndPropagateCriticalSideEffects() {
        Processo processo = Processo.builder()
                .id(200L)
                .numeroUnificado("000200-11.2026.8.06.0001")
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .resultadoFinal("EM_ANALISE")
                .valorCausa(BigDecimal.valueOf(25000))
                .build();
        PropostaAcordo proposta = PropostaAcordo.builder()
                .id(300L)
                .uuid(UUID.randomUUID())
                .processo(processo)
                .termosHtml("<p>acordo homologável</p>")
                .status(StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ)
                .hashAssinaturaParte1("parte-1")
                .hashAssinaturaParte2("parte-2")
                .build();
        Usuario juiz = usuario(400L, "Juiz Titular", TipoUsuario.JUIZ);
        Usuario admin = usuario(1L, "Sistema", TipoUsuario.ADMINISTRADOR);

        when(propostaAcordoRepository.findById(300L)).thenReturn(Optional.of(proposta));
        when(usuarioRepository.findByTipoUsuario(TipoUsuario.ADMINISTRADOR)).thenReturn(List.of(admin));
        when(pdfGeneratorService.generatePdfWithSealAndQr(eq("<p>acordo homologável</p>"), eq(proposta.getUuid()), any()))
                .thenReturn(new PdfGenerationResult("/pdfs/homologado.pdf", "hash-pdf"));
        when(acordoHomologadoRepository.save(any(AcordoHomologado.class))).thenAnswer(invocation -> {
            AcordoHomologado acordo = invocation.getArgument(0);
            acordo.setId(500L);
            return acordo;
        });
        when(processoRepository.save(any(Processo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(propostaAcordoRepository.save(any(PropostaAcordo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final AcordoHomologado[] holder = new AcordoHomologado[1];
        RequestContext.run("req-acordo", () -> {
            EquipeContexto.setMembroAtivo(membroJuiz(juiz));
            holder[0] = service.homologarAcordoJudicial(300L, "hash-juiz");
        });
        AcordoHomologado homologado = holder[0];

        assertNotNull(homologado);
        assertEquals(500L, homologado.getId());
        assertSame(proposta, homologado.getProposta());
        assertSame(processo, homologado.getProcesso());
        assertSame(juiz, homologado.getJuiz());
        assertEquals("/pdfs/homologado.pdf", homologado.getUrlPdfHomologado());
        assertEquals("hash-juiz", homologado.getHashAssinaturaJuiz());
        assertEquals(StatusProcesso.JULGADO, processo.getStatusProcesso());
        assertEquals("ACORDO_HOMOLOGADO", processo.getResultadoFinal());
        assertEquals(StatusAcordo.HOMOLOGADO, proposta.getStatus());

        verify(uiHistoryService).recordProcessoStatusChange(
                eq(processo),
                eq(StatusProcesso.EM_ANDAMENTO),
                eq("EM_ANALISE"),
                eq(StatusProcesso.JULGADO),
                eq("ACORDO_HOMOLOGADO"),
                eq("Acordo homologado judicialmente")
        );
        verify(auditService).recordAgreementHomologation(eq(proposta.getUuid()), any());
        verify(notificationService).notifyJudge(eq(juiz), eq(processo), eq("Homologação Realizada"), eq("Acordo selado no processo 000200-11.2026.8.06.0001"), eq("/pdfs/homologado.pdf"));
        verify(notificationService).notifyLawyers(eq(processo), eq("Acordo Homologado"), eq("/pdfs/homologado.pdf"));
        verify(chatService).postarMensagemSistema(eq(processo), eq(admin), eq("⚖️ Sentença Homologatória proferida. Processo extinto com resolução de mérito."));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        AcordoHomologadoEvent event = (AcordoHomologadoEvent) eventCaptor.getValue();
        assertEquals(200L, fieldValue(event, "processoId"));
        assertEquals(proposta.getUuid(), fieldValue(event, "propostaUuid"));
        assertEquals(400L, fieldValue(event, "juizId"));
    }

    @Test
    void shouldRejectHomologationWhenProposalIsOutOfJudgeStage() {
        Processo processo = Processo.builder().id(201L).numeroUnificado("000201").statusProcesso(StatusProcesso.EM_ANDAMENTO).build();
        PropostaAcordo proposta = PropostaAcordo.builder()
                .id(301L)
                .uuid(UUID.randomUUID())
                .processo(processo)
                .termosHtml("<p>pendente</p>")
                .status(StatusAcordo.EM_NEGOCIACAO)
                .build();
        Usuario juiz = usuario(401L, "Juiz", TipoUsuario.JUIZ);
        when(propostaAcordoRepository.findById(301L)).thenReturn(Optional.of(proposta));

        RequestContext.run("req-acordo-invalido", () -> {
            EquipeContexto.setMembroAtivo(membroJuiz(juiz));
            assertThrows(RegraNegocioException.class, () -> service.homologarAcordoJudicial(301L, "hash-juiz"));
        });

        verify(pdfGeneratorService, never()).generatePdfWithSealAndQr(any(), any(), any());
        verify(acordoHomologadoRepository, never()).save(any());
        verify(processoRepository, never()).save(any());
        verify(domainEventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectHomologationWithoutJudgeContext() {
        PropostaAcordo proposta = PropostaAcordo.builder()
                .id(302L)
                .uuid(UUID.randomUUID())
                .processo(Processo.builder().id(202L).numeroUnificado("000202").build())
                .termosHtml("<p>acordo</p>")
                .status(StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ)
                .build();
        when(propostaAcordoRepository.findById(302L)).thenReturn(Optional.of(proposta));

        RequestContext.run("req-acordo-sem-juiz", () -> {
            assertThrows(SecurityException.class, () -> service.homologarAcordoJudicial(302L, "hash-juiz"));
        });

        verify(propostaAcordoRepository, never()).save(any(PropostaAcordo.class));
        verify(acordoHomologadoRepository, never()).save(any(AcordoHomologado.class));
    }


    @Test
    void shouldReturnProposalToReviewWhenJudgeRequestsRevision() {
        Processo processo = Processo.builder().id(203L).numeroUnificado("000203").statusProcesso(StatusProcesso.EM_ANDAMENTO).build();
        PropostaAcordo proposta = PropostaAcordo.builder()
                .id(303L)
                .uuid(UUID.randomUUID())
                .processo(processo)
                .termosHtml("<p>acordo</p>")
                .status(StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ)
                .build();
        Usuario juiz = usuario(402L, "Juiz Revisor", TipoUsuario.JUIZ);
        when(propostaAcordoRepository.findById(303L)).thenReturn(Optional.of(proposta));
        when(propostaAcordoRepository.save(any(PropostaAcordo.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agreementChatLedgerService.renderDecisionMessage(eq("DEVOLVER_PARA_REVISAO"), any())).thenReturn("Decisão judicial do acordo: DEVOLVER_PARA_REVISAO.");

        RequestContext.run("req-acordo-revisao", () -> {
            EquipeContexto.setMembroAtivo(membroJuiz(juiz));
            var response = service.decidirHomologacaoJudicial(303L, "DEVOLVER_PARA_REVISAO", "Ajustar cronograma e cláusula penal.", null, false);
            assertEquals("DEVOLVER_PARA_REVISAO", response.action());
            assertEquals(StatusAcordo.AGUARDANDO_REVISAO_HUMANA.name(), response.statusAcordo());
        });

        assertEquals(StatusAcordo.AGUARDANDO_REVISAO_HUMANA, proposta.getStatus());
        verify(chatService).postarMensagemSistema(eq(processo), org.mockito.ArgumentMatchers.contains("DEVOLVER_PARA_REVISAO"));
    }

    private static Usuario usuario(Long id, String nome, TipoUsuario tipoUsuario) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setTipoUsuario(tipoUsuario);
        return usuario;
    }

    private static Object fieldValue(Object target, String fieldName) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private static MembroEquipe membroJuiz(Usuario juiz) {
        MembroEquipe membroEquipe = new MembroEquipe();
        membroEquipe.setUsuario(juiz);
        membroEquipe.setPapel(PapelEquipe.JUIZ_GABINETE);
        return membroEquipe;
    }
}
