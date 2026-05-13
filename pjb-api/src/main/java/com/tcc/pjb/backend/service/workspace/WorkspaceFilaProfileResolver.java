package com.tcc.pjb.backend.service.workspace;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.workspace.fila.WorkspaceFilaWorkItemCriteria;
import com.tcc.pjb.backend.model.dto.workspace.fila.WorkspaceFilaWorkItemMode;
import com.tcc.pjb.backend.model.dto.workspace.localizador.WorkspaceLocalizadorCriteria;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceFila;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceFilaAudience;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceFilaKind;

@Component
public class WorkspaceFilaProfileResolver {

  private final ObjectMapper mapper;

  public WorkspaceFilaProfileResolver(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  public WorkspaceFilaProfile resolve(WorkspaceFila fila, long count) {
    Objects.requireNonNull(fila, "fila");
    WorkspaceFilaKind kind = fila.getKind();
    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
    LinkedHashSet<String> labels = new LinkedHashSet<>();
    labels.add(kind != null ? kind.name() : "UNKNOWN");
    labels.add(fila.isSistema() ? "SYSTEM" : "USER");
    if (fila.isCompartilhado()) {
      labels.add("SHARED");
    }
    WorkspaceFilaAudience audience = fila.getAudience() != null ? fila.getAudience() : WorkspaceFilaAudience.ALL;
    labels.add(audience.name());
    metadata.put("audience", audience.name());
    metadata.put("count", count);
    metadata.put("shared", fila.isCompartilhado());
    metadata.put("ownerUserId", fila.getOwnerUserId());

    String scope = kind != null ? kind.name() : "UNKNOWN";
    String operationalMode = "STANDARD";
    String sortHint = kind == WorkspaceFilaKind.WORKITEM ? "dueAt,prioridade,createdAt" : "numeroUnificado";
    int autoRefreshSeconds = kind == WorkspaceFilaKind.WORKITEM ? 15 : 30;
    String assistantDesk = kind == WorkspaceFilaKind.WORKITEM ? "ASSIST_WORKITEM_BASE" : "ASSIST_PROCESSO_BASE";
    String escalationDesk = kind == WorkspaceFilaKind.WORKITEM ? "ESC_WORKITEM_BASE" : "ESC_PROCESSO_BASE";
    String coordinationChannel = kind == WorkspaceFilaKind.WORKITEM ? "WORKITEM/OPERACIONAL" : "PROCESSO/LOCALIZACAO";
    boolean redistributionEligible = fila.isCompartilhado() || fila.isSistema();
    boolean audienceSensitive = false;

    if (kind == WorkspaceFilaKind.WORKITEM) {
      WorkspaceFilaWorkItemCriteria criteria = parse(fila.getCriterioJson(), WorkspaceFilaWorkItemCriteria.class, WorkspaceFilaWorkItemCriteria.builder().mode(WorkspaceFilaWorkItemMode.AUTO_INBOX).build());
      WorkspaceFilaWorkItemMode mode = criteria.getMode() != null ? criteria.getMode() : WorkspaceFilaWorkItemMode.AUTO_INBOX;
      operationalMode = mode.name();
      metadata.put("mode", mode.name());
      metadata.put("blockingOnly", Boolean.TRUE.equals(criteria.getBlockingOnly()));
      metadata.put("includeOverdue", Boolean.TRUE.equals(criteria.getIncludeOverdue()));
      metadata.put("maxPrioridade", criteria.getMaxPrioridade());
      metadata.put("types", criteria.getTypes() == null ? List.of() : criteria.getTypes().stream().map(Enum::name).toList());
      metadata.put("status", criteria.getStatus() == null ? List.of() : criteria.getStatus().stream().map(Enum::name).toList());
      if (Boolean.TRUE.equals(criteria.getBlockingOnly())) {
        labels.add("BLOCKING");
        assistantDesk = "ASSIST_BLOCKING_CONTROL";
        escalationDesk = "ESC_BLOCKING_CONTROL";
        coordinationChannel = "WORKITEM/BLOCKING";
      }
      if (mode == WorkspaceFilaWorkItemMode.DUE_WITHIN_HOURS) {
        labels.add("SLA");
        autoRefreshSeconds = 10;
        metadata.put("hours", criteria.getHours());
        assistantDesk = "ASSIST_SLA_RECOVERY";
        escalationDesk = "ESC_SLA_RECOVERY";
        coordinationChannel = "WORKITEM/SLA";
        redistributionEligible = true;
      }
      if (criteria.getTypes() != null && !criteria.getTypes().isEmpty()) {
        labels.add("TYPE_FILTER");
      }
      scope = "WORKITEM:" + mode.name();
    } else if (kind == WorkspaceFilaKind.PROCESSO) {
      WorkspaceLocalizadorCriteria criteria = parse(fila.getCriterioJson(), WorkspaceLocalizadorCriteria.class, new WorkspaceLocalizadorCriteria());
      metadata.put("somenteMeus", Boolean.TRUE.equals(criteria.getSomenteMeus()));
      metadata.put("jurisdicaoId", criteria.getJurisdicaoId());
      metadata.put("status", criteria.getStatus() == null ? List.of() : criteria.getStatus().stream().map(Enum::name).toList());
      metadata.put("fases", criteria.getFases() == null ? List.of() : criteria.getFases().stream().map(Enum::name).toList());
      metadata.put("ritos", criteria.getRitos() == null ? List.of() : criteria.getRitos().stream().map(Enum::name).toList());
      metadata.put("etiquetas", criteria.getEtiquetaIds() == null ? List.of() : criteria.getEtiquetaIds());
      if (Boolean.TRUE.equals(criteria.getSomenteMeus())) {
        labels.add("OWNERSHIP");
        redistributionEligible = false;
      }
      if (criteria.getRitos() != null && !criteria.getRitos().isEmpty()) {
        labels.add("RITO_FILTER");
        assistantDesk = "ASSIST_RITO_ESPECIALIZADO";
      }
      if (criteria.getFases() != null && !criteria.getFases().isEmpty()) {
        labels.add("PHASE_FILTER");
        escalationDesk = "ESC_FASE_PROCESSUAL";
      }
      audienceSensitive = containsAudienceSignal(fila.getNome(), fila.getDescricao(), criteria);
      if (audienceSensitive) {
        labels.add("AUDIENCE_SENSITIVE");
        assistantDesk = "ASSIST_AUDIENCIA_OPERACIONAL";
        coordinationChannel = "PROCESSO/AUDIENCIA";
      } else if (criteria.getRitos() != null && !criteria.getRitos().isEmpty()) {
        coordinationChannel = "PROCESSO/RITO";
      } else {
        coordinationChannel = Boolean.TRUE.equals(criteria.getSomenteMeus()) ? "PROCESSO/OWNERSHIP" : "PROCESSO/SHARED";
      }
      scope = "PROCESSO:SEARCH";
      operationalMode = Boolean.TRUE.equals(criteria.getSomenteMeus()) ? "MY_CASES" : "SHARED_SEARCH";
      sortHint = audienceSensitive ? "faseProcessual,numeroUnificado" : "status,faseProcessual,numeroUnificado";
    }

    String workloadBand = resolveWorkloadBand(kind, count, metadata);
    String descriptor = buildDescriptor(fila, scope, operationalMode, workloadBand);
    metadata.put("redistributionEligible", redistributionEligible);
    metadata.put("audienceSensitive", audienceSensitive);
    metadata.put("assistantDesk", assistantDesk);
    metadata.put("escalationDesk", escalationDesk);
    metadata.put("coordinationChannel", coordinationChannel);
    return new WorkspaceFilaProfile(
        descriptor,
        operationalMode,
        scope,
        autoRefreshSeconds,
        sortHint,
        workloadBand,
        assistantDesk,
        escalationDesk,
        coordinationChannel,
        redistributionEligible,
        audienceSensitive,
        List.copyOf(labels),
        metadata
    );
  }

  private String resolveWorkloadBand(WorkspaceFilaKind kind, long count, LinkedHashMap<String, Object> metadata) {
    long thresholdHigh = kind == WorkspaceFilaKind.WORKITEM ? 80 : 300;
    long thresholdModerate = kind == WorkspaceFilaKind.WORKITEM ? 20 : 80;
    String band = count >= thresholdHigh ? "HIGH" : count >= thresholdModerate ? "MODERATE" : "NORMAL";
    metadata.put("thresholdModerate", thresholdModerate);
    metadata.put("thresholdHigh", thresholdHigh);
    return band;
  }

  private String buildDescriptor(WorkspaceFila fila, String scope, String operationalMode, String workloadBand) {
    String baseName = fila.getNome() == null || fila.getNome().isBlank() ? "FILA" : fila.getNome().trim().replaceAll("\\s+", "_").toUpperCase();
    return baseName + ':' + scope + ':' + operationalMode + ':' + workloadBand;
  }

  private boolean containsAudienceSignal(String nome, String descricao, WorkspaceLocalizadorCriteria criteria) {
    String base = ((nome == null ? "" : nome) + ' ' + (descricao == null ? "" : descricao)).toUpperCase();
    if (base.contains("AUDIENCIA") || base.contains("SESSAO") || base.contains("CONCILIACAO") || base.contains("MEDIACAO")) {
      return true;
    }
    if (criteria == null || criteria.getFases() == null || criteria.getFases().isEmpty()) {
      return false;
    }
    return criteria.getFases().stream().map(Enum::name).anyMatch(name -> name.contains("AUDI") || name.contains("INSTRUT") || name.contains("SESSAO"));
  }

  private <T> T parse(String json, Class<T> type, T fallback) {
    if (json == null || json.isBlank()) {
      return fallback;
    }
    try {
      return mapper.readValue(json, type);
    } catch (Exception ignored) {
      return fallback;
    }
  }
}
