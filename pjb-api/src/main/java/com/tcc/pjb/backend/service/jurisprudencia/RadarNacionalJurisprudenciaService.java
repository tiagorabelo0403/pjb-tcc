package com.tcc.pjb.backend.service.jurisprudencia;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.ai.jurimetria.JurimetriaService;
import com.tcc.pjb.backend.ai.jurimetria.model.JurimetriaReport;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.TemaPrecedenteVinculante;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.TemaPrecedenteVinculanteRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.jurisprudencia.graph.CitationExtractor;
import com.tcc.pjb.backend.service.jurisprudencia.graph.CitationRef;

@Service
public class RadarNacionalJurisprudenciaService {

    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final TemaPrecedenteVinculanteRepository temaRepository;
    private final CitationExtractor citationExtractor;
    private final JurimetriaService jurimetriaService;
    private final PjbAuthorizationService authorizationService;

    public RadarNacionalJurisprudenciaService(ProcessoRepository processoRepository,
                                              MovimentacaoProcessualRepository movimentacaoRepository,
                                              TemaPrecedenteVinculanteRepository temaRepository,
                                              CitationExtractor citationExtractor,
                                              JurimetriaService jurimetriaService,
                                              PjbAuthorizationService authorizationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.temaRepository = Objects.requireNonNull(temaRepository);
        this.citationExtractor = Objects.requireNonNull(citationExtractor);
        this.jurimetriaService = Objects.requireNonNull(jurimetriaService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    @Transactional(readOnly = true)
    public RadarJurisprudenciaView analisar(Long processoId) {
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);

        List<MovimentacaoProcessual> movimentos = movimentacaoRepository.findTop60ByProcesso_IdOrderByDataMovimentacaoDesc(processoId);
        String corpus = buildCorpus(processo, movimentos);
        List<CitationRef> citacoes = citationExtractor.extract(corpus);
        List<TemaRadarView> temas = temaRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(tema -> toTemaView(tema, processo, corpus))
                .filter(view -> view.scoreAderencia() >= 0.28d)
                .sorted(Comparator.comparingDouble(TemaRadarView::scoreAderencia).reversed())
                .limit(12)
                .toList();
        JurimetriaReport jurimetria = jurimetriaService.gerarRelatorio(
                processo.getPedidoPrincipal(),
                processo.getTribunal(),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                Map.of("ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : "CIVIL")
        );
        GrafoPrecedenteView grafo = buildGraph(citacoes, temas);
        double aderenciaMedia = temas.isEmpty() ? 0.0d : temas.stream().mapToDouble(TemaRadarView::scoreAderencia).average().orElse(0.0d);
        return new RadarJurisprudenciaView(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                round(aderenciaMedia),
                temas,
                citacoes.stream().map(ref -> new CitationView(ref.relation().name(), ref.targetType().name(), ref.targetRef(), ref.raw())).toList(),
                grafo,
                jurimetria.getIndicadores().stream().map(ind -> new JurimetriaIndicadorView(ind.getNome(), ind.getValor(), ind.getUnidade())).toList(),
                jurimetria.getObservacoes(),
                Instant.now()
        );
    }

    private TemaRadarView toTemaView(TemaPrecedenteVinculante tema, Processo processo, String corpus) {
        Set<String> processoTokens = tokens(corpus);
        String temaTexto = String.join(" ", nonBlank(tema.getCodigo()), nonBlank(tema.getTipo()), nonBlank(tema.getEmenta()), nonBlank(tema.getTeseFirmada()), nonBlank(tema.getFundamentosResumo()), nonBlank(tema.getAbrangencia()));
        Set<String> temaTokens = tokens(temaTexto);
        double score = jaccard(processoTokens, temaTokens);
        if (processo.getRamoDireito() != null && temaTexto.toUpperCase(Locale.ROOT).contains(processo.getRamoDireito().name())) {
            score += 0.08d;
        }
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Sobreposição lexical entre causa de pedir, pedido e texto do tema vinculante.");
        if (tema.getLeadingCaseProcesso() != null && processo.getClasseProcessual() != null && processo.getClasseProcessual().equalsIgnoreCase(tema.getLeadingCaseProcesso().getClasseProcessual())) {
            fundamentos.add("Classe processual alinhada ao leading case.");
            score += 0.05d;
        }
        if (tema.getStatus() != null && tema.getStatus().equalsIgnoreCase("APLICADO")) {
            fundamentos.add("Tema já aplicado e pronto para replicação decisional.");
            score += 0.04d;
        }
        return new TemaRadarView(
                tema.getCodigo(),
                tema.getTipo(),
                tema.getStatus(),
                round(Math.min(1.0d, score)),
                tema.getEmenta(),
                tema.getTeseFirmada(),
                List.copyOf(fundamentos),
                tema.getLeadingCaseProcesso() != null ? tema.getLeadingCaseProcesso().getNumeroProcesso() : null
        );
    }

    private GrafoPrecedenteView buildGraph(List<CitationRef> citacoes, List<TemaRadarView> temas) {
        ArrayList<GraphNodeView> nodes = new ArrayList<>();
        ArrayList<GraphEdgeView> edges = new ArrayList<>();
        nodes.add(new GraphNodeView("PROCESSO", "PROCESSO_EM_ANALISE", "PROCESSO"));
        LinkedHashMap<String, String> targetToNodeId = new LinkedHashMap<>();
        int nodeSequence = 1;
        for (CitationRef ref : citacoes) {
            String nodeId = "CIT-" + nodeSequence++;
            targetToNodeId.put(ref.targetRef(), nodeId);
            nodes.add(new GraphNodeView(nodeId, ref.targetRef(), ref.targetType().name()));
            edges.add(new GraphEdgeView("PROCESSO", nodeId, ref.relation().name()));
        }
        for (TemaRadarView tema : temas) {
            String nodeId = targetToNodeId.computeIfAbsent(tema.codigo(), key -> "TEMA-" + key.replaceAll("[^A-Za-z0-9]", ""));
            nodes.add(new GraphNodeView(nodeId, tema.codigo(), "TEMA_PRECEDENTE"));
            edges.add(new GraphEdgeView("PROCESSO", nodeId, "ADERENCIA_" + Math.round(tema.scoreAderencia() * 100)));
        }
        return new GrafoPrecedenteView(List.copyOf(distinctNodes(nodes)), List.copyOf(edges));
    }

    private List<GraphNodeView> distinctNodes(List<GraphNodeView> nodes) {
        LinkedHashMap<String, GraphNodeView> map = new LinkedHashMap<>();
        for (GraphNodeView node : nodes) {
            map.putIfAbsent(node.id(), node);
        }
        return new ArrayList<>(map.values());
    }

    private String buildCorpus(Processo processo, List<MovimentacaoProcessual> movimentos) {
        StringBuilder sb = new StringBuilder();
        append(sb, processo.getClasseProcessual());
        append(sb, processo.getAssunto());
        append(sb, processo.getPedidoPrincipal());
        append(sb, processo.getPedidosConsolidados());
        append(sb, processo.getResumoIA());
        for (MovimentacaoProcessual movimento : movimentos) {
            append(sb, movimento.getDescricao());
        }
        return sb.toString();
    }

    private void append(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(' ');
        }
    }

    private String nonBlank(String value) {
        return value == null ? "" : value;
    }

    private Set<String> tokens(String source) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (source == null || source.isBlank()) {
            return out;
        }
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .toUpperCase(Locale.ROOT);
        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 4) {
                out.add(token);
            }
        }
        return out;
    }

    private double jaccard(Set<String> a, Set<String> b) {
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

    private double round(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }

    public record RadarJurisprudenciaView(
            Long processoId,
            String numeroProcesso,
            String classeProcessual,
            String assunto,
            double aderenciaMedia,
            List<TemaRadarView> temas,
            List<CitationView> citacoesExtraidas,
            GrafoPrecedenteView grafo,
            List<JurimetriaIndicadorView> jurimetria,
            List<String> observacoesJurimetricas,
            Instant geradoEm
    ) {
    }

    public record TemaRadarView(
            String codigo,
            String tipo,
            String status,
            double scoreAderencia,
            String ementa,
            String teseFirmada,
            List<String> fundamentos,
            String leadingCaseNumero
    ) {
    }

    public record CitationView(
            String relation,
            String targetType,
            String targetRef,
            String raw
    ) {
    }

    public record GrafoPrecedenteView(
            List<GraphNodeView> nodes,
            List<GraphEdgeView> edges
    ) {
    }

    public record GraphNodeView(
            String id,
            String label,
            String type
    ) {
    }

    public record GraphEdgeView(
            String from,
            String to,
            String label
    ) {
    }

    public record JurimetriaIndicadorView(
            String nome,
            double valor,
            String unidade
    ) {
    }
}
