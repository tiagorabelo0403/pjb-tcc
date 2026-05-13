package com.tcc.pjb.backend.service.processual.participacao;

import com.tcc.pjb.backend.core.kernel.process.ProcessEventStore;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventType;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.processual.guard.DefensoriaInstitutionalCompetenceGuardService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import com.tcc.pjb.backend.service.processual.participacao.submission.AttachmentRequest;
import com.tcc.pjb.backend.service.processual.participacao.submission.ProcessualParticipacaoAtivaSubmissionSupport;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionRequest;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionResponse;
import com.tcc.pjb.backend.service.processual.participacao.workspace.ProcessualParticipacaoAtivaWorkspaceSupport;
import com.tcc.pjb.backend.service.processual.participacao.workspace.WorkspaceView;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessualParticipacaoAtivaFacadeServiceTest {

    @Mock
    private ProcessoRepository processoRepository;
    @Mock
    private DocumentoProcessualRepository documentoRepository;
    @Mock
    private WorkItemRepository workItemRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PjbAuthorizationService authorizationService;
    @Mock
    private ProcessEventStore processEventStore;
    @Mock
    private EntityManager entityManager;
    @Mock
    private DefensoriaInstitutionalCompetenceGuardService defensoriaInstitutionalCompetenceGuardService;

    private final RepresentacaoProcessualPolicyService representacaoProcessualPolicyService = new RepresentacaoProcessualPolicyService();

    private ProcessualParticipacaoAtivaFacadeService service;

    @BeforeEach
    void setUp() {
        ProcessualParticipacaoAtivaWorkspaceSupport workspaceSupport = new ProcessualParticipacaoAtivaWorkspaceSupport(
                documentoRepository,
                workItemRepository,
                representacaoProcessualPolicyService
        );
        ProcessualParticipacaoAtivaSubmissionSupport submissionSupport = new ProcessualParticipacaoAtivaSubmissionSupport(
                documentoRepository,
                entityManager
        );
        service = new ProcessualParticipacaoAtivaFacadeService(
                processoRepository,
                workItemRepository,
                currentUserService,
                authorizationService,
                processEventStore,
                entityManager,
                defensoriaInstitutionalCompetenceGuardService,
                workspaceSupport,
                submissionSupport
        );
        lenient().doNothing().when(defensoriaInstitutionalCompetenceGuardService).requireAllowedForProcessParticipation(any(Processo.class));
    }

    @Test
    void workspaceDeAdvogadoDeveExporReplicaEContrarrazoes() {
        Processo processo = processoBase(10L, FaseProcessual.CONHECIMENTO);
        Usuario advogado = usuarioBase(22L, TipoUsuario.ADVOGADO, "Advogada Privada");

        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(advogado);
        doNothing().when(authorizationService).requireWriteProcesso(processo);
        when(documentoRepository.findRecentContextoByProcessoId(10L, 36)).thenReturn(List.of());
        when(workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(eq(10L), eq(22L), any(), eq(WorkItemStatus.CANCELADO), any()))
                .thenReturn(List.of());

        WorkspaceView workspace = service.workspace(10L);

        List<String> codes = workspace.actions().stream().map(ActionProfile::code).toList();
        assertTrue(codes.contains("APRESENTAR_REPLICA"));
        assertTrue(codes.contains("APRESENTAR_MANIFESTACAO"));
        assertFalse(codes.contains("SUBMETER_LAUDO"));
        assertEquals("ADVOCACIA_PRIVADA", workspace.persona());
    }

    @Test
    void peritoNaoPodeProtocolarReplica() {
        Processo processo = processoBase(18L, FaseProcessual.PERICIA_TECNICA);
        Usuario perito = usuarioBase(31L, TipoUsuario.PERITO_CONTABIL, "Perito Contábil");

        when(processoRepository.findById(18L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(perito);
        doNothing().when(authorizationService).requireWriteProcesso(processo);

        SubmissionRequest request = new SubmissionRequest(
                "APRESENTAR_REPLICA",
                "Réplica indevida",
                "Conteúdo que não deveria ser aceito.",
                null,
                null,
                false,
                "CERTIFICADO_INSTITUCIONAL",
                "ABCD12345678",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                List.of()
        );

        assertThrows(ErroDeValidacaoException.class, () -> service.protocolar(18L, request));
        verify(documentoRepository, never()).saveAll(any());
    }

    @Test
    void protocoloDeLaudoDeveGerarEventoEDeskDeRecepcao() {
        Processo processo = processoBase(33L, FaseProcessual.PERICIA_TECNICA);
        Usuario perito = usuarioBase(44L, TipoUsuario.PERITO_MEDICO, "Perito Médico");
        byte[] pdf = "PDF-SINTETICO-LAUDO".getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.getEncoder().encodeToString(pdf);

        when(processoRepository.findById(33L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(perito);
        doNothing().when(authorizationService).requireWriteProcesso(processo);
        when(entityManager.getReference(Processo.class, 33L)).thenReturn(processo);
        when(documentoRepository.existsByProcessoIdAndSha256(eq(33L), any())).thenReturn(false);
        when(documentoRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(processEventStore.append(eq(33L), eq(ProcessEventType.DOCUMENTS_BULK_ADDED), any())).thenReturn(ProcessEventEnvelope.builder().seq(77L).processoId(33L).eventType(ProcessEventType.DOCUMENTS_BULK_ADDED.name()).createdAt(Instant.now()).build());
        when(processEventStore.append(eq(33L), eq(ProcessEventType.MOVEMENT_RECORDED), any())).thenReturn(ProcessEventEnvelope.builder().seq(78L).processoId(33L).eventType(ProcessEventType.MOVEMENT_RECORDED.name()).createdAt(Instant.now()).build());
        when(workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(eq(33L), any(), eq(WorkItemStatus.CANCELADO))).thenReturn(Optional.empty());
        when(workItemRepository.save(any())).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(901L);
            return item;
        });

        SubmissionRequest request = new SubmissionRequest(
                "SUBMETER_LAUDO",
                "Laudo médico complementar",
                "Conclusões técnicas, metodologia empregada e respostas aos quesitos apresentados.",
                null,
                "SIGILO_N2",
                true,
                "CERTIFICADO_INSTITUCIONAL",
                "MEDI1234567890",
                "FING1234567890",
                "Prazo do juízo para entrega do laudo",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                List.of(new AttachmentRequest(
                        "laudo.pdf",
                        "application/pdf",
                        base64,
                        "Laudo PDF",
                        null,
                        "SIGILO_N2"
                ))
        );

        SubmissionResponse response = service.protocolar(33L, request);

        assertEquals(77L, response.eventoSeq());
        assertEquals(901L, response.workItemRecepcaoId());
        assertEquals("SUBMETER_LAUDO", response.acaoCodigo());
        assertEquals(2, response.documentos().size());
        assertTrue(response.inboxKey().contains("PARTICIPACAO_ATIVA"));

        ArgumentCaptor<List<DocumentoProcessual>> captor = ArgumentCaptor.forClass(List.class);
        verify(documentoRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().stream().anyMatch(doc -> "text/markdown".equals(doc.getContentType())));
        assertTrue(captor.getValue().stream().anyMatch(doc -> "application/pdf".equals(doc.getContentType())));
    }

    @Test
    void advocaciaPrivadaDeveExigirRepresentacaoParaProtocolar() {
        Processo processo = processoBase(41L, FaseProcessual.CONHECIMENTO);
        Usuario advogado = usuarioBase(51L, TipoUsuario.ADVOGADO, "Advogado Privado");

        when(processoRepository.findById(41L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(advogado);
        doNothing().when(authorizationService).requireWriteProcesso(processo);

        SubmissionRequest request = new SubmissionRequest(
                "APRESENTAR_REPLICA",
                "Réplica sem procuração",
                "Manifestação sem envelope representativo suficiente.",
                null,
                null,
                false,
                "ICP_BRASIL",
                null,
                null,
                null,
                null,
                "MANDATO_AD_JUDICIA",
                false,
                false,
                null,
                false,
                false,
                null,
                null,
                null,
                false,
                null,
                null,
                List.of()
        );

        assertThrows(ErroDeValidacaoException.class, () -> service.protocolar(41L, request));
        verify(documentoRepository, never()).saveAll(any());
    }

    @Test
    void advocaciaPrivadaComRepresentacaoDeveProtocolarReplica() {
        Processo processo = processoBase(42L, FaseProcessual.CONHECIMENTO);
        Usuario advogado = usuarioBase(52L, TipoUsuario.ADVOGADO, "Advogado Regular");

        when(processoRepository.findById(42L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(advogado);
        doNothing().when(authorizationService).requireWriteProcesso(processo);
        when(entityManager.getReference(Processo.class, 42L)).thenReturn(processo);
        when(documentoRepository.existsByProcessoIdAndSha256(eq(42L), any())).thenReturn(false);
        when(documentoRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(processEventStore.append(eq(42L), eq(ProcessEventType.DOCUMENTS_BULK_ADDED), any())).thenReturn(ProcessEventEnvelope.builder().seq(101L).processoId(42L).eventType(ProcessEventType.DOCUMENTS_BULK_ADDED.name()).createdAt(Instant.now()).build());
        when(processEventStore.append(eq(42L), eq(ProcessEventType.MOVEMENT_RECORDED), any())).thenReturn(ProcessEventEnvelope.builder().seq(102L).processoId(42L).eventType(ProcessEventType.MOVEMENT_RECORDED.name()).createdAt(Instant.now()).build());
        when(workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(eq(42L), any(), eq(WorkItemStatus.CANCELADO))).thenReturn(Optional.empty());
        when(workItemRepository.save(any())).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(990L);
            return item;
        });

        SubmissionRequest request = new SubmissionRequest(
                "APRESENTAR_REPLICA",
                "Réplica regular",
                "Réplica protocolada com representação ativa e identificação profissional.",
                null,
                null,
                false,
                "ICP_BRASIL",
                null,
                null,
                "Prazo legal da réplica",
                null,
                "MANDATO_AD_JUDICIA",
                true,
                true,
                "OAB/CE 12345",
                false,
                false,
                null,
                null,
                null,
                false,
                null,
                null,
                List.of()
        );

        SubmissionResponse response = service.protocolar(42L, request);

        assertEquals("REGULAR", response.representacao().status());
        assertEquals("ABERTO_GOVERNADO", response.seguranca().classificacao());
        assertEquals("ORDINARIO_GOVERNADO", response.prazo().lane());
        assertEquals(990L, response.workItemRecepcaoId());
    }

    @Test
    void advocaciaPrivadaNaoPodeProtocolarEmSigiloMaximoSemCanalInstitucional() {
        Processo processo = processoBase(44L, FaseProcessual.RECURSAL);
        processo.setNivelSigilo(NivelSigilo.SIGILO_N4);
        Usuario advogado = usuarioBase(54L, TipoUsuario.ADVOGADO, "Advogado Sigiloso");

        when(processoRepository.findById(44L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(advogado);
        doNothing().when(authorizationService).requireWriteProcesso(processo);

        SubmissionRequest request = new SubmissionRequest(
                "APRESENTAR_RECURSO",
                "Recurso em sigilo máximo",
                "Peça recursal em ambiente de sigilo máximo sem canal institucional.",
                null,
                "SIGILO_N4",
                false,
                "ICP_BRASIL",
                "ADV123456789",
                null,
                null,
                null,
                "MANDATO_AD_JUDICIA",
                true,
                true,
                "OAB/CE 99999",
                false,
                false,
                null,
                null,
                null,
                true,
                null,
                null,
                List.of()
        );

        assertThrows(ErroDeValidacaoException.class, () -> service.protocolar(44L, request));
        verify(documentoRepository, never()).saveAll(any());
    }

    @Test
    void workspaceSigilosoDeveExporPosturaDeSegurancaReforcada() {
        Processo processo = processoBase(43L, FaseProcessual.RECURSAL);
        processo.setNivelSigilo(NivelSigilo.SIGILO_N4);
        Usuario procurador = usuarioBase(53L, TipoUsuario.PROCURADORIA_ESTADUAL, "Procurador Estadual");

        when(processoRepository.findById(43L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(procurador);
        doNothing().when(authorizationService).requireWriteProcesso(processo);
        when(documentoRepository.findRecentContextoByProcessoId(43L, 36)).thenReturn(List.of());
        when(workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(eq(43L), eq(53L), any(), eq(WorkItemStatus.CANCELADO), any()))
                .thenReturn(List.of());

        WorkspaceView workspace = service.workspace(43L);

        assertEquals("ALTA_RESTRICAO", workspace.seguranca().classificacao());
        assertTrue(workspace.seguranca().certificadoObrigatorio());
        assertTrue(workspace.seguranca().stepUpObrigatorio());
    }

    private static Processo processoBase(Long id, FaseProcessual fase) {
        Processo processo = new Processo();
        processo.setId(id);
        processo.setNumeroProcesso("0001234-56.2026.8.06.0001");
        processo.setNumeroUnificado("0001234-56.2026.8.06.0001");
        processo.setTribunal("TJCE");
        processo.setVara("1ª Vara Cível");
        processo.setComarca("Fortaleza");
        processo.setUf("CE");
        processo.setUnidadeJudiciariaCodigo("VARA-1");
        processo.setFaseAtual(fase);
        processo.setNivelSigilo(NivelSigilo.PUBLICO);
        processo.setParteAutoraNome("Autor Teste");
        processo.setParteReuNome("Réu Teste");
        return processo;
    }

    private static Usuario usuarioBase(Long id, TipoUsuario tipo, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoUsuario(tipo);
        usuario.setNome(nome);
        usuario.setEmail(nome.toLowerCase().replace(' ', '.') + "@pjb.test");
        usuario.setCpf(UUID.randomUUID().toString());
        return usuario;
    }
}
