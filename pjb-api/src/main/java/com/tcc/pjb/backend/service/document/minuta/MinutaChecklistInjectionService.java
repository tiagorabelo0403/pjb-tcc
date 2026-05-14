package com.tcc.pjb.backend.service.document.minuta;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MinutaChecklistInjectionService {

    public record ChecklistItem(
            String descricao,
            boolean obrigatorio,
            boolean concluido
    ) {}

    public record MinutaChecklist(
            List<ChecklistItem> itens,
            boolean minutaApta
    ) {}

    public MinutaChecklist construir(RitoProcessual rito, boolean temPreliminar,
            boolean provasEncerradas, boolean partesOuvidas) {
        List<ChecklistItem> itens = new ArrayList<>();
        itens.add(new ChecklistItem("Relatório revisado", true, true));
        itens.add(new ChecklistItem("Fundamentação completa", true, provasEncerradas));
        itens.add(new ChecklistItem("Dispositivo claro e executável", true, false));
        itens.add(new ChecklistItem("Custas e honorários definidos", false, false));

        if (temPreliminar) {
            itens.add(new ChecklistItem("Preliminar processada antes do mérito", true, false));
        }
        if (rito.name().contains("PENAL") || rito.name().contains("CRIMINAL")) {
            itens.add(new ChecklistItem("Dosimetria da pena fundamentada", true, false));
            itens.add(new ChecklistItem("Regime inicial de cumprimento definido", true, false));
        }
        if (rito.isFamiliaSucessoes()) {
            itens.add(new ChecklistItem("Interesse de menores verificado", true, partesOuvidas));
        }

        boolean apta = itens.stream().filter(ChecklistItem::obrigatorio).allMatch(ChecklistItem::concluido);
        return new MinutaChecklist(itens, apta);
    }
}
