package com.tcc.pjb.backend.core.processo.painel.application;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelContextualWidget;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelMalhaNacionalAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelRotaTaticaAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoMalhaNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalRisco;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelMalhaNacionalApplicationService {

    private final ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService;
    private final ProcessoPainelRotaTaticaApplicationService processoPainelRotaTaticaApplicationService;

    public ProcessoPainelMalhaNacionalApplicationService(ProcessoMalhaNacionalApplicationService processoMalhaNacionalApplicationService,
                                                         ProcessoPainelRotaTaticaApplicationService processoPainelRotaTaticaApplicationService) {
        this.processoMalhaNacionalApplicationService = Objects.requireNonNull(processoMalhaNacionalApplicationService);
        this.processoPainelRotaTaticaApplicationService = Objects.requireNonNull(processoPainelRotaTaticaApplicationService);
    }

    public ProcessoPainelMalhaNacionalAggregate detalhar(Long processoId) {
        ProcessoMalhaNacionalAggregate malha = processoMalhaNacionalApplicationService.detalhar(processoId);
        ProcessoPainelRotaTaticaAggregate rotaTatica = processoPainelRotaTaticaApplicationService.detalhar(processoId);
        ArrayList<ProcessoPainelContextualWidget> widgets = new ArrayList<>();
        widgets.add(new ProcessoPainelContextualWidget(
                "MALHA_IDENTIDADE",
                "Malha de identidade jurídica",
                "COUNTER",
                malha.totalVerticesIdentidade() > 0 ? "ATIVA" : "VAZIA",
                malha.hotspots().contains("IDENTIDADE") ? "AMBER" : "BLUE",
                malha.totalVerticesIdentidade() + " vértices mapeados",
                malha.totalProcessosCorrelatos() + " processos correlatos projetados",
                insights(malha, "IDENTIDADE"),
                "/api/v1/processual/unificado/" + processoId + "/malha-nacional"
        ));
        widgets.add(new ProcessoPainelContextualWidget(
                "MALHA_DISTRIBUICAO",
                "Prevenção, conexão e dependência",
                "DECISION",
                malha.travaDistribuicaoOuFluxo() ? "BLOQUEADA" : malha.totalProcessosCorrelatos() > 0 ? "ATENCAO" : "ESTAVEL",
                malha.travaDistribuicaoOuFluxo() ? "RED" : malha.totalProcessosCorrelatos() > 0 ? "AMBER" : "GREEN",
                "Hotspots: " + String.join(", ", malha.hotspots()),
                "Bloqueios: " + malha.totalBloqueios(),
                insights(malha, "DISTRIBUICAO"),
                "/api/v1/processual/unificado/" + processoId + "/rota-tatica"
        ));
        widgets.add(new ProcessoPainelContextualWidget(
                "MALHA_PROBATORIA",
                "Prova, evidência e sigilo",
                "RISK",
                malha.nivelSigiloRecomendado().nivel() > malha.nivelSigiloAtual().nivel() ? "RECLASSIFICAR" : malha.totalDocumentosCriticos() > 0 ? "ATENCAO" : "ESTAVEL",
                malha.nivelSigiloRecomendado().nivel() > malha.nivelSigiloAtual().nivel() ? "RED" : malha.totalDocumentosCriticos() > 0 ? "AMBER" : "GREEN",
                malha.totalDocumentosCriticos() + " documentos críticos",
                malha.nivelSigiloAtual().name() + " -> " + malha.nivelSigiloRecomendado().name(),
                insights(malha, "SIGILO"),
                "/api/v1/processual/unificado/" + processoId + "/sigilo-probatorio"
        ));
        if (!rotaTatica.itens().isEmpty()) {
            widgets.add(new ProcessoPainelContextualWidget(
                    "ROTA_TATICA",
                    "Rota tática operacional",
                    "ACTION",
                    "PRONTA",
                    "BLUE",
                    rotaTatica.itens().getFirst().acao(),
                    rotaTatica.itens().getFirst().fundamento(),
                    rotaTatica.fundamentos(),
                    "/api/v1/processual/unificado/" + processoId + "/rota-tatica"
            ));
        }
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.addAll(malha.fundamentos());
        fundamentos.addAll(rotaTatica.fundamentos());
        return new ProcessoPainelMalhaNacionalAggregate(
                processoId,
                malha.numeroProcesso(),
                malha.travaDistribuicaoOuFluxo() ? "CRITICO" : !malha.riscos().isEmpty() ? "ATENCAO" : "ESTAVEL",
                malha.riscos().size(),
                malha.totalBloqueios(),
                List.copyOf(widgets),
                List.copyOf(fundamentos.stream().limit(40).toList()),
                Instant.now()
        );
    }

    private List<String> insights(ProcessoMalhaNacionalAggregate malha, String dominio) {
        return malha.riscos().stream()
                .filter(risco -> Objects.equals(risco.dominio(), dominio) || risco.fundamentos().stream().anyMatch(f -> f.contains(dominio)))
                .map(ProcessoMalhaNacionalRisco::titulo)
                .limit(4)
                .toList();
    }
}
