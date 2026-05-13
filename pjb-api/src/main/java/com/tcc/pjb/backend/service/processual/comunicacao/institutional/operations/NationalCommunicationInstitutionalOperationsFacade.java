package com.tcc.pjb.backend.service.processual.comunicacao.institutional.operations;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.AutorizacaoCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.VinculoUsuarioCaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.VinculoUsuarioCaixaInstitucionalResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.application.InstitutionalCommunicationAuditApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain.InstitutionalDeliveryProof;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain.InstitutionalTimelineEvent;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.application.InstitutionalDeliveryQueueApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeadLetterEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.application.InstitutionalCommunicationGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.application.InstitutionalCommunicationConcurrencyGuardService;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.application.InstitutionalCommunicationHardeningApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.hardening.domain.InstitutionalCommunicationHardeningReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxActionResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalExternalDispatch;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.application.InstitutionalCommunicationObservabilityApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.observability.domain.InstitutionalObservabilityDashboard;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessCheckRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessCheckResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalMembershipResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalDeadLetterResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalDeliveryProofResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalExternalDispatchResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalFulfillRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalGateStateResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalReceiveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalRedistributeRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalReprocessDeliveryRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalScienceRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalDeliveryQueueItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalInboxItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalObservabilityBucketResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalObservabilityDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalTimelineEventResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalHardeningFindingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalHardeningReportResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.access.InstitutionalRequestAccessContextFacadeService;
import java.util.List;
import java.util.Objects;

public final class NationalCommunicationInstitutionalOperationsFacade {

    private final ProcessoRepository processoRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final VinculoUsuarioCaixaInstitucionalResolver vinculoUsuarioCaixaInstitucionalResolver;
    private final AutorizacaoCaixaInstitucionalService autorizacaoCaixaInstitucionalService;
    private final InstitutionalInboxApplicationService institutionalInboxApplicationService;
    private final InstitutionalCommunicationAuditApplicationService institutionalCommunicationAuditApplicationService;
    private final InstitutionalCommunicationGateApplicationService institutionalCommunicationGateApplicationService;
    private final InstitutionalDeliveryQueueApplicationService institutionalDeliveryQueueApplicationService;
    private final InstitutionalCommunicationObservabilityApplicationService institutionalCommunicationObservabilityApplicationService;
    private final InstitutionalCommunicationConcurrencyGuardService institutionalCommunicationConcurrencyGuardService;
    private final InstitutionalCommunicationHardeningApplicationService institutionalCommunicationHardeningApplicationService;
    private final InstitutionalRequestAccessContextFacadeService accessContextFacadeService;

    public NationalCommunicationInstitutionalOperationsFacade(ProcessoRepository processoRepository,
                                                       CurrentUserService currentUserService,
                                                       PjbAuthorizationService authorizationService,
                                                       VinculoUsuarioCaixaInstitucionalResolver vinculoUsuarioCaixaInstitucionalResolver,
                                                       AutorizacaoCaixaInstitucionalService autorizacaoCaixaInstitucionalService,
                                                       InstitutionalInboxApplicationService institutionalInboxApplicationService,
                                                       InstitutionalCommunicationAuditApplicationService institutionalCommunicationAuditApplicationService,
                                                       InstitutionalCommunicationGateApplicationService institutionalCommunicationGateApplicationService,
                                                       InstitutionalDeliveryQueueApplicationService institutionalDeliveryQueueApplicationService,
                                                       InstitutionalCommunicationObservabilityApplicationService institutionalCommunicationObservabilityApplicationService,
                                                       InstitutionalCommunicationConcurrencyGuardService institutionalCommunicationConcurrencyGuardService,
                                                       InstitutionalCommunicationHardeningApplicationService institutionalCommunicationHardeningApplicationService,
                                                       InstitutionalRequestAccessContextFacadeService accessContextFacadeService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.vinculoUsuarioCaixaInstitucionalResolver = Objects.requireNonNull(vinculoUsuarioCaixaInstitucionalResolver);
        this.autorizacaoCaixaInstitucionalService = Objects.requireNonNull(autorizacaoCaixaInstitucionalService);
        this.institutionalInboxApplicationService = Objects.requireNonNull(institutionalInboxApplicationService);
        this.institutionalCommunicationAuditApplicationService = Objects.requireNonNull(institutionalCommunicationAuditApplicationService);
        this.institutionalCommunicationGateApplicationService = Objects.requireNonNull(institutionalCommunicationGateApplicationService);
        this.institutionalDeliveryQueueApplicationService = Objects.requireNonNull(institutionalDeliveryQueueApplicationService);
        this.institutionalCommunicationObservabilityApplicationService = Objects.requireNonNull(institutionalCommunicationObservabilityApplicationService);
        this.institutionalCommunicationConcurrencyGuardService = Objects.requireNonNull(institutionalCommunicationConcurrencyGuardService);
        this.institutionalCommunicationHardeningApplicationService = Objects.requireNonNull(institutionalCommunicationHardeningApplicationService);
        this.accessContextFacadeService = Objects.requireNonNull(accessContextFacadeService);
    }

