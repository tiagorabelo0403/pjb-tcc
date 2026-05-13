package com.tcc.pjb.backend.service.jurisprudencia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.repository.PrecedenteRepository;
import com.tcc.pjb.backend.service.jurisprudencia.graph.JurisprudenceGraphService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JurisprudenciaSyncJob {

    private final ObjectProvider<JurisprudenciaProvider> providers;
    private final PrecedenteRepository repository;
    private final JurisprudenceGraphService graphService;

    
    private final Map<String, LocalDateTime> lastSyncByProvider = new LinkedHashMap<>();

    
    private final ReentrantLock lock = new ReentrantLock();

    public JurisprudenciaSyncJob(ObjectProvider<JurisprudenciaProvider> providers,
                                 PrecedenteRepository repository,
                                 JurisprudenceGraphService graphService) {
        this.providers = providers;
        this.repository = repository;
        this.graphService = graphService;
    }

    @PjbClusterSingletonTask(key = "jurisprudencia-sync", ttl = "PT1H")
    @Scheduled(cron = "${pjb.jurisprudencia.sync.cron:0 0 3 1 1 ? 2099}")
    public void sync() {
        if (!lock.tryLock()) return;
        try {
            for (JurisprudenciaProvider p : providers) {
                String providerName = safeProviderName(p);
                LocalDateTime lastSync = lastSyncByProvider.getOrDefault(providerName, LocalDateTime.now().minusDays(30));
                try {
                    List<Precedente> updates = p.fetchUpdates(lastSync);
                    if (updates != null && !updates.isEmpty()) {
                        upsertBatch(updates);
                    }
                    lastSyncByProvider.put(providerName, LocalDateTime.now());
                } catch (Exception e) {
                    log.warn("Falha ao sincronizar jurisprudência. provider={} lastSync={} err={}"
                            , providerName, lastSync, e.getMessage());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void upsertBatch(List<Precedente> updates) {
        List<Precedente> toSave = new ArrayList<>(updates.size());
        for (Precedente p : updates) {
            Precedente merged = upsertOne(p);
            if (merged != null) {
                toSave.add(merged);
            }
        }
        if (!toSave.isEmpty()) {
            List<Precedente> saved = repository.saveAll(toSave);
            for (Precedente s : saved) {
                try {
                    graphService.upsertEdgesFor(s);
                } catch (Exception e) {
                    log.debug("Falha ao gerar arestas do grafo. precedenteId={} err={}"
                            , s != null ? s.getId() : null, e.getMessage());
                }
            }
        }
    }

    private Precedente upsertOne(Precedente incoming) {
        if (incoming == null) return null;

        sanitize(incoming);

        String ident = normalize(incoming.getIdentificador());
        if (ident != null) {
            Precedente existing = repository.findFirstByFonteAndTipoAndIdentificador(
                    incoming.getFonte(), incoming.getTipo(), ident);
            if (existing != null) {
                merge(existing, incoming);
                return existing;
            }
        }

        
        incoming.setId(null);
        return incoming;
    }

    private static void merge(Precedente target, Precedente src) {
        if (target == null || src == null) return;
        if (isBlank(target.getTitulo())) target.setTitulo(src.getTitulo());
        if (isBlank(target.getTese())) target.setTese(src.getTese());
        if (isBlank(target.getEmentaResumo())) target.setEmentaResumo(src.getEmentaResumo());
        if (isBlank(target.getUrlReferencia())) target.setUrlReferencia(src.getUrlReferencia());
        if (target.getDataPublicacao() == null) target.setDataPublicacao(src.getDataPublicacao());
        if (target.getRamoSugerido() == null) target.setRamoSugerido(src.getRamoSugerido());
        if (target.getRitoSugerido() == null) target.setRitoSugerido(src.getRitoSugerido());
    }

    private static void sanitize(Precedente p) {
        
        if (p.getFonte() == null) p.setFonte(com.tcc.pjb.backend.model.entity.enums.TribunalFonte.OUTRO);
        if (p.getTipo() == null) p.setTipo(com.tcc.pjb.backend.model.entity.enums.TipoPrecedente.OUTRO);

        p.setIdentificador(normalize(p.getIdentificador()));
        p.setTitulo(limit(normalize(p.getTitulo()), 260));
        p.setUrlReferencia(limit(normalize(p.getUrlReferencia()), 600));

        
        p.setTese(normalizeSpaces(p.getTese()));
        p.setEmentaResumo(normalizeSpaces(p.getEmentaResumo()));
    }

    private static String safeProviderName(JurisprudenciaProvider p) {
        try {
            String n = p.getName();
            if (!isBlank(n)) return n.trim();
        } catch (Exception ignored) {
        }
        return p.getClass().getSimpleName();
    }

    private static String normalize(String v) {
        if (v == null) return null;
        String s = v.trim().replaceAll("\\s+", " ");
        return s.isEmpty() ? null : s;
    }

    private static String normalizeSpaces(String v) {
        if (v == null) return null;
        String s = v.trim().replaceAll("\\s+", " ");
        return s.isEmpty() ? null : s;
    }

    private static String limit(String v, int max) {
        if (v == null) return null;
        if (v.length() <= max) return v;
        return v.substring(0, max);
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }
}
