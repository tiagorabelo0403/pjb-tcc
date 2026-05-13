package com.tcc.pjb.backend.service.oficial_justica;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaAgendaOperacionalResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OficialJusticaAgendaPanelSupportTest {

    private final OficialJusticaAgendaPanelSupport support = new OficialJusticaAgendaPanelSupport();

    @Test
    void deveReordenarPorPesoDinamicoETerritorio() {
        OficialJusticaAgendaOperacionalResponse.StopRow pendente = row(5, 1L, "0001", "PENDENTE", "Centro:1", 0, null, false, null, null);
        OficialJusticaAgendaOperacionalResponse.StopRow atrasada = row(2, 2L, "0002", "ATRASADA", "Centro:1", 2, Instant.parse("2026-04-17T13:00:00Z"), true, "RETORNO", "Laranja");
        OficialJusticaAgendaOperacionalResponse.StopRow bloqueada = row(3, 3L, "0003", "BLOQUEADA", "Bairro:2", 0, null, false, null, null);

        List<OficialJusticaAgendaOperacionalResponse.StopRow> ordered = support.reorderRows(List.of(pendente, atrasada, bloqueada));

        assertThat(ordered).extracting(OficialJusticaAgendaOperacionalResponse.StopRow::workItemId).containsExactly(2L, 1L, 3L);
        assertThat(ordered.get(0).loteTerritorial()).contains("Centro:1");
        assertThat(ordered.get(0).ordem()).isEqualTo(1);
    }

    @Test
    void deveMontarResumoDeReplanejamentoEMapaDePainel() {
        OficialJusticaAgendaOperacionalResponse.StopRow replanejada = row(1, 11L, "00011", "AGUARDANDO_RETORNO", "Centro:1", 2, Instant.parse("2026-04-17T10:00:00Z"), true, "ENDERECO_DIVERGENTE_GEOFENCE", "Endereço divergente");
        OficialJusticaAgendaOperacionalResponse.StopRow emDiligencia = row(2, 12L, "00012", "EM_DILIGENCIA", "Centro:1", 1, Instant.parse("2026-04-17T09:00:00Z"), false, null, null);
        DiligenceRouteOptimizationResponse route = new DiligenceRouteOptimizationResponse(
                "oficial",
                null,
                5.0,
                25,
                Instant.parse("2026-04-17T18:00:00Z"),
                List.of(),
                List.of(),
                List.of(new DiligenceRouteOptimizationResponse.DeferredStop("11", "Mandado", "Replanejar após geofence")),
                Instant.parse("2026-04-17T08:00:00Z")
        );

        OficialJusticaAgendaOperacionalResponse.ReplanningSummary summary = support.buildReplanningSummary(List.of(replanejada, emDiligencia), route);
        OficialJusticaAgendaOperacionalResponse agenda = new OficialJusticaAgendaOperacionalResponse(
                "CE:Fortaleza",
                Instant.parse("2026-04-17T08:00:00Z"),
                new OficialJusticaAgendaOperacionalResponse.Scope("MODE", "Label", "JUSTICA_ESTADUAL", "COBERTURA", "TJCE", true, List.of("1ª Vara"), List.of("Fortaleza"), List.of("Comum")),
                new OficialJusticaAgendaOperacionalResponse.Summary(2, 2, 1, 1, 0, 2, 0, 0, 1, 1, 0),
                List.of(),
                List.of(),
                List.of(),
                support.buildStatusBuckets(List.of(replanejada, emDiligencia)),
                support.agendaColorLegend(),
                summary,
                List.of(replanejada, emDiligencia),
                List.of(),
                List.of("alerta")
        );
        Map<String, Object> painel = support.buildPainelResumo(agenda);

        assertThat(summary.reorderSuggested()).isTrue();
        assertThat(summary.routeVersion()).isGreaterThan(1);
        assertThat(summary.frustracoesEstruturadas()).hasSize(1);
        assertThat(summary.adiadas()).hasSize(1);
        assertThat(painel).containsKeys("scope", "summary", "replanejamentoVivo", "statusBuckets", "topStops");
    }

    private OficialJusticaAgendaOperacionalResponse.StopRow row(int ordem,
                                                                Long workItemId,
                                                                String processoNumero,
                                                                String status,
                                                                String microterritorio,
                                                                int tentativas,
                                                                Instant ultimaTentativa,
                                                                boolean replanejar,
                                                                String motivoCode,
                                                                String motivoLabel) {
        return new OficialJusticaAgendaOperacionalResponse.StopRow(
                ordem,
                workItemId,
                workItemId + 100,
                processoNumero,
                "Comum",
                "1ª Vara",
                "Fortaleza",
                "TJCE",
                "JUSTICA_ESTADUAL",
                "EM_ANDAMENTO",
                "HOJE",
                "ALTA",
                status,
                status,
                Instant.parse("2026-04-17T20:00:00Z"),
                Instant.parse("2026-04-17T12:00:00Z"),
                Instant.parse("2026-04-17T16:00:00Z"),
                "LOTE",
                10.0,
                20,
                "Rua 1",
                "Centro",
                microterritorio,
                null,
                "Alvo",
                "Resumo",
                "Fundamento",
                "Calc",
                "AMARELO",
                "LARANJA",
                tentativas,
                ultimaTentativa,
                motivoCode,
                motivoLabel,
                replanejar,
                replanejar ? "Motivo" : null,
                "RETORNO",
                true,
                null,
                Map.of(),
                List.of(replanejar ? "RETORNO_RECOMENDADO" : "OK")
        );
    }
}