    public List<NationalCommunicationInstitutionalMembershipResponse> minhasCaixasInstitucionais(DestinatarioInstitucionalKind destinatarioKind,
                                                                                           String uf,
                                                                                           String comarca) {
        Usuario usuario = currentUserService.getRequired();
        return vinculoUsuarioCaixaInstitucionalResolver.resolver(usuario, destinatarioKind, uf, comarca).stream()
                .map(this::toMembershipResponse)
                .toList();
    }

    public NationalCommunicationInstitutionalAccessCheckResponse autorizarCaixaInstitucional(NationalCommunicationInstitutionalAccessCheckRequest request) {
        var result = autorizacaoCaixaInstitucionalService.autorizar(request.unidadeCodigo(), request.caixaCodigo(), request.capacidade());
        return new NationalCommunicationInstitutionalAccessCheckResponse(
                result.autorizado(),
                result.unidadeCodigo(),
                result.caixaCodigo(),
                result.capacidadeSolicitada().name(),
                result.justificativas(),
                result.vinculosElegiveis().stream().map(this::toMembershipResponse).toList()
        );
    }

    public List<NationalCommunicationInstitutionalInboxItemResponse> listarInboxInstitucional(StatusComunicacaoInstitucional status,
                                                                                       Long processoId) {
        return institutionalInboxApplicationService.listarMinhasCaixas(status, processoId).stream()
                .map(this::toInboxResponse)
                .toList();
    }

    public NationalCommunicationInstitutionalActionResponse receberInboxInstitucional(NationalCommunicationInstitutionalReceiveRequest request) {
        Objects.requireNonNull(request);
        return institutionalCommunicationConcurrencyGuardService.execute(
                "receber-inbox",
                request.expedicaoUuid(),
                () -> toActionResponse(institutionalInboxApplicationService.receber(request.expedicaoUuid(), request.detalhe()))
        );
    }

    public NationalCommunicationInstitutionalActionResponse redistribuirInboxInstitucional(NationalCommunicationInstitutionalRedistributeRequest request) {
        Objects.requireNonNull(request);
        return institutionalCommunicationConcurrencyGuardService.execute(
                "redistribuir-inbox",
                request.expedicaoUuid(),
                () -> toActionResponse(institutionalInboxApplicationService.redistribuir(request.expedicaoUuid(), request.caixaDestinoCodigo(), request.detalhe()))
        );
    }

    public NationalCommunicationInstitutionalActionResponse certificarCienciaInstitucional(NationalCommunicationInstitutionalScienceRequest request) {
        Objects.requireNonNull(request);
        return institutionalCommunicationConcurrencyGuardService.execute(
                "certificar-ciencia",
                request.expedicaoUuid(),
                () -> toActionResponse(institutionalInboxApplicationService.certificarCiencia(request.expedicaoUuid(), request.detalhe()))
        );
    }

    public NationalCommunicationInstitutionalActionResponse cumprirInboxInstitucional(NationalCommunicationInstitutionalFulfillRequest request) {
        Objects.requireNonNull(request);
        return institutionalCommunicationConcurrencyGuardService.execute(
                "cumprir-inbox",
                request.expedicaoUuid(),
                () -> toActionResponse(institutionalInboxApplicationService.cumprir(request.expedicaoUuid(), request.detalhe()))
        );
    }

    public List<NationalCommunicationInstitutionalTimelineEventResponse> timelineInstitucional(String expedicaoUuid) {
        institutionalInboxApplicationService.loadVisible(expedicaoUuid);
        return institutionalCommunicationAuditApplicationService.timeline(expedicaoUuid).stream()
                .map(this::toTimelineResponse)
                .toList();
    }

    public List<NationalCommunicationInstitutionalDeliveryProofResponse> provasInstitucionais(String expedicaoUuid) {
        institutionalInboxApplicationService.loadVisible(expedicaoUuid);
        return institutionalCommunicationAuditApplicationService.provas(expedicaoUuid).stream()
                .map(this::toProofResponse)
                .toList();
    }

