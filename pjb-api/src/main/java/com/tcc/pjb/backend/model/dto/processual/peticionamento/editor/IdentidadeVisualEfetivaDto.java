package com.tcc.pjb.backend.model.dto.processual.peticionamento.editor;

import java.util.List;
import java.util.Map;

/**
 * Identidade visual efetiva do ator, já resolvida (institucional do órgão + individual sobreposto),
 * tipada para consumo direto do frontend. Preenchida a partir do preset resolvido pelo serviço.
 */
public record IdentidadeVisualEfetivaDto(
        String classeIdentidade,
        String poderRamo,
        String esfera,
        String nomeOrgao,
        String escopoRef,
        String brasaoCoresOrigem,
        String nomeExibicao,
        String nomeInstituicao,
        String brasaoOuLogomarcaUri,
        String cabecalhoLivre,
        String rodapeLivre,
        String paletaPrimaria,
        String paletaSecundaria,
        Boolean exibirRegistroProfissional,
        Boolean exibirBrasaoOuLogomarca,
        String registroLabel,
        List<String> cabecalhoSugerido
) {

    @SuppressWarnings("unchecked")
    public static IdentidadeVisualEfetivaDto fromPreset(Map<String, Object> p) {
        if (p == null) {
            p = Map.of();
        }
        Object cabecalho = p.get("cabecalhoSugerido");
        List<String> cabecalhoSugerido = cabecalho instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        return new IdentidadeVisualEfetivaDto(
                str(p, "classeIdentidade"),
                str(p, "poderRamo"),
                str(p, "esfera"),
                str(p, "nomeOrgao"),
                str(p, "escopoRef"),
                str(p, "brasaoCoresOrigem"),
                str(p, "nomeExibicao"),
                str(p, "nomeInstituicao"),
                str(p, "brasaoOuLogomarcaUri"),
                str(p, "cabecalhoLivre"),
                str(p, "rodapeLivre"),
                str(p, "paletaPrimaria"),
                str(p, "paletaSecundaria"),
                bool(p, "exibirRegistroProfissional"),
                bool(p, "exibirBrasaoOuLogomarca"),
                str(p, "registroLabel"),
                cabecalhoSugerido);
    }

    private static String str(Map<String, Object> p, String key) {
        Object v = p.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Boolean bool(Map<String, Object> p, String key) {
        Object v = p.get(key);
        return v instanceof Boolean b ? b : null;
    }
}
