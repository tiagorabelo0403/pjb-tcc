package com.tcc.pjb.backend.service.cidadao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventType;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.stepup.JwtStepUpClaims;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoJuntadaResumoDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.model.repository.ProcessEventRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.unified.eventsourcing.JudiciarioEventSourcingEngine;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CidadaoJuntadasService {

  private record JuntadaPayload(String label, UUID eventoId, List<JudiciarioEventSourcingEngine.DocumentoMetadata> documentos) {}

  private final ProcessoRepository processoRepo;
  private final ProcessEventRepository eventRepo;
  private final DocumentoProcessualRepository docRepo;
  private final PjbAuthorizationService authz;
  private final RecursalEffectiveSecrecyService secrecyService;
  private final ObjectMapper mapper;

  public CidadaoJuntadasService(ProcessoRepository processoRepo,
                               ProcessEventRepository eventRepo,
                               DocumentoProcessualRepository docRepo,
                               PjbAuthorizationService authz,
                               RecursalEffectiveSecrecyService secrecyService,
                               ObjectMapper mapper) {
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.eventRepo = Objects.requireNonNull(eventRepo);
    this.docRepo = Objects.requireNonNull(docRepo);
    this.authz = Objects.requireNonNull(authz);
    this.secrecyService = Objects.requireNonNull(secrecyService);
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Transactional(readOnly = true)
  public List<CidadaoJuntadaResumoDto> listar(Long processoId, Integer limit) {
    Processo p = processoRepo.findById(processoId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

    authz.requireReadProcessoAsCidadaoParte(p);

    NivelSigilo efetivo = secrecyService.effectiveSecrecyForProcesso(processoId);
    authz.requireReadProcessoAtSecrecy(p, efetivo);

    int lim = (limit == null || limit <= 0) ? 50 : Math.min(200, limit);
    List<String> types = List.of(ProcessEventType.DOCUMENTS_BULK_ADDED.name(), ProcessEventType.DOCUMENT_ADDED.name());
    List<ProcessEventEnvelope> events = eventRepo.findRecentByProcessoIdAndTypes(processoId, types, PageRequest.of(0, lim));

    if (events.isEmpty()) return List.of();

    List<JuntadaPayload> payloads = new ArrayList<>(events.size());
    Set<UUID> allDocIds = new HashSet<>();
    for (ProcessEventEnvelope env : events) {
      JuntadaPayload jp = parse(env);
      payloads.add(jp);
      if (jp.documentos != null) {
        for (JudiciarioEventSourcingEngine.DocumentoMetadata dm : jp.documentos) {
          if (dm != null && dm.docId() != null) allDocIds.add(dm.docId());
        }
      }
    }

    Map<UUID, DocumentoProcessual> docs = new HashMap<>();
    if (!allDocIds.isEmpty()) {
      for (DocumentoProcessual d : docRepo.findAllById(allDocIds)) {
        if (d != null && d.getId() != null) docs.put(d.getId(), d);
      }
    }

    boolean stepUpOk = JwtStepUpClaims.hasMfa();

    List<CidadaoJuntadaResumoDto> out = new ArrayList<>(events.size());
    for (int i = 0; i < events.size(); i++) {
      ProcessEventEnvelope env = events.get(i);
      JuntadaPayload jp = payloads.get(i);
      int total = (jp.documentos == null) ? 0 : jp.documentos.size();

      int visible = 0;
      if (jp.documentos != null) {
        for (JudiciarioEventSourcingEngine.DocumentoMetadata dm : jp.documentos) {
          if (dm == null || dm.docId() == null) continue;
          DocumentoProcessual dp = docs.get(dm.docId());
          if (dp == null) continue;

          boolean policyAllowed = authz.canReadDocumentoAtSecrecy(p, dp, efetivo).allowed();
          if (!policyAllowed) continue;

          DocumentoCategoria cat = dp.getCategoria() == null ? DocumentoCategoria.PUBLICO : dp.getCategoria();
          NivelSigilo docSig = dp.getNivelSigilo() == null ? NivelSigilo.PUBLICO : dp.getNivelSigilo();
          NivelSigilo minCat = (cat == DocumentoCategoria.PESSOAL) ? NivelSigilo.SIGILO_N2 : NivelSigilo.PUBLICO;
          NivelSigilo docEfetivo = maxSigilo(efetivo, maxSigilo(docSig, minCat));

          boolean high = docEfetivo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel();
          if (high && !stepUpOk) {
            continue;
          }
          visible++;
        }
      }

      out.add(new CidadaoJuntadaResumoDto(
          env.getSeq() != null ? env.getSeq() : 0L,
          env.getCreatedAt(),
          env.getEventType(),
          jp.label,
          total,
          visible,
          jp.eventoId
      ));
    }

    return out;
  }

  private JuntadaPayload parse(ProcessEventEnvelope env) {
    String type = env.getEventType();
    String payload = env.getPayload();
    if (payload == null || payload.isBlank()) {
      return new JuntadaPayload("JUNTADA", null, List.of());
    }

    if (ProcessEventType.DOCUMENTS_BULK_ADDED.name().equals(type)) {
      try {
        JudiciarioEventSourcingEngine.DocumentosJuntados dj = mapper.readValue(payload, JudiciarioEventSourcingEngine.DocumentosJuntados.class);
        return new JuntadaPayload("JUNTADA_EM_LOTE", dj.eventoId(), dj.documentos() != null ? dj.documentos() : List.of());
      } catch (Exception ignore) {
      }
    }

    try {
      JudiciarioEventSourcingEngine.DocumentosJuntados dj = mapper.readValue(payload, JudiciarioEventSourcingEngine.DocumentosJuntados.class);
      if (dj.documentos() != null && !dj.documentos().isEmpty()) {
        return new JuntadaPayload("JUNTADA", dj.eventoId(), dj.documentos());
      }
    } catch (Exception ignore) {
    }

    try {
      Map<?, ?> m = mapper.readValue(payload, Map.class);
      Object docIdRaw = m.get("docId");
      UUID docId = docIdRaw != null ? UUID.fromString(String.valueOf(docIdRaw)) : null;
      String storageUri = m.get("storageUri") != null ? String.valueOf(m.get("storageUri")) : null;
      String hashSha384 = m.get("hashSha384") != null ? String.valueOf(m.get("hashSha384")) : null;
      long size = 0L;
      Object sz = m.get("tamanhoBytes");
      if (sz instanceof Number n) size = n.longValue();
      else if (sz != null) {
        try { size = Long.parseLong(String.valueOf(sz)); } catch (Exception ignore) {}
      }
      List<JudiciarioEventSourcingEngine.DocumentoMetadata> docs = docId != null
          ? List.of(new JudiciarioEventSourcingEngine.DocumentoMetadata(docId, storageUri, hashSha384, size))
          : List.of();
      return new JuntadaPayload("JUNTADA", null, docs);
    } catch (Exception ignore) {
      return new JuntadaPayload("JUNTADA", null, List.of());
    }
  }

  private static NivelSigilo maxSigilo(NivelSigilo a, NivelSigilo b) {
    NivelSigilo x = (a == null) ? NivelSigilo.PUBLICO : a;
    NivelSigilo y = (b == null) ? NivelSigilo.PUBLICO : b;
    return (x.getNivel() >= y.getNivel()) ? x : y;
  }
}
