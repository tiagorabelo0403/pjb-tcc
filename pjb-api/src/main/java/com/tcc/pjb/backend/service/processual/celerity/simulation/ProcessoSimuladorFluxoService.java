package com.tcc.pjb.backend.service.processual.celerity.simulation;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessoSimuladorFluxoService {

    public record SimulacaoInput(
            UUID processoId,
            RitoProcessual ritoAtual,
            RitoProcessual ritoSimulado,
            FaseProcessual faseAtual,
            Duration tempoPadrao
    ) {}

    public record ImpactoSimulado(
            String campo,
            String valorAntes,
            String valorDepois,
            String descricaoImpacto
    ) {}

    public record SimulacaoResultado(
            UUID processoId,
            RitoProcessual ritoSimulado,
            List<ImpactoSimulado> impactos,
            boolean exigeConfirmacaoHumana,
            String aviso
    ) {}

    public SimulacaoResultado simular(SimulacaoInput input) {
        List<ImpactoSimulado> impactos = new java.util.ArrayList<>();

        if (input.ritoAtual() != input.ritoSimulado()) {
            impactos.add(new ImpactoSimulado("rito",
                    input.ritoAtual().name(), input.ritoSimulado().name(),
                    "Alteração de rito altera prazos, competência, audiência e fluxo de fases."));
        }

        impactos.add(new ImpactoSimulado("fase",
                input.faseAtual().name(), "RECALCULAR",
                "Fase atual pode não ser válida no novo rito — recalcular com motor de distribuição."));

        impactos.add(new ImpactoSimulado("prazo",
                input.tempoPadrao().toDays() + " dias",
                "DEPENDE DO RITO SIMULADO",
                "SLA do rito simulado pode ser diferente — verificar RitoComputavelMatrix."));

        return new SimulacaoResultado(
                input.processoId(),
                input.ritoSimulado(),
                impactos,
                true,
                "SIMULAÇÃO: nenhuma alteração aplicada. Exige revisão e confirmação humana antes de qualquer mudança.");
    }
}
