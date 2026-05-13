package com.tcc.pjb.backend.model.dto.oficial_justica;

import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationResponse;
import java.time.Instant;
import java.util.List;

public record OficialJusticaEnderecoTriageResponse(
        String territorio,
        Instant generatedAt,
        Summary summary,
        List<String> columns,
        List<TriageRow> rows,
        DiligenceRouteOptimizationResponse rotaSugerida,
        List<String> alerts
) {
    public OficialJusticaEnderecoTriageResponse {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    public record Summary(
            int totalRows,
            int prioridadeCritica,
            int prioridadeAlta,
            int comEnderecoMaterializado,
            int semEnderecoMaterializado,
            int comSinalReceita,
            int comProntuarioAtivo,
            int comPendenciaFatal
    ) {
    }

    public record TriageRow(
            Long workItemId,
            Long processoId,
            String processoNumero,
            String targetPolo,
            String targetNome,
            String cpfMascarado,
            String processoStatus,
            String faseAtual,
            Instant prazoFatalEm,
            String prioridadeOperacional,
            int score,
            String melhorEndereco,
            String cidadeUf,
            String fonteEndereco,
            Double confiancaEndereco,
            boolean enderecoEstritoLiberado,
            int totalEnderecos,
            String receitaStatus,
            int processosAtivosPessoa,
            String prontuarioUri,
            String recomendacao,
            List<String> alertas
    ) {
        public TriageRow {
            alertas = alertas == null ? List.of() : List.copyOf(alertas);
        }
    }
}