    public List<NationalCommunicationInstitutionalGateStateResponse> gatesInstitucionais(Long processoId,
                                                                                  String expedicaoUuid) {
        if (expedicaoUuid != null && !expedicaoUuid.isBlank()) {
            institutionalInboxApplicationService.loadVisible(expedicaoUuid);
            return institutionalCommunicationGateApplicationService.consultarPorExpedicao(expedicaoUuid).stream()
                    .map(this::toGateResponse)
                    .toList();
        }
        if (processoId == null) {
            return List.of();
        }
        requireReadProcesso(processoId);
        return institutionalCommunicationGateApplicationService.consultarPorProcesso(processoId).stream()
                .map(this::toGateResponse)
                .toList();
    }

    public List<NationalCommunicationInstitutionalDeliveryQueueItemResponse> listarEntregasInstitucionais(Long processoId,
                                                                                                   String expedicaoUuid) {
        if (!ensureInboxOrProcessScope(processoId, expedicaoUuid)) {
            return List.of();
        }
        return institutionalDeliveryQueueApplicationService.listarEntregas(processoId, expedicaoUuid).stream()
                .map(this::toDeliveryQueueResponse)
                .toList();
    }

    public List<NationalCommunicationInstitutionalDeadLetterResponse> listarDlqInstitucional(Long processoId,
                                                                                       String expedicaoUuid) {
        if (!ensureInboxOrProcessScope(processoId, expedicaoUuid)) {
            return List.of();
        }
        return institutionalDeliveryQueueApplicationService.listarDlq(processoId, expedicaoUuid).stream()
                .map(this::toDeadLetterResponse)
                .toList();
    }

    public NationalCommunicationInstitutionalDeliveryQueueItemResponse reprocessarEntregaInstitucional(NationalCommunicationInstitutionalReprocessDeliveryRequest request) {
        Objects.requireNonNull(request);
        InstitutionalDeliveryJob current = institutionalDeliveryQueueApplicationService.consultarJob(request.jobId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("InstitutionalDeliveryJob", request.jobId()));
        institutionalInboxApplicationService.loadVisible(current.expedicaoUuid());
        return institutionalCommunicationConcurrencyGuardService.execute(
                "reprocessar-entrega",
                request.jobId(),
                () -> toDeliveryQueueResponse(institutionalDeliveryQueueApplicationService.reprocessar(request.jobId(), request.detalhe()))
        );
    }

    public List<NationalCommunicationInstitutionalExternalDispatchResponse> listarIntegracoesExternas(Long processoId,
                                                                                               String expedicaoUuid) {
        if (!ensureInboxOrProcessScope(processoId, expedicaoUuid)) {
            return List.of();
        }
        return institutionalCommunicationObservabilityApplicationService.listarIntegracoesExternas(processoId, expedicaoUuid).stream()
                .map(this::toExternalDispatchResponse)
                .toList();
    }

    public NationalCommunicationInstitutionalObservabilityDashboardResponse observabilidadeInstitucional(Long processoId,
                                                                                                  String uf,
                                                                                                  DestinatarioInstitucionalKind destinatarioKind) {
        if (processoId != null) {
            requireReadProcesso(processoId);
        }
        InstitutionalObservabilityDashboard dashboard = institutionalCommunicationObservabilityApplicationService.dashboard(processoId, uf, destinatarioKind);
        return toObservabilityResponse(dashboard);
    }

    public NationalCommunicationInstitutionalHardeningReportResponse hardeningInstitucional() {
        InstitutionalCommunicationHardeningReport report = institutionalCommunicationHardeningApplicationService.gerarRelatorio();
        return new NationalCommunicationInstitutionalHardeningReportResponse(
                report.aprovado(),
                report.totalUnidades(),
                report.totalUnidadesAtivas(),
                report.totalInboxPendentes(),
                report.totalGatesBloqueando(),
                report.totalDlq(),
                report.totalIntegracoesExternasComFalha(),
                report.totalEntregasEmAberto(),
                report.canaisExternosCobertos(),
                report.findings().stream()
                        .map(finding -> new NationalCommunicationInstitutionalHardeningFindingResponse(
                                finding.code(),
                                finding.severity().name(),
                                finding.message(),
                                finding.evidencias()))
                        .toList(),
                report.geradoEm(),
                report.hashIntegridade()
        );
    }

    private Processo requireReadProcesso(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return processo;
    }

