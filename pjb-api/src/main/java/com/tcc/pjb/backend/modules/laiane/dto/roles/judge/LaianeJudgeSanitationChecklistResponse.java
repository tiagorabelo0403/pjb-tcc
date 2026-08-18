package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

@Schema(description = "Checklist de saneamento processual gerado pelo Laiane para o magistrado")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeJudgeSanitationChecklistResponse {
    @Schema(description = "ID do processo saneado", example = "12345")
    private Long processoId;
    @Size(max = 100)
    @Schema(description = "Itens do checklist de saneamento (máx. 100)",
            example = "[\"Verificar legitimidade das partes\",\"Conferir representação processual\"]")
    private List<String> checklist;
}
