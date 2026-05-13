package com.tcc.pjb.backend.core.quality.codebase.application;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record PjbCodebaseLearningSettings(
        String basePackage,
        double testRatioFloor,
        double laneReadyFloor,
        double laneHardenThreshold,
        int blueprintLimit,
        Duration cacheTtl,
        List<PjbCodebaseCriticalFlowDefinition> criticalFlows
) {

    public PjbCodebaseLearningSettings {
        basePackage = normalizeBasePackage(basePackage);
        testRatioFloor = clampRatio(testRatioFloor, 0.20d);
        laneReadyFloor = Math.max(testRatioFloor, clampRatio(laneReadyFloor, 0.30d));
        laneHardenThreshold = Math.min(laneReadyFloor, clampRatio(laneHardenThreshold, 0.10d));
        blueprintLimit = blueprintLimit <= 0 ? 6 : blueprintLimit;
        cacheTtl = cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero() ? Duration.ofMinutes(5) : cacheTtl;
        criticalFlows = criticalFlows == null ? List.of() : List.copyOf(criticalFlows);
    }

    public static PjbCodebaseLearningSettings defaults() {
        return new PjbCodebaseLearningSettings(
                "com.tcc.pjb.backend.",
                0.20d,
                0.30d,
                0.10d,
                6,
                Duration.ofMinutes(5),
                List.of(
                        new PjbCodebaseCriticalFlowDefinition(
                                "peticao-triagem-secretaria-gabinete-decisao-publicacao",
                                List.of("peticao", "triagem", "secretaria", "gabinete", "decisao", "publicacao")
                        ),
                        new PjbCodebaseCriticalFlowDefinition(
                                "protocolo-24x7-integridade",
                                List.of("protocolo", "hash", "integridade", "recibo")
                        ),
                        new PjbCodebaseCriticalFlowDefinition(
                                "intimacao-multicanal-ciencia",
                                List.of("intimacao", "notificacao", "whatsapp", "ciencia")
                        ),
                        new PjbCodebaseCriticalFlowDefinition(
                                "prazo-painel-alerta",
                                List.of("prazo", "painel", "alerta", "pred")
                        )
                )
        );
    }

    private static String normalizeBasePackage(String basePackage) {
        String normalized = Objects.toString(basePackage, "com.tcc.pjb.backend.").trim();
        if (normalized.isEmpty()) {
            return "com.tcc.pjb.backend.";
        }
        return normalized.endsWith(".") ? normalized : normalized + ".";
    }

    private static double clampRatio(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0d) {
            return fallback;
        }
        return Math.min(value, 1.0d);
    }
}
