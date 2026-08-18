package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

@Schema(description = "Resultado da validação de anexos da petição pelo Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeLawyerAttachmentValidationResponse {
    @Size(max = 100)
    @Schema(description = "Tipos de anexo informados pelo peticionante (máx. 100)")
    private List<String> informed;
    @Schema(description = "Rito processual usado na validação", example = "RITO_ORDINARIO")
    private String rito;
    @Schema(description = "Indica se todos os anexos obrigatórios estão presentes", example = "true")
    private boolean ok;
    @Size(max = 50)
    @Schema(description = "Tipos de anexo exigidos pelo rito (máx. 50)")
    private List<String> required;
    @Size(max = 100)
    @Schema(description = "Tipos de anexo presentes na petição (máx. 100)")
    private List<String> present;
    @Size(max = 50)
    @Schema(description = "Tipos de anexo ausentes e obrigatórios (máx. 50)")
    private List<String> missing;
}
