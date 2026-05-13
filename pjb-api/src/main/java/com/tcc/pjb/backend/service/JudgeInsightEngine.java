package com.tcc.pjb.backend.service;

import com.tcc.pjb.backend.model.dto.JudgeProfile;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class JudgeInsightEngine {

    private final ProcessoRepository processoRepository;

    public JudgeInsightEngine(ProcessoRepository processoRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    public JudgeProfile analyze(Long juizId) {
        long safeId = juizId == null ? 0L : juizId;
        return new JudgeProfile(safeId, "MEDIA", 0.74d, List.of("Cláusula de Quitação Balanceada", "Calendário objetivo de cumprimento"));
    }

    public JudgeProfile analyze(Processo processo) {
        if (processo == null) {
            return analyze((Long) null);
        }
        List<Processo> comparaveis = processoRepository.findComparableCases(
                processo.getId(),
                processo.getTribunal(),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                PageRequest.of(0, 120)
        );
        int total = 0;
        int homologados = 0;
        int recursais = 0;
        int linguagemFormal = 0;
        for (Processo caso : comparaveis) {
            if (caso == null || caso.getId() == null || Objects.equals(caso.getId(), processo.getId())) {
                continue;
            }
            total++;
            String resultado = upper(caso.getResultadoFinal());
            if (containsAny(resultado, "ACORDO", "HOMOLOG", "CONCILIACAO", "MEDIACAO")) {
                homologados++;
            }
            if (containsAny(upper(caso.getPreventionMode()), "RECURSO", "RECORR", "EMBARGO")
                    || containsAny(upper(caso.getRoutingRiskLevel()), "ALTO", "CRITICO")) {
                recursais++;
            }
            String corpus = upper(join(caso.getClasseProcessual(), caso.getAssunto(), caso.getResumoIA(), caso.getObjetoProcessual()));
            if (containsAny(corpus, "TUTELA", "URG", "LIMINAR", "DECISAO", "FUNDAMENT")) {
                linguagemFormal++;
            }
        }
        double taxaHomologacao = total == 0 ? 0.74d : clamp(homologados / (double) total);
        double taxaRecursal = total == 0 ? 0.32d : clamp(recursais / (double) total);
        double formalidade = total == 0 ? 0.55d : clamp(linguagemFormal / (double) total);
        String tendenciaFormalidade = formalidade >= 0.70d ? "ALTA" : formalidade >= 0.45d ? "MEDIA" : "CONTROLADA";
        ArrayList<String> clausulas = new ArrayList<>();
        clausulas.add("Calendário objetivo de cumprimento");
        clausulas.add("Mora com gatilho verificável");
        if (taxaHomologacao >= 0.65d) {
            clausulas.add("Cláusula de quitação progressiva");
        }
        if (taxaRecursal >= 0.45d) {
            clausulas.add("Matriz de inadimplemento e reforço de executividade");
        }
        if (containsAny(upper(join(processo.getAssunto(), processo.getPedidoPrincipal())), "TRABALH", "SALAR", "VERBA")) {
            clausulas.add("Parcelamento com memória de cálculo fechada");
        }
        if (containsAny(upper(join(processo.getAssunto(), processo.getPedidoPrincipal())), "CONSUM", "INDENIZ", "DANO MORAL")) {
            clausulas.add("Obrigação de fazer com prazo e multa parametrizada");
        }
        return new JudgeProfile(
                processo.getId(),
                tendenciaFormalidade,
                round(taxaHomologacao),
                clausulas.stream().distinct().limit(5).toList()
        );
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

    private String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private double round(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }
}
