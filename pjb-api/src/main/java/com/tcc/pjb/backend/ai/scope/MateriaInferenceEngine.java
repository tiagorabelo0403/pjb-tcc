package com.tcc.pjb.backend.ai.scope;

import java.util.*;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.shared.text.TextTokenUtils;
import java.util.Locale;

@Service
public class MateriaInferenceEngine {

    private static final EnumMap<MateriaJurisdicao, List<Term>> LEXICON = buildLexicon();

    public MateriaDecision infer(String... inputs) {
        String corpus = buildCorpus(inputs);
        if (corpus.isBlank()) {
            return new MateriaDecision(MateriaJurisdicao.MULTIMATERIA, 0.0, List.of());
        }

        List<String> tokens = new ArrayList<>(TextTokenUtils.tokens(corpus));
        EnumMap<MateriaJurisdicao, Integer> scores = new EnumMap<>(MateriaJurisdicao.class);
        EnumMap<MateriaJurisdicao, LinkedHashSet<String>> matched = new EnumMap<>(MateriaJurisdicao.class);

        for (MateriaJurisdicao m : MateriaJurisdicao.values()) {
            scores.put(m, 0);
            matched.put(m, new LinkedHashSet<>());
        }

        String lc = corpus.toLowerCase(Locale.ROOT);
        for (Map.Entry<MateriaJurisdicao, List<Term>> e : LEXICON.entrySet()) {
            MateriaJurisdicao m = e.getKey();
            int s = 0;
            for (Term t : e.getValue()) {
                boolean hit = t.matchMode == MatchMode.CONTAINS
                        ? lc.contains(t.term)
                        : tokens.contains(t.term);
                if (hit) {
                    s += t.weight;
                    if (matched.get(m).size() < 16) matched.get(m).add(t.term);
                }
            }
            scores.put(m, scores.get(m) + s);
        }

        List<Map.Entry<MateriaJurisdicao, Integer>> ranked = scores.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();

        MateriaJurisdicao best = ranked.get(0).getKey();
        int bestScore = ranked.get(0).getValue();
        int secondScore = ranked.size() > 1 ? ranked.get(1).getValue() : 0;

        if (bestScore <= 0) {
            return new MateriaDecision(MateriaJurisdicao.MULTIMATERIA, 0.15, List.of());
        }

        MateriaJurisdicao materia = best;
        if (secondScore > 0 && (bestScore - secondScore) <= Math.max(2, bestScore / 6)) {
            materia = MateriaJurisdicao.MULTIMATERIA;
        }

        double conf = confidence(bestScore, secondScore, materia == MateriaJurisdicao.MULTIMATERIA);
        List<String> signals = matched.get(best).stream().toList();

        return new MateriaDecision(materia, conf, signals);
    }

    private static double confidence(int best, int second, boolean multi) {
        double denom = Math.max(1.0, best + second);
        double margin = Math.max(0.0, (best - second) / denom);
        double base = 0.45 + 0.55 * margin;
        if (multi) base = Math.min(base, 0.62);
        return Math.max(0.05, Math.min(0.98, base));
    }

    private static String buildCorpus(String... inputs) {
        if (inputs == null || inputs.length == 0) return "";
        StringBuilder sb = new StringBuilder(512);
        for (String s : inputs) {
            if (s == null || s.isBlank()) continue;
            if (!sb.isEmpty()) sb.append('\n');
            sb.append(s);
        }
        return sb.toString();
    }

