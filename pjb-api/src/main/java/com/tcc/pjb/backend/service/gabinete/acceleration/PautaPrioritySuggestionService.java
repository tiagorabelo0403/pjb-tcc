package com.tcc.pjb.backend.service.gabinete.acceleration;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PautaPrioritySuggestionService {

    public record ItemPauta(
            UUID processoId,
            String numeroProcesso,
            RitoProcessual rito,
            boolean habeasCorpus,
            boolean mandadoSeguranca,
            boolean reuPreso,
            boolean idosoOuDeficiente,
            int tempoEsperaEmDias
    ) {}

    public record SugestaoOrdenacaoPauta(
            List<ItemPauta> ordenados,
            String criterioAplicado
    ) {}

    public SugestaoOrdenacaoPauta sugerir(List<ItemPauta> itens) {
        List<ItemPauta> ordenados = itens.stream()
                .sorted(Comparator
                        .comparingInt(PautaPrioritySuggestionService::pontuacao).reversed()
                        .thenComparingInt(i -> -i.tempoEsperaEmDias()))
                .toList();
        return new SugestaoOrdenacaoPauta(ordenados,
                "HC/MS > réu preso > idoso/deficiente > tempo de espera");
    }

    private static int pontuacao(ItemPauta item) {
        int score = 0;
        if (item.habeasCorpus()) score += 100;
        if (item.mandadoSeguranca()) score += 80;
        if (item.reuPreso()) score += 60;
        if (item.idosoOuDeficiente()) score += 40;
        return score;
    }
}
