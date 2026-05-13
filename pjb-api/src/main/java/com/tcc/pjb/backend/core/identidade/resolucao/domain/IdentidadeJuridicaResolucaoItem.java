package com.tcc.pjb.backend.core.identidade.resolucao.domain;

import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaSemente;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaVerticeTipo;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record IdentidadeJuridicaResolucaoItem(
        String codigo,
        String origem,
        String rotuloEntrada,
        IdentidadeJuridicaVerticeTipo tipoResolvido,
        String verticeId,
        String chaveCanonica,
        String rotuloResolvido,
        IdentidadeJuridicaResolucaoStatus status,
        double confianca,
        List<IdentidadeJuridicaSemente> sementes,
        List<String> fundamentos,
        Map<String, String> atributosNormalizados
) {
    public IdentidadeJuridicaResolucaoItem {
        codigo = Objects.toString(codigo, "").trim();
        origem = Objects.toString(origem, "").trim();
        rotuloEntrada = Objects.toString(rotuloEntrada, "").trim();
        tipoResolvido = Objects.requireNonNull(tipoResolvido, "tipoResolvido");
        verticeId = Objects.toString(verticeId, "").trim();
        chaveCanonica = Objects.toString(chaveCanonica, "").trim();
        rotuloResolvido = Objects.toString(rotuloResolvido, chaveCanonica).trim();
        status = Objects.requireNonNull(status, "status");
        confianca = Math.max(0d, Math.min(1d, confianca));
        sementes = sementes == null ? List.of() : List.copyOf(sementes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        atributosNormalizados = atributosNormalizados == null ? Map.of() : Map.copyOf(atributosNormalizados);
    }
}
