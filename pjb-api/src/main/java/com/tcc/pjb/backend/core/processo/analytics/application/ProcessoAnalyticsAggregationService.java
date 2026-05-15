package com.tcc.pjb.backend.core.processo.analytics.application;

import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProcessoAnalyticsAggregationService {

    private final ProcessoRepository processoRepository;

    public ProcessoAnalyticsAggregationService(ProcessoRepository processoRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Cacheable(cacheNames = "processo_analytics_ramo", key = "#ramo")
    public List<Object[]> agregadosPorRamo(String ramo) {
        return processoRepository.agregadosPorRamo(ramo);
    }

    @Cacheable(cacheNames = "processo_analytics_tribunal", key = "#ramo + ':' + #tribunal")
    public List<Object[]> agregadosPorRamoETribunal(String ramo, String tribunal) {
        return processoRepository.agregadosPorRamoETribunal(ramo, tribunal);
    }
}
