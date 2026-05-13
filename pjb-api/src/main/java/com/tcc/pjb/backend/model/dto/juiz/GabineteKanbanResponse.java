package com.tcc.pjb.backend.model.dto.juiz;

import java.util.List;
import com.tcc.pjb.backend.service.juiz.decision.JuizGabineteDecisionalService;

public record GabineteKanbanResponse(
        List<JuizGabineteDecisionalService.FilaDecisionalItem> decisional,
        List<JuizGabineteDecisionalService.FilaDecisionalItem> assessoria,
        List<JuizGabineteDecisionalService.FilaDecisionalItem> recursal,
        List<JuizGabineteDecisionalService.FilaDecisionalItem> audiencia,
        List<PrazoFatalDto> prazosFatais,
        String loadBand
) {
}
