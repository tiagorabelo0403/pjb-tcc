package com.tcc.pjb.backend.service.gabinete.acceleration;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MagistradoWorkloadInsightService {

    public record WorkloadInput(
            UUID magistradoId,
            int totalProcessos,
            int processosProntos,
            int processosCriticos,
            Map<RitoProcessual, Integer> distribuicaoPorRito,
            int minutasEmRascunho
    ) {}

    public record WorkloadInsight(
            UUID magistradoId,
            int prontosPautar,
            int criticosPrioritarios,
            List<String> sugestoes,
            String resumoOperacional
    ) {}

    public WorkloadInsight gerar(WorkloadInput input) {
        List<String> sugestoes = new java.util.ArrayList<>();

        if (input.processosCriticos() > 0) {
            sugestoes.add(input.processosCriticos() + " processo(s) crítico(s) exigem análise prioritária.");
        }
        if (input.minutasEmRascunho() > 5) {
            sugestoes.add(input.minutasEmRascunho() + " minutas em rascunho — revisar e assinar.");
        }
        if (input.processosProntos() > 20) {
            sugestoes.add(input.processosProntos() + " processos prontos para pauta — oportunidade de lote de sentenças.");
        }

        String rito = input.distribuicaoPorRito().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().name() + " (" + e.getValue() + ")")
                .orElse("variado");

        String resumo = "Acervo: " + input.totalProcessos() + " processos | Prontos: "
                + input.processosProntos() + " | Rito principal: " + rito;

        return new WorkloadInsight(input.magistradoId(), input.processosProntos(),
                input.processosCriticos(), sugestoes, resumo);
    }
}
