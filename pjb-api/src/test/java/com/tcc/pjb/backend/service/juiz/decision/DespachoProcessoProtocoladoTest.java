package com.tcc.pjb.backend.service.juiz.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusCiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.judicial.DjePublicacao;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.model.entity.outbox.OutboxStatus;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.CienciaProcessualRepository;
import com.tcc.pjb.backend.model.repository.DjePublicacaoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.governance.DocumentTrustChainService;
import com.tcc.pjb.backend.service.juiz.session.GabineteOperationalProfileResolver;
import com.tcc.pjb.backend.service.juiz.session.GabineteSessionProfileResolver;
import com.tcc.pjb.backend.service.julgamento.safety.DecisionSafetyService;
import com.tcc.pjb.backend.service.juiz.guardrails.JuizProcessoGuardRailService;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingProfile;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingResolver;
import com.tcc.pjb.backend.service.processo.DespachoInicialContinuityOrchestratorService;
import com.tcc.pjb.backend.service.processual.pauta.PautaAudienciaNacionalService;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import com.tcc.pjb.backend.service.casefile.CaseContinuityDecisionGateService;
import com.tcc.pjb.backend.service.juiz.topology.JuizGabineteQueueIsolationService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.mockito.Mockito;

@DataJpaTest
@ActiveProfiles("test")
class DespachoProcessoProtocoladoTest {

    private JuizGabineteDecisionalService decisionalService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private DocumentoProcessualRepository documentoProcessualRepository;

    @Autowired
    private MovimentacaoProcessualRepository movimentacaoProcessualRepository;

    @Autowired
    private DjePublicacaoRepository djePublicacaoRepository;

    @Autowired
    private CienciaProcessualRepository cienciaProcessualRepository;

    @Autowired
    private PoloProcessualRepository poloProcessualRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private PerfilDashboardContextFactory contextFactory;
    private CurrentUserService currentUserService;
    private DecisionSafetyService decisionSafetyService;
    private JuizProcessoGuardRailService guardRailService;
    private JuizGabineteRoutingResolver juizGabineteRoutingResolver;
    private DespachoInicialContinuityOrchestratorService despachoInicialContinuityOrchestratorService;
    private QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;
    private DocumentTrustChainService documentTrustChainService;
    private PjbAuthorizationService authorizationService;
    private ProcessoLifecycleMachine lifecycleMachine;
    private DespachoComunicacaoPosAtoService despachoComunicacaoPosAtoService;

    @Autowired
    private WorkItemRepository workItemRepository;

    @BeforeEach
    void setUp() {
        contextFactory = Mockito.mock(PerfilDashboardContextFactory.class);
        currentUserService = Mockito.mock(CurrentUserService.class);
        decisionSafetyService = Mockito.mock(DecisionSafetyService.class);
        guardRailService = Mockito.mock(JuizProcessoGuardRailService.class);
        juizGabineteRoutingResolver = Mockito.mock(JuizGabineteRoutingResolver.class);
        despachoInicialContinuityOrchestratorService = Mockito.mock(DespachoInicialContinuityOrchestratorService.class);
        qualifiedDocumentSignatureEnvelopeService = Mockito.mock(QualifiedDocumentSignatureEnvelopeService.class);
        documentTrustChainService = Mockito.mock(DocumentTrustChainService.class);
        authorizationService = Mockito.mock(PjbAuthorizationService.class);
        lifecycleMachine = Mockito.mock(ProcessoLifecycleMachine.class);
        OfficialDocumentTemplateService templateService = new OfficialDocumentTemplateService(
                processoRepository,
                documentoProcessualRepository,
                currentUserService,
                authorizationService,
                documentTrustChainService,
                qualifiedDocumentSignatureEnvelopeService
        );
        despachoComunicacaoPosAtoService = new DespachoComunicacaoPosAtoService(
                djePublicacaoRepository,
                cienciaProcessualRepository,
                poloProcessualRepository,
                usuarioRepository,
                outboxEventRepository,
                new ObjectMapper()
        );
        decisionalService = new JuizGabineteDecisionalService(
                contextFactory,
                Mockito.mock(PainelServiceCommons.class),
                processoRepository,
                workItemRepository,
                authorizationService,
                lifecycleMachine,
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
                despachoComunicacaoPosAtoService
        );
        prepararAutorizacao();
        when(lifecycleMachine.apply(any(Processo.class), eq(ProcessoLifecycleAction.ASSINAR_DESPACHO))).thenAnswer(invocation -> {
            Processo processo = invocation.getArgument(0);
            processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
            processo.setDataUltimaMovimentacao(LocalDateTime.now());
            return null;
        });
    }

