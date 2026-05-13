package com.tcc.pjb.backend.service.engine;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;

@Component
public class LegalRhythmEngine {

    public boolean isConclusoParaSentenca(Processo processo) {
        
        
        return processo.getDataUltimaMovimentacao() != null
                && processo.getFaseAtual() == FaseProcessual.CONHECIMENTO;
        
    }

    public boolean isPeriodoSuspensao(Jurisdicao jurisdicao) {
        LocalDate hoje = LocalDate.now();
        
        if ((hoje.getMonthValue() == 12 && hoje.getDayOfMonth() >= 20) ||
                (hoje.getMonthValue() == 1 && hoje.getDayOfMonth() <= 20)) {
            return true;
        }
        
        return hoje.getDayOfWeek() == DayOfWeek.SATURDAY || hoje.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}