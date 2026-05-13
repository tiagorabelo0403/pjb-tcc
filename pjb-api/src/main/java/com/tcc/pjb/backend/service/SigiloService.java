package com.tcc.pjb.backend.service;

import java.text.Normalizer;
import java.util.*;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.Locale;

@Service
public class SigiloService {

    public int definirNivelSigilo(Processo processo) {
        return avaliar(processo).nivel().nivel();
    }

    public SigiloDecision avaliarCorpus(String corpus) {
        if (corpus == null) {
            return SigiloDecision.publico("corpus_null");
        }

        String norm = normalize(corpus);
        if (norm.isBlank()) {
            return SigiloDecision.publico("corpus_vazio");
        }

        EnumSet<SigiloSignal> signals = EnumSet.noneOf(SigiloSignal.class);

        int score = 0;

        score += match(norm, signals, SigiloSignal.INFANCIA, 45,
                "crianca", "criança", "adolescente", "menor", "infancia", "infância",
                "guarda", "adoção", "adocao", "alimentos", "visitas", "paternidade", "maternidade");

        score += match(norm, signals, SigiloSignal.VIOLENCIA_DOMESTICA, 45,
                "violencia domestica", "violência doméstica", "lei maria da penha", "medida protetiva",
                "ameaça", "ameaca", "perseguicao", "perseguição", "stalking");

        score += match(norm, signals, SigiloSignal.SAUDE, 40,
                "saude", "saúde", "prontuario", "prontuário", "laudo", "diagnostico", "diagnóstico",
                "tratamento", "internacao", "internação", "cirurgia", "psi", "psiquiatr", "oncolog");

        score += match(norm, signals, SigiloSignal.BANCARIO_FISCAL, 35,
                "sigilo bancario", "sigilo bancário", "extrato", "conta", "cartao", "cartão",
                "dados fiscais", "imposto de renda", "declaracao", "declaração", "e-financeira");

        score += match(norm, signals, SigiloSignal.PENAL_SENSIVEL, 55,
                "interceptacao", "interceptação", "organizacao criminosa", "organização criminosa",
                "colaboracao premiada", "colaboração premiada", "operacao", "operação",
                "inteligencia", "inteligência", "investigacao", "investigação");

        score += match(norm, signals, SigiloSignal.LGPD, 15,
                "lgpd", "dados sensiveis", "dados sensíveis", "biometria", "cpf", "rg", "cnh", "endereco", "endereço");

        NivelSigilo nivel = decideLevel(score, signals);

        return new SigiloDecision(nivel, score, signals, recomendacoes(nivel, signals));
    }

    public SigiloDecision avaliar(Processo processo) {
        if (processo == null) {
            return SigiloDecision.publico("processo_null");
        }

        String corpus = normalize(join(
                processo.getAssunto(),
                processo.getClasseProcessual(),
                processo.getRamoDireito(),
                processo.getObjetoProcessual(),
                processo.getMaterialProbatorioResumo()
        ));

        if (corpus.isBlank()) {
            return SigiloDecision.publico("corpus_vazio");
        }

        EnumSet<SigiloSignal> signals = EnumSet.noneOf(SigiloSignal.class);

        int score = 0;

        score += match(corpus, signals, SigiloSignal.INFANCIA, 45,
                "crianca", "criança", "adolescente", "menor", "infancia", "infância",
                "guarda", "adoção", "adocao", "alimentos", "visitas", "paternidade", "maternidade");

        score += match(corpus, signals, SigiloSignal.VIOLENCIA_DOMESTICA, 45,
                "violencia domestica", "violência doméstica", "lei maria da penha", "medida protetiva",
                "ameaça", "ameaca", "perseguicao", "perseguição", "stalking");

        score += match(corpus, signals, SigiloSignal.SAUDE, 40,
                "saude", "saúde", "prontuario", "prontuário", "laudo", "diagnostico", "diagnóstico",
                "tratamento", "internacao", "internação", "cirurgia", "psi", "psiquiatr", "oncolog");

        score += match(corpus, signals, SigiloSignal.BANCARIO_FISCAL, 35,
                "sigilo bancario", "sigilo bancário", "extrato", "conta", "cartao", "cartão",
                "dados fiscais", "imposto de renda", "declaracao", "declaração", "e-financeira");

        score += match(corpus, signals, SigiloSignal.PENAL_SENSIVEL, 55,
                "interceptacao", "interceptação", "organizacao criminosa", "organização criminosa",
                "colaboracao premiada", "colaboração premiada", "operacao", "operação",
                "inteligencia", "inteligência", "investigacao", "investigação");

        score += match(corpus, signals, SigiloSignal.LGPD, 15,
                "lgpd", "dados sensiveis", "dados sensíveis", "biometria", "cpf", "rg", "cnh", "endereco", "endereço");

        NivelSigilo nivel = decideLevel(score, signals);

        return new SigiloDecision(nivel, score, signals, recomendacoes(nivel, signals));
    }

