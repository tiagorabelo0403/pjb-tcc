package com.tcc.pjb.backend.service.infra.scaling;

import com.tcc.pjb.backend.core.forum.routing.SecretariatInboxKeyParser;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver.SecretariatSpecializationProfile;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialScaleProfileResolver {

    public static final String HEADER_SCALE_PROFILE = "X-PJB-Scale-Profile";
    public static final String HEADER_INSTANCIA = "X-PJB-Instancia";
    public static final String HEADER_RAMO = "X-PJB-Ramo";
    public static final String HEADER_SECRETARIAT_CLASS = "X-PJB-Secretariat-Class";

    public JudicialScaleProfile resolveProfile(String instanciaAxis,
                                               String ramoAxis,
                                               SecretariatSpecializationProfile specialization) {
        JudicialScaleProfile explicit = specialization == null ? null : JudicialScaleProfile.fromToken(readSpecializationToken(specialization, "judicialScaleProfile"));
        if (explicit != null) {
            return explicit;
        }
        String instanceClass = specialization == null ? resolveInstanceClass(instanciaAxis, null) : specialization.secretariatInstanceClass();
        String branchClass = specialization == null ? resolveBranchClass(ramoAxis, null) : specialization.secretariatBranchClass();
        return resolveBaseProfile(instanceClass, branchClass);
    }

    public JudicialScalePolicy resolvePolicy(String instanciaAxis,
                                             String ramoAxis,
                                             SecretariatSpecializationProfile specialization) {
        String instanceClass = specialization == null ? resolveInstanceClass(instanciaAxis, null) : normalizeInstanceClass(specialization.secretariatInstanceClass(), instanciaAxis);
        String branchClass = specialization == null ? resolveBranchClass(ramoAxis, null) : normalizeBranchClass(specialization.secretariatBranchClass(), ramoAxis);
        JudicialScaleProfile profile = resolveProfile(instanciaAxis, ramoAxis, specialization);
        return buildPolicy(profile, instanceClass, branchClass, "ROUTING");
    }

    public JudicialScalePolicy resolvePolicy(HttpServletRequest request) {
        String explicitProfile = header(request, HEADER_SCALE_PROFILE);
        JudicialScaleProfile profile = JudicialScaleProfile.fromToken(explicitProfile);
        String path = request == null ? null : request.getRequestURI();
        String secretariatClass = header(request, HEADER_SECRETARIAT_CLASS);
        String instanceClass = resolveInstanceClass(header(request, HEADER_INSTANCIA), secretariatClass == null ? path : secretariatClass + '|' + path);
        String branchClass = resolveBranchClass(header(request, HEADER_RAMO), join(secretariatClass, path));
        if (profile == null) {
            profile = resolveBaseProfile(instanceClass, branchClass);
        }
        return buildPolicy(profile, instanceClass, branchClass, "HTTP");
    }

    public JudicialScalePolicy resolvePolicyFromInbox(String inboxKey, String jobType) {
        String fallback = join(inboxKey, jobType);
        String instanceAxis = null;
        String ramoAxis = null;
        SecretariatInboxKeyParser.Parts parts = SecretariatInboxKeyParser.parse(inboxKey).orElse(null);
        if (parts != null) {
            instanceAxis = parts.instance();
            ramoAxis = join(parts.org(), parts.lane(), parts.jurisdicao());
        }
        String instanceClass = resolveInstanceClass(instanceAxis, fallback);
        String branchClass = resolveBranchClass(ramoAxis, fallback);
        JudicialScaleProfile profile = resolveBaseProfile(instanceClass, branchClass);
        return buildPolicy(profile, instanceClass, branchClass, "JOB");
    }

    public List<JudicialScalePolicy> defaultMatrix() {
        return List.of(
                buildPolicy(JudicialScaleProfile.VARA_1G, "PRIMEIRA_INSTANCIA", "ESTADUAL", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.VARA_1G, "PRIMEIRA_INSTANCIA", "PENAL", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.VARA_1G, "PRIMEIRA_INSTANCIA", "FEDERAL", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.VARA_1G, "PRIMEIRA_INSTANCIA", "TRABALHISTA", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.VARA_1G, "PRIMEIRA_INSTANCIA", "ELEITORAL", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.VARA_1G, "PRIMEIRA_INSTANCIA", "MILITAR", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.TURMA_RECURSAL, "SEGUNDA_INSTANCIA", "JUIZADO_ESPECIAL", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.SECRETARIA_TRIBUNAL, "SEGUNDA_INSTANCIA", "ESTADUAL", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.SECRETARIA_TRIBUNAL, "SEGUNDA_INSTANCIA", "FEDERAL", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.SECRETARIA_TRIBUNAL, "SEGUNDA_INSTANCIA", "TRABALHISTA", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.SECRETARIA_TRIBUNAL, "SEGUNDA_INSTANCIA", "ELEITORAL", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.SECRETARIA_TRIBUNAL, "SEGUNDA_INSTANCIA", "MILITAR", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.SECRETARIA_TRIBUNAL_SUPERIOR, "INSTANCIA_SUPERIOR", "FEDERAL", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.SECRETARIA_TRIBUNAL_SUPERIOR, "INSTANCIA_SUPERIOR", "TRABALHISTA", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.SECRETARIA_TRIBUNAL_SUPERIOR, "INSTANCIA_SUPERIOR", "ELEITORAL", "CATALOGO"),
                buildPolicy(JudicialScaleProfile.SECRETARIA_TRIBUNAL_SUPERIOR, "INSTANCIA_SUPERIOR", "MILITAR", "CATALOGO")
        );
    }

    public JudicialScalePolicy buildPolicy(JudicialScaleProfile profile,
                                           String instanceClass,
                                           String branchClass,
                                           String source) {
        Objects.requireNonNull(profile, "profile");
        String effectiveInstanceClass = normalizeInstanceClass(instanceClass, profile.instanceClass());
        String effectiveBranchClass = normalizeBranchClass(branchClass, "ESTADUAL");
        BranchModifier modifier = modifierForBranch(effectiveBranchClass);
        double queueParallelismFactor = clamp(profile.queueParallelismFactor() * modifier.queueParallelismFactor(), 0.55d, 1.90d);
        double queueBudgetFactor = clamp(profile.queueBudgetFactor() * modifier.queueBudgetFactor(), 0.70d, 1.80d);
        double replicaLagFactor = clamp(profile.replicaLagFactor() * modifier.replicaLagFactor(), 0.55d, 1.50d);
        double degradedReplicaLagFactor = clamp(replicaLagFactor * modifier.degradedReplicaLagExtraFactor(), 0.60d, 1.80d);
        double readPressureFactor = clamp(profile.readPressureFactor() * modifier.readPressureFactor(), 0.75d, 1.35d);
        double rateLimitFactor = clamp(profile.rateLimitFactor() * modifier.rateLimitFactor(), 0.65d, 1.85d);
        boolean cacheHotPreferred = profile.cacheHotPreferred() || modifier.cacheHotPreferred();
        boolean searchPreferred = profile.searchIndexPreferred() || modifier.searchPreferred();
        boolean asyncWritePreferred = profile.asyncWritePreferred() || modifier.asyncWritePreferred();
        String descriptor = profile.name() + ':' + effectiveInstanceClass + ':' + effectiveBranchClass;
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", blankToNull(source));
        metadata.put("profileDisplayName", profile.displayName());
        metadata.put("modifierBranch", effectiveBranchClass);
        metadata.put("descriptor", descriptor);
        metadata.put("cacheBias", cacheHotPreferred ? "HOT_CACHE" : "STRICT_READ");
        metadata.put("searchBias", searchPreferred ? "SEARCH_BACKED" : "DB_BACKED");
        metadata.put("asyncBias", asyncWritePreferred ? "OUTBOX_QUEUE" : "INLINE_MUTATION");
        return new JudicialScalePolicy(
                profile,
                profile.displayName(),
                effectiveInstanceClass,
                effectiveBranchClass,
                queueParallelismFactor,
                queueBudgetFactor,
                replicaLagFactor,
                degradedReplicaLagFactor,
                readPressureFactor,
                rateLimitFactor,
                cacheHotPreferred,
                searchPreferred,
                asyncWritePreferred,
                descriptor,
                Collections.unmodifiableMap(metadata)
        );
    }

    public Duration scaleDuration(Duration base, double factor) {
        if (base == null) {
            return null;
        }
        long millis = Math.max(1L, Math.round(base.toMillis() * clamp(factor, 0.25d, 4d)));
        return Duration.ofMillis(millis);
    }

    private JudicialScaleProfile resolveBaseProfile(String instanceClass, String branchClass) {
        String normalizedInstance = normalizeInstanceClass(instanceClass, null);
        String normalizedBranch = normalizeBranchClass(branchClass, null);
        if ("INSTANCIA_SUPERIOR".equals(normalizedInstance)) {
            return JudicialScaleProfile.SECRETARIA_TRIBUNAL_SUPERIOR;
        }
        if ("JUIZADO_ESPECIAL".equals(normalizedBranch)) {
            return JudicialScaleProfile.TURMA_RECURSAL;
        }
        if ("SEGUNDA_INSTANCIA".equals(normalizedInstance)) {
            return JudicialScaleProfile.SECRETARIA_TRIBUNAL;
        }
        return JudicialScaleProfile.VARA_1G;
    }

    private String resolveInstanceClass(String instanciaAxis, String fallbackToken) {
        String token = normalizeToken(join(instanciaAxis, fallbackToken));
        if (containsAny(token, "SUPERIOR", "TRIBUNAL_SUPERIOR", "MINISTRO", "PLENARIO", "SECAO", "TURMA_STJ", "TURMA_STF")) {
            return "INSTANCIA_SUPERIOR";
        }
        if (containsAny(token, "TURMA_RECURSAL", "RECURSAL", "SEGUNDO_GRAU", "SEGUNDA_INSTANCIA", "DESEMBARGADOR", "CAMARA", "COLEGIADO", "ACORDAO", "TRIBUNAL")) {
            return "SEGUNDA_INSTANCIA";
        }
        return "PRIMEIRA_INSTANCIA";
    }

    private String resolveBranchClass(String ramoAxis, String fallbackToken) {
        String token = normalizeToken(join(ramoAxis, fallbackToken));
        if (containsAny(token, "JUIZADO", "TURMA_RECURSAL", "JEC", "JEF", "JEFZ")) {
            return "JUIZADO_ESPECIAL";
        }
        if (containsAny(token, "PENAL", "CRIMINAL", "INQUERITO", "EXECUCAO_PENAL", "TRIBUNAL_JURI")) {
            return "PENAL";
        }
        if (containsAny(token, "ELEITORAL", "TRE", "TSE", "ZONA_ELEITORAL")) {
            return "ELEITORAL";
        }
        if (containsAny(token, "TRABALHISTA", "TRABALHO", "TRT", "TST")) {
            return "TRABALHISTA";
        }
        if (containsAny(token, "MILITAR", "STM", "TJM", "AUDITORIA_MILITAR")) {
            return "MILITAR";
        }
        if (containsAny(token, "FEDERAL", "TRF", "JF", "SECAO_JUDICIARIA", "SUBSECAO", "STJ")) {
            return "FEDERAL";
        }
        return "ESTADUAL";
    }

    private BranchModifier modifierForBranch(String branchClass) {
        return switch (normalizeBranchClass(branchClass, "ESTADUAL")) {
            case "PENAL" -> new BranchModifier(1.06d, 1.10d, 0.76d, 1.05d, 0.90d, false, false, false);
            case "FEDERAL" -> new BranchModifier(1.10d, 1.08d, 0.96d, 1.08d, 1.10d, false, true, true);
            case "TRABALHISTA" -> new BranchModifier(1.16d, 1.06d, 1.02d, 1.08d, 1.14d, true, true, true);
            case "ELEITORAL" -> new BranchModifier(1.22d, 1.14d, 0.90d, 1.12d, 1.24d, true, true, true);
            case "MILITAR" -> new BranchModifier(1.02d, 1.10d, 0.82d, 0.94d, 0.86d, false, false, false);
            case "JUIZADO_ESPECIAL" -> new BranchModifier(1.08d, 0.94d, 0.92d, 1.10d, 1.06d, true, false, true);
            default -> new BranchModifier(1.00d, 1.00d, 1.00d, 1.00d, 1.00d, false, false, false);
        };
    }

    private String readSpecializationToken(SecretariatSpecializationProfile specialization, String key) {
        if (specialization == null || specialization.metadata() == null) {
            return null;
        }
        Object value = specialization.metadata().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeInstanceClass(String value, String fallback) {
        String token = normalizeToken(firstNonBlank(value, fallback, "PRIMEIRA_INSTANCIA"));
        if (containsAny(token, "INSTANCIA_SUPERIOR", "TRIBUNAL_SUPERIOR", "SUPERIOR")) {
            return "INSTANCIA_SUPERIOR";
        }
        if (containsAny(token, "SEGUNDA_INSTANCIA", "SEGUNDO_GRAU", "TURMA_RECURSAL", "RECURSAL", "2G")) {
            return "SEGUNDA_INSTANCIA";
        }
        return "PRIMEIRA_INSTANCIA";
    }

    private String normalizeBranchClass(String value, String fallback) {
        String token = normalizeToken(firstNonBlank(value, fallback, "ESTADUAL"));
        if (containsAny(token, "JUIZADO_ESPECIAL", "TURMA_RECURSAL", "JUIZADO", "JEC", "JEF")) {
            return "JUIZADO_ESPECIAL";
        }
        if (containsAny(token, "PENAL", "CRIMINAL")) {
            return "PENAL";
        }
        if (containsAny(token, "ELEITORAL")) {
            return "ELEITORAL";
        }
        if (containsAny(token, "TRABALHISTA", "TRABALHO")) {
            return "TRABALHISTA";
        }
        if (containsAny(token, "MILITAR")) {
            return "MILITAR";
        }
        if (containsAny(token, "FEDERAL")) {
            return "FEDERAL";
        }
        return "ESTADUAL";
    }

    private boolean containsAny(String token, String... options) {
        if (token == null || token.isBlank()) {
            return false;
        }
        for (String option : options) {
            if (option != null && !option.isBlank() && token.contains(normalizeToken(option))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    private String header(HttpServletRequest request, String name) {
        if (request == null || name == null || name.isBlank()) {
            return null;
        }
        String value = request.getHeader(name);
        return blankToNull(value);
    }

    private String join(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized == null) {
                continue;
            }
            if (out.length() > 0) {
                out.append('|');
            }
            out.append(normalized);
        }
        return out.isEmpty() ? null : out.toString();
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record BranchModifier(double queueParallelismFactor,
                                  double queueBudgetFactor,
                                  double replicaLagFactor,
                                  double readPressureFactor,
                                  double rateLimitFactor,
                                  boolean cacheHotPreferred,
                                  boolean searchPreferred,
                                  boolean asyncWritePreferred) {
        private double degradedReplicaLagExtraFactor() {
            if (replicaLagFactor < 1d) {
                return 1.12d;
            }
            return 1.18d;
        }
    }

    public record JudicialScalePolicy(JudicialScaleProfile profile,
                                      String displayName,
                                      String instanceClass,
                                      String branchClass,
                                      double queueParallelismFactor,
                                      double queueBudgetFactor,
                                      double replicaLagFactor,
                                      double degradedReplicaLagFactor,
                                      double readPressureFactor,
                                      double rateLimitFactor,
                                      boolean cacheHotPreferred,
                                      boolean searchPreferred,
                                      boolean asyncWritePreferred,
                                      String descriptor,
                                      Map<String, Object> metadata) {

        public JudicialScalePolicy {
            Objects.requireNonNull(profile, "profile");
            displayName = displayName == null || displayName.isBlank() ? profile.displayName() : displayName;
            instanceClass = instanceClass == null || instanceClass.isBlank() ? profile.instanceClass() : instanceClass;
            branchClass = branchClass == null || branchClass.isBlank() ? "ESTADUAL" : branchClass;
            descriptor = descriptor == null || descriptor.isBlank() ? profile.name() + ':' + instanceClass + ':' + branchClass : descriptor;
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
        }

        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("profile", profile.name());
            out.put("displayName", displayName);
            out.put("instanceClass", instanceClass);
            out.put("branchClass", branchClass);
            out.put("queueParallelismFactor", queueParallelismFactor);
            out.put("queueBudgetFactor", queueBudgetFactor);
            out.put("replicaLagFactor", replicaLagFactor);
            out.put("degradedReplicaLagFactor", degradedReplicaLagFactor);
            out.put("readPressureFactor", readPressureFactor);
            out.put("rateLimitFactor", rateLimitFactor);
            out.put("cacheHotPreferred", cacheHotPreferred);
            out.put("searchPreferred", searchPreferred);
            out.put("asyncWritePreferred", asyncWritePreferred);
            out.put("descriptor", descriptor);
            out.put("metadata", metadata);
            return Collections.unmodifiableMap(out);
        }
    }
}
