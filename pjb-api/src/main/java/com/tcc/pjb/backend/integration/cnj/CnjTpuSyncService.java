package com.tcc.pjb.backend.integration.cnj;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.stereotype.Service;

@Service
public class CnjTpuSyncService {

    private static final Logger log = LoggerFactory.getLogger(CnjTpuSyncService.class);

    
    public record TpuCatalogSnapshot(
            String snapshotId,
            Instant syncedAt,
            String sourceVersion,
            boolean fromCnj,
            int totalClasses,
            int totalAssuntos,
            List<TpuClasseEntry> classes,
            List<TpuAssuntoEntry> assuntos,
            List<TpuDocumentoEntry> documentos,
            List<String> divergencias,
            Map<String, Object> metadata
    ) {
        public boolean isFresh(Duration maxAge) {
            return syncedAt != null && !syncedAt.plus(maxAge).isBefore(Instant.now());
        }
        public boolean isFresh() { return isFresh(Duration.ofHours(24)); }
    }

    
    public record TpuClasseEntry(
            int codigo,
            String nome,
            String descricao,
            String ramoDireito,
            String ramoJustica,
            boolean ativo,
            String versaoBoletim,
            Instant vigenciaInicio
    ) {}

    
    public record TpuAssuntoEntry(
            int codigo,
            String nome,
            String descricao,
            String tabelaId,
            boolean ativo
    ) {}

    
    public record TpuDocumentoEntry(
            int codigo,
            String nome,
            String descricao,
            boolean obrigatorio,
            String escopoClasse
    ) {}

    
    public record DivergenceReport(
            Instant generatedAt,
            int classesLocais,
            int classesCnj,
            List<String> classesNoLocalNaoCnj,
            List<String> classesCnjNaoLocal,
            List<String> descricoesDesatualizadas,
            boolean integridadeOk
    ) {}

    
    private final AtomicReference<TpuCatalogSnapshot> currentSnapshot = new AtomicReference<>();
    private final AtomicReference<Instant> lastSyncAttempt = new AtomicReference<>(Instant.EPOCH);
    private final AtomicReference<String> lastSyncError = new AtomicReference<>(null);

    @Value("${pjb.cnj.datajud.api-key:#{null}}")
    private String datajudApiKey;

    @Value("${pjb.cnj.datajud.base-url:https://api-publica.datajud.cnj.jus.br}")
    private String datajudBaseUrl;

    @Value("${pjb.cnj.datajud.timeout-ms:30000}")
    private long datajudTimeoutMs;

    @Value("${pjb.cnj.tpu.sync-enabled:true}")
    private boolean syncEnabled;

    @Value("${pjb.cnj.tpu.max-age-hours:24}")
    private int maxAgeHours;

    private final HttpClient httpClient;

    public CnjTpuSyncService(@Qualifier("pjbSharedHttpClient") HttpClient httpClient) {
        this.httpClient = java.util.Objects.requireNonNull(httpClient);
    }

    
    @PjbClusterSingletonTask(key = "cnj-tpu-sync", ttl = "PT30M")
    @Scheduled(cron = "0 0 3 * * *", zone = "America/Sao_Paulo")
    public void scheduledSync() {
        if (!syncEnabled) {
            log.info("[CnjTpuSync] Sincronização desabilitada por configuração.");
            return;
        }
        log.info("[CnjTpuSync] Iniciando sincronização agendada com TPU/CNJ...");
        try {
            TpuCatalogSnapshot snap = buildFromLocal();
            currentSnapshot.set(snap);
            lastSyncAttempt.set(Instant.now());
            lastSyncError.set(null);
            log.info("[CnjTpuSync] Snapshot local construído: {} classes, {} assuntos.",
                    snap.totalClasses(), snap.totalAssuntos());
            if (datajudApiKey != null && !datajudApiKey.isBlank()) {
                tryEnrichFromDataJud();
            }
        } catch (Exception e) {
            lastSyncError.set(e.getMessage());
            log.error("[CnjTpuSync] Erro na sincronização: {}", e.getMessage(), e);
        }
    }

    
    public TpuCatalogSnapshot forceSync() {
        scheduledSync();
        return currentSnapshot.get();
    }

    public Optional<TpuCatalogSnapshot> currentSnapshot() {
        TpuCatalogSnapshot snap = currentSnapshot.get();
        if (snap == null) {
            snap = buildFromLocal();
            currentSnapshot.set(snap);
        }
        return Optional.of(snap);
    }

    public boolean isFresh() {
        TpuCatalogSnapshot snap = currentSnapshot.get();
        return snap != null && snap.isFresh(Duration.ofHours(Math.max(1, maxAgeHours)));
    }

