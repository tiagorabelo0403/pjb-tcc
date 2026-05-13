package com.tcc.pjb.backend.service.jurisprudencia.search;

import java.util.Collections;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.repository.PrecedenteRepository;
import com.tcc.pjb.backend.shared.text.TextTokenUtils;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.util.Locale;

@Service
public class JurisprudenceSearchEngine {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceSearchEngine.class);

    private final PrecedenteRepository repository;
    private final ProceduralCatalogService proceduralCatalogService;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    
    private volatile Index index = Index.empty();

    public JurisprudenceSearchEngine(PrecedenteRepository repository, ProceduralCatalogService proceduralCatalogService) {
        this.repository = repository;
        this.proceduralCatalogService = proceduralCatalogService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void buildOnReady() {
        
        try {
            rebuild();
        } catch (Exception e) {
            log.warn("Falha ao construir índice BM25 de jurisprudência: {}", e.getMessage());
        }
    }

    
    public void rebuild() {
        lock.writeLock().lock();
        try {
            List<Precedente> all = fetchAllSafely(5_000);
            this.index = Index.build(all);
            log.info("JurisprudenceSearchEngine index built: docs={} terms={} avgdl={}",
                    index.docs.size(), index.df.size(), String.format(Locale.ROOT, "%.2f", index.avgdl));
        } finally {
            lock.writeLock().unlock();
        }
    }

    
    public RefreshStats refreshIncremental() {
        lock.writeLock().lock();
        try {
            long lastId = index.maxId;
            if (lastId <= 0 || index.docs.isEmpty()) {
                rebuild();
                return new RefreshStats("REBUILT", index.docs.size(), 0, index.maxId);
            }

            
            int pageSize = 500;
            int page = 0;
            ArrayList<Precedente> newOnes = new ArrayList<>();
            while (page < 10) { 
                var pr = repository.findAll(PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "id")));
                if (pr.isEmpty()) break;
                for (Precedente p : pr.getContent()) {
                    if (p == null || p.getId() == null) continue;
                    if (p.getId() > lastId) newOnes.add(p);
                }
                if (!pr.hasNext()) break;
                
                Precedente tail = pr.getContent().get(pr.getContent().size() - 1);
                if (tail != null && tail.getId() != null && tail.getId() <= lastId) break;
                page++;
            }

            if (newOnes.isEmpty()) {
                return new RefreshStats("NOOP", index.docs.size(), 0, index.maxId);
            }

            
            List<Precedente> merged = new ArrayList<>(Math.min(5_000, index.docs.size() + newOnes.size()));
            
            
            
            merged.addAll(fetchAllSafely(5_000));
            this.index = Index.build(merged);

            return new RefreshStats("MERGED", index.docs.size(), newOnes.size(), index.maxId);
        } catch (Exception e) {
            log.warn("Falha em refreshIncremental: {}", e.getMessage());
            return new RefreshStats("FAILED", index.docs.size(), 0, index.maxId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    
    @Scheduled(fixedDelayString = "${pjb.jurisprudencia.index.refreshDelayMs:900000}") 
    public void scheduledRefresh() {
        try {
            RefreshStats s = refreshIncremental();
            if (!"NOOP".equals(s.mode())) {
                log.info("JurisprudenceSearchEngine refresh: mode={} docs={} added={} maxId={}",
                        s.mode(), s.docs(), s.added(), s.maxId());
            }
        } catch (Exception ignored) {
        }
    }

    public IndexStatus status() {
        lock.readLock().lock();
        try {
            return new IndexStatus(index.docs.size(), index.df.size(), index.maxId, index.avgdl);
        } finally {
            lock.readLock().unlock();
        }
    }

    public record RefreshStats(String mode, int docs, int added, long maxId) {}
    public record IndexStatus(int docs, int terms, long maxId, double avgdl) {}

    public List<JurisprudenceSearchHit> search(String query, RamoDireito ramo, String ritoName, int topK) {
        RitoProcessual rito = ritoName == null || ritoName.isBlank() ? null : proceduralCatalogService.resolveRito(ritoName, null, null);
        return search(query, ramo, rito, topK);
    }

    public List<JurisprudenceSearchHit> search(String query, RamoDireito ramo, RitoProcessual rito, int topK) {
        if (query == null || query.isBlank()) return List.of();
        topK = Math.max(1, Math.min(topK, 25));

        
        if (index.docs.isEmpty()) {
            try {
                long c = repository.count();
                if (c > 0) rebuild();
            } catch (Exception ignored) {
            }
        }

        lock.readLock().lock();
        try {
            return index.search(query, ramo, rito, topK);
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<Precedente> fetchAllSafely(int hardLimit) {
        
        int pageSize = 500;
        int max = Math.max(1, hardLimit);
        ArrayList<Precedente> out = new ArrayList<>(Math.min(max, 2_000));

        int page = 0;
        while (out.size() < max) {
            var pr = repository.findAll(PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "dataPublicacao", "id")));
            if (pr.isEmpty()) break;
            out.addAll(pr.getContent());
            if (!pr.hasNext()) break;
            page++;
        }

        if (out.size() > max) {
            return out.subList(0, max);
        }
        return out;
    }

    

    static final class Index {
        final List<Doc> docs;
        final Map<String, Integer> df; 
        final double avgdl;
        final long maxId;

        private Index(List<Doc> docs, Map<String, Integer> df, double avgdl, long maxId) {
            this.docs = docs;
            this.df = df;
            this.avgdl = avgdl;
            this.maxId = maxId;
        }

        static Index empty() {
            return new Index(List.of(), Map.of(), 0.0, 0L);
        }

        static Index build(List<Precedente> precedentes) {
            if (precedentes == null || precedentes.isEmpty()) return empty();

            ArrayList<Doc> docs = new ArrayList<>(precedentes.size());
            HashMap<String, Integer> df = new HashMap<>(8_192);
            long totalLen = 0;
            long maxId = 0L;

            for (Precedente p : precedentes) {
                if (p == null) continue;
                Doc d = Doc.from(p);
                docs.add(d);
                totalLen += d.len;

                if (d.id != null) {
                    maxId = Math.max(maxId, d.id);
                }

                
                for (String term : d.uniqueTerms) {
                    df.merge(term, 1, Integer::sum);
                }
            }
            double avgdl = docs.isEmpty() ? 0.0 : ((double) totalLen / (double) docs.size());
            return new Index(Collections.unmodifiableList(docs), Collections.unmodifiableMap(df), avgdl, maxId);
        }

        List<JurisprudenceSearchHit> search(String query, RamoDireito ramo, RitoProcessual rito, int topK) {
            List<String> qTokens = TextTokenUtils.orderedTokens(query);
            if (qTokens.isEmpty() || docs.isEmpty()) return List.of();

            int N = docs.size();
            double avgdlLocal = avgdl <= 0.0 ? 1.0 : avgdl;

            
            double k1 = 1.2;
            double b = 0.75;

            ArrayList<ScoredDoc> scored = new ArrayList<>(Math.min(2_000, docs.size()));
            for (Doc d : docs) {
                if (d == null) continue;
                if (ramo != null && d.ramoSugerido != null && d.ramoSugerido != ramo) continue;
                if (rito != null && d.ritoSugerido != null && d.ritoSugerido != rito) continue;

                double score = 0.0;
                for (String term : qTokens) {
                    Integer tf = d.tf.get(term);
                    if (tf == null || tf <= 0) continue;
                    int dfTerm = df.getOrDefault(term, 0);
                    double idf = Math.log(1.0 + ((N - dfTerm + 0.5) / (dfTerm + 0.5)));
                    double denom = tf + k1 * (1.0 - b + b * ((double) d.len / avgdlLocal));
                    score += idf * (tf * (k1 + 1.0)) / (denom <= 0.0 ? 1.0 : denom);
                }

                if (score <= 0.0) continue;

                
                double titleBoost = phraseBoost(query, d.tituloNorm);
                score *= (1.0 + (0.10 * titleBoost));

                
                score *= (1.0 + (0.08 * recencyBoost(d.dataPublicacao)));

                scored.add(new ScoredDoc(d, score));
            }

            scored.sort((a, b2) -> Double.compare(b2.score, a.score));
            if (scored.isEmpty()) return List.of();

            ArrayList<JurisprudenceSearchHit> out = new ArrayList<>(Math.min(topK, scored.size()));
            double maxScore = scored.get(0).score;
            for (int i = 0; i < scored.size() && out.size() < topK; i++) {
                ScoredDoc sd = scored.get(i);
                
                double norm = maxScore <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, sd.score / maxScore));
                Doc d = sd.doc;
                out.add(new JurisprudenceSearchHit(
                        d.id,
                        d.fonte,
                        d.tipo,
                        d.identificador,
                        d.titulo,
                        d.tese,
                        d.ementaResumo,
                        d.urlReferencia,
                        d.dataPublicacao,
                        d.ramoSugerido,
                        d.ritoSugerido,
                        norm
                ));
            }
            return Collections.unmodifiableList(out);
        }

        private static double phraseBoost(String q, String titleNorm) {
            if (q == null || q.isBlank() || titleNorm == null || titleNorm.isBlank()) return 0.0;
            String nq = normalizeSimple(q);
            if (nq.isBlank()) return 0.0;
            if (titleNorm.contains(nq)) return 1.0;
            
            Set<String> qt = TextTokenUtils.tokens(q);
            Set<String> tt = TextTokenUtils.tokens(titleNorm);
            if (qt.isEmpty() || tt.isEmpty()) return 0.0;
            int inter = 0;
            for (String t : qt) if (tt.contains(t)) inter++;
            return Math.max(0.0, Math.min(1.0, (double) inter / (double) qt.size()));
        }

        private static double recencyBoost(LocalDate date) {
            if (date == null) return 0.0;
            long days = Math.abs(ChronoUnit.DAYS.between(date, LocalDate.now()));
            
            double years = (double) days / 365.25;
            double v = Math.exp(-years / 5.0);
            return Math.max(0.0, Math.min(1.0, v));
        }

        private static String normalizeSimple(String raw) {
            if (raw == null) return "";
            String s = raw.trim().toLowerCase(Locale.ROOT);
            s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
            s = s.replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
            return s;
        }

        private record ScoredDoc(Doc doc, double score) {}
    }

    static final class Doc {
        final Long id;
        final com.tcc.pjb.backend.model.entity.enums.TribunalFonte fonte;
        final com.tcc.pjb.backend.model.entity.enums.TipoPrecedente tipo;
        final String identificador;
        final String titulo;
        final String tese;
        final String ementaResumo;
        final String urlReferencia;
        final LocalDate dataPublicacao;
        final RamoDireito ramoSugerido;
        final RitoProcessual ritoSugerido;
        final Map<String, Integer> tf;
        final Set<String> uniqueTerms;
        final int len;
        final String tituloNorm;

        private Doc(Long id,
                    com.tcc.pjb.backend.model.entity.enums.TribunalFonte fonte,
                    com.tcc.pjb.backend.model.entity.enums.TipoPrecedente tipo,
                    String identificador,
                    String titulo,
                    String tese,
                    String ementaResumo,
                    String urlReferencia,
                    LocalDate dataPublicacao,
                    RamoDireito ramoSugerido,
                    RitoProcessual ritoSugerido,
                    Map<String, Integer> tf,
                    Set<String> uniqueTerms,
                    int len,
                    String tituloNorm) {
            this.id = id;
            this.fonte = fonte;
            this.tipo = tipo;
            this.identificador = identificador;
            this.titulo = titulo;
            this.tese = tese;
            this.ementaResumo = ementaResumo;
            this.urlReferencia = urlReferencia;
            this.dataPublicacao = dataPublicacao;
            this.ramoSugerido = ramoSugerido;
            this.ritoSugerido = ritoSugerido;
            this.tf = tf;
            this.uniqueTerms = uniqueTerms;
            this.len = len;
            this.tituloNorm = tituloNorm;
        }

        static Doc from(Precedente p) {
            String docText = join(p.getTitulo(), p.getTese(), p.getEmentaResumo());
            List<String> toks = TextTokenUtils.orderedTokens(docText);
            HashMap<String, Integer> tf = new HashMap<>(Math.min(2048, toks.size() * 2));
            for (String t : toks) {
                tf.merge(t, 1, Integer::sum);
            }
            Set<String> uniq = new HashSet<>(tf.keySet());
            String tnorm = Index.normalizeSimple(p.getTitulo());
            return new Doc(
                    p.getId(),
                    p.getFonte(),
                    p.getTipo(),
                    safe(p.getIdentificador()),
                    safe(p.getTitulo()),
                    safe(p.getTese()),
                    safe(p.getEmentaResumo()),
                    safe(p.getUrlReferencia()),
                    p.getDataPublicacao(),
                    p.getRamoSugerido(),
                    p.getRitoSugerido(),
                    Collections.unmodifiableMap(tf),
                    Collections.unmodifiableSet(uniq),
                    toks.size(),
                    tnorm
            );
        }

        private static String join(String... parts) {
            if (parts == null || parts.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                String s = safe(p);
                if (s == null) continue;
                if (!sb.isEmpty()) sb.append('\n');
                sb.append(s);
            }
            return sb.toString();
        }

        private static String safe(String v) {
            if (v == null) return null;
            String s = v.trim();
            return s.isBlank() ? null : s;
        }
    }
}
