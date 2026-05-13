package com.tcc.pjb.backend.service.offline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.access.PrivateResourceAccessGuardService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.offline.PwaOfflineBundle;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.PwaOfflineBundleRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.offline.domain.BundleView;
import com.tcc.pjb.backend.service.offline.domain.ConflictResolution;
import com.tcc.pjb.backend.service.offline.domain.CriarBundleRequest;
import com.tcc.pjb.backend.service.offline.domain.OfflineActionHealthView;
import com.tcc.pjb.backend.service.offline.domain.OfflineActionView;
import com.tcc.pjb.backend.service.offline.domain.OfflineActionWindowView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleActionQuery;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleActionResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleAuditQuery;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleAuditResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleConflictView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleConsistencyHealthQuery;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleConsistencyHealthResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleConsistencyQuery;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleConsistencyResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleConsistencyView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleDecisionHealthQuery;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleDecisionHealthResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleEnvelopeView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleExpiryQuery;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleExpiryResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleExpiryView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleGovernanceStatusView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleHealthQuery;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleHealthResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleManifestSnapshot;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleManifestView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleMetricsView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleOwnerView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleOwnershipQuery;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleOwnershipResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleOwnershipView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleQuery;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleQueryResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleReplaySnapshot;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleReplayView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleSignalView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleStatusSnapshot;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleSyncSnapshot;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleTimelineEntry;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleTimelineHealthView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleWindowAuditView;
import com.tcc.pjb.backend.service.offline.domain.OfflineBundleWindowView;
import com.tcc.pjb.backend.service.offline.domain.OfflineConflictTimelineEntry;
import com.tcc.pjb.backend.service.offline.domain.OfflineConflictTimelineResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineGovernanceAuditView;
import com.tcc.pjb.backend.service.offline.domain.OfflineManifestAuditView;
import com.tcc.pjb.backend.service.offline.domain.OfflineReplayAuditView;
import com.tcc.pjb.backend.service.offline.domain.OfflineReplayHealthView;
import com.tcc.pjb.backend.service.offline.domain.OfflineReplayQuery;
import com.tcc.pjb.backend.service.offline.domain.OfflineReplayResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineSyncDecisionView;
import com.tcc.pjb.backend.service.offline.domain.OfflineSyncQuery;
import com.tcc.pjb.backend.service.offline.domain.OfflineSyncResult;
import com.tcc.pjb.backend.service.offline.domain.OfflineSyncWindowView;
import com.tcc.pjb.backend.service.offline.domain.SincronizarBundleRequest;
import com.tcc.pjb.backend.service.offline.domain.SyncGovernance;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PwaOfflineService {

    private static final long DEFAULT_TTL_SECONDS = 60L * 60L * 24L * 3L;

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final PwaOfflineBundleRepository bundleRepository;
    private final PrivateResourceAccessGuardService accessGuard;
    private final ObjectMapper objectMapper;
    private final OfflineBundleGovernanceService offlineBundleGovernanceService;
    private final OfflineConflictResolver offlineConflictResolver;

    public PwaOfflineService(ProcessoRepository processoRepository,
                             DocumentoProcessualRepository documentoRepository,
                             MovimentacaoProcessualRepository movimentacaoRepository,
                             PwaOfflineBundleRepository bundleRepository,
                             PrivateResourceAccessGuardService accessGuard,
                             ObjectMapper objectMapper,
                             OfflineBundleGovernanceService offlineBundleGovernanceService,
                             OfflineConflictResolver offlineConflictResolver) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.bundleRepository = Objects.requireNonNull(bundleRepository);
        this.accessGuard = Objects.requireNonNull(accessGuard);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.offlineBundleGovernanceService = Objects.requireNonNull(offlineBundleGovernanceService);
        this.offlineConflictResolver = Objects.requireNonNull(offlineConflictResolver);
    }

    @Transactional
    public BundleView criarBundle(CriarBundleRequest request) {
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        Usuario usuario = accessGuard.requireCurrentUser();
        accessGuard.requireReadProcesso(processo);
        List<DocumentoProcessual> documentos = documentoRepository.findTop18ByProcesso_IdOrderByCriadoEmDesc(processo.getId());
        List<MovimentacaoProcessual> movimentacoes = movimentacaoRepository.findTop60ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId());
        Instant now = Instant.now();
        LinkedHashMap<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("processoId", processo.getId());
        manifest.put("numeroProcesso", processo.getNumeroProcesso());
        manifest.put("tribunal", processo.getTribunal());
        manifest.put("ramoDireito", processo.getRamoDireito() == null ? null : processo.getRamoDireito().name());
        manifest.put("sigilo", processo.getNivelSigilo() == null ? null : processo.getNivelSigilo().name());
        manifest.put("escopo", defaultText(request.escopo(), "PROCESSO_TIMELINE"));
        manifest.put("documentos", documentos.stream().map(this::documentoResumo).toList());
        manifest.put("movimentacoes", movimentacoes.stream().map(this::movimentacaoResumo).toList());
        manifest.put("geradoEm", now.toString());
        manifest.put("expiraEm", now.plusSeconds(DEFAULT_TTL_SECONDS).toString());
        manifest.put("deviceFingerprint", request.deviceFingerprint());
        manifest.put("offlineCapability", offlineBundleGovernanceService.buildOfflineCapability(
                processo,
                usuario,
                defaultText(request.escopo(), "PROCESSO_TIMELINE"),
                documentos,
                movimentacoes
        ));
        String manifestJson = writeJson(manifest);

        PwaOfflineBundle bundle = new PwaOfflineBundle();
        bundle.setBundleToken("PWA-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        bundle.setProcesso(processo);
        bundle.setSolicitante(usuario);
        bundle.setDeviceFingerprint(request.deviceFingerprint());
        bundle.setEscopo(defaultText(request.escopo(), "PROCESSO_TIMELINE"));
        bundle.setStatus("ABERTO");
        bundle.setManifestJson(manifestJson);
        bundle.setManifestHash(Hashes.sha256Hex(manifestJson));
        bundle.setAbertoEm(now);
        bundle.setExpiraEm(now.plusSeconds(DEFAULT_TTL_SECONDS));
        return toView(bundleRepository.save(bundle));
    }

    @Transactional(readOnly = true)
    public List<BundleView> listarBundlesRecentes() {
        long userId = accessGuard.requireCurrentUser().getId();
        return bundleRepository.findTop50BySolicitante_IdOrderByCreatedAtDesc(userId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public BundleView detalhar(String bundleToken) {
        PwaOfflineBundle bundle = find(bundleToken);
        ensureBundleAccess(bundle);
        ensureNotExpired(bundle);
        return toView(bundle);
    }

    @Transactional
    public BundleView sincronizar(String bundleToken, SincronizarBundleRequest request) {
        PwaOfflineBundle bundle = find(bundleToken);
        ensureBundleAccess(bundle);
        ensureNotExpired(bundle);
        SyncGovernance governance = offlineBundleGovernanceService.governSync(bundle, bundle.getProcesso(), request);
        LinkedHashMap<String, Object> replay = new LinkedHashMap<>();
        replay.put("acoes", request.acoes() == null ? List.of() : request.acoes());
        replay.put("deviceClock", request.deviceClock());
        replay.put("ultimaSincronizacaoConhecida", request.ultimaSincronizacaoConhecida());
        replay.put("syncGovernance", governance.envelope());
        if ("PENDENTE_CONFLITO".equals(governance.status())) {
            ConflictResolution resolution = offlineConflictResolver.resolve(bundle, request.acoes());
            if (resolution.requiresReview()) {
                replay.put("aplicadoEm", Instant.now().toString());
                replay.put("conflictResolution", conflictEnvelope(resolution));
                bundle.setReplayAcoesJson(writeJson(replay));
                bundle.setConflitoResumo(resolution.summary());
                bundle.setSincronizadoEm(Instant.now());
                bundle.setStatus("PENDENTE_REVISAO_CONFLITO");
                return toView(bundleRepository.save(bundle));
            }
            replay.put("conflictResolution", conflictEnvelope(resolution));
        }
        replay.put("aplicadoEm", Instant.now().toString());
        bundle.setReplayAcoesJson(writeJson(replay));
        bundle.setConflitoResumo(governance.conflictSummary());
        bundle.setSincronizadoEm(Instant.now());
        bundle.setStatus(governance.status());
        return toView(bundleRepository.save(bundle));
    }

    @Transactional(readOnly = true)
    public OfflineBundleQueryResult consultar(OfflineBundleQuery query) {
        Objects.requireNonNull(query);
        PwaOfflineBundle bundle = find(query.bundleToken());
        ensureBundleAccess(bundle);
        return new OfflineBundleQueryResult(
                bundle.getId(),
                bundle.getBundleToken(),
                bundle.getProcesso() == null ? null : bundle.getProcesso().getId(),
                bundle.getEscopo(),
                bundle.getStatus(),
                bundle.getAbertoEm(),
                bundle.getSincronizadoEm(),
                bundle.getExpiraEm(),
                bundle.getConflitoResumo()
        );
    }

    @Transactional(readOnly = true)
    public OfflineBundleManifestSnapshot manifestSnapshot(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleManifestSnapshot(bundle.getManifestHash(), bundle.getManifestJson());
    }

    @Transactional(readOnly = true)
    public OfflineBundleReplaySnapshot replaySnapshot(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleReplaySnapshot(bundle.getStatus(), bundle.getReplayAcoesJson(), bundle.getConflitoResumo());
    }

    @Transactional(readOnly = true)
    public OfflineBundleSyncSnapshot syncSnapshot(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleSyncSnapshot(bundle.getStatus(), bundle.getSincronizadoEm(), bundle.getConflitoResumo());
    }

    @Transactional(readOnly = true)
    public List<OfflineBundleTimelineEntry> timeline(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        List<OfflineBundleTimelineEntry> entries = new ArrayList<>();
        if (bundle.getAbertoEm() != null) {
            entries.add(new OfflineBundleTimelineEntry("ABERTO", bundle.getAbertoEm(), bundle.getEscopo()));
        }
        if (bundle.getSincronizadoEm() != null) {
            entries.add(new OfflineBundleTimelineEntry("SINCRONIZADO", bundle.getSincronizadoEm(), bundle.getStatus()));
        }
        if (bundle.getExpiraEm() != null) {
            entries.add(new OfflineBundleTimelineEntry("EXPIRA", bundle.getExpiraEm(), bundle.getManifestHash()));
        }
        return List.copyOf(entries);
    }

    @Transactional(readOnly = true)
    public OfflineBundleAuditResult audit(OfflineBundleAuditQuery query) {
        Objects.requireNonNull(query);
        return new OfflineBundleAuditResult(
                manifestSnapshot(query.bundleToken()),
                replaySnapshot(query.bundleToken()),
                syncSnapshot(query.bundleToken())
        );
    }

    @Transactional(readOnly = true)
    public OfflineBundleHealthResult health(OfflineBundleHealthQuery query) {
        Objects.requireNonNull(query);
        PwaOfflineBundle bundle = requireAccessible(query.bundleToken());
        boolean expirado = isExpired(bundle, Instant.now());
        boolean sincronizado = bundle.getSincronizadoEm() != null;
        return new OfflineBundleHealthResult(bundle.getBundleToken(), bundle.getStatus(), bundle.getExpiraEm(), expirado, sincronizado);
    }

    @Transactional(readOnly = true)
    public OfflineBundleManifestView manifestView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleManifestView(bundle.getBundleToken(), bundle.getManifestHash(), bundle.getEscopo(), bundle.getStatus());
    }

    @Transactional(readOnly = true)
    public OfflineBundleStatusSnapshot statusSnapshot(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleStatusSnapshot(bundle.getId(), bundle.getStatus(), bundle.getAbertoEm(), bundle.getSincronizadoEm());
    }

    @Transactional(readOnly = true)
    public OfflineBundleConflictView conflictView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleConflictView(bundle.getId(), bundle.getStatus(), bundle.getConflitoResumo(), isConflictPending(bundle));
    }

    @Transactional(readOnly = true)
    public OfflineBundleReplayView replayView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleReplayView(bundle.getId(), bundle.getReplayAcoesJson(), bundle.getConflitoResumo());
    }

    @Transactional(readOnly = true)
    public OfflineBundleActionResult actions(OfflineBundleActionQuery query) {
        Objects.requireNonNull(query);
        PwaOfflineBundle bundle = requireAccessible(query.bundleToken());
        List<OfflineActionView> actions = parseActions(bundle).stream()
                .map(this::actionView)
                .toList();
        return new OfflineBundleActionResult(bundle.getId(), actions);
    }

    @Transactional(readOnly = true)
    public OfflineBundleOwnershipResult ownership(OfflineBundleOwnershipQuery query) {
        Objects.requireNonNull(query);
        PwaOfflineBundle bundle = requireAccessible(query.bundleToken());
        Long solicitanteId = query.solicitanteId();
        Long ownerId = bundle.getSolicitante() == null ? null : bundle.getSolicitante().getId();
        return new OfflineBundleOwnershipResult(bundle.getBundleToken(), ownerId, Objects.equals(ownerId, solicitanteId), bundle.getEscopo());
    }

    @Transactional(readOnly = true)
    public OfflineBundleOwnershipView ownershipView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleOwnershipView(
                bundle.getId(),
                bundle.getProcesso() == null ? null : bundle.getProcesso().getId(),
                bundle.getSolicitante() == null ? null : bundle.getSolicitante().getId(),
                bundle.getDeviceFingerprint()
        );
    }

    @Transactional(readOnly = true)
    public OfflineBundleExpiryResult expiry(OfflineBundleExpiryQuery query) {
        Objects.requireNonNull(query);
        PwaOfflineBundle bundle = requireAccessible(query.bundleToken());
        Instant reference = query.referenceTime() == null ? Instant.now() : query.referenceTime();
        long remainingSeconds = bundle.getExpiraEm() == null ? 0L : Math.max(0L, Duration.between(reference, bundle.getExpiraEm()).getSeconds());
        return new OfflineBundleExpiryResult(bundle.getBundleToken(), bundle.getExpiraEm(), isExpired(bundle, reference), remainingSeconds);
    }

    @Transactional(readOnly = true)
    public OfflineBundleExpiryView expiryView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleExpiryView(bundle.getId(), bundle.getExpiraEm(), isExpired(bundle, Instant.now()));
    }

    @Transactional(readOnly = true)
    public OfflineBundleConsistencyResult consistency(OfflineBundleConsistencyQuery query) {
        Objects.requireNonNull(query);
        PwaOfflineBundle bundle = requireAccessible(query.referencia());
        boolean consistent = isManifestConsistent(bundle);
        return new OfflineBundleConsistencyResult(consistent, consistent ? "manifesto consistente" : "manifesto divergente do hash persistido", Instant.now());
    }

    @Transactional(readOnly = true)
    public OfflineBundleConsistencyView consistencyView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        boolean consistent = isManifestConsistent(bundle);
        return new OfflineBundleConsistencyView(
                bundle.getBundleToken(),
                bundle.getStatus(),
                consistent,
                Instant.now(),
                consistent ? "manifesto íntegro" : "manifesto fora de integridade"
        );
    }

    @Transactional(readOnly = true)
    public OfflineBundleConsistencyHealthResult consistencyHealth(OfflineBundleConsistencyHealthQuery query) {
        Objects.requireNonNull(query);
        PwaOfflineBundle bundle = requireAccessible(query.reference());
        boolean consistent = isManifestConsistent(bundle);
        return new OfflineBundleConsistencyHealthResult(true, consistent ? "manifesto íntegro" : "manifesto divergente", (long) parseActions(bundle).size());
    }

    @Transactional(readOnly = true)
    public OfflineBundleDecisionHealthResult decisionHealth(OfflineBundleDecisionHealthQuery query) {
        Objects.requireNonNull(query);
        PwaOfflineBundle bundle = requireAccessible(query.reference());
        boolean conflict = isConflictPending(bundle);
        return new OfflineBundleDecisionHealthResult(true, conflict ? "bundle exige revisão humana" : "bundle apto para replay", (long) parseActions(bundle).size());
    }

    @Transactional(readOnly = true)
    public OfflineReplayResult replay(OfflineReplayQuery query) {
        Objects.requireNonNull(query);
        PwaOfflineBundle bundle = requireAccessible(query.bundleToken());
        Map<String, Object> replay = parseReplay(bundle);
        Map<String, Object> conflictResolution = asMap(replay.get("conflictResolution"));
        boolean requiresReview = Boolean.TRUE.equals(conflictResolution.get("requiresReview")) || isConflictPending(bundle);
        boolean replaySafe = !requiresReview;
        String summary = Objects.toString(conflictResolution.getOrDefault("summary", defaultText(bundle.getConflitoResumo(), replaySafe ? "replay seguro" : "replay pendente de revisão")), "");
        return new OfflineReplayResult(bundle.getBundleToken(), replaySafe, requiresReview, summary);
    }

    @Transactional(readOnly = true)
    public OfflineSyncResult sync(OfflineSyncQuery query) {
        Objects.requireNonNull(query);
        PwaOfflineBundle bundle = requireAccessible(query.bundleToken());
        return new OfflineSyncResult(bundle.getBundleToken(), bundle.getStatus(), bundle.getConflitoResumo());
    }

    @Transactional(readOnly = true)
    public OfflineConflictTimelineResult conflictTimeline(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        List<OfflineConflictTimelineEntry> entries = new ArrayList<>();
        if (bundle.getAbertoEm() != null) {
            entries.add(new OfflineConflictTimelineEntry("BUNDLE_ABERTO", bundle.getAbertoEm(), bundle.getEscopo()));
        }
        if (bundle.getSincronizadoEm() != null) {
            entries.add(new OfflineConflictTimelineEntry("GOVERNANCA_AVALIADA", bundle.getSincronizadoEm(), bundle.getStatus()));
        }
        if (bundle.getConflitoResumo() != null && !bundle.getConflitoResumo().isBlank()) {
            entries.add(new OfflineConflictTimelineEntry(
                    isConflictPending(bundle) ? "CONFLITO_PENDENTE_REVISAO" : "CONFLITO_REGISTRADO",
                    bundle.getUpdatedAt() == null ? Instant.now() : bundle.getUpdatedAt(),
                    bundle.getConflitoResumo()
            ));
        }
        return new OfflineConflictTimelineResult(bundle.getBundleToken(), List.copyOf(entries));
    }

    @Transactional(readOnly = true)
    public OfflineBundleGovernanceStatusView governanceStatusView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleGovernanceStatusView(bundle.getBundleToken(), bundle.getStatus(), defaultText(bundle.getConflitoResumo(), bundle.getEscopo()));
    }

    @Transactional(readOnly = true)
    public OfflineBundleMetricsView metricsView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleMetricsView(
                bundle.getId(),
                parseActions(bundle).size(),
                bundle.getConflitoResumo() == null || bundle.getConflitoResumo().isBlank() ? 0 : 1,
                bundle.getUpdatedAt() == null ? bundle.getCreatedAt() : bundle.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public OfflineBundleEnvelopeView envelopeView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleEnvelopeView(
                bundle.getBundleToken(),
                bundle.getStatus(),
                bundle.getManifestHash(),
                bundle.getCreatedAt() == null ? Instant.now() : bundle.getCreatedAt(),
                bundle.getId()
        );
    }

    @Transactional(readOnly = true)
    public OfflineBundleOwnerView ownerView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        Long ownerId = bundle.getSolicitante() == null ? null : bundle.getSolicitante().getId();
        return new OfflineBundleOwnerView(
                bundle.getBundleToken(),
                ownerId == null ? "SEM_DONO" : "OWNER_BOUND",
                ownerId == null ? "bundle sem solicitante persistido" : "solicitante=" + ownerId,
                bundle.getCreatedAt() == null ? Instant.now() : bundle.getCreatedAt(),
                ownerId
        );
    }

    @Transactional(readOnly = true)
    public OfflineBundleSignalView signalView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleSignalView(
                bundle.getBundleToken(),
                isConflictPending(bundle) ? "PENDENTE_REVISAO_CONFLITO" : bundle.getStatus(),
                defaultText(bundle.getConflitoResumo(), bundle.getEscopo()),
                bundle.getUpdatedAt() == null ? Instant.now() : bundle.getUpdatedAt(),
                bundle.getId()
        );
    }

    @Transactional(readOnly = true)
    public OfflineBundleWindowView windowView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleWindowView(
                bundle.getBundleToken(),
                isExpired(bundle, Instant.now()) ? "EXPIRADO" : "ATIVO",
                buildWindowDetail(bundle),
                bundle.getExpiraEm() == null ? Instant.now() : bundle.getExpiraEm(),
                bundle.getId()
        );
    }

    @Transactional(readOnly = true)
    public OfflineSyncDecisionView syncDecisionView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineSyncDecisionView(
                bundle.getBundleToken(),
                bundle.getStatus(),
                isConflictPending(bundle),
                isConflictPending(bundle) ? "REVIEW_BEFORE_APPLY" : "APPLY_OR_REPLAY"
        );
    }

    @Transactional(readOnly = true)
    public OfflineReplayHealthView replayHealthView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineReplayHealthView(bundle.getBundleToken(), isConflictPending(bundle) ? "REVIEW_REQUIRED" : "REPLAY_READY", bundle.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public OfflineSyncWindowView syncWindowView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineSyncWindowView(bundle.getBundleToken(), bundle.getStatus(), bundle.getSincronizadoEm());
    }

    @Transactional(readOnly = true)
    public OfflineActionHealthView actionHealthView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineActionHealthView(bundle.getBundleToken(), bundle.getStatus(), "acoes=" + parseActions(bundle).size());
    }

    @Transactional(readOnly = true)
    public OfflineActionWindowView actionWindowView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineActionWindowView(bundle.getBundleToken(), bundle.getStatus(), buildWindowDetail(bundle));
    }

    @Transactional(readOnly = true)
    public OfflineManifestAuditView manifestAuditView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineManifestAuditView(bundle.getBundleToken(), isManifestConsistent(bundle) ? "OK" : "DRIFT", bundle.getManifestHash());
    }

    @Transactional(readOnly = true)
    public OfflineReplayAuditView replayAuditView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineReplayAuditView(bundle.getBundleToken(), bundle.getStatus(), bundle.getSincronizadoEm(), defaultText(bundle.getConflitoResumo(), "replay registrado"));
    }

    @Transactional(readOnly = true)
    public OfflineGovernanceAuditView governanceAuditView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineGovernanceAuditView(bundle.getBundleToken(), bundle.getStatus(), bundle.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public OfflineBundleTimelineHealthView timelineHealthView(String bundleToken) {
        PwaOfflineBundle bundle = requireAccessible(bundleToken);
        return new OfflineBundleTimelineHealthView(bundle.getBundleToken(), bundle.getStatus(), "eventos=" + timeline(bundleToken).size());
    }

    private Map<String, Object> documentoResumo(DocumentoProcessual documento) {
        LinkedHashMap<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("id", documento.getId());
        resumo.put("titulo", defaultText(documento.getTitulo(), documento.getNomeOriginal()));
        resumo.put("sha256", documento.getSha256());
        resumo.put("sigilo", documento.getNivelSigilo() == null ? null : documento.getNivelSigilo().name());
        resumo.put("categoria", documento.getCategoria() == null ? null : documento.getCategoria().name());
        resumo.put("criadoEm", documento.getCriadoEm() == null ? null : documento.getCriadoEm().toString());
        return resumo;
    }

    private Map<String, Object> movimentacaoResumo(MovimentacaoProcessual movimentacao) {
        LinkedHashMap<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("id", movimentacao.getId());
        resumo.put("faseDe", movimentacao.getFaseDe() == null ? null : movimentacao.getFaseDe().name());
        resumo.put("fasePara", movimentacao.getFasePara() == null ? null : movimentacao.getFasePara().name());
        resumo.put("descricao", movimentacao.getDescricao());
        resumo.put("data", movimentacao.getDataMovimentacao() == null ? null : movimentacao.getDataMovimentacao().toString());
        return resumo;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar bundle offline.", e);
        }
    }

    private BundleView toView(PwaOfflineBundle bundle) {
        Map<String, Object> manifest = offlineBundleGovernanceService.parseManifest(bundle.getManifestJson());
        return new BundleView(
                bundle.getId(),
                bundle.getBundleToken(),
                bundle.getProcesso() == null ? null : bundle.getProcesso().getId(),
                bundle.getProcesso() == null ? null : bundle.getProcesso().getNumeroProcesso(),
                bundle.getStatus(),
                bundle.getEscopo(),
                bundle.getDeviceFingerprint(),
                bundle.getManifestHash(),
                bundle.getAbertoEm(),
                bundle.getExpiraEm(),
                bundle.getSincronizadoEm(),
                bundle.getConflitoResumo(),
                manifest
        );
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private PwaOfflineBundle requireAccessible(String bundleToken) {
        PwaOfflineBundle bundle = find(bundleToken);
        ensureBundleAccess(bundle);
        return bundle;
    }

    private void ensureBundleAccess(PwaOfflineBundle bundle) {
        accessGuard.requireOwnerOrPrivileged(
                bundle.getSolicitante() == null ? null : bundle.getSolicitante().getId(),
                bundle.getProcesso(),
                "bundle offline"
        );
    }

    private void ensureNotExpired(PwaOfflineBundle bundle) {
        if (isExpired(bundle, Instant.now())) {
            throw new IllegalStateException("Bundle offline expirado. Gere um novo bundle antes de sincronizar.");
        }
    }

    private boolean isExpired(PwaOfflineBundle bundle, Instant referenceTime) {
        return bundle.getExpiraEm() != null && referenceTime != null && referenceTime.isAfter(bundle.getExpiraEm());
    }

    private PwaOfflineBundle find(String bundleToken) {
        return bundleRepository.findByBundleToken(bundleToken)
                .orElseThrow(() -> new IllegalArgumentException("Bundle offline não localizado."));
    }

    private boolean isConflictPending(PwaOfflineBundle bundle) {
        return "PENDENTE_REVISAO_CONFLITO".equalsIgnoreCase(bundle.getStatus())
                || (bundle.getConflitoResumo() != null && !bundle.getConflitoResumo().isBlank() && "PENDENTE_CONFLITO".equalsIgnoreCase(bundle.getStatus()));
    }

    private boolean isManifestConsistent(PwaOfflineBundle bundle) {
        if (bundle.getManifestJson() == null || bundle.getManifestJson().isBlank()) {
            return false;
        }
        return Objects.equals(bundle.getManifestHash(), Hashes.sha256Hex(bundle.getManifestJson()));
    }

    private List<Map<String, Object>> parseActions(PwaOfflineBundle bundle) {
        Map<String, Object> replay = parseReplay(bundle);
        Object value = replay.get("acoes");
        if (value instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
                    map.forEach((key, entry) -> normalized.put(String.valueOf(key), entry));
                    out.add(Map.copyOf(normalized));
                }
            }
            return List.copyOf(out);
        }
        return List.of();
    }

    private Map<String, Object> parseReplay(PwaOfflineBundle bundle) {
        return offlineBundleGovernanceService.parseManifest(bundle.getReplayAcoesJson());
    }

    private OfflineActionView actionView(Map<String, Object> action) {
        int index = parseInteger(action.get("index"), 0);
        String tipo = firstText(action.get("tipo"), action.get("type"), action.get("acao"), "SYNC_ACTION");
        boolean decisoria = isDecisionAction(tipo);
        String resumo = firstText(action.get("resumo"), action.get("summary"), action.get("descricao"), tipo);
        return new OfflineActionView(index, tipo, decisoria, resumo);
    }

    private boolean isDecisionAction(String tipo) {
        String normalized = tipo == null ? "" : tipo.trim().toUpperCase();
        return normalized.startsWith("DECISAO")
                || normalized.startsWith("SENTENCA")
                || normalized.startsWith("ASSINATURA")
                || normalized.startsWith("DESPACHO");
    }

    private int parseInteger(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String firstText(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private String buildWindowDetail(PwaOfflineBundle bundle) {
        if (bundle.getExpiraEm() == null) {
            return "sem expiração definida";
        }
        long remaining = Math.max(0L, Duration.between(Instant.now(), bundle.getExpiraEm()).toHours());
        return "restanteHoras=" + remaining;
    }

    private Map<String, Object> conflictEnvelope(ConflictResolution resolution) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("safe", resolution.safe());
        out.put("requiresReview", resolution.requiresReview());
        out.put("acoesCount", resolution.acoesCount());
        out.put("onlineMovsCount", resolution.onlineMovsCount());
        out.put("summary", resolution.summary());
        return Collections.unmodifiableMap(out);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, item) -> out.put(String.valueOf(key), item));
            return Collections.unmodifiableMap(out);
        }
        return Map.of();
    }
}
