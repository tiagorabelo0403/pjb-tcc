package com.tcc.pjb.backend.service.processual.acceleration.familia;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FamiliaAlimentosChecklistService {

    public record AlimentosInput(
            boolean comprovanteRendaJuntado,
            boolean declaracaoHipossuficienciaPresentada,
            boolean calculosPensioProposto,
            boolean parteCitada,
            boolean respostaApresentada,
            boolean audienciaMarcada,
            boolean acordoAlimentarTentado
    ) {}

    public record ItemAlimentos(
            String descricao,
            boolean concluido,
            boolean bloqueante
    ) {}

    public record ChecklistAlimentos(
            List<ItemAlimentos> itens,
            boolean aptoParaDecisao,
            String proximaAcao
    ) {}

    public ChecklistAlimentos avaliar(AlimentosInput input) {
        List<ItemAlimentos> itens = new ArrayList<>();
        itens.add(new ItemAlimentos("Comprovante de renda juntado", input.comprovanteRendaJuntado(), true));
        itens.add(new ItemAlimentos("Declaração de hipossuficiência", input.declaracaoHipossuficienciaPresentada(), false));
        itens.add(new ItemAlimentos("Cálculos de pensão propostos", input.calculosPensioProposto(), false));
        itens.add(new ItemAlimentos("Parte citada", input.parteCitada(), true));
        itens.add(new ItemAlimentos("Resposta apresentada", input.respostaApresentada(), false));
        itens.add(new ItemAlimentos("Audiência marcada", input.audienciaMarcada(), false));
        itens.add(new ItemAlimentos("Acordo tentado", input.acordoAlimentarTentado(), false));

        boolean bloqueantePendente = itens.stream()
                .anyMatch(i -> i.bloqueante() && !i.concluido());
        boolean apto = !bloqueantePendente && input.respostaApresentada();

        String proxima = bloqueantePendente
                ? "Regularizar itens bloqueantes antes de avançar."
                : apto ? "Processo apto para decisão sobre alimentos." : "Aguardar resposta ou audiência.";

        return new ChecklistAlimentos(itens, apto, proxima);
    }
}
