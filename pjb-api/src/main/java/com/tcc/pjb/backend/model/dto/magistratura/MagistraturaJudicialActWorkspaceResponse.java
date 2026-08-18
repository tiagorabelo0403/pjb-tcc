package com.tcc.pjb.backend.model.dto.magistratura;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

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
        @Schema(description = "Sinais do processo para IA de ato judicial — chaves variam por estado processual e tipo de ato", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> processSignals
) {
}

