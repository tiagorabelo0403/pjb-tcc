package com.tcc.pjb.backend.service.secretariat.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import com.tcc.pjb.backend.service.secretariat.triage.SecretariatWorkloadProfile;
import com.tcc.pjb.backend.service.secretariat.triage.TriageRoutingProfile;

@Service
public class SecretariatQueueProjectionService {

  private final SecretariatQueueItemRepository repo;
  private final ObjectMapper mapper;

  public SecretariatQueueProjectionService(SecretariatQueueItemRepository repo, ObjectMapper mapper) {
    this.repo = Objects.requireNonNull(repo);
    this.mapper = Objects.requireNonNull(mapper);
  }

  public SecretariatQueueItem upsert(WorkItem w, int score, List<String> tags) {
    return upsert(w, score, tags, null, null, null);
  }

  public SecretariatQueueItem upsert(WorkItem w,
                                     int score,
                                     List<String> tags,
                                     java.util.Map<String, Object> extraMetadata) {
    return upsert(w, score, tags, null, null, extraMetadata);
  }

  public SecretariatQueueItem upsert(WorkItem w,
                                     int score,
                                     List<String> tags,
                                     TriageRoutingProfile triageProfile,
                                     SecretariatWorkloadProfile workloadProfile) {
    return upsert(w, score, tags, triageProfile, workloadProfile, null);
  }

  public SecretariatQueueItem upsert(WorkItem w,
                                     int score,
                                     List<String> tags,
                                     TriageRoutingProfile triageProfile,
                                     SecretariatWorkloadProfile workloadProfile,
                                     java.util.Map<String, Object> extraMetadata) {
    Objects.requireNonNull(w);
    Long wid = w.getId();
    Long pid = w.getProcesso() != null ? w.getProcesso().getId() : null;
    if (wid == null || pid == null) {
      throw new IllegalStateException("workItem inválido");
    }

    SecretariatQueueItem q = repo.findById(wid).orElse(null);
    Instant now = Instant.now();
    String tagsJson = writeJson(tags);
    LinkedHashMap<String, Object> resolvedMetadata = new LinkedHashMap<>(q == null ? java.util.Map.of() : parseMetadata(q.getMetadataJson()));
    resolvedMetadata.putAll(deriveWorkItemMetadata(w));
    if (extraMetadata != null) {
      extraMetadata.forEach((key, value) -> {
        if (key != null && !key.isBlank() && value != null) {
          resolvedMetadata.put(key, value);
        }
      });
    }
    String metadataJson = buildMetadataJson(triageProfile, workloadProfile, resolvedMetadata);
    String laneCode = triageProfile != null && triageProfile.metadata() != null ? stringValue(triageProfile.metadata().get("resolvedLane")) : null;
    String deskAxis = triageProfile != null ? triageProfile.deskAxis() : null;
    String workloadBand = workloadProfile != null ? workloadProfile.workloadBand() : null;
    String routingFingerprint = joinFingerprint(triageProfile != null ? triageProfile.fingerprint() : null,
        workloadProfile != null ? workloadProfile.fingerprint() : null);
    boolean escalationRequired = triageProfile != null && (triageProfile.escalationRequired() || workloadProfile != null && workloadProfile.fastTrackDesk());
    boolean secrecyReviewRequired = triageProfile != null && triageProfile.secrecyReviewRequired();
    boolean hearingSensitive = triageProfile != null && triageProfile.hearingSensitive();
    boolean blocking = triageProfile != null ? triageProfile.blocking() : w.isBlocking();

    if (q == null) {
      q = SecretariatQueueItem.builder()
          .workItemId(wid)
          .processoId(pid)
          .inboxKey(nz(w.getInboxKey()))
          .queueCode(w.getQueueCode())
          .laneCode(laneCode)
          .deskAxis(deskAxis)
          .workloadBand(workloadBand)
          .routingFingerprint(routingFingerprint)
          .status(w.getStatus() != null ? w.getStatus().name() : "")
          .prioridade(w.getPrioridade() != null ? w.getPrioridade() : 3)
          .dueAt(w.getDueAt())
          .score(score)
          .tagsJson(tagsJson)
          .metadataJson(metadataJson)
          .titulo(nz(w.getTitulo()))
          .escalationRequired(escalationRequired)
          .secrecyReviewRequired(secrecyReviewRequired)
          .hearingSensitive(hearingSensitive)
          .blocking(blocking)
          .createdAt(now)
          .updatedAt(now)
          .build();
      return repo.save(q);
    }

    q.setProcessoId(pid);
    q.setInboxKey(nz(w.getInboxKey()));
    q.setQueueCode(w.getQueueCode());
    q.setLaneCode(laneCode);
    q.setDeskAxis(deskAxis);
    q.setWorkloadBand(workloadBand);
    q.setRoutingFingerprint(routingFingerprint);
    q.setStatus(w.getStatus() != null ? w.getStatus().name() : "");
    q.setPrioridade(w.getPrioridade() != null ? w.getPrioridade() : 3);
    q.setDueAt(w.getDueAt());
    q.setScore(score);
    q.setTagsJson(tagsJson);
    q.setMetadataJson(metadataJson);
    q.setTitulo(nz(w.getTitulo()));
    q.setEscalationRequired(escalationRequired);
    q.setSecrecyReviewRequired(secrecyReviewRequired);
    q.setHearingSensitive(hearingSensitive);
    q.setBlocking(blocking);
    q.setUpdatedAt(now);
    return repo.save(q);
  }

