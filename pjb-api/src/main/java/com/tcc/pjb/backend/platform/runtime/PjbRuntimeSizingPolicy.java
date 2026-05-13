package com.tcc.pjb.backend.platform.runtime;

import java.util.Locale;

public final class PjbRuntimeSizingPolicy {

    private PjbRuntimeSizingPolicy() {
    }

    public static Footprint detect() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemoryBytes = runtime.maxMemory();
        long maxMemoryMiB = maxMemoryBytes > 0L ? Math.max(256L, maxMemoryBytes / (1024L * 1024L)) : 1024L;
        return new Footprint(runtime.availableProcessors(), maxMemoryMiB);
    }

    public static int clampHikariMaximumPoolSize(String beanName, int configuredMaximumPoolSize, Footprint footprint) {
        int configured = Math.max(1, configuredMaximumPoolSize);
        Footprint safeFootprint = footprint == null ? detect() : footprint;
        String name = beanName == null ? "" : beanName.toLowerCase(Locale.ROOT);
        boolean readPool = name.contains("read");
        boolean writePool = name.contains("write") || name.contains("primary");
        int cpuMultiplier = readPool ? 5 : 4;
        int cpuCeiling = safeFootprint.availableProcessors() * cpuMultiplier;
        if (!readPool && !writePool) {
            cpuCeiling = safeFootprint.availableProcessors() * 3;
        }
        cpuCeiling = clamp(cpuCeiling, 4, readPool ? 40 : 28);
        int memoryDivisor = readPool ? 104 : 128;
        int memoryCeiling = clamp((int) (safeFootprint.maxMemoryMiB() / memoryDivisor), 4, readPool ? 40 : 28);
        int ceiling = Math.max(4, Math.min(cpuCeiling, memoryCeiling));
        return Math.min(configured, ceiling);
    }

    public static int clampHikariMinimumIdle(int configuredMinimumIdle, int maximumPoolSize) {
        int maxPool = Math.max(1, maximumPoolSize);
        int configured = Math.max(0, configuredMinimumIdle);
        int idleRatioCap = maxPool <= 4 ? maxPool : Math.max(1, maxPool / 3);
        return Math.min(configured, Math.min(maxPool, idleRatioCap));
    }

    static int clampLaneConcurrency(String laneName,
                                    int configuredConcurrencyLimit,
                                    Footprint footprint,
                                    String componentRole) {
        int configured = Math.max(1, configuredConcurrencyLimit);
        Footprint safeFootprint = footprint == null ? detect() : footprint;
        String normalizedLane = normalizeLane(laneName);
        String normalizedRole = normalizeRole(componentRole);
        int cpu = safeFootprint.availableProcessors();
        int memoryBound = clamp((int) (safeFootprint.maxMemoryMiB() / 10L), 24, 384);
        int laneBound = switch (normalizedLane) {
            case "async" -> clamp(cpu * 6, 12, 96);
            case "io" -> clamp(cpu * 16, 16, 192);
            case "burst" -> clamp(cpu * 20, 20, 224);
            case "externalio" -> clamp(cpu * 12, 12, 128);
            case "live" -> clamp(cpu * 10, 12, 96);
            case "job" -> clamp(cpu * 12, 12, 160);
            default -> clamp(cpu * 8, 12, 96);
        };
        laneBound = applyRoleBias(normalizedRole, normalizedLane, laneBound);
        if (safeFootprint.maxMemoryMiB() < 1024L) {
            memoryBound = Math.min(memoryBound, switch (normalizedLane) {
                case "burst" -> 72;
                case "io", "job" -> 56;
                case "externalio", "live" -> 40;
                default -> 24;
            });
        } else if (safeFootprint.maxMemoryMiB() < 2048L) {
            memoryBound = Math.min(memoryBound, switch (normalizedLane) {
                case "burst" -> 112;
                case "io", "job" -> 80;
                case "externalio", "live" -> 64;
                default -> 48;
            });
        }
        return Math.min(configured, Math.min(laneBound, memoryBound));
    }

    static int timeoutSchedulerPoolSize(Footprint footprint) {
        Footprint safeFootprint = footprint == null ? detect() : footprint;
        if (safeFootprint.availableProcessors() >= 16 && safeFootprint.maxMemoryMiB() >= 8192L) {
            return 3;
        }
        if (safeFootprint.availableProcessors() >= 8 && safeFootprint.maxMemoryMiB() >= 3072L) {
            return 2;
        }
        return 1;
    }

    static double readinessExecutorUtilizationThreshold(String componentRole) {
        return switch (normalizeRole(componentRole)) {
            case "worker" -> 0.98d;
            case "api" -> 0.94d;
            default -> 0.96d;
        };
    }

    private static int applyRoleBias(String componentRole, String laneName, int laneBound) {
        return switch (componentRole) {
            case "api" -> switch (laneName) {
                case "externalio" -> Math.max(12, clamp((int) Math.round(laneBound * 1.15d), 12, 160));
                case "live" -> Math.max(12, clamp((int) Math.round(laneBound * 1.10d), 12, 128));
                case "job" -> Math.max(8, clamp((int) Math.round(laneBound * 0.55d), 8, 96));
                case "burst" -> Math.max(12, clamp((int) Math.round(laneBound * 0.85d), 12, 192));
                default -> laneBound;
            };
            case "worker" -> switch (laneName) {
                case "job" -> Math.max(12, clamp((int) Math.round(laneBound * 1.20d), 12, 224));
                case "io" -> Math.max(16, clamp((int) Math.round(laneBound * 1.10d), 16, 224));
                case "live" -> Math.max(8, clamp((int) Math.round(laneBound * 0.70d), 8, 96));
                default -> laneBound;
            };
            default -> laneBound;
        };
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static String normalizeLane(String laneName) {
        if (laneName == null) {
            return "default";
        }
        String normalized = laneName.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "external-io", "external_io" -> "externalio";
            default -> normalized.replace("-", "").replace("_", "");
        };
    }

    private static String normalizeRole(String componentRole) {
        if (componentRole == null || componentRole.isBlank()) {
            return "mixed";
        }
        String normalized = componentRole.toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "api", "worker", "mixed" -> normalized;
            default -> "mixed";
        };
    }

    public record Footprint(int availableProcessors, long maxMemoryMiB) {
        public Footprint {
            availableProcessors = Math.max(1, availableProcessors);
            maxMemoryMiB = Math.max(256L, maxMemoryMiB);
        }
    }
}
