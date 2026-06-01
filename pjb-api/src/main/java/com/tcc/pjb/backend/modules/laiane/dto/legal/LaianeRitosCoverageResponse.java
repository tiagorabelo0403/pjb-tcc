package com.tcc.pjb.backend.modules.laiane.dto.legal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Schema(description = "Cobertura de ritos processuais suportados pelo Laiane")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeRitosCoverageResponse {
    @Size(max = 200)
    @Schema(description = "Ritos processuais com suporte completo no Laiane (máx. 200)",
            example = "[\"RITO_ORDINARIO\",\"RITO_SUMARIO\"]")
    @Builder.Default
    private List<String> supported = new ArrayList<>();

    @Size(max = 100)
    @Schema(description = "Ritos com definição de pacote de documentação faltante (máx. 100)",
            example = "[\"RITO_ESPECIAL_FALENCIA\"]")
    @Builder.Default
    private List<String> missingPackDefinition = new ArrayList<>();
}