  private LinkedHashMap<String, Object> deriveWorkItemMetadata(WorkItem w) {
    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
    if (w == null) {
      return metadata;
    }
    putIfPresent(metadata, "workItemId", w.getId());
    putIfPresent(metadata, "inboxKey", nz(w.getInboxKey()));
    putIfPresent(metadata, "queueCode", w.getQueueCode());
    putIfPresent(metadata, "workItemType", w.getType() == null ? null : w.getType().name());
    putIfPresent(metadata, "status", w.getStatus() == null ? null : w.getStatus().name());
    putIfPresent(metadata, "prioridade", w.getPrioridade());
    putIfPresent(metadata, "assignedRole", w.getAssignedRole() == null ? null : w.getAssignedRole().name());
    if (w.getAssignedUser() != null) {
      putIfPresent(metadata, "assignedUserId", w.getAssignedUser().getId());
      putIfPresent(metadata, "assignedUserName", firstNonBlank(w.getAssignedUser().getNome(), w.getAssignedUser().getNomeCompleto()));
      putIfPresent(metadata, "assignedUserEmail", w.getAssignedUser().getEmail());
      putIfPresent(metadata, "assignedUserOab", w.getAssignedUser().getOab());
      putIfPresent(metadata, "assignedUserPerfil", w.getAssignedUser().getPerfil());
      LinkedHashMap<String, Object> assignedUser = new LinkedHashMap<>();
      putIfPresent(assignedUser, "id", w.getAssignedUser().getId());
      putIfPresent(assignedUser, "nome", firstNonBlank(w.getAssignedUser().getNome(), w.getAssignedUser().getNomeCompleto()));
      putIfPresent(assignedUser, "email", w.getAssignedUser().getEmail());
      putIfPresent(assignedUser, "perfil", w.getAssignedUser().getPerfil());
      putIfPresent(assignedUser, "tipoUsuario", w.getAssignedUser().getTipoUsuario() == null ? null : w.getAssignedUser().getTipoUsuario().name());
      if (!assignedUser.isEmpty()) {
        metadata.put("assignedUser", Map.copyOf(assignedUser));
      }
    }
    putIfPresent(metadata, "panelReferenceAt", referenceAt(w));
    putIfPresent(metadata, "panelDateBucket", dateBucket(referenceAt(w)));
    if (w.getProcesso() == null) {
      return metadata;
    }
    putIfPresent(metadata, "processoId", w.getProcesso().getId());
    putIfPresent(metadata, "processoNumero", nz(firstNonBlank(w.getProcesso().getNumeroProcesso(), w.getProcesso().getNumeroUnificado(), w.getProcesso().getNumero())));
    putIfPresent(metadata, "ritoProcessual", w.getProcesso().getRito() == null ? null : w.getProcesso().getRito().name());
    putIfPresent(metadata, "ramoDireito", w.getProcesso().getRamoDireito() == null ? null : w.getProcesso().getRamoDireito().name());
    putIfPresent(metadata, "classeProcessual", firstNonBlank(w.getProcesso().getClasseProcessual(), w.getProcesso().getClasseTpuCodigo()));
    putIfPresent(metadata, "classeTpuCodigo", w.getProcesso().getClasseTpuCodigo());
    putIfPresent(metadata, "vara", w.getProcesso().getVara());
    putIfPresent(metadata, "comarca", firstNonBlank(w.getProcesso().getComarca(), w.getComarca()));
    putIfPresent(metadata, "uf", firstNonBlank(w.getProcesso().getUf(), w.getUf()));
    putIfPresent(metadata, "tribunalCodigo", w.getProcesso().getTribunalCodigoRoteado());
    putIfPresent(metadata, "tribunalNome", w.getProcesso().getTribunal());
    putIfPresent(metadata, "unidadeJudiciariaCodigo", w.getProcesso().getUnidadeJudiciariaCodigo());
    putIfPresent(metadata, "tipoJustica", w.getProcesso().getTipoJustica() == null ? null : w.getProcesso().getTipoJustica().name());
    putIfPresent(metadata, "faseProcessual", w.getProcesso().getFaseAtual() == null ? null : w.getProcesso().getFaseAtual().name());
    return metadata;
  }

