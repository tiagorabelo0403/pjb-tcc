package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record CalculoJudicialAssistenciaResponse(
        String dominio,
        CalculoJudicialSolicitantePerfil perfilResolvido,
        String titulo,
        String mensagemAbertura,
        List<String> mensagensAjuda,
        List<String> camposCriticosPendentes,
        List<String> validacoesBloqueantes,
        List<String> ajustesAutomaticosSugeridos,
        List<String> proximosPassos,
        @Schema(description = "Sugestoes de auto-preenchimento validadas — chaves dependem do formulario do rito processual", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> autopreenchimentoSeguro,
        @Schema(description = "Configuracao visual assistida do formulario — hints de UI por perfil de magistrado e tribunal", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> desenhoAssistido,
        List<String> guardrailsIa,
        Instant geradoEm
) {
}

