package com.tcc.pjb.backend.model.dto.consultapublica;

import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoOverviewResponse;
import com.tcc.pjb.backend.model.dto.publico.PrazoRealPredictionResponse;
import com.tcc.pjb.backend.model.dto.timeline.TimelineItemResponse;
import java.util.List;

public record ConsultaPublicaPersonalCockpitSpotlightDto(
        Long processoId,
        String processoNumero,
        String colorBand,
        CidadaoProcessoOverviewResponse overview,
        PrazoRealPredictionResponse prazoReal,
        ConsultaPublicaPersonalCalendarDigestDto calendar,
        List<TimelineItemResponse> timeline,
        List<ConsultaPublicaPersonalProcessTagDto> tags,
        long notesCount,
        List<ConsultaPublicaWorkspaceActionDto> actions,
        List<String> warnings
) {
}
