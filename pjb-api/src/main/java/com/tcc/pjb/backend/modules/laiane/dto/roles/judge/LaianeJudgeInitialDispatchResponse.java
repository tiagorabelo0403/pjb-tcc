package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Schema(description = "Resposta do despacho inicial assistido pelo Laiane para o magistrado")
@Value
@Builder
public class LaianeJudgeInitialDispatchResponse {

    @Schema(description = "ID do processo alvo do despacho inicial", example = "12345")
    Long processId;

    @Schema(description = "Rito processual aplicável", example = "RITO_ORDINARIO")
    String rito;

    @Schema(description = "Minuta do despacho inicial gerada pelo Laiane",
            example = "Cite-se o réu nos termos do artigo 248 do CPC...")
    String minuta;

    @Size(max = 50)
    @Schema(description = "Travas que impedem ou condicionam o despacho (máx. 50)",
            example = "[\"AUSENCIA_PROCURACAO\",\"VALOR_CAUSA_NAO_INFORMADO\"]")
    List<String> travas;
}
