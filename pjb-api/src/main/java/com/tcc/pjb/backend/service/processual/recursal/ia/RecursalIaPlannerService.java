package com.tcc.pjb.backend.service.processual.recursal.ia;

import com.tcc.pjb.backend.ai.juridica.v3.core.LegalDraftingService;
import com.tcc.pjb.backend.ai.jurimetria.JurimetriaService;
import com.tcc.pjb.backend.ai.jurimetria.model.JurimetriaReport;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.jurisprudencia.PrecedentFoundationQueryRequest;
import com.tcc.pjb.backend.model.dto.jurisprudencia.PrecedentFoundationResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaStructuredAnalysis;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.judicial.TemaPrecedenteVinculante;
import com.tcc.pjb.backend.model.entity.judicial.TemaRecursoRepetitivo;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacao;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.TemaPrecedenteVinculanteRepository;
import com.tcc.pjb.backend.model.repository.TemaRecursoRepetitivoRepository;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.jurisprudencia.PrecedentFoundationCatalogService;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalSigiloGovernanceService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RecursalIaPlannerService {

    private final ProcessoRepository processoRepository;
    private final PeritoNomeacaoRepository peritoNomeacaoRepository;
    private final PjbAuthorizationService authorizationService;
    private final PrecedentFoundationCatalogService precedentFoundationCatalogService;
    private final JurimetriaService jurimetriaService;
    private final LegalDraftingService legalDraftingService;
    private final ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    private final TemaPrecedenteVinculanteRepository temaPrecedenteVinculanteRepository;
    private final TemaRecursoRepetitivoRepository temaRecursoRepetitivoRepository;
    private final RecursalSigiloGovernanceService recursalSigiloGovernanceService;
    private final PjbExecutionOrchestrator executionOrchestrator;
    private final Duration aiMeshTimeout;

    public RecursalIaPlannerService(ProcessoRepository processoRepository,
                                    PeritoNomeacaoRepository peritoNomeacaoRepository,
                                    PjbAuthorizationService authorizationService,
                                    PrecedentFoundationCatalogService precedentFoundationCatalogService,
                                    JurimetriaService jurimetriaService,
                                    LegalDraftingService legalDraftingService,
                                    ProcessoDocumentoApplicationService processoDocumentoApplicationService,
                                    TemaPrecedenteVinculanteRepository temaPrecedenteVinculanteRepository,
                                    TemaRecursoRepetitivoRepository temaRecursoRepetitivoRepository,
                                    RecursalSigiloGovernanceService recursalSigiloGovernanceService,
                                    PjbExecutionOrchestrator executionOrchestrator,
                                    @Value("${pjb.processual.recursal.ai-mesh.timeout:5s}") Duration aiMeshTimeout) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.peritoNomeacaoRepository = Objects.requireNonNull(peritoNomeacaoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.precedentFoundationCatalogService = Objects.requireNonNull(precedentFoundationCatalogService);
        this.jurimetriaService = Objects.requireNonNull(jurimetriaService);
        this.legalDraftingService = Objects.requireNonNull(legalDraftingService);
        this.processoDocumentoApplicationService = Objects.requireNonNull(processoDocumentoApplicationService);
        this.temaPrecedenteVinculanteRepository = Objects.requireNonNull(temaPrecedenteVinculanteRepository);
        this.temaRecursoRepetitivoRepository = Objects.requireNonNull(temaRecursoRepetitivoRepository);
        this.recursalSigiloGovernanceService = Objects.requireNonNull(recursalSigiloGovernanceService);
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator);
        this.aiMeshTimeout = Objects.requireNonNull(aiMeshTimeout);
    }

    public RecursalIaStructuredAnalysis analyze(RecursalIaConferenciaRequest command,
                                                RecursalAdmissibilityResponse admissibility) {
        Objects.requireNonNull(command, "command");
        RecursalAdmissibilityRequest request = Objects.requireNonNull(command.admissibilidade(), "admissibilidade");
        Processo processo = loadProcesso(command.processoId());
        List<PeritoNomeacao> pericias = processo == null || !command.considerarHistoricoPericial()
                ? List.of()
                : peritoNomeacaoRepository.findTop200ByProcesso_IdOrderByNomeadoEmDesc(processo.getId());
        LegalAppealType appealType = resolveAppealType(command, request);
        RamoDireito ramoEfetivo = resolveRamo(processo, command);
        RitoProcessual ritoEfetivo = resolveRito(processo, command);
        Map<String, Object> contextoCaso = buildContextoCaso(processo, request, admissibility, pericias, appealType, ramoEfetivo, ritoEfetivo);
        Map<String, Object> classificacaoMaterial = buildClassificacaoMaterial(processo, request, admissibility, appealType, ramoEfetivo, ritoEfetivo);
        List<String> riscosAnulacao = buildRiscosAnulacao(processo, command, request, admissibility, pericias, appealType, ramoEfetivo, ritoEfetivo);
        List<String> checklistBlindagem = buildChecklistBlindagem(command, request, admissibility, pericias, appealType, ramoEfetivo, ritoEfetivo);
        List<String> fundamentosEstruturais = buildFundamentosEstruturais(processo, request, admissibility, appealType, ramoEfetivo, ritoEfetivo);
        List<String> tesesPrioritarias = buildTesesPrioritarias(processo, request, admissibility, appealType, ramoEfetivo, ritoEfetivo);
        CompletableFuture<Map<String, Object>> jurisprudenciaFuture = supplyAsyncMap("recursal-jurisprudencia", command.aprofundarJurisprudencia(), () -> buildJurisprudencia(command, processo, appealType, ramoEfetivo, ritoEfetivo));
        CompletableFuture<Map<String, Object>> jurimetriaFuture = supplyAsyncMap("recursal-jurimetria", command.aprofundarJurimetria(), () -> buildJurimetria(command, processo, appealType, ramoEfetivo, ritoEfetivo));
        CompletableFuture<Map<String, Object>> precedentesFuture = supplyAsyncMap("recursal-precedentes", true, () -> buildPrecedentesQualificados(command, processo, appealType, ramoEfetivo, ritoEfetivo));
        ProcessoDocumentoAggregate documentoAggregate = safeDocumentoAggregate(processo);
        Map<String, Object> jurisprudencia = resolveFuture("jurisprudencia", jurisprudenciaFuture);
        Map<String, Object> jurimetria = resolveFuture("jurimetria", jurimetriaFuture);
        Map<String, Object> precedentesQualificados = resolveFuture("precedentes_qualificados", precedentesFuture);
        Map<String, Object> blueprintRecursal = buildBlueprintRecursal(processo, request, admissibility, appealType, ramoEfetivo, ritoEfetivo, checklistBlindagem, tesesPrioritarias);
        Map<String, Object> memoriaProcessual = buildMemoriaProcessual(command, processo, pericias);
        Map<String, Object> sigiloRecursal = recursalSigiloGovernanceService.avaliar(processo, appealType, command.pedidoUsuario(), null, command.pedidoUsuario(), admissibility, null);
        Map<String, Object> contrarrazoes = buildContrarrazoes(admissibility, appealType, ramoEfetivo, ritoEfetivo, fundamentosEstruturais, tesesPrioritarias);
        Map<String, Object> embargosEspecializados = buildEmbargosEspecializados(command, processo, appealType, admissibility, pericias, checklistBlindagem);
        Map<String, Object> assinaturaRecursal = buildAssinaturaRecursal(processo, admissibility, documentoAggregate, appealType, blueprintRecursal);
        Map<String, Object> protocoloExterno = buildProtocoloExterno(processo, admissibility, appealType, documentoAggregate, precedentesQualificados);
        return new RecursalIaStructuredAnalysis(
                contextoCaso,
                classificacaoMaterial,
                riscosAnulacao,
                checklistBlindagem,
                fundamentosEstruturais,
                tesesPrioritarias,
                jurisprudencia,
                jurimetria,
                blueprintRecursal,
                memoriaProcessual,
                sigiloRecursal,
                contrarrazoes,
                embargosEspecializados,
                assinaturaRecursal,
                protocoloExterno,
                precedentesQualificados
        );
    }

    private CompletableFuture<Map<String, Object>> supplyAsyncMap(String operationName, boolean enabled, java.util.function.Supplier<Map<String, Object>> supplier) {
        if (!enabled) {
            return CompletableFuture.completedFuture(Map.of("status", "SKIPPED"));
        }
        return executionOrchestrator.supply(PjbExecutionDescriptor.burst(operationName, aiMeshTimeout), () -> {
            try {
                return supplier.get();
            } catch (RuntimeException ex) {
                return statusPayload("DEGRADED", null, safeMessage(ex));
            }
        }).exceptionally(ex -> statusPayload("DEGRADED", null, safeMessage(ex)));
    }

    private Map<String, Object> resolveFuture(String channel, CompletableFuture<Map<String, Object>> future) {
        if (future == null) {
            return Map.of("status", "SKIPPED");
        }
        try {
            return future.get(aiMeshTimeout.plusMillis(250).toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return statusPayload("DEGRADED", channel, safeMessage(ex));
        } catch (TimeoutException ex) {
            future.cancel(true);
            return statusPayload("TIMEOUT", channel, "timeout controlado na malha recursal");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            return statusPayload("DEGRADED", channel, safeMessage(cause));
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            return statusPayload("DEGRADED", channel, safeMessage(cause));
        }
    }


    private Map<String, Object> statusPayload(String status, String channel, String error) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", status);
        if (channel != null && !channel.isBlank()) {
            out.put("canal", channel);
        }
        if (error != null && !error.isBlank()) {
            out.put("erro", error);
        }
        return Collections.unmodifiableMap(out);
    }

    private Processo loadProcesso(Long processoId) {
        if (processoId == null) {
            return null;
        }
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return processo;
    }

    private LegalAppealType resolveAppealType(RecursalIaConferenciaRequest command, RecursalAdmissibilityRequest request) {
        String raw = firstNonBlank(command.tipoRecursoInformado(), request.recursoId());
        return LegalAppealType.fromString(raw);
    }

    private RamoDireito resolveRamo(Processo processo, RecursalIaConferenciaRequest command) {
        if (processo != null && processo.getRamoDireito() != null) {
            return processo.getRamoDireito();
        }
        if (command.ramoSugerido() != null) {
            RamoDireito ramo = RamoDireito.fromString(command.ramoSugerido());
            if (ramo != null) {
                return ramo;
            }
        }
        String base = String.join(" ", List.of(
                safe(command.pedidoUsuario()),
                processo != null ? safe(processo.getAssunto()) : "",
                processo != null ? safe(processo.getObjetoProcessual()) : "",
                processo != null ? safe(processo.getPedidoPrincipal()) : "",
                processo != null ? safe(processo.getPeticaoInicialText()) : "",
                processo != null ? safe(processo.getAnaliseTriagemV1()) : ""
        )).toUpperCase(Locale.ROOT);
        if (containsAny(base, "TRABALH", "CLT", "RESCISAO", "VERBAS RESCISORIAS", "HORAS EXTRAS", "ADICIONAL")) {
            return RamoDireito.TRABALHISTA;
        }
        if (containsAny(base, "PREVIDENCI", "INSS", "BPC", "LOAS", "AUXILIO", "APOSENTADORIA", "PENSAO")) {
            return RamoDireito.PREVIDENCIARIO;
        }
        if (containsAny(base, "TRIBUT", "ICMS", "IPI", "ISS", "IPTU", "EXECUCAO FISCAL", "CDA")) {
            return RamoDireito.TRIBUTARIO;
        }
        if (containsAny(base, "PENAL", "CRIME", "DENUNCIA", "PRONUNCIA", "DOSIMETRIA", "HABEAS")) {
            return RamoDireito.PENAL;
        }
        if (containsAny(base, "ELEITORAL", "CANDIDAT", "AIRC", "AIJE", "INELEGIBILIDADE")) {
            return RamoDireito.ELEITORAL;
        }
        if (containsAny(base, "MILITAR", "CASERNA", "CPM", "IPM")) {
            return RamoDireito.MILITAR;
        }
        if (containsAny(base, "AMBIENT", "IBAMA", "LICENCIAMENTO", "UNIDADE DE CONSERVACAO")) {
            return RamoDireito.AMBIENTAL;
        }
        if (containsAny(base, "CONSUMIDOR", "CDC", "VICIO DO PRODUTO", "PLANO DE SAUDE", "COBRANCA INDEVIDA")) {
            return RamoDireito.CONSUMIDOR;
        }
        if (containsAny(base, "FAMILIA", "ALIMENTOS", "GUARDA", "DIVORCIO", "INVENTARIO")) {
            return RamoDireito.FAMILIA;
        }
        if (containsAny(base, "SERVIDOR", "CONCURSO", "ATO ADMINISTRATIVO", "IMPROBIDADE")) {
            return RamoDireito.ADMINISTRATIVO;
        }
        if (containsAny(base, "AGRARIO", "POSSE RURAL", "DESAPROPRIACAO", "TERRA")) {
            return RamoDireito.AGRARIO;
        }
        return RamoDireito.CIVIL;
    }

    private RitoProcessual resolveRito(Processo processo, RecursalIaConferenciaRequest command) {
        if (processo != null && processo.getRito() != null) {
            return processo.getRito();
        }
        if (command.ritoSugerido() != null && !command.ritoSugerido().isBlank()) {
            return RitoProcessual.fromString(command.ritoSugerido());
        }
        RamoDireito ramo = resolveRamo(processo, command);
        return switch (ramo) {
            case PENAL -> RitoProcessual.PROCEDIMENTO_PENAL_COMUM;
            case TRABALHISTA -> RitoProcessual.TRABALHISTA_ORDINARIO;
            case PREVIDENCIARIO -> RitoProcessual.PREVIDENCIARIO_COMUM;
            case TRIBUTARIO -> RitoProcessual.TRIBUTARIO_DECLARATORIA;
            case ELEITORAL -> RitoProcessual.ELEITORAL;
            case MILITAR -> RitoProcessual.MILITAR_PROCESSO_PENAL_MILITAR;
            case AMBIENTAL -> RitoProcessual.AMBIENTAL_ACP;
            case ADMINISTRATIVO, CONSTITUCIONAL -> RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO;
            case FAMILIA -> RitoProcessual.CIVIL_FAMILIA_ALIMENTOS;
            case CONSUMIDOR -> RitoProcessual.JUIZADO_ESPECIAL_CIVEL;
            default -> RitoProcessual.COMUM_ORDINARIO;
        };
    }

    private Map<String, Object> buildContextoCaso(Processo processo,
                                                  RecursalAdmissibilityRequest request,
                                                  RecursalAdmissibilityResponse admissibility,
                                                  List<PeritoNomeacao> pericias,
                                                  LegalAppealType appealType,
                                                  RamoDireito ramoEfetivo,
                                                  RitoProcessual ritoEfetivo) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        var context = request.context();
        var species = request.species();
        put(out, "processoId", processo != null ? processo.getId() : context == null ? null : context.processoId());
        put(out, "numeroProcesso", processo != null ? firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()) : context == null ? null : context.numeroProcesso());
        put(out, "ramoEfetivo", ramoEfetivo != null ? ramoEfetivo.name() : null);
        put(out, "ritoEfetivo", ritoEfetivo != null ? ritoEfetivo.name() : null);
        put(out, "faseAtual", processo != null && processo.getFaseAtual() != null ? processo.getFaseAtual().name() : context == null || context.fase() == null ? null : context.fase().name());
        put(out, "classeProcessual", processo != null ? processo.getClasseProcessual() : context == null ? null : context.classeProcessual());
        put(out, "assunto", processo != null ? processo.getAssunto() : null);
        put(out, "tipoRecurso", appealType.name());
        put(out, "speciesRecursal", species == null || species.type() == null ? null : species.type().name());
        put(out, "tribunalCodigo", request.tribunalCodigo());
        put(out, "instanciaAtual", context == null || context.instanciaAtual() == null ? null : context.instanciaAtual().name());
        put(out, "orgaoProlator", context == null || context.orgaoProlator() == null ? null : context.orgaoProlator().name());
        put(out, "periciaAtiva", !pericias.isEmpty());
        put(out, "quantidadeNomeacoesPericiais", pericias.size());
        if (admissibility != null) {
            put(out, "tribunalDestino", admissibility.tribunalDestino());
            put(out, "autoridadeJulgamento", admissibility.autoridadeJulgamento());
            put(out, "perfilRecursal", admissibility.perfilRecursal());
            put(out, "routeKind", admissibility.routeKind());
            put(out, "riskLevel", admissibility.riskLevel());
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildClassificacaoMaterial(Processo processo,
                                                           RecursalAdmissibilityRequest request,
                                                           RecursalAdmissibilityResponse admissibility,
                                                           LegalAppealType appealType,
                                                           RamoDireito ramoEfetivo,
                                                           RitoProcessual ritoEfetivo) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        put(out, "ramoDominante", ramoEfetivo != null ? ramoEfetivo.name() : null);
        put(out, "verticalPrincipal", ramoEfetivo != null ? ramoEfetivo.verticalPrincipal() : null);
        put(out, "ritoDominante", ritoEfetivo != null ? ritoEfetivo.name() : null);
        var context = request.context();
        put(out, "classeProcessual", processo != null ? processo.getClasseProcessual() : context == null ? null : context.classeProcessual());
        put(out, "tipoJustica", context == null || context.tipoJustica() == null ? null : context.tipoJustica().name());
        put(out, "familiaRecursal", context == null || context.classFamily() == null ? null : context.classFamily().name());
        put(out, "tipoRecursoCanonico", appealType.name());
        put(out, "recursoExcepcional", appealType.isExceptional());
        put(out, "incidenteExecutivo", appealType.isExecutoryIncident());
        put(out, "revisaoInterna", appealType.isInternalReview());
        put(out, "materiaConstitucional", context == null ? null : context.materiaConstitucional());
        put(out, "materiaFederalInfraconstitucional", context == null ? null : context.materiaFederalInfraconstitucional());
        put(out, "fazendaPublicaOuMp", context == null ? null : context.fazendaPublicaOuMp());
        put(out, "justicaGratuitaOuIsencaoLegal", context == null ? null : context.justicaGratuitaOuIsencaoLegal());
        if (admissibility != null) {
            put(out, "competenceHint", admissibility.competenceHint());
            put(out, "preventionMode", admissibility.preventionMode());
        }
        return Collections.unmodifiableMap(out);
    }

    private List<String> buildRiscosAnulacao(Processo processo,
                                             RecursalIaConferenciaRequest command,
                                             RecursalAdmissibilityRequest request,
                                             RecursalAdmissibilityResponse admissibility,
                                             List<PeritoNomeacao> pericias,
                                             LegalAppealType appealType,
                                             RamoDireito ramoEfetivo,
                                             RitoProcessual ritoEfetivo) {
        LinkedHashSet<String> riscos = new LinkedHashSet<>();
        if (admissibility != null && !admissibility.tempestivo()) {
            riscos.add("Tempestividade negativa ou não demonstrada com segurança material suficiente.");
        }
        if (admissibility != null && admissibility.preparoExigido() && !admissibility.preparoSatisfeito()) {
            riscos.add("Preparo exigido sem lastro suficiente de recolhimento ou de dispensa legal documentada.");
        }
        if (admissibility != null && admissibility.stepUpRequired()) {
            riscos.add("Fluxo exige step-up de credencial; protocolo sem reforço pode comprometer a submissão regular.");
        }
        if (admissibility != null && admissibility.certificateRequired()) {
            riscos.add("Canal ou perfil exige certificado/credencial reforçada para blindagem da prática recursal.");
        }
        if (processo != null && processo.getFaseAtual() != null && !isRecursalLike(processo.getFaseAtual()) && appealType != LegalAppealType.EMBARGOS_DECLARACAO) {
            riscos.add("Fase processual ainda não está claramente consolidada como recursal; validar marco impugnável e órgão prolator.");
        }
        if (command.exigirBlindagemAnulacao() && appealType == LegalAppealType.EMBARGOS_DECLARACAO && !mentionsEmbargosGround(command.pedidoUsuario())) {
            riscos.add("Embargos de declaração sem indicação nítida de omissão, contradição, obscuridade ou erro material tendem a sofrer rejeição ou multa.");
        }
        if (appealType.isExceptional()) {
            riscos.add("Recurso excepcional exige ataque técnico de admissibilidade reforçada, com foco em prequestionamento, filtro constitucional/legal e precedente qualificado.");
        }
        if (pericias.isEmpty() && requiresPericialAttention(ramoEfetivo, ritoEfetivo, processo)) {
            riscos.add("Há indicativo de matéria sensível a prova técnica; a ausência de enfrentamento pericial ou de impugnação técnica pode fragilizar o recurso.");
        }
        if (processo != null && blank(processo.getPeticaoInicialText()) && command.aprofundarBaseProcessual()) {
            riscos.add("Petição inicial sem lastro textual consolidado para retroalimentar a coerência da tese recursal.");
        }
        if (processo != null && blank(processo.getMaterialProbatorioResumo()) && pericias.isEmpty()) {
            riscos.add("Material probatório resumido escasso; recurso pode parecer dissociado das provas efetivamente produzidas.");
        }
        return List.copyOf(riscos);
    }

    private List<String> buildChecklistBlindagem(RecursalIaConferenciaRequest command,
                                                 RecursalAdmissibilityRequest request,
                                                 RecursalAdmissibilityResponse admissibility,
                                                 List<PeritoNomeacao> pericias,
                                                 LegalAppealType appealType,
                                                 RamoDireito ramoEfetivo,
                                                 RitoProcessual ritoEfetivo) {
        LinkedHashSet<String> itens = new LinkedHashSet<>();
        itens.add("Confirmar decisão recorrida, órgão prolator, data da intimação e data real do protocolo recursal.");
        itens.add("Conferir se a espécie recursal escolhida corresponde ao tipo de pronunciamento impugnado e à fase processual efetiva.");
        itens.add("Vincular decisão recorrida, certidão/intimação, procuração, substabelecimento e documentos indispensáveis no mesmo pacote recursal.");
        if (command.exigirConferenciaCompetencia()) {
            itens.add("Verificar competência, tribunal de destino, prevenção e eventual regimento local aplicável ao recurso.");
        }
        if (command.exigirConferenciaTempestividade()) {
            itens.add("Reconstituir contagem do prazo com feriados locais, suspensão processual, ciência eletrônica e hipótese de interrupção legal.");
        }
        if (command.exigirConferenciaPreparo()) {
            itens.add("Checar preparo, gratuidade, isenção legal, complementação e prova material do recolhimento ou da dispensa.");
        }
        if (!pericias.isEmpty()) {
            itens.add("Enfrentar laudo pericial, metodologia, cadeia de custódia, respostas aos quesitos e eventuais inconsistências técnicas da prova.");
        }
        if (appealType.isExceptional()) {
            itens.add("Demonstrar prequestionamento, distinguishing/superação quando houver precedente qualificado e adequação ao filtro excepcional correspondente.");
        }
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            itens.add("Explicitar fundamento integrativo específico dos embargos e evitar rediscussão genérica do mérito sem base integrativa.");
        }
        if (ramoEfetivo != null && ramoEfetivo.isPenalLike()) {
            itens.add("Revisar nulidades, contraditório, cadeia probatória, dosimetria, correlação acusação-sentença e garantias processuais penais pertinentes.");
        }
        if (ramoEfetivo == RamoDireito.TRABALHISTA) {
            itens.add("Revisar transcendência, delimitação de matérias/valores, ônus probatório e alinhamento com CLT, súmulas e OJs aplicáveis.");
        }
        if (ramoEfetivo == RamoDireito.PREVIDENCIARIO) {
            itens.add("Revisar DIB/DER, qualidade de segurado, incapacidade, carência, laudo e especialidade do benefício em relação ao pedido inicial.");
        }
        if (ramoEfetivo == RamoDireito.TRIBUTARIO || (ritoEfetivo != null && ritoEfetivo.isTribFazenda())) {
            itens.add("Revisar CDA, constituição do crédito, prescrição/decadência, base de cálculo e regime de suspensão da exigibilidade.");
        }
        if (admissibility != null && admissibility.automaticSuspensiveEffect()) {
            itens.add("Demonstrar de forma limpa o regime do efeito suspensivo e qualquer pedido cautelar recursal associado.");
        }
        return List.copyOf(itens);
    }

    private List<String> buildFundamentosEstruturais(Processo processo,
                                                     RecursalAdmissibilityRequest request,
                                                     RecursalAdmissibilityResponse admissibility,
                                                     LegalAppealType appealType,
                                                     RamoDireito ramoEfetivo,
                                                     RitoProcessual ritoEfetivo) {
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("A peça recursal deve preservar congruência entre petição inicial, prova produzida, decisão recorrida e pedidos recursais.");
        fundamentos.add("A IA deve confrontar cabimento, tempestividade, preparo, preclusão, competência, órgão julgador e risco de nulidade antes de sugerir a peça final.");
        fundamentos.add("O raciocínio recursal precisa separar erro de procedimento, erro de julgamento, matéria probatória, precedente aplicável e efeito pretendido.");
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            fundamentos.add("Embargos exigem aderência ao fundamento integrativo específico e não devem servir como sucedâneo recursal genérico.");
        }
        if (appealType.isExceptional()) {
            fundamentos.add("Recursos excepcionais exigem camada extra de filtragem: precedente qualificado, prequestionamento e demonstração técnica do cabimento extraordinário.");
        }
        if (ramoEfetivo == RamoDireito.TRABALHISTA) {
            fundamentos.add("Na seara trabalhista, a IA recursal deve cruzar CLT, súmulas, OJs, transcendência e delimitação de matérias/valores antes da minuta.");
        }
        if (ramoEfetivo == RamoDireito.PREVIDENCIARIO) {
            fundamentos.add("Na seara previdenciária, a IA precisa alinhar petição inicial, DER/DIB, laudo, incapacidade, carência e espécie de benefício à tese recursal.");
        }
        if (ramoEfetivo != null && ramoEfetivo.isPenalLike()) {
            fundamentos.add("Nos ritos penais e afins, a IA deve tratar nulidades, prova, dosimetria, correlação e garantias fundamentais como camada obrigatória de revisão.");
        }
        if (admissibility != null && "HIGH".equalsIgnoreCase(admissibility.riskLevel())) {
            fundamentos.add("Risco operacional elevado exige revisão humana obrigatória antes do protocolo efetivo.");
        }
        if (processo != null && !blank(processo.getAnaliseTriagemV1())) {
            fundamentos.add("A triagem inicial do processo deve ser reaproveitada como memória institucional para verificar eventual mudança de tese ou de classificação material.");
        }
        return List.copyOf(fundamentos);
    }

    private List<String> buildTesesPrioritarias(Processo processo,
                                                RecursalAdmissibilityRequest request,
                                                RecursalAdmissibilityResponse admissibility,
                                                LegalAppealType appealType,
                                                RamoDireito ramoEfetivo,
                                                RitoProcessual ritoEfetivo) {
        LinkedHashSet<String> teses = new LinkedHashSet<>();
        teses.add("Cabimento e adequação da espécie recursal ao pronunciamento atacado.");
        teses.add("Tempestividade demonstrada com base material de intimação e protocolo.");
        teses.add("Coerência entre narrativa fática original, prova produzida e pedido recursal.");
        if (admissibility != null && admissibility.preparoExigido()) {
            teses.add(admissibility.preparoDispensado()
                    ? "Dispensa de preparo juridicamente demonstrada e documentalmente sustentada."
                    : "Satisfação do preparo ou tese de complementação/dispensa validada no caso concreto.");
        }
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            teses.add("Identificação objetiva do vício integrativo e da utilidade concreta do provimento aclaratório.");
        }
        if (appealType.isExceptional()) {
            teses.add("Demonstração do filtro excepcional cabível: prequestionamento, ofensa normativa qualificada, repercussão ou dissídio, conforme a espécie.");
        }
        if (ramoEfetivo == RamoDireito.TRABALHISTA) {
            teses.add("Correção da distribuição do ônus probatório e aderência a súmulas/OJs aplicáveis ao ponto recursal.");
        }
        if (ramoEfetivo == RamoDireito.PREVIDENCIARIO || (ritoEfetivo != null && ritoEfetivo.isPrevidenciario())) {
            teses.add("Alinhamento entre incapacidade, qualidade de segurado, carência, perícia e espécie do benefício discutido.");
        }
        if (ramoEfetivo == RamoDireito.TRIBUTARIO || (ritoEfetivo != null && ritoEfetivo.isTribFazenda())) {
            teses.add("Conferência de prescrição, decadência, liquidez do título e legalidade da constituição do crédito.");
        }
        if (ramoEfetivo != null && ramoEfetivo.isPenalLike()) {
            teses.add("Nulidades, licitude/força da prova, dosimetria e correlação entre acusação, fundamentação e dispositivo.");
        }
        if (processo != null && !blank(processo.getResultadoFinal())) {
            teses.add("Enfrentamento preciso dos fundamentos determinantes da decisão recorrida, sem descolar do resultado efetivamente lançado nos autos.");
        }
        return List.copyOf(teses);
    }

    private Map<String, Object> buildJurisprudencia(RecursalIaConferenciaRequest command,
                                                    Processo processo,
                                                    LegalAppealType appealType,
                                                    RamoDireito ramoEfetivo,
                                                    RitoProcessual ritoEfetivo) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (!command.aprofundarJurisprudencia()) {
            out.put("status", "SKIPPED");
            out.put("motivo", "jurisprudencia_nao_solicitada");
            return Collections.unmodifiableMap(out);
        }
        try {
            String query = buildJurisprudenceQuery(processo, appealType, ramoEfetivo, ritoEfetivo, command.pedidoUsuario());
            PrecedentFoundationResponse response = precedentFoundationCatalogService.search(new PrecedentFoundationQueryRequest(
                    processo != null ? processo.getId() : null,
                    null,
                    null,
                    ramoEfetivo,
                    ritoEfetivo != null ? ritoEfetivo.name() : null,
                    query,
                    0,
                    5
            ));
            out.put("status", "READY");
            put(out, "queryEfetiva", response.queryEfetiva());
            out.put("totalResultados", response.totalResultados());
            if (!response.fundamentos().isEmpty()) {
                out.put("fundamentos", response.fundamentos());
            }
            if (!response.porFonte().isEmpty()) {
                out.put("porFonte", response.porFonte());
            }
            if (!response.porTipo().isEmpty()) {
                out.put("porTipo", response.porTipo());
            }
            out.put("precedentes", response.precedentes().stream().limit(5).map(item -> {
                LinkedHashMap<String, Object> precedent = new LinkedHashMap<>();
                put(precedent, "id", item.id());
                put(precedent, "fonte", item.fonte());
                put(precedent, "tipo", item.tipo());
                put(precedent, "identificador", item.identificador());
                put(precedent, "titulo", item.titulo());
                put(precedent, "tese", item.tese());
                put(precedent, "ementaResumo", item.ementaResumo());
                put(precedent, "dataPublicacao", item.dataPublicacao());
                put(precedent, "urlReferencia", item.urlReferencia());
                put(precedent, "ramoSugerido", item.ramoSugerido());
                put(precedent, "ritoSugerido", item.ritoSugerido());
                return Map.copyOf(precedent);
            }).toList());
            return Collections.unmodifiableMap(out);
        } catch (RuntimeException ex) {
            out.put("status", "DEGRADED");
            out.put("erro", safeMessage(ex));
            return Collections.unmodifiableMap(out);
        }
    }

    private Map<String, Object> buildJurimetria(RecursalIaConferenciaRequest command,
                                                Processo processo,
                                                LegalAppealType appealType,
                                                RamoDireito ramoEfetivo,
                                                RitoProcessual ritoEfetivo) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (!command.aprofundarJurimetria()) {
            out.put("status", "SKIPPED");
            out.put("motivo", "jurimetria_nao_solicitada");
            return Collections.unmodifiableMap(out);
        }
        try {
            LinkedHashMap<String, Object> filtros = new LinkedHashMap<>();
            put(filtros, "ramo", ramoEfetivo != null ? ramoEfetivo.name() : null);
            put(filtros, "rito", ritoEfetivo != null ? ritoEfetivo.name() : null);
            String tese = buildJurisprudenceQuery(processo, appealType, ramoEfetivo, ritoEfetivo, command.pedidoUsuario());
            JurimetriaReport report = jurimetriaService.gerarRelatorio(
                    tese,
                    processo != null ? processo.getTribunal() : null,
                    processo != null ? processo.getClasseProcessual() : null,
                    processo != null ? processo.getAssunto() : null,
                    filtros
            );
            out.put("status", "READY");
            put(out, "tese", report.getTese());
            put(out, "tribunal", report.getTribunal());
            put(out, "classe", report.getClasse());
            put(out, "assunto", report.getAssunto());
            put(out, "explicacao", report.getExplicacao());
            out.put("observacoes", report.getObservacoes() == null ? List.of() : report.getObservacoes());
            out.put("indicadores", report.getIndicadores() == null ? List.of() : report.getIndicadores().stream().limit(8).map(indicador -> {
                LinkedHashMap<String, Object> item = new LinkedHashMap<>();
                put(item, "nome", indicador.getNome());
                put(item, "valor", indicador.getValor());
                put(item, "unidade", indicador.getUnidade());
                return Map.copyOf(item);
            }).toList());
            return Collections.unmodifiableMap(out);
        } catch (RuntimeException ex) {
            out.put("status", "DEGRADED");
            out.put("erro", safeMessage(ex));
            return Collections.unmodifiableMap(out);
        }
    }

    private Map<String, Object> buildBlueprintRecursal(Processo processo,
                                                       RecursalAdmissibilityRequest request,
                                                       RecursalAdmissibilityResponse admissibility,
                                                       LegalAppealType appealType,
                                                       RamoDireito ramoEfetivo,
                                                       RitoProcessual ritoEfetivo,
                                                       List<String> checklistBlindagem,
                                                       List<String> tesesPrioritarias) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        List<String> sectionOrder = buildSectionOrder(appealType, ramoEfetivo);
        out.put("sectionOrder", sectionOrder);
        out.put("controlPoints", checklistBlindagem.stream().limit(8).toList());
        out.put("tesesPrioritarias", tesesPrioritarias.stream().limit(6).toList());
        LinkedHashMap<String, Object> ctx = new LinkedHashMap<>();
        var context = request.context();
        put(ctx, "numero_processo", processo != null ? firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()) : context == null ? null : context.numeroProcesso());
        put(ctx, "autor", processo != null ? processo.getParteAutoraNome() : null);
        put(ctx, "reu", processo != null ? processo.getParteReuNome() : null);
        put(ctx, "tempestividade", buildTempestividadeResumo(request, admissibility));
        put(ctx, "decisao", processo != null ? firstNonBlank(processo.getResultadoFinal(), processo.getResumoIA()) : null);
        put(ctx, "fundamentos", String.join(" ", tesesPrioritarias));
        put(ctx, "pedidos", tesesPrioritarias.stream().limit(4).toList());
        put(ctx, "provas", checklistBlindagem.stream().filter(item -> item.toLowerCase(Locale.ROOT).contains("prova") || item.toLowerCase(Locale.ROOT).contains("laudo")).limit(4).toList());
        put(ctx, "local_data", "[LOCAL], [DATA]");
        put(ctx, "assinatura", "[ASSINATURA / OAB / PERFIL INSTITUCIONAL]");
        put(ctx, "pleading_blueprint", sectionOrder);
        String minutaBase = legalDraftingService.draftRecurso(ctx);
        put(out, "minutaBase", minutaBase);
        if (admissibility != null) {
            put(out, "effectMode", admissibility.effectMode());
            put(out, "counterReasonsMode", admissibility.counterReasonsMode());
            put(out, "reviewDesk", admissibility.reviewDesk());
        }
        put(out, "tipoRecurso", appealType.name());
        put(out, "ramo", ramoEfetivo != null ? ramoEfetivo.name() : null);
        put(out, "rito", ritoEfetivo != null ? ritoEfetivo.name() : null);
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildMemoriaProcessual(RecursalIaConferenciaRequest command,
                                                       Processo processo,
                                                       List<PeritoNomeacao> pericias) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (processo == null) {
            out.put("status", "NO_PROCESS_CONTEXT");
            return Collections.unmodifiableMap(out);
        }
        out.put("status", command.aprofundarBaseProcessual() ? "READY" : "LIGHT");
        put(out, "peticaoInicialBase", command.aprofundarBaseProcessual() ? snippet(processo.getPeticaoInicialText(), 1200) : snippet(processo.getPeticaoInicialText(), 350));
        put(out, "analiseTriagemInicial", snippet(processo.getAnaliseTriagemV1(), 700));
        put(out, "resumoIa", snippet(processo.getResumoIA(), 700));
        put(out, "materialProbatorio", snippet(processo.getMaterialProbatorioResumo(), 900));
        put(out, "pedidoPrincipal", snippet(processo.getPedidoPrincipal(), 500));
        put(out, "pedidosConsolidados", snippet(processo.getPedidosConsolidados(), 700));
        if (!pericias.isEmpty()) {
            out.put("pericias", pericias.stream().limit(5).map(pericia -> {
                LinkedHashMap<String, Object> item = new LinkedHashMap<>();
                put(item, "id", pericia.getId());
                put(item, "status", pericia.getStatus() != null ? pericia.getStatus().name() : null);
                put(item, "nomeadoEm", pericia.getNomeadoEm() != null ? pericia.getNomeadoEm().toString() : null);
                put(item, "observacao", snippet(pericia.getObservacao(), 280));
                return Map.copyOf(item);
            }).toList());
        }
        return Collections.unmodifiableMap(out);
    }

    private ProcessoDocumentoAggregate safeDocumentoAggregate(Processo processo) {
        if (processo == null || processo.getId() == null) {
            return null;
        }
        try {
            return processoDocumentoApplicationService.detalhar(processo.getId());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Map<String, Object> buildContrarrazoes(RecursalAdmissibilityResponse admissibility,
                                                    LegalAppealType appealType,
                                                    RamoDireito ramoEfetivo,
                                                    RitoProcessual ritoEfetivo,
                                                    List<String> fundamentosEstruturais,
                                                    List<String> tesesPrioritarias) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        String mode = admissibility == null ? null : admissibility.counterReasonsMode();
        boolean habilitado = mode != null && !mode.isBlank() && !"NONE".equalsIgnoreCase(mode) && !"NAO_APLICAVEL".equalsIgnoreCase(mode);
        out.put("status", habilitado ? "PLANEJADA" : "LATENTE");
        out.put("habilitada", habilitado);
        put(out, "mode", mode);
        put(out, "desk", admissibility == null ? null : admissibility.counterReasonsDesk());
        put(out, "sessionMode", admissibility == null ? null : admissibility.sessionMode());
        put(out, "routingBucket", admissibility == null ? null : admissibility.routingBucket());
        out.put("checklist", buildCounterReasonsChecklist(appealType, ramoEfetivo, ritoEfetivo));
        out.put("capitulosResposta", tesesPrioritarias.stream().limit(4).map(item -> "Responder capítulo: " + item).toList());
        out.put("blindagemDialetica", fundamentosEstruturais.stream().limit(4).toList());
        out.put("endpointSugerido", "/api/v1/processual/recursal/contrarrazoes");
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildEmbargosEspecializados(RecursalIaConferenciaRequest command,
                                                            Processo processo,
                                                            LegalAppealType appealType,
                                                            RecursalAdmissibilityResponse admissibility,
                                                            List<PeritoNomeacao> pericias,
                                                            List<String> checklistBlindagem) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        boolean embargosDeclaracao = appealType == LegalAppealType.EMBARGOS_DECLARACAO;
        boolean embargosTerceiro = appealType == LegalAppealType.EMBARGOS_TERCEIRO;
        boolean embargosInfringentes = appealType == LegalAppealType.EMBARGOS_INFRINGENTES;
        out.put("status", embargosDeclaracao || embargosTerceiro || embargosInfringentes ? "TIPO_ATIVO" : "ESTUDO_COMPLEMENTAR");
        out.put("embargosDeclaracao", embargosDeclaracao);
        out.put("embargosTerceiro", embargosTerceiro);
        out.put("embargosInfringentes", embargosInfringentes);
        out.put("fundamentosDetectados", detectEmbargosGrounds(command, processo, admissibility, pericias));
        out.put("efeitosPossiveis", detectEmbargosEffects(appealType, admissibility));
        out.put("checklist", checklistBlindagem.stream().filter(item -> item.toLowerCase(Locale.ROOT).contains("omiss") || item.toLowerCase(Locale.ROOT).contains("contradi") || item.toLowerCase(Locale.ROOT).contains("prova") || item.toLowerCase(Locale.ROOT).contains("laudo")).limit(6).toList());
        put(out, "endpointSugerido", embargosDeclaracao ? "/api/v1/processual/recursal/embargos-declaracao" : embargosTerceiro ? "/api/v1/processual/recursal/embargos-terceiro" : "/api/v1/processual/recursal/embargos");
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildAssinaturaRecursal(Processo processo,
                                                        RecursalAdmissibilityResponse admissibility,
                                                        ProcessoDocumentoAggregate documentoAggregate,
                                                        LegalAppealType appealType,
                                                        Map<String, Object> blueprintRecursal) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        boolean stepUp = admissibility != null && admissibility.stepUpRequired();
        boolean certificate = admissibility != null && admissibility.certificateRequired();
        String modo = certificate ? "CERTIFICADO_OU_CREDENCIAL_REFORCADA" : stepUp ? "STEP_UP_FORTE" : "ASSINATURA_ELETRONICA_CONTROLADA";
        out.put("status", "READY_FOR_SIGNATURE_PATH");
        out.put("signatureMode", modo);
        out.put("stepUpRequired", stepUp);
        out.put("certificateRequired", certificate);
        out.put("tipoRecurso", appealType.name());
        put(out, "govBrStepUpEndpoint", "/api/v1/auth/govbr/stepup/start");
        put(out, "passkeyStepUpEndpoint", "/api/v1/auth/stepup/options");
        put(out, "faceStepUpEndpoint", "/api/v1/magistratura/face/issue");
        if (documentoAggregate != null) {
            out.put("lotesAssinaveis", documentoAggregate.trilhaAssinavel());
            out.put("alertasDocumentais", documentoAggregate.alertas());
            out.put("documentosAssinados", documentoAggregate.assinados());
            out.put("documentosCustodiados", documentoAggregate.custodiados());
        }
        put(out, "resumoMinuta", snippet((String) blueprintRecursal.get("minutaBase"), 420));
        put(out, "processoNumero", processo == null ? null : firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildProtocoloExterno(Processo processo,
                                                      RecursalAdmissibilityResponse admissibility,
                                                      LegalAppealType appealType,
                                                      ProcessoDocumentoAggregate documentoAggregate,
                                                      Map<String, Object> precedentesQualificados) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        boolean readyForDryRun = admissibility != null && admissibility.tempestivo() && (!admissibility.preparoExigido() || admissibility.preparoSatisfeito() || admissibility.preparoDispensado());
        out.put("status", readyForDryRun ? "READY_FOR_DRY_RUN" : "REVIEW_REQUIRED_BEFORE_PROTOCOL");
        out.put("tipoRecurso", appealType.name());
        put(out, "connectorSystem", admissibility == null ? null : admissibility.connectorSystem());
        put(out, "integrationChannel", admissibility == null ? null : admissibility.integrationChannel());
        put(out, "payloadPolicy", admissibility == null ? null : admissibility.payloadPolicy());
        put(out, "transmissionMode", admissibility == null ? null : admissibility.transmissionMode());
        put(out, "protocolDesk", admissibility == null ? null : admissibility.protocolDesk());
        put(out, "manualSubmissionDesk", admissibility == null ? null : admissibility.manualSubmissionDesk());
        put(out, "receiptChannel", admissibility == null ? null : admissibility.receiptChannel());
        put(out, "proofBundleMode", admissibility == null ? null : admissibility.proofBundleMode());
        put(out, "connectorBaseUrl", admissibility == null ? null : admissibility.connectorBaseUrl());
        out.put("readyForDryRun", readyForDryRun);
        out.put("dryRunEndpoint", "/api/v1/laiane/protocol/preflight");
        out.put("packageEndpoint", "/api/v1/laiane/lawyer/peticao/protocol-package");
        out.put("submissionEndpoint", "/api/v1/laiane/protocol/{id}/submit");
        out.put("requisitos", buildProtocolRequirements(admissibility, documentoAggregate, precedentesQualificados));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> buildPrecedentesQualificados(RecursalIaConferenciaRequest command,
                                                              Processo processo,
                                                              LegalAppealType appealType,
                                                              RamoDireito ramoEfetivo,
                                                              RitoProcessual ritoEfetivo) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        String query = buildJurisprudenceQuery(processo, appealType, ramoEfetivo, ritoEfetivo, command.pedidoUsuario());
        List<TemaPrecedenteVinculante> temasVinculantes = temaPrecedenteVinculanteRepository.findTop100ByOrderByCreatedAtDesc();
        List<TemaRecursoRepetitivo> temasRepetitivos = temaRecursoRepetitivoRepository.findTop100ByOrderByCreatedAtDesc();
        List<Map<String, Object>> vinculantesRelacionados = temasVinculantes.stream()
                .map(item -> temaMap(item, query, processo))
                .filter(item -> ((Integer) item.get("score")) >= 20)
                .sorted((a, b) -> Integer.compare((Integer) b.get("score"), (Integer) a.get("score")))
                .limit(5)
                .toList();
        List<Map<String, Object>> repetitivosRelacionados = temasRepetitivos.stream()
                .map(item -> temaMap(item, query, processo))
                .filter(item -> ((Integer) item.get("score")) >= 20)
                .sorted((a, b) -> Integer.compare((Integer) b.get("score"), (Integer) a.get("score")))
                .limit(5)
                .toList();
        out.put("status", vinculantesRelacionados.isEmpty() && repetitivosRelacionados.isEmpty() ? "NO_STRONG_MATCHES" : "MATCHES_FOUND");
        out.put("queryBase", query);
        out.put("temasVinculantesRelacionados", vinculantesRelacionados);
        out.put("temasRepetitivosRelacionados", repetitivosRelacionados);
        out.put("acionarTrilhaQualificada", appealType.isExceptional() || !vinculantesRelacionados.isEmpty() || !repetitivosRelacionados.isEmpty());
        out.put("endpointTribunais", "/api/v1/processual/substituicao/precedentes-qualificados");
        out.put("endpointTemasRepetitivos", "/api/v1/ministro/temas-repetitivos");
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> temaMap(TemaPrecedenteVinculante tema, String query, Processo processo) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        int score = similarityScore(query, String.join(" ", List.of(safe(tema.getCodigo()), safe(tema.getTipo()), safe(tema.getStatus()), safe(tema.getEmenta()), safe(tema.getTeseFirmada()), safe(tema.getFundamentosResumo()), processo != null && tema.getLeadingCaseProcesso() != null && Objects.equals(tema.getLeadingCaseProcesso().getId(), processo.getId()) ? "LEADING_CASE" : "")));
        out.put("codigo", tema.getCodigo());
        out.put("tipo", tema.getTipo());
        out.put("status", tema.getStatus());
        out.put("score", score);
        put(out, "abrangencia", tema.getAbrangencia());
        put(out, "ementa", snippet(tema.getEmenta(), 260));
        put(out, "teseFirmada", snippet(tema.getTeseFirmada(), 220));
        put(out, "fundamentosResumo", snippet(tema.getFundamentosResumo(), 220));
        put(out, "leadingCaseProcessoId", tema.getLeadingCaseProcesso() == null ? null : tema.getLeadingCaseProcesso().getId());
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> temaMap(TemaRecursoRepetitivo tema, String query, Processo processo) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        int score = similarityScore(query, String.join(" ", List.of(safe(tema.getCodigo()), safe(tema.getStatus()), safe(tema.getTribunalSigla()), safe(tema.getEmenta()), safe(tema.getTeseFirmada()), safe(tema.getFundamentosResumo()), processo != null && tema.getRecursoRepresentativoProcesso() != null && Objects.equals(tema.getRecursoRepresentativoProcesso().getId(), processo.getId()) ? "REPRESENTATIVO" : "")));
        out.put("codigo", tema.getCodigo());
        out.put("status", tema.getStatus());
        out.put("score", score);
        put(out, "tribunalSigla", tema.getTribunalSigla());
        put(out, "ementa", snippet(tema.getEmenta(), 260));
        put(out, "teseFirmada", snippet(tema.getTeseFirmada(), 220));
        put(out, "fundamentosResumo", snippet(tema.getFundamentosResumo(), 220));
        put(out, "recursoRepresentativoProcessoId", tema.getRecursoRepresentativoProcesso() == null ? null : tema.getRecursoRepresentativoProcesso().getId());
        return Collections.unmodifiableMap(out);
    }

    private List<String> buildCounterReasonsChecklist(LegalAppealType appealType,
                                                      RamoDireito ramoEfetivo,
                                                      RitoProcessual ritoEfetivo) {
        LinkedHashSet<String> checklist = new LinkedHashSet<>();
        checklist.add("Responder cada capítulo recursal com dialeticidade específica.");
        checklist.add("Preservar fatos incontroversos e destacar pontos de manutenção da decisão.");
        checklist.add("Reforçar precedentes aderentes e distinguir os desfavoráveis.");
        if (appealType.isExceptional()) {
            checklist.add("Sustentar inadmissão por ausência de repercussão/filtro excepcional quando cabível.");
        }
        if (ramoEfetivo == RamoDireito.TRABALHISTA || (ritoEfetivo != null && ritoEfetivo.isTrabalhista())) {
            checklist.add("Controlar transcendência, delimitação e pressupostos específicos da Justiça do Trabalho.");
        }
        if (ramoEfetivo != null && ramoEfetivo.isPenalLike()) {
            checklist.add("Blindar a resposta com foco em nulidades, prova, contraditório e individualização.");
        }
        return List.copyOf(checklist);
    }

    private List<String> detectEmbargosGrounds(RecursalIaConferenciaRequest command,
                                               Processo processo,
                                               RecursalAdmissibilityResponse admissibility,
                                               List<PeritoNomeacao> pericias) {
        LinkedHashSet<String> grounds = new LinkedHashSet<>();
        String combined = String.join(" ", List.of(
                safe(command.pedidoUsuario()),
                processo != null ? safe(processo.getResumoIA()) : "",
                processo != null ? safe(processo.getResultadoFinal()) : "",
                processo != null ? safe(processo.getMaterialProbatorioResumo()) : "",
                admissibility != null && admissibility.fundamentos() != null ? String.join(" ", admissibility.fundamentos()) : "",
                pericias.stream().map(item -> safe(item.getObservacao())).collect(Collectors.joining(" "))
        )).toUpperCase(Locale.ROOT);
        if (containsAny(combined, "OMISSAO", "OMISSÃO")) grounds.add("OMISSAO");
        if (containsAny(combined, "CONTRADICAO", "CONTRADIÇÃO")) grounds.add("CONTRADICAO");
        if (containsAny(combined, "OBSCURIDADE")) grounds.add("OBSCURIDADE");
        if (containsAny(combined, "ERRO MATERIAL", "ERRO_MATERIAL")) grounds.add("ERRO_MATERIAL");
        if (containsAny(combined, "EFEITO INFRINGENTE", "EFEITOS INFRINGENTES", "REFORMA")) grounds.add("EFEITO_INFRINGENTE_POTENCIAL");
        if (grounds.isEmpty()) grounds.add("ANALISE_ESPECIALIZADA_RECOMENDADA");
        return List.copyOf(grounds);
    }

    private List<String> detectEmbargosEffects(LegalAppealType appealType,
                                               RecursalAdmissibilityResponse admissibility) {
        LinkedHashSet<String> effects = new LinkedHashSet<>();
        effects.add("INTEGRATIVO");
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            effects.add("PREQUESTIONAMENTO_ESTRATEGICO");
        }
        if (admissibility != null && admissibility.automaticSuspensiveEffect()) {
            effects.add("IMPACTO_NO_CURSO_DO_PRAZO_RECURSAL");
        }
        if (appealType == LegalAppealType.EMBARGOS_TERCEIRO) {
            effects.add("PROTECAO_DE_PATRIMONIO_DE_TERCEIRO");
        }
        return List.copyOf(effects);
    }

    private List<String> buildProtocolRequirements(RecursalAdmissibilityResponse admissibility,
                                                   ProcessoDocumentoAggregate documentoAggregate,
                                                   Map<String, Object> precedentesQualificados) {
        LinkedHashSet<String> requirements = new LinkedHashSet<>();
        requirements.add("Peça recursal final alinhada com os capítulos da decisão recorrida.");
        if (admissibility != null && admissibility.preparoExigido() && !admissibility.preparoDispensado()) {
            requirements.add("Comprovante de preparo ou validação de recolhimento." );
        }
        if (admissibility != null && admissibility.stepUpRequired()) {
            requirements.add("Step-up obrigatório antes do protocolo." );
        }
        if (admissibility != null && admissibility.certificateRequired()) {
            requirements.add("Credencial reforçada/certificado válido para transmissão." );
        }
        if (documentoAggregate != null && !documentoAggregate.trilhaAssinavel().isEmpty()) {
            requirements.add("Lotes assináveis controlados: " + String.join(", ", documentoAggregate.trilhaAssinavel().stream().limit(3).toList()));
        }
        if (precedentesQualificados != null && Boolean.TRUE.equals(precedentesQualificados.get("acionarTrilhaQualificada"))) {
            requirements.add("Conferir aderência a precedentes vinculantes e temas repetitivos antes da remessa." );
        }
        return List.copyOf(requirements);
    }

    private int similarityScore(String left, String right) {
        Set<String> a = tokenize(left);
        Set<String> b = tokenize(right);
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        long intersection = a.stream().filter(b::contains).count();
        long union = a.size() + b.size() - intersection;
        if (union <= 0) {
            return 0;
        }
        return (int) Math.round(((double) intersection / (double) union) * 100.0d);
    }

    private Set<String> tokenize(String value) {
        if (blank(value)) {
            return Set.of();
        }
        return java.util.Arrays.stream(normalizeForSimilarity(value).split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 4)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeForSimilarity(String value) {
        return safe(value).toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
    }

    private String buildJurisprudenceQuery(Processo processo,
                                           LegalAppealType appealType,
                                           RamoDireito ramoEfetivo,
                                           RitoProcessual ritoEfetivo,
                                           String pedidoUsuario) {
        List<String> parts = new ArrayList<>();
        parts.add(appealType.name().replace('_', ' '));
        if (ramoEfetivo != null) {
            parts.add(ramoEfetivo.name().replace('_', ' '));
        }
        if (ritoEfetivo != null) {
            parts.add(ritoEfetivo.name().replace('_', ' '));
        }
        if (processo != null) {
            addPart(parts, processo.getAssunto());
            addPart(parts, processo.getPedidoPrincipal());
            addPart(parts, processo.getClasseProcessual());
        }
        addPart(parts, pedidoUsuario);
        return parts.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).limit(6).collect(Collectors.joining(" "));
    }

    private List<String> buildSectionOrder(LegalAppealType appealType, RamoDireito ramoEfetivo) {
        ArrayList<String> sections = new ArrayList<>(List.of(
                "Tempestividade e preparo",
                "Síntese da decisão recorrida",
                "Delimitação das matérias impugnadas",
                "Razões recursais por capítulos",
                "Pedidos recursais",
                "Documentos e prova de apoio"
        ));
        if (appealType == LegalAppealType.EMBARGOS_DECLARACAO) {
            sections.add(3, "Vício integrativo específico e utilidade prática do aclaramento");
        }
        if (appealType.isExceptional()) {
            sections.add(1, "Capítulo de admissibilidade excepcional e filtros do tribunal superior");
        }
        if (ramoEfetivo != null && ramoEfetivo.isPenalLike()) {
            sections.add("Nulidades, prova e garantias processuais penais");
        }
        return List.copyOf(new LinkedHashSet<>(sections));
    }

    private String buildTempestividadeResumo(RecursalAdmissibilityRequest request, RecursalAdmissibilityResponse admissibility) {
        ArrayList<String> parts = new ArrayList<>();
        if (request.dataIntimacao() != null) {
            parts.add("Intimação em " + request.dataIntimacao());
        }
        if (request.dataProtocolo() != null) {
            parts.add("Protocolo em " + request.dataProtocolo());
        }
        if (admissibility != null && admissibility.dataLimite() != null) {
            parts.add("Prazo limite em " + admissibility.dataLimite());
        }
        if (admissibility != null) {
            parts.add(admissibility.tempestivo() ? "Tempestividade positiva na análise real." : "Tempestividade sob risco na análise real.");
        }
        return String.join(" ", parts);
    }

    private boolean requiresPericialAttention(RamoDireito ramoEfetivo, RitoProcessual ritoEfetivo, Processo processo) {
        if (ramoEfetivo == RamoDireito.PREVIDENCIARIO || ramoEfetivo == RamoDireito.AMBIENTAL) {
            return true;
        }
        if (ritoEfetivo != null && (ritoEfetivo.isPrevidenciario() || ritoEfetivo.isAmbiental())) {
            return true;
        }
        String combined = String.join(" ", List.of(
                processo != null ? safe(processo.getAssunto()) : "",
                processo != null ? safe(processo.getObjetoProcessual()) : "",
                processo != null ? safe(processo.getPedidoPrincipal()) : "",
                processo != null ? safe(processo.getMaterialProbatorioResumo()) : ""
        )).toUpperCase(Locale.ROOT);
        return containsAny(combined, "LAUDO", "PERICIA", "INCAPACIDADE", "INSALUBRIDADE", "PERICULO", "AMBIENTAL", "DANO TECNICO");
    }

    private boolean mentionsEmbargosGround(String text) {
        String normalized = safe(text).toUpperCase(Locale.ROOT);
        return containsAny(normalized, "OMISSAO", "OMISSÃO", "CONTRADICAO", "CONTRADIÇÃO", "OBSCURIDADE", "ERRO MATERIAL");
    }

    private boolean isRecursalLike(FaseProcessual fase) {
        String token = fase.name();
        return token.contains("RECUR") || token.contains("JULG") || token.contains("CUMPRIMENTO") || token.contains("EXECUCAO");
    }

    private static boolean containsAny(String source, String... values) {
        if (source == null || source.isBlank() || values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && !value.isBlank() && source.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static void addPart(List<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value.trim());
        }
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (target != null && key != null && value != null) {
            target.put(key, value);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String snippet(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, max - 1)).trim() + '…';
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Falha degradada na composição do contexto analítico recursal.";
        }
        return throwable.getMessage().trim();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