    private static EnumMap<MateriaJurisdicao, List<Term>> buildLexicon() {
        EnumMap<MateriaJurisdicao, List<Term>> m = new EnumMap<>(MateriaJurisdicao.class);

        m.put(MateriaJurisdicao.SAUDE, List.of(
                Term.contains("sus", 8),
                Term.contains("anvisa", 8),
                Term.contains("ans", 7),
                Term.contains("plano de saude", 9),
                Term.contains("plano de saúde", 9),
                Term.contains("medicamento", 7),
                Term.contains("tratamento", 6),
                Term.contains("internacao", 7),
                Term.contains("internação", 7),
                Term.contains("uti", 8),
                Term.contains("cirurgia", 6),
                Term.contains("home care", 7),
                Term.contains("negativa", 4),
                Term.contains("cid", 4)
        ));

        m.put(MateriaJurisdicao.TRABALHISTA, List.of(
                Term.contains("clt", 9),
                Term.contains("verbas rescisorias", 9),
                Term.contains("verbas rescisórias", 9),
                Term.contains("fgts", 8),
                Term.contains("horas extras", 8),
                Term.contains("insalubridade", 7),
                Term.contains("periculosidade", 7),
                Term.contains("vinculo", 6),
                Term.contains("vínculo", 6)
        ));

        m.put(MateriaJurisdicao.EXECUCAO_FISCAL, List.of(
                Term.contains("execucao fiscal", 12),
                Term.contains("execução fiscal", 12),
                Term.contains("cda", 8),
                Term.contains("certidao de divida ativa", 12),
                Term.contains("certidão de dívida ativa", 12),
                Term.contains("penhora", 6)
        ));

        m.put(MateriaJurisdicao.TRIBUTARIA, List.of(
                Term.contains("icms", 8),
                Term.contains("ipi", 7),
                Term.contains("iss", 7),
                Term.contains("irpf", 8),
                Term.contains("irpj", 8),
                Term.contains("pis", 6),
                Term.contains("cofins", 6),
                Term.contains("lancamento", 5),
                Term.contains("lançamento", 5),
                Term.contains("auto de infracao", 7),
                Term.contains("auto de infração", 7)
        ));

        m.put(MateriaJurisdicao.PENAL, List.of(
                Term.contains("habeas corpus", 10),
                Term.contains("prisao", 8),
                Term.contains("prisão", 8),
                Term.contains("flagrante", 7),
                Term.contains("denuncia", 6),
                Term.contains("denúncia", 6),
                Term.contains("pena", 5),
                Term.contains("dosimetria", 7),
                Term.contains("audiencia de custodia", 10),
                Term.contains("audiência de custódia", 10)
        ));

        m.put(MateriaJurisdicao.FAMILIA, List.of(
                Term.contains("alimentos", 8),
                Term.contains("guarda", 7),
                Term.contains("visitas", 6),
                Term.contains("divorcio", 7),
                Term.contains("divórcio", 7),
                Term.contains("uniao estavel", 8),
                Term.contains("união estável", 8),
                Term.contains("paternidade", 8)
        ));

        m.put(MateriaJurisdicao.PREVIDENCIARIA, List.of(
                Term.contains("inss", 10),
                Term.contains("aposentadoria", 9),
                Term.contains("beneficio", 7),
                Term.contains("benefício", 7),
                Term.contains("bpc", 8),
                Term.contains("loas", 8),
                Term.contains("auxilio", 7),
                Term.contains("auxílio", 7)
        ));

        m.put(MateriaJurisdicao.CIVIL, List.of(
                Term.contains("indenizacao", 7),
                Term.contains("indenização", 7),
                Term.contains("danos morais", 8),
                Term.contains("obrigacao", 6),
                Term.contains("obrigação", 6),
                Term.contains("tutela de urgencia", 7),
                Term.contains("tutela de urgência", 7),
                Term.contains("responsabilidade civil", 8)
        ));

        m.put(MateriaJurisdicao.ADMINISTRATIVO, List.of(
                Term.contains("licitacao", 8),
                Term.contains("licitação", 8),
                Term.contains("servidor publico", 8),
                Term.contains("servidor público", 8),
                Term.contains("improbidade", 8),
                Term.contains("concurso publico", 8),
                Term.contains("concurso público", 8)
        ));

        for (MateriaJurisdicao mj : MateriaJurisdicao.values()) {
            m.putIfAbsent(mj, List.of());
        }

        return m;
    }

    private enum MatchMode { CONTAINS, TOKEN }

    private record Term(String term, int weight, MatchMode matchMode) {
        static Term contains(String term, int weight) {
            return new Term(term.toLowerCase(Locale.ROOT), weight, MatchMode.CONTAINS);
        }
    }
}
