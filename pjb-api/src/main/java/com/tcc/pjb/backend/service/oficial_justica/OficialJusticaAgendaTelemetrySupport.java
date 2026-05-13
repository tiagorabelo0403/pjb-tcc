package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaDiligenciaQueueResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaAgendaSupportUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
class OficialJusticaAgendaTelemetrySupport {

    private final OficialJusticaEnderecoTriageService enderecoTriageService;
    private final DiligenciaOperadorCheckpointEventoRepository checkpointRepository;
    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository;
    private final OficialJusticaAgendaAssemblySupport assemblySupport;

    OficialJusticaAgendaTelemetrySupport(OficialJusticaEnderecoTriageService enderecoTriageService,
                                        DiligenciaOperadorCheckpointEventoRepository checkpointRepository,
                                        DiligenciaOperadorEncerramentoRepository encerramentoRepository,
                                        OficialJusticaAgendaAssemblySupport assemblySupport) {
        this.enderecoTriageService = Objects.requireNonNull(enderecoTriageService);
        this.checkpointRepository = Objects.requireNonNull(checkpointRepository);
        this.encerramentoRepository = Objects.requireNonNull(encerramentoRepository);
        this.assemblySupport = Objects.requireNonNull(assemblySupport);
    }

    Map<Long, OficialJusticaAgendaTerritorialHint> buildTerritorialHints(List<OficialJusticaDiligenciaQueueResponse.Row> rows,
                                                                         int maxLookups) {
        LinkedHashMap<Long, OficialJusticaAgendaTerritorialHint> out = new LinkedHashMap<>();
        rows.stream()
                .filter(row -> row.workItemId() != null)
                .sorted(Comparator.comparingInt(this::territorialLookupPriority).reversed())
                .limit(Math.max(4, Math.min(maxLookups, 16)))
                .forEach(row -> out.put(row.workItemId(), fetchTerritorialHint(row)));
        rows.stream()
                .filter(row -> row.workItemId() != null)
                .forEach(row -> out.computeIfAbsent(row.workItemId(), ignored -> fallbackTerritorialHint(row)));
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    Map<Long, OficialJusticaAgendaLiveEventDigest> buildLiveDigests(Usuario usuario,
                                                                    List<OficialJusticaDiligenciaQueueResponse.Row> rows) {
        if (usuario == null || usuario.getId() == null) {
            return Map.of();
        }
        List<Long> workItemIds = rows.stream().map(OficialJusticaDiligenciaQueueResponse.Row::workItemId).filter(Objects::nonNull).distinct().toList();
        if (workItemIds.isEmpty()) {
            return Map.of();
        }
        List<DiligenciaOperadorCheckpointEvento> checkpoints = checkpointRepository.findByOperatorUserIdAndCanalAndWorkItemIdInOrderByOccurredAtDesc(
                usuario.getId(),
                TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                workItemIds
        );
        List<DiligenciaOperadorEncerramento> encerramentos = encerramentoRepository.findByOperatorUserIdAndCanalAndWorkItemIdInOrderByCreatedAtDesc(
                usuario.getId(),
                TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                workItemIds
        );
        LinkedHashMap<Long, DiligenciaOperadorCheckpointEvento> latestCheckpoint = new LinkedHashMap<>();
        LinkedHashMap<Long, Long> checkpointCounts = new LinkedHashMap<>();
        for (DiligenciaOperadorCheckpointEvento checkpoint : checkpoints) {
            if (checkpoint.getWorkItemId() == null) {
                continue;
            }
            latestCheckpoint.putIfAbsent(checkpoint.getWorkItemId(), checkpoint);
            checkpointCounts.merge(checkpoint.getWorkItemId(), 1L, Long::sum);
        }
        LinkedHashMap<Long, DiligenciaOperadorEncerramento> latestClosure = new LinkedHashMap<>();
        for (DiligenciaOperadorEncerramento encerramento : encerramentos) {
            if (encerramento.getWorkItemId() == null) {
                continue;
            }
            latestClosure.putIfAbsent(encerramento.getWorkItemId(), encerramento);
        }
        LinkedHashMap<Long, OficialJusticaAgendaLiveEventDigest> out = new LinkedHashMap<>();
        for (OficialJusticaDiligenciaQueueResponse.Row row : rows) {
            if (row.workItemId() == null) {
                continue;
            }
            DiligenciaOperadorCheckpointEvento checkpoint = latestCheckpoint.get(row.workItemId());
            DiligenciaOperadorEncerramento encerramento = latestClosure.get(row.workItemId());
            int attempts = Math.max(row.tentativasRealizadas(), checkpointCounts.getOrDefault(row.workItemId(), 0L).intValue());
            String frustrationCode = resolveFrustrationCode(row, checkpoint, encerramento, attempts);
            String frustrationLabel = frustrationLabel(frustrationCode);
            String returnStrategy = resolveReturnStrategy(row, frustrationCode, checkpoint, encerramento);
            boolean requiresReorder = frustrationCode != null || "EM_DILIGENCIA".equals(row.statusOperacional()) || (checkpoint != null && !checkpoint.isInsideGeofence());
            out.put(row.workItemId(), new OficialJusticaAgendaLiveEventDigest(
                    attempts,
                    checkpoint != null ? checkpoint.getOccurredAt() : row.ultimoMovimentoEm(),
                    frustrationCode,
                    frustrationLabel,
                    returnStrategy,
                    requiresReorder
            ));
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private OficialJusticaAgendaTerritorialHint fetchTerritorialHint(OficialJusticaDiligenciaQueueResponse.Row row) {
        if (row == null || row.workItemId() == null) {
            return fallbackTerritorialHint(row);
        }
        try {
            var rastreio = enderecoTriageService.rastrearMandado(String.valueOf(row.workItemId()), false, true, false);
            PessoaLocalizacaoResponse.EnderecoCandidato best = selectBestAddress(rastreio.localizacao());
            if (best == null) {
                return fallbackTerritorialHint(row);
            }
            String cityUf = joinCidadeUf(best.cidade(), best.uf());
            String microterritorio = composeMicroterritorioBase(row, best.bairro(), cityUf, best.descricao());
            return new OficialJusticaAgendaTerritorialHint(
                    OficialJusticaAgendaSupportUtils.firstNonBlank(best.descricao(), assemblySupport.composeAddress(row)),
                    best.bairro(),
                    cityUf,
                    microterritorio,
                    best.fonte(),
                    best.confianca()
            );
        } catch (Exception ex) {
            return fallbackTerritorialHint(row);
        }
    }

    private OficialJusticaAgendaTerritorialHint fallbackTerritorialHint(OficialJusticaDiligenciaQueueResponse.Row row) {
        String address = assemblySupport.composeAddress(row);
        String cityUf = OficialJusticaAgendaSupportUtils.firstNonBlank(row.comarca(), row.vara(), "LOCALIDADE_NAO_IDENTIFICADA");
        return new OficialJusticaAgendaTerritorialHint(address, null, cityUf, composeMicroterritorioBase(row, null, cityUf, address), null, null);
    }

    private PessoaLocalizacaoResponse.EnderecoCandidato selectBestAddress(PessoaLocalizacaoResponse localizacao) {
        if (localizacao == null || localizacao.enderecos() == null || localizacao.enderecos().isEmpty()) {
            return null;
        }
        return localizacao.enderecos().stream()
                .sorted(Comparator.comparing(PessoaLocalizacaoResponse.EnderecoCandidato::principal).reversed()
                        .thenComparing(PessoaLocalizacaoResponse.EnderecoCandidato::confianca, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PessoaLocalizacaoResponse.EnderecoCandidato::atualizadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }

    private String composeMicroterritorioBase(OficialJusticaDiligenciaQueueResponse.Row row,
                                              String bairro,
                                              String cityUf,
                                              String address) {
        String base = OficialJusticaAgendaSupportUtils.firstNonBlank(bairro, row != null ? row.comarca() : null, row != null ? row.vara() : null, "MICRO");
        String tail = OficialJusticaAgendaSupportUtils.firstNonBlank(cityUf, address, "BASE").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        if (tail.length() > 18) {
            tail = tail.substring(0, 18);
        }
        return base + ':' + tail;
    }

    private String joinCidadeUf(String cidade, String uf) {
        if (cidade == null && uf == null) {
            return null;
        }
        if (cidade == null || cidade.isBlank()) {
            return uf;
        }
        if (uf == null || uf.isBlank()) {
            return cidade;
        }
        return cidade + " / " + uf;
    }

    private int territorialLookupPriority(OficialJusticaDiligenciaQueueResponse.Row row) {
        int score = 0;
        if (row == null) {
            return score;
        }
        if ("ATRASADA".equals(row.statusOperacional())) {
            score += 10;
        }
        if ("AGUARDANDO_RETORNO".equals(row.statusOperacional())) {
            score += 9;
        }
        if ("EM_DILIGENCIA".equals(row.statusOperacional())) {
            score += 8;
        }
        if (row.prazoFatalEm() != null && row.prazoFatalEm().isBefore(Instant.now().plus(6, ChronoUnit.HOURS))) {
            score += 6;
        }
        score += Math.min(5, row.tentativasRealizadas());
        return score;
    }

    private String resolveFrustrationCode(OficialJusticaDiligenciaQueueResponse.Row row,
                                          DiligenciaOperadorCheckpointEvento checkpoint,
                                          DiligenciaOperadorEncerramento encerramento,
                                          int attempts) {
        if (encerramento != null && encerramento.getOutcome() == DiligenciaEncerramentoTipo.CUMPRIMENTO_FRUSTRADO) {
            if (checkpoint != null && !checkpoint.isInsideGeofence()) {
                return "ENDERECO_DIVERGENTE_GEOFENCE";
            }
            if (attempts >= 2) {
                return "REINCIDENCIA_TENTATIVA_FRUSTRADA";
            }
            if (row != null && row.prazoFatalEm() != null && row.prazoFatalEm().isBefore(Instant.now().plus(8, ChronoUnit.HOURS))) {
                return "PRAZO_EM_RISCO_APOS_FRUSTRACAO";
            }
            return "AUSENCIA_DESTINATARIO_CONFIRMADA";
        }
        if (encerramento != null && encerramento.getOutcome() == DiligenciaEncerramentoTipo.DILIGENCIA_PARCIAL) {
            return "RETORNO_COMPLEMENTAR_OBRIGATORIO";
        }
        if (checkpoint != null && !checkpoint.isInsideGeofence()) {
            return "LOCALIDADE_INCERTA_OU_FORA_DA_GEOFENCE";
        }
        return null;
    }

    private String frustrationLabel(String code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case "ENDERECO_DIVERGENTE_GEOFENCE" -> "Endereço divergente em relação à geofence; conferir localidade antes do próximo deslocamento.";
            case "REINCIDENCIA_TENTATIVA_FRUSTRADA" -> "Houve reincidência de tentativa frustrada; escalar prioridade e ajustar a janela de retorno.";
            case "PRAZO_EM_RISCO_APOS_FRUSTRACAO" -> "A diligência foi frustrada com prazo já em risco; subir imediatamente na rota do dia.";
            case "AUSENCIA_DESTINATARIO_CONFIRMADA" -> "Não houve confirmação útil do destinatário no local na última tentativa.";
            case "RETORNO_COMPLEMENTAR_OBRIGATORIO" -> "A diligência ficou parcial e exige retorno complementar com continuidade da missão.";
            case "LOCALIDADE_INCERTA_OU_FORA_DA_GEOFENCE" -> "A chegada foi registrada fora da geofence esperada; revisar endereço e lote territorial.";
            default -> code;
        };
    }

    private String resolveReturnStrategy(OficialJusticaDiligenciaQueueResponse.Row row,
                                         String frustrationCode,
                                         DiligenciaOperadorCheckpointEvento checkpoint,
                                         DiligenciaOperadorEncerramento encerramento) {
        if (frustrationCode == null) {
            return null;
        }
        if ("ENDERECO_DIVERGENTE_GEOFENCE".equals(frustrationCode) || "LOCALIDADE_INCERTA_OU_FORA_DA_GEOFENCE".equals(frustrationCode)) {
            return "VALIDAR_ENDERECO_E_REENTRAR_NO_LOTE_TERRITORIAL";
        }
        if ("REINCIDENCIA_TENTATIVA_FRUSTRADA".equals(frustrationCode)) {
            return "RETORNO_COM_ESCALONAMENTO_DA_PRIORIDADE";
        }
        if ("RETORNO_COMPLEMENTAR_OBRIGATORIO".equals(frustrationCode)) {
            return "RETORNAR_AINDA_HOJE_COM_CONTINUIDADE_DA_DILIGENCIA";
        }
        if (row != null && row.janelaRetornoRecomendadaEm() != null) {
            return "RETORNAR_NA_JANELA_OPERACIONAL_RECOMENDADA";
        }
        if (checkpoint != null || encerramento != null) {
            return "RETORNAR_COM_REORDENACAO_DA_ROTA_DO_DIA";
        }
        return "REAVALIAR_LOTE_TERRITORIAL";
    }
}
