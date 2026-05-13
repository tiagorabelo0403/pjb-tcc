package com.tcc.pjb.backend.service.infra.scaling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum JudicialScaleProfile {
    VARA_1G(
            "Vara de 1º Grau",
            "PRIMEIRA_INSTANCIA",
            1.00d,
            1.00d,
            1.00d,
            1.00d,
            1.00d,
            true,
            false,
            true
    ),
    TURMA_RECURSAL(
            "Turma Recursal",
            "SEGUNDA_INSTANCIA",
            0.92d,
            0.95d,
            0.90d,
            0.96d,
            0.92d,
            true,
            true,
            false
    ),
    SECRETARIA_TRIBUNAL(
            "Secretaria Judiciária de Tribunal",
            "SEGUNDA_INSTANCIA",
            1.28d,
            1.18d,
            1.10d,
            1.08d,
            1.24d,
            true,
            true,
            true
    ),
    SECRETARIA_TRIBUNAL_SUPERIOR(
            "Secretaria Judiciária de Tribunal Superior",
            "INSTANCIA_SUPERIOR",
            1.42d,
            1.20d,
            0.88d,
            1.02d,
            1.36d,
            true,
            true,
            true
    );

    private final String displayName;
    private final String instanceClass;
    private final double queueParallelismFactor;
    private final double queueBudgetFactor;
    private final double replicaLagFactor;
    private final double readPressureFactor;
    private final double rateLimitFactor;
    private final boolean cacheHotPreferred;
    private final boolean searchIndexPreferred;
    private final boolean asyncWritePreferred;

    JudicialScaleProfile(String displayName,
                         String instanceClass,
                         double queueParallelismFactor,
                         double queueBudgetFactor,
                         double replicaLagFactor,
                         double readPressureFactor,
                         double rateLimitFactor,
                         boolean cacheHotPreferred,
                         boolean searchIndexPreferred,
                         boolean asyncWritePreferred) {
        this.displayName = displayName;
        this.instanceClass = instanceClass;
        this.queueParallelismFactor = queueParallelismFactor;
        this.queueBudgetFactor = queueBudgetFactor;
        this.replicaLagFactor = replicaLagFactor;
        this.readPressureFactor = readPressureFactor;
        this.rateLimitFactor = rateLimitFactor;
        this.cacheHotPreferred = cacheHotPreferred;
        this.searchIndexPreferred = searchIndexPreferred;
        this.asyncWritePreferred = asyncWritePreferred;
    }

    public String displayName() {
        return displayName;
    }

    public String instanceClass() {
        return instanceClass;
    }

    public double queueParallelismFactor() {
        return queueParallelismFactor;
    }

    public double queueBudgetFactor() {
        return queueBudgetFactor;
    }

    public double replicaLagFactor() {
        return replicaLagFactor;
    }

    public double readPressureFactor() {
        return readPressureFactor;
    }

    public double rateLimitFactor() {
        return rateLimitFactor;
    }

    public boolean cacheHotPreferred() {
        return cacheHotPreferred;
    }

    public boolean searchIndexPreferred() {
        return searchIndexPreferred;
    }

    public boolean asyncWritePreferred() {
        return asyncWritePreferred;
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", name());
        out.put("displayName", displayName);
        out.put("instanceClass", instanceClass);
        out.put("queueParallelismFactor", queueParallelismFactor);
        out.put("queueBudgetFactor", queueBudgetFactor);
        out.put("replicaLagFactor", replicaLagFactor);
        out.put("readPressureFactor", readPressureFactor);
        out.put("rateLimitFactor", rateLimitFactor);
        out.put("cacheHotPreferred", cacheHotPreferred);
        out.put("searchIndexPreferred", searchIndexPreferred);
        out.put("asyncWritePreferred", asyncWritePreferred);
        return Collections.unmodifiableMap(out);
    }

    public static JudicialScaleProfile fromToken(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String token = value.trim().toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');
        for (JudicialScaleProfile profile : values()) {
            if (profile.name().equals(token)) {
                return profile;
            }
        }
        return null;
    }
}
