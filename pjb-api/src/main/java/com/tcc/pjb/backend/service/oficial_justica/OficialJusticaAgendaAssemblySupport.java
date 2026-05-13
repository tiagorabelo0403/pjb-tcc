package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaAgendaOperacionalResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaBalcaoVirtualChatResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaDiligenciaQueueResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
class OficialJusticaAgendaAssemblySupport {

    private final OficialJusticaCommunicationFormalModelService communicationFormalModelService;

    OficialJusticaAgendaAssemblySupport(OficialJusticaCommunicationFormalModelService communicationFormalModelService) {
        this.communicationFormalModelService = Objects.requireNonNull(communicationFormalModelService);
    }

    DiligenceRouteOptimizationRequest.StopInput toStopInput(OficialJusticaDiligenciaQueueResponse.Row row) {
        return new DiligenceRouteOptimizationRequest.StopInput(
                row.workItemId() != null ? String.valueOf(row.workItemId()) : row.processoNumero(),
                OficialJusticaAgendaSupportUtils.firstNonBlank(row.categoria(), row.processoNumero(), "Diligência"),
                composeAddress(row),
                null,
                null,
                prioridadeNumerica(row.prioridadeOperacional()),
                row.prazoFatalEm(),
                row.janelaRetornoRecomendadaEm() != null ? row.janelaRetornoRecomendadaEm().atZone(ZoneOffset.UTC).getHour() : null,
                row.pasta() != null && row.pasta().contains("HOJE") ? 20 : 18
        );
    }

    List<OficialJusticaAgendaOperacionalResponse.VirtualDeskRoom> buildDeskRooms(OficialJusticaBalcaoVirtualChatResponse balcao) {
        return balcao.salas().stream()
                .map(room -> new OficialJusticaAgendaOperacionalResponse.VirtualDeskRoom(
                        room.roomKey(),
                        room.processoId(),
                        room.processoNumero(),
                        room.organDisplay() + " / " + room.lane(),
                        room.organDisplay(),
                        room.instance(),
                        room.lane(),
                        room.esfera(),
                        room.inboxKey(),
                        room.historyPath(),
                        room.sendPath(),
                        room.enabled()
                )).toList();
    }

    List<String> buildAlerts(OficialJusticaDiligenciaQueueResponse fila,
                             DiligenceRouteOptimizationResponse route,
                             List<OficialJusticaAgendaOperacionalResponse.StopRow> rows,
                             List<OficialJusticaAgendaOperacionalResponse.VirtualDeskRoom> deskRooms) {
        List<String> alerts = new ArrayList<>(fila.alerts());
        if (route != null) {
            alerts.addAll(OficialJusticaAgendaSupportUtils.nonBlank(route.warnings()));
            if (route.adiadas() != null && !route.adiadas().isEmpty()) {
                alerts.add("Há diligências adiadas na agenda e a rota do dia deve ser replanejada após os primeiros cumprimentos.");
            }
        }
        if (rows.stream().anyMatch(row -> row.bairro() != null && !row.bairro().isBlank())) {
            alerts.add("A agenda territorial agrupou diligências por bairro e microterritório para reduzir deslocamento improdutivo do oficial.");
        }
        if (rows.stream().anyMatch(row -> row.motivoFrustracaoEstruturado() != null)) {
            alerts.add("Existem motivos estruturados de frustração na agenda; reavalie a ordem da rota e a janela de retorno antes do próximo deslocamento.");
        }
        if (deskRooms.isEmpty()) {
            alerts.add("Nenhuma sala de balcão virtual foi aberta automaticamente na agenda porque ainda não há processos vinculados suficientes nesta visão.");
        }
        return List.copyOf(OficialJusticaAgendaSupportUtils.nonBlank(alerts).stream().distinct().toList());
    }

    OficialJusticaAgendaOperacionalResponse.Scope buildScope(Usuario usuario,
                                                             OficialJusticaDiligenciaQueueResponse fila,
                                                             List<OficialJusticaAgendaOperacionalResponse.StopRow> rows,
                                                             List<String> lotacoes,
                                                             List<String> ritos) {
        return new OficialJusticaAgendaOperacionalResponse.Scope(
                fila.scope() != null ? fila.scope().mode() : "NOMEACAO_DIRETA_CONTROLADA",
                fila.scope() != null ? fila.scope().label() : "Agenda derivada da fila viva do oficial",
                resolveEsferaAgenda(usuario, rows),
                coberturaLabel(usuario, rows),
                tribunalPrincipal(rows),
                fila.scope() != null && fila.scope().cobreTodasAsVaras(),
                fila.scope() != null ? fila.scope().varas() : List.of(),
                lotacoes,
                ritos
        );
    }

