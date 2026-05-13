package com.tcc.pjb.backend.core.processo.timeline.application;

import com.tcc.pjb.backend.core.processo.competencia.application.ProcessoCompetenciaMalhaApplicationService;
import com.tcc.pjb.backend.core.processo.competencia.domain.ProcessoCompetenciaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.application.ProcessoDistribuicaoMalhaApplicationService;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaMotivo;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineMalhaAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineMalhaEvento;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoMalhaNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalRisco;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoTimelineMalhaApplicationService {

    private final ProcessoTimelineApplicationService processoTimelineApplicationService;
    private final ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService;
    private final ProcessoCompetenciaMalhaApplicationService processoCompetenciaMalhaApplicationService;
    private final ProcessoDistribuicaoMalhaApplicationService processoDistribuicaoMalhaApplicationService;

    public ProcessoTimelineMalhaApplicationService(ProcessoTimelineApplicationService processoTimelineApplicationService,
                                                   ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService,
                                                   ProcessoCompetenciaMalhaApplicationService processoCompetenciaMalhaApplicationService,
                                                   ProcessoDistribuicaoMalhaApplicationService processoDistribuicaoMalhaApplicationService) {
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
        this.processoMalhaNacionalApplicationService = Objects.requireNonNull(processoMalhaNacionalApplicationService);
        this.processoCompetenciaMalhaApplicationService = Objects.requireNonNull(processoCompetenciaMalhaApplicationService);
        this.processoDistribuicaoMalhaApplicationService = Objects.requireNonNull(processoDistribuicaoMalhaApplicationService);
    }

    @Transactional(readOnly = true)
    public ProcessoTimelineMalhaAggregate detalhar(Long processoId) {
        ProcessoTimelineAggregate timeline = processoTimelineApplicationService.detalhar(processoId);
        ProcessoMalhaNacionalAggregate malha = processoMalhaNacionalApplicationService.detalhar(processoId);
        ProcessoCompetenciaMalhaAggregate competencia = processoCompetenciaMalhaApplicationService.analisar(processoId);
        ProcessoDistribuicaoMalhaAggregate distribuicao = processoDistribuicaoMalhaApplicationService.detalhar(processoId);
        ArrayList<ProcessoTimelineMalhaEvento> eventos = new ArrayList<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();

        for (ProcessoMalhaNacionalRisco risco : malha.riscos()) {
            eventos.add(new ProcessoTimelineMalhaEvento(
                    risco.codigo(),
                    risco.dominio(),
                    risco.severidade(),
                    malha.geradoEm(),
                    risco.bloqueante(),
                    risco.titulo(),
                    risco.acaoSugerida(),
                    "/api/v1/processual/unificado/" + processoId + "/malha-nacional",
                    risco.fundamentos()
            ));
            fundamentos.addAll(risco.fundamentos());
        }
        competencia.itens().forEach(item -> {
            eventos.add(new ProcessoTimelineMalhaEvento(
                    item.codigo(),
                    item.eixo(),
                    item.bloqueante() ? "CRITICO" : item.score() >= 0.8d ? "ALTO" : "ATENCAO",
                    competencia.geradoEm(),
                    item.bloqueante(),
                    item.acao(),
                    detalheCompetencia(item, competencia),
                    "/api/v1/processual/competencia/" + processoId + "/malha",
                    item.fundamentos()
            ));
            fundamentos.addAll(item.fundamentos());
        });
        distribuicao.motivos().stream().filter(ProcessoDistribuicaoMalhaMotivo::bloqueante).forEach(motivo -> {
            eventos.add(new ProcessoTimelineMalhaEvento(
                    motivo.codigo(),
                    motivo.dominio(),
                    motivo.severidade(),
                    distribuicao.geradoEm(),
                    true,
                    motivo.resumo(),
                    distribuicao.acaoPrimaria(),
                    "/api/v1/processual/distribuicao/" + processoId + "/malha",
                    motivo.fundamentos()
            ));
            fundamentos.addAll(motivo.fundamentos());
        });

        if (!timeline.proximoCiclo().isEmpty()) {
            eventos.add(new ProcessoTimelineMalhaEvento(
                    "PROXIMO_MELHOR_ATO_MALHA",
                    "TIMELINE",
                    distribuicao.travaDistribuicao() ? "CRITICO" : "INFO",
                    Instant.now(),
                    distribuicao.travaDistribuicao(),
                    "Próximo ato guiado pela malha",
                    firstNonBlank(distribuicao.acaoPrimaria(), timeline.proximoCiclo().getFirst()),
                    "/api/v1/processual/unificado/" + processoId + "/timeline",
                    timeline.alertas()
            ));
        }

        eventos.sort(Comparator.comparing(ProcessoTimelineMalhaEvento::bloqueante).reversed()
                .thenComparing(ProcessoTimelineMalhaEvento::instante)
                .thenComparing(ProcessoTimelineMalhaEvento::codigo));

        return new ProcessoTimelineMalhaAggregate(
                processoId,
                timeline.identity().numeroProcesso(),
                eventos.size(),
                (int) eventos.stream().filter(ProcessoTimelineMalhaEvento::bloqueante).count(),
                firstNonBlank(distribuicao.acaoPrimaria(), timeline.proximoCiclo().isEmpty() ? "MANTER_TRILHA_ATUAL" : timeline.proximoCiclo().getFirst()),
                malha.hotspots(),
                List.copyOf(eventos),
                List.copyOf(fundamentos.stream().limit(60).toList()),
                Instant.now()
        );
    }

    private String detalheCompetencia(com.tcc.pjb.backend.core.processo.competencia.domain.ProcessoCompetenciaMalhaItem item,
                                      ProcessoCompetenciaMalhaAggregate competencia) {
        LinkedHashSet<String> partes = new LinkedHashSet<>();
        partes.add(item.acao());
        if (!item.destinoTribunal().isBlank()) {
            partes.add(item.destinoTribunal());
        }
        if (!item.destinoUnidade().isBlank()) {
            partes.add(item.destinoUnidade());
        }
        if (!competencia.acaoPrimaria().isBlank()) {
            partes.add(competencia.acaoPrimaria());
        }
        return String.join(" | ", partes);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
