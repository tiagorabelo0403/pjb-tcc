package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.intelligence.QualifiedThemeAlertResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.TemaRecursoRepetitivo;
import com.tcc.pjb.backend.model.entity.judicial.TemaRepercussaoGeral;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.TemaRecursoRepetitivoRepository;
import com.tcc.pjb.backend.model.repository.TemaRepercussaoGeralRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QualifiedThemeProactiveService {

    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final TemaRecursoRepetitivoRepository temaRecursoRepetitivoRepository;
    private final TemaRepercussaoGeralRepository temaRepercussaoGeralRepository;
    private final PjbAuthorizationService authorizationService;

    public QualifiedThemeProactiveService(ProcessoRepository processoRepository,
                                          MovimentacaoProcessualRepository movimentacaoRepository,
                                          TemaRecursoRepetitivoRepository temaRecursoRepetitivoRepository,
                                          TemaRepercussaoGeralRepository temaRepercussaoGeralRepository,
                                          PjbAuthorizationService authorizationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.temaRecursoRepetitivoRepository = Objects.requireNonNull(temaRecursoRepetitivoRepository);
        this.temaRepercussaoGeralRepository = Objects.requireNonNull(temaRepercussaoGeralRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    @Transactional(readOnly = true)
    public QualifiedThemeAlertResponse analyze(Long processoId) {
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return analyze(processo);
    }

    @Transactional(readOnly = true)
    public QualifiedThemeAlertResponse analyze(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        Set<String> processTokens = tokens(buildCorpus(processo, movimentacaoRepository.findTop60ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId())));
        ArrayList<QualifiedThemeAlertResponse.ThemeMatch> matches = new ArrayList<>();
        for (TemaRecursoRepetitivo tema : temaRecursoRepetitivoRepository.findTop100ByOrderByCreatedAtDesc()) {
            addRepetitivoMatch(matches, processo, processTokens, tema);
        }
        for (TemaRepercussaoGeral tema : temaRepercussaoGeralRepository.findTop100ByOrderByCreatedAtDesc()) {
            addRepercussaoMatch(matches, processo, processTokens, tema);
        }
        List<QualifiedThemeAlertResponse.ThemeMatch> ordered = matches.stream()
                .sorted(Comparator.comparingDouble(QualifiedThemeAlertResponse.ThemeMatch::aderencia).reversed())
                .limit(8)
                .toList();
        boolean autoStay = ordered.stream().anyMatch(QualifiedThemeAlertResponse.ThemeMatch::stayEligible);
        boolean apply = ordered.stream().anyMatch(match -> containsAny(upper(match.status()), "JULGADO", "APLICADO"));
        String nextStep = apply ? "ALERTAR_MAGISTRADO_PARA_APLICACAO_DE_TEMA" : autoStay ? "ALERTAR_SOBRESTAMENTO_PREVENTIVO" : ordered.isEmpty() ? "SEM_TEMA_QUALIFICADO_FORTE" : "ALERTAR_REVISAO_QUALIFICADA";
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("Varredura lexical proativa sobre temas de repercussão geral e recursos repetitivos em vez de depender apenas de acionamento manual.");
        if (!ordered.isEmpty()) {
            fundamentos.add("Melhor aderência identificada: " + ordered.getFirst().codigo() + " com score de " + pct(ordered.getFirst().aderencia()) + '.');
        }
        if (autoStay) {
            fundamentos.add("Há sinal suficiente para sobrestamento preventivo ou análise qualificada imediata.");
        }
        return new QualifiedThemeAlertResponse(
                processo.getId(),
                autoStay,
                apply,
                nextStep,
                ordered,
                List.copyOf(fundamentos)
        );
    }

    private void addRepetitivoMatch(List<QualifiedThemeAlertResponse.ThemeMatch> out,
                                    Processo processo,
                                    Set<String> processTokens,
                                    TemaRecursoRepetitivo tema) {
        if (tema == null) {
            return;
        }
        String text = join(tema.getCodigo(), tema.getStatus(), tema.getEmenta(), tema.getTeseFirmada(), tema.getFundamentosResumo(), tema.getCriterioAfetacao());
        double score = score(processo, processTokens, text, tema.getStatus());
        if (score < 0.22d) {
            return;
        }
        boolean stayEligible = score >= 0.32d && containsAny(upper(tema.getStatus()), "AFETADO", "SOBRESTADO");
        String action = containsAny(upper(tema.getStatus()), "JULGADO", "APLICADO") ? "APLICAR_TEMA" : stayEligible ? "SOBRESTAR_OU_SUBMETER_AO_GABINETE" : "REVISAR_QUALIFICACAO";
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("Sobreposição lexical entre pedido, causa de pedir e texto do tema repetitivo.");
        if (tema.getRecursoRepresentativoProcesso() != null && Objects.equals(tema.getRecursoRepresentativoProcesso().getClasseProcessual(), processo.getClasseProcessual())) {
            fundamentos.add("Classe processual alinhada ao recurso representativo.");
        }
        out.add(new QualifiedThemeAlertResponse.ThemeMatch(
                "RECURSO_REPETITIVO",
                tema.getCodigo(),
                tema.getStatus(),
                round(score),
                stayEligible,
                action,
                tema.getEmenta(),
                tema.getTeseFirmada(),
                List.copyOf(fundamentos)
        ));
    }

    private void addRepercussaoMatch(List<QualifiedThemeAlertResponse.ThemeMatch> out,
                                     Processo processo,
                                     Set<String> processTokens,
                                     TemaRepercussaoGeral tema) {
        if (tema == null) {
            return;
        }
        String text = join(tema.getCodigo(), tema.getStatus(), tema.getModalidade(), tema.getEmenta(), tema.getTeseFirmada(), tema.getFundamentosResumo(), tema.getEfeitosProcessuais());
        double score = score(processo, processTokens, text, tema.getStatus());
        if (score < 0.22d) {
            return;
        }
        boolean stayEligible = score >= 0.32d && containsAny(upper(tema.getStatus()), "RECONHECIDO", "SOBRESTADO");
        String action = containsAny(upper(tema.getStatus()), "JULGADO", "APLICADO") ? "APLICAR_TEMA" : stayEligible ? "SOBRESTAR_OU_SUBMETER_AO_GABINETE" : "REVISAR_QUALIFICACAO";
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("Aderência entre narrativa do processo e tese qualificada da repercussão geral.");
        if (tema.getLeadingCaseProcesso() != null && Objects.equals(tema.getLeadingCaseProcesso().getClasseProcessual(), processo.getClasseProcessual())) {
            fundamentos.add("Leading case com classe processual compatível.");
        }
        out.add(new QualifiedThemeAlertResponse.ThemeMatch(
                "REPERCUSSAO_GERAL",
                tema.getCodigo(),
                tema.getStatus(),
                round(score),
                stayEligible,
                action,
                tema.getEmenta(),
                tema.getTeseFirmada(),
                List.copyOf(fundamentos)
        ));
    }

    private double score(Processo processo, Set<String> processTokens, String text, String status) {
        Set<String> themeTokens = tokens(text);
        double score = jaccard(processTokens, themeTokens);
        if (processo.getRamoDireito() != null && upper(text).contains(processo.getRamoDireito().name())) {
            score += 0.08d;
        }
        if (containsAny(upper(status), "JULGADO", "APLICADO")) {
            score += 0.03d;
        }
        return Math.min(1.0d, score);
    }

    private String buildCorpus(Processo processo, List<MovimentacaoProcessual> movimentos) {
        StringBuilder sb = new StringBuilder();
        append(sb, processo.getClasseProcessual());
        append(sb, processo.getAssunto());
        append(sb, processo.getObjetoProcessual());
        append(sb, processo.getPedidoPrincipal());
        append(sb, processo.getPedidosConsolidados());
        append(sb, processo.getResumoIA());
        for (MovimentacaoProcessual movimento : movimentos) {
            append(sb, movimento.getDescricao());
        }
        return sb.toString();
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
            return 0d;
        }
        int intersection = 0;
        for (String token : a) {
            if (b.contains(token)) {
                intersection++;
            }
        }
        int union = a.size() + b.size() - intersection;
        return union == 0 ? 0d : (double) intersection / (double) union;
    }

    private void append(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(' ');
        }
    }

    private String join(String... values) {
        StringBuilder sb = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(value.trim());
            }
        }
        return sb.toString();
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || value.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private double round(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }

    private String pct(double value) {
        return Math.round(Math.max(0d, Math.min(1d, value)) * 100d) + "%";
    }
}
