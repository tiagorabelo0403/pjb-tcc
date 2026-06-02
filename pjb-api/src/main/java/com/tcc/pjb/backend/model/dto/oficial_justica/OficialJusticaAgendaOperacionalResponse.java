package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.Objects;

public record OficialJusticaAgendaOperacionalResponse(
        String territorio,
        Instant generatedAt,
        Scope scope,
        Summary summary,
        List<FilterGroup> filtros,
        List<AgendaFolder> pastas,
        List<RitoBucket> organizacaoPorRito,
        List<StatusBucket> organizacaoPorStatus,
        Map<String, String> legendaCores,
        ReplanningSummary replanejamentoVivo,
        List<StopRow> agenda,
        List<VirtualDeskRoom> balcaoVirtualSugerido,
        List<String> alerts
) {
    public OficialJusticaAgendaOperacionalResponse {
        filtros = filtros == null ? List.of() : List.copyOf(filtros);
        pastas = pastas == null ? List.of() : List.copyOf(pastas);
        organizacaoPorRito = organizacaoPorRito == null ? List.of() : List.copyOf(organizacaoPorRito);
        organizacaoPorStatus = organizacaoPorStatus == null ? List.of() : List.copyOf(organizacaoPorStatus);
        legendaCores = immutableStringMap(legendaCores);
        agenda = agenda == null ? List.of() : List.copyOf(agenda);
        balcaoVirtualSugerido = balcaoVirtualSugerido == null ? List.of() : List.copyOf(balcaoVirtualSugerido);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    public record Scope(
            String mode,
            String label,
            String esferaAtuacao,
            String coberturaOrganizacional,
            String tribunalPrincipal,
            boolean cobreTodasAsVaras,
            List<String> varas,
            List<String> lotacoes,
            List<String> ritos
    ) {
        public Scope {
            varas = varas == null ? List.of() : List.copyOf(varas);
            lotacoes = lotacoes == null ? List.of() : List.copyOf(lotacoes);
            ritos = ritos == null ? List.of() : List.copyOf(ritos);
        }
    }

    public record Summary(
            int totalStops,
            int pendentes,
            int criticas,
            int atrasadas,
            int federais,
            int estaduais,
            int concluidas,
            int bloqueadasParaEnvio,
            int varasCobertas,
            int ritosCobertos,
            int salasBalcaoVirtual
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

    public record AgendaFolder(
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

    public record StatusBucket(
            String statusOperacional,
            String label,
            String colorToken,
            int total,
            List<String> processos
    ) {
        public StatusBucket {
            processos = processos == null ? List.of() : List.copyOf(processos);
        }
    }

    public record ReplanningSummary(
            boolean reorderSuggested,
            int routeVersion,
            int emDiligencia,
            int aguardandoRetorno,
            int atrasadas,
            int candidatasRetornoImediato,
            Instant suggestedReplanAt,
            Instant lastReorderedAt,
            List<String> motivosDominantes,
            List<TerritorialBatch> lotesTerritoriais,
            List<FrustrationBucket> frustracoesEstruturadas,
            List<AddressAttemptSummary> tentativasPorEndereco,
            List<DeferredItem> adiadas
    ) {
        public ReplanningSummary {
            motivosDominantes = motivosDominantes == null ? List.of() : List.copyOf(motivosDominantes);
            lotesTerritoriais = lotesTerritoriais == null ? List.of() : List.copyOf(lotesTerritoriais);
            frustracoesEstruturadas = frustracoesEstruturadas == null ? List.of() : List.copyOf(frustracoesEstruturadas);
            tentativasPorEndereco = tentativasPorEndereco == null ? List.of() : List.copyOf(tentativasPorEndereco);
            adiadas = adiadas == null ? List.of() : List.copyOf(adiadas);
        }
    }

    public record TerritorialBatch(
            String microterritorio,
            int total,
            List<String> processos
    ) {
        public TerritorialBatch {
            processos = processos == null ? List.of() : List.copyOf(processos);
        }
    }

    public record DeferredItem(
            Long workItemId,
            String processoNumero,
            String motivo
    ) {
    }

    public record FrustrationBucket(
            String code,
            String label,
            int total,
            List<String> processos
    ) {
        public FrustrationBucket {
            processos = processos == null ? List.of() : List.copyOf(processos);
        }
    }

    public record AddressAttemptSummary(
            Long workItemId,
            String processoNumero,
            String endereco,
            String bairro,
            String microterritorio,
            String statusOperacional,
            String motivoFrustracaoCode,
            String motivoFrustracaoLabel,
            int tentativas,
            Instant ultimaTentativaEm,
            Instant janelaRetornoEm,
            String janelaRetornoLabel,
            String colorToken
    ) {
    }

    public record StopRow(
            int ordem,
            Long workItemId,
            Long processoId,
            String processoNumero,
            String rito,
            String vara,
            String lotacao,
            String tribunal,
            String esfera,
            String processoStatus,
            String pasta,
            String prioridadeOperacional,
            String statusOperacional,
            String statusLabel,
            Instant prazoFatalEm,
            Instant chegadaEstimada,
            Instant janelaRetornoRecomendadaEm,
            String classificacaoRota,
            double distanciaTrechoKm,
            long deslocamentoMinutos,
            String enderecoReferencia,
            String bairro,
            String microterritorio,
            String loteTerritorial,
            String alvoPrincipal,
            String resumoProcessual,
            String fundamentoMissao,
            String calculadoraSugerida,
            String corAndamento,
            String corStatus,
            int tentativasRealizadas,
            Instant ultimaTentativaEm,
            String motivoFrustracaoEstruturado,
            String motivoFrustracaoLabel,
            boolean replanejamentoRecomendado,
            String motivoReplanejamento,
            String janelaRetornoLabel,
            boolean podeEnviarNoProcesso,
            String bloqueioEnvio,
            @Schema(description = "Acoes rapidas disponíveis — polimorficas por tipo de diligencia e fase", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> quickActions,
            List<String> alertas
    ) {
        public StopRow {
            quickActions = immutableObjectMap(quickActions);
            alertas = alertas == null ? List.of() : List.copyOf(alertas);
        }
    }

    public record VirtualDeskRoom(
            String roomKey,
            Long processoId,
            String processoNumero,
            String label,
            String organDisplay,
            String instance,
            String lane,
            String esfera,
            String inboxKey,
            String historyPath,
            String sendPath,
            boolean chatEnabled
    ) {
    }

    private static Map<String, String> immutableStringMap(Map<String, String> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }

    private static Map<String, Object> immutableObjectMap(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && !key.isBlank() && Objects.nonNull(value)) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }
}

