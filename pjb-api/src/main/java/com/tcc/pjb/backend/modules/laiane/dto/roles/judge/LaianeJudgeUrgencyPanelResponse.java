package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeWorkItemLiteDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

@Schema(description = "Painel de urgências processuais do magistrado no Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeJudgeUrgencyPanelResponse {
    @Size(max = 50)
    @Schema(description = "Itens de trabalho com urgência máxima (máx. 50)")
    private List<LaianeWorkItemLiteDto> items;
    @Schema(description = "Dica ou orientação contextual sobre as urgências",
            example = "2 processos com prazo vencendo hoje")
    private String hint;
}
