package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record OficialJusticaDiligenciaQueueResponse(
        String territorio,
        Instant generatedAt,
        Scope scope,
        Summary summary,
        List<FilterGroup> filtros,
        List<FolderBucket> pastas,
        List<RitoBucket> organizacaoPorRito,
        List<Row> rows,
        List<String> alerts
) {
    public OficialJusticaDiligenciaQueueResponse {
        filtros = filtros == null ? List.of() : List.copyOf(filtros);
        pastas = pastas == null ? List.of() : List.copyOf(pastas);
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
            int atrasadas,
            int criticas,
            int aguardandoRetorno,
            int cumpridas,
            int bloqueadasParaEnvio,
            int comCalculoRelevante,
            int porRitoEspecial,
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

    public record FolderBucket(
            String code,
            String label,
            int count,
            String colorToken
    ) {
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

    public record Row(
            Long workItemId,
            Long processoId,
            String processoNumero,
            String tribunal,
            String vara,
            String lotacao,
            String rito,
            String ramo,
            String faseAtual,
            String processoStatus,
            String corAndamento,
            String pasta,
            String categoria,
            String prioridadeOperacional,
            String statusOperacional,
            String statusLabel,
            String corStatus,
            Instant prazoFatalEm,
            Instant ultimoMovimentoEm,
            int tentativasRealizadas,
            Instant janelaRetornoRecomendadaEm,
            boolean podeEnviarNoProcesso,
            String bloqueioEnvio,
            String alvoPrincipal,
            String comarca,
            String resumoProcessual,
            String fundamentoMissao,
            String calculadoraSugerida,
            @Schema(description = "Estado de execucao ativa da diligencia — polimórfico por tipo de cumprimento", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> execucaoViva,
            @Schema(description = "Contexto da unidade judicial executora — chaves variam por tribunal e tipo de diligencia", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> unidadeContexto,
            List<String> alertas,
            @Schema(description = "Acoes rapidas disponíveis — polimorficas por tipo de diligencia e fase", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> quickActions
    ) {
        public Row {
            execucaoViva = immutableObjectMap(execucaoViva);
            unidadeContexto = immutableObjectMap(unidadeContexto);
            alertas = alertas == null ? List.of() : List.copyOf(alertas);
            quickActions = immutableObjectMap(quickActions);
        }
    }

    private static Map<String, Object> immutableObjectMap(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Object> safe = new java.util.LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }
}

