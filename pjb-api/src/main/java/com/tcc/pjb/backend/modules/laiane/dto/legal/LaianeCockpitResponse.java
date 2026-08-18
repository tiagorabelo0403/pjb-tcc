package com.tcc.pjb.backend.modules.laiane.dto.legal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.*;

@Schema(description = "Cockpit de orientação processual gerado pelo Laiane para o advogado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeCockpitResponse {

    @Schema(description = "Inferência de rito processual realizada pelo Laiane")
    @Builder.Default
    private Map<String, Object> ritoInference = new LinkedHashMap<>();

    @Size(max = 50)
    @Schema(description = "Playbook de ações recomendadas pelo Laiane (máx. 50)")
    @Builder.Default
    private List<LaianePlaybookItemDto> playbook = new ArrayList<>();

    @Size(max = 20)
    @Schema(description = "Diferenciais estratégicos identificados para o caso (máx. 20)",
            example = "[\"Confissão espontânea favorece atenuante\"]")
    @Builder.Default
    private List<String> differentiators = new ArrayList<>();
}
