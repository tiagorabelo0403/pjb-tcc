package com.tcc.pjb.backend.service.security.breakglass;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.profile.operational.BreakGlassAcessoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceFieldResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.security.BreakGlassAccessSession;
import com.tcc.pjb.backend.model.repository.BreakGlassAccessSessionRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.outbox.FederatedOutboxDispatchService;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatProcessoVisibilidadePessoalService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalBreakGlassAccessService {

    private final ProcessoRepository processoRepository;
    private final BreakGlassAccessSessionRepository sessionRepository;
    private final CurrentUserService currentUserService;
    private final SecretariatProcessoVisibilidadePessoalService visibilidadePessoalService;
    private final FederatedOutboxDispatchService federatedOutboxDispatchService;
    private final AuditLedgerService auditLedgerService;
    private final ObjectMapper objectMapper;

    public InstitutionalBreakGlassAccessService(ProcessoRepository processoRepository,
                                                BreakGlassAccessSessionRepository sessionRepository,
                                                CurrentUserService currentUserService,
                                                SecretariatProcessoVisibilidadePessoalService visibilidadePessoalService,
                                                FederatedOutboxDispatchService federatedOutboxDispatchService,
                                                AuditLedgerService auditLedgerService,
                                                ObjectMapper objectMapper) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.visibilidadePessoalService = Objects.requireNonNull(visibilidadePessoalService);
        this.federatedOutboxDispatchService = Objects.requireNonNull(federatedOutboxDispatchService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional(readOnly = true)
    public SurfaceSnapshotResponse snapshot(Long processoId) {
        Processo processo = resolveProcesso(processoId);
        List<BreakGlassAccessSession> sessions = sessionRepository.findTop50ByProcessoIdOrderByCreatedAtDesc(processoId);
        ArrayList<SurfaceFieldResponse> fields = new ArrayList<>();
        fields.add(new SurfaceFieldResponse("processoId", processo.getId()));
        fields.add(new SurfaceFieldResponse("nupn", resolveNupn(processo)));
        fields.add(new SurfaceFieldResponse("statusProcesso", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null));
        fields.add(new SurfaceFieldResponse("breakGlassAtivo", sessions.stream().anyMatch(this::ativa)));
        fields.add(new SurfaceFieldResponse("sessoesRecentes", sessions.stream().map(this::toMap).toList()));
        return new SurfaceSnapshotResponse("BREAK_GLASS_PROCESSO", List.copyOf(fields));
    }

    @Transactional
    public SurfaceActionResponse activate(Long processoId, BreakGlassAcessoRequest request) {
        Objects.requireNonNull(request);
        Processo processo = resolveProcesso(processoId);
        Usuario usuario = currentUserService.getRequired();
        Instant now = Instant.now();
        boolean stepUpRequired = requiresStepUp(processo, usuario);
        boolean stepUpSatisfied = Boolean.TRUE.equals(request.stepUpSatisfeito());
        String status = stepUpRequired && !stepUpSatisfied ? "PENDING_STEP_UP" : "ACTIVE";
        String correlationId = UUID.randomUUID().toString();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("processoId", processo.getId());
        metadata.put("nupn", resolveNupn(processo));
        metadata.put("escopoAcesso", normalizeScope(request.escopoAcesso()));
        metadata.put("statusProcesso", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        metadata.put("sigilo", processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : null);
        metadata.put("stepUpRequired", stepUpRequired);
        metadata.put("stepUpSatisfied", stepUpSatisfied);
        metadata.put("requestedByPerfil", resolvePerfil(usuario));
        metadata.put("riskLevel", resolveRiskLevel(processo));
        BreakGlassAccessSession session = BreakGlassAccessSession.builder()
                .processoId(processo.getId())
                .nupn(resolveNupn(processo))
                .requestedByUsuarioId(usuario.getId())
                .requestedByProfile(resolvePerfil(usuario))
                .accessScope(normalizeScope(request.escopoAcesso()))
                .justification(normalizeText(request.justificativa(), 1000, true))
                .approvalBasis(normalizeText(request.fundamentoAprovacao(), 240, false))
                .riskLevel(resolveRiskLevel(processo))
                .status(status)
                .stepUpRequired(stepUpRequired)
                .stepUpSatisfied(stepUpSatisfied)
                .expiresAt(resolveExpiry(request.horasValidade(), now))
                .correlationId(correlationId)
                .auditHash(Hashes.sha256Hex(resolveNupn(processo) + '|' + usuario.getId() + '|' + request.justificativa() + '|' + status + '|' + now))
                .metadataJson(serialize(metadata))
                .build();
        BreakGlassAccessSession persisted = sessionRepository.save(session);
        if ("ACTIVE".equals(status)) {
            int validityDays = resolveValidityDays(request.horasValidade());
            visibilidadePessoalService.definir(processoId, true, "BREAK_GLASS: " + normalizeText(request.justificativa(), 460, true), validityDays);
        }
        federatedOutboxDispatchService.dispatch(
                "break-glass-processo",
                "BREAK_GLASS_ACCESS_" + status,
                "PJB",
                processo.getTribunalCodigoRoteado(),
                "PROCESSO",
                String.valueOf(processo.getId()),
                correlationId,
                1L,
                Map.of(
                        "nupn", resolveNupn(processo),
                        "requestedByUsuarioId", usuario.getId(),
                        "requestedByPerfil", resolvePerfil(usuario),
                        "riskLevel", resolveRiskLevel(processo)
                ),
                toMap(persisted)
        );
        auditLedgerService.appendSafely("BREAK_GLASS_ACCESS_" + status, "PROCESSO", String.valueOf(processo.getId()), persisted.getAuditHash(), correlationId);
        List<SurfaceFieldResponse> fields = new ArrayList<>();
        fields.add(new SurfaceFieldResponse("sessionId", persisted.getId()));
        fields.add(new SurfaceFieldResponse("processoId", processo.getId()));
        fields.add(new SurfaceFieldResponse("nupn", resolveNupn(processo)));
        fields.add(new SurfaceFieldResponse("status", persisted.getStatus()));
        fields.add(new SurfaceFieldResponse("stepUpRequired", persisted.isStepUpRequired()));
        fields.add(new SurfaceFieldResponse("stepUpSatisfied", persisted.isStepUpSatisfied()));
        fields.add(new SurfaceFieldResponse("expiresAt", persisted.getExpiresAt()));
        fields.add(new SurfaceFieldResponse("correlationId", persisted.getCorrelationId()));
        fields.add(new SurfaceFieldResponse("riskLevel", persisted.getRiskLevel()));
        return new SurfaceActionResponse("BREAK_GLASS_PROCESSO", "ATIVAR_BREAK_GLASS", processo.getId(), persisted.getStatus(), List.copyOf(fields));
    }

    private Processo resolveProcesso(Long processoId) {
        if (processoId == null) {
            throw new IllegalArgumentException("processoId obrigatório");
        }
        return processoRepository.findContextoCompletoById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado para break-glass institucional"));
    }

    private boolean requiresStepUp(Processo processo, Usuario usuario) {
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            return true;
        }
        RamoDireito ramo = processo.getRamoDireito();
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE || ramo == RamoDireito.MILITAR) {
            return true;
        }
        return usuario.isMagistrado() || usuario.isServidorJudiciario();
    }

    private String resolveRiskLevel(Processo processo) {
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            return "CRITICAL";
        }
        RamoDireito ramo = processo.getRamoDireito();
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE || ramo == RamoDireito.MILITAR) {
            return "HIGH";
        }
        return "CONTROLLED";
    }

    private boolean ativa(BreakGlassAccessSession session) {
        if (session == null || !"ACTIVE".equals(session.getStatus())) {
            return false;
        }
        return session.getExpiresAt() == null || session.getExpiresAt().isAfter(Instant.now());
    }

    private int resolveValidityDays(Integer hours) {
        int safeHours = hours == null || hours <= 0 ? 24 : Math.min(168, hours);
        return Math.max(1, (int) Math.ceil(safeHours / 24.0d));
    }

    private Instant resolveExpiry(Integer hours, Instant now) {
        int safeHours = hours == null || hours <= 0 ? 24 : Math.min(168, hours);
        return now.plus(safeHours, ChronoUnit.HOURS);
    }

    private String resolvePerfil(Usuario usuario) {
        if (usuario.getTipoUsuario() != null) {
            return usuario.getTipoUsuario().name();
        }
        if (usuario.getPerfil() != null && !usuario.getPerfil().isBlank()) {
            return usuario.getPerfil().trim().toUpperCase(Locale.ROOT);
        }
        return "OPERADOR";
    }

    private String resolveNupn(Processo processo) {
        String value = processo.getNumeroUnificado();
        if (value == null || value.isBlank()) {
            value = processo.getNumeroProcesso();
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Processo sem número apto para break-glass institucional");
        }
        return value.trim();
    }

    private String normalizeScope(String value) {
        return normalizeText(value, 60, true).toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private String normalizeText(String value, int max, boolean required) {
        String effective = value == null ? "" : value.trim();
        if (required && effective.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório para break-glass institucional");
        }
        if (effective.isBlank()) {
            return null;
        }
        return effective.length() <= max ? effective : effective.substring(0, max);
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException ex) {
            return String.valueOf(payload);
        }
    }

    private Map<String, Object> toMap(BreakGlassAccessSession session) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("id", session.getId());
        out.put("processoId", session.getProcessoId());
        out.put("nupn", session.getNupn());
        out.put("requestedByUsuarioId", session.getRequestedByUsuarioId());
        out.put("requestedByProfile", session.getRequestedByProfile());
        out.put("accessScope", session.getAccessScope());
        out.put("riskLevel", session.getRiskLevel());
        out.put("status", session.getStatus());
        out.put("stepUpRequired", session.isStepUpRequired());
        out.put("stepUpSatisfied", session.isStepUpSatisfied());
        out.put("expiresAt", session.getExpiresAt());
        out.put("correlationId", session.getCorrelationId());
        out.put("createdAt", session.getCreatedAt());
        out.put("updatedAt", session.getUpdatedAt());
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