    public Instant lastSyncAttempt() { return lastSyncAttempt.get(); }
    public Optional<String> lastSyncError() { return Optional.ofNullable(lastSyncError.get()); }

    
    public DivergenceReport checkDivergence() {
        TpuCatalogSnapshot snapshot = currentSnapshot.get();
        TpuCatalogSnapshot effectiveSnapshot = snapshot == null ? buildFromLocal() : snapshot;

        Map<Integer, TpuClasseCnj> localByCode = new LinkedHashMap<>();
        Map<String, TpuClasseCnj> localByNormalizedName = new LinkedHashMap<>();
        for (TpuClasseCnj local : TpuClasseCnj.values()) {
            localByCode.put(local.codigoTpu(), local);
            localByNormalizedName.put(normalizeKey(local.descricao()), local);
            localByNormalizedName.putIfAbsent(normalizeKey(local.name().replace('_', ' ')), local);
        }

        Map<Integer, TpuClasseEntry> remoteByCode = new LinkedHashMap<>();
        Map<String, TpuClasseEntry> remoteByNormalizedName = new LinkedHashMap<>();
        for (TpuClasseEntry remote : effectiveSnapshot.classes()) {
            remoteByCode.putIfAbsent(remote.codigo(), remote);
            remoteByNormalizedName.putIfAbsent(normalizeKey(remote.nome()), remote);
        }

        List<String> noLocalNaoCnj = new ArrayList<>();
        for (TpuClasseCnj local : TpuClasseCnj.values()) {
            if (!remoteByCode.containsKey(local.codigoTpu())
                    && !remoteByNormalizedName.containsKey(normalizeKey(local.descricao()))
                    && !remoteByNormalizedName.containsKey(normalizeKey(local.name().replace('_', ' ')))) {
                noLocalNaoCnj.add(local.name());
            }
        }

        List<String> cnjNaoLocal = new ArrayList<>();
        for (TpuClasseEntry remote : effectiveSnapshot.classes()) {
            if (!localByCode.containsKey(remote.codigo())
                    && !localByNormalizedName.containsKey(normalizeKey(remote.nome()))) {
                cnjNaoLocal.add(remote.codigo() + " - " + remote.nome());
            }
        }

        List<String> desatualiz = new ArrayList<>();
        for (TpuClasseCnj local : TpuClasseCnj.values()) {
            TpuClasseEntry remote = remoteByCode.get(local.codigoTpu());
            if (remote != null && !normalizeKey(remote.nome()).equals(normalizeKey(local.descricao()))) {
                desatualiz.add("Código " + local.codigoTpu() + ": local='" +
                        local.descricao() + "' CNJ='" + remote.nome() + "'");
            }
        }

        return new DivergenceReport(
                Instant.now(),
                localByCode.size(),
                effectiveSnapshot.classes().size(),
                noLocalNaoCnj,
                cnjNaoLocal,
                desatualiz,
                noLocalNaoCnj.isEmpty() && cnjNaoLocal.isEmpty() && desatualiz.isEmpty()
        );
    }

    
    public Map<String, Object> exportMni() {
        TpuCatalogSnapshot snap = currentSnapshot().orElseGet(this::buildFromLocal);
        Map<String, Object> mni = new LinkedHashMap<>();
        mni.put("versao", "2.2.2");
        mni.put("snapshotId", snap.snapshotId());
        mni.put("syncedAt", snap.syncedAt().toString());
        mni.put("totalClasses", snap.totalClasses());
        mni.put("classes", snap.classes().stream().map(c -> {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("codigo", c.codigo());
            cm.put("nome", c.nome());
            cm.put("ramoDireito", c.ramoDireito());
            cm.put("ramoJustica", c.ramoJustica());
            cm.put("ativo", c.ativo());
            return cm;
        }).toList());
        mni.put("tribunais", Arrays.stream(NationalCompetenceMatrix.values())
                .map(NationalCompetenceMatrix::toMap).toList());
        return mni;
    }

    public Map<String, Object> health() {
        TpuCatalogSnapshot snap = currentSnapshot.get();
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("syncEnabled", syncEnabled);
        h.put("hasSnapshot", snap != null);
        h.put("snapshotFresh", isFresh());
        h.put("lastSyncAttempt", lastSyncAttempt().toString());
        h.put("lastSyncError", lastSyncError().orElse(null));
        h.put("totalClassesLocal", TpuClasseCnj.values().length);
        h.put("totalTribunais", NationalCompetenceMatrix.values().length);
        if (snap != null) {
            h.put("snapshotId", snap.snapshotId());
            h.put("syncedAt", snap.syncedAt().toString());
            h.put("totalClassesCnj", snap.totalClasses());
        }
        return h;
    }

    
    private static String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.strip()
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .toUpperCase(java.util.Locale.ROOT);
    }

    private TpuCatalogSnapshot buildFromLocal() {
        List<TpuClasseEntry> classes = Arrays.stream(TpuClasseCnj.values())
                .map(c -> new TpuClasseEntry(
                        c.codigoTpu(),
                        c.descricao(),
                        c.descricao(),
                        c.ramoDireito().name(),
                        c.ramoJustica().name(),
                        true,
                        "LOCAL-2026",
                        Instant.parse("2026-01-01T00:00:00Z")
                )).toList();

        return new TpuCatalogSnapshot(
                UUID.randomUUID().toString(),
                Instant.now(),
                "LOCAL-2026",
                false,
                classes.size(),
                0,
                classes,
                List.of(),
                List.of(),
                List.of("Snapshot construído a partir do catálogo local — CNJ não consultado nesta execução."),
                Map.of("source", "LOCAL", "totalTribunais", NationalCompetenceMatrix.values().length)
        );
    }

    private void tryEnrichFromDataJud() {
        String url = datajudBaseUrl + "/api_publica_cnj/_search";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(Math.max(1000L, datajudTimeoutMs)))
                .header("Authorization", "ApiKey " + datajudApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"query\":{\"match_all\":{}},\"size\":1,\"_source\":[\"codigoClasse\",\"nomeClasse\"]}"))
                .build();

        httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> {
                    if (resp.statusCode() == 200) {
                        log.info("[CnjTpuSync] DataJud acessível — enriquecimento registrado. " +
                                "Implemente parser completo conforme necessidade de produção.");
                    } else {
                        log.warn("[CnjTpuSync] DataJud retornou HTTP {} — usando snapshot local.", resp.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    log.warn("[CnjTpuSync] DataJud inacessível: {} — usando snapshot local.", ex.getMessage());
                    return null;
                });
    }
}
