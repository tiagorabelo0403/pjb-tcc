package com.tcc.pjb.backend.core.forum.routing;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.Locale;

public final class OrganizacaoJudiciaria {

    private OrganizacaoJudiciaria() {}

    
    public enum Ramo {
        JUSTICA_ESTADUAL,
        JUSTICA_FEDERAL,
        JUSTICA_DO_TRABALHO,
        JUSTICA_ELEITORAL,
        JUSTICA_MILITAR,
        TRIBUNAIS_SUPERIORES,
        STF
    }

    
    public enum Instancia {
        PRIMEIRO_GRAU,
        SEGUNDO_GRAU,
        SUPERIOR
    }

    public static final List<String> IDEIAS = List.of(
            "Heurística de competência por palavras-chave com scoring.",
            "Sugestão de instância por termos recursais e fase processual.",
            "Detecção de urgência (liminar/tutela/habeas corpus) com alerta.",
            "Indicadores de sigilo potencial (família, menor, saúde).",
            "Geração de rota sugerida (consulta/protocolo) por ramo.",
            "Tags padronizadas para observabilidade e auditoria.",
            "Normalização de texto (acentos e pontuação) para matching confiável.",
            "Peso por palavra-chave para reduzir falsos positivos.",
            "Justificativa auditável (por que o motor sugeriu).",
            "Fallback para casos indeterminados com checklist de dados faltantes.",
            "Separação rigorosa: sugestão ≠ decisão.",
            "Modo compatível com front: retorno em Map<String,Object>.",
            "Saída estruturada com alertas e recomendações.",
            "Órgãos típicos por ramo para exibição e roteamento.",
            "Vocabulário recursal: REsp/RE/agravo/apelação/habeas corpus.",
            "Checklist mínimo de competência e territorialidade.",
            "Detecção de matérias sensíveis (ambiental/saúde/infância).",
            "Pronto para integração futura sem acoplamento (CNJ/tribunais)."
    );

    
    private static final Map<Ramo, Map<String, Integer>> KEYWORDS_RAMO = buildKeywords();

    private static Map<Ramo, Map<String, Integer>> buildKeywords() {
        Map<Ramo, Map<String, Integer>> m = new EnumMap<>(Ramo.class);

        m.put(Ramo.JUSTICA_DO_TRABALHO, Map.of(
                "clt", 5, "verbas rescisorias", 4, "rescisao indireta", 4, "reclamacao trabalhista", 5,
                "fgts", 3, "horas extras", 3, "adicional", 2, "insalubridade", 3, "periculosidade", 3
        ));

        m.put(Ramo.JUSTICA_FEDERAL, Map.of(
                "inss", 5, "previdenciario", 4, "beneficio", 3, "uniao", 4, "receita federal", 4,
                "caixa economica", 3, "ibama", 3, "dnit", 2, "aneel", 2, "anvisa", 2
        ));

        m.put(Ramo.JUSTICA_ELEITORAL, Map.of(
                "tse", 5, "tre", 4, "registro de candidatura", 5, "propaganda eleitoral", 4,
                "prestacao de contas", 3, "inelegibilidade", 4
        ));

        m.put(Ramo.JUSTICA_MILITAR, Map.of(
                "crime militar", 5, "justica militar", 5, "cpm", 4, "transgressao disciplinar", 3
        ));

        m.put(Ramo.TRIBUNAIS_SUPERIORES, Map.of(
                "resp", 5, "recurso especial", 5, "stj", 5, "sumula", 2, "embargos de divergencia", 4
        ));

        m.put(Ramo.STF, Map.of(
                "re", 5, "recurso extraordinario", 5, "stf", 5, "repercussao geral", 4,
                "adpf", 4, "adi", 4, "ado", 3, "mandado de injuncao", 3
        ));

        
        m.put(Ramo.JUSTICA_ESTADUAL, Map.of(
                "vara", 1, "comarca", 2, "juizado", 2, "familia", 3, "consumidor", 2,
                "danos morais", 2, "responsabilidade civil", 2, "contrato", 2, "condominio", 2
        ));

        return m;
    }

    
    private static final Map<String, Instancia> KEYWORDS_INSTANCIA = Map.ofEntries(
            Map.entry("apela", Instancia.SEGUNDO_GRAU),
            Map.entry("agravo", Instancia.SEGUNDO_GRAU),
            Map.entry("embargos", Instancia.SEGUNDO_GRAU),
            Map.entry("resp", Instancia.SUPERIOR),
            Map.entry("recurso especial", Instancia.SUPERIOR),
            Map.entry("re", Instancia.SUPERIOR),
            Map.entry("recurso extraordinario", Instancia.SUPERIOR),
            Map.entry("stj", Instancia.SUPERIOR),
            Map.entry("stf", Instancia.SUPERIOR)
    );

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    
    public static Map<String, Object> sugerirRota(String textoLivre) {
        String norm = normalizar(textoLivre);

        Map<Ramo, Integer> score = new EnumMap<>(Ramo.class);
        for (Ramo r : Ramo.values()) score.put(r, 0);

        for (var entry : KEYWORDS_RAMO.entrySet()) {
            Ramo ramo = entry.getKey();
            for (var kw : entry.getValue().entrySet()) {
                if (norm.contains(kw.getKey())) {
                    score.put(ramo, score.get(ramo) + kw.getValue());
                }
            }
        }

        Ramo ramoSugerido = score.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Ramo.JUSTICA_ESTADUAL);

        Instancia instancia = sugerirInstancia(norm);

        List<String> alertas = new ArrayList<>();
        if (containsAny(norm, "tutela", "liminar", "urgencia", "habeas corpus")) {
            alertas.add("Indicador de urgência detectado: conferir rito/plantão e prazos.");
        }
        if (containsAny(norm, "menor", "crianca", "adolescente", "saude", "internacao", "familia")) {
            alertas.add("Possível matéria sensível: avaliar necessidade de sigilo e cuidados LGPD.");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ramo_sugerido", ramoSugerido.name());
        out.put("instancia_sugerida", instancia.name());
        out.put("score_ramo", scoreToMap(score));
        out.put("rota_sugerida", rotaPorRamo(ramoSugerido, instancia));
        out.put("alertas", alertas);
        out.put("justificativa", "Sugestão baseada em heurística por palavras-chave; requer validação em fonte oficial.");
        out.put("checklist_faltantes", checklistFaltantes(norm));

        
        out.put("etiquetas_metricas", List.of(
                "ramo:" + ramoSugerido.name(),
                "instancia:" + instancia.name(),
                "urgencia:" + (containsAny(norm, "tutela", "liminar", "urgencia", "habeas corpus") ? "sim" : "nao")
        ));

        return out;
    }

    private static Instancia sugerirInstancia(String norm) {
        for (var e : KEYWORDS_INSTANCIA.entrySet()) {
            if (norm.contains(e.getKey())) return e.getValue();
        }
        return Instancia.PRIMEIRO_GRAU;
    }

    private static Map<String, Integer> scoreToMap(Map<Ramo, Integer> score) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (var e : score.entrySet()) m.put(e.getKey().name(), e.getValue());
        return m;
    }

    private static String rotaPorRamo(Ramo ramo, Instancia instancia) {
        return switch (ramo) {
            case JUSTICA_DO_TRABALHO -> instancia == Instancia.PRIMEIRO_GRAU
                    ? "Protocolo/consulta: TRT (1º grau) / PJe-JT, conforme região."
                    : "Recurso/consulta: TRT (2º grau) ou TST (quando cabível).";
            case JUSTICA_FEDERAL -> "Protocolo/consulta: TRF/JF (eproc ou PJe Federal, conforme região).";
            case JUSTICA_ELEITORAL -> "Protocolo/consulta: TRE/TSE (conforme matéria).";
            case JUSTICA_MILITAR -> "Protocolo/consulta: Justiça Militar (União/estadual, conforme caso).";
            case TRIBUNAIS_SUPERIORES -> "Protocolo/consulta: STJ (REsp, habeas corpus, etc.).";
            case STF -> "Protocolo/consulta: STF (RE, ADI/ADPF/ADO, etc.).";
            case JUSTICA_ESTADUAL -> "Protocolo/consulta: TJ/Comarca (PJe/e-SAJ/eproc, conforme estado).";
            default -> "Protocolo/consulta: verificar ramo competente e sistema do tribunal (PJe/eproc/e-SAJ/Projudi, conforme região).";
        };
    }

    private static List<String> checklistFaltantes(String norm) {
        List<String> faltantes = new ArrayList<>();
        if (!containsAny(norm, "cpf", "cnpj", "parte", "autor", "reu")) faltantes.add("Qualificação mínima das partes (autor/réu).");
        if (!containsAny(norm, "comarca", "cidade", "uf", "estado")) faltantes.add("Localidade/competência territorial (comarca/UF).");
        if (!containsAny(norm, "classe", "acao", "pedido")) faltantes.add("Classe/ação e pedido principal.");
        if (!containsAny(norm, "fatos", "ocorreu", "data")) faltantes.add("Fatos essenciais e datas relevantes.");
        return faltantes;
    }

    private static boolean containsAny(String norm, String... tokens) {
        for (String t : tokens) {
            if (norm.contains(normalizar(t))) return true;
        }
        return false;
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        n = n.replaceAll("[^a-z0-9\\s]", " ");
        n = MULTI_SPACE.matcher(n).replaceAll(" ").trim();
        return n;
    }
}
