package com.tcc.pjb.backend.service.ajuizamento.federal;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.repository.JurisdicaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class FederalismoRedistribuicaoService {

    private static final Duration REPORT_CACHE_TTL = Duration.ofSeconds(20);
    private static final int MAX_REPORT_CACHE = 16;

    private final JurisdicaoRepository jurisdicaoRepository;
    private final ProcessoRepository processoRepository;
    private final ConcurrentHashMap<Double, CachedReport> reportCache = new ConcurrentHashMap<>();

    public FederalismoRedistribuicaoService(JurisdicaoRepository jurisdicaoRepository,
                                            ProcessoRepository processoRepository) {
        this.jurisdicaoRepository = Objects.requireNonNull(jurisdicaoRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @PjbTransactionalBudget(operation = "federalismo.redistribuicao.sugerir", maxMillis = 8000)
    @Transactional(readOnly = true)
    public RedistribuicaoFederativaReport sugerir(double indiceCritico) {
        double corte = indiceCritico <= 0.0d ? 0.85d : Math.min(0.98d, Math.max(0.50d, indiceCritico));
        CachedReport cache = reportCache.get(corte);
        if (isFresh(cache)) {
            return cache.report();
        }
        trimCache();
        Instant now = Instant.now();
        LocalDateTime staleThreshold = LocalDateTime.now().minus(90, ChronoUnit.DAYS);
        List<Jurisdicao> jurisdicoes = jurisdicaoRepository.findAll();
        Map<Long, JurisdicaoLoad> loadByJurisdicaoId = processoRepository.computeLoadByJurisdicao(staleThreshold)
                .stream()
                .collect(Collectors.toMap(
                        ProcessoRepository.JurisdicaoLoadProjection::getJurisdicaoId,
                        p -> new JurisdicaoLoad(
                                p.getTotalAtivos() == null ? 0L : p.getTotalAtivos(),
                                p.getAtrasoEstrutural() == null ? 0L : p.getAtrasoEstrutural())));
        List<VaraAnalise> analises = new ArrayList<>();
        for (Jurisdicao jurisdicao : jurisdicoes) {
            if (jurisdicao == null || Boolean.FALSE.equals(jurisdicao.getAtivo())) {
                continue;
            }
            JurisdicaoLoad load = loadByJurisdicaoId.getOrDefault(jurisdicao.getId(), JurisdicaoLoad.EMPTY);
            double indice = load.totalAtivos() == 0 ? 0.0d : (double) load.atrasoEstrutural() / (double) load.totalAtivos();
            if (indice > corte) {
                analises.add(new VaraAnalise(
                        jurisdicao.getId(),
                        jurisdicao.getSigla(),
                        jurisdicao.getNome(),
                        jurisdicao.getUf(),
                        jurisdicao.getCidade(),
                        jurisdicao.getMateria() != null ? jurisdicao.getMateria().name() : null,
                        load.totalAtivos(),
                        load.atrasoEstrutural(),
                        indice,
                        localizarCandidatas(jurisdicao, jurisdicoes, loadByJurisdicaoId)
                ));
            }
        }
        List<VaraAnalise> ordenadas = analises.stream()
                .sorted(Comparator.comparingDouble(VaraAnalise::indiceSobrecarga).reversed())
                .toList();
        RedistribuicaoFederativaReport report = new RedistribuicaoFederativaReport(ordenadas, now);
        reportCache.put(corte, new CachedReport(report, now.plus(REPORT_CACHE_TTL)));
        trimCache();
        return report;
    }


    private boolean isFresh(CachedReport cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private void trimCache() {
        Instant now = Instant.now();
        reportCache.entrySet().removeIf(entry -> !isFresh(entry.getValue()) || entry.getValue().expiresAt().isBefore(now));
        if (reportCache.size() <= MAX_REPORT_CACHE) {
            return;
        }
        int overflow = reportCache.size() - MAX_REPORT_CACHE;
        reportCache.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().expiresAt()))
                .limit(overflow)
                .map(java.util.Map.Entry::getKey)
                .toList()
                .forEach(reportCache::remove);
    }

    private List<CandidataRedistribuicao> localizarCandidatas(Jurisdicao origem,
                                                              List<Jurisdicao> jurisdicoes,
                                                              Map<Long, JurisdicaoLoad> loadByJurisdicaoId) {
        return jurisdicoes.stream()
                .filter(destino -> !Objects.equals(destino.getId(), origem.getId()))
                .filter(destino -> Boolean.TRUE.equals(destino.getAtivo()))
                .filter(destino -> Objects.equals(destino.getUf(), origem.getUf()))
                .filter(destino -> destino.getMateria() == origem.getMateria())
                .map(destino -> {
                    JurisdicaoLoad load = loadByJurisdicaoId.getOrDefault(destino.getId(), JurisdicaoLoad.EMPTY);
                    double indice = load.totalAtivos() == 0 ? 0.0d : (double) load.atrasoEstrutural() / (double) load.totalAtivos();
                    return new CandidataRedistribuicao(destino.getId(), destino.getSigla(), destino.getNome(), destino.getCidade(), load.totalAtivos(), indice);
                })
                .sorted(Comparator.comparingDouble(CandidataRedistribuicao::indiceSobrecarga))
                .limit(5)
                .toList();
    }

    private record JurisdicaoLoad(long totalAtivos, long atrasoEstrutural) {
        private static final JurisdicaoLoad EMPTY = new JurisdicaoLoad(0L, 0L);
    }

    public record RedistribuicaoFederativaReport(
            List<VaraAnalise> varasCriticas,
            Instant geradoEm
    ) {
    }

    public record VaraAnalise(
            Long jurisdicaoId,
            String sigla,
            String nome,
            String uf,
            String comarca,
            String materia,
            long totalAtivos,
            long atrasoEstrutural,
            double indiceSobrecarga,
            List<CandidataRedistribuicao> candidatasRedistribuicao
    ) {
    }

    public record CandidataRedistribuicao(
            Long jurisdicaoId,
            String sigla,
            String nome,
            String comarca,
            long totalAtivos,
            double indiceSobrecarga
    ) {
    }

    private record CachedReport(RedistribuicaoFederativaReport report, Instant expiresAt) {
    }
}

