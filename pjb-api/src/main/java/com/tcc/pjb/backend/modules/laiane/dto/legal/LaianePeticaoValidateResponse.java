package com.tcc.pjb.backend.modules.laiane.dto.legal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Schema(description = "Resultado da validação de petição pelo Laiane")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianePeticaoValidateResponse {
    @Schema(description = "Indica se a petição passou em todas as validações", example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean ok;

    @Size(max = 50)
    @Schema(description = "Erros de validação que impedem o protocolo (máx. 50)",
            example = "[\"Procuração não habilitada\",\"Valor da causa ausente\"]")
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Size(max = 100)
    @Schema(description = "Avisos que não impedem o protocolo mas requerem atenção (máx. 100)",
            example = "[\"Documentos opcionais ausentes\"]")
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
