package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Tese jurídica cadastrada pelo advogado no Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeLawyerTeseResponse {
    @Schema(description = "Identificador interno da tese", example = "7")
    private Long id;
    @Schema(description = "Área do direito da tese", example = "PENAL")
    private String area;
    @Size(max = 500)
    @Schema(description = "Título da tese jurídica", example = "Atipicidade da conduta por ausência de dolo")
    private String titulo;
    @Size(max = 100_000)
    @Schema(description = "Conteúdo completo da tese em markdown",
            example = "## Fundamento\nA ausência de dolo é causa excludente...")
    private String corpo;
    @Schema(description = "JSON das tags associadas à tese", example = "[\"penal\",\"dolo\"]")
    private String tagsJson;
    @Schema(description = "Data e hora de criação da tese", example = "2026-04-01T10:00:00")
    private OffsetDateTime createdAt;
    @Schema(description = "Data e hora da última atualização", example = "2026-05-15T16:30:00")
    private OffsetDateTime updatedAt;
}
