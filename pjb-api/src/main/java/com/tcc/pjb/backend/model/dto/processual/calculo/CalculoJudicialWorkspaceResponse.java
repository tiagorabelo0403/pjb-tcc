package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CalculoJudicialWorkspaceResponse(
        String abaPadrao,
        String titulo,
        String subtitulo,
        CalculoJudicialSolicitantePerfil perfilResolvido,
        List<String> abasDisponiveis,
        List<String> mensagensGlobais,
        List<CalculoJudicialWorkspaceCardResponse> calculadoras,
        List<String> comportamentoDiario,
        List<String> guardrailsIa,
        @Schema(description = "Configuracao de design da navegacao do workspace de calculo — varia por preferencia do usuario e tema institucional", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> designNavegacao,
        Instant geradoEm
) {
}