  private Instant referenceAt(WorkItem w) {
    if (w == null) {
      return null;
    }
    if (w.getDueAt() != null) {
      return w.getDueAt();
    }
    if (w.getUpdatedAt() != null) {
      return w.getUpdatedAt();
    }
    return w.getCreatedAt();
  }

  private String dateBucket(Instant instant) {
    if (instant == null) {
      return null;
    }
    return LocalDate.ofInstant(instant, ZoneOffset.UTC).toString();
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private LinkedHashMap<String, Object> parseMetadata(String json) {
    if (json == null || json.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      return mapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
    } catch (Exception e) {
      return new LinkedHashMap<>();
    }
  }

  private String buildMetadataJson(TriageRoutingProfile triageProfile,
                                   SecretariatWorkloadProfile workloadProfile,
                                   java.util.Map<String, Object> extraMetadata) {
    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(baseMetadata(triageProfile == null ? null : triageProfile.metadata(), workloadProfile == null ? null : workloadProfile.toMap()));
    if (triageProfile != null) {
      metadata.put("triage", triageProfile.toMap());
      metadata.put("triageFingerprint", triageProfile.fingerprint());
    }
    if (workloadProfile != null) {
      metadata.put("workload", workloadProfile.toMap());
      metadata.put("workloadFingerprint", workloadProfile.fingerprint());
    }
    if (extraMetadata != null) {
      extraMetadata.forEach((key, value) -> {
        if (key != null && !key.isBlank() && value != null) {
          metadata.put(key, value);
        }
      });
    }
    metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
    return writeJson(metadata);
  }

  private LinkedHashMap<String, Object> baseMetadata(java.util.Map<String, Object> triageMetadata,
                                                     java.util.Map<String, Object> workloadMetadata) {
    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
    putIfPresent(metadata, "ritoProcessual", valueOf(triageMetadata == null ? null : triageMetadata.get("rito")));
    putIfPresent(metadata, "territorialAnchor", valueOf(triageMetadata == null ? null : triageMetadata.get("territorialAnchor")));
    putIfPresent(metadata, "workloadBandDerived", valueOf(workloadMetadata == null ? null : workloadMetadata.get("workloadBand")));
    return metadata;
  }

  private static void putIfPresent(LinkedHashMap<String, Object> metadata, String key, Object value) {
    if (key != null && !key.isBlank() && value != null) {
      metadata.put(key, value);
    }
  }

  private static String valueOf(Object raw) {
    if (raw == null) {
      return null;
    }
    String value = String.valueOf(raw).trim();
    return value.isBlank() ? null : value;
  }

  private String writeJson(Object v) {
    try {
      return mapper.writeValueAsString(v);
    } catch (Exception e) {
      return "{}";
    }
  }

  private static String joinFingerprint(String first, String second) {
    if ((first == null || first.isBlank()) && (second == null || second.isBlank())) {
      return null;
    }
    if (first == null || first.isBlank()) {
      return second;
    }
    if (second == null || second.isBlank()) {
      return first;
    }
    return first + "::" + second;
  }

  private static String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private static String nz(String v) {
    return v == null ? "" : v;
  }
}
