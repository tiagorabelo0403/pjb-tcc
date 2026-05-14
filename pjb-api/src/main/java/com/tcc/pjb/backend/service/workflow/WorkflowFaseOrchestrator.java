package com.tcc.pjb.backend.service.workflow;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class WorkflowFaseOrchestrator {

    public record TransicaoComando(
            UUID processoId,
            RitoProcessual rito,
            FaseProcessual faseAtual,
            FaseProcessual faseAlvo,
            List<String> atos,
            String solicitanteId,
            String motivacao
    ) {}

    public record TransicaoEvento(
            UUID processoId,
            FaseProcessual faseAnterior,
            FaseProcessual faseAtual,
            boolean efetivada,
            List<String> bloqueios,
            List<String> alertas,
            Instant ocorreuEm
    ) {}

    private final FaseAtosObrigatoriosRegistry registry;

    public WorkflowFaseOrchestrator(FaseAtosObrigatoriosRegistry registry) {
        this.registry = registry;
    }

    public TransicaoEvento avancar(TransicaoComando cmd) {
        List<String> atosConfirmados = registry.confirmarAtos(
                cmd.rito(), cmd.faseAtual(), cmd.atos());
        FaseTransicaoPolicy.TransicaoResult resultado = FaseTransicaoPolicy.avaliar(
                new FaseTransicaoPolicy.TransicaoInput(
                        cmd.rito(), cmd.faseAtual(), cmd.faseAlvo(), atosConfirmados));
        return new TransicaoEvento(
                cmd.processoId(),
                cmd.faseAtual(),
                resultado.permitida() ? cmd.faseAlvo() : cmd.faseAtual(),
                resultado.permitida(),
                resultado.bloqueios(),
                resultado.alertas(),
                Instant.now()
        );
    }
}
