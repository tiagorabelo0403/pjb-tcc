package com.tcc.pjb.backend.core.procedural;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.Processo;

public final class ProcessoCanonicalPayloadFactory {

    private ProcessoCanonicalPayloadFactory() {
    }

    public static Map<String, Object> fromProcesso(Processo processo) {
        return fromProcesso(processo, null);
    }

    public static Map<String, Object> fromProcesso(Processo processo, String resumo) {
        Objects.requireNonNull(processo, "processo");
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        putText(payload, "rito", processo.getRito() != null ? processo.getRito().name() : null);
        putText(payload, "rito_processual", processo.getRito() != null ? processo.getRito().name() : null);
        putText(payload, "ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
        putText(payload, "materia", processo.getMateria() != null ? processo.getMateria().name() : null);
        putText(payload, "ambito_direito", processo.getMateria() != null ? processo.getMateria().name() : null);
        putText(payload, "competencia", processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null);
        putText(payload, "classe", processo.getClasseProcessual());
        putText(payload, "classeProcessual", processo.getClasseProcessual());
        putText(payload, "classeTpu", firstNonBlank(processo.getClasseTpuCodigo(), processo.getClasseProcessual()));
        putText(payload, "classeTpuCodigo", processo.getClasseTpuCodigo());
        putText(payload, "classeTpuNome", processo.getClasseProcessual());
        putText(payload, "assunto", processo.getAssunto());
        putText(payload, "objetoProcessual", processo.getObjetoProcessual());
        putText(payload, "pedidoPrincipal", processo.getPedidoPrincipal());
        putText(payload, "pedidos", processo.getPedidosConsolidados());
        putText(payload, "provas", processo.getMaterialProbatorioResumo());
        putText(payload, "materialProbatorioResumo", processo.getMaterialProbatorioResumo());
        putText(payload, "resumo", firstNonBlank(resumo, processo.getResumoIA(), processo.getObjetoProcessual(), processo.getPedidoPrincipal()));
        putValue(payload, "valorCausa", processo.getValorCausa());
        putValue(payload, "valor_causa", normalizeBigDecimal(processo.getValorCausa()));
        putValue(payload, "potencialAcordoScore", processo.getPotencialAcordoScore());
        putText(payload, "janelaAcordoResumo", processo.getJanelaAcordoResumo());
        putText(payload, "numeroProcesso", processo.getNumeroProcesso());
        putText(payload, "numeroUnificado", processo.getNumeroUnificado());
        putText(payload, "uf", processo.getUf());
        putText(payload, "estado", processo.getUf());
        putText(payload, "cidade", processo.getComarca());
        putText(payload, "comarca", processo.getComarca());
        putText(payload, "foro", processo.getComarca());
        putText(payload, "tribunalCodigo", processo.getTribunalCodigoRoteado());
        putText(payload, "tribunal", processo.getTribunal());
        putText(payload, "varaPretendida", firstNonBlank(processo.getVara(), processo.getUnidadeJudiciariaCodigo()));
        enrichJurisdicao(payload, processo.getJurisdicao());
        return PayloadMaps.deepCopyWithoutNulls(payload);
    }

    private static void enrichJurisdicao(Map<String, Object> payload, Jurisdicao jurisdicao) {
        if (jurisdicao == null) {
            return;
        }
        putText(payload, "unidade_nome", jurisdicao.getNome());
        putText(payload, "unidade_tipo", jurisdicao.getTipo() != null ? jurisdicao.getTipo().name() : null);
        putText(payload, "unidade_grau", jurisdicao.getGrau() != null ? jurisdicao.getGrau().name() : null);
        putText(payload, "unidade_esfera", jurisdicao.getEsfera() != null ? jurisdicao.getEsfera().name() : null);
        putText(payload, "unidade_materia", jurisdicao.getMateria() != null ? jurisdicao.getMateria().name() : null);
        putText(payload, "unidade_comarca", jurisdicao.getCidade());
        putText(payload, "unidade_cidade", jurisdicao.getCidade());
        putText(payload, "unidade_foro", jurisdicao.getForo());
        putText(payload, "unidade_secao_judiciaria", jurisdicao.getSecaoJudiciaria());
        putText(payload, "unidade_subsecao_judiciaria", jurisdicao.getSubsecaoJudiciaria());
        putText(payload, "unidade_circunscricao", jurisdicao.getCircunscricao());
        putText(payload, "unidade_uf", jurisdicao.getUf());
        putText(payload, "unidade_sigla", jurisdicao.getSigla());
        putText(payload, "uf", jurisdicao.getUf());
        putText(payload, "estado", jurisdicao.getUf());
        putText(payload, "cidade", jurisdicao.getCidade());
        putText(payload, "comarca", jurisdicao.getCidade());
        putText(payload, "foro", jurisdicao.getForo());
        putText(payload, "secaoJudiciaria", jurisdicao.getSecaoJudiciaria());
        putText(payload, "subsecaoJudiciaria", jurisdicao.getSubsecaoJudiciaria());
        putText(payload, "circunscricao", jurisdicao.getCircunscricao());
        putText(payload, "tribunalCodigo", jurisdicao.getSigla());
        putText(payload, "tribunal", jurisdicao.getSigla());
        PayloadMaps.deepCopyWithoutNulls(jurisdicao.snapshotTerritorial()).forEach(payload::putIfAbsent);
    }

    private static void putText(Map<String, Object> target, String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        String normalized = value.trim();
        if (!normalized.isBlank()) {
            target.put(key, normalized);
        }
    }

    private static void putValue(Map<String, Object> target, String key, Object value) {
        if (key != null && !key.isBlank() && value != null) {
            target.put(key, value);
        }
    }

    private static BigDecimal normalizeBigDecimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
