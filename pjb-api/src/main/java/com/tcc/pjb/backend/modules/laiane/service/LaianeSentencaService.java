package com.tcc.pjb.backend.modules.laiane.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.core.pipeline.EvidenceContradictionAnalyzer;
import com.tcc.pjb.backend.ai.core.pipeline.EvidenceContradictionReport;
import com.tcc.pjb.backend.ai.core.pipeline.MinimalQuestionPlanner;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem.EvidenceType;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.crypto.quantum.QuantumDecisionSignerService;
import com.tcc.pjb.backend.model.dto.intelligence.JudgeDecisionConsistencyResponse;
import com.tcc.pjb.backend.model.dto.intelligence.QualifiedThemeAlertResponse;
import com.tcc.pjb.backend.model.dto.intelligence.StructuredProcessSummaryResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.VotoColegiado;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianePrecedenteLiteDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudicialDecisionAdvisoryResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeSentencaDraftRequest;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeSentencaDraftResponse;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeSentencaDraft;
import com.tcc.pjb.backend.modules.laiane.model.LaianeSentencaStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeSentencaDraftRepository;
import com.tcc.pjb.backend.platform.observability.ai.AiTelemetryDomain;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.ai.juridica.v3.core.LegalDraftingService;
import com.tcc.pjb.backend.service.semantic.SemanticPrecedentSearchService;
import com.tcc.pjb.backend.core.security.magistratura.delegation.MagistraturaAccessService;
import com.tcc.pjb.backend.service.intelligence.JudgeDecisionConsistencyService;
import com.tcc.pjb.backend.service.intelligence.QualifiedThemeProactiveService;
import com.tcc.pjb.backend.service.intelligence.StructuredProcessSummaryService;

@Service
public class LaianeSentencaService {

    private static final String CACHE_SENTENCA_LATEST = "sentenca_latest";

    private final ProcessoRepository processoRepository;
    private final LaianeSentencaDraftRepository repository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final LegalDraftingService legalDraftingService;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final MagistraturaAccessService magistraturaAccessService;
    private final QuantumDecisionSignerService quantumDecisionSignerService;
    private final SemanticPrecedentSearchService semanticPrecedentSearchService;
    private final MinimalQuestionPlanner questionPlanner;
    private final LaianeJudicialDecisionAdvisoryService judicialDecisionAdvisoryService;
    private final JudgeDecisionConsistencyService judgeDecisionConsistencyService;
    private final StructuredProcessSummaryService structuredProcessSummaryService;
    private final QualifiedThemeProactiveService qualifiedThemeProactiveService;
    private final PjbTimeService timeService;
    private final EvidenceContradictionAnalyzer contradictionAnalyzer = new EvidenceContradictionAnalyzer();

    public LaianeSentencaService(ProcessoRepository processoRepository,
                                 LaianeSentencaDraftRepository repository,
                                 CurrentUserService currentUserService,
                                 PjbAuthorizationService authorizationService,
                                 LegalDraftingService legalDraftingService,
                                 ObjectMapper objectMapper,
                                 CacheManager cacheManager,
                                 MagistraturaAccessService magistraturaAccessService,
                                 QuantumDecisionSignerService quantumDecisionSignerService,
                                 SemanticPrecedentSearchService semanticPrecedentSearchService,
                                 MinimalQuestionPlanner questionPlanner,
                                 LaianeJudicialDecisionAdvisoryService judicialDecisionAdvisoryService,
                                 JudgeDecisionConsistencyService judgeDecisionConsistencyService,
                                 StructuredProcessSummaryService structuredProcessSummaryService,
                                 QualifiedThemeProactiveService qualifiedThemeProactiveService,
                                 PjbTimeService timeService) {
        this.processoRepository = processoRepository;
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.legalDraftingService = legalDraftingService;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
        this.magistraturaAccessService = magistraturaAccessService;
        this.quantumDecisionSignerService = quantumDecisionSignerService;
        this.semanticPrecedentSearchService = semanticPrecedentSearchService;
        this.questionPlanner = questionPlanner;
        this.judicialDecisionAdvisoryService = judicialDecisionAdvisoryService;
        this.judgeDecisionConsistencyService = judgeDecisionConsistencyService;
        this.structuredProcessSummaryService = structuredProcessSummaryService;
        this.qualifiedThemeProactiveService = qualifiedThemeProactiveService;
        this.timeService = timeService;
    }

