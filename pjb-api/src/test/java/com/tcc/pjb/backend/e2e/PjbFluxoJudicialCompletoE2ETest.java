package com.tcc.pjb.backend.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.NationalRecursalMeshEngine;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.domain.valueobject.NumeroProcesso;
import com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa;
import com.tcc.pjb.backend.integration.oab.OabValidationClient;
import com.tcc.pjb.backend.integration.oab.OabValidationResult;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoCommand;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoResult;
import com.tcc.pjb.backend.model.dto.publico.PublicProcessoConsultaResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.ProcessoDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.competencia.StatusDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusCiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.TipoCienciaProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.judicial.DjePublicacao;
import com.tcc.pjb.backend.model.entity.judicial.MniRemessa;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.model.entity.outbox.OutboxStatus;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.CienciaProcessualRepository;
import com.tcc.pjb.backend.model.repository.DjePublicacaoRepository;
import com.tcc.pjb.backend.model.repository.MniRemessaRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoDistribuicaoCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalAggregateStateRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalTransitionLedgerRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import com.tcc.pjb.backend.service.casefile.CaseContinuityDecisionGateService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.governance.DocumentTrustChainService;
import com.tcc.pjb.backend.service.juiz.decision.DespachoComunicacaoPosAtoService;
import com.tcc.pjb.backend.service.juiz.decision.JuizGabineteDecisionalService;
import com.tcc.pjb.backend.service.juiz.guardrails.JuizProcessoGuardRailService;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingProfile;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingResolver;
import com.tcc.pjb.backend.service.juiz.session.GabineteOperationalProfileResolver;
import com.tcc.pjb.backend.service.juiz.session.GabineteSessionProfileResolver;
import com.tcc.pjb.backend.service.juiz.topology.JuizGabineteQueueIsolationService;
import com.tcc.pjb.backend.service.julgamento.safety.DecisionSafetyService;
import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import com.tcc.pjb.backend.service.processo.DespachoInicialContinuityOrchestratorService;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import com.tcc.pjb.backend.service.processual.pauta.PautaAudienciaNacionalService;
import com.tcc.pjb.backend.service.processual.recursal.RecursalFluxoMinimoPersistenciaService;
import com.tcc.pjb.backend.service.processual.recursal.RecursalMeshBundleService;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoFacadeService;
import com.tcc.pjb.backend.service.processual.recursal.RecursalValidacaoMinimaService;
import com.tcc.pjb.backend.service.processual.recursal.formalizacao.RecursalFormalizacaoService;
import com.tcc.pjb.backend.service.processual.recursal.ia.RecursalIaConferenciaService;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalOperationalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalSigiloGovernanceService;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalDraftPreviewAssembler;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalProjectionAssembler;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import com.tcc.pjb.backend.service.processual.surface.ProcessualOperationalSurfaceFacadeService;
import com.tcc.pjb.backend.service.publico.PublicProcessoConsultaService;
import com.tcc.pjb.backend.service.recursal.RecursalFactIdempotentIngestService;
import com.tcc.pjb.backend.service.recursal.RecursalIntelligenceFacadeService;
import com.tcc.pjb.backend.service.recursal.mesh.NationalRecursalMeshService;
import com.tcc.pjb.backend.service.recursal.mesh.RecursalMeshRequestMapper;
import com.tcc.pjb.backend.service.recurso.RecursoAdmissibilidadeService;
import com.tcc.pjb.backend.service.recurso.RecursoTempestividadeGuardService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.scheduling.enabled=false",
        "pjb.workflow.enabled=false",
        "pjb.outbox.ingress.enabled=false",
        "pjb.integrations.oab.warn-on-indeterminate-allowed=false"
})
class PjbFluxoJudicialCompletoE2ETest extends PjbIntegrationTestBase {

    @Autowired
    private LaianePeticaoInicialDraftService peticaoInicialDraftService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private PoloProcessualRepository poloProcessualRepository;

    @Autowired
    private DocumentoProcessualRepository documentoProcessualRepository;

    @Autowired
    private DocumentoPaginaRepository documentoPaginaRepository;

    @Autowired
    private PublicProcessoConsultaService publicProcessoConsultaService;

    @Autowired
    private ProcessoDistribuicaoCompetenciaRepository distribuicaoCompetenciaRepository;

    @Autowired
    private UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository;

    @Autowired
    private MovimentacaoProcessualRepository movimentacaoProcessualRepository;

    @Autowired
    private DjePublicacaoRepository djePublicacaoRepository;

    @Autowired
    private CienciaProcessualRepository cienciaProcessualRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private RecursalAggregateStateRepository aggregateRepository;

    @Autowired
    private RecursalProcessIntegrationStateRepository projectionRepository;

    @Autowired
    private RecursalTransitionLedgerRepository ledgerRepository;

    @Autowired
    private MniRemessaRepository mniRemessaRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private OabValidationClient oabValidationClient;

