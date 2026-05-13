package com.tcc.pjb.backend.core.comunicacao.institucional.integration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryDispatchResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.contract.InstitutionalCommunicationContractEnvelope;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.contract.InstitutionalCommunicationContractFactory;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalCommunicationMeshMirrorResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatch;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatchResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.infrastructure.InstitutionalExternalAdapter;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.application.InstitutionalCommunicationObservabilityApplicationService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.MotivoFalhaEntregaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusIntegracaoInstitucionalExterna;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalExternalIntegrationApplicationService {

    private final List<InstitutionalExternalAdapter> adapters;
    private final InstitutionalCommunicationObservabilityApplicationService observabilityService;
    private final InstitutionalCommunicationMeshBridgeService meshBridgeService;
    private final InstitutionalCommunicationContractFactory contractFactory;
    private final ObjectMapper objectMapper;

    public InstitutionalExternalIntegrationApplicationService(List<InstitutionalExternalAdapter> adapters,
                                                              InstitutionalCommunicationObservabilityApplicationService observabilityService,
                                                              InstitutionalCommunicationMeshBridgeService meshBridgeService,
                                                              InstitutionalCommunicationContractFactory contractFactory,
                                                              ObjectMapper objectMapper) {
        this.adapters = List.copyOf(Objects.requireNonNull(adapters));
        this.observabilityService = Objects.requireNonNull(observabilityService);
        this.meshBridgeService = Objects.requireNonNull(meshBridgeService);
        this.contractFactory = Objects.requireNonNull(contractFactory);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public InstitutionalDeliveryDispatchResult despachar(InstitutionalDeliveryJob job) {
        return despacharRastreavel(job).deliveryResult();
    }

    public TraceableExternalDispatchResult despacharRastreavel(InstitutionalDeliveryJob job) {
        InstitutionalExternalAdapter adapter = adapters.stream()
                .filter(candidate -> candidate.supports(job.currentChannel()))
                .findFirst()
                .orElse(null);
        if (adapter == null) {
            InstitutionalDeliveryDispatchResult failure = InstitutionalDeliveryDispatchResult.falhaTerminal(
                    MotivoFalhaEntregaInstitucional.CANAL_NAO_SUPORTADO,
                    "NO_EXTERNAL_ADAPTER",
                    "Nenhum adapter externo disponível para o canal " + job.currentChannel().name()
            );
            return new TraceableExternalDispatchResult(null, null, null, failure);
        }
        Instant now = Instant.now();
        String provider = providerName(job.currentChannel());
        String payloadJson = serialize(buildContract(job, provider));
        String dispatchId = UUID.nameUUIDFromBytes((job.jobId() + "|" + job.currentChannel().name() + "|" + (job.attemptCount() + 1)).getBytes(StandardCharsets.UTF_8)).toString();
        InstitutionalExternalDispatch prepared = new InstitutionalExternalDispatch(
                dispatchId,
                job.jobId(),
                job.expedicaoUuid(),
                job.processoId(),
                job.processoNumero(),
                job.unidadeCodigo(),
                job.caixaCodigo(),
                job.destinatarioKind(),
                job.papelProcessual(),
                job.currentChannel(),
                provider,
                routingKey(job.currentChannel()),
                eventType(job.currentChannel()),
                "institutional-external:" + job.jobId() + ":" + job.currentChannel().name() + ":" + Math.max(1, job.attemptCount() + 1),
                StatusIntegracaoInstitucionalExterna.PREPARADA,
                null,
                Hashes.sha256Hex(payloadJson),
                payloadJson,
                null,
                null,
                now,
                now
        );
        observabilityService.registrarIntegracaoExterna(prepared);
        InstitutionalCommunicationMeshMirrorResult mirror = job.currentChannel() != CanalComunicacaoInstitucional.PJB_INBOX
                ? meshBridgeService.espelhar(job)
                : null;
        InstitutionalExternalDispatchResult result = adapter.dispatch(prepared);
        InstitutionalExternalDispatch updated = switch (result.status()) {
            case ACEITA -> prepared.withAccepted(Instant.now(), result.providerReference(), result.responsePayload());
            case FALHA_TRANSITORIA -> prepared.withFailure(Instant.now(), StatusIntegracaoInstitucionalExterna.FALHA_TRANSITORIA, result.failureReason(), result.responsePayload());
            case FALHA_TERMINAL -> prepared.withFailure(Instant.now(), StatusIntegracaoInstitucionalExterna.FALHA_TERMINAL, result.failureReason(), result.responsePayload());
            default -> prepared.withFailure(Instant.now(), result.status(), result.failureReason(), result.responsePayload());
        };
        observabilityService.registrarIntegracaoExterna(updated);
        InstitutionalDeliveryDispatchResult deliveryResult = switch (result.status()) {
            case ACEITA -> InstitutionalDeliveryDispatchResult.encaminhada(result.providerReference(), result.providerStatus(), "Integração externa aceita pelo adaptador " + updated.provider());
            case FALHA_TRANSITORIA -> InstitutionalDeliveryDispatchResult.retry(MotivoFalhaEntregaInstitucional.INTEGRACAO_INDISPONIVEL, result.providerStatus(), result.failureReason());
            case FALHA_TERMINAL -> InstitutionalDeliveryDispatchResult.falhaTerminal(MotivoFalhaEntregaInstitucional.CANAL_NAO_SUPORTADO, result.providerStatus(), result.failureReason());
            default -> InstitutionalDeliveryDispatchResult.retry(MotivoFalhaEntregaInstitucional.ERRO_TRANSITORIO, result.providerStatus(), result.failureReason());
        };
        return new TraceableExternalDispatchResult(mirror, prepared, updated, deliveryResult);
    }

    private InstitutionalCommunicationContractEnvelope buildContract(InstitutionalDeliveryJob job, String provider) {
        return contractFactory.build(job, provider);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("institutionalExternalPayload", ex);
        }
    }

    private String providerName(CanalComunicacaoInstitucional channel) {
        return switch (channel) {
            case DOMICILIO_JUDICIAL_ELETRONICO -> "DJE";
            case DJEN -> "DJEN";
            case WEBHOOK_INSTITUCIONAL -> "WEBHOOK";
            case PORTAL_LEGADO_INTEGRADO -> "LEGACY_PORTAL";
            case COMUNICACAO_FISICA_OFICIAL -> "OFICIAL_FISICO";
            default -> "PJB";
        };
    }

    private String routingKey(CanalComunicacaoInstitucional channel) {
        return "institutional.external." + channel.name().toLowerCase();
    }

    private String eventType(CanalComunicacaoInstitucional channel) {
        return switch (channel) {
            case DOMICILIO_JUDICIAL_ELETRONICO -> "INSTITUTIONAL_DJE_REQUEST";
            case DJEN -> "INSTITUTIONAL_DJEN_REQUEST";
            case WEBHOOK_INSTITUCIONAL -> "INSTITUTIONAL_WEBHOOK_REQUEST";
            case PORTAL_LEGADO_INTEGRADO -> "INSTITUTIONAL_LEGACY_PORTAL_REQUEST";
            case COMUNICACAO_FISICA_OFICIAL -> "INSTITUTIONAL_OFFICIAL_REQUEST";
            default -> "INSTITUTIONAL_EXTERNAL_REQUEST";
        };
    }

    public record TraceableExternalDispatchResult(
            InstitutionalCommunicationMeshMirrorResult meshMirror,
            InstitutionalExternalDispatch preparedDispatch,
            InstitutionalExternalDispatch dispatch,
            InstitutionalDeliveryDispatchResult deliveryResult
    ) {
    }
}
