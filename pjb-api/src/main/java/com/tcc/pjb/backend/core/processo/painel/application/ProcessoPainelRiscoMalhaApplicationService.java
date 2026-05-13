package com.tcc.pjb.backend.core.processo.painel.application;

import com.tcc.pjb.backend.core.processo.anomalia.application.ProcessoAnomaliaMalhaApplicationService;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.application.ProcessoDistribuicaoMalhaApplicationService;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelContextualWidget;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelMalhaNacionalAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelRiscoMalhaAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineMalhaApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineMalhaAggregate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoPainelRiscoMalhaApplicationService {

    private final ProcessoPainelMalhaNacionalApplicationService processoPainelMalhaNacionalApplicationService;
    private final ProcessoAnomaliaMalhaApplicationService processoAnomaliaMalhaApplicationService;
    private final ProcessoDistribuicaoMalhaApplicationService processoDistribuicaoMalhaApplicationService;
    private final ProcessoTimelineMalhaApplicationService processoTimelineMalhaApplicationService;

    public ProcessoPainelRiscoMalhaApplicationService(ProcessoPainelMalhaNacionalApplicationService processoPainelMalhaNacionalApplicationService,
                                                      ProcessoAnomaliaMalhaApplicationService processoAnomaliaMalhaApplicationService,
                                                      ProcessoDistribuicaoMalhaApplicationService processoDistribuicaoMalhaApplicationService,
                                                      ProcessoTimelineMalhaApplicationService processoTimelineMalhaApplicationService) {
        this.processoPainelMalhaNacionalApplicationService = Objects.requireNonNull(processoPainelMalhaNacionalApplicationService);
        this.processoAnomaliaMalhaApplicationService = Objects.requireNonNull(processoAnomaliaMalhaApplicationService);
        this.processoDistribuicaoMalhaApplicationService = Objects.requireNonNull(processoDistribuicaoMalhaApplicationService);
        this.processoTimelineMalhaApplicationService = Objects.requireNonNull(processoTimelineMalhaApplicationService);
    }

    @Transactional(readOnly = true)
    public ProcessoPainelRiscoMalhaAggregate detalhar(Long processoId) {
        ProcessoPainelMalhaNacionalAggregate painelBase = processoPainelMalhaNacionalApplicationService.detalhar(processoId);
        ProcessoAnomaliaMalhaAggregate anomalia = processoAnomaliaMalhaApplicationService.detalhar(processoId);
        ProcessoDistribuicaoMalhaAggregate distribuicao = processoDistribuicaoMalhaApplicationService.detalhar(processoId);
        ProcessoTimelineMalhaAggregate timeline = processoTimelineMalhaApplicationService.detalhar(processoId);
        ArrayList<ProcessoPainelContextualWidget> widgets = new ArrayList<>(painelBase.widgets());
        widgets.add(new ProcessoPainelContextualWidget(
                "MALHA_ANOMALIA",
                "Radar de anomalia e fraude",
                "RISK",
                anomalia.nivelGlobal(),
                accent(anomalia.nivelGlobal()),
                anomalia.scoreGlobal() + " pontos de risco",
                anomalia.itens().isEmpty() ? "Sem anomalias materiais abertas" : anomalia.itens().getFirst().titulo(),
                anomalia.itens().stream().map(item -> item.codigo() + " — " + item.titulo()).limit(4).toList(),
                "/api/v1/processual/anomalia/" + processoId + "/malha"
        ));
        widgets.add(new ProcessoPainelContextualWidget(
                "MALHA_TIMELINE_VIVA",
                "Timeline viva da malha",
                "TIMELINE",
                timeline.totalBloqueiosMalha() > 0 ? "BLOQUEADA" : "ATIVA",
                timeline.totalBloqueiosMalha() > 0 ? "RED" : "BLUE",
                timeline.proximaAcaoOperacional(),
                timeline.totalEventosMalha() + " eventos correlacionados",
                timeline.eventos().stream().map(item -> item.titulo()).limit(4).toList(),
                "/api/v1/processual/timeline/" + processoId + "/malha"
        ));
        widgets.add(new ProcessoPainelContextualWidget(
                "MALHA_DISTRIBUICAO_EXECUTIVA",
                "Execução da distribuição guiada pela malha",
                "ACTION",
                distribuicao.travaDistribuicao() ? "TRIAGEM" : distribuicao.acaoPrimaria(),
                distribuicao.travaDistribuicao() ? "AMBER" : distribuicao.exigeSigiloReforcado() ? "RED" : "GREEN",
                distribuicao.filaSugerida(),
                distribuicao.inboxSugerida(),
                distribuicao.fundamentos().stream().limit(4).toList(),
                "/api/v1/processual/distribuicao/" + processoId + "/malha"
        ));
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(painelBase.fundamentos());
        fundamentos.addAll(anomalia.fundamentos());
        fundamentos.addAll(distribuicao.fundamentos());
        fundamentos.addAll(timeline.fundamentos());
        int scoreGlobal = Math.max(anomalia.scoreGlobal(), distribuicao.prioridade() * 20 + (timeline.totalBloqueiosMalha() * 7));
        boolean possuiBloqueio = painelBase.totalBloqueios() > 0 || distribuicao.travaDistribuicao() || timeline.totalBloqueiosMalha() > 0;
        String statusGeral = possuiBloqueio ? "CRITICO" : scoreGlobal >= 75 ? "ALTO" : scoreGlobal >= 45 ? "ATENCAO" : "ESTAVEL";
        return new ProcessoPainelRiscoMalhaAggregate(
                processoId,
                painelBase.numeroProcesso(),
                statusGeral,
                scoreGlobal,
                possuiBloqueio,
                List.copyOf(widgets),
                List.copyOf(fundamentos.stream().limit(60).toList()),
                Instant.now()
        );
    }

    private String accent(String nivel) {
        if (Objects.equals(nivel, "CRITICO")) {
            return "RED";
        }
        if (Objects.equals(nivel, "ALTO")) {
            return "AMBER";
        }
        if (Objects.equals(nivel, "ATENCAO")) {
            return "ORANGE";
        }
        return "GREEN";
    }
}
