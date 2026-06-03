package com.tcc.pjb.backend.model.dto.processual.peticionamento.completude;

import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Resultado da pré-validação de completude (modo rascunho, sem efeito no estado do protocolo)")
public record PreValidacaoCompletudeResponse(
        @Schema(description = "Status que resultaria se o protocolo fosse submetido agora")
        ProtocoloCompletudeStatus statusPrevisto,

        @Schema(description = "Protocolo passaria no gate (sem bloqueantes)")
        boolean aprovado,

        @Schema(description = "Versão das regras aplicadas", example = "v1.0")
        String versaoRegras,

        @Schema(description = "Violações detectadas (bloqueantes e advertências)")
        List<ViolacaoCompletudeDto> violacoes
) {
    @Schema(description = "Violação de completude documental")
    public record ViolacaoCompletudeDto(
            @Schema(description = "Código da violação", example = "DOC_OBRIGATORIO_AUSENTE")
            String codigo,
            @Schema(description = "Severidade: BLOQUEANTE ou ADVERTENCIA")
            String severidade,
            @Schema(description = "Campo ou tipo de documento afetado")
            String campo,
            @Schema(description = "Ação corretiva recomendada")
            String acaoCorretiva,
            @Schema(description = "Fundamento normativo da exigência")
            FundamentoDto fundamento
    ) {}

    @Schema(description = "Fundamento normativo da exigência documental")
    public record FundamentoDto(
            String tipo,
            String identificador,
            String resumo,
            String grau
    ) {}
}