    private static NivelSigilo decideLevel(int score, EnumSet<SigiloSignal> signals) {
        if (signals.contains(SigiloSignal.PENAL_SENSIVEL)) {
            if (score >= 90) return NivelSigilo.SIGILO_N4;
            return NivelSigilo.SIGILO_N3;
        }
        if (signals.contains(SigiloSignal.INFANCIA) || signals.contains(SigiloSignal.VIOLENCIA_DOMESTICA) || signals.contains(SigiloSignal.SAUDE)) {
            if (score >= 80) return NivelSigilo.SIGILO_N3;
            return NivelSigilo.SIGILO_N2;
        }
        if (signals.contains(SigiloSignal.BANCARIO_FISCAL)) {
            if (score >= 70) return NivelSigilo.SIGILO_N2;
            return NivelSigilo.SEGREDO_JUSTICA;
        }
        if (signals.contains(SigiloSignal.LGPD)) {
            if (score >= 50) return NivelSigilo.SEGREDO_JUSTICA;
            return NivelSigilo.PUBLICO;
        }
        if (score >= 60) return NivelSigilo.SEGREDO_JUSTICA;
        return NivelSigilo.PUBLICO;
    }

    private static List<String> recomendacoes(NivelSigilo nivel, EnumSet<SigiloSignal> signals) {
        ArrayList<String> out = new ArrayList<>(8);
        if (nivel.exigeCredencial()) {
            out.add("exigir_justificativa_need_to_know");
            out.add("bloquear_cache_para_documentos_sigilosos");
        }
        if (signals.contains(SigiloSignal.INFANCIA)) out.add("ocultar_identificadores_de_menores");
        if (signals.contains(SigiloSignal.SAUDE)) out.add("mascarar_dados_de_saude_em_resumos_publicos");
        if (signals.contains(SigiloSignal.BANCARIO_FISCAL)) out.add("redacao_dados_financeiros_para_terceiros");
        if (signals.contains(SigiloSignal.PENAL_SENSIVEL)) out.add("minimizar_divulgacao_de_medidas_sigilosas");
        return List.copyOf(out);
    }

    private static int match(String corpus, EnumSet<SigiloSignal> signals, SigiloSignal signal, int weight, String... tokens) {
        for (String t : tokens) {
            String token = normalize(t);
            if (!token.isBlank() && corpus.contains(token)) {
                signals.add(signal);
                return weight;
            }
        }
        return 0;
    }

    private static String join(Object... parts) {
        if (parts == null || parts.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            if (p == null) continue;
            String s;
            if (p instanceof CharSequence cs) {
                s = cs.toString();
            } else {
                s = p.toString();
            }
            if (s == null) continue;
            s = s.trim();
            if (s.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(s);
        }
        return sb.toString();
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String x = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        x = x.replaceAll("[^a-z0-9\\s]", " ");
        x = x.replaceAll("\\s+", " ").trim();
        return x;
    }

    public record SigiloDecision(NivelSigilo nivel,
                                int score,
                                EnumSet<SigiloSignal> signals,
                                List<String> recomendacoes) {
        public static SigiloDecision publico(String reason) {
            return new SigiloDecision(NivelSigilo.PUBLICO, 0, EnumSet.noneOf(SigiloSignal.class), List.of(reason));
        }
    }

    public enum SigiloSignal {
        INFANCIA,
        VIOLENCIA_DOMESTICA,
        SAUDE,
        BANCARIO_FISCAL,
        PENAL_SENSIVEL,
        LGPD
    }
}
