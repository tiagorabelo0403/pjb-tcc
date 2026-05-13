package com.tcc.pjb.backend.service.processual.observability.metrics;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.dto.processual.observability.business.ProcessBusinessObservabilityResponse;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class ProcessBusinessMetrics {

    private final AtomicLong totalProcessos = new AtomicLong();
    private final AtomicLong ativos = new AtomicLong();
    private final AtomicLong recursais = new AtomicLong();
    private final AtomicLong workItemsPendentes = new AtomicLong();
    private final AtomicLong workItemsVencidos = new AtomicLong();
    private final AtomicLong outboxPendentes = new AtomicLong();
    private final AtomicLong comunicacoesPendentes = new AtomicLong();
    private final AtomicLong caseFilesUnificados = new AtomicLong();
    private final AtomicLong proceedingsMaterializados = new AtomicLong();
    private final AtomicLong proceedingsRecursais = new AtomicLong();
    private final AtomicLong proceedingsExecutorios = new AtomicLong();
    private final AtomicLong staleProceedings = new AtomicLong();
    private final AtomicLong caseFileEvents = new AtomicLong();
    private final AtomicLong caseFilesAttentionRequired = new AtomicLong();
    private final AtomicLong orphanProceedingParents = new AtomicLong();
    private final AtomicLong divergentRootProceedings = new AtomicLong();

    public ProcessBusinessMetrics(MeterRegistry registry) {
        Objects.requireNonNull(registry);
        Gauge.builder("pjb.process.business.total", totalProcessos, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.ativos", ativos, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.recursais", recursais, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.workitems.pendentes", workItemsPendentes, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.workitems.vencidos", workItemsVencidos, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.outbox.pendentes", outboxPendentes, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.comunicacoes.pendentes", comunicacoesPendentes, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.case_files.unificados", caseFilesUnificados, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.case_files.proceedings", proceedingsMaterializados, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.case_files.recursais", proceedingsRecursais, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.case_files.executorios", proceedingsExecutorios, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.case_files.stale_proceedings", staleProceedings, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.case_files.events", caseFileEvents, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.case_files.attention_required", caseFilesAttentionRequired, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.case_files.orphan_parents", orphanProceedingParents, AtomicLong::get).register(registry);
        Gauge.builder("pjb.process.business.case_files.divergent_roots", divergentRootProceedings, AtomicLong::get).register(registry);
    }

    public void publish(ProcessBusinessObservabilityResponse response) {
        if (response == null) {
            return;
        }
        totalProcessos.set(response.totalProcessos());
        ativos.set(response.ativos());
        recursais.set(response.recursais());
        workItemsPendentes.set(response.workItemsPendentes());
        workItemsVencidos.set(response.workItemsVencidos());
        outboxPendentes.set(response.outboxPendentes());
        comunicacoesPendentes.set(response.comunicacoesPendentes());
        caseFilesUnificados.set(response.caseFilesUnificados());
        proceedingsMaterializados.set(response.proceedingsMaterializados());
        proceedingsRecursais.set(response.proceedingsRecursais());
        proceedingsExecutorios.set(response.proceedingsExecutorios());
        staleProceedings.set(response.staleProceedings());
        caseFileEvents.set(response.caseFileEvents());
        caseFilesAttentionRequired.set(response.caseFilesAttentionRequired());
        orphanProceedingParents.set(response.orphanProceedingParents());
        divergentRootProceedings.set(response.divergentRootProceedings());
    }
}