    private boolean ensureInboxOrProcessScope(Long processoId, String expedicaoUuid) {
        if (expedicaoUuid != null && !expedicaoUuid.isBlank()) {
            institutionalInboxApplicationService.loadVisible(expedicaoUuid);
            return true;
        }
        if (processoId == null) {
            return false;
        }
        requireReadProcesso(processoId);
        return true;
    }

    private NationalCommunicationInstitutionalMembershipResponse toMembershipResponse(VinculoUsuarioCaixaInstitucional vinculo) {
        return new NationalCommunicationInstitutionalMembershipResponse(
                vinculo.unidade().codigo(),
                vinculo.unidade().nomeOficial(),
                vinculo.unidade().destinatarioKind() != null ? vinculo.unidade().destinatarioKind().name() : null,
                vinculo.unidade().uf(),
                vinculo.unidade().comarca(),
                vinculo.caixa().codigo(),
                vinculo.caixa().nomeExibicao(),
                vinculo.caixa().tipo() != null ? vinculo.caixa().tipo().name() : null,
                vinculo.funcaoOperacional() != null ? vinculo.funcaoOperacional().name() : null,
                vinculo.abrangencia() != null ? vinculo.abrangencia().name() : null,
                vinculo.capacidades().stream().map(Enum::name).sorted().toList(),
                vinculo.justificativa()
        );
    }

    private NationalCommunicationInstitutionalInboxItemResponse toInboxResponse(InstitutionalInboxItem item) {
        InstitutionalRequestAccessContextFacadeService.InstitutionalAccessDigest digest = accessContextFacadeService.digest();
        return new NationalCommunicationInstitutionalInboxItemResponse(
                item.inboxItemId(),
                item.expedicaoUuid(),
                item.processoId(),
                item.processoNumero(),
                item.unidadeCodigo(),
                item.unidadeSigla(),
                item.destinatarioKind() != null ? item.destinatarioKind().name() : null,
                item.papelProcessual() != null ? item.papelProcessual().name() : null,
                item.tipoComunicacao() != null ? item.tipoComunicacao().name() : null,
                item.caixaCodigoOrigem(),
                item.caixaCodigoAtual(),
                item.canalPrincipal(),
                item.status() != null ? item.status().name() : null,
                item.gateCode(),
                item.bloqueiaFluxo(),
                item.atribuidoUsuarioId(),
                item.ultimoOperadorUsuarioId(),
                digest.horizontalDataPlaneKey(),
                digest.rlsScopeKey(),
                digest.coverageMode(),
                digest.readOnly(),
                digest.requiresStepUp(),
                digest.requiresQualifiedCertificate(),
                item.disponibilizadaEm(),
                item.recebidaEm(),
                item.cientificadaEm(),
                item.cumpridaEm(),
                item.prazoCienciaEm(),
                item.prazoRespostaEm(),
                item.updatedAt(),
                item.justificativas()
        );
    }

    private NationalCommunicationInstitutionalActionResponse toActionResponse(InstitutionalInboxActionResult result) {
        return new NationalCommunicationInstitutionalActionResponse(
                result.expedicaoUuid(),
                result.unidadeCodigo(),
                result.caixaCodigo(),
                result.status() != null ? result.status().name() : null,
                result.gateStatus() != null ? result.gateStatus().name() : null,
                result.gateBloqueado(),
                result.justificativas(),
                result.hashIntegridade()
        );
    }

    private NationalCommunicationInstitutionalTimelineEventResponse toTimelineResponse(InstitutionalTimelineEvent event) {
        return new NationalCommunicationInstitutionalTimelineEventResponse(
                event.eventId(),
                event.expedicaoUuid(),
                event.processoId(),
                event.processoNumero(),
                event.eventType() != null ? event.eventType().name() : null,
                event.statusComunicacao() != null ? event.statusComunicacao().name() : null,
                event.unidadeCodigo(),
                event.caixaCodigo(),
                event.actorUserId(),
                event.actorTipoUsuario() != null ? event.actorTipoUsuario().name() : null,
                event.resumo(),
                event.detalhes(),
                event.occurredAt(),
                event.hashIntegridade()
        );
    }

    private NationalCommunicationInstitutionalDeliveryProofResponse toProofResponse(InstitutionalDeliveryProof proof) {
        return new NationalCommunicationInstitutionalDeliveryProofResponse(
                proof.proofId(),
                proof.expedicaoUuid(),
                proof.processoId(),
                proof.etapa(),
                proof.canal(),
                proof.actorUserId(),
                proof.actorTipoUsuario() != null ? proof.actorTipoUsuario().name() : null,
                proof.evidenciaTipo(),
                proof.evidencia(),
                proof.createdAt(),
                proof.hashIntegridade()
        );
    }

