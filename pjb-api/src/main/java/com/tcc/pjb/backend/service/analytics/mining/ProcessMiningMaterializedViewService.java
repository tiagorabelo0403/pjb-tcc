package com.tcc.pjb.backend.service.analytics.mining;

import com.tcc.pjb.backend.platform.concurrent.PjbVirtualThreadSpine;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class ProcessMiningMaterializedViewService {

    public enum DimensaoGargalo {
        POR_TIPO_ATO, POR_INTEGRACAO, POR_RITO, POR_FASE, POR_DOCUMENTO, POR_COMUNICACAO
    }

    public record MiningEvent(
            String tipoAto,
            String sistemaOrigem,
            String rito,
            String fase,
            String categoriaDocumento,
            String canalComunicacao,
            double duracaoHoras
    ) {}

    public record MiningEventLog(List<MiningEvent> eventos) {}

    public record GargaloMetric(
            String chave,
            long quantidade,
            double tempoMedioHoras,
            double impactoRelativo
    ) {}

    public record MaterializedSnapshot(
            Instant atualizadoEm,
            Map<DimensaoGargalo, List<GargaloMetric>> gargalosPorDimensao,
            long totalEventos,
            boolean parcial
    ) {}

    private final AtomicReference<MaterializedSnapshot> snapshot =
            new AtomicReference<>(emptySnapshot());
    private final ExecutorService executor;

    public ProcessMiningMaterializedViewService() {
        this.executor = PjbVirtualThreadSpine.newPerTaskExecutor("pjb-mining-mv");
    }

    public MaterializedSnapshot atual() {
        return snapshot.get();
    }

    public void atualizarAsync(MiningEventLog log) {
        executor.execute(() -> recalcular(log));
    }

    private void recalcular(MiningEventLog log) {
        Map<DimensaoGargalo, List<GargaloMetric>> mapa = new ConcurrentHashMap<>();
        EnumSet<DimensaoGargalo> dimensoes = EnumSet.allOf(DimensaoGargalo.class);
        for (DimensaoGargalo d : dimensoes) {
            mapa.put(d, calcularDimensao(d, log));
        }
        snapshot.set(new MaterializedSnapshot(Instant.now(), mapa, log.eventos().size(), false));
    }

    private List<GargaloMetric> calcularDimensao(DimensaoGargalo d, MiningEventLog log) {
        return switch (d) {
            case POR_TIPO_ATO -> agrupar(log, MiningEvent::tipoAto);
            case POR_INTEGRACAO -> agrupar(log, MiningEvent::sistemaOrigem);
            case POR_RITO -> agrupar(log, MiningEvent::rito);
            case POR_FASE -> agrupar(log, MiningEvent::fase);
            case POR_DOCUMENTO -> agrupar(log, MiningEvent::categoriaDocumento);
            case POR_COMUNICACAO -> agrupar(log, MiningEvent::canalComunicacao);
        };
    }

    private List<GargaloMetric> agrupar(MiningEventLog log, Function<MiningEvent, String> keyFn) {
        Map<String, Long> counts = new ConcurrentHashMap<>();
        Map<String, Double> tempos = new ConcurrentHashMap<>();
        for (MiningEvent e : log.eventos()) {
            String k = keyFn.apply(e);
            if (k == null || k.isBlank()) continue;
            counts.merge(k, 1L, Long::sum);
            tempos.merge(k, e.duracaoHoras(), Double::sum);
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return counts.entrySet().stream()
                .map(en -> new GargaloMetric(
                        en.getKey(),
                        en.getValue(),
                        en.getValue() > 0 ? tempos.getOrDefault(en.getKey(), 0.0) / en.getValue() : 0,
                        total > 0 ? (double) en.getValue() / total : 0))
                .sorted((a, b) -> Double.compare(b.impactoRelativo(), a.impactoRelativo()))
                .toList();
    }

    private static MaterializedSnapshot emptySnapshot() {
        return new MaterializedSnapshot(Instant.EPOCH, Map.of(), 0, true);
    }
}
