package com.tcc.pjb.backend.service.ministro;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.judicial.TemaRepercussaoGeral;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.TemaRepercussaoGeralRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processual.document.template.RecursalQualifiedDocumentMaterializerService;

@Service
public class RepercussaoGeralService {

    private final TemaRepercussaoGeralRepository temaRepository;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService;

    public RepercussaoGeralService(TemaRepercussaoGeralRepository temaRepository,
                                   ProcessoRepository processoRepository,
                                   WorkItemRepository workItemRepository,
                                   CurrentUserService currentUserService,
                                   ObjectMapper objectMapper,
                                   InstitutionalActorRoutingService institutionalActorRoutingService,
                                   RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService) {
        this.temaRepository = Objects.requireNonNull(temaRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.recursalQualifiedDocumentMaterializerService = Objects.requireNonNull(recursalQualifiedDocumentMaterializerService);
    }

    @Transactional(readOnly = true)
    public List<TemaRepercussaoView> listarTemas() {
        return temaRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::toView).toList();
    }

    @Transactional
    public TemaRepercussaoView reconhecer(Long processoId, ReconhecimentoRequest request) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        Usuario relator = requireMinister();
        List<ProcessoRelacionavel> relacionados = localizarRelacionados(
                processo,
                request.corteMinimoSimilaridade(),
                request.limitProcessosRelacionadosEfetivo()
        );
        TemaRepercussaoGeral tema = new TemaRepercussaoGeral();
        tema.setCodigo(gerarCodigo(request.modalidade(), processo));
        tema.setModalidade(normalizeModalidade(request.modalidade()));
        tema.setStatus("RECONHECIDO");
        tema.setEmenta(request.ementa());
        tema.setFundamentosResumo(request.fundamentosResumo());
        tema.setProcessosSobrestados(relacionados.size());
        tema.setScoreCorte((int) Math.round(request.corteMinimoSimilaridade() * 100.0d));
        tema.setProcessosRelacionadosJson(writeProcessosJson(relacionados));
        tema.setLeadingCaseProcesso(processo);
        tema.setRelator(relator);
        tema.setReconhecidoEm(Instant.now());
        TemaRepercussaoGeral salvo = temaRepository.save(tema);

