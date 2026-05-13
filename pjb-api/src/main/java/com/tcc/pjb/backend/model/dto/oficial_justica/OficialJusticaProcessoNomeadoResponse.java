package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;
import java.util.List;

public record OficialJusticaProcessoNomeadoResponse(
        String territorio,
        Instant generatedAt,
        Scope scope,
        Summary summary,
        List<String> columns,
        List<FilterGroup> filtros,
        List<RitoBucket> organizacaoPorRito,
        List<ProcessoRow> rows,
        List<String> alerts
) {
    public OficialJusticaProcessoNomeadoResponse {
        columns = columns == null ? List.of() : List.copyOf(columns);
        filtros = filtros == null ? List.of() : List.copyOf(filtros);
        organizacaoPorRito = organizacaoPorRito == null ? List.of() : List.copyOf(organizacaoPorRito);
        rows = rows == null ? List.of() : List.copyOf(rows);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    public record Scope(
            String mode,
            String label,
            boolean institutionManaged,
            boolean cobreTodasAsVaras,
            List<String> varas,
            List<String> unidades
    ) {
        public Scope {
            varas = varas == null ? List.of() : List.copyOf(varas);
            unidades = unidades == null ? List.of() : List.copyOf(unidades);
        }
    }

    public record Summary(
            int totalRows,
            int comSigilo,
            int comPendenciaAtiva,
            int comAcessoLiberado,
            int comAcessoRestrito,
            int ritosCobertos,
            int varasCobertas
    ) {
    }

    public record FilterGroup(
            String key,
            String label,
            List<String> values
    ) {
        public FilterGroup {
            values = values == null ? List.of() : List.copyOf(values);
        }
    }

    public record RitoBucket(
            String rito,
            int total,
            List<String> processos
    ) {
        public RitoBucket {
            processos = processos == null ? List.of() : List.copyOf(processos);
        }
    }

    public record ProcessoRow(
            Long processoId,
            String processoNumero,
            String tribunal,
            String vara,
            String lotacao,
            String rito,
            String processoStatus,
            String faseAtual,
            Instant prazoFatalEm,
            String baseNomeacao,
            boolean acessoProcessoPermitido,
            String fundamentoAcesso,
            Long workItemVinculoId,
            String tipoVinculo,
            String statusVinculo,
            boolean possuiPendenciaAtiva,
            String proximaAcao,
            java.util.Map<String, Object> unidadeContexto,
            List<String> alerts
    ) {
        public ProcessoRow {
            unidadeContexto = unidadeContexto == null ? java.util.Map.of() : java.util.Map.copyOf(unidadeContexto);
            alerts = alerts == null ? List.of() : List.copyOf(alerts);
        }
    }
}
