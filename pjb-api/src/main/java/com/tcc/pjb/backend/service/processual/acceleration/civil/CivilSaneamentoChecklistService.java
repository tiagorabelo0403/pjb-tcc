package com.tcc.pjb.backend.service.processual.acceleration.civil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CivilSaneamentoChecklistService {

    public record SaneamentoChecklistInput(
            UUID processoId,
            boolean preliminaresApreciadas,
            boolean pontosControvertidosSaneados,
            boolean provasNecessariasDefinidas,
            boolean onusProvaDistribuido,
            boolean cabeJulgamentoAntecipado,
            boolean cabeAcordoProvavel,
            boolean periciaRequerida
    ) {}

    public record ItemSaneamento(
            String descricao,
            boolean concluido,
            boolean eAtoJurisdicional
    ) {}

    public record SaneamentoChecklistSnapshot(
            UUID processoId,
            List<ItemSaneamento> itens,
            boolean aptoParaInstrucao,
            boolean sugestaoJulgamentoAntecipado,
            String proximaAcao
    ) {}

    public SaneamentoChecklistSnapshot avaliar(SaneamentoChecklistInput input) {
        List<ItemSaneamento> itens = new ArrayList<>();
        itens.add(new ItemSaneamento("Preliminares apreciadas", input.preliminaresApreciadas(), true));
        itens.add(new ItemSaneamento("Pontos controvertidos saneados", input.pontosControvertidosSaneados(), true));
        itens.add(new ItemSaneamento("Provas necessárias definidas", input.provasNecessariasDefinidas(), true));
        itens.add(new ItemSaneamento("Ônus da prova distribuído", input.onusProvaDistribuido(), true));
        if (input.periciaRequerida()) {
            itens.add(new ItemSaneamento("Perícia requerida — designar perito", false, true));
        }

        boolean apto = input.preliminaresApreciadas() && input.pontosControvertidosSaneados();

        String proxima = input.cabeJulgamentoAntecipado()
                ? "Processo apto para julgamento antecipado do mérito."
                : input.cabeAcordoProvavel()
                        ? "Há indícios de acordo provável — tentar conciliação."
                        : apto ? "Prosseguir com instrução probatória."
                                : "Regularizar pontos do saneamento antes de instruir.";

        return new SaneamentoChecklistSnapshot(input.processoId(), itens, apto,
                input.cabeJulgamentoAntecipado(), proxima);
    }
}