        for (ProcessoRelacionavel relacionado : relacionados) {
            Processo candidato = relacionado.processo();
            candidato.setResumoIA(mergeResumo(candidato.getResumoIA(), "Sobrestamento potencial pelo tema " + salvo.getCodigo()));
            processoRepository.save(candidato);
            criarWorkItemSeNecessario(
                    candidato,
                    "TEMA_RG_SOBRESTAR:" + salvo.getCodigo() + ":" + candidato.getId(),
                    WorkItemType.RECURSO,
                    "Sobrestamento por tema qualificado — " + salvo.getCodigo() + " — " + firstNonBlank(candidato.getNumeroUnificado(), candidato.getNumeroProcesso(), String.valueOf(candidato.getId())),
                    "Leading case: " + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId()))
                            + " | aderencia: " + percent(relacionado.score())
                            + " | modalidade: " + salvo.getModalidade(),
                    TipoUsuario.ASSESSOR_MINISTRO,
                    Instant.now().plus(72, ChronoUnit.HOURS)
            );
        }
        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarPronunciamentoRelatoria(
                processoId,
                "Reconhecimento de tema qualificado — " + salvo.getCodigo() + " — " + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId())),
                firstNonBlank(request.fundamentosResumo(), request.ementa(), "Reconhecimento de tema qualificado para racionalização recursal."),
                "Reconheço o tema qualificado " + salvo.getCodigo() + " na modalidade " + salvo.getModalidade()
                        + " com leading case " + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId()))
                        + " e potencial sobrestamento de " + relacionados.size() + " processo(s) relacionado(s).",
                resolveOrgaoRecursal(processo),
                "ULTIMA_INSTANCIA",
                "TEMA_QUALIFICADO_RECONHECIMENTO",
                Map.of(
                        "codigoTema", safeText(salvo.getCodigo()),
                        "modalidadeTema", safeText(salvo.getModalidade()),
                        "ementaTema", safeText(salvo.getEmenta())
                )
        );
        return toView(salvo, documentoFormalAssinado);
    }

    @Transactional
    public TemaRepercussaoView aplicarResultado(String codigo, JulgamentoRequest request) {
        TemaRepercussaoGeral tema = temaRepository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Tema de repercussao geral/repetitivo nao encontrado"));
        requireMinister();
        List<Long> idsRelacionados = readIds(tema.getProcessosRelacionadosJson());
        List<Processo> relacionados = idsRelacionados.isEmpty()
                ? List.of()
                : processoRepository.findAllById(idsRelacionados);

        tema.setStatus("APLICADO");
        tema.setTeseFirmada(request.teseFirmada());
        tema.setEfeitosProcessuais(request.efeitosProcessuais());
        tema.setProcessosAplicados(relacionados.size());
        tema.setJulgadoEm(Instant.now());
        tema.setAplicadoEm(Instant.now());
        TemaRepercussaoGeral salvo = temaRepository.save(tema);

        for (Processo processo : relacionados) {
            processo.setResumoIA(mergeResumo(processo.getResumoIA(), "Tema " + salvo.getCodigo() + " julgado: " + request.teseFirmada()));
            processoRepository.save(processo);
            criarWorkItemSeNecessario(
                    processo,
                    "TEMA_RG_APLICAR:" + salvo.getCodigo() + ":" + processo.getId(),
                    WorkItemType.DECISAO,
                    "Aplicar tese qualificada — " + salvo.getCodigo() + " — " + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId())),
                    request.teseFirmada(),
                    TipoUsuario.ASSESSOR_MINISTRO,
                    Instant.now().plus(48, ChronoUnit.HOURS)
            );
        }
        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarAcordao(
                tema.getLeadingCaseProcesso() != null ? tema.getLeadingCaseProcesso().getId() : null,
                "Julgamento de tema qualificado — " + salvo.getCodigo(),
                firstNonBlank(salvo.getEmenta(), request.teseFirmada(), "Tema qualificado julgado em corte superior."),
                firstNonBlank(salvo.getFundamentosResumo(), request.efeitosProcessuais(), request.teseFirmada()),
                "Tese firmada: " + request.teseFirmada() + ". Efeitos processuais: " + firstNonBlank(request.efeitosProcessuais(), "NAO_INFORMADO"),
                resolveOrgaoRecursal(tema.getLeadingCaseProcesso()),
                "ULTIMA_INSTANCIA",
                "TEMA_QUALIFICADO_JULGADO"
        );
        return toView(salvo, documentoFormalAssinado);
    }

    private Usuario requireMinister() {
        Usuario usuario = currentUserService.getRequired();
        if (usuario.getTipoUsuario() != TipoUsuario.MINISTRO) {
            throw new IllegalStateException("Operacao exclusiva de ministro.");
        }
        return usuario;
    }

    private List<ProcessoRelacionavel> localizarRelacionados(Processo leadingCase, double corte, int limite) {
        Set<String> leadingTokens = tokensOf(leadingCase);
        if (leadingTokens.isEmpty()) {
            return List.of();
        }
        double effectiveCorte = Math.max(0.55d, Math.min(0.98d, corte));
        int effectiveLimit = Math.max(1, Math.min(500, limite));
        List<ProcessoRelacionavel> relacionados = new ArrayList<>();
        List<Processo> candidatos = processoRepository.findComparableCases(
                leadingCase.getId(),
                firstNonBlank(leadingCase.getTribunal(), leadingCase.getJurisdicao() == null ? null : leadingCase.getJurisdicao().getCodigo(), leadingCase.getJurisdicao() == null ? null : leadingCase.getJurisdicao().getSigla()),
                leadingCase.getClasseProcessual(),
                leadingCase.getAssunto(),
                PageRequest.of(0, candidateScanLimit(effectiveLimit), Sort.by(Sort.Order.desc("dataUltimaMovimentacao"), Sort.Order.desc("id")))
        );
        for (Processo candidato : candidatos) {
            if (candidato.getId() == null || candidato.getId().equals(leadingCase.getId())) {
                continue;
            }
            if (leadingCase.getRamoDireito() != null && candidato.getRamoDireito() != null && leadingCase.getRamoDireito() != candidato.getRamoDireito()) {
                continue;
            }
            double score = calcularSimilaridade(leadingTokens, tokensOf(candidato));
            if (score >= effectiveCorte) {
                relacionados.add(new ProcessoRelacionavel(candidato, score));
            }
        }
        return relacionados.stream()
                .sorted(Comparator.comparingDouble(ProcessoRelacionavel::score).reversed())
                .limit(effectiveLimit)
                .toList();
    }

    private int candidateScanLimit(int effectiveLimit) {
        return Math.max(200, Math.min(1600, effectiveLimit * 8));
    }

    private void criarWorkItemSeNecessario(Processo processo,
                                           String templateCode,
                                           WorkItemType type,
                                           String titulo,
                                           String descricao,
                                           TipoUsuario assignedRole,
                                           Instant dueAt) {
        boolean existe = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(
                processo.getId(),
                templateCode,
                WorkItemStatus.CANCELADO
        ).isPresent();
        if (existe) {
            return;
        }
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.resolveByAssignedRole(
                processo.getId(),
                assignedRole,
                normalizeRouteAxis(type, templateCode)
        );
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(templateCode)
                .type(type)
                .titulo(titulo)
                .descricao(descricao)
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .uf(processo.getJurisdicao() != null ? processo.getJurisdicao().getUf() : null)
                .comarca(processo.getJurisdicao() != null ? processo.getJurisdicao().getComarca() : null)
                .dueAt(dueAt)
                .build();
        workItemRepository.save(item);
    }

    private TemaRepercussaoView toView(TemaRepercussaoGeral tema) {
        return toView(tema, Map.of());
    }

    private TemaRepercussaoView toView(TemaRepercussaoGeral tema, Map<String, Object> documentoFormalAssinado) {
        return new TemaRepercussaoView(
                tema.getId(),
                tema.getCodigo(),
                tema.getModalidade(),
                tema.getStatus(),
                tema.getLeadingCaseProcesso() != null ? tema.getLeadingCaseProcesso().getId() : null,
                tema.getLeadingCaseProcesso() != null ? tema.getLeadingCaseProcesso().getNumeroProcesso() : null,
                tema.getRelator() != null ? tema.getRelator().getId() : null,
                tema.getRelator() != null ? tema.getRelator().getNome() : null,
                tema.getScoreCorte(),
                tema.getProcessosSobrestados(),
                tema.getProcessosAplicados(),
                tema.getEmenta(),
                tema.getTeseFirmada(),
                tema.getEfeitosProcessuais(),
                tema.getFundamentosResumo(),
                tema.getReconhecidoEm(),
                tema.getJulgadoEm(),
                tema.getAplicadoEm(),
                readIds(tema.getProcessosRelacionadosJson()),
                safeEnvelope(documentoFormalAssinado),
                safeEnvelope(castMap(documentoFormalAssinado.get("assinaturaQualificada"))),
                safeEnvelope(castMap(documentoFormalAssinado.get("validacaoSoberana")))
        );
    }

    private String resolveOrgaoRecursal(Processo processo) {
        if (processo == null) {
            return "CORTE_SUPERIOR";
        }
        return firstNonBlank(processo.getTribunal(), processo.getNumeroProcesso(), "CORTE_SUPERIOR");
    }

    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map && !map.isEmpty()) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> {
                if (key != null && mapValue != null) {
                    out.put(String.valueOf(key), mapValue);
                }
            });
            return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
        }
        return Map.of();
    }

    private static Map<String, Object> safeEnvelope(Map<String, Object> value) {
        return value == null || value.isEmpty() ? Map.of() : Map.copyOf(value);
    }

    private String safeText(String value) {
        String normalized = firstNonBlank(value);
        return normalized == null ? "NAO_INFORMADO" : normalized;
    }

    private String writeProcessosJson(List<ProcessoRelacionavel> relacionados) {
        List<Long> ids = relacionados.stream().map(ProcessoRelacionavel::processo).map(Processo::getId).filter(Objects::nonNull).distinct().toList();
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar processos relacionados do tema", e);
        }
    }

    private List<Long> readIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            Long[] ids = objectMapper.readValue(json, Long[].class);
            return ids == null ? List.of() : List.of(ids);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String gerarCodigo(String modalidade, Processo processo) {
        String prefixo = normalizeModalidade(modalidade).startsWith("RECURSO") ? "RPT" : "RG";
        String base = prefixo + ":" + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId())) + ":" + Instant.now().toEpochMilli();
        return prefixo + "-" + UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString().substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private double calcularSimilaridade(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0d;
        }
        int intersecao = 0;
        for (String token : a) {
            if (b.contains(token)) {
                intersecao++;
            }
        }
        int uniao = a.size() + b.size() - intersecao;
        return uniao == 0 ? 0.0d : (double) intersecao / (double) uniao;
    }

    private Set<String> tokensOf(Processo processo) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        appendTokens(tokens, processo.getAssunto());
        appendTokens(tokens, processo.getObjetoProcessual());
        appendTokens(tokens, processo.getPedidoPrincipal());
        appendTokens(tokens, processo.getPedidosConsolidados());
        appendTokens(tokens, processo.getResumoIA());
        appendTokens(tokens, processo.getClasseProcessual());
        appendTokens(tokens, processo.getParteAutoraNome());
        appendTokens(tokens, processo.getParteReuNome());
        return tokens;
    }

    private void appendTokens(Set<String> bucket, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String[] partes = value.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9ÁÀÂÃÉÈÊÍÌÎÓÒÔÕÚÙÛÇ ]", " ")
                .split("\\s+");
        for (String parte : partes) {
            String token = parte.trim();
            if (token.length() >= 4) {
                bucket.add(token);
            }
        }
    }

    private String mergeResumo(String atual, String complemento) {
        if (atual == null || atual.isBlank()) {
            return complemento;
        }
        if (complemento == null || complemento.isBlank() || atual.contains(complemento)) {
            return atual;
        }
        return atual + " | " + complemento;
    }

    private String normalizeRouteAxis(WorkItemType type, String templateCode) {
        if (type != null) {
            return type.name();
        }
        String raw = templateCode == null ? "WORK_ITEM" : templateCode;
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? "WORK_ITEM" : normalized;
    }

    private String normalizeModalidade(String value) {
        String normalized = value == null ? "REPERCUSSAO_GERAL" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "REPETITIVO", "RECURSO_REPETITIVO", "RECURSOS_REPETITIVOS" -> "RECURSO_REPETITIVO";
            default -> "REPERCUSSAO_GERAL";
        };
    }

    private String percent(double score) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", score * 100.0d);
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

    public record ProcessoRelacionavel(Processo processo, double score) {
    }

    public record TemaRepercussaoView(
            Long id,
            String codigo,
            String modalidade,
            String status,
            Long leadingCaseProcessoId,
            String leadingCaseNumero,
            Long relatorId,
            String relatorNome,
            Integer scoreCorte,
            Integer processosSobrestados,
            Integer processosAplicados,
            String ementa,
            String teseFirmada,
            String efeitosProcessuais,
            String fundamentosResumo,
            Instant reconhecidoEm,
            Instant julgadoEm,
            Instant aplicadoEm,
            List<Long> processosRelacionados,
            Map<String, Object> documentoFormalAssinado,
            Map<String, Object> assinaturaQualificada,
            Map<String, Object> validacaoSoberana
    ) {
        public TemaRepercussaoView {
            documentoFormalAssinado = safeEnvelope(documentoFormalAssinado);
            assinaturaQualificada = safeEnvelope(assinaturaQualificada);
            validacaoSoberana = safeEnvelope(validacaoSoberana);
        }
    }

    public record ReconhecimentoRequest(
            @NotBlank String modalidade,
            @NotBlank String ementa,
            String fundamentosResumo,
            @Min(1) @Max(500) Integer limitProcessosRelacionados,
            @Min(55) @Max(99) Integer corteMinimoSimilaridadePercent
    ) {
        public int limitProcessosRelacionadosEfetivo() {
            return limitProcessosRelacionados == null ? 200 : limitProcessosRelacionados;
        }

        public double corteMinimoSimilaridade() {
            int percentual = corteMinimoSimilaridadePercent == null ? 85 : corteMinimoSimilaridadePercent;
            return percentual / 100.0d;
        }
    }

    public record JulgamentoRequest(
            @NotBlank String teseFirmada,
            String efeitosProcessuais
    ) {
    }
}