    @Transactional
    public LaianeSentencaDraftResponse gerarRascunho(LaianeSentencaDraftRequest req) {
        Objects.requireNonNull(req, "req é obrigatório");
        Objects.requireNonNull(req.getProcessoId(), "processoId é obrigatório");

        Processo processo = processoRepository.findById(req.getProcessoId())
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + req.getProcessoId()));

        authorizationService.requireReadProcesso(processo);
        magistraturaAccessService.requireDraftAccess();

        Usuario ator = currentUserService.getOptional().orElse(null);
        boolean incluirAnalise = Boolean.TRUE.equals(req.getIncluirAnalise());

        List<Precedente> precedentes = semanticPrecedentSearchService.semanticSearch(
                processo.getRamoDireito(),
                processo.getRito(),
                buildQueryBase(processo, req),
                incluirAnalise ? 8 : 4
        );
        List<LaianePrecedenteLiteDto> precedentesRelacionados = precedentes.stream()
                .map(this::toPrecedenteLite)
                .toList();

        List<String> questoesASolver = incluirAnalise
                ? questionPlanner.topQuestions(
                        AiTelemetryDomain.LEGAL,
                        "judge_sentence_draft",
                        ApiVersion.latest(),
                        buildQuestionCandidates(processo, req, precedentesRelacionados))
                : List.of();

        List<String> contradicoes = incluirAnalise
                ? extractContradictions(analyzeContradictions(processo, req, precedentes))
                : List.of();

        String resumoExecutivo = buildResumoExecutivo(processo, req, precedentesRelacionados, questoesASolver, contradicoes);
        LaianeJudicialDecisionAdvisoryResponse decisaoAssistida = judicialDecisionAdvisoryService.analyze(processo, req);
        StructuredProcessSummaryResponse resumoProcessualEstruturado = structuredProcessSummaryService.summarize(processo);
        QualifiedThemeAlertResponse temasQualificados = qualifiedThemeProactiveService.analyze(processo);

        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("kind", "SENTENCA");
        ctx.put("numero_processo", processo.getNumeroUnificado());
        ctx.put("orgao_judiciario", firstNonBlank(req.getOrgaoJudiciario(), guessOrgao(processo)));
        ctx.put("autor", firstNonBlank(processo.getParteAutoraNome(), "[NOME DO(A) AUTOR(A)]"));
        ctx.put("reu", firstNonBlank(processo.getParteReuNome(), "[NOME DO RÉU]"));
        ctx.put("fatos", firstNonBlank(req.getFatos(), processo.getObjetoProcessual(), processo.getResumoIA()));
        ctx.put("fundamentos", firstNonBlank(req.getFundamentos(), processo.getResumoIA(), processo.getAssunto()));
        ctx.put("pedidos", sanitizeList(req.getPedidos(), processo.getPedidosConsolidados(), processo.getPedidoPrincipal()));
        ctx.put("provas", sanitizeList(req.getProvas(), processo.getMaterialProbatorioResumo(), processo.getMaterialProbatorioHash()));
        ctx.put("valor_causa", firstNonBlank(req.getValorCausa(), safeValor(processo)));
        ctx.put("dispositivo_extra", req.getDispositivoExtra());
        ctx.put("local_data", req.getLocalData());
        ctx.put("assinatura", req.getAssinatura());
        ctx.put("fase_atual", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null);
        ctx.put("rito", processo.getRito() != null ? processo.getRito().name() : null);
        ctx.put("nivel_sigilo", processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : null);
        ctx.put("resumo_executivo", resumoExecutivo);
        ctx.put("questoes_a_resolver", questoesASolver);
        ctx.put("contradicoes", contradicoes);
        ctx.put("precedentes_relacionados", precedentesRelacionados);
        ctx.put("incluir_analise", incluirAnalise);
        ctx.put("decision_blueprint", decisaoAssistida != null ? decisaoAssistida.toMap() : Map.of());

        String ctxJson = toJson(ctx);
        String inputHash = sha256Hex(ctxJson);

        Optional<LaianeSentencaDraft> existing = repository
                .findFirstByProcesso_IdAndInputHashAndStatusOrderByCreatedAtDesc(processo.getId(), inputHash, LaianeSentencaStatus.DRAFT);
        if (existing.isPresent()) {
            return toResponse(existing.get(), null, null, null, null, null, null, null, null, null);
        }

        String markdown = legalDraftingService.draftSentenca(ctx);
        JudgeDecisionConsistencyResponse consistenciaDecisoria = incluirAnalise ? judgeDecisionConsistencyService.analyze(processo, markdown) : null;
        ctx.put("resumo_processual_estruturado", resumoProcessualEstruturado);
        ctx.put("temas_qualificados", temasQualificados);
        ctx.put("consistencia_decisoria", consistenciaDecisoria);
        ctxJson = toJson(ctx);
        inputHash = sha256Hex(ctxJson);

        LaianeSentencaDraft entity = LaianeSentencaDraft.builder()
                .processo(processo)
                .criadoPor(ator)
                .status(LaianeSentencaStatus.DRAFT)
                .inputHash(inputHash)
                .draftMarkdown(markdown)
                .contextJson(ctxJson)
                .publishedAt(null)
                .build();

        LaianeSentencaDraft saved = repository.save(entity);
        evictLatestCache(processo.getId());
        return toResponse(saved, resumoExecutivo, questoesASolver, contradicoes, precedentesRelacionados, decisaoAssistida, consistenciaDecisoria, resumoProcessualEstruturado, temasQualificados, null);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_SENTENCA_LATEST, key = "#processoId", condition = "@cacheRuntime.redisEnabled()")
    public LaianeSentencaDraftResponse latest(Long processoId) {
        Objects.requireNonNull(processoId, "processoId é obrigatório");

        LaianeSentencaDraft draft = repository
                .findFirstByProcesso_IdAndStatusInOrderByCreatedAtDesc(processoId, List.of(LaianeSentencaStatus.DRAFT, LaianeSentencaStatus.PUBLISHED))
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma minuta de sentença encontrada para o processo: " + processoId));

        if (draft.getProcesso() != null) {
            authorizationService.requireReadProcesso(draft.getProcesso());
        }
        magistraturaAccessService.requireFinalActAccess();
        return toResponse(draft, null, null, null, null, null, null, null, null, null);
    }

    @Transactional
    public LaianeSentencaDraftResponse publicar(Long draftId) {
        Objects.requireNonNull(draftId, "draftId é obrigatório");
        LaianeSentencaDraft draft = repository.findById(draftId)
                .orElseThrow(() -> new IllegalArgumentException("Minuta não encontrada: " + draftId));

        magistraturaAccessService.requireFinalActAccess();

        if (draft.getProcesso() != null) {
            authorizationService.requireReadProcesso(draft.getProcesso());
        }
        if (draft.getStatus() == LaianeSentencaStatus.ARCHIVED) {
            throw new IllegalStateException("Minuta está arquivada e não pode ser publicada.");
        }
        if (draft.getStatus() == LaianeSentencaStatus.PUBLISHED) {
            return toResponse(draft, null, null, null, null, null, null, null, null, null);
        }

        var pqcEvidence = quantumDecisionSignerService.signAndAttachEvidenceOrThrowIfEnabled(draft).orElse(null);
        draft.setStatus(LaianeSentencaStatus.PUBLISHED);
        draft.setPublishedAt(LocalDateTime.ofInstant(timeService.nowUtc(), timeService.legalZone()));
        LaianeSentencaDraft saved = repository.save(draft);
        evictLatestCache(saved.getProcesso() != null ? saved.getProcesso().getId() : null);
        return toResponse(saved, null, null, null, null, null, null, null, null, pqcEvidence);
    }

    private EvidenceContradictionReport analyzeContradictions(Processo processo,
                                                              LaianeSentencaDraftRequest req,
                                                              List<Precedente> precedentes) {
        return contradictionAnalyzer.analyze(
                buildEvidence(processo, req, precedentes),
                AiTelemetryDomain.LEGAL,
                ApiVersion.latest()
        );
    }

    private List<EvidenceItem> buildEvidence(Processo processo,
                                             LaianeSentencaDraftRequest req,
                                             List<Precedente> precedentes) {
        List<EvidenceItem> out = new ArrayList<>();
        addEvidence(out, "processo-fatos", firstNonBlank(req.getFatos(), processo.getObjetoProcessual(), processo.getResumoIA()), EvidenceType.PECA_PROCESSUAL, processo.getTribunal());
        addEvidence(out, "processo-provas", firstNonBlank(joinLines(req.getProvas()), processo.getMaterialProbatorioResumo(), processo.getMaterialProbatorioHash()), EvidenceType.PECA_PROCESSUAL, processo.getTribunal());
        addEvidence(out, "processo-pedidos", firstNonBlank(joinLines(req.getPedidos()), processo.getPedidosConsolidados(), processo.getPedidoPrincipal()), EvidenceType.PECA_PROCESSUAL, processo.getTribunal());
        for (Precedente precedente : precedentes) {
            out.add(EvidenceItem.builder()
                    .docId(precedente.getId() != null ? precedente.getId().toString() : null)
                    .tipo(EvidenceType.JURISPRUDENCIA)
                    .titulo(precedente.getTitulo())
                    .tribunal(precedente.getFonte() != null ? precedente.getFonte().name() : processo.getTribunal())
                    .fonteSistema("PJB_PRECEDENTE")
                    .trecho(firstNonBlank(precedente.getTese(), precedente.getEmentaResumo()))
                    .build());
        }
        return out;
    }

    private void addEvidence(List<EvidenceItem> out,
                             String docId,
                             String trecho,
                             EvidenceType type,
                             String tribunal) {
        if (trecho == null || trecho.isBlank()) {
            return;
        }
        out.add(EvidenceItem.builder()
                .docId(docId)
                .tipo(type)
                .titulo(docId)
                .tribunal(tribunal)
                .fonteSistema("PJB_PROCESSO")
                .trecho(limit(trecho, 2000))
                .build());
    }

    private List<String> extractContradictions(EvidenceContradictionReport report) {
        List<String> out = new ArrayList<>();
        if (report.contradictionScore() > 0.0d) {
            out.add("Score de contradição: " + round(report.contradictionScore()));
        }
        if (report.inconsistencyScore() > 0.0d) {
            out.add("Score de inconsistência: " + round(report.inconsistencyScore()));
        }
        if (report.mixedJurisdiction()) {
            out.add("Há mistura de fontes e jurisdições no conjunto probatório e jurisprudencial.");
        }
        report.signals().stream().limit(4).map(this::humanizeSignal).forEach(out::add);
        if (report.negativeStance() > 0 && report.positiveStance() > 0) {
            out.add("Existem fundamentos com orientação favorável e desfavorável exigindo escolha explícita na fundamentação.");
        }
        return out.stream().distinct().limit(6).toList();
    }

    private String humanizeSignal(String signal) {
        return switch (signal) {
            case "mixed_stance" -> "As evidências trazem posicionamentos opostos.";
            case "temporal_spread_high" -> "As referências cobrem uma janela temporal extensa e exigem recorte atual.";
            case "mixed_jurisdiction_or_sources" -> "Foram identificadas fontes de níveis jurisdicionais distintos.";
            case "inconclusive_language_present" -> "Há linguagem inconclusiva em parte do acervo probatório.";
            default -> "Sinal analítico detectado: " + signal;
        };
    }

    private String buildResumoExecutivo(Processo processo,
                                        LaianeSentencaDraftRequest req,
                                        List<LaianePrecedenteLiteDto> precedentes,
                                        List<String> questoes,
                                        List<String> contradicoes) {
        List<String> partes = new ArrayList<>();
        partes.add("Processo " + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId())));
        if (processo.getClasseProcessual() != null && !processo.getClasseProcessual().isBlank()) {
            partes.add("classe " + processo.getClasseProcessual().trim());
        }
        if (processo.getRito() != null) {
            partes.add("rito " + processo.getRito().name());
        }
        if (req.getFatos() != null && !req.getFatos().isBlank()) {
            partes.add(limit(req.getFatos(), 200));
        } else if (processo.getResumoIA() != null && !processo.getResumoIA().isBlank()) {
            partes.add(limit(processo.getResumoIA(), 200));
        }
        if (!questoes.isEmpty()) {
            partes.add("questões centrais: " + String.join("; ", questoes.stream().limit(3).toList()));
        }
        if (!contradicoes.isEmpty()) {
            partes.add("atenções: " + String.join("; ", contradicoes.stream().limit(2).toList()));
        }
        if (!precedentes.isEmpty()) {
            partes.add("precedentes: " + String.join(", ", precedentes.stream().map(LaianePrecedenteLiteDto::tema).filter(Objects::nonNull).limit(3).toList()));
        }
        return String.join(" | ", partes);
    }

    private List<String> buildQuestionCandidates(Processo processo,
                                                 LaianeSentencaDraftRequest req,
                                                 List<LaianePrecedenteLiteDto> precedentes) {
        List<String> candidates = new ArrayList<>();
        candidates.add("Há prova suficiente sobre os fatos controvertidos do processo?");
        candidates.add("O rito " + (processo.getRito() != null ? processo.getRito().name() : "aplicável") + " foi observado em todos os atos essenciais?");
        candidates.add("Existe precedente dominante aplicável ao assunto " + firstNonBlank(processo.getAssunto(), processo.getClasseProcessual(), "principal") + "?");
        candidates.add("Há nulidade, incompetência ou prevenção a ser enfrentada antes do mérito?");
        if (req.getPedidos() != null && !req.getPedidos().isEmpty()) {
            req.getPedidos().stream().filter(Objects::nonNull).limit(3)
                    .forEach(p -> candidates.add("O pedido '" + limit(p, 90) + "' está devidamente provado e juridicamente enquadrado?"));
        }
        precedentes.stream().map(LaianePrecedenteLiteDto::tema).filter(Objects::nonNull).limit(3)
                .forEach(tema -> candidates.add("O precedente tema '" + tema + "' incide integralmente sobre o caso concreto?"));
        return candidates;
    }

    private List<String> sanitizeList(List<String> primary, String fallback, String secondFallback) {
        if (primary != null && !primary.isEmpty()) {
            return primary.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).limit(20).toList();
        }
        String base = firstNonBlank(fallback, secondFallback);
        if (base == null || base.isBlank()) {
            return List.of();
        }
        String[] raw = base.replace('\r', '\n').split("(?:\\n|;)");
        List<String> out = new ArrayList<>();
        for (String item : raw) {
            if (item == null) {
                continue;
            }
            String normalized = item.trim();
            if (!normalized.isBlank()) {
                out.add(limit(normalized, 280));
            }
        }
        return out.isEmpty() ? List.of(limit(base.trim(), 280)) : out.stream().limit(20).toList();
    }

    private String buildQueryBase(Processo processo, LaianeSentencaDraftRequest req) {
        List<String> parts = new ArrayList<>();
        parts.add(firstNonBlank(processo.getAssunto(), processo.getClasseProcessual()));
        parts.add(firstNonBlank(req.getFatos(), processo.getResumoIA(), processo.getObjetoProcessual()));
        parts.add(firstNonBlank(req.getFundamentos(), processo.getPedidoPrincipal(), processo.getPedidosConsolidados()));
        return parts.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).limit(3).reduce((a, b) -> a + " | " + b).orElse("sentença");
    }

    private void evictLatestCache(Long processoId) {
        if (processoId == null) {
            return;
        }
        try {
            Cache cache = cacheManager.getCache(CACHE_SENTENCA_LATEST);
            if (cache != null) {
                cache.evict(processoId);
            }
        } catch (Exception ignored) {
        }
    }

    private String toJson(Map<String, Object> ctx) {
        try {
            return objectMapper.writeValueAsString(ctx);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar contexto da sentença.", e);
        }
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular hash da sentença.", e);
        }
    }

    private String firstNonBlank(String... values) {
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

    private String guessOrgao(Processo processo) {
        String comarca = processo.getJurisdicao() != null ? processo.getJurisdicao().getComarca() : null;
        if (comarca != null && !comarca.isBlank()) {
            return "Vara da Comarca de " + comarca.trim();
        }
        return "Vara Competente";
    }

    private static String safeValor(Processo processo) {
        return processo.getValorCausa() == null ? null : processo.getValorCausa().toPlainString();
    }

    private String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        return String.join("\n", lines.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).toList());
    }

    private LaianePrecedenteLiteDto toPrecedenteLite(Precedente precedente) {
        String tribunal = precedente.getFonte() != null ? precedente.getFonte().name() : null;
        String tema = precedente.getIdentificador() != null ? precedente.getIdentificador() : (precedente.getTipo() != null ? precedente.getTipo().name() : null);
        return LaianePrecedenteLiteDto.builder()
                .id(precedente.getId())
                .titulo(precedente.getTitulo())
                .tribunal(tribunal)
                .tema(tema)
                .ementa(precedente.getEmentaResumo())
                .build();
    }

    private LaianeSentencaDraftResponse toResponse(LaianeSentencaDraft draft,
                                                   String resumoExecutivo,
                                                   List<String> questoes,
                                                   List<String> contradicoes,
                                                   List<LaianePrecedenteLiteDto> precedentes) {
        return toResponse(draft, resumoExecutivo, questoes, contradicoes, precedentes, null, null, null, null, null);
    }

    private LaianeSentencaDraftResponse toResponse(LaianeSentencaDraft draft,
                                                   String resumoExecutivo,
                                                   List<String> questoes,
                                                   List<String> contradicoes,
                                                   List<LaianePrecedenteLiteDto> precedentes,
                                                   LaianeJudicialDecisionAdvisoryResponse decisaoAssistida) {
        return toResponse(draft, resumoExecutivo, questoes, contradicoes, precedentes, decisaoAssistida, null, null, null, null);
    }

    private LaianeSentencaDraftResponse toResponse(LaianeSentencaDraft draft,
                                                   String resumoExecutivo,
                                                   List<String> questoes,
                                                   List<String> contradicoes,
                                                   List<LaianePrecedenteLiteDto> precedentes,
                                                   LaianeJudicialDecisionAdvisoryResponse decisaoAssistida,
                                                   JudgeDecisionConsistencyResponse consistenciaDecisoria,
                                                   StructuredProcessSummaryResponse resumoProcessualEstruturado,
                                                   QualifiedThemeAlertResponse temasQualificados,
                                                   com.tcc.pjb.backend.core.security.crypto.quantum.PqcEvidence pqc) {
        Map<String, Object> ctx = parseContext(draft.getContextJson());
        List<String> resolvedQuestoes = questoes != null ? questoes : readStringList(ctx.get("questoes_a_resolver"));
        List<String> resolvedContradicoes = contradicoes != null ? contradicoes : readStringList(ctx.get("contradicoes"));
        List<LaianePrecedenteLiteDto> resolvedPrecedentes = precedentes != null ? precedentes : readPrecedentes(ctx.get("precedentes_relacionados"));
        LaianeJudicialDecisionAdvisoryResponse resolvedDecisaoAssistida = decisaoAssistida != null ? decisaoAssistida : readDecisionBlueprint(ctx.get("decision_blueprint"));
        JudgeDecisionConsistencyResponse resolvedConsistencia = consistenciaDecisoria != null ? consistenciaDecisoria : readConsistency(ctx.get("consistencia_decisoria"));
        StructuredProcessSummaryResponse resolvedResumoEstruturado = resumoProcessualEstruturado != null ? resumoProcessualEstruturado : readStructuredSummary(ctx.get("resumo_processual_estruturado"));
        QualifiedThemeAlertResponse resolvedTemasQualificados = temasQualificados != null ? temasQualificados : readQualifiedThemes(ctx.get("temas_qualificados"));
        String resolvedResumoExecutivo = resumoExecutivo != null ? resumoExecutivo : asString(ctx.get("resumo_executivo"));
        return LaianeSentencaDraftResponse.builder()
                .draftId(draft.getId())
                .uuid(draft.getUuid() != null ? draft.getUuid().toString() : null)
                .processoId(draft.getProcesso() != null ? draft.getProcesso().getId() : null)
                .status(draft.getStatus() != null ? draft.getStatus().name() : null)
                .inputHash(draft.getInputHash())
                .draftMarkdown(draft.getDraftMarkdown())
                .createdAt(draft.getCreatedAt())
                .publishedAt(draft.getPublishedAt())
                .resumoExecutivo(resolvedResumoExecutivo)
                .questoesASolver(resolvedQuestoes)
                .contradicoes(resolvedContradicoes)
                .precedentesRelacionados(resolvedPrecedentes)
                .decisaoAssistida(resolvedDecisaoAssistida)
                .consistenciaDecisoria(resolvedConsistencia)
                .resumoProcessualEstruturado(resolvedResumoEstruturado)
                .temasQualificados(resolvedTemasQualificados)
                .pqcAlgorithm(pqc != null ? pqc.algorithm() : null)
                .pqcSignatureB64(pqc != null ? pqc.signatureB64() : null)
                .pqcPublicKeyB64(pqc != null ? pqc.publicKeyB64() : null)
                .build();
    }

    private LaianeJudicialDecisionAdvisoryResponse readDecisionBlueprint(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        Object templateCodeRaw = map.get("templateCode");
        com.tcc.pjb.backend.modules.laiane.dto.roles.judge.JudicialDecisionTemplateCode templateCode = null;
        if (templateCodeRaw != null) {
            try {
                templateCode = com.tcc.pjb.backend.modules.laiane.dto.roles.judge.JudicialDecisionTemplateCode.valueOf(String.valueOf(templateCodeRaw).trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        Map<String, String> fillable = new LinkedHashMap<>();
        Object fillableRaw = map.get("fillableVariables");
        if (fillableRaw instanceof Map<?, ?> fillableMap) {
            fillableMap.forEach((k, v) -> {
                if (k != null && v != null) {
                    fillable.put(String.valueOf(k), String.valueOf(v));
                }
            });
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        Object metadataRaw = map.get("metadata");
        if (metadataRaw instanceof Map<?, ?> metadataMap) {
            metadataMap.forEach((k, v) -> {
                if (k != null && v != null) {
                    metadata.put(String.valueOf(k), v);
                }
            });
        }
        return new LaianeJudicialDecisionAdvisoryResponse(
                templateCode,
                asString(map.get("advisoryMode")),
                asBoolean(map.get("reviewRequired")),
                asBoolean(map.get("publicationLocked")),
                asString(map.get("rationaleSummary")),
                readStringList(map.get("legalAnchors")),
                readStringList(map.get("reasoningChecklist")),
                readStringList(map.get("pendingFacts")),
                asString(map.get("dispositiveBase")),
                Map.copyOf(fillable),
                Map.copyOf(metadata)
        );
    }

    private JudgeDecisionConsistencyResponse readConsistency(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        return new JudgeDecisionConsistencyResponse(
                asBoolean(map.get("available")),
                asBoolean(map.get("divergenceRisk")),
                asString(map.get("currentOrientation")),
                asString(map.get("historicalDominantOrientation")),
                asDouble(map.get("consistencyScore")),
                readDecisionReferences(map.get("references")),
                readStringList(map.get("fundamentos")),
                readStringList(map.get("reviewChecklist"))
        );
    }

    private List<JudgeDecisionConsistencyResponse.DecisionReference> readDecisionReferences(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<JudgeDecisionConsistencyResponse.DecisionReference> out = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            out.add(new JudgeDecisionConsistencyResponse.DecisionReference(
                    parseLong(map.get("draftId")),
                    parseLong(map.get("processoId")),
                    asString(map.get("processoNumero")),
                    asString(map.get("orientation")),
                    asString(map.get("classeProcessual")),
                    asString(map.get("assunto")),
                    asString(map.get("createdAt"))
            ));
        }
        return List.copyOf(out);
    }

    private StructuredProcessSummaryResponse readStructuredSummary(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        return new StructuredProcessSummaryResponse(
                parseLong(map.get("processoId")),
                asString(map.get("numeroProcesso")),
                asString(map.get("quickSummary")),
                asInt(map.get("estimatedReadingSeconds")),
                readStringList(map.get("partes")),
                readStringList(map.get("pedidos")),
                readStringList(map.get("provasPrincipais")),
                readStringList(map.get("decisoesAnteriores")),
                readStringList(map.get("estadoAtual")),
                readStringList(map.get("proximosAtos")),
                readStringList(map.get("alertas")),
                readStringList(map.get("fontes"))
        );
    }

    private QualifiedThemeAlertResponse readQualifiedThemes(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        return new QualifiedThemeAlertResponse(
                parseLong(map.get("processoId")),
                asBoolean(map.get("autoStaySuggested")),
                asBoolean(map.get("applicationSuggested")),
                asString(map.get("recommendedNextStep")),
                readThemeMatches(map.get("matches")),
                readStringList(map.get("fundamentos"))
        );
    }

    private List<QualifiedThemeAlertResponse.ThemeMatch> readThemeMatches(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<QualifiedThemeAlertResponse.ThemeMatch> out = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            out.add(new QualifiedThemeAlertResponse.ThemeMatch(
                    asString(map.get("family")),
                    asString(map.get("codigo")),
                    asString(map.get("status")),
                    asDouble(map.get("aderencia")),
                    asBoolean(map.get("stayEligible")),
                    asString(map.get("suggestedAction")),
                    asString(map.get("ementa")),
                    asString(map.get("tese")),
                    readStringList(map.get("fundamentos"))
            ));
        }
        return List.copyOf(out);
    }

    private Map<String, Object> parseContext(String contextJson) {
        if (contextJson == null || contextJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(contextJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<String> readStringList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Objects::nonNull).map(String::valueOf).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private boolean asBoolean(Object raw) {
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }

    private List<LaianePrecedenteLiteDto> readPrecedentes(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<LaianePrecedenteLiteDto> out = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            out.add(LaianePrecedenteLiteDto.builder()
                    .id(parseLong(map.get("id")))
                    .titulo(asString(map.get("titulo")))
                    .tribunal(asString(map.get("tribunal")))
                    .tema(asString(map.get("tema")))
                    .ementa(asString(map.get("ementa")))
                    .build());
        }
        return out;
    }

    private double asDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw == null) {
            return 0d;
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (Exception e) {
            return 0d;
        }
    }

    private int asInt(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (Exception e) {
            return 0;
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @Transactional(readOnly = true)
    public String sugerirEmentaColegiada(JulgamentoColegiado julgamento, List<VotoColegiado> votos) {
        Objects.requireNonNull(julgamento, "julgamento é obrigatório");
        List<VotoColegiado> lista = votos == null ? List.of() : votos.stream().filter(Objects::nonNull).toList();
        String tribunal = firstNonBlank(julgamento.getTribunalSigla(), "TRIBUNAL");
        String orgao = firstNonBlank(julgamento.getOrgaoJulgador(), "ÓRGÃO COLEGIADO");
        String relator = firstNonBlank(julgamento.getRelatorNome(), "RELATOR");
        String numeroProcesso = julgamento.getProcesso() != null
                ? firstNonBlank(julgamento.getProcesso().getNumeroUnificado(), julgamento.getProcesso().getNumeroProcesso(), "PROCESSO SEM NÚMERO")
                : "PROCESSO SEM NÚMERO";

        Map<String, Long> frequencias = new LinkedHashMap<>();
        List<String> fundamentos = new ArrayList<>();
        for (VotoColegiado voto : lista) {
            String chave = voto.getVotoTipo() == null ? "OUTRO" : voto.getVotoTipo().name();
            frequencias.merge(chave, 1L, Long::sum);
            if (voto.getVotoResumo() != null && !voto.getVotoResumo().isBlank()) {
                String resumo = voto.getVotoResumo().trim().replaceAll("\\s+", " ");
                fundamentos.add(resumo.length() <= 180 ? resumo : resumo.substring(0, 180));
            }
        }

        String tesePredominante = frequencias.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("ANÁLISE COLEGIADA");

        String fundamentoCondensado = fundamentos.stream()
                .distinct()
                .limit(2)
                .reduce((a, b) -> a + "; " + b)
                .orElse("fundamentos em consolidação progressiva pelo colegiado");

        String placar = "placar " + Math.max(0, julgamento.getPlacarFavor() == null ? 0 : julgamento.getPlacarFavor())
                + "x" + Math.max(0, julgamento.getPlacarContra() == null ? 0 : julgamento.getPlacarContra());
        if ((julgamento.getPlacarParcial() != null && julgamento.getPlacarParcial() > 0)
                || (julgamento.getPlacarOutros() != null && julgamento.getPlacarOutros() > 0)) {
            placar = placar + ", com parciais/outros="
                    + Math.max(0, julgamento.getPlacarParcial() == null ? 0 : julgamento.getPlacarParcial())
                    + "/" + Math.max(0, julgamento.getPlacarOutros() == null ? 0 : julgamento.getPlacarOutros());
        }

        return (tribunal + " — " + orgao + " — processo " + numeroProcesso
                + ". Relatoria de " + relator
                + ". Julgamento colegiado com tendência predominante de " + tesePredominante.replace('_', ' ').toLowerCase()
                + ", " + placar
                + ". Síntese: " + fundamentoCondensado + ".")
                .replaceAll("\\s+", " ")
                .trim();
    }


}
