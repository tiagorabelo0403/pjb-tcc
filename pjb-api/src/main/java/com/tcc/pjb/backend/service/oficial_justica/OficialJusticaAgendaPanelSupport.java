package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaAgendaOperacionalResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class OficialJusticaAgendaPanelSupport {

    List<OficialJusticaAgendaOperacionalResponse.StopRow> reorderRows(List<OficialJusticaAgendaOperacionalResponse.StopRow> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        Map<String, Long> territorialMass = source.stream()
                .filter(row -> row.microterritorio() != null && !row.microterritorio().isBlank())
                .collect(Collectors.groupingBy(OficialJusticaAgendaOperacionalResponse.StopRow::microterritorio, LinkedHashMap::new, Collectors.counting()));
        boolean dynamic = source.stream().anyMatch(row -> row.replanejamentoRecomendado() || row.motivoFrustracaoEstruturado() != null || row.janelaRetornoRecomendadaEm() != null);
        List<OficialJusticaAgendaOperacionalResponse.StopRow> ordered = source.stream()
                .sorted(Comparator.comparingInt((OficialJusticaAgendaOperacionalResponse.StopRow row) -> dynamicOrderWeight(row, territorialMass)).reversed()
                        .thenComparing(OficialJusticaAgendaOperacionalResponse.StopRow::prazoFatalEm, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OficialJusticaAgendaOperacionalResponse.StopRow::ordem))
                .toList();
        List<OficialJusticaAgendaOperacionalResponse.StopRow> out = new ArrayList<>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            OficialJusticaAgendaOperacionalResponse.StopRow row = ordered.get(i);
            String lote = row.microterritorio() == null ? null : row.microterritorio() + " · " + territorialMass.getOrDefault(row.microterritorio(), 1L) + " dilig.";
            out.add(copyRow(row, dynamic ? i + 1 : row.ordem(), lote));
        }
        return List.copyOf(out);
    }

    List<OficialJusticaAgendaOperacionalResponse.StatusBucket> buildStatusBuckets(List<OficialJusticaAgendaOperacionalResponse.StopRow> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(OficialJusticaAgendaOperacionalResponse.StopRow::statusOperacional, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> new OficialJusticaAgendaOperacionalResponse.StatusBucket(
                        entry.getKey(),
                        resolveStatusLabel(entry.getKey()),
                        resolveStatusColor(entry.getKey(), null),
                        entry.getValue().size(),
                        entry.getValue().stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::processoNumero).filter(Objects::nonNull).distinct().toList()
                ))
                .toList();
    }

    Map<String, String> agendaColorLegend() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        out.put("VERMELHO", "Atrasada ou prazo fatal vencido.");
        out.put("AMARELO", "Pendente dentro da janela do dia.");
        out.put("LARANJA", "Tentativa frustrada ou retorno recomendado.");
        out.put("ROXO", "Dependência cartorária, juntada ou expedição complementar.");
        out.put("AZUL", "Execução em campo, rota ativa ou movimento operacional do dia.");
        out.put("VERDE", "Concluída.");
        out.put("CINZA_AZULADO", "Bloqueada para novo envio ou em conferência apenas.");
        return Collections.unmodifiableMap(out);
    }

    OficialJusticaAgendaOperacionalResponse.ReplanningSummary buildReplanningSummary(List<OficialJusticaAgendaOperacionalResponse.StopRow> rows,
                                                                                     DiligenceRouteOptimizationResponse route) {
        List<OficialJusticaAgendaOperacionalResponse.TerritorialBatch> territorialBatches = rows.stream()
                .filter(row -> row.microterritorio() != null && !row.microterritorio().isBlank())
                .collect(Collectors.groupingBy(OficialJusticaAgendaOperacionalResponse.StopRow::microterritorio, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, List<OficialJusticaAgendaOperacionalResponse.StopRow>> entry) -> entry.getValue().size()).reversed())
                .limit(8)
                .map(entry -> new OficialJusticaAgendaOperacionalResponse.TerritorialBatch(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::processoNumero).filter(Objects::nonNull).distinct().toList()
                ))
                .toList();
        Map<Long, OficialJusticaAgendaOperacionalResponse.StopRow> rowsByWorkItem = rows.stream()
                .filter(row -> row.workItemId() != null)
                .collect(Collectors.toMap(OficialJusticaAgendaOperacionalResponse.StopRow::workItemId, row -> row, (left, right) -> left, LinkedHashMap::new));
        List<OficialJusticaAgendaOperacionalResponse.DeferredItem> deferred = route == null || route.adiadas() == null
                ? List.of()
                : route.adiadas().stream().map(item -> {
                    Long workItemId = OficialJusticaAgendaSupportUtils.parseId(item.id());
                    OficialJusticaAgendaOperacionalResponse.StopRow row = workItemId == null ? null : rowsByWorkItem.get(workItemId);
                    return new OficialJusticaAgendaOperacionalResponse.DeferredItem(
                            workItemId,
                            row != null ? row.processoNumero() : null,
                            OficialJusticaAgendaSupportUtils.firstNonBlank(item.motivo(), row != null ? resolveReplanReasonFromStop(row) : null, "Replanejamento territorial sugerido")
                    );
                }).toList();
        Instant suggested = rows.stream()
                .filter(row -> row.replanejamentoRecomendado() || "AGUARDANDO_RETORNO".equals(row.statusOperacional()) || "ATRASADA".equals(row.statusOperacional()))
                .map(OficialJusticaAgendaOperacionalResponse.StopRow::janelaRetornoRecomendadaEm)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElseGet(() -> rows.stream().anyMatch(row -> "EM_DILIGENCIA".equals(row.statusOperacional())) ? Instant.now().plusSeconds(1800) : null);
        Instant lastReorderedAt = rows.stream()
                .map(OficialJusticaAgendaOperacionalResponse.StopRow::ultimaTentativaEm)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(rows.stream().anyMatch(OficialJusticaAgendaOperacionalResponse.StopRow::replanejamentoRecomendado) ? Instant.now() : null);
        List<String> motivos = rows.stream()
                .flatMap(row -> dominantReplanSignals(row).stream())
                .distinct()
                .limit(10)
                .toList();
        List<OficialJusticaAgendaOperacionalResponse.FrustrationBucket> frustrationBuckets = rows.stream()
                .filter(row -> row.motivoFrustracaoEstruturado() != null && !row.motivoFrustracaoEstruturado().isBlank())
                .collect(Collectors.groupingBy(OficialJusticaAgendaOperacionalResponse.StopRow::motivoFrustracaoEstruturado, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, List<OficialJusticaAgendaOperacionalResponse.StopRow>> entry) -> entry.getValue().size()).reversed())
                .map(entry -> new OficialJusticaAgendaOperacionalResponse.FrustrationBucket(
                        entry.getKey(),
                        entry.getValue().get(0).motivoFrustracaoLabel(),
                        entry.getValue().size(),
                        entry.getValue().stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::processoNumero).filter(Objects::nonNull).distinct().toList()
                ))
                .toList();
        List<OficialJusticaAgendaOperacionalResponse.AddressAttemptSummary> attempts = rows.stream()
                .filter(row -> row.tentativasRealizadas() > 0 || row.janelaRetornoRecomendadaEm() != null)
                .sorted(Comparator.comparingInt((OficialJusticaAgendaOperacionalResponse.StopRow row) -> dynamicOrderWeight(row)).reversed()
                        .thenComparing(OficialJusticaAgendaOperacionalResponse.StopRow::ultimaTentativaEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(16)
                .map(row -> new OficialJusticaAgendaOperacionalResponse.AddressAttemptSummary(
                        row.workItemId(),
                        row.processoNumero(),
                        row.enderecoReferencia(),
                        row.bairro(),
                        row.microterritorio(),
                        row.statusOperacional(),
                        row.motivoFrustracaoEstruturado(),
                        row.motivoFrustracaoLabel(),
                        row.tentativasRealizadas(),
                        row.ultimaTentativaEm(),
                        row.janelaRetornoRecomendadaEm(),
                        row.janelaRetornoLabel(),
                        row.corStatus()
                ))
                .toList();
        boolean reorder = !deferred.isEmpty() || rows.stream().anyMatch(row -> row.replanejamentoRecomendado() || "EM_DILIGENCIA".equals(row.statusOperacional()));
        return new OficialJusticaAgendaOperacionalResponse.ReplanningSummary(
                reorder,
                computeRouteVersion(rows, deferred, frustrationBuckets),
                (int) rows.stream().filter(row -> "EM_DILIGENCIA".equals(row.statusOperacional())).count(),
                (int) rows.stream().filter(row -> "AGUARDANDO_RETORNO".equals(row.statusOperacional())).count(),
                (int) rows.stream().filter(row -> "ATRASADA".equals(row.statusOperacional())).count(),
                (int) rows.stream().filter(row -> row.janelaRetornoRecomendadaEm() != null && row.janelaRetornoRecomendadaEm().isBefore(Instant.now().plusSeconds(4 * 3600))).count(),
                suggested,
                lastReorderedAt,
                motivos,
                territorialBatches,
                frustrationBuckets,
                attempts,
                deferred
        );
    }

    Map<String, Object> buildPainelResumo(OficialJusticaAgendaOperacionalResponse agenda) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("mode", "OFICIAL_AGENDA_OPERACIONAL_TERRITORIAL_V2");
        LinkedHashMap<String, Object> scope = new LinkedHashMap<>();
        OficialJusticaAgendaSupportUtils.putIfNotNull(scope, "esferaAtuacao", agenda.scope() != null ? agenda.scope().esferaAtuacao() : null);
        OficialJusticaAgendaSupportUtils.putIfNotNull(scope, "coberturaOrganizacional", agenda.scope() != null ? agenda.scope().coberturaOrganizacional() : null);
        OficialJusticaAgendaSupportUtils.putIfNotNull(scope, "tribunalPrincipal", agenda.scope() != null ? agenda.scope().tribunalPrincipal() : null);
        scope.put("cobreTodasAsVaras", agenda.scope() != null && agenda.scope().cobreTodasAsVaras());
        if (!scope.isEmpty()) {
            out.put("scope", Map.copyOf(scope));
        }
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalStops", agenda.summary().totalStops());
        summary.put("criticas", agenda.summary().criticas());
        summary.put("atrasadas", agenda.summary().atrasadas());
        summary.put("federais", agenda.summary().federais());
        summary.put("estaduais", agenda.summary().estaduais());
        summary.put("concluidas", agenda.summary().concluidas());
        summary.put("salasBalcaoVirtual", agenda.summary().salasBalcaoVirtual());
        out.put("summary", Map.copyOf(summary));
        out.put("agendaPath", "/api/v1/oficial-justica/agenda-operacional");
        out.put("balcaoVirtualPath", "/api/v1/oficial-justica/balcao-virtual/salas");
        out.put("legendaCores", agenda.legendaCores());
        if (agenda.replanejamentoVivo() != null) {
            LinkedHashMap<String, Object> repl = new LinkedHashMap<>();
            repl.put("reorderSuggested", agenda.replanejamentoVivo().reorderSuggested());
            repl.put("routeVersion", agenda.replanejamentoVivo().routeVersion());
            repl.put("emDiligencia", agenda.replanejamentoVivo().emDiligencia());
            repl.put("aguardandoRetorno", agenda.replanejamentoVivo().aguardandoRetorno());
            repl.put("atrasadas", agenda.replanejamentoVivo().atrasadas());
            repl.put("candidatasRetornoImediato", agenda.replanejamentoVivo().candidatasRetornoImediato());
            repl.put("suggestedReplanAt", agenda.replanejamentoVivo().suggestedReplanAt());
            repl.put("lastReorderedAt", agenda.replanejamentoVivo().lastReorderedAt());
            repl.put("motivosDominantes", agenda.replanejamentoVivo().motivosDominantes());
            repl.put("lotesTerritoriais", agenda.replanejamentoVivo().lotesTerritoriais().stream().map(batch -> {
                LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                row.put("microterritorio", batch.microterritorio());
                row.put("total", batch.total());
                row.put("processos", batch.processos());
                return OficialJusticaAgendaSupportUtils.safeCopy(row);
            }).toList());
            repl.put("frustracoesEstruturadas", agenda.replanejamentoVivo().frustracoesEstruturadas().stream().map(bucket -> {
                LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                row.put("code", bucket.code());
                row.put("label", bucket.label());
                row.put("total", bucket.total());
                row.put("processos", bucket.processos());
                return OficialJusticaAgendaSupportUtils.safeCopy(row);
            }).toList());
            repl.put("tentativasPorEndereco", agenda.replanejamentoVivo().tentativasPorEndereco().stream().map(item -> {
                LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                row.put("workItemId", item.workItemId());
                row.put("processoNumero", item.processoNumero());
                row.put("endereco", item.endereco());
                row.put("bairro", item.bairro());
                row.put("microterritorio", item.microterritorio());
                row.put("statusOperacional", item.statusOperacional());
                row.put("motivoFrustracaoCode", item.motivoFrustracaoCode());
                row.put("motivoFrustracaoLabel", item.motivoFrustracaoLabel());
                row.put("tentativas", item.tentativas());
                row.put("ultimaTentativaEm", item.ultimaTentativaEm());
                row.put("janelaRetornoEm", item.janelaRetornoEm());
                row.put("janelaRetornoLabel", item.janelaRetornoLabel());
                row.put("colorToken", item.colorToken());
                return OficialJusticaAgendaSupportUtils.safeCopy(row);
            }).toList());
            repl.put("adiadas", agenda.replanejamentoVivo().adiadas().stream().map(item -> {
                LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                row.put("workItemId", item.workItemId());
                row.put("processoNumero", item.processoNumero());
                row.put("motivo", item.motivo());
                return OficialJusticaAgendaSupportUtils.safeCopy(row);
            }).toList());
            out.put("replanejamentoVivo", OficialJusticaAgendaSupportUtils.safeCopy(repl));
        }
        out.put("statusBuckets", agenda.organizacaoPorStatus().stream().map(bucket -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("statusOperacional", bucket.statusOperacional());
            row.put("label", bucket.label());
            row.put("colorToken", bucket.colorToken());
            row.put("total", bucket.total());
            return OficialJusticaAgendaSupportUtils.safeCopy(row);
        }).toList());
        out.put("topStops", agenda.agenda().stream().limit(6).map(stop -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("ordem", stop.ordem());
            row.put("processoNumero", stop.processoNumero());
            row.put("vara", stop.vara());
            row.put("esfera", stop.esfera());
            row.put("prioridadeOperacional", stop.prioridadeOperacional());
            row.put("statusOperacional", stop.statusOperacional());
            row.put("corStatus", stop.corStatus());
            row.put("corAndamento", stop.corAndamento());
            row.put("chegadaEstimada", stop.chegadaEstimada());
            row.put("classificacaoRota", stop.classificacaoRota());
            OficialJusticaAgendaSupportUtils.putIfNotNull(row, "bairro", stop.bairro());
            OficialJusticaAgendaSupportUtils.putIfNotNull(row, "microterritorio", stop.microterritorio());
            OficialJusticaAgendaSupportUtils.putIfNotNull(row, "loteTerritorial", stop.loteTerritorial());
            OficialJusticaAgendaSupportUtils.putIfNotNull(row, "ultimaTentativaEm", stop.ultimaTentativaEm());
            OficialJusticaAgendaSupportUtils.putIfNotNull(row, "motivoFrustracaoEstruturado", stop.motivoFrustracaoEstruturado());
            OficialJusticaAgendaSupportUtils.putIfNotNull(row, "motivoFrustracaoLabel", stop.motivoFrustracaoLabel());
            return OficialJusticaAgendaSupportUtils.safeCopy(row);
        }).toList());
        out.put("alerts", agenda.alerts());
        return OficialJusticaAgendaSupportUtils.safeCopy(out);
    }

    private List<String> dominantReplanSignals(OficialJusticaAgendaOperacionalResponse.StopRow row) {
        if (row == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        if ("AGUARDANDO_RETORNO".equals(row.statusOperacional())) {
            out.add("retorno_recomendado_por_tentativa_frustrada");
        }
        if ("ATRASADA".equals(row.statusOperacional())) {
            out.add("prazo_fatal_expirado");
        }
        if ("EM_DILIGENCIA".equals(row.statusOperacional())) {
            out.add("rota_em_execucao");
        }
        if (row.alertas().contains("CHEGADA_FORA_GEOFENCE")) {
            out.add("geofence_divergente");
        }
        if (row.alertas().contains("RETORNO_RECOMENDADO")) {
            out.add("janela_de_retorno_aberta");
        }
        if (row.motivoFrustracaoEstruturado() != null) {
            out.add("frustracao_estruturada_" + row.motivoFrustracaoEstruturado().toLowerCase());
        }
        if (!row.podeEnviarNoProcesso()) {
            out.add("modo_somente_leitura_pos_cumprimento");
        }
        return out;
    }

    private String resolveReplanReasonFromStop(OficialJusticaAgendaOperacionalResponse.StopRow row) {
        if (row == null) {
            return null;
        }
        if (row.motivoFrustracaoLabel() != null) {
            return row.motivoFrustracaoLabel();
        }
        if (row.motivoReplanejamento() != null) {
            return row.motivoReplanejamento();
        }
        return "Replanejamento territorial sugerido";
    }

    private int dynamicOrderWeight(OficialJusticaAgendaOperacionalResponse.StopRow row,
                                   Map<String, Long> territorialMass) {
        int weight = dynamicOrderWeight(row);
        if (row.microterritorio() != null) {
            weight += territorialMass.getOrDefault(row.microterritorio(), 0L).intValue();
        }
        return weight;
    }

    private int dynamicOrderWeight(OficialJusticaAgendaOperacionalResponse.StopRow row) {
        int weight = switch (OficialJusticaAgendaSupportUtils.firstNonBlank(row.statusOperacional(), "PENDENTE")) {
            case "ATRASADA" -> 500;
            case "AGUARDANDO_RETORNO" -> 450;
            case "EM_DILIGENCIA" -> 420;
            case "PENDENTE" -> 300;
            case "AGUARDANDO_CARTORIO" -> 150;
            case "BLOQUEADA" -> 120;
            default -> 10;
        };
        if (row.replanejamentoRecomendado()) {
            weight += 80;
        }
        if (row.janelaRetornoRecomendadaEm() != null && row.janelaRetornoRecomendadaEm().isBefore(Instant.now().plus(4, ChronoUnit.HOURS))) {
            weight += 60;
        }
        if (row.ultimaTentativaEm() != null) {
            weight += 40;
        }
        if (row.motivoFrustracaoEstruturado() != null) {
            weight += 30;
        }
        if (row.prazoFatalEm() != null && row.prazoFatalEm().isBefore(Instant.now().plus(2, ChronoUnit.HOURS))) {
            weight += 70;
        }
        return weight + Math.min(25, row.tentativasRealizadas() * 5);
    }

    private OficialJusticaAgendaOperacionalResponse.StopRow copyRow(OficialJusticaAgendaOperacionalResponse.StopRow row,
                                                                    int ordem,
                                                                    String loteTerritorial) {
        return new OficialJusticaAgendaOperacionalResponse.StopRow(
                ordem,
                row.workItemId(),
                row.processoId(),
                row.processoNumero(),
                row.rito(),
                row.vara(),
                row.lotacao(),
                row.tribunal(),
                row.esfera(),
                row.processoStatus(),
                row.pasta(),
                row.prioridadeOperacional(),
                row.statusOperacional(),
                row.statusLabel(),
                row.prazoFatalEm(),
                row.chegadaEstimada(),
                row.janelaRetornoRecomendadaEm(),
                row.classificacaoRota(),
                row.distanciaTrechoKm(),
                row.deslocamentoMinutos(),
                row.enderecoReferencia(),
                row.bairro(),
                row.microterritorio(),
                loteTerritorial,
                row.alvoPrincipal(),
                row.resumoProcessual(),
                row.fundamentoMissao(),
                row.calculadoraSugerida(),
                row.corAndamento(),
                row.corStatus(),
                row.tentativasRealizadas(),
                row.ultimaTentativaEm(),
                row.motivoFrustracaoEstruturado(),
                row.motivoFrustracaoLabel(),
                row.replanejamentoRecomendado(),
                row.motivoReplanejamento(),
                row.janelaRetornoLabel(),
                row.podeEnviarNoProcesso(),
                row.bloqueioEnvio(),
                row.quickActions(),
                row.alertas()
        );
    }

    private int computeRouteVersion(List<OficialJusticaAgendaOperacionalResponse.StopRow> rows,
                                    List<OficialJusticaAgendaOperacionalResponse.DeferredItem> deferred,
                                    List<OficialJusticaAgendaOperacionalResponse.FrustrationBucket> frustrationBuckets) {
        int base = 1;
        base += (int) rows.stream().filter(OficialJusticaAgendaOperacionalResponse.StopRow::replanejamentoRecomendado).count();
        base += deferred.size();
        base += frustrationBuckets.size();
        return Math.max(1, base);
    }

    private String resolveStatusLabel(String statusOperacional) {
        return switch (statusOperacional) {
            case "CONCLUIDA" -> "Concluída";
            case "BLOQUEADA" -> "Bloqueada para envio";
            case "ATRASADA" -> "Atrasada";
            case "AGUARDANDO_RETORNO" -> "Aguardando retorno";
            case "AGUARDANDO_CARTORIO" -> "Aguardando cartório";
            case "EM_DILIGENCIA" -> "Em diligência";
            default -> "Pendente";
        };
    }

    private String resolveStatusColor(String statusOperacional, String corAndamento) {
        return switch (statusOperacional) {
            case "CONCLUIDA" -> "VERDE";
            case "BLOQUEADA" -> "CINZA_AZULADO";
            case "ATRASADA" -> "VERMELHO";
            case "AGUARDANDO_RETORNO" -> "LARANJA";
            case "AGUARDANDO_CARTORIO" -> "ROXO";
            case "EM_DILIGENCIA" -> "AZUL";
            default -> OficialJusticaAgendaSupportUtils.firstNonBlank(corAndamento, "AMARELO");
        };
    }
}
