package com.tcc.pjb.backend.service.gabinete.acceleration;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GabinetePreWorkPackageService {

    public record PreWorkInput(
            UUID processoId,
            String numeroProcesso,
            RitoProcessual rito,
            String resumoPedidos,
            List<String> pecasRelevantes,
            List<String> precedentesAplicaveis,
            boolean temMinutaBase
    ) {}

    public record PreWorkPackage(
            UUID processoId,
            String numeroProcesso,
            RitoProcessual rito,
            String resumoExecutivo,
            List<String> pecasRelevantes,
            List<String> precedentes,
            boolean minutaBaseDisponivel,
            String instrucaoGabinete
    ) {}

    public PreWorkPackage montar(PreWorkInput input) {
        String resumo = montarResumo(input);
        String instrucao = montarInstrucao(input);
        return new PreWorkPackage(
                input.processoId(),
                input.numeroProcesso(),
                input.rito(),
                resumo,
                input.pecasRelevantes(),
                input.precedentesAplicaveis(),
                input.temMinutaBase(),
                instrucao);
    }

    private String montarResumo(PreWorkInput input) {
        return "Processo " + input.numeroProcesso() + " — Rito: " + input.rito().name()
                + " | Pedidos: " + (input.resumoPedidos() != null ? input.resumoPedidos() : "não informado");
    }

    private String montarInstrucao(PreWorkInput input) {
        if (input.temMinutaBase()) return "Minuta-base disponível — revisar e assinar.";
        if (!input.precedentesAplicaveis().isEmpty()) return "Há " + input.precedentesAplicaveis().size() + " precedente(s) possivelmente aplicável(is).";
        return "Processo pronto para análise e decisão.";
    }
}
