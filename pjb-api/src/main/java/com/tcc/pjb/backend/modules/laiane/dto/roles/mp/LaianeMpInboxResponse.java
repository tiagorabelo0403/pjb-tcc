package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeWorkItemLiteDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

@Schema(description = "Inbox de itens de trabalho do Ministério Público no Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpInboxResponse {
    @Schema(description = "Dica ou orientação contextual para o inbox",
            example = "Você tem 3 ofícios aguardando resposta")
    private String hint;
    @Schema(description = "Número da página atual (base 0)", example = "0")
    private int page;
    @Schema(description = "Tamanho da página", example = "20")
    private int size;
    @Schema(description = "Total de itens no inbox", example = "7")
    private long total;
    @Size(max = 500)
    @Schema(description = "Itens de trabalho da página atual (máx. 500)")
    private List<LaianeWorkItemLiteDto> items;
}
