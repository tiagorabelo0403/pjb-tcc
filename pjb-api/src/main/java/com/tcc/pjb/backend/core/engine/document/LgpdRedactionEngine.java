package com.tcc.pjb.backend.core.engine.document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LgpdRedactionEngine {

    private static final Pattern CPF_PATTERN = Pattern.compile("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b");
    private static final Pattern CPF_11 = Pattern.compile("\\b\\d{11}\\b");
    private static final Pattern CARTAO_CREDITO = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14})\\b");
    private static final Pattern EMAIL = Pattern.compile("\\b[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern TELEFONE = Pattern.compile("\\b(?:\\+?55\\s?)?(?:\\(?\\d{2}\\)?\\s?)?9?\\d{4}-?\\d{4}\\b");

    private static final List<String> GATILHOS_SIGILO = List.of(
            "ABUSO SEXUAL",
            "MENOR DE IDADE",
            "QUEBRA DE SIGILO BANCARIO",
            "DADOS MEDICOS",
            "VIOLENCIA DOMESTICA",
            "ADOCAO",
            "INTERDICAO"
    );

    public LgpdResult analisarRisco(String conteudoDocumento) {
        String texto = conteudoDocumento == null ? "" : conteudoDocumento;

        List<String> achados = new ArrayList<>();
        int risco = 0;
        boolean sugereSigilo = false;

        risco += coletar(texto, CPF_PATTERN, "CPF", achados) * 10;
        risco += coletar(texto, CPF_11, "CPF", achados) * 8;
        risco += coletar(texto, EMAIL, "EMAIL", achados) * 5;
        risco += coletar(texto, TELEFONE, "TELEFONE", achados) * 5;

        int cartoes = coletar(texto, CARTAO_CREDITO, "CARTAO", achados);
        if (cartoes > 0) {
            risco += cartoes * 50;
            sugereSigilo = true;
        }

        String upper = texto.toUpperCase();
        for (String g : GATILHOS_SIGILO) {
            if (upper.contains(g)) {
                achados.add("TEMA_SENSIVEL:" + g);
                risco += 25;
                sugereSigilo = true;
            }
        }

        int score = Math.min(risco, 100);
        String sanitizado = aplicarMascaras(texto);

        if (score > 0) {
            log.debug("LGPD: score={} achados={}", score, achados.size());
        }

        return LgpdResult.builder()
                .riscoScore(score)
                .sugereSegredoJustica(sugereSigilo)
                .dadosOcultados(achados)
                .conteudoSanitizado(sanitizado)
                .build();
    }

    private int coletar(String texto, Pattern pattern, String label, List<String> achados) {
        Matcher m = pattern.matcher(texto);
        int count = 0;
        while (m.find()) {
            count++;
            String raw = m.group();
            achados.add(label + ":" + mascarar(raw));
        }
        return count;
    }

    private String aplicarMascaras(String texto) {
        String t = CPF_PATTERN.matcher(texto).replaceAll("[CPF_OCULTO]");
        t = CPF_11.matcher(t).replaceAll("[CPF_OCULTO]");
        t = CARTAO_CREDITO.matcher(t).replaceAll("[DADO_FINANCEIRO_OCULTO]");
        t = EMAIL.matcher(t).replaceAll("[EMAIL_OCULTO]");
        t = TELEFONE.matcher(t).replaceAll("[TELEFONE_OCULTO]");
        return t;
    }

    private String mascarar(String dado) {
        if (dado == null) return "";
        return dado.replaceAll("\\d", "*");
    }

    @Value
    @Builder
    public static class LgpdResult {
        int riscoScore;
        boolean sugereSegredoJustica;
        List<String> dadosOcultados;
        String conteudoSanitizado;
    }
}