    @Test
    void juizProfereDespachoComDocumentoMovimentacaoEUltimaMovimentacaoPersistidos() {
        Usuario juiz = salvarUsuario(TipoUsuario.JUIZ_ESTADUAL);
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO);
        Usuario reu = salvarUsuario(TipoUsuario.CIDADAO);
        Processo processo = salvarProcesso();
        salvarPolo(processo, advogado, TipoPolo.ATIVO, TipoParte.AUTOR, 1);
        salvarPolo(processo, reu, TipoPolo.PASSIVO, TipoParte.REU, 2);
        prepararFluxoPermitido(juiz, processo);

        Map<String, Object> payload = decisionalService.assinarDespacho(
                processo.getId(),
                "Cite-se a parte requerida para contestar no prazo legal.",
                "CPC arts. 238 e 335"
        );

        Processo atualizado = processoRepository.findById(processo.getId()).orElseThrow();
        List<DocumentoProcessual> documentos = documentoProcessualRepository.findByProcessoId(processo.getId());
        List<MovimentacaoProcessual> movimentacoes = movimentacaoProcessualRepository.findTop60ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId());
        List<DjePublicacao> publicacoesDje = djePublicacaoRepository.findTop100ByProcessoIdOrderByCreatedAtDesc(processo.getId());
        List<CienciaProcessual> ciencias = cienciaProcessualRepository.findByProcessoIdAndStatus(processo.getId(), StatusCiencia.PENDENTE);
        List<OutboxEvent> outboxEvents = outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("Processo", String.valueOf(processo.getId()));

        assertThat(payload.get("status")).isEqualTo("ASSINADO");
        assertThat(payload.get("documentoAssinado")).isInstanceOf(Map.class);
        assertThat(payload.get("movimentacaoId")).isNotNull();
        assertThat(payload.get("comunicacaoPosDespacho")).isInstanceOf(Map.class);
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
        assertThat(atualizado.getDataUltimaMovimentacao()).isNotNull();
        assertThat(atualizado.getStatusProcesso()).isEqualTo(StatusProcesso.EM_ANDAMENTO);
        assertThat(publicacoesDje).hasSize(1);
        assertThat(publicacoesDje.getFirst().getTipoAto()).isEqualTo("DESPACHO");
        assertThat(publicacoesDje.getFirst().getStatus()).isEqualTo("PENDENTE_ENVIO");
        assertThat(publicacoesDje.getFirst().getPrazoComecaEm()).isNotNull();
        assertThat(ciencias).hasSize(2);
        assertThat(ciencias).allSatisfy(ciencia -> {
            assertThat(ciencia.getTipoCiencia()).isEqualTo(com.tcc.pjb.backend.model.entity.enums.TipoCienciaProcessual.INTIMACAO_DECISAO);
            assertThat(ciencia.getAtoProcessualId()).isEqualTo(payload.get("movimentacaoId"));
            assertThat(ciencia.getDataExpiracao()).isNotNull();
        });
        assertThat(ciencias).extracting(ciencia -> ciencia.getUsuario().getId()).containsExactlyInAnyOrder(advogado.getId(), reu.getId());
        assertThat(outboxEvents).extracting(OutboxEvent::getEventType)
                .contains("DJE_PUBLICACAO_SOLICITADA", "PRAZO_PROCESSUAL_CRIADO", "INTIMACAO_PROCESSUAL_CRIADA");
        assertThat(outboxEvents).allSatisfy(event -> assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING));
    }

    @Test
    void advogadoNaoConsegueProferirDespacho() {
        Usuario advogado = salvarUsuario(TipoUsuario.ADVOGADO);
        Processo processo = salvarProcesso();
        when(contextFactory.build()).thenReturn(contexto(advogado));

        long documentosAntes = documentoProcessualRepository.countByProcesso_Id(processo.getId());
        long movimentacoesAntes = movimentacaoProcessualRepository.findTop60ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId()).size();

        assertThatThrownBy(() -> decisionalService.assinarDespacho(processo.getId(), "Despacho indevido", "Sem competencia"))
                .isInstanceOf(AccessDeniedPjbException.class);

        assertThat(documentoProcessualRepository.countByProcesso_Id(processo.getId())).isEqualTo(documentosAntes);
        assertThat(movimentacaoProcessualRepository.findTop60ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId())).hasSize((int) movimentacoesAntes);
    }

    private void prepararFluxoPermitido(Usuario juiz, Processo processo) {
        when(contextFactory.build()).thenReturn(contexto(juiz));
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
        when(qualifiedDocumentSignatureEnvelopeService.signOfficialTemplate(any(), any(), eq(TemplateDocumentoOficial.DESPACHO), anyString(), anyString(), eq(true)))
                .thenAnswer(invocation -> {
                    String conteudo = invocation.getArgument(4);
                    return new QualifiedDocumentSignatureEnvelopeService.SignedContent(
                            conteudo,
                            Hashes.sha256Hex(conteudo),
                            Map.of("rubrica", "PJB-RUB-TESTE"),
                            Map.of("status", "VALIDO")
                    );
                });
    }

    private void prepararAutorizacao() {
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
                "GABINETE_TESTE",
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
                "SECRETARIA_TESTE",
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

    private Processo salvarProcesso() {
        Processo processo = new Processo();
        processo.setNumeroProcesso("0001234-56.2026.8.06.0001");
        processo.setNumeroUnificado(processo.getNumeroProcesso());
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setRamoDireito(com.tcc.pjb.backend.model.entity.enums.RamoDireito.CIVIL);
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setStatusProcesso(StatusProcesso.DISTRIBUIDO);
        processo.setNivelSigilo(NivelSigilo.PUBLICO);
        processo.setClasseProcessual("Procedimento Comum Civel");
        processo.setAssunto("Cobranca contratual");
        processo.setParteAutoraNome("Autora Despacho");
        processo.setParteReuNome("Reu Despacho");
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        processo.setTribunal("TJCE");
        processo.setVara("1 Vara Civel de Fortaleza");
        processo.setUnidadeJudiciariaCodigo("0001");
        processo.setTribunalCodigoRoteado("06");
        processo.setDataCriacao(LocalDateTime.now());
        processo.setDataDistribuicao(LocalDateTime.now());
        return processoRepository.saveAndFlush(processo);
    }

    private Usuario salvarUsuario(TipoUsuario tipo) {
        long unique = Math.abs(System.nanoTime() % 800000L) + 100000L;
        Usuario usuario = new Usuario();
        usuario.setNome(tipo == TipoUsuario.ADVOGADO ? "Advogado Despacho" : "Juiz Despacho");
        usuario.setEmail(tipo.name().toLowerCase(java.util.Locale.ROOT) + ".despacho." + System.nanoTime() + "@test.local");
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
        return usuarioRepository.save(usuario);
    }

    private PoloProcessual salvarPolo(Processo processo, Usuario usuario, TipoPolo tipoPolo, TipoParte tipoParte, int ordem) {
        return poloProcessualRepository.save(new PoloProcessual(
                processo.getId(),
                tipoPolo,
                tipoParte,
                usuario.getNome(),
                usuario.getCpf(),
                "CPF",
                usuario.getOabNumero(),
                usuario.getOabUf(),
                usuario.getId(),
                usuario.getIdentidadeJuridicaId(),
                null,
                ordem
        ));
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
