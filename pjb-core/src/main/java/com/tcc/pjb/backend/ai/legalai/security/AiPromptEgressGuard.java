package com.tcc.pjb.backend.ai.legalai.security;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AiPromptEgressGuard {

    public static final String MARCADOR_NEUTRALIZADO = "[marcador de protocolo removido]";

    private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}+");

    private static final List<Pattern> TOKENS_DE_PROTOCOLO = List.of(
            Pattern.compile("<\\|[^|>]{0,64}\\|>"),
            Pattern.compile("(?i)\\[/?INST\\]"),
            Pattern.compile("(?i)<</?SYS>>")
    );

    private static final List<Sinal> SINAIS = List.of(
            new Sinal("instrucao_anterior", Pattern.compile(
                    "(?i)\\b(ignore|ignora|desconsidere|desconsidera|esqueca|esquecam)\\s+"
                            + "(todas?\\s+)?(as\\s+)?(instrucoes|orientacoes|ordens|regras)\\s+"
                            + "(anteriores|acima|previas|do\\s+sistema)")),
            new Sinal("instrucao_anterior", Pattern.compile(
                    "(?i)\\b(ignore|disregard|forget)\\s+(all\\s+)?(the\\s+)?"
                            + "(previous|prior|above|system)\\s+(instructions?|context|rules)")),
            new Sinal("troca_de_papel", Pattern.compile(
                    "(?i)\\bvoce\\s+(agora\\s+)?(e|sera)\\s+(um|uma|o|a)\\b")),
            new Sinal("troca_de_papel", Pattern.compile(
                    "(?i)\\byou\\s+are\\s+now\\s+")),
            new Sinal("troca_de_papel", Pattern.compile(
                    "(?i)\\b(aja|atue|comporte-se)\\s+como\\s+(um|uma|se)\\b")),
            new Sinal("troca_de_papel", Pattern.compile(
                    "(?i)\\b(pretend|act)\\s+(you\\s+are|as\\s+if\\s+you)\\b")),
            new Sinal("revelar_prompt", Pattern.compile(
                    "(?i)\\b(revele|mostre|exiba|imprima|repita)\\s+(o\\s+|a\\s+|as\\s+|seu\\s+|sua\\s+|suas\\s+)*"
                            + "(prompt|instrucoes\\s+do\\s+sistema)")),
            new Sinal("revelar_prompt", Pattern.compile(
                    "(?i)\\breveal\\s+(your\\s+)?(system\\s+)?(prompt|instructions?)\\b")),
            new Sinal("modo_irrestrito", Pattern.compile(
                    "(?i)\\b(modo\\s+desenvolvedor|modo\\s+irrestrito|developer\\s+mode|jailbreak)\\b")),
            new Sinal("sobrepor_salvaguarda", Pattern.compile(
                    "(?i)\\b(desative|desabilite|remova|sobreponha|burle|burlar|contorne|contornar)\\s+"
                            + "(todas?\\s+)?(as\\s+|os\\s+)?"
                            + "(regras|restricoes|diretrizes|salvaguardas|filtros|limites)")),
            new Sinal("sobrepor_salvaguarda", Pattern.compile(
                    "(?i)\\b(override|bypass|disable|ignore)\\s+(all\\s+)?(the\\s+)?"
                            + "(safety|guidelines|rules|constraints|restrictions|filters|limits)\\b")),
            new Sinal("persona_jailbreak", Pattern.compile(
                    "(?i)(\\bDAN\\b|\\bdo\\s+anything\\s+now\\b)")),
            new Sinal("marcador_de_papel", Pattern.compile(
                    "(?im)^\\s*(system|assistant|human|sistema|assistente)\\s*:\\s")),
            new Sinal("nova_instrucao", Pattern.compile(
                    "(?i)\\b(novas?\\s+instrucoes|new\\s+instructions?)\\s*:"))
    );

    public AiPromptInspection inspecionar(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return new AiPromptInspection(prompt, false, Set.of());
        }

        String saneado = prompt;
        boolean neutralizado = false;
        for (Pattern token : TOKENS_DE_PROTOCOLO) {
            Matcher matcher = token.matcher(saneado);
            if (matcher.find()) {
                saneado = matcher.replaceAll(Matcher.quoteReplacement(MARCADOR_NEUTRALIZADO));
                neutralizado = true;
            }
        }

        String comparavel = semAcentos(saneado);
        Set<String> sinais = new LinkedHashSet<>();
        for (Sinal sinal : SINAIS) {
            if (sinal.padrao().matcher(comparavel).find()) {
                sinais.add(sinal.id());
            }
        }

        return new AiPromptInspection(saneado, neutralizado, sinais);
    }

    private static String semAcentos(String texto) {
        return DIACRITICOS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
    }

    private record Sinal(String id, Pattern padrao) {
    }
}