    private NationalCommunicationInstitutionalGateStateResponse toGateResponse(InstitutionalGateState state) {
        return new NationalCommunicationInstitutionalGateStateResponse(
                state.gateStateId(),
                state.expedicaoUuid(),
                state.processoId(),
                state.processoNumero(),
                state.gateCode(),
                state.status() != null ? state.status().name() : null,
                state.bloqueado(),
                state.motivo(),
                state.ultimaProvaTipo(),
                state.createdAt(),
                state.updatedAt(),
                state.releasedAt(),
                state.justificativas(),
                state.hashIntegridade()
        );
    }

    private NationalCommunicationInstitutionalDeliveryQueueItemResponse toDeliveryQueueResponse(InstitutionalDeliveryJob job) {
        return new NationalCommunicationInstitutionalDeliveryQueueItemResponse(
                job.jobId(),
                job.expedicaoUuid(),
                job.processoId(),
                job.processoNumero(),
                job.unidadeCodigo(),
                job.caixaCodigo(),
                job.destinatarioKind() != null ? job.destinatarioKind().name() : null,
                job.papelProcessual() != null ? job.papelProcessual().name() : null,
                job.currentChannel() != null ? job.currentChannel().name() : null,
                job.status() != null ? job.status().name() : null,
                job.attemptCount(),
                job.maxAttempts(),
                job.nextAttemptAt(),
                job.lastAttemptAt(),
                job.terminalAt(),
                job.providerReference(),
                job.lastFailureReason() != null ? job.lastFailureReason().name() : null,
                job.lastError(),
                job.justificativas(),
                job.hashIntegridade()
        );
    }

    private NationalCommunicationInstitutionalDeadLetterResponse toDeadLetterResponse(InstitutionalDeadLetterEntry entry) {
        return new NationalCommunicationInstitutionalDeadLetterResponse(
                entry.entryId(),
                entry.jobId(),
                entry.expedicaoUuid(),
                entry.processoId(),
                entry.processoNumero(),
                entry.unidadeCodigo(),
                entry.caixaCodigo(),
                entry.channel() != null ? entry.channel().name() : null,
                entry.reason() != null ? entry.reason().name() : null,
                entry.attempts(),
                entry.detail(),
                entry.justificativas(),
                entry.createdAt(),
                entry.hashIntegridade()
        );
    }

    private NationalCommunicationInstitutionalExternalDispatchResponse toExternalDispatchResponse(InstitutionalExternalDispatch dispatch) {
        return new NationalCommunicationInstitutionalExternalDispatchResponse(
                dispatch.dispatchId(),
                dispatch.jobId(),
                dispatch.expedicaoUuid(),
                dispatch.processoId(),
                dispatch.processoNumero(),
                dispatch.unidadeCodigo(),
                dispatch.caixaCodigo(),
                dispatch.destinatarioKind().name(),
                dispatch.papelProcessual().name(),
                dispatch.channel().name(),
                dispatch.provider(),
                dispatch.status().name(),
                dispatch.providerReference(),
                dispatch.payloadHash(),
                dispatch.failureReason(),
                dispatch.createdAt(),
                dispatch.updatedAt()
        );
    }

    private NationalCommunicationInstitutionalObservabilityDashboardResponse toObservabilityResponse(InstitutionalObservabilityDashboard dashboard) {
        return new NationalCommunicationInstitutionalObservabilityDashboardResponse(
                dashboard.totalEntregas(),
                dashboard.totalPendentes(),
                dashboard.totalDlq(),
                dashboard.totalIntegracoesExternas(),
                dashboard.totalIntegracoesAceitas(),
                dashboard.totalIntegracoesFalha(),
                dashboard.totalGatesBloqueando(),
                dashboard.totalInboxPendentes(),
                dashboard.totalSlaRisco(),
                dashboard.porCanal().stream().map(bucket -> new NationalCommunicationInstitutionalObservabilityBucketResponse(bucket.key(), bucket.count())).toList(),
                dashboard.porStatusEntrega().stream().map(bucket -> new NationalCommunicationInstitutionalObservabilityBucketResponse(bucket.key(), bucket.count())).toList(),
                dashboard.porDestinatario().stream().map(bucket -> new NationalCommunicationInstitutionalObservabilityBucketResponse(bucket.key(), bucket.count())).toList(),
                dashboard.geradoEm()
        );
    }

    private String firstNonBlank(String... values) {
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

}