package com.tcc.pjb.backend.service.rito;

import java.util.Collections;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.juridica.v3.core.LegalRitosEngine;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate.GateResult;
import com.tcc.pjb.backend.core.procedural.ProcessoCanonicalPayloadFactory;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.RitoOverrideRepository;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;







@Service
public class RitoResolutionService {

    private static final int MAX_CACHE_ENTRIES = 20_000;
    private static final int TARGET_CACHE_ENTRIES = 16_000;

    private final LegalRitosEngine engine;
    private final RitoPackService ritoPack;
    private final DecisionTraceService traceService;
    private final RitoOverrideRepository overrideRepository;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final CanonicalSanityGate canonicalSanityGate;

    private final boolean traceEnabled;
    private final Duration cacheTtl;

    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong cleanupClock = new java.util.concurrent.atomic.AtomicLong();

    public RitoResolutionService(LegalRitosEngine engine,
                                RitoPackService ritoPack,
                                DecisionTraceService traceService,
                                RitoOverrideRepository overrideRepository,
                                ProceduralCanonicalResolver proceduralCanonicalResolver,
                                CanonicalSanityGate canonicalSanityGate,
                                @Value("${pjb.ritos.trace-enabled:false}") boolean traceEnabled,
                                @Value("${pjb.ritos.cache-ttl-minutes:360}") long cacheTtlMinutes) {
        this.engine = Objects.requireNonNull(engine);
        this.ritoPack = Objects.requireNonNull(ritoPack);
        this.traceService = Objects.requireNonNull(traceService);
        this.overrideRepository = Objects.requireNonNull(overrideRepository);
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.canonicalSanityGate = Objects.requireNonNull(canonicalSanityGate);
        this.traceEnabled = traceEnabled;
        this.cacheTtl = Duration.ofMinutes(Math.max(5, cacheTtlMinutes));
    }

    public RitoResolution resolve(Processo p, String lastMovText) {
        return resolveDetailed(p, lastMovText).resolution();
    }

    public RitoResolutionDetail resolveDetailed(Processo p, String lastMovText) {
        if (p == null || p.getId() == null) {
            CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(Map.of());
            GateResult gateResult = canonicalSanityGate.evaluate(canonicalContext);
            RitoResolution fallback = RitoResolution.fallback(RitoProcessual.COMUM_ORDINARIO, "processo_nulo");
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("origin", "processo_nulo");
            metadata.put("canonicalContext", canonicalContext.toMap());
            metadata.put("sanityGate", gateResult.toMap());
            metadata.put("statusCodes", gateResult.statusCodes());
            return new RitoResolutionDetail(fallback, gateResult.overallStatus(), gateResult.hasBlockingIssues(), canonicalContext, Collections.unmodifiableMap(metadata));
        }

        Map<String, Object> payload = ProcessoCanonicalPayloadFactory.fromProcesso(p, lastMovText);
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(payload);
        GateResult gateResult = canonicalSanityGate.evaluate(canonicalContext);
        RitoResolution resolution = resolveCached(p, lastMovText, payload, canonicalContext);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("canonicalContext", canonicalContext.toMap());
        metadata.put("sanityGate", gateResult.toMap());
        metadata.put("statusCodes", gateResult.statusCodes());
        metadata.put("resolutionReasons", resolution.reasons());
        if (resolution.rito() != null) {
            metadata.put("resolvedRito", resolution.rito().name());
        }
        return new RitoResolutionDetail(resolution, gateResult.overallStatus(), gateResult.hasBlockingIssues(), canonicalContext, Collections.unmodifiableMap(metadata));
    }

    private RitoResolution resolveCached(Processo p, String lastMovText, Map<String, Object> ctx, CanonicalContext canonicalContext) {
        try {
            var ov = overrideRepository.findByProcessoId(p.getId()).orElse(null);
            if (ov != null && ov.getRitoCode() != null && !ov.getRitoCode().isBlank()) {
                RitoProcessual rito = parseRito(ov.getRitoCode());
                if (rito != null) {
                    RitoResolution out = enrich(rito, 1.0, List.of("override:processo"));
                    putCacheEntry(p.getId(), new CacheEntry(out, p, lastMovText, cacheTtl));
                    return out;
                }
            }
        } catch (Exception ignored) {
        }

        maybeCleanupCache();
        CacheEntry ce = cache.get(p.getId());
        if (ce != null) {
            if (ce.isExpired()) {
                cache.remove(p.getId(), ce);
            } else if (ce.sameInput(p, lastMovText)) {
                return ce.value;
            }
        }

        if (p.getRito() != null && p.getRito() != RitoProcessual.COMUM_ORDINARIO) {
            RitoResolution out = enrich(p.getRito(), 1.0, List.of("db:rito=" + p.getRito().name()));
            putCacheEntry(p.getId(), new CacheEntry(out, p, lastMovText, cacheTtl));
            return out;
        }

        Map<String, Object> inferred = engine.inferRito(ctx);
        String ritoName = inferred != null ? Objects.toString(inferred.get("rito"), null) : null;

        RitoProcessual rito = canonicalContext.rito() != null ? canonicalContext.rito() : parseRito(ritoName);
        if (rito == null) rito = RitoProcessual.COMUM_ORDINARIO;

        double conf = computeConfidence(p, lastMovText);
        List<String> reasons = buildReasons(p, lastMovText, inferred, canonicalContext);
        RitoResolution out = enrich(rito, conf, reasons);

        if (traceEnabled && conf < 0.70) {
            try {
                traceService.record(
                        "RITO_RESOLUTION",
                        "PROCESSO",
                        String.valueOf(p.getId()),
                        BigDecimal.valueOf(conf).setScale(4, java.math.RoundingMode.HALF_UP),
                        toJsonArray(reasons),
                        null,
                        null,
                        null,
                        "ritos_engine_v1",
                        null
                );
            } catch (Exception ignored) {
            }
        }

        putCacheEntry(p.getId(), new CacheEntry(out, p, lastMovText, cacheTtl));
        return out;
    }

    


    private void putCacheEntry(Long processoId, CacheEntry entry) {
        cache.put(processoId, entry);
        if (cache.size() > MAX_CACHE_ENTRIES) {
            trimOverflowCache();
        }
    }

    private void maybeCleanupCache() {
        long tick = cleanupClock.incrementAndGet();
        if ((tick & 255L) != 0L && cache.size() <= MAX_CACHE_ENTRIES) {
            return;
        }
        purgeExpiredCacheEntries();
        if (cache.size() > MAX_CACHE_ENTRIES) {
            trimOverflowCache();
        }
    }

    private void purgeExpiredCacheEntries() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void trimOverflowCache() {
        int overflow = cache.size() - TARGET_CACHE_ENTRIES;
        if (overflow <= 0) {
            return;
        }
        cache.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getValue().createdAtMillis))
                .limit(overflow)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(cache::remove);
    }

    public void invalidateCache(Long processoId) {
        if (processoId != null) {
            cache.remove(processoId);
        }
    }

    private RitoResolution enrich(RitoProcessual rito, double confidence, List<String> reasons) {
        Optional<RitoDefinition> def = ritoPack.get(rito);
        String title = def.map(RitoDefinition::getTitle).orElse(rito.name());
        String ramo = def.map(RitoDefinition::getRamoSugerido).orElse(null);
        return new RitoResolution(rito, title, ramo, confidence, reasons);
    }

    private static String safeJoin(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        String j = (x + " " + y).trim();
        return j.isBlank() ? null : j;
    }

    private static RitoProcessual parseRito(String raw) {
        return raw == null || raw.isBlank() ? null : RitoProcessual.tryParse(raw).orElse(null);
    }

    private static double computeConfidence(Processo p, String lastMovText) {
        double c = 0.55;
        if (p.getAssunto() != null && !p.getAssunto().isBlank()) c += 0.15;
        if (p.getClasseProcessual() != null && !p.getClasseProcessual().isBlank()) c += 0.10;
        if (p.getMateria() != null) c += 0.10;
        if (p.getRamoDireito() != null) c += 0.05;
        
        if (p.getJurisdicao() != null) c += 0.07;
        if (lastMovText != null && lastMovText.length() >= 16) c += 0.05;
        return Math.min(0.95, c);
    }

    private static List<String> buildReasons(Processo p, String lastMovText, Map<String, Object> inferred, CanonicalContext canonicalContext) {
        List<String> r = new ArrayList<>();
        if (p.getMateria() != null) r.add("materia=" + p.getMateria().name());
        if (p.getRamoDireito() != null) r.add("ramo=" + p.getRamoDireito().name());

        if (p.getClasseProcessual() != null && !p.getClasseProcessual().isBlank()) r.add("classe=" + shortStr(p.getClasseProcessual()));
        if (p.getAssunto() != null && !p.getAssunto().isBlank()) r.add("assunto=" + shortStr(p.getAssunto()));
        if (lastMovText != null && !lastMovText.isBlank()) r.add("mov=" + shortStr(lastMovText));
        if (inferred != null && inferred.get("status") != null) r.add("engine_status=" + inferred.get("status"));
        if (canonicalContext != null && canonicalContext.rito() != null) r.add("canonical_rito=" + canonicalContext.rito().name());
        if (canonicalContext != null && canonicalContext.classeTpuCodigo() != null) r.add("canonical_classe=" + canonicalContext.classeTpuCodigo());
        if (canonicalContext != null && canonicalContext.tribunalCodigo() != null) r.add("canonical_tribunal=" + canonicalContext.tribunalCodigo());
        r.add("engine=LegalRitosEngine");
        return List.copyOf(r);
    }

    private static String shortStr(String s) {
        String t = s.trim();
        if (t.length() <= 140) return t;
        return t.substring(0, 137) + "…";
    }

    private static String toJsonArray(List<String> items) {
        if (items == null || items.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            String it = items.get(i);
            sb.append('"').append(it.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            if (i < items.size() - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }

    private static final class CacheEntry {
        final RitoResolution value;
        final long ttlMillis;
        final long createdAtMillis;
        final String inputDigest;
        final LocalDateTime ultimaMov;

        CacheEntry(RitoResolution value, Processo p, String lastMovText, Duration ttl) {
            this.value = value;
            this.ttlMillis = ttl.toMillis();
            this.createdAtMillis = System.currentTimeMillis();
            this.ultimaMov = p.getDataUltimaMovimentacao();
            this.inputDigest = digest(p, lastMovText);
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAtMillis > ttlMillis;
        }

        boolean sameInput(Processo p, String lastMovText) {
            if (!Objects.equals(ultimaMov, p.getDataUltimaMovimentacao())) return false;
            return Objects.equals(inputDigest, digest(p, lastMovText));
        }

        static String digest(Processo p, String lastMovText) {
            String jur = "";
            return String.valueOf(p.getRito()) + "|" +
                    safe(p.getClasseProcessual()) + "|" +
                    safe(p.getAssunto()) + "|" +
                    safe(lastMovText) + "|" +
                    jur;
        }

        static String safe(String s) {
            if (s == null) return "";
            String t = s.trim();
            return t.length() <= 64 ? t : t.substring(0, 64);
        }
    }

    public record RitoResolutionDetail(
            RitoResolution resolution,
            String status,
            boolean blocking,
            CanonicalContext canonicalContext,
            Map<String, Object> metadata
    ) {
    }

    public record RitoResolution(
            RitoProcessual rito,
            String ritoTitle,
            String ramoSugerido,
            double confidence,
            List<String> reasons
    ) {
        static RitoResolution fallback(RitoProcessual rito, String reason) {
            return new RitoResolution(rito, rito != null ? rito.name() : null, null, 0.50, List.of(reason));
        }
    }
}