    OficialJusticaAgendaOperacionalResponse.Summary buildSummary(OficialJusticaDiligenciaQueueResponse fila,
                                                                 List<OficialJusticaAgendaOperacionalResponse.StopRow> rows,
                                                                 int deskRoomsSize) {
        return new OficialJusticaAgendaOperacionalResponse.Summary(
                rows.size(),
                (int) rows.stream().filter(row -> !"CONCLUIDA".equals(row.statusOperacional())).count(),
                fila.summary().criticas(),
                fila.summary().atrasadas(),
                (int) rows.stream().filter(row -> "JUSTICA_FEDERAL".equals(row.esfera())).count(),
                (int) rows.stream().filter(row -> "JUSTICA_ESTADUAL".equals(row.esfera())).count(),
                (int) rows.stream().filter(row -> "CONCLUIDA".equals(row.statusOperacional())).count(),
                fila.summary().bloqueadasParaEnvio(),
                fila.summary().varasCobertas(),
                fila.summary().ritosCobertos(),
                deskRoomsSize
        );
    }

    List<OficialJusticaAgendaOperacionalResponse.FilterGroup> buildFilterGroups(List<OficialJusticaAgendaOperacionalResponse.StopRow> rows) {
        return List.of(
                new OficialJusticaAgendaOperacionalResponse.FilterGroup("rito", "Rito processual", OficialJusticaAgendaSupportUtils.unique(rows.stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::rito).toList())),
                new OficialJusticaAgendaOperacionalResponse.FilterGroup("vara", "Vara / lotação", OficialJusticaAgendaSupportUtils.unique(rows.stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::vara).toList())),
                new OficialJusticaAgendaOperacionalResponse.FilterGroup("esfera", "Esfera", OficialJusticaAgendaSupportUtils.unique(rows.stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::esfera).toList())),
                new OficialJusticaAgendaOperacionalResponse.FilterGroup("justicaAxis", "Justiça / malha do oficial", OficialJusticaAgendaSupportUtils.unique(rows.stream().map(row -> stringFromQuickAction(row.quickActions(), "formalModel.justicaAxis")).toList())),
                new OficialJusticaAgendaOperacionalResponse.FilterGroup("naturezaComunicacao", "Natureza da diligência pessoal", OficialJusticaAgendaSupportUtils.unique(rows.stream().map(row -> stringFromQuickAction(row.quickActions(), "formalModel.naturezaComunicacao")).toList())),
                new OficialJusticaAgendaOperacionalResponse.FilterGroup("pasta", "Pasta operacional", OficialJusticaAgendaSupportUtils.unique(rows.stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::pasta).toList())),
                new OficialJusticaAgendaOperacionalResponse.FilterGroup("status", "Status operacional", OficialJusticaAgendaSupportUtils.unique(rows.stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::statusLabel).toList())),
                new OficialJusticaAgendaOperacionalResponse.FilterGroup("bairro", "Bairro / microterritório", OficialJusticaAgendaSupportUtils.unique(rows.stream().map(row -> OficialJusticaAgendaSupportUtils.firstNonBlank(row.bairro(), row.microterritorio())).toList()))
        );
    }

    OficialJusticaAgendaOperacionalResponse.StopRow toAgendaRow(Usuario usuario,
                                                                OficialJusticaDiligenciaQueueResponse.Row row,
                                                                DiligenceRouteOptimizationResponse.OptimizedStop stop,
                                                                OficialJusticaAgendaTerritorialHint hint,
                                                                OficialJusticaAgendaLiveEventDigest digest) {
        String esfera = resolveEsfera(usuario, row.tribunal(), row.vara());
        String endereco = resolveAddressReference(row, hint);
        String bairro = resolveBairro(hint);
        String microterritorio = resolveMicroterritorio(row, hint, endereco);
        List<String> alerts = new ArrayList<>(row.alertas());
        String statusOperacional = OficialJusticaAgendaSupportUtils.firstNonBlank(row.statusOperacional(), resolveStatusOperacional(row));
        String statusLabel = OficialJusticaAgendaSupportUtils.firstNonBlank(row.statusLabel(), resolveStatusLabel(statusOperacional));
        String corStatus = OficialJusticaAgendaSupportUtils.firstNonBlank(row.corStatus(), resolveStatusColor(statusOperacional, row.corAndamento()));
        int attempts = Math.max(row.tentativasRealizadas(), digest != null ? digest.attempts() : 0);
        if (digest != null && digest.frustrationCode() != null) {
            alerts.add(digest.frustrationCode());
        }
        if (stop == null) {
            alerts.add("Sem posicionamento fechado na primeira roteirização; manter item sob replanejamento dinâmico.");
        }
        if ("JUSTICA_FEDERAL".equals(esfera)) {
            alerts.add("Agenda com paridade funcional para oficial federal habilitada na mesma espinha operacional.");
        }
        if (bairro != null && !bairro.isBlank()) {
            alerts.add("AGRUPAMENTO_BAIRRO_ATIVO");
        }
        return new OficialJusticaAgendaOperacionalResponse.StopRow(
                stop != null ? stop.ordem() : Integer.MAX_VALUE,
                row.workItemId(),
                row.processoId(),
                row.processoNumero(),
                row.rito(),
                row.vara(),
                row.lotacao(),
                row.tribunal(),
                esfera,
                row.processoStatus(),
                row.pasta(),
                row.prioridadeOperacional(),
                statusOperacional,
                statusLabel,
                row.prazoFatalEm(),
                stop != null ? stop.chegadaEstimada() : null,
                row.janelaRetornoRecomendadaEm(),
                stop != null ? stop.classificacao() : "REPLANEJAR",
                stop != null ? stop.distanciaTrechoKm() : 0d,
                stop != null ? stop.deslocamentoMinutos() : 0L,
                endereco,
                bairro,
                microterritorio,
                null,
                row.alvoPrincipal(),
                row.resumoProcessual(),
                row.fundamentoMissao(),
                row.calculadoraSugerida(),
                row.corAndamento(),
                corStatus,
                attempts,
                digest != null ? digest.latestAttemptAt() : row.ultimoMovimentoEm(),
                digest != null ? digest.frustrationCode() : null,
                digest != null ? digest.frustrationLabel() : null,
                shouldReplan(row, stop, digest),
                resolveReplanReason(row, stop, digest),
                resolveReturnLabel(row, digest),
                row.podeEnviarNoProcesso(),
                row.bloqueioEnvio(),
                enrichQuickActions(usuario, row),
                List.copyOf(OficialJusticaAgendaSupportUtils.nonBlank(alerts).stream().distinct().toList())
        );
    }

    String composeTerritorio(Usuario usuario) {
        if (usuario == null) {
            return "XX:SEM_COMARCA";
        }
        return OficialJusticaAgendaSupportUtils.firstNonBlank(usuario.getUf(), "XX") + ':' + OficialJusticaAgendaSupportUtils.firstNonBlank(usuario.getComarca(), "SEM_COMARCA");
    }

    String composeAddress(OficialJusticaDiligenciaQueueResponse.Row row) {
        return OficialJusticaAgendaSupportUtils.firstNonBlank(row.alvoPrincipal(), "Alvo não informado") + ", "
                + OficialJusticaAgendaSupportUtils.firstNonBlank(row.comarca(), "SEM_COMARCA") + ", "
                + OficialJusticaAgendaSupportUtils.firstNonBlank(row.vara(), "VARA_NAO_IDENTIFICADA");
    }

    private String resolveMicroterritorio(OficialJusticaDiligenciaQueueResponse.Row row,
                                          OficialJusticaAgendaTerritorialHint hint,
                                          String address) {
        if (hint != null && hint.microterritorio() != null && !hint.microterritorio().isBlank()) {
            return hint.microterritorio();
        }
        String base = OficialJusticaAgendaSupportUtils.firstNonBlank(resolveBairro(hint), row.comarca(), row.vara(), "MICRO");
        String tail = address == null ? "BASE" : address.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        if (tail.length() > 18) {
            tail = tail.substring(0, 18);
        }
        return base + ':' + tail;
    }

    private String resolveAddressReference(OficialJusticaDiligenciaQueueResponse.Row row,
                                           OficialJusticaAgendaTerritorialHint hint) {
        return OficialJusticaAgendaSupportUtils.firstNonBlank(hint != null ? hint.address() : null, composeAddress(row));
    }

    private String resolveBairro(OficialJusticaAgendaTerritorialHint hint) {
        return hint == null ? null : OficialJusticaAgendaSupportUtils.firstNonBlank(hint.bairro(), hint.cityUf());
    }

    private int prioridadeNumerica(String prioridade) {
        String normalized = prioridade == null ? "" : prioridade.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CRITICA" -> 1;
            case "ALTA" -> 2;
            case "MEDIA" -> 3;
            case "BAIXA" -> 4;
            default -> 3;
        };
    }

    private String resolveStatusOperacional(OficialJusticaDiligenciaQueueResponse.Row row) {
        if (row == null) {
            return "PENDENTE";
        }
        if (row.statusOperacional() != null && !row.statusOperacional().isBlank()) {
            return row.statusOperacional();
        }
        if ("CONCLUIDAS".equals(row.pasta())) {
            return "CONCLUIDA";
        }
        if ("ATRASADAS".equals(row.pasta())) {
            return "ATRASADA";
        }
        if ("AGUARDANDO_RETORNO".equals(row.pasta())) {
            return "AGUARDANDO_RETORNO";
        }
        if ("AGUARDANDO_CARTORIO".equals(row.pasta())) {
            return "AGUARDANDO_CARTORIO";
        }
        if (!row.podeEnviarNoProcesso()) {
            return "BLOQUEADA";
        }
        return "PENDENTE";
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

    private boolean shouldReplan(OficialJusticaDiligenciaQueueResponse.Row row,
                                 DiligenceRouteOptimizationResponse.OptimizedStop stop,
                                 OficialJusticaAgendaLiveEventDigest digest) {
        if (row == null) {
            return false;
        }
        return stop == null
                || "AGUARDANDO_RETORNO".equals(row.statusOperacional())
                || "ATRASADA".equals(row.statusOperacional())
                || row.alertas().contains("CHEGADA_FORA_GEOFENCE")
                || row.alertas().contains("RETORNO_RECOMENDADO")
                || (digest != null && digest.requiresReorder());
    }

    private String resolveReplanReason(OficialJusticaDiligenciaQueueResponse.Row row,
                                       DiligenceRouteOptimizationResponse.OptimizedStop stop,
                                       OficialJusticaAgendaLiveEventDigest digest) {
        if (row == null) {
            return null;
        }
        if (digest != null && digest.frustrationLabel() != null) {
            return digest.frustrationLabel();
        }
        if (row.alertas().contains("CHEGADA_FORA_GEOFENCE")) {
            return "Chegada fora da geofence; revisar endereço e reposicionar a rota do dia.";
        }
        if ("AGUARDANDO_RETORNO".equals(row.statusOperacional()) || row.alertas().contains("RETORNO_RECOMENDADO")) {
            return "Tentativa frustrada com janela de retorno aberta para nova passagem.";
        }
        if ("ATRASADA".equals(row.statusOperacional())) {
            return "Prazo fatal expirado; diligência deve subir na rota imediatamente.";
        }
        if (stop == null) {
            return "Item ainda não estabilizado na primeira roteirização; manter sob replanejamento vivo.";
        }
        return "Rota estável no bloco territorial atual.";
    }

    private String resolveReturnLabel(OficialJusticaDiligenciaQueueResponse.Row row,
                                      OficialJusticaAgendaLiveEventDigest digest) {
        String formatted = formatReturnWindow(row == null ? null : row.janelaRetornoRecomendadaEm());
        return OficialJusticaAgendaSupportUtils.firstNonBlank(formatted, digest != null ? digest.returnStrategy() : null);
    }

    private String formatReturnWindow(Instant value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneOffset.UTC).toLocalDateTime().toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> enrichQuickActions(Usuario usuario, OficialJusticaDiligenciaQueueResponse.Row row) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (row != null && row.quickActions() != null) {
            out.putAll(row.quickActions());
        }
        Processo processo = null;
        if (row != null && row.processoId() != null) {
            processo = new Processo();
            processo.setNumeroProcesso(row.processoNumero());
            processo.setTribunal(row.tribunal());
            processo.setVara(row.vara());
            processo.setComarca(row.comarca());
            processo.setClasseProcessual(row.categoria());
        }
        WorkItem item = row == null ? null : WorkItem.builder()
                .id(row.workItemId())
                .processo(processo)
                .titulo(row.alvoPrincipal())
                .descricao(row.resumoProcessual())
                .baseLegal(row.fundamentoMissao())
                .build();
        out.put("formalModel", communicationFormalModelService.buildProfile(processo, item, usuario));
        out.put("manualActions", ((Map<String, Object>) out.get("formalModel")).get("manualActions"));
        out.put("automaticActions", ((Map<String, Object>) out.get("formalModel")).get("automaticActions"));
        out.put("officialLane", stringFromMap((Map<String, Object>) out.get("formalModel"), "justicaAxis"));
        return Collections.unmodifiableMap(out);
    }

    @SuppressWarnings("unchecked")
    private String stringFromQuickAction(Map<String, Object> quickActions, String path) {
        if (quickActions == null || path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.split("\\.");
        Object current = quickActions;
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
        }
        return current == null ? null : String.valueOf(current);
    }

    private String stringFromMap(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return null;
        }
        Object raw = source.get(key);
        return raw == null ? null : String.valueOf(raw);
    }

    private String resolveEsferaAgenda(Usuario usuario, List<OficialJusticaAgendaOperacionalResponse.StopRow> rows) {
        if (rows.stream().anyMatch(row -> "JUSTICA_FEDERAL".equals(row.esfera()))) {
            return "JUSTICA_FEDERAL";
        }
        if (usuario != null && usuario.atuaNaUniao()) {
            return "JUSTICA_FEDERAL";
        }
        if (rows.stream().anyMatch(row -> "JUSTICA_DO_TRABALHO".equals(row.esfera()))) {
            return "JUSTICA_DO_TRABALHO";
        }
        if (rows.stream().anyMatch(row -> "JUSTICA_ELEITORAL".equals(row.esfera()))) {
            return "JUSTICA_ELEITORAL";
        }
        return "JUSTICA_ESTADUAL";
    }

    private String coberturaLabel(Usuario usuario, List<OficialJusticaAgendaOperacionalResponse.StopRow> rows) {
        if (usuario != null && usuario.atuaNaUniao()) {
            return "COBERTURA FEDERAL / SUBSEÇÃO / SEÇÃO / TRIBUNAL";
        }
        if (rows.stream().anyMatch(row -> "JUSTICA_FEDERAL".equals(row.esfera()))) {
            return "COBERTURA FEDERAL MISTA COM PARIDADE FUNCIONAL";
        }
        return "COBERTURA ESTADUAL / COMARCA / VARA";
    }

    private String tribunalPrincipal(List<OficialJusticaAgendaOperacionalResponse.StopRow> rows) {
        return rows.stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::tribunal).filter(Objects::nonNull).findFirst().orElse("TRIBUNAL_NAO_IDENTIFICADO");
    }

    private String resolveEsfera(Usuario usuario, String tribunal, String vara) {
        String normalizedTribunal = tribunal == null ? "" : tribunal.trim().toUpperCase(Locale.ROOT);
        String normalizedVara = vara == null ? "" : vara.trim().toUpperCase(Locale.ROOT);
        if (normalizedTribunal.startsWith("TRF") || normalizedTribunal.contains("FEDERAL") || normalizedVara.contains("FEDERAL") || usuario != null && usuario.atuaNaUniao()) {
            return "JUSTICA_FEDERAL";
        }
        if (normalizedTribunal.startsWith("TRT") || normalizedTribunal.contains("TRABALHO")) {
            return "JUSTICA_DO_TRABALHO";
        }
        if (normalizedTribunal.startsWith("TRE") || normalizedTribunal.contains("ELEITORAL")) {
            return "JUSTICA_ELEITORAL";
        }
        if (normalizedTribunal.startsWith("TJM") || normalizedTribunal.contains("MILITAR")) {
            return "JUSTICA_MILITAR";
        }
        return "JUSTICA_ESTADUAL";
    }
}
