package com.tcc.pjb.backend.model.dto.magistratura;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import java.util.Map;

public record MagistraturaJudicialActWorkspaceResponse(
        Long userId,
        String nome,
        TipoUsuario tipoUsuario,
        GrauJurisdicao grau,
        EsferaJurisdicao esfera,
        String lane,
        Long processoId,
        String processoNumero,
        List<String> anchors,
        List<MagistraturaJudicialActAvailabilityResponse> acts,
        Map<String, Object> processSignals
) {
}
