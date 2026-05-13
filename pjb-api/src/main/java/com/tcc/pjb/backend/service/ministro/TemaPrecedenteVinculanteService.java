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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.judicial.TemaPrecedenteVinculante;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.TemaPrecedenteVinculanteRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processual.document.template.RecursalQualifiedDocumentMaterializerService;

@Service
public class TemaPrecedenteVinculanteService {

    private final TemaPrecedenteVinculanteRepository temaRepository;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final CurrentUserService currentUserService;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService;

    public TemaPrecedenteVinculanteService(TemaPrecedenteVinculanteRepository temaRepository,
                                           ProcessoRepository processoRepository,
                                           WorkItemRepository workItemRepository,
                                           CurrentUserService currentUserService,
                                           InstitutionalActorRoutingService institutionalActorRoutingService,
                                           RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService) {
        this.temaRepository = Objects.requireNonNull(temaRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.recursalQualifiedDocumentMaterializerService = Objects.requireNonNull(recursalQualifiedDocumentMaterializerService);
    }

    @Transactional(readOnly = true)
    public List<TemaPrecedenteView> listarTemas() {
        return temaRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::toView).toList();
    }

    @Transactional
    public TemaPrecedenteView reconhecer(Long processoId, TemaPrecedenteReconhecimentoRequest request) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        Usuario relator = requireMinister();
        List<ProcessoRelacionavel> relacionados = localizarRelacionados(processo, request.corteMinimoSimilaridade(), request.limitProcessosRelacionados());

        TemaPrecedenteVinculante tema = new TemaPrecedenteVinculante();
        tema.setCodigo(gerarCodigo(request.tipo(), processo));
        tema.setTipo(normalizeUpper(request.tipo()));
        tema.setStatus("RECONHECIDO");
        tema.setEmenta(request.ementa());
        tema.setAbrangencia(normalizeUpper(request.abrangencia()));
        tema.setScoreCorte((int) Math.round(request.corteMinimoSimilaridade() * 100.0));
        tema.setFundamentosResumo(request.fundamentosResumo());
        tema.setProcessosSobrestados(relacionados.size());
        tema.setLeadingCaseProcesso(processo);
        tema.setRelator(relator);
        TemaPrecedenteVinculante salvo = temaRepository.save(tema);

        for (ProcessoRelacionavel relacionado : relacionados) {
            criarWorkItemSeNecessario(
                    relacionado.processo(),
                    "TEMA_SOBRESTADO:" + salvo.getCodigo() + ":" + relacionado.processo().getId(),
                    WorkItemType.RECURSO,
                    "Tema vinculante reconhecido — " + salvo.getCodigo() + " — " + relacionado.processo().getNumeroProcesso(),
                    "Leading case: " + processo.getNumeroProcesso() + " | Similaridade: " + percent(relacionado.score()),
                    TipoUsuario.ASSESSOR_MINISTRO,
                    Instant.now().plus(72, ChronoUnit.HOURS)
            );
        }
        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarPronunciamentoRelatoria(
                processoId,
                "Reconhecimento de precedente vinculante — " + salvo.getCodigo() + " — " + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId())),
                firstNonBlank(request.fundamentosResumo(), request.ementa(), "Reconhecimento de precedente vinculante."),
                "Reconheço o precedente vinculante " + salvo.getCodigo() + " com abrangência " + firstNonBlank(salvo.getAbrangencia(), "NACIONAL")
                        + " e potencial incidência sobre " + relacionados.size() + " processo(s) relacionado(s).",
                resolveOrgaoRecursal(processo),
                "ULTIMA_INSTANCIA",
                "PRECEDENTE_VINCULANTE_RECONHECIMENTO",
                Map.of(
                        "codigoTema", safeText(salvo.getCodigo()),
                        "tipoTema", safeText(salvo.getTipo()),
                        "abrangenciaTema", safeText(salvo.getAbrangencia())
                )
        );
        return toView(salvo, documentoFormalAssinado);
    }

    @Transactional
    public TemaPrecedenteView aplicarResultado(String codigo, TemaPrecedenteAplicacaoRequest request) {
        TemaPrecedenteVinculante tema = temaRepository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Tema precedente nao encontrado"));
        requireMinister();
        Processo leadingCase = tema.getLeadingCaseProcesso();
        List<ProcessoRelacionavel> relacionados = leadingCase == null
                ? List.of()
                : localizarRelacionados(leadingCase, tema.getScoreCorte() == null ? 0.85 : tema.getScoreCorte() / 100.0d, request.limitProcessosAplicacao());

        tema.setTeseFirmada(request.teseFirmada());
        tema.setEfeitosProcessuais(request.efeitosProcessuais());
        tema.setStatus("APLICADO");
        tema.setJulgadoEm(Instant.now());
        tema.setAplicadoEm(Instant.now());
        tema.setProcessosAplicados(relacionados.size());
        TemaPrecedenteVinculante salvo = temaRepository.save(tema);

        for (ProcessoRelacionavel relacionado : relacionados) {
            Processo processo = relacionado.processo();
            criarWorkItemSeNecessario(
                    processo,
                    "TEMA_APLICADO:" + salvo.getCodigo() + ":" + processo.getId(),
                    WorkItemType.DECISAO,
                    "Aplicar tese vinculante — " + salvo.getCodigo() + " — " + processo.getNumeroProcesso(),
                    request.teseFirmada(),
                    TipoUsuario.ASSESSOR_MINISTRO,
                    Instant.now().plus(48, ChronoUnit.HOURS)
            );
            processo.setResumoIA(mergeResumo(processo.getResumoIA(), "Tema " + salvo.getCodigo() + " aplicado: " + request.teseFirmada()));
            processoRepository.save(processo);
        }
        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarAcordao(
                leadingCase == null ? null : leadingCase.getId(),
                "Aplicação de precedente vinculante — " + salvo.getCodigo(),
                firstNonBlank(salvo.getEmenta(), request.teseFirmada(), "Precedente vinculante aplicado em corte superior."),
                firstNonBlank(salvo.getFundamentosResumo(), request.efeitosProcessuais(), request.teseFirmada()),
                "Tese firmada: " + firstNonBlank(request.teseFirmada(), salvo.getTeseFirmada(), "NAO_INFORMADO")
                        + ". Efeitos processuais: " + firstNonBlank(request.efeitosProcessuais(), "NAO_INFORMADO"),
                resolveOrgaoRecursal(leadingCase),
                "ULTIMA_INSTANCIA",
                "PRECEDENTE_VINCULANTE_APLICADO"
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
        Set<String> tokensLeading = tokensOf(leadingCase);
        if (tokensLeading.isEmpty()) {
            return List.of();
        }
        double effectiveCorte = Math.max(0.55d, Math.min(0.98d, corte));
        int effectiveLimit = Math.max(1, Math.min(200, limite));
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
            double score = calcularSimilaridade(tokensLeading, tokensOf(candidato));
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
        boolean existe = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processo.getId(), templateCode, WorkItemStatus.CANCELADO).isPresent();
        if (existe) {
            return;
        }
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.resolveByAssignedRole(
                processo.getId(),
                assignedRole,
                normalizeUpper(type == null ? templateCode : type.name())
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

    private TemaPrecedenteView toView(TemaPrecedenteVinculante tema) {
        return toView(tema, Map.of());
    }

    private TemaPrecedenteView toView(TemaPrecedenteVinculante tema, Map<String, Object> documentoFormalAssinado) {
        return new TemaPrecedenteView(
                tema.getId(),
                tema.getCodigo(),
                tema.getTipo(),
                tema.getStatus(),
                tema.getLeadingCaseProcesso() != null ? tema.getLeadingCaseProcesso().getId() : null,
                tema.getLeadingCaseProcesso() != null ? tema.getLeadingCaseProcesso().getNumeroProcesso() : null,
                tema.getRelator() != null ? tema.getRelator().getId() : null,
                tema.getRelator() != null ? tema.getRelator().getNome() : null,
                tema.getAbrangencia(),
                tema.getScoreCorte(),
                tema.getProcessosSobrestados(),
                tema.getProcessosAplicados(),
                tema.getEmenta(),
                tema.getTeseFirmada(),
                tema.getEfeitosProcessuais(),
                tema.getFundamentosResumo(),
                tema.getCreatedAt(),
                tema.getJulgadoEm(),
                tema.getAplicadoEm(),
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

    private String gerarCodigo(String tipo, Processo processo) {
        String prefixo = normalizeUpper(tipo).startsWith("REPET") ? "RPT" : "RG";
        String base = prefixo + ":" + (processo.getNumeroProcesso() == null ? processo.getId() : processo.getNumeroProcesso()) + ":" + Instant.now().toEpochMilli();
        return prefixo + "-" + UUID.nameUUIDFromBytes(base.getBytes(StandardCharsets.UTF_8)).toString().substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private double calcularSimilaridade(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0d;
        }
        int intersecao = 0;
        for (String item : a) {
            if (b.contains(item)) {
                intersecao++;
            }
        }
        int uniao = a.size() + b.size() - intersecao;
        return uniao == 0 ? 0.0d : (double) intersecao / (double) uniao;
    }

    private Set<String> tokensOf(Processo processo) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        appendTokens(tokens, processo.getNumeroProcesso());
        appendTokens(tokens, processo.getAssunto());
        appendTokens(tokens, processo.getObjetoProcessual());
        appendTokens(tokens, processo.getPedidoPrincipal());
        appendTokens(tokens, processo.getPedidosConsolidados());
        appendTokens(tokens, processo.getParteAutoraNome());
        appendTokens(tokens, processo.getParteReuNome());
        appendTokens(tokens, processo.getResumoIA());
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

    private String percent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0d);
    }

    private String mergeResumo(String atual, String adicao) {
        if (atual == null || atual.isBlank()) {
            return adicao;
        }
        if (atual.contains(adicao)) {
            return atual;
        }
        return atual + " | " + adicao;
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

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record ProcessoRelacionavel(Processo processo, double score) {
    }

    public record TemaPrecedenteReconhecimentoRequest(
            String tipo,
            String ementa,
            String abrangencia,
            String fundamentosResumo,
            double corteMinimoSimilaridade,
            int limitProcessosRelacionados
    ) {
        public TemaPrecedenteReconhecimentoRequest {
            tipo = tipo == null || tipo.isBlank() ? "REPERCUSSAO_GERAL" : tipo;
            abrangencia = abrangencia == null || abrangencia.isBlank() ? "NACIONAL" : abrangencia;
            corteMinimoSimilaridade = corteMinimoSimilaridade <= 0.0d ? 0.86d : corteMinimoSimilaridade;
            limitProcessosRelacionados = limitProcessosRelacionados <= 0 ? 200 : limitProcessosRelacionados;
        }
    }

    public record TemaPrecedenteAplicacaoRequest(
            String teseFirmada,
            String efeitosProcessuais,
            int limitProcessosAplicacao
    ) {
        public TemaPrecedenteAplicacaoRequest {
            limitProcessosAplicacao = limitProcessosAplicacao <= 0 ? 200 : limitProcessosAplicacao;
        }
    }

    public record TemaPrecedenteView(
            Long id,
            String codigo,
            String tipo,
            String status,
            Long leadingCaseProcessoId,
            String leadingCaseNumero,
            Long relatorId,
            String relatorNome,
            String abrangencia,
            Integer scoreCorte,
            Integer processosSobrestados,
            Integer processosAplicados,
            String ementa,
            String teseFirmada,
            String efeitosProcessuais,
            String fundamentosResumo,
            Instant createdAt,
            Instant julgadoEm,
            Instant aplicadoEm,
            Map<String, Object> documentoFormalAssinado,
            Map<String, Object> assinaturaQualificada,
            Map<String, Object> validacaoSoberana
    ) {
        public TemaPrecedenteView {
            documentoFormalAssinado = safeEnvelope(documentoFormalAssinado);
            assinaturaQualificada = safeEnvelope(assinaturaQualificada);
            validacaoSoberana = safeEnvelope(validacaoSoberana);
        }
    }
}
