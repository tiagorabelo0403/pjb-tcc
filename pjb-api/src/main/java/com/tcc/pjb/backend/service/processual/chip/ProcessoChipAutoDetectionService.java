package com.tcc.pjb.backend.service.processual.chip;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProcessoChipAutoDetectionService {

    public record DetectionInput(
            RitoProcessual rito,
            boolean reuPreso,
            boolean tutelaUrgentePendente,
            boolean partePrioridadeLegal,
            boolean menorEnvolvido,
            boolean envolveAlimentos,
            boolean envolveQuestaoSaude,
            boolean mandadoDevolvido,
            boolean prazoVencido,
            boolean processoRepetitivo,
            boolean comarcaInterior,
            Duration tempoParado,
            boolean execucaoParada
    ) {}

    public Set<ProcessoChipTipo> detectar(DetectionInput input) {
        Set<ProcessoChipTipo> chips = EnumSet.noneOf(ProcessoChipTipo.class);
        if (input.reuPreso()) chips.add(ProcessoChipTipo.REU_PRESO);
        if (input.tutelaUrgentePendente()) chips.add(ProcessoChipTipo.TUTELA_URGENCIA);
        if (input.partePrioridadeLegal()) chips.add(ProcessoChipTipo.IDOSO);
        if (input.menorEnvolvido()) chips.add(ProcessoChipTipo.MENOR_ENVOLVIDO);
        if (input.envolveAlimentos()) chips.add(ProcessoChipTipo.ALIMENTOS);
        if (input.envolveQuestaoSaude()) chips.add(ProcessoChipTipo.SAUDE);
        if (input.mandadoDevolvido()) chips.add(ProcessoChipTipo.MANDADO_DEVOLVIDO);
        if (input.prazoVencido()) chips.add(ProcessoChipTipo.PRAZO_VENCIDO);
        if (input.processoRepetitivo()) chips.add(ProcessoChipTipo.PROCESSO_REPETITIVO);
        if (input.comarcaInterior()) chips.add(ProcessoChipTipo.COMARCA_DO_INTERIOR);
        if (input.execucaoParada()) chips.add(ProcessoChipTipo.EXECUCAO_PARADA);
        if (input.rito().name().contains("SUMARISSIMO")) chips.add(ProcessoChipTipo.RITO_SUMARISSIMO);
        return chips;
    }
}
