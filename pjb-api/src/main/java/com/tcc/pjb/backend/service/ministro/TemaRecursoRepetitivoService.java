package com.tcc.pjb.backend.service.ministro;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.judicial.TemaRecursoRepetitivo;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.TemaRecursoRepetitivoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processual.document.template.RecursalQualifiedDocumentMaterializerService;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class TemaRecursoRepetitivoService {

    private final TemaRecursoRepetitivoRepository repository;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService;

    public TemaRecursoRepetitivoService(TemaRecursoRepetitivoRepository repository,
                                        ProcessoRepository processoRepository,
                                        WorkItemRepository workItemRepository,
                                        CurrentUserService currentUserService,
                                        ObjectMapper objectMapper,
                                        InstitutionalActorRoutingService institutionalActorRoutingService,
                                        RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService) {
        this.repository = Objects.requireNonNull(repository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.recursalQualifiedDocumentMaterializerService = Objects.requireNonNull(recursalQualifiedDocumentMaterializerService);
    }

    @Transactional(readOnly = true)
    public List<TemaRecursoRepetitivoView> listar(String status) {
        List<TemaRecursoRepetitivo> base = status == null || status.isBlank()
                ? repository.findTop100ByOrderByCreatedAtDesc()
                : repository.findTop100ByStatusOrderByCreatedAtDesc(status.trim().toUpperCase(Locale.ROOT));
        return base.stream().map(this::toView).toList();
    }

    @Transactional
    public TemaRecursoRepetitivoView afetar(Long processoId, AfetarTemaRequest request) {
        Usuario relator = requireMagistradoSuperior();
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

        TemaRecursoRepetitivo tema = new TemaRecursoRepetitivo();
        tema.setCodigo(resolveCodigo(request.codigo(), processoId));
        tema.setTribunalSigla(resolveTribunalSigla(processo));
        tema.setStatus("AFETADO");
        tema.setEmenta(defaultText(request.ementa(), "Tema afetado como recurso repetitivo."));
        tema.setFundamentosResumo(trimToNull(request.fundamentosResumo()));
        tema.setCriterioAfetacao(defaultText(request.criterioAfetacao(), "Multiplicidade de recursos e identidade juridica relevante."));
        tema.setRecursoRepresentativoProcesso(processo);
        tema.setRelator(relator);
        tema.setProcessosRelacionadosJson(writeIds(List.of(processoId)));
        tema.setAfetadoEm(Instant.now());
        TemaRecursoRepetitivo saved = repository.save(tema);

        criarWorkItem(
                processo,
                "TEMA-REPETITIVO-AFETACAO:" + saved.getCodigo(),
                "Tema repetitivo afetado",
                "Tema " + saved.getCodigo() + " afetado com recurso representativo " + processo.getNumeroProcesso() + ".",
                TipoUsuario.ASSESSOR_MINISTRO,
                WorkItemType.RECURSO,
                0,
                Instant.now().plus(12, ChronoUnit.HOURS)
        );

        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarPronunciamentoRelatoria(
                processoId,
                "Afetação de tema repetitivo — " + saved.getCodigo() + " — " + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId())),
                firstNonBlank(request.fundamentosResumo(), request.ementa(), request.criterioAfetacao(), "Afetação de tema repetitivo."),
                "Afeto o tema repetitivo " + saved.getCodigo() + " com recurso representativo " + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId()))
                        + " e critério de afetação " + firstNonBlank(saved.getCriterioAfetacao(), "NAO_INFORMADO") + ".",
                resolveOrgaoRecursal(processo),
                "ULTIMA_INSTANCIA",
                "TEMA_REPETITIVO_AFETACAO",
                Map.of(
                        "codigoTema", safeText(saved.getCodigo()),
                        "tribunalSigla", safeText(saved.getTribunalSigla()),
                        "criterioAfetacao", safeText(saved.getCriterioAfetacao())
                )
        );

        return toView(saved, documentoFormalAssinado);
    }

    @Transactional
    public TemaRecursoRepetitivoView sobrestar(Long temaId, RelacionarProcessosRequest request) {
        requireMagistradoSuperior();
        TemaRecursoRepetitivo tema = repository.findById(temaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("TemaRecursoRepetitivo", temaId));
        List<Processo> processos = processoRepository.findAllById(request.processoIds());
        if (processos.isEmpty()) {
            throw new IllegalArgumentException("Nenhum processo encontrado para sobrestamento.");
        }
        Set<Long> relacionados = new LinkedHashSet<>(readIds(tema.getProcessosRelacionadosJson()));
        processos.stream().map(Processo::getId).filter(Objects::nonNull).forEach(relacionados::add);
        tema.setProcessosRelacionadosJson(writeIds(relacionados.stream().toList()));
        tema.setProcessosSobrestados(Math.max(processos.size(), Math.max(0, relacionados.size() - 1)));
        tema.setStatus("SOBRESTADO");
        TemaRecursoRepetitivo saved = repository.save(tema);

        for (Processo processo : processos) {
            criarWorkItem(
                    processo,
                    "TEMA-REPETITIVO-SOBRESTAR:" + saved.getCodigo() + ":" + processo.getId(),
                    "Sobrestamento por tema repetitivo",
                    "Processo vinculado ao tema " + saved.getCodigo() + " para sobrestamento preventivo.",
                    TipoUsuario.SERVIDOR_FORUM,
                    WorkItemType.DECISAO,
                    1,
                    Instant.now().plus(48, ChronoUnit.HOURS)
            );
        }
        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarPronunciamentoRelatoria(
                tema.getRecursoRepresentativoProcesso() != null ? tema.getRecursoRepresentativoProcesso().getId() : null,
                "Sobrestamento por tema repetitivo — " + saved.getCodigo(),
                firstNonBlank(saved.getFundamentosResumo(), saved.getEmenta(), "Sobrestamento de processos por tema repetitivo."),
                "Determino o sobrestamento preventivo de " + processos.size() + " processo(s) vinculado(s) ao tema " + saved.getCodigo() + ".",
                resolveOrgaoRecursal(saved.getRecursoRepresentativoProcesso()),
                "ULTIMA_INSTANCIA",
                "TEMA_REPETITIVO_SOBRESTAMENTO",
                Map.of(
                        "codigoTema", safeText(saved.getCodigo()),
                        "processosSobrestados", String.valueOf(saved.getProcessosSobrestados() == null ? 0 : saved.getProcessosSobrestados())
                )
        );
        return toView(saved, documentoFormalAssinado);
    }

    @Transactional
    public TemaRecursoRepetitivoView julgar(Long temaId, JulgarTemaRequest request) {
        requireMagistradoSuperior();
        TemaRecursoRepetitivo tema = repository.findById(temaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("TemaRecursoRepetitivo", temaId));
        tema.setStatus("JULGADO");
        tema.setTeseFirmada(defaultText(request.teseFirmada(), tema.getTeseFirmada()));
        tema.setFundamentosResumo(defaultText(request.fundamentosResumo(), tema.getFundamentosResumo()));
        tema.setEmenta(defaultText(request.ementa(), tema.getEmenta()));
        tema.setJulgadoEm(Instant.now());
        TemaRecursoRepetitivo saved = repository.save(tema);
        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarAcordao(
                saved.getRecursoRepresentativoProcesso() != null ? saved.getRecursoRepresentativoProcesso().getId() : null,
                "Julgamento de tema repetitivo — " + saved.getCodigo(),
                firstNonBlank(saved.getEmenta(), request.ementa(), "Tema repetitivo julgado em corte superior."),
                firstNonBlank(saved.getFundamentosResumo(), request.fundamentosResumo(), request.teseFirmada()),
                "Tese firmada: " + firstNonBlank(saved.getTeseFirmada(), request.teseFirmada(), "NAO_INFORMADO"),
                resolveOrgaoRecursal(saved.getRecursoRepresentativoProcesso()),
                "ULTIMA_INSTANCIA",
                "TEMA_REPETITIVO_JULGADO"
        );
        return toView(saved, documentoFormalAssinado);
    }

    @PjbTransactionalBudget(operation = "ministro.tema-recurso-repetitivo.aplicar-resultado", maxMillis = 8000)
    @Transactional
    public TemaRecursoRepetitivoView aplicarResultado(Long temaId, RelacionarProcessosRequest request) {
        requireMagistradoSuperior();
        TemaRecursoRepetitivo tema = repository.findById(temaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("TemaRecursoRepetitivo", temaId));
        if (!"JULGADO".equalsIgnoreCase(tema.getStatus()) && !"APLICADO".equalsIgnoreCase(tema.getStatus())) {
            throw new IllegalStateException("O tema precisa estar julgado antes da aplicacao automatica.");
        }
        List<Processo> processos = processoRepository.findAllById(request.processoIds());
        if (processos.isEmpty()) {
            throw new IllegalArgumentException("Nenhum processo localizado para aplicacao do tema.");
        }
        List<Long> processoIds = processos.stream().map(Processo::getId).toList();
        List<String> templateCodes = processos.stream()
                .map(processo -> "TEMA-REPETITIVO-APLICAR:" + tema.getCodigo() + ":" + processo.getId())
                .toList();
        Set<Long> processosComWorkItemAplicado = workItemRepository.findAllByProcesso_IdInAndTemplateCodeIn(processoIds, templateCodes).stream()
                .map(item -> item.getProcesso().getId())
                .collect(Collectors.toSet());
        for (Processo processo : processos) {
            String marcador = "Tema repetitivo " + tema.getCodigo() + ": " + defaultText(tema.getTeseFirmada(), tema.getEmenta());
            String anterior = trimToNull(processo.getResultadoFinal());
            processo.setResultadoFinal(anterior == null ? marcador : anterior + " | " + marcador);
            if (processosComWorkItemAplicado.contains(processo.getId())) {
                continue;
            }
            criarWorkItem(
                    processo,
                    "TEMA-REPETITIVO-APLICAR:" + tema.getCodigo() + ":" + processo.getId(),
                    "Aplicar tese repetitiva",
                    "Aplicar automaticamente a tese do tema " + tema.getCodigo() + " ao caso concreto.",
                    TipoUsuario.JUIZ,
                    WorkItemType.DECISAO,
                    1,
                    Instant.now().plus(72, ChronoUnit.HOURS)
            );
        }
        processoRepository.saveAll(processos);

        Set<Long> relacionados = new LinkedHashSet<>(readIds(tema.getProcessosRelacionadosJson()));
        processos.stream().map(Processo::getId).filter(Objects::nonNull).forEach(relacionados::add);
        tema.setProcessosRelacionadosJson(writeIds(relacionados.stream().toList()));
        tema.setProcessosAplicados((tema.getProcessosAplicados() == null ? 0 : tema.getProcessosAplicados()) + processos.size());
        tema.setAplicadoEm(Instant.now());
        tema.setStatus("APLICADO");
        TemaRecursoRepetitivo saved = repository.save(tema);
        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarPronunciamentoRelatoria(
                saved.getRecursoRepresentativoProcesso() != null ? saved.getRecursoRepresentativoProcesso().getId() : null,
                "Aplicação de tese repetitiva — " + saved.getCodigo(),
                firstNonBlank(saved.getFundamentosResumo(), saved.getEmenta(), saved.getTeseFirmada()),
                "Determino a aplicação da tese repetitiva " + firstNonBlank(saved.getCodigo(), "NAO_INFORMADO")
                        + " aos processos relacionados, com total aplicado de " + (saved.getProcessosAplicados() == null ? 0 : saved.getProcessosAplicados()) + ".",
                resolveOrgaoRecursal(saved.getRecursoRepresentativoProcesso()),
                "ULTIMA_INSTANCIA",
                "TEMA_REPETITIVO_APLICACAO",
                Map.of(
                        "codigoTema", safeText(saved.getCodigo()),
                        "teseFirmada", safeText(saved.getTeseFirmada())
                )
        );
        return toView(saved, documentoFormalAssinado);
    }

    private Usuario requireMagistradoSuperior() {
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        boolean permitido = tipo == TipoUsuario.MINISTRO
                || tipo == TipoUsuario.DESEMBARGADOR
                || tipo == TipoUsuario.DESEMBARGADOR_FEDERAL;
        if (!permitido) {
            throw new AccessDeniedPjbException("Apenas desembargadores e ministros podem operar temas repetitivos");
        }
        return usuario;
    }

    private void criarWorkItem(Processo processo,
                               String templateCode,
                               String titulo,
                               String descricao,
                               TipoUsuario assignedRole,
                               WorkItemType type,
                               int prioridade,
                               Instant dueAt) {
        if (workItemRepository.findLatestByProcessoIdAndTemplateCode(processo.getId(), templateCode).isPresent()) {
            return;
        }
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.resolveByAssignedRole(
                processo.getId(),
                assignedRole,
                normalizeRouteAxis(type, templateCode)
        );
        WorkItem workItem = WorkItem.builder()
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
                .prioridade(prioridade)
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .dueAt(dueAt)
                .build();
        workItemRepository.save(workItem);
    }

    private TemaRecursoRepetitivoView toView(TemaRecursoRepetitivo tema) {
        return toView(tema, Map.of());
    }

    private TemaRecursoRepetitivoView toView(TemaRecursoRepetitivo tema, Map<String, Object> documentoFormalAssinado) {
        return new TemaRecursoRepetitivoView(
                tema.getId(),
                tema.getCodigo(),
                tema.getTribunalSigla(),
                tema.getStatus(),
                tema.getEmenta(),
                tema.getTeseFirmada(),
                tema.getFundamentosResumo(),
                tema.getCriterioAfetacao(),
                tema.getRecursoRepresentativoProcesso() != null ? tema.getRecursoRepresentativoProcesso().getId() : null,
                tema.getRecursoRepresentativoProcesso() != null ? tema.getRecursoRepresentativoProcesso().getNumeroProcesso() : null,
                tema.getRelator() != null ? tema.getRelator().getId() : null,
                tema.getRelator() != null ? tema.getRelator().getNome() : null,
                tema.getProcessosSobrestados(),
                tema.getProcessosAplicados(),
                readIds(tema.getProcessosRelacionadosJson()),
                tema.getAfetadoEm(),
                tema.getJulgadoEm(),
                tema.getAplicadoEm(),
                tema.getCreatedAt(),
                tema.getUpdatedAt(),
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

    private String resolveCodigo(String raw, Long processoId) {
        if (raw != null && !raw.isBlank()) {
            return raw.trim().toUpperCase(Locale.ROOT);
        }
        return "RR-" + UUID.nameUUIDFromBytes(("tema-repetitivo|" + processoId + "|" + Instant.now().toEpochMilli()).getBytes(StandardCharsets.UTF_8))
                .toString()
                .substring(0, 18)
                .toUpperCase(Locale.ROOT);
    }

    private String resolveTribunalSigla(Processo processo) {
        if (processo.getJurisdicao() != null && processo.getJurisdicao().getSigla() != null && !processo.getJurisdicao().getSigla().isBlank()) {
            return processo.getJurisdicao().getSigla().trim().toUpperCase(Locale.ROOT);
        }
        return trimToNull(processo.getTribunalCodigoRoteado());
    }

    private String writeIds(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids == null ? List.of() : ids.stream().distinct().toList());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar ids de tema repetitivo", e);
        }
    }

    private List<Long> readIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readerForListOf(Long.class).readValue(json);
        } catch (Exception e) {
            return List.of();
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

    private String defaultText(String preferred, String fallback) {
        String normalized = trimToNull(preferred);
        return normalized != null ? normalized : fallback;
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public record AfetarTemaRequest(
            String codigo,
            @NotBlank String ementa,
            String fundamentosResumo,
            String criterioAfetacao
    ) {
    }

    public record RelacionarProcessosRequest(
            @NotNull List<Long> processoIds
    ) {
    }

    public record JulgarTemaRequest(
            String ementa,
            @NotBlank String teseFirmada,
            String fundamentosResumo
    ) {
    }

    public record TemaRecursoRepetitivoView(
            Long id,
            String codigo,
            String tribunalSigla,
            String status,
            String ementa,
            String teseFirmada,
            String fundamentosResumo,
            String criterioAfetacao,
            Long recursoRepresentativoProcessoId,
            String recursoRepresentativoNumero,
            Long relatorId,
            String relatorNome,
            Integer processosSobrestados,
            Integer processosAplicados,
            List<Long> processosRelacionados,
            Instant afetadoEm,
            Instant julgadoEm,
            Instant aplicadoEm,
            Instant createdAt,
            Instant updatedAt,
            Map<String, Object> documentoFormalAssinado,
            Map<String, Object> assinaturaQualificada,
            Map<String, Object> validacaoSoberana
    ) {
        public TemaRecursoRepetitivoView {
            documentoFormalAssinado = safeEnvelope(documentoFormalAssinado);
            assinaturaQualificada = safeEnvelope(assinaturaQualificada);
            validacaoSoberana = safeEnvelope(validacaoSoberana);
        }
    }
}
