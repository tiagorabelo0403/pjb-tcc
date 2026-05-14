package com.tcc.pjb.backend.service.perito;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LaudoPericialReadinessService {

    public record LaudoInput(
            UUID processoId,
            UUID peritoId,
            LocalDate dataEntregaPrevista,
            boolean laudoEntregue,
            boolean laudoAssinado,
            boolean honorariosDepositados,
            boolean quesitosRespondidos,
            boolean partesIntimadas
    ) {}

    public record LaudoReadiness(
            UUID processoId,
            boolean apto,
            boolean atrasado,
            long diasAtraso,
            List<String> pendencias,
            String proximaProvidencia
    ) {}

    public LaudoReadiness avaliar(LaudoInput input) {
        List<String> pendencias = new ArrayList<>();
        if (!input.laudoEntregue()) {
            pendencias.add("Laudo não entregue pelo perito.");
        }
        if (input.laudoEntregue() && !input.laudoAssinado()) {
            pendencias.add("Laudo entregue, mas não assinado digitalmente.");
        }
        if (!input.honorariosDepositados()) {
            pendencias.add("Honorários periciais não depositados.");
        }
        if (input.laudoEntregue() && !input.quesitosRespondidos()) {
            pendencias.add("Quesitos não respondidos no laudo — solicitar complementação.");
        }
        if (input.laudoEntregue() && !input.partesIntimadas()) {
            pendencias.add("Partes não intimadas da juntada do laudo.");
        }
        boolean atrasado = !input.laudoEntregue()
                && LocalDate.now().isAfter(input.dataEntregaPrevista());
        long dias = atrasado
                ? input.dataEntregaPrevista().until(LocalDate.now()).getDays() : 0;
        String proxima = pendencias.isEmpty()
                ? "Laudo em ordem. Prosseguir com instrução."
                : pendencias.get(0);
        return new LaudoReadiness(input.processoId(), pendencias.isEmpty(),
                atrasado, dias, List.copyOf(pendencias), proxima);
    }
}
