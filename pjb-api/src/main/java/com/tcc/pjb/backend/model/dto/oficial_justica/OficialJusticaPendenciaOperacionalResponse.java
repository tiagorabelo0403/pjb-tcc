package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;
import java.util.List;

public record OficialJusticaPendenciaOperacionalResponse(
        String territorio,
        Instant generatedAt,
        Scope scope,
        Summary summary,
        List<String> columns,
        List<FilterGroup> filtros,
        List<PendenciaRow> rows,
        List<String> alerts
) {
    public OficialJusticaPendenciaOperacionalResponse {
        columns = columns == null ? List.of() : List.copyOf(columns);
        filtros = filtros == null ? List.of() : List.copyOf(filtros);
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
            int atrasadas,
            int criticas,
            int aguardandoRastreio,
            int aguardandoJuntada,
            int aguardandoConfirmacaoExterna,
            int comAcessoProcessual,
            int semAcessoProcessual,
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

    public record PendenciaRow(
            Long workItemId,
            Long processoId,
            String processoNumero,
            String tribunal,
            String vara,
            String lotacao,
            String rito,
            String processoStatus,
            String faseAtual,
            String tipoPendencia,
            String prioridadeOperacional,
            Instant prazoFatalEm,
            boolean acessoProcessoPermitido,
            String fundamentoAcesso,
            String proximaAcao,
            List<String> dependenciasProcessuais,
            java.util.Map<String, Object> unidadeContexto,
            List<String> alerts
    ) {
        public PendenciaRow {
            dependenciasProcessuais = dependenciasProcessuais == null ? List.of() : List.copyOf(dependenciasProcessuais);
            unidadeContexto = unidadeContexto == null ? java.util.Map.of() : java.util.Map.copyOf(unidadeContexto);
            alerts = alerts == null ? List.of() : List.copyOf(alerts);
        }
    }
}
