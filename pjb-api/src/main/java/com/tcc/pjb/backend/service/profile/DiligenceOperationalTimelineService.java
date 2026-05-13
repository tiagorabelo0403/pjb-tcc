package com.tcc.pjb.backend.service.profile;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalTimelineEntryResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorAnexacaoInstitucional;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorFormalizacaoProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorJuntadaProcessual;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorMalhaInstitucionalDispatch;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorTelemetria;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorAnexacaoInstitucionalRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorFormalizacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorJuntadaProcessualRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorMalhaInstitucionalDispatchRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorTelemetriaRepository;
import com.tcc.pjb.backend.model.repository.ProcessEventRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class DiligenceOperationalTimelineService {

    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final DiligenciaOperadorTelemetriaRepository telemetriaRepository;
    private final DiligenciaOperadorCheckpointEventoRepository checkpointRepository;
    private final DiligenciaOperadorCertidaoRepository certidaoRepository;
    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository;
    private final DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository;
    private final DiligenciaOperadorJuntadaProcessualRepository juntadaRepository;
    private final DiligenciaOperadorAnexacaoInstitucionalRepository anexacaoRepository;
    private final DiligenciaOperadorMalhaInstitucionalDispatchRepository meshDispatchRepository;
    private final DiligenceReferenceResolverService referenceResolverService;
    private final ProcessoRepository processoRepository;
    private final ProcessEventRepository processEventRepository;

    public DiligenceOperationalTimelineService(CurrentUserService currentUserService,
                                               PjbAuthorizationService authorizationService,
                                               DiligenciaOperadorTelemetriaRepository telemetriaRepository,
                                               DiligenciaOperadorCheckpointEventoRepository checkpointRepository,
                                               DiligenciaOperadorCertidaoRepository certidaoRepository,
                                               DiligenciaOperadorEncerramentoRepository encerramentoRepository,
                                               DiligenciaOperadorFormalizacaoProcessualRepository formalizacaoRepository,
                                               DiligenciaOperadorJuntadaProcessualRepository juntadaRepository,
                                               DiligenciaOperadorAnexacaoInstitucionalRepository anexacaoRepository,
                                               DiligenciaOperadorMalhaInstitucionalDispatchRepository meshDispatchRepository,
                                               DiligenceReferenceResolverService referenceResolverService,
                                               ProcessoRepository processoRepository,
                                               ProcessEventRepository processEventRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.telemetriaRepository = Objects.requireNonNull(telemetriaRepository);
        this.checkpointRepository = Objects.requireNonNull(checkpointRepository);
        this.certidaoRepository = Objects.requireNonNull(certidaoRepository);
        this.encerramentoRepository = Objects.requireNonNull(encerramentoRepository);
        this.formalizacaoRepository = Objects.requireNonNull(formalizacaoRepository);
        this.juntadaRepository = Objects.requireNonNull(juntadaRepository);
        this.anexacaoRepository = Objects.requireNonNull(anexacaoRepository);
        this.meshDispatchRepository = Objects.requireNonNull(meshDispatchRepository);
        this.referenceResolverService = Objects.requireNonNull(referenceResolverService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processEventRepository = Objects.requireNonNull(processEventRepository);
    }

    @Transactional(readOnly = true)
    public List<DiligenceOperationalTimelineEntryResponse> timeline(TelemetriaOperacionalCanal canal,
                                                                    String diligenceReference,
                                                                    int limit) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        int cappedLimit = Math.max(1, Math.min(limit, 60));
        Usuario actor = currentUserService.getRequired();
        String normalizedReference = diligenceReference.trim();
        List<DiligenceOperationalTimelineEntryResponse> out = new ArrayList<>();

        List<DiligenciaOperadorCheckpointEvento> checkpoints = checkpointRepository
                .findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(actor.getId(), canal, normalizedReference);
        List<DiligenciaOperadorCertidao> certidoes = certidaoRepository
                .findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference);
        List<DiligenciaOperadorEncerramento> encerramentos = encerramentoRepository
                .findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference);
        List<DiligenciaOperadorFormalizacaoProcessual> formalizacoes = formalizacaoRepository
                .findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference);
        List<DiligenciaOperadorJuntadaProcessual> juntadas = juntadaRepository
                .findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference);
        List<DiligenciaOperadorAnexacaoInstitucional> anexacoes = anexacaoRepository
                .findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference);
        List<DiligenciaOperadorMalhaInstitucionalDispatch> meshDispatches = meshDispatchRepository
                .findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, normalizedReference);

        Instant minOperationalInstant = checkpoints.isEmpty() ? null : checkpoints.getLast().getOccurredAt();
        Instant maxOperationalInstant = latestInstant(checkpoints, certidoes, encerramentos, formalizacoes, juntadas, anexacoes, meshDispatches);
        List<DiligenciaOperadorTelemetria> telemetrias = telemetriaRepository
                .findByOperatorUserIdAndCanalOrderByCapturadoEmDesc(actor.getId(), canal, PageRequest.of(0, 30))
                .stream()
                .filter(item -> withinOperationalWindow(item.getCapturadoEm(), minOperationalInstant, maxOperationalInstant))
                .limit(8)
                .toList();

        Long processoId = resolveProcessoId(checkpoints, certidoes, formalizacoes, juntadas, anexacoes, meshDispatches, canal, normalizedReference);
        String processoNumero = resolveProcessoNumero(checkpoints, certidoes, formalizacoes, juntadas, anexacoes, meshDispatches, canal, normalizedReference);
        if (processoId != null) {
            Processo processo = processoRepository.findById(processoId).orElse(null);
            if (processo != null) {
                authorizationService.requireReadProcesso(processo);
                processoNumero = processo.getNumeroProcesso() != null ? processo.getNumeroProcesso() : processoNumero;
            }
        }
        final Long timelineProcessoId = processoId;
        final String timelineProcessoNumero = processoNumero;

        telemetrias.forEach(item -> out.add(new DiligenceOperationalTimelineEntryResponse(
                "TELEMETRIA",
                item.getId(),
                item.getCapturadoEm(),
                canal.name(),
                normalizedReference,
                timelineProcessoId,
                timelineProcessoNumero,
                null,
                null,
                "Telemetria operacional capturada",
                item.isForeground() ? "FOREGROUND" : "BACKGROUND",
                "fonte=" + nv(item.getFonte()) + "; lat=" + item.getLatitude() + "; lon=" + item.getLongitude() + "; precisao_metros=" + nv(item.getPrecisaoMetros()),
                null,
                null,
                null
        )));
        checkpoints.forEach(item -> out.add(new DiligenceOperationalTimelineEntryResponse(
                "CHECKPOINT",
                item.getId(),
                item.getOccurredAt(),
                canal.name(),
                normalizedReference,
                firstNonNull(item.getProcessoId(), timelineProcessoId),
                firstNonBlank(item.getProcessoNumero(), timelineProcessoNumero),
                item.getWorkItemId(),
                null,
                "Checkpoint de chegada operacional",
                item.getClassification(),
                "checkpoint_tipo=" + item.getCheckpointTipo().name() + "; distancia_metros=" + item.getDistanceMeters() + "; inside_geofence=" + item.isInsideGeofence(),
                item.getLocationSignatureSha256(),
                null,
                null
        )));
        certidoes.forEach(item -> out.add(new DiligenceOperationalTimelineEntryResponse(
                "CERTIDAO",
                item.getId(),
                item.getCreatedAt(),
                canal.name(),
                normalizedReference,
                firstNonNull(item.getProcessoId(), timelineProcessoId),
                firstNonBlank(item.getProcessoNumero(), timelineProcessoNumero),
                item.getWorkItemId(),
                null,
                item.getTitulo(),
                item.getCertidaoTipo().name(),
                summarizeNarrative(item.getNarrativa()),
                item.getCertificateDigestSha256(),
                null,
                null
        )));
        encerramentos.forEach(item -> out.add(new DiligenceOperationalTimelineEntryResponse(
                "ENCERRAMENTO",
                item.getId(),
                item.getCreatedAt(),
                canal.name(),
                normalizedReference,
                firstNonNull(item.getProcessoId(), timelineProcessoId),
                firstNonBlank(item.getProcessoNumero(), timelineProcessoNumero),
                item.getWorkItemId(),
                null,
                "Encerramento operacional registrado",
                item.getOutcome().name(),
                "work_item_status_final=" + nv(item.getWorkItemStatusFinal()) + "; followup_work_item_id=" + nv(item.getFollowupWorkItemId()),
                item.getExecutionDigestSha256(),
                null,
                null
        )));
        formalizacoes.forEach(item -> out.add(new DiligenceOperationalTimelineEntryResponse(
                "FORMALIZACAO",
                item.getId(),
                item.getCreatedAt(),
                canal.name(),
                normalizedReference,
                firstNonNull(item.getProcessoId(), timelineProcessoId),
                firstNonBlank(item.getProcessoNumero(), timelineProcessoNumero),
                item.getWorkItemId(),
                item.getMovimentacaoEventSeq(),
                "Formalização processual auditável",
                "FORMALIZADO",
                "movimentacao_id=" + nv(item.getMovimentacaoId()) + "; minuta_documento_id=" + nv(item.getMinutaDocumentoId()) + "; documentos_referenciados=" + nv(item.getDocumentosReferenciados()),
                item.getFormalizationDigestSha256(),
                item.getMinutaDocumentoId() != null ? item.getMinutaDocumentoId().toString() : null,
                null
        )));
        juntadas.forEach(item -> out.add(new DiligenceOperationalTimelineEntryResponse(
                "JUNTADA_AUTOMATICA",
                item.getId(),
                item.getCreatedAt(),
                canal.name(),
                normalizedReference,
                firstNonNull(item.getProcessoId(), timelineProcessoId),
                firstNonBlank(item.getProcessoNumero(), timelineProcessoNumero),
                item.getWorkItemId(),
                item.getMovimentacaoEventSeq(),
                item.getPacoteTitulo() != null ? item.getPacoteTitulo() : "Juntada automática institucional",
                Boolean.TRUE.equals(item.getExportarMalhaExterna()) ? "EXPORTE_HABILITADO" : "LOCAL",
                "movimentacao_id=" + nv(item.getMovimentacaoId()) + "; pacote_documento_id=" + nv(item.getPacoteDocumentoId()) + "; external_system_code=" + nv(item.getExternalSystemCode()),
                item.getBundleDigestSha256(),
                item.getPacoteDocumentoId() != null ? item.getPacoteDocumentoId().toString() : null,
                item.getBundleReference()
        )));
        anexacoes.forEach(item -> out.add(new DiligenceOperationalTimelineEntryResponse(
                "ANEXACAO_INSTITUCIONAL",
                item.getId(),
                item.getCreatedAt(),
                canal.name(),
                normalizedReference,
                firstNonNull(item.getProcessoId(), timelineProcessoId),
                firstNonBlank(item.getProcessoNumero(), timelineProcessoNumero),
                item.getWorkItemId(),
                item.getProcessEventSeq(),
                "Anexação institucional confirmada",
                item.getAnnexationStatus(),
                "external_system_code=" + nv(item.getExternalSystemCode()) + "; destination_box=" + nv(item.getDestinationBox()) + "; ack_protocol=" + nv(item.getAckProtocol()),
                item.getExecutionDigestSha256(),
                item.getPacoteDocumentoId() != null ? item.getPacoteDocumentoId().toString() : null,
                item.getBundleReference()
        )));


        meshDispatches.forEach(item -> out.add(new DiligenceOperationalTimelineEntryResponse(
                "MALHA_DISPATCH",
                item.getId(),
                firstNonNull(item.getAcknowledgedAt(), item.getDeliveredAt(), item.getCreatedAt()),
                canal.name(),
                normalizedReference,
                firstNonNull(item.getProcessoId(), timelineProcessoId),
                firstNonBlank(item.getProcessoNumero(), timelineProcessoNumero),
                item.getWorkItemId(),
                null,
                "Expedição transacional de malha institucional",
                item.getDispatchStatus(),
                "routing_key=" + nv(item.getRoutingKey()) + "; mesh_org_key=" + nv(item.getMeshOrgKey()) + "; mesh_unit_key=" + nv(item.getMeshUnitKey()),
                item.getPayloadDigestSha256(),
                item.getOutboxEventId() != null ? item.getOutboxEventId().toString() : null,
                null
        )));

        if (timelineProcessoId != null) {
            processEventRepository.findRecentByProcessoIdAndTypes(timelineProcessoId,
                            List.of("MOVEMENT_RECORDED", "DOCUMENT_ADDED", "DOCUMENTS_BULK_ADDED"),
                            PageRequest.of(0, 12))
                    .forEach(item -> out.add(toProcessEventEntry(item, canal, normalizedReference, timelineProcessoId, timelineProcessoNumero)));
        }

        return out.stream()
                .sorted(Comparator.comparing(DiligenceOperationalTimelineEntryResponse::occurredAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DiligenceOperationalTimelineEntryResponse::sourceType, Comparator.nullsLast(String::compareTo)))
                .limit(cappedLimit)
                .toList();
    }

    private DiligenceOperationalTimelineEntryResponse toProcessEventEntry(ProcessEventEnvelope item,
                                                                          TelemetriaOperacionalCanal canal,
                                                                          String normalizedReference,
                                                                          Long processoId,
                                                                          String processoNumero) {
        return new DiligenceOperationalTimelineEntryResponse(
                "PROCESS_EVENT",
                item.getId(),
                item.getCreatedAt(),
                canal.name(),
                normalizedReference,
                processoId,
                processoNumero,
                null,
                item.getSeq(),
                "Evento processual sincronizado",
                item.getEventType(),
                summarizeProcessPayload(item.getPayload()),
                item.getPayloadHash(),
                null,
                null
        );
    }

    private boolean withinOperationalWindow(Instant capturedAt,
                                            Instant minOperationalInstant,
                                            Instant maxOperationalInstant) {
        if (capturedAt == null) {
            return false;
        }
        if (minOperationalInstant == null && maxOperationalInstant == null) {
            return true;
        }
        Instant min = minOperationalInstant != null ? minOperationalInstant.minus(Duration.ofHours(12)) : maxOperationalInstant.minus(Duration.ofHours(24));
        Instant max = maxOperationalInstant != null ? maxOperationalInstant.plus(Duration.ofHours(12)) : minOperationalInstant.plus(Duration.ofHours(24));
        return !capturedAt.isBefore(min) && !capturedAt.isAfter(max);
    }

    private Instant latestInstant(List<DiligenciaOperadorCheckpointEvento> checkpoints,
                                  List<DiligenciaOperadorCertidao> certidoes,
                                  List<DiligenciaOperadorEncerramento> encerramentos,
                                  List<DiligenciaOperadorFormalizacaoProcessual> formalizacoes,
                                  List<DiligenciaOperadorJuntadaProcessual> juntadas,
                                  List<DiligenciaOperadorAnexacaoInstitucional> anexacoes,
                                  List<DiligenciaOperadorMalhaInstitucionalDispatch> meshDispatches) {
        List<Instant> instants = new ArrayList<>();
        addIfPresent(instants, firstFrom(checkpoints, DiligenciaOperadorCheckpointEvento::getOccurredAt));
        addIfPresent(instants, firstFrom(certidoes, DiligenciaOperadorCertidao::getCreatedAt));
        addIfPresent(instants, firstFrom(encerramentos, DiligenciaOperadorEncerramento::getCreatedAt));
        addIfPresent(instants, firstFrom(formalizacoes, DiligenciaOperadorFormalizacaoProcessual::getCreatedAt));
        addIfPresent(instants, firstFrom(juntadas, DiligenciaOperadorJuntadaProcessual::getCreatedAt));
        addIfPresent(instants, firstFrom(anexacoes, DiligenciaOperadorAnexacaoInstitucional::getCreatedAt));
        addIfPresent(instants, firstFrom(meshDispatches,
                dispatch -> firstAvailable(dispatch.getAcknowledgedAt(), dispatch.getDeliveredAt(), dispatch.getCreatedAt())));
        return instants.stream().max(Instant::compareTo).orElse(null);
    }

    private static void addIfPresent(List<Instant> instants,
                                     Instant value) {
        if (value != null) {
            instants.add(value);
        }
    }

    private static <T> Instant firstFrom(List<T> values,
                                         java.util.function.Function<T, Instant> extractor) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        T first = values.getFirst();
        return first == null ? null : extractor.apply(first);
    }

    private static Instant firstAvailable(Instant... values) {
        if (values == null) {
            return null;
        }
        for (Instant value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long resolveProcessoId(List<DiligenciaOperadorCheckpointEvento> checkpoints,
                                   List<DiligenciaOperadorCertidao> certidoes,
                                   List<DiligenciaOperadorFormalizacaoProcessual> formalizacoes,
                                   List<DiligenciaOperadorJuntadaProcessual> juntadas,
                                   List<DiligenciaOperadorAnexacaoInstitucional> anexacoes,
                                   List<DiligenciaOperadorMalhaInstitucionalDispatch> meshDispatches,
                                   TelemetriaOperacionalCanal canal,
                                   String diligenceReference) {
        Long processId = formalizacoes.stream().map(DiligenciaOperadorFormalizacaoProcessual::getProcessoId).filter(Objects::nonNull).findFirst().orElse(null);
        if (processId != null) {
            return processId;
        }
        processId = juntadas.stream().map(DiligenciaOperadorJuntadaProcessual::getProcessoId).filter(Objects::nonNull).findFirst().orElse(null);
        if (processId != null) {
            return processId;
        }
        processId = certidoes.stream().map(DiligenciaOperadorCertidao::getProcessoId).filter(Objects::nonNull).findFirst().orElse(null);
        if (processId != null) {
            return processId;
        }
        processId = checkpoints.stream().map(DiligenciaOperadorCheckpointEvento::getProcessoId).filter(Objects::nonNull).findFirst().orElse(null);
        if (processId != null) {
            return processId;
        }
        processId = anexacoes.stream().map(DiligenciaOperadorAnexacaoInstitucional::getProcessoId).filter(Objects::nonNull).findFirst().orElse(null);
        if (processId != null) {
            return processId;
        }
        processId = meshDispatches.stream().map(DiligenciaOperadorMalhaInstitucionalDispatch::getProcessoId).filter(Objects::nonNull).findFirst().orElse(null);
        if (processId != null) {
            return processId;
        }
        return referenceResolverService.resolve(canal, diligenceReference).map(DiligenceReferenceResolverService.ResolvedDiligenceReference::processoId).orElse(null);
    }

    private String resolveProcessoNumero(List<DiligenciaOperadorCheckpointEvento> checkpoints,
                                         List<DiligenciaOperadorCertidao> certidoes,
                                         List<DiligenciaOperadorFormalizacaoProcessual> formalizacoes,
                                         List<DiligenciaOperadorJuntadaProcessual> juntadas,
                                         List<DiligenciaOperadorAnexacaoInstitucional> anexacoes,
                                         List<DiligenciaOperadorMalhaInstitucionalDispatch> meshDispatches,
                                         TelemetriaOperacionalCanal canal,
                                         String diligenceReference) {
        String processNumber = formalizacoes.stream().map(DiligenciaOperadorFormalizacaoProcessual::getProcessoNumero).filter(Objects::nonNull).findFirst().orElse(null);
        if (processNumber != null) {
            return processNumber;
        }
        processNumber = juntadas.stream().map(DiligenciaOperadorJuntadaProcessual::getProcessoNumero).filter(Objects::nonNull).findFirst().orElse(null);
        if (processNumber != null) {
            return processNumber;
        }
        processNumber = certidoes.stream().map(DiligenciaOperadorCertidao::getProcessoNumero).filter(Objects::nonNull).findFirst().orElse(null);
        if (processNumber != null) {
            return processNumber;
        }
        processNumber = checkpoints.stream().map(DiligenciaOperadorCheckpointEvento::getProcessoNumero).filter(Objects::nonNull).findFirst().orElse(null);
        if (processNumber != null) {
            return processNumber;
        }
        processNumber = anexacoes.stream().map(DiligenciaOperadorAnexacaoInstitucional::getProcessoNumero).filter(Objects::nonNull).findFirst().orElse(null);
        if (processNumber != null) {
            return processNumber;
        }
        processNumber = meshDispatches.stream().map(DiligenciaOperadorMalhaInstitucionalDispatch::getProcessoNumero).filter(Objects::nonNull).findFirst().orElse(null);
        if (processNumber != null) {
            return processNumber;
        }
        return referenceResolverService.resolve(canal, diligenceReference).map(DiligenceReferenceResolverService.ResolvedDiligenceReference::processoNumero).orElse(null);
    }

    private String summarizeNarrative(String narrative) {
        if (narrative == null || narrative.isBlank()) {
            return "-";
        }
        String normalized = narrative.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180);
    }

    private String summarizeProcessPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return "payload_vazio";
        }
        String normalized = payload.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180);
    }

    private Instant firstNonNull(Instant... values) {
        if (values == null) {
            return null;
        }
        for (Instant value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long firstNonNull(Long first,
                              Long second) {
        return first != null ? first : second;
    }

    private String firstNonBlank(String first,
                                 String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String nv(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
