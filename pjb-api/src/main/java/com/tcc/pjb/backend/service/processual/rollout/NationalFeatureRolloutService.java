package com.tcc.pjb.backend.service.processual.rollout;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.configs.PjbFeatureFlagsProperties;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorPolicyOverlay;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorPolicyService;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.dto.processual.rollout.NationalFeatureRolloutRequest;
import com.tcc.pjb.backend.model.dto.processual.rollout.NationalFeatureRolloutResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class NationalFeatureRolloutService {

    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final JudicialConnectorPolicyService policyService;
    private final PjbFeatureFlagsProperties featureFlagsProperties;
    private final CurrentUserService currentUserService;

    public NationalFeatureRolloutService(ProcessoRepository processoRepository,
                                         PjbAuthorizationService authorizationService,
                                         JudicialConnectorPolicyService policyService,
                                         PjbFeatureFlagsProperties featureFlagsProperties,
                                         CurrentUserService currentUserService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.policyService = Objects.requireNonNull(policyService);
        this.featureFlagsProperties = Objects.requireNonNull(featureFlagsProperties);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    public NationalFeatureRolloutResponse resolve(NationalFeatureRolloutRequest request) {
        Objects.requireNonNull(request);
        Processo processo = request.processoId() == null ? null : processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        if (processo != null) {
            authorizationService.requireReadProcesso(processo);
        }
        Usuario usuario = currentUserService.getOrNull();
        String featureCode = normalizeToken(request.featureCode());
        String perfilAlvo = resolvePerfil(request.targetProfile(), usuario);
        String tribunalCodigo = firstNonBlank(normalizeToken(request.tribunalCodigo()), processo != null ? normalizeToken(processo.getTribunal()) : null);
        JudicialSystem system = request.judicialSystem() != null ? request.judicialSystem() : parseSystem(processo != null ? processo.getConnectorSystem() : null);
        JudicialConnectorPolicyOverlay overlay = system != null || tribunalCodigo != null
                ? policyService.resolve(system != null ? system : JudicialSystem.OUTRO, tribunalCodigo)
                : JudicialConnectorPolicyOverlay.none(null, "default", tribunalCodigo);
        boolean baseEnabled = resolveBaseEnabled(featureCode, overlay);
        String rolloutMode = resolveRolloutMode(baseEnabled, overlay);
        int threshold = resolveThreshold(rolloutMode, request.rolloutPercentOverride());
        String anchor = resolveAnchor(featureCode, processo, tribunalCodigo, usuario);
        int bucket = hashBucket(anchor);
        boolean enabled = evaluateEnabled(baseEnabled, rolloutMode, threshold, bucket, perfilAlvo, overlay);
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Feature avaliada com âncora determinística para rollout reproduzível.");
        fundamentos.add("Política de connector e toggle local foram consolidados na mesma decisão.");
        if (isCriticalProfile(perfilAlvo)) {
            fundamentos.add("Perfil crítico recebe prioridade em modos INTERNAL e PILOT.");
        }
        List<String> warnings = new ArrayList<>(overlay.warnings());
        if (Boolean.TRUE.equals(overlay.maintenanceMode())) {
            warnings.add("CONNECTOR_POLICY_MAINTENANCE_MODE");
        }
        if (Boolean.TRUE.equals(overlay.quarantineEnabled())) {
            warnings.add("CONNECTOR_POLICY_QUARANTINED");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("baseEnabled", baseEnabled);
        metadata.put("rolloutState", overlay.rolloutState());
        metadata.put("policyPresent", overlay.policyPresent());
        metadata.put("judicialSystem", system != null ? system.name() : null);
        metadata.put("uf", firstNonBlank(request.uf(), processo != null ? processo.getUf() : null, usuario != null ? usuario.getUf() : null));
        metadata.put("comarca", firstNonBlank(request.comarca(), processo != null ? processo.getComarca() : null, usuario != null ? usuario.getComarca() : null));
        metadata.put("tribunalCodigo", tribunalCodigo);
        metadata.put("hashBucket", bucket);
        metadata.put("thresholdPercent", threshold);
        metadata.put("anchor", anchor);
        metadata.values().removeIf(Objects::isNull);
        return new NationalFeatureRolloutResponse(
                featureCode,
                enabled,
                rolloutMode,
                threshold,
                bucket,
                tribunalCodigo,
                perfilAlvo,
                anchor,
                fundamentos,
                List.copyOf(new java.util.LinkedHashSet<>(warnings)),
                Collections.unmodifiableMap(metadata)
        );
    }

    private boolean evaluateEnabled(boolean baseEnabled,
                                    String rolloutMode,
                                    int threshold,
                                    int bucket,
                                    String perfilAlvo,
                                    JudicialConnectorPolicyOverlay overlay) {
        if (!baseEnabled) {
            return false;
        }
        if (Boolean.TRUE.equals(overlay.tribunalBlocked()) || Boolean.TRUE.equals(overlay.quarantineEnabled()) || Boolean.TRUE.equals(overlay.maintenanceMode())) {
            return false;
        }
        return switch (rolloutMode) {
            case "FULL" -> true;
            case "CANARY" -> bucket < threshold;
            case "PILOT" -> isCriticalProfile(perfilAlvo) || bucket < threshold;
            case "INTERNAL" -> isCriticalProfile(perfilAlvo);
            case "OFF", "DISABLED", "BLOCKED" -> false;
            default -> bucket < threshold;
        };
    }

    private boolean resolveBaseEnabled(String featureCode, JudicialConnectorPolicyOverlay overlay) {
        return switch (featureCode) {
            case "KAFKA" -> featureFlagsProperties.getKafka().isEnabled();
            case "WORKFLOW" -> featureFlagsProperties.getWorkflow().isEnabled();
            case "SEARCH" -> featureFlagsProperties.getSearch().isEnabled();
            case "GOV_VITAL_MONITOR", "VITAL_MONITOR" -> featureFlagsProperties.getGov().getVitalMonitor().isEnabled();
            default -> overlay.policyPresent();
        };
    }

    private String resolveRolloutMode(boolean baseEnabled, JudicialConnectorPolicyOverlay overlay) {
        String raw = normalizeToken(overlay.rolloutState());
        if (raw != null) {
            return raw;
        }
        return baseEnabled ? "FULL" : "OFF";
    }

    private int resolveThreshold(String rolloutMode, Integer override) {
        if (override != null) {
            return Math.max(0, Math.min(100, override));
        }
        return switch (rolloutMode) {
            case "FULL" -> 100;
            case "PILOT" -> 25;
            case "CANARY" -> 10;
            case "INTERNAL" -> 5;
            default -> 0;
        };
    }

    private String resolvePerfil(String targetProfile, Usuario usuario) {
        if (targetProfile != null && !targetProfile.isBlank()) {
            return normalizeToken(targetProfile);
        }
        return usuario != null && usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "ANONIMO";
    }

    private String resolveAnchor(String featureCode, Processo processo, String tribunalCodigo, Usuario usuario) {
        return firstNonBlank(
                processo != null ? processo.getNumeroProcesso() : null,
                tribunalCodigo,
                usuario != null && usuario.getId() != null ? String.valueOf(usuario.getId()) : null,
                featureCode
        );
    }

    private int hashBucket(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int hash = 0;
        for (byte b : bytes) {
            hash = 31 * hash + b;
        }
        return Math.floorMod(hash, 100);
    }

    private JudicialSystem parseSystem(String value) {
        String normalized = normalizeToken(value);
        if (normalized == null) {
            return null;
        }
        try {
            return JudicialSystem.valueOf(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isCriticalProfile(String rawPerfil) {
        TipoUsuario tipoUsuario = TipoUsuario.fromPerfil(rawPerfil);
        return tipoUsuario != null && (tipoUsuario.isPerfilCritico() || tipoUsuario.isAdmin() || tipoUsuario.isMagistratura() || tipoUsuario.isServidorJudiciario());
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
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
