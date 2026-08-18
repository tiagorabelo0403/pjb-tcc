package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Substabelecimento de procuração judicial para outro advogado")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeSubstabelecimentoRequest {
    @NotNull
    @Schema(description = "ID do advogado destinatário do substabelecimento", example = "77")
    private Long advogadoDestinoId;

    @Schema(description = "Se true, o advogado substabelecente mantém seus próprios poderes (substabelecimento com reserva de poderes)", example = "false")
    private boolean comReservaDePoderes;
}
