package com.tcc.pjb.backend.modules.laiane.dto.legal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Schema(description = "Rascunho de documento jurídico gerado pelo Laiane")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeDraftResponse {
    @Schema(description = "Tipo de rascunho gerado",
            example = "PETICAO_INICIAL",
            allowableValues = {"PETICAO_INICIAL", "CONTESTACAO", "RECURSO", "PARECER", "MEMORANDO", "OFICIO"})
    private String kind;
    @Size(max = 500_000)
    @Schema(description = "Conteúdo do rascunho em markdown",
            example = "# Petição Inicial\n\nExmo. Sr. Juiz...")
    private String draftMarkdown;

    @Size(max = 50)
    @Schema(description = "Avisos gerados durante a elaboração do rascunho (máx. 50)",
            example = "[\"Valor da causa deve ser informado\"]")
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
