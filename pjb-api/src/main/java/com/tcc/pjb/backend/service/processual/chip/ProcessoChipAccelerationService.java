package com.tcc.pjb.backend.service.processual.chip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProcessoChipAccelerationService {

    public record AcaoChip(
            String chip,
            String acao,
            String mensagem,
            boolean urgente
    ) {}

    public List<AcaoChip> sugerirAcoes(Collection<ProcessoChipTipo> chips) {
        List<AcaoChip> acoes = new ArrayList<>();
        for (ProcessoChipTipo chip : chips) {
            if (chip.geraAlertaUrgente()) {
                acoes.add(new AcaoChip(chip.name(), "PRIORIZAR",
                        "Chip urgente detectado: " + chip.rotulo() + " — processo deve ser priorizado.", true));
            }
            if (chip.requerAcaoCartoraria()) {
                acoes.add(new AcaoChip(chip.name(), "ACAO_CARTORARIA",
                        chip.rotulo() + " — acionar cartório para resolução imediata.", false));
            }
        }
        return acoes;
    }

    public boolean processoPrioritario(Collection<ProcessoChipTipo> chips) {
        return chips.stream().anyMatch(ProcessoChipTipo::geraAlertaUrgente);
    }
}
