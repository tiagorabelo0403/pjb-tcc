package com.tcc.pjb.backend.modules.laiane.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.preflight.ProceduralPreflightEngine;
import com.tcc.pjb.backend.core.preflight.ProceduralPreflightEngine.PreflightContext;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionResult;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService.RoutingDecision;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionRequest;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.service.competencia.CompetenceResolverService;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LaianeNationalPreflightService {

    public record PreflightIssue(String code, String severity, String message, boolean blocking) {
    }

    public record PreflightResult(
            String requestId,
            Instant generatedAt,
            String rito,
            String ramoDireito,
            String tipoJustica,
            String competencia,
            String tribunalCodigo,
            String unidadeJudiciariaCodigo,
            String connectorSystem,
            boolean readyForSubmission,
            List<String> requiredPartyRoles,
            List<String> missingPartyRoles,
            List<String> requiredDocuments,
            List<String> missingDocuments,
            List<PreflightIssue> issues,
            Map<String, Object> details
    ) {}

    private final ObjectMapper objectMapper;
    private final ProceduralCatalogService proceduralCatalogService;
    private final ProceduralPreflightEngine proceduralPreflightEngine;
    private final CompetenceResolverService competenceResolverService;
    private final MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine;
    private final TribunalProtocolRoutingService tribunalProtocolRoutingService;
    private final JudicialConnectorRegistry connectorRegistry;
    private final JudicialConnectorReadinessService judicialConnectorReadinessService;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;
    private final LaianeSubmissionGuardrailService laianeSubmissionGuardrailService;

    public LaianeNationalPreflightService(ObjectMapper objectMapper,
                                          ProceduralCatalogService proceduralCatalogService,
                                          ProceduralPreflightEngine proceduralPreflightEngine,
                                          CompetenceResolverService competenceResolverService,
                                          MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine,
                                          TribunalProtocolRoutingService tribunalProtocolRoutingService,
                                          JudicialConnectorRegistry connectorRegistry,
                                          JudicialConnectorReadinessService judicialConnectorReadinessService,
                                          ProceduralCanonicalResolver proceduralCanonicalResolver,
                                          RepresentacaoProcessualPolicyService representacaoProcessualPolicyService,
                                          LaianeSubmissionGuardrailService laianeSubmissionGuardrailService) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
        this.proceduralPreflightEngine = Objects.requireNonNull(proceduralPreflightEngine);
        this.competenceResolverService = Objects.requireNonNull(competenceResolverService);
        this.mapaCompetenciaDinamicoEngine = Objects.requireNonNull(mapaCompetenciaDinamicoEngine);
        this.tribunalProtocolRoutingService = Objects.requireNonNull(tribunalProtocolRoutingService);
        this.connectorRegistry = Objects.requireNonNull(connectorRegistry);
        this.judicialConnectorReadinessService = Objects.requireNonNull(judicialConnectorReadinessService);
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.representacaoProcessualPolicyService = Objects.requireNonNull(representacaoProcessualPolicyService);
        this.laianeSubmissionGuardrailService = Objects.requireNonNull(laianeSubmissionGuardrailService);
    }

    public PreflightResult analyze(Object payload) {
        Map<String, Object> map = toMap(payload);
        return analyze(map, null);
    }

    public PreflightResult analyze(Map<String, Object> payload, CanonicalContext canonicalContext) {
        Map<String, Object> map = payload == null ? Map.of() : new LinkedHashMap<>(payload);
        CanonicalContext resolvedCanonicalContext = canonicalContext != null ? canonicalContext : proceduralCanonicalResolver.resolve(map);
        Map<String, Object> canonicalPayload = mergeCanonicalMap(map, resolvedCanonicalContext);
        String ritoRaw = firstNonBlank(text(canonicalPayload.get("rito")), text(canonicalPayload.get("rito_processual")), text(canonicalPayload.get("ritoProcessual")), resolvedCanonicalContext.rito() != null ? resolvedCanonicalContext.rito().name() : null);
        String ramoRaw = firstNonBlank(text(canonicalPayload.get("ramo_direito")), text(canonicalPayload.get("ramoDireito")), text(canonicalPayload.get("materia")), resolvedCanonicalContext.ramoDireito());
        String classeTpuRaw = firstNonBlank(text(canonicalPayload.get("classeTpu")), text(canonicalPayload.get("classe_tpu")), text(canonicalPayload.get("classeProcessual")), text(canonicalPayload.get("classe")), resolvedCanonicalContext.classeTpuCodigo(), resolvedCanonicalContext.classeTpuNome());
        var rito = resolvedCanonicalContext.rito() != null ? resolvedCanonicalContext.rito() : proceduralCatalogService.resolveRito(canonicalPayload);

        String tribunalCodigo = firstNonBlank(text(canonicalPayload.get("tribunalCodigo")), text(canonicalPayload.get("tribunal_codigo")), text(canonicalPayload.get("tribunal")), resolvedCanonicalContext.tribunalCodigo());
        TipoUsuario actorProfile = inferActorProfile(canonicalPayload);
        var representacaoPolicy = representacaoProcessualPolicyService.resolve(
                firstNonBlank(ramoRaw, resolvedCanonicalContext.ramoDireito()),
                rito != null ? rito.name() : ritoRaw,
                tribunalCodigo,
                actorProfile,
                firstNonBlank(text(canonicalPayload.get("tipoInstrumentoRepresentacao")), text(canonicalPayload.get("tipoInstrumento"))),
                null,
                firstNonBlank(text(canonicalPayload.get("tipoAudiencia")), text(canonicalPayload.get("tipo_audiencia"))),
                flag(canonicalPayload, "contextoConsensual") || flag(canonicalPayload, "audienciaMediacao") || flag(canonicalPayload, "audienciaConciliacao"),
                flag(canonicalPayload, "poderesEspeciaisTransigir") || flag(canonicalPayload, "poderesTransigir"),
                text(canonicalPayload.get("termoAudiencia")),
                text(canonicalPayload.get("ataAudiencia"))
        );
        boolean possuiDocumentoProcuracao = flag(canonicalPayload, "possuiProcuracao") || flag(canonicalPayload, "temProcuracao") || containsDocument(canonicalPayload, "PROCURACAO");
        boolean possuiIdentificacaoAdvocatica = hasText(text(canonicalPayload.get("advogado_oab"))) || hasText(text(canonicalPayload.get("advogadoOab"))) || (actorProfile != null && actorProfile.isInstitucional());

        LinkedHashMap<String, Object> preflightInput = new LinkedHashMap<>(canonicalPayload);
        preflightInput.put("rito", rito != null ? rito.name() : ritoRaw);
        preflightInput.put("ramoDireito", firstNonBlank(ramoRaw, resolvedCanonicalContext.ramoDireito()));
        preflightInput.put("classeTpu", classeTpuRaw);
        preflightInput.put("tribunalCodigo", tribunalCodigo);
        preflightInput.put("uf", firstNonBlank(text(canonicalPayload.get("uf")), text(canonicalPayload.get("ufAutor")), text(canonicalPayload.get("ufReu"))));
        preflightInput.put("comarca", firstNonBlank(text(canonicalPayload.get("comarca")), text(canonicalPayload.get("comarcaAutor")), text(canonicalPayload.get("comarcaReu"))));
        preflightInput.put("vara", firstNonBlank(text(canonicalPayload.get("vara")), text(canonicalPayload.get("varaDestino")), text(canonicalPayload.get("orgaoJulgador"))));
        preflightInput.put("valorCausa", firstNonBlank(text(canonicalPayload.get("valorCausa")), text(canonicalPayload.get("valor_causa"))));
        preflightInput.put("partesPresentes", collectPartyRoles(canonicalPayload));
        preflightInput.put("documentosPresentes", collectDocuments(canonicalPayload));
        preflightInput.put("formatosArquivo", collectFormats(canonicalPayload));
        preflightInput.put("assinadoDigitalmente", flag(canonicalPayload, "assinadoDigitalmente") || flag(canonicalPayload, "assinaturaDigital") || flag(canonicalPayload, "signed"));
        preflightInput.put("possuiProcuracao", representacaoProcessualPolicyService.representacaoSuficiente(representacaoPolicy, possuiDocumentoProcuracao, possuiIdentificacaoAdvocatica));
        preflightInput.put("recurso", flag(canonicalPayload, "recurso"));
        preflightInput.put("tutela", flag(canonicalPayload, "tutela") || containsNormalized(text(canonicalPayload.get("pedido")), "tutela"));
        preflightInput.put("segredoJustica", flag(canonicalPayload, "segredoJustica"));
        preflightInput.put("representacaoPolicy", representacaoPolicy.envelope());
        preflightInput.putAll(safeExtras(canonicalPayload));

        ProceduralPreflightEngine.PreflightResult engineResult = proceduralPreflightEngine.evaluate(PreflightContext.fromMap(preflightInput));

        CompetenceResolveResponse competence = competenceResolverService.resolve(buildCompetenceRequest(canonicalPayload, rito != null ? rito.name() : null));
        Optional<DynamicCompetenceDistributionResponse> distribution = mapaCompetenciaDinamicoEngine.distribuir(buildDistributionRequest(canonicalPayload, competence, rito != null ? rito.name() : null));
        DynamicCompetenceDistributionResponse distributed = distribution.orElse(null);
        String competenciaResolvida = firstNonBlank(
                text(canonicalPayload.get("competencia")),
                text(canonicalPayload.get("competenciaSugerida")),
                competence != null ? competence.tipoJusticaSugerida() : null,
                resolvedCanonicalContext.ramoJusticaNacional(),
                rito != null ? rito.group() : null
        );

        RoutingDecision routing = tribunalProtocolRoutingService.resolve(
                canonicalPayload,
                rito,
                firstNonBlank(ramoRaw, resolvedCanonicalContext.ramoDireito(), rito != null ? rito.suggestedRamo().name() : null),
                competenciaResolvida,
                flag(canonicalPayload, "recurso")
        );
        JudicialProcessConnector connector = connectorRegistry.get(routing.judicialSystem());

        ProtocolSubmissionRequest dryRunRequest = new ProtocolSubmissionRequest(
                UUID.randomUUID().toString(),
                text(canonicalPayload.get("numeroUnificado")),
                firstNonBlank(text(canonicalPayload.get("title")), text(canonicalPayload.get("titulo")), "Dry Run PJB"),
                routing.tribunalCodigo(),
                distributed != null ? distributed.unidadeCodigo() : null,
                distributed != null ? distributed.comarca() : null,
                engineResult.resolvedRito(),
                engineResult.resolvedClasseTpu(),
                firstNonBlank(ramoRaw, resolvedCanonicalContext.ramoDireito(), rito != null ? rito.suggestedRamo().name() : null),
                canonicalJson(canonicalPayload),
                sha256(canonicalJson(canonicalPayload)),
                toLong(firstNonBlank(text(canonicalPayload.get("signerUserId")), text(canonicalPayload.get("signer_user_id")))),
                toLong(firstNonBlank(text(canonicalPayload.get("executorUserId")), text(canonicalPayload.get("executor_user_id")))),
                true,
                JudicialMapSupport.compact("competencia", competenciaResolvida)
        );
        JudicialConnectorReadinessReport readiness = judicialConnectorReadinessService.analyze(routing.judicialSystem(), routing.capability(), dryRunRequest);
        ProtocolSubmissionResult dryRun = routing.capability().supportsDryRun() && readiness.readyForDryRun()
                ? connector.submit(dryRunRequest)
                : new ProtocolSubmissionResult(false, routing.judicialSystem(), null, routing.capability().supportsDryRun() ? "DRY_RUN_BLOCKED_BY_READINESS" : "DRY_RUN_NOT_SUPPORTED", routing.capability().supportsDryRun() ? String.join(" | ", readiness.blockers()) : "Conector não oferece dry-run.", Instant.now(), readiness.toMap());

        LaianeSubmissionGuardrailService.GuardrailSnapshot submissionGuardrails = laianeSubmissionGuardrailService.analyze(canonicalPayload);
        List<PreflightIssue> issues = new ArrayList<>(engineResult.issues().stream()
                .map(i -> new PreflightIssue(i.code(), i.severity().label(), i.message(), i.blocks()))
                .toList());
        if (submissionGuardrails.blocking()) {
            for (String blocker : submissionGuardrails.blockers()) {
                issues.add(new PreflightIssue("SUBMISSION_GUARDRAIL_BLOCKED", "CRITICO", blocker, true));
            }
        } else if (submissionGuardrails.warning()) {
            for (String warning : submissionGuardrails.warnings()) {
                issues.add(new PreflightIssue("SUBMISSION_GUARDRAIL_WARNING", "AVISO", warning, false));
            }
        }
        if (distribution.isEmpty()) {
            issues.add(new PreflightIssue("NO_JUDICIAL_UNIT", "AVISO", "Nenhuma unidade judiciária foi selecionada automaticamente.", false));
        }
        if (routing.stepUpRequired() && !flag(canonicalPayload, "govbrStepUp") && !flag(canonicalPayload, "govbr_step_up") && !flag(canonicalPayload, "stepUpGovBr")) {
            issues.add(new PreflightIssue("STEP_UP_REQUIRED", "CRITICO", "Conector exige autenticação reforçada antes do protocolo.", true));
        }
        if (routing.certificateRequired() && !flag(canonicalPayload, "assinadoDigitalmente") && !flag(canonicalPayload, "assinaturaDigital")) {
            issues.add(new PreflightIssue("CERTIFICATE_REQUIRED", "CRITICO", "Conector exige assinatura/certificado antes do protocolo.", true));
        }
        if (!routing.capability().enabled()) {
            issues.add(new PreflightIssue("CONNECTOR_DISABLED", "CRITICO", "Conector judicial selecionado não está habilitado.", true));
        }
        if (!readiness.readyForSubmission()) {
            issues.add(new PreflightIssue("CONNECTOR_READINESS_BLOCKED", "CRITICO", readiness.blockers().isEmpty() ? "Conector sem prontidão operacional." : String.join(" | ", readiness.blockers()), true));
        }
        if (!dryRun.accepted() && routing.capability().supportsDryRun()) {
            issues.add(new PreflightIssue("DRY_RUN_REJECTED", "AVISO", dryRun.message(), false));
        }

        Optional<TpuClasseCnj> classeTpu = proceduralCatalogService.resolveClasseTpu(classeTpuRaw, rito);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("canonicalContext", resolvedCanonicalContext.toMap());
        details.put("routing", routing);
        details.put("connectorReadiness", readiness.toMap());
        details.put("dryRun", dryRun);
        details.put("catalog", proceduralCatalogService.describe(rito));
        details.put("tpuClasse", classeTpu.map(TpuClasseCnj::toMap).orElse(null));
        details.put("tribunalMatrix", proceduralCatalogService.resolveNationalTribunal(routing.tribunalCodigo()).map(t -> t.toMap()).orElse(null));
        if (distributed != null) {
            details.put("distribution", distributed);
        }
        details.put("engineMetadata", engineResult.metadata());
        details.put("submissionGuardrails", submissionGuardrails.toMap());
        details.put("payloadKeys", canonicalPayload.keySet().stream().sorted().toList());

        boolean ready = issues.stream().noneMatch(PreflightIssue::blocking);
        return new PreflightResult(
                engineResult.requestId(),
                Instant.now(),
                engineResult.resolvedRito(),
                firstNonBlank(ramoRaw, resolvedCanonicalContext.ramoDireito(), rito != null ? rito.suggestedRamo().name() : null),
                competence != null ? competence.tipoJusticaSugerida() : null,
                competenciaResolvida,
                routing.tribunalCodigo(),
                distributed != null ? distributed.unidadeCodigo() : null,
                routing.judicialSystem().name(),
                ready,
                engineResult.requiredParties(),
                engineResult.missingParties(),
                engineResult.requiredDocuments(),
                engineResult.missingDocuments(),
                List.copyOf(new LinkedHashSet<>(issues)),
                JudicialMapSupport.copyNonNull(details)
        );
    }

    private Map<String, Object> mergeCanonicalMap(Map<String, Object> map, CanonicalContext canonicalContext) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>(map == null ? Map.of() : map);
        if (canonicalContext != null) {
            if (canonicalContext.rito() != null) {
                merged.putIfAbsent("rito", canonicalContext.rito().name());
                merged.putIfAbsent("rito_processual", canonicalContext.rito().name());
            }
            if (canonicalContext.ramoDireito() != null) {
                merged.putIfAbsent("ramo_direito", canonicalContext.ramoDireito());
            }
            if (canonicalContext.classeTpuCodigo() != null) {
                merged.putIfAbsent("classeTpu", canonicalContext.classeTpuCodigo());
            }
            if (canonicalContext.tribunalCodigo() != null) {
                merged.putIfAbsent("tribunalCodigo", canonicalContext.tribunalCodigo());
            }
        }
        return JudicialMapSupport.copyNonNull(merged);
    }

    public Map<String, Object> toMap(Object payload) {
        if (payload == null) {
            return new LinkedHashMap<>();
        }
        if (payload instanceof Map<?, ?> map) {
            return objectMapper.convertValue(map, new TypeReference<LinkedHashMap<String, Object>>() {});
        }
        if (payload instanceof String text && !text.isBlank()) {
            try {
                return objectMapper.readValue(text, new TypeReference<LinkedHashMap<String, Object>>() {});
            } catch (Exception ignored) {
                return new LinkedHashMap<>(Map.of("texto", text));
            }
        }
        return objectMapper.convertValue(payload, new TypeReference<LinkedHashMap<String, Object>>() {});
    }

    private CompetenceResolveRequest buildCompetenceRequest(Map<String, Object> map, String rito) {
        return new CompetenceResolveRequest(
                collectBody(map),
                firstNonBlank(text(map.get("assunto")), text(map.get("assuntoTpu")), text(map.get("classeTpu"))),
                firstNonBlank(text(map.get("classeTpu")), text(map.get("classeProcessual")), text(map.get("classe"))),
                firstNonBlank(text(map.get("materia")), text(map.get("ramo_direito")), suggestedRamo(rito)),
                firstNonBlank(text(map.get("uf")), text(map.get("ufAutor")), text(map.get("ufReu"))),
                firstNonBlank(text(map.get("comarca")), text(map.get("comarcaAutor")), text(map.get("comarcaReu"))),
                toBigDecimal(firstNonBlank(text(map.get("valor_causa")), text(map.get("valorCausa")))),
                flag(map, "envolveUniao"),
                flag(map, "envolveAutarquiaFederal"),
                flag(map, "envolveEmpresaPublicaFederal"),
                flag(map, "envolveEstado"),
                flag(map, "envolveMunicipio"),
                flag(map, "envolveRelacaoTrabalho"),
                flag(map, "envolveEleitoral") || isEleitoralRito(rito),
                flag(map, "envolveMilitar") || isMilitarRito(rito)
        );
    }

    private DynamicCompetenceDistributionRequest buildDistributionRequest(Map<String, Object> map,
                                                                          CompetenceResolveResponse competence,
                                                                          String rito) {
        return new DynamicCompetenceDistributionRequest(
                text(map.get("nupn")),
                firstNonBlank(text(map.get("classeTpu")), text(map.get("classeProcessual")), text(map.get("classe"))),
                firstNonBlank(text(map.get("assuntoTpu")), text(map.get("assunto"))),
                firstNonBlank(text(map.get("ramo_direito")), suggestedRamo(rito)),
                toBigDecimal(firstNonBlank(text(map.get("valorCausa")), text(map.get("valor_causa")))),
                firstNonBlank(text(map.get("ufAutor")), text(map.get("uf"))),
                firstNonBlank(text(map.get("comarcaAutor")), text(map.get("comarca"))),
                text(map.get("ufReu")),
                text(map.get("comarcaReu")),
                flag(map, "requerJuizadoEspecial"),
                flag(map, "requerVaraEspecializada"),
                firstNonBlank(text(map.get("materiaPrincipal")), text(map.get("materia"))),
                competence != null ? competence.tipoJusticaSugerida() : null,
                flag(map, "casoUrgente"),
                flag(map, "preferenciaDigital"),
                toLong(text(map.get("processoId")))
        );
    }

    private String suggestedRamo(String rito) {
        if (rito == null || rito.isBlank()) {
            return null;
        }
        String normalized = rito.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("TRABALH")) return "TRABALHISTA";
        if (normalized.contains("ELEITORAL")) return "ELEITORAL";
        if (normalized.contains("MILITAR")) return "MILITAR";
        if (normalized.contains("PREVID")) return "PREVIDENCIARIO";
        if (normalized.contains("TRIBUT") || normalized.contains("FAZENDA") || normalized.equals("EXECUCAO_FISCAL")) return "TRIBUTARIO";
        if (normalized.contains("PENAL") || normalized.contains("JURI") || normalized.contains("HABEAS_CORPUS") || normalized.contains("EXECUCAO_PENAL")) return "PENAL";
        return null;
    }

    private boolean isEleitoralRito(String rito) {
        return rito != null && rito.toUpperCase(Locale.ROOT).contains("ELEITORAL");
    }

    private boolean isMilitarRito(String rito) {
        return rito != null && rito.toUpperCase(Locale.ROOT).contains("MILITAR");
    }

    private List<String> collectPartyRoles(Map<String, Object> map) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        appendCollection(values, map.get("partesPresentes"));
        appendCollection(values, map.get("partes"));
        appendCollection(values, map.get("parties"));
        appendCollection(values, map.get("polos"));
        for (String key : List.of("autor", "reu", "impetrante", "impetrado", "representante", "representado", "acusado", "vitima", "ministerioPublico", "mp", "paciente", "autoridadeCoatora")) {
            if (map.containsKey(key) && map.get(key) != null) {
                values.add(key);
            }
        }
        return List.copyOf(values);
    }

    private List<String> collectDocuments(Map<String, Object> map) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        appendCollection(values, map.get("documentosPresentes"));
        appendCollection(values, map.get("documentos"));
        appendCollection(values, map.get("documents"));
        appendCollection(values, map.get("anexos"));
        appendCollection(values, map.get("attachments"));
        return List.copyOf(values);
    }

    private List<String> collectFormats(Map<String, Object> map) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        appendCollection(values, map.get("formatosArquivo"));
        appendCollection(values, map.get("fileFormats"));
        appendCollection(values, map.get("formatos"));
        return List.copyOf(values);
    }

    private void appendCollection(LinkedHashSet<String> target, Object raw) {
        if (raw == null) {
            return;
        }
        if (raw instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item == null) {
                    continue;
                }
                target.add(String.valueOf(item));
            }
            return;
        }
        target.add(String.valueOf(raw));
    }

    private boolean containsDocument(Map<String, Object> map, String code) {
        String normalized = normalize(code);
        return collectDocuments(map).stream().map(this::normalize).anyMatch(normalized::equals);
    }

    private Map<String, Object> safeExtras(Map<String, Object> map) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (map == null) {
            return out;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                out.put(entry.getKey(), entry.getValue());
            }
        }
        return out;
    }

    private String collectBody(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder(1024);
        for (String key : List.of("texto", "peticao", "peticaoInicial", "fatos", "fundamentos", "pedidos", "resumo")) {
            String value = text(map.get(key));
            if (value != null && !value.isBlank()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(value.trim());
            }
        }
        return sb.toString();
    }

    private String canonicalJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(payload);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar hash SHA-256", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A').replace('Ã', 'A').replace('Â', 'A').replace('À', 'A')
                .replace('É', 'E').replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O').replace('Ô', 'O').replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replace(' ', '_');
    }

    private boolean containsNormalized(String value, String token) {
        return value != null && normalize(value).contains(normalize(token));
    }

    private boolean flag(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && "true".equalsIgnoreCase(String.valueOf(value));
    }

    private BigDecimal toBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String normalized = value.replace("R$", "").replace(".", "").replace(",", ".").trim();
            return normalized.isBlank() ? null : new BigDecimal(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private Long toLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private TipoUsuario inferActorProfile(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        for (String key : List.of("perfilAtor", "perfil_ator", "tipoUsuario", "tipo_usuario", "atorPerfil", "actorProfile")) {
            String raw = text(map.get(key));
            if (!hasText(raw)) {
                continue;
            }
            try {
                return TipoUsuario.valueOf(normalize(raw));
            } catch (Exception ignored) {
            }
        }
        if (hasText(text(map.get("advogadoOab"))) || hasText(text(map.get("advogado_oab")))) {
            return TipoUsuario.ADVOGADO;
        }
        if (flag(map, "ministerioPublico") || flag(map, "mp")) {
            return TipoUsuario.MEMBRO_MINISTERIO_PUBLICO;
        }
        if (flag(map, "defensoriaPublica") || flag(map, "defensoria_publica")) {
            return TipoUsuario.DEFENSOR_PUBLICO;
        }
        if (flag(map, "procuradoria") || flag(map, "advocaciaPublica") || flag(map, "advocacia_publica")) {
            return TipoUsuario.PROCURADOR;
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