    @MockitoBean
    private TriagemNacionalIAEngine triagemNacionalIAEngine;

    private JuizGabineteDecisionalService decisionalService;
    private RecursalPeticionamentoFacadeService recursalFacadeService;
    private PerfilDashboardContextFactory juizContextFactory;
    private PerfilDashboardContextFactory recursalContextFactory;
    private DecisionSafetyService decisionSafetyService;
    private JuizProcessoGuardRailService guardRailService;
    private JuizGabineteRoutingResolver juizGabineteRoutingResolver;
    private DespachoInicialContinuityOrchestratorService despachoInicialContinuityOrchestratorService;
    private QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;
    private ProcessoLifecycleMachine despachoLifecycleMachine;
    private ProcessoLifecycleMachine recursalLifecycleMachine;

    @BeforeEach
    void setUp() {
        configurarDespacho();
        configurarRecursal();
    }

    @Test
    void fluxoJudicialCompletoPercorrePeticionamentoDistribuicaoDespachoERecurso() {
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO, "Advogada E2E");
        when(currentUserService.getRequired()).thenReturn(advogado);
        when(oabValidationClient.validate(any(), same(advogado))).thenReturn(OabValidationResult.apto("test"));

        LaianePeticaoInicialDraftService.DraftView draft = peticaoInicialDraftService.salvar(new LaianePeticaoInicialDraftService.EstruturarRequest(
                null,
                "Cobranca contratual integral",
                "Maria E2e",
                "Empresa E2e Ltda",
                "CIVIL",
                "COMUM_ORDINARIO",
                "ACAO_DE_COBRANCA",
                List.of("Contrato inadimplido em Fortaleza"),
                List.of("Obrigacao contratual vencida"),
                List.of("Condenacao ao pagamento"),
                List.of("Contrato assinado"),
                BigDecimal.valueOf(18250),
                false,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        LaianePeticaoInicialDraftService.ProtocolarResult protocolo = peticaoInicialDraftService.protocolar(
                draft.id(),
                new LaianePeticaoInicialDraftService.ProtocolarRequest("ESTADUAL")
        );

        Processo processo = processoRepository.findById(protocolo.processoId()).orElseThrow();
        validarPeticionamento(processo, advogado, draft);
        validarConsultaPublica(processo);
        validarConsultaSigilosa(processo);
        ProcessoDistribuicaoCompetencia distribuicao = validarDistribuicao(processo);

        Usuario juiz = salvarUsuario(TipoUsuario.JUIZ_ESTADUAL, "Juiz E2E");
        prepararFluxoDespacho(juiz, processo);
        Map<String, Object> despacho = decisionalService.assinarDespacho(
                processo.getId(),
                "Cite-se a parte requerida para contestar no prazo legal.",
                "CPC arts. 238 e 335"
        );
        validarDespachoComunicacao(processo.getId(), juiz, advogado, despacho);

        Map<String, Object> sentenca = decisionalService.proferirSentenca(
                processo.getId(),
                "Julgo parcialmente procedente o pedido para condenar a parte requerida ao pagamento do valor reconhecido.",
                "CPC arts. 487, I, e 1.009",
                "PARCIAL_PROCEDENCIA"
        );
        validarSentencaRecorrivel(processo.getId(), juiz, sentenca);

        Processo processoAposSentenca = processoRepository.findById(processo.getId()).orElseThrow();
        prepararFluxoRecursal(advogado);
        Map<String, Object> recurso = recursalFacadeService.interporRecurso(
                processoAposSentenca.getId(),
                "APELACAO",
                "Razoes de apelacao com impugnacao especifica da sentenca.",
                "CPC art. 1009",
                false,
                false,
                "sentenca recorrida"
        );
        validarRecurso(processoAposSentenca.getId(), recurso, distribuicao);
        validarConsultaFinalSemVazamento(processoAposSentenca.getNumeroProcesso());
    }

    private void validarPeticionamento(Processo processo, Usuario advogado, LaianePeticaoInicialDraftService.DraftView draft) {
        assertThat(processo.getId()).isNotNull();
        assertThat(processo.getUsuario().getId()).isEqualTo(advogado.getId());
        assertThat(processo.getNumeroProcesso()).matches("\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}");
        assertThat(NumeroProcesso.validar(processo.getNumeroProcesso())).isTrue();
        assertThat(processo.getNumeroUnificado()).isEqualTo(processo.getNumeroProcesso());
        assertThat(processo.getParteAutoraNome()).isEqualTo("Maria E2e");
        assertThat(processo.getParteReuNome()).isEqualTo("Empresa E2e Ltda");

        List<PoloProcessual> polos = poloProcessualRepository.findByProcessoIdAndAtivo(processo.getId(), true);
        assertThat(polos).hasSize(2);
        assertThat(polos).anySatisfy(polo -> {
            assertThat(polo.getTipoPolo()).isEqualTo(TipoPolo.ATIVO);
            assertThat(polo.getTipoParte()).isEqualTo(TipoParte.AUTOR);
            assertThat(polo.getUsuarioId()).isEqualTo(advogado.getId());
            assertThat(polo.getOabNumero()).isNotBlank();
            assertThat(polo.getOabUf()).isEqualTo("CE");
        });
        assertThat(polos).anySatisfy(polo -> {
            assertThat(polo.getTipoPolo()).isEqualTo(TipoPolo.PASSIVO);
            assertThat(polo.getTipoParte()).isEqualTo(TipoParte.REU);
            assertThat(polo.getNomeCompleto()).isEqualTo("Empresa E2e Ltda");
        });

        DocumentoProcessual recibo = documentoProcessualRepository.findByProcessoId(processo.getId()).stream()
                .filter(documento -> documento.getTitulo() != null && documento.getTitulo().startsWith("Recibo de protocolo"))
                .findFirst()
                .orElseThrow();
        assertThat(recibo.getSha256()).hasSize(64);
        assertThat(recibo.getSha384()).hasSize(96);
        assertThat(recibo.getProtocoloExterno()).isEqualTo(processo.getConnectorProtocolReference());
        assertThat(recibo.getEstadoOperacional()).isEqualTo("RECIBO_PROTOCOLO_EMITIDO");
        List<DocumentoPagina> paginas = documentoPaginaRepository.findByDocumentoId(recibo.getId());
        assertThat(paginas).hasSize(1);
        assertThat(paginas.getFirst().getTextoExtraido()).contains(
                "Numero CNJ: " + processo.getNumeroProcesso(),
                "Tipo de peticao: PETICAO_INICIAL",
                "Parte autora: Maria E2e",
                "Parte re: Empresa E2e Ltda",
                "Hash do conteudo: " + draft.hashIntegridade(),
                "Status: PROTOCOLO_REALIZADO"
        );
    }

    private void validarConsultaPublica(Processo processo) {
        PublicProcessoConsultaResponse consultaFormatada = publicProcessoConsultaService.consultarPorNumero(processo.getNumeroProcesso());
        PublicProcessoConsultaResponse consultaSemMascara = publicProcessoConsultaService.consultarPorNumero(processo.getNumeroProcesso().replaceAll("\\D", ""));

        assertThat(consultaFormatada.processoId()).isEqualTo(processo.getId());
        assertThat(consultaSemMascara.processoId()).isEqualTo(processo.getId());
        assertThat(consultaFormatada.numero()).isEqualTo(processo.getNumeroProcesso());
        assertThat(consultaFormatada.acessoRestrito()).isFalse();
        assertThat(consultaFormatada.partes().parteAutora()).isEqualTo("Maria E2e");
        assertThat(consultaFormatada.partes().parteReu()).isEqualTo("Empresa E2e Ltda");
        assertThat(consultaFormatada.documentos()).isEmpty();
    }

    private void validarConsultaSigilosa(Processo processo) {
        processo.setNivelSigilo(NivelSigilo.SIGILO_N2);
        processoRepository.saveAndFlush(processo);

        PublicProcessoConsultaResponse consulta = publicProcessoConsultaService.consultarPorNumero(processo.getNumeroProcesso());

        assertThat(consulta.processoId()).isEqualTo(processo.getId());
        assertThat(consulta.acessoRestrito()).isTrue();
        assertThat(consulta.partes()).isNull();
        assertThat(consulta.movimentacoes()).isEmpty();
        assertThat(consulta.documentos()).isEmpty();
        assertThat(consulta.aviso()).isNotBlank();
        assertThat(consulta.orientacaoAcesso()).isNotBlank();

        processo.setNivelSigilo(NivelSigilo.PUBLICO);
        processoRepository.saveAndFlush(processo);
    }

    private ProcessoDistribuicaoCompetencia validarDistribuicao(Processo processo) {
        ProcessoDistribuicaoCompetencia distribuicao = distribuicaoCompetenciaRepository.findTopByProcesso_IdOrderByIdDesc(processo.getId()).orElseThrow();
        Processo distribuido = processoRepository.findById(processo.getId()).orElseThrow();
        UnidadeJudiciariaCompetencia unidade = unidadeJudiciariaCompetenciaRepository.findById(distribuicao.getUnidade().getId()).orElseThrow();

        assertThat(distribuicao.getProcesso().getId()).isEqualTo(processo.getId());
        assertThat(distribuicao.getUnidade()).isNotNull();
        assertThat(unidade.getCodigo()).isNotBlank();
        assertThat(distribuicao.getCriadoEm()).isNotNull();
        assertThat(distribuicao.getMotivacao()).contains("score=");
        assertThat(distribuicao.getStatus()).isEqualTo(StatusDistribuicaoCompetencia.APLICADA);
        assertThat(distribuicao.isDistribuicaoAutomatica()).isTrue();
        assertThat(distribuido.getTipoJustica()).isEqualTo(TipoJustica.ESTADUAL);
        assertThat(unidade.getTipoJustica()).isEqualTo(TipoJustica.ESTADUAL);
        assertThat(unidade.getTribunalCodigo()).isEqualTo("TJCE");
        assertThat(distribuido.getDataDistribuicao()).isNotNull();
        assertThat(distribuido.getUnidadeJudiciariaCodigo()).isNotBlank();
        assertThat(distribuido.getVara()).isNotBlank();
        assertThat(distribuido.getTribunalCodigoRoteado()).isEqualTo("TJCE");
        return distribuicao;
    }

    private void validarDespachoComunicacao(Long processoId, Usuario juiz, Usuario advogado, Map<String, Object> despacho) {
        Processo processo = processoRepository.findById(processoId).orElseThrow();
        List<DocumentoProcessual> documentos = documentoProcessualRepository.findByProcessoId(processoId);
        List<MovimentacaoProcessual> movimentacoes = movimentacaoProcessualRepository.findTop60ByProcesso_IdOrderByDataMovimentacaoDesc(processoId);
        List<DjePublicacao> publicacoesDje = djePublicacaoRepository.findTop100ByProcessoIdOrderByCreatedAtDesc(processoId);
        List<CienciaProcessual> ciencias = cienciaProcessualRepository.findByProcessoIdAndStatus(processoId, StatusCiencia.PENDENTE);
        List<OutboxEvent> outboxEvents = outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("Processo", String.valueOf(processoId));

        assertThat(despacho.get("status")).isEqualTo("ASSINADO");
        assertThat(despacho.get("documentoAssinado")).isInstanceOf(Map.class);
        assertThat(despacho.get("movimentacaoId")).isNotNull();
        assertThat(despacho.get("comunicacaoPosDespacho")).isInstanceOf(Map.class);
        assertThat(documentos).anySatisfy(documento -> {
            assertThat(documento.getTitulo()).contains("Despacho judicial");
            assertThat(documento.getSha256()).hasSize(64);
            assertThat(documento.getPdf()).isNotEmpty();
            assertThat(documento.getCriadoPor()).isEqualTo(juiz.getId());
        });
        assertThat(movimentacoes).anySatisfy(movimentacao -> {
            assertThat(movimentacao.getAtor().getId()).isEqualTo(juiz.getId());
            assertThat(movimentacao.getFaseDe()).isEqualTo(FaseProcessual.CONHECIMENTO);
            assertThat(movimentacao.getFasePara()).isEqualTo(FaseProcessual.CONHECIMENTO);
            assertThat(movimentacao.getDescricao()).contains("Despacho judicial proferido");
        });
        assertThat(processo.getDataUltimaMovimentacao()).isNotNull();
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.EM_ANDAMENTO);
        assertThat(publicacoesDje).hasSize(1);
        assertThat(publicacoesDje.getFirst().getTipoAto()).isEqualTo("DESPACHO");
        assertThat(publicacoesDje.getFirst().getStatus()).isEqualTo("PENDENTE_ENVIO");
        assertThat(publicacoesDje.getFirst().getPrazoComecaEm()).isNotNull();
        assertThat(ciencias).isNotEmpty();
        assertThat(ciencias).allSatisfy(ciencia -> {
            assertThat(ciencia.getTipoCiencia()).isEqualTo(TipoCienciaProcessual.INTIMACAO_DECISAO);
            assertThat(ciencia.getAtoProcessualId()).isEqualTo(despacho.get("movimentacaoId"));
            assertThat(ciencia.getDataExpiracao()).isNotNull();
        });
        assertThat(ciencias).anySatisfy(ciencia -> assertThat(ciencia.getUsuario().getId()).isEqualTo(advogado.getId()));
        assertThat(outboxEvents).extracting(OutboxEvent::getEventType)
                .contains("DJE_PUBLICACAO_SOLICITADA", "PRAZO_PROCESSUAL_CRIADO", "INTIMACAO_PROCESSUAL_CRIADA");
        assertThat(outboxEvents).allSatisfy(event -> assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING));
    }

    private void validarSentencaRecorrivel(Long processoId, Usuario juiz, Map<String, Object> sentenca) {
        Processo processo = processoRepository.findById(processoId).orElseThrow();
        List<DocumentoProcessual> documentos = documentoProcessualRepository.findByProcessoId(processoId);
        List<WorkItem> workItems = workItemRepository.findAllByProcesso(processoId);

        assertThat(sentenca.get("status")).isEqualTo("SENTENCA_PROFERIDA");
        assertThat(sentenca.get("documentoAssinado")).isInstanceOf(Map.class);
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.SENTENCA_PROFERIDA);
        assertThat(processo.getFaseAtual()).isEqualTo(FaseProcessual.JULGAMENTO);
        assertThat(processo.getResultadoFinal()).contains("parcialmente procedente");
        assertThat(processo.getDataUltimaMovimentacao()).isNotNull();
        assertThat(workItems).extracting(WorkItem::getType).contains(WorkItemType.SENTENCA);
        assertThat(documentos).anySatisfy(documento -> {
            assertThat(documento.getTitulo()).contains("Senten");
            assertThat(documento.getSha256()).hasSize(64);
            assertThat(documento.getCriadoPor()).isEqualTo(juiz.getId());
        });
    }

    private void validarRecurso(Long processoId, Map<String, Object> recurso, ProcessoDistribuicaoCompetencia distribuicao) {
        String recursoId = ((Map<?, ?>) recurso.get("fluxoRecursalPersistido")).get("recursoId").toString();
        String numeroRecursal = String.valueOf(recurso.get("numeroRecursal"));
        Processo processo = processoRepository.findById(processoId).orElseThrow();
        List<WorkItem> workItems = workItemRepository.findAllByProcesso(processoId);
        List<DocumentoProcessual> documentos = documentoProcessualRepository.findByProcessoId(processoId);
        DocumentoProcessual documentoRecursal = documentos.stream()
                .filter(documento -> numeroRecursal.equals(documento.getProtocoloExterno()))
                .findFirst()
                .orElseThrow();
        RecursalAggregateState aggregate = aggregateRepository.findById(recursoId).orElseThrow();
        List<MniRemessa> remessas = mniRemessaRepository.findAll().stream()
                .filter(remessa -> remessa.getProcessoId().equals(processoId))
                .toList();
        List<OutboxEvent> eventosRecursais = outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("RECURSO_PROCESSUAL", recursoId);

        assertThat(recurso.get("status")).isEqualTo("RECURSO_INTERPOSTO");
        assertThat(numeroRecursal).startsWith("REC-" + processoId);
        assertThat(recurso.get("admissibilidadeFormal")).isInstanceOf(Map.class);
        assertThat(workItems).extracting(WorkItem::getType).contains(WorkItemType.PETICAO, WorkItemType.RECURSO);
        assertThat(documentoRecursal.getSha256()).hasSize(64);
        assertThat(documentoRecursal.getSha384()).hasSize(96);
        assertThat(documentoRecursal.getEstadoOperacional()).isEqualTo("PECA_RECURSAL_PROTOCOLADA");
        assertThat(aggregate.getCurrentState()).isEqualTo(RecursalLifecycleState.INTERPOSTO);
        assertThat(projectionRepository.findTop50ByProcesso_IdOrderByUpdatedAtDesc(processoId)).isNotEmpty();
        assertThat(ledgerRepository.findTop100ByRecursoIdOrderByToRevisionDesc(recursoId)).isNotEmpty();
        assertThat(remessas).anySatisfy(remessa -> {
            assertThat(remessa.getStatus()).isEqualTo(MniStatusRemessa.PENDING);
            assertThat(remessa.getTribunalDestino()).isNotBlank();
            assertThat(remessa.getMotivo()).contains("RECURSO");
        });
        assertThat(eventosRecursais).extracting(OutboxEvent::getEventType)
                .contains("RECURSO_INTERPOSTO", "REMESSA_RECURSAL_SOLICITADA");
        assertThat(eventosRecursais).allSatisfy(event -> assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING));
        assertThat(processo.getFaseAtual()).isEqualTo(FaseProcessual.RECURSAL);
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.RECURSO_INTERPOSTO);
        assertThat(processo.getNumeroProcesso()).isNotBlank();
        assertThat(distribuicao.getProcesso().getId()).isEqualTo(processoId);
    }

    private void validarConsultaFinalSemVazamento(String numeroProcesso) {
        PublicProcessoConsultaResponse consulta = publicProcessoConsultaService.consultarPorNumero(numeroProcesso);

        assertThat(consulta.processoId()).isNotNull();
        assertThat(consulta.acessoRestrito()).isFalse();
        assertThat(consulta.documentos()).isEmpty();
    }

    private void configurarDespacho() {
        juizContextFactory = Mockito.mock(PerfilDashboardContextFactory.class);
        decisionSafetyService = Mockito.mock(DecisionSafetyService.class);
        guardRailService = Mockito.mock(JuizProcessoGuardRailService.class);
        juizGabineteRoutingResolver = Mockito.mock(JuizGabineteRoutingResolver.class);
        despachoInicialContinuityOrchestratorService = Mockito.mock(DespachoInicialContinuityOrchestratorService.class);
        qualifiedDocumentSignatureEnvelopeService = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);
        DocumentTrustChainService documentTrustChainService = Mockito.mock(DocumentTrustChainService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        despachoLifecycleMachine = Mockito.mock(ProcessoLifecycleMachine.class);
        prepararAutorizacaoJuiz(authorizationService);
        OfficialDocumentTemplateService templateService = new OfficialDocumentTemplateService(
                processoRepository,
                documentoProcessualRepository,
                currentUserService,
                authorizationService,
                documentTrustChainService,
                qualifiedDocumentSignatureEnvelopeService
        );
        DespachoComunicacaoPosAtoService comunicacaoPosAtoService = new DespachoComunicacaoPosAtoService(
                djePublicacaoRepository,
                cienciaProcessualRepository,
                poloProcessualRepository,
                usuarioRepository,
                outboxEventRepository,
                new ObjectMapper()
        );
        decisionalService = new JuizGabineteDecisionalService(
                juizContextFactory,
                Mockito.mock(PainelServiceCommons.class),
                processoRepository,
                workItemRepository,
                authorizationService,
                despachoLifecycleMachine,
                Mockito.mock(PautaAudienciaNacionalService.class),
                decisionSafetyService,
                Mockito.mock(CaseContinuityDecisionGateService.class),
                Mockito.mock(GabineteOperationalProfileResolver.class),
                Mockito.mock(GabineteSessionProfileResolver.class),
                Mockito.mock(RepresentacaoProcessualPolicyService.class),
                despachoInicialContinuityOrchestratorService,
                guardRailService,
                juizGabineteRoutingResolver,
                Mockito.mock(JuizGabineteQueueIsolationService.class),
                templateService,
                movimentacaoProcessualRepository,
                comunicacaoPosAtoService
        );
        when(despachoLifecycleMachine.apply(any(Processo.class), eq(ProcessoLifecycleAction.ASSINAR_DESPACHO))).thenAnswer(invocation -> {
            Processo processo = invocation.getArgument(0);
            processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
            processo.setDataUltimaMovimentacao(LocalDateTime.now());
            return null;
        });
        when(despachoLifecycleMachine.apply(any(Processo.class), eq(ProcessoLifecycleAction.PROFERIR_SENTENCA))).thenAnswer(invocation -> {
            Processo processo = invocation.getArgument(0);
            processo.setFaseAtual(FaseProcessual.JULGAMENTO);
            processo.setStatusProcesso(StatusProcesso.SENTENCA_PROFERIDA);
            processo.setResultadoFinal("Sentenca parcialmente procedente.");
            processo.setDataUltimaMovimentacao(LocalDateTime.now().minusDays(1));
            return null;
        });
    }

    private void configurarRecursal() {
        recursalContextFactory = Mockito.mock(PerfilDashboardContextFactory.class);
        recursalLifecycleMachine = Mockito.mock(ProcessoLifecycleMachine.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        doNothing().when(authorizationService).requireRole(any(), any(String[].class));
        doNothing().when(authorizationService).requireReadProcesso(any());
        RecursalMeshRequestMapper meshRequestMapper = new RecursalMeshRequestMapper();
        NationalRecursalMeshService recursalMeshService = Mockito.mock(NationalRecursalMeshService.class);
        when(recursalMeshService.plan(any(RecursalMeshPlanRequest.class))).thenAnswer(invocation -> {
            RecursalMeshPlanRequest request = invocation.getArgument(0);
            return new NationalRecursalMeshEngine().plan(
                    meshRequestMapper.toContext(request.context()),
                    meshRequestMapper.toSpecies(request.species()),
                    request.recursoId()
            );
        });
        RecursalProjectionAssembler projectionAssembler = Mockito.mock(RecursalProjectionAssembler.class);
        prepararProjectionAssembler(projectionAssembler);
        RecursalFormalizacaoService formalizacaoService = Mockito.mock(RecursalFormalizacaoService.class);
        when(formalizacaoService.formalizar(Mockito.<RecursalFormalizacaoCommand>any())).thenReturn(new RecursalFormalizacaoResult(
                true,
                "FORMALIZACAO_RECURSAL_PREPARADA",
                Map.of("conteudoMinuta", "Peca recursal formalizada para validacao ponta a ponta."),
                null,
                Map.of(),
                Map.of(),
                Map.of("modo", "ASSINATURA_CONTROLADA"),
                Map.of("canal", "INTERNO"),
                Map.of(),
                Map.of()
        ));
        RecursalSigiloGovernanceService sigiloGovernanceService = Mockito.mock(RecursalSigiloGovernanceService.class);
        when(sigiloGovernanceService.avaliar(any(), any(), any(), any(), any(), any(), any())).thenReturn(Map.of());
        RecursalOperationalAutomationService automationService = Mockito.mock(RecursalOperationalAutomationService.class);
        when(automationService.materialize(any(), any(), any(), any(), any())).thenReturn(Map.of("status", "REGISTRADA"));
        RecursalValidacaoMinimaService validacaoMinimaService = new RecursalValidacaoMinimaService(
                new RecursoAdmissibilidadeService(),
                new RecursoTempestividadeGuardService(new CalendarioUteisService()),
                workItemRepository
        );
        RecursalFluxoMinimoPersistenciaService persistenciaService = new RecursalFluxoMinimoPersistenciaService(
                documentoProcessualRepository,
                aggregateRepository,
                projectionRepository,
                ledgerRepository,
                mniRemessaRepository,
                processoRepository,
                outboxEventRepository,
                new ObjectMapper(),
                meshRequestMapper
        );
        RecursalMeshBundleService recursalMeshBundleService = new RecursalMeshBundleService(
                recursalMeshService,
                Mockito.mock(ProcessualOperationalSurfaceFacadeService.class),
                Mockito.mock(RecursalIaConferenciaService.class)
        );
        recursalFacadeService = new RecursalPeticionamentoFacadeService(
                recursalContextFactory,
                processoRepository,
                workItemRepository,
                authorizationService,
                recursalLifecycleMachine,
                Mockito.mock(PainelServiceCommons.class),
                Mockito.mock(ProcessoRecursalApplicationService.class),
                recursalMeshBundleService,
                Mockito.mock(RecursalDraftPreviewAssembler.class),
                Mockito.mock(RecursalIntelligenceFacadeService.class),
                Mockito.mock(RecursalFactIdempotentIngestService.class),
                formalizacaoService,
                sigiloGovernanceService,
                projectionAssembler,
                automationService,
                validacaoMinimaService,
                persistenciaService
        );
        when(recursalLifecycleMachine.apply(any(Processo.class), eq(ProcessoLifecycleAction.INTERPOR_RECURSO))).thenAnswer(invocation -> {
            Processo processo = invocation.getArgument(0);
            processo.setFaseAtual(FaseProcessual.RECURSAL);
            processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
            processo.setDataUltimaMovimentacao(LocalDateTime.now());
            return null;
        });
    }

    private void prepararFluxoDespacho(Usuario juiz, Processo processo) {
        when(juizContextFactory.build()).thenReturn(contexto(juiz));
        when(currentUserService.getRequired()).thenReturn(juiz);
        when(currentUserService.getOrNull()).thenReturn(juiz);
        when(currentUserService.currentUser()).thenReturn(Optional.of(juiz));
        when(currentUserService.currentUserIdOrZero()).thenReturn(juiz.getId());
        when(decisionSafetyService.registrarConferenciaCruzadaSeNecessario(any(), any(), anyString(), anyString())).thenReturn(Optional.empty());
        when(guardRailService.requireAtuacaoPermitida(any(), any(), anyString())).thenReturn(guardRail(processo));
        when(juizGabineteRoutingResolver.resolve(any())).thenReturn(gabineteRouting());
        when(despachoInicialContinuityOrchestratorService.onDespachoInicialAssinado(any(), any(), anyString(), anyString()))
                .thenReturn(new DespachoInicialContinuityOrchestratorService.InitialDispatchFollowUpResponse(
                        "CITACAO_INICIAL",
                        null,
                        0,
                        List.of("Despacho inicial apto a gerar citacao."),
                        List.of()
                ));
        when(qualifiedDocumentSignatureEnvelopeService.signOfficialTemplate(any(), any(), any(TemplateDocumentoOficial.class), anyString(), anyString(), eq(true)))
                .thenAnswer(invocation -> {
                    String conteudo = invocation.getArgument(4);
                    return new QualifiedDocumentSignatureEnvelopeService.SignedContent(
                            conteudo,
                            Hashes.sha256Hex(conteudo),
                            Map.of("rubrica", "PJB-RUB-E2E"),
                            Map.of("status", "VALIDO")
                    );
                });
    }

    private void prepararFluxoRecursal(Usuario advogado) {
        when(recursalContextFactory.build()).thenReturn(contexto(advogado));
    }

    private void prepararAutorizacaoJuiz(PjbAuthorizationService authorizationService) {
        doAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            if (usuario == null || usuario.getTipoUsuario() == TipoUsuario.ADVOGADO) {
                throw new AccessDeniedPjbException("Acesso negado ao perfil informado.");
            }
            Object[] args = invocation.getArguments();
            boolean permitido = false;
            for (int i = 1; i < args.length; i++) {
                String role = String.valueOf(args[i]);
                permitido = permitido
                        || role.equals("ROLE_JUIZ")
                        || role.equals("ROLE_MAGISTRADO")
                        || role.equals("ROLE_JUIZ_ESTADUAL")
                        || role.equals("ROLE_JUIZ_FEDERAL");
            }
            if (!permitido) {
                throw new AccessDeniedPjbException("Acesso negado ao perfil informado.");
            }
            return null;
        }).when(authorizationService).requireRole(any(), any(String[].class));
    }

    private void prepararProjectionAssembler(RecursalProjectionAssembler projectionAssembler) {
        when(projectionAssembler.buildEndpoints(anyLong())).thenReturn(new LinkedHashMap<>());
        when(projectionAssembler.buildStrategy(any(), any(), Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(new LinkedHashMap<>());
        when(projectionAssembler.buildWorkspaceProjection(anyLong(), any(), any())).thenReturn(new LinkedHashMap<>());
        when(projectionAssembler.buildAssistedFilingProjection(any(), any(), any(), Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(Map.of());
        when(projectionAssembler.buildDistributionProjection(any(), any())).thenReturn(new LinkedHashMap<>());
        when(projectionAssembler.buildDecisionCarryOverProjection(any(), any(), any())).thenReturn(Map.of());
        when(projectionAssembler.buildGraphProjection(any(), anyLong())).thenReturn(new LinkedHashMap<>());
        when(projectionAssembler.resolveTargetInstanceHint(any(), any())).thenReturn(InstanceLevel.SECOND_INSTANCE);
        when(projectionAssembler.resolveTargetCourtHint(any(), any(), any())).thenReturn("TJCE");
        when(projectionAssembler.inferAutosApartadosLikely(any(), any())).thenReturn(false);
        when(projectionAssembler.buildEscalonamentoNotes(any(), any(), any(), any())).thenReturn("interposicao recursal registrada");
    }

    private PerfilDashboardContext contexto(Usuario usuario) {
        return new PerfilDashboardContext(
                usuario,
                null,
                LocalDateTime.now(),
                usuario.getTipoUsuario().name(),
                usuario.getNome(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                List.of(),
                null
        );
    }

    private JuizProcessoGuardRailService.GuardRailSnapshot guardRail(Processo processo) {
        return new JuizProcessoGuardRailService.GuardRailSnapshot(
                processo.getId(),
                processo.getNumeroProcesso(),
                "ASSINAR_DESPACHO",
                true,
                "LIBERADO",
                secretariaRouting(),
                List.of(),
                List.of("Atuacao judicial permitida."),
                Map.of("allowed", true)
        );
    }

    private JuizGabineteRoutingProfile gabineteRouting() {
        return new JuizGabineteRoutingProfile(
                "GABINETE_E2E",
                "GABINETE_1VARA_CIVEL",
                "GAB:CE:FORTALEZA:1VARA",
                "ASSESSORIA_1VARA_CIVEL",
                "AUDIENCIA_1VARA_CIVEL",
                "COORD_1VARA_CIVEL",
                "REDIST_1VARA_CIVEL",
                "GABINETE_SINGULAR",
                "TJCE/Fortaleza/1 Vara Civel",
                Duration.ofHours(8),
                List.of("PRIMEIRO_GRAU", "CIVIL", "ESTADUAL"),
                Map.of(),
                null,
                secretariaRouting()
        );
    }

    private SecretariatOperationalRoutingProfile secretariaRouting() {
        return new SecretariatOperationalRoutingProfile(
                "SECRETARIA_E2E",
                "ESTADUAL",
                "TJCE",
                "PRIMEIRO_GRAU",
                "COMUM",
                "CIVIL",
                "VARA_CIVEL",
                "SEC_1VARA_CIVEL",
                "SECRETARIA_RECEPCAO",
                "SEC:RECEPCAO",
                "SECRETARIA_SANEAMENTO",
                "SEC:SANEAMENTO",
                "SECRETARIA_AUDIENCIA",
                "SEC:AUDIENCIA",
                "SECRETARIA_JUNTADA",
                "SEC:JUNTADA",
                "SALA_FORUM",
                "TJCE/Fortaleza/1 Vara Civel",
                Duration.ofHours(1),
                Duration.ofHours(2),
                Duration.ofHours(24),
                60,
                true,
                true,
                false,
                false,
                List.of(),
                List.of(),
                null,
                null,
                Map.of()
        );
    }

    private Usuario salvarUsuario(TipoUsuario tipo, String nome) {
        long unique = Math.abs(System.nanoTime() % 800000L) + 100000L;
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(tipo.name().toLowerCase(java.util.Locale.ROOT) + ".e2e." + System.nanoTime() + "@test.local");
        usuario.setCpf(cpfValido(Math.toIntExact(unique)));
        usuario.setTipoUsuario(tipo);
        usuario.setPerfil(tipo.name());
        usuario.setAtivo(true);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        if (tipo == TipoUsuario.ADVOGADO) {
            usuario.setOab("OAB/CE " + unique);
            usuario.setOabUf("CE");
            usuario.setOabNormalizada("CE-" + unique);
        }
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String cpfValido(int base) {
        String raiz = String.format("%09d", base);
        int primeiro = digitoCpf(raiz);
        int segundo = digitoCpf(raiz + primeiro);
        return raiz + primeiro + segundo;
    }

    private int digitoCpf(String valor) {
        int peso = valor.length() + 1;
        int soma = 0;
        for (int i = 0; i < valor.length(); i++) {
            soma += Character.digit(valor.charAt(i), 10) * (peso - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
