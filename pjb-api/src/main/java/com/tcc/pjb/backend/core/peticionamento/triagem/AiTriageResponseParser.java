package com.tcc.pjb.backend.core.peticionamento.triagem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class AiTriageResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Optional<AiTriageSuggestion> parse(String resposta) {
        String json = extrairObjetoJson(resposta);
        if (json == null) {
            return Optional.empty();
        }
        try {
            var campos = MAPPER.readValue(json, new TypeReference<java.util.Map<String, Object>>() {});
            return Optional.of(new AiTriageSuggestion(
                    texto(campos.get("vara")),
                    texto(campos.get("movimento")),
                    booleano(campos.get("conciliacao")),
                    texto(campos.get("minuta")),
                    listaDeTexto(campos.get("alertas")),
                    confianca(campos.get("confianca")),
                    true));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String extrairObjetoJson(String resposta) {
        if (resposta == null || resposta.isBlank()) {
            return null;
        }
        int inicio = resposta.indexOf('{');
        int fim = resposta.lastIndexOf('}');
        if (inicio < 0 || fim <= inicio) {
            return null;
        }
        return resposta.substring(inicio, fim + 1);
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        String s = String.valueOf(valor).trim();
        return s.isEmpty() ? null : s;
    }

    private static boolean booleano(Object valor) {
        return valor instanceof Boolean b && b;
    }

    private static double confianca(Object valor) {
        if (!(valor instanceof Number numero)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, numero.doubleValue()));
    }

    private static List<String> listaDeTexto(Object valor) {
        if (!(valor instanceof Iterable<?> itens)) {
            return List.of();
        }
        List<String> resultado = new ArrayList<>();
        for (Object item : itens) {
            String texto = texto(item);
            if (texto != null) {
                resultado.add(texto);
            }
        }
        return List.copyOf(resultado);
    }
}
