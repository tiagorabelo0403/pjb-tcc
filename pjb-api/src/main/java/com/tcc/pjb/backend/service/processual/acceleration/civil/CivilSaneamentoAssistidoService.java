package com.tcc.pjb.backend.service.processual.acceleration.civil;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CivilSaneamentoAssistidoService {

    public record SaneamentoInput(
            boolean preliminaresResolvidas,
            boolean pontosControvertidosDefinidos,
            boolean provasRequeridas,
            boolean onusDistribuido,
            boolean cabePericiaContabil,
            boolean cabePericiaEngenharia,
            boolean acordoProvavel,
            boolean cabeJulgamentoAntecipado
    ) {}

    public record ItemChecklist(
            String descricao,
            boolean concluido,
            boolean exigeDecisaoJudicial
    ) {}

    public record SaneamentoSnapshot(
            List<ItemChecklist> checklist,
            int percentualConcluido,
            boolean aptoParaInstrucao
    ) {}

    public SaneamentoSnapshot avaliar(SaneamentoInput input) {
        List<ItemChecklist> checklist = new ArrayList<>();
        checklist.add(new ItemChecklist("Preliminares resolvidas", input.preliminaresResolvidas(), true));
        checklist.add(new ItemChecklist("Pontos controvertidos definidos", input.pontosControvertidosDefinidos(), true));
        checklist.add(new ItemChecklist("Provas requeridas avaliadas", input.provasRequeridas(), true));
        checklist.add(new ItemChecklist("Ônus da prova distribuído", input.onusDistribuido(), true));
        if (input.cabePericiaContabil()) {
            checklist.add(new ItemChecklist("Perícia contábil requerida — designar perito", false, true));
        }
        if (input.cabePericiaEngenharia()) {
            checklist.add(new ItemChecklist("Perícia de engenharia requerida — designar perito", false, true));
        }

        long concluidos = checklist.stream().filter(ItemChecklist::concluido).count();
        int percentual = checklist.isEmpty() ? 100 : (int) (concluidos * 100 / checklist.size());
        boolean apto = input.preliminaresResolvidas() && input.pontosControvertidosDefinidos();

        return new SaneamentoSnapshot(checklist, percentual, apto);
    }
}
