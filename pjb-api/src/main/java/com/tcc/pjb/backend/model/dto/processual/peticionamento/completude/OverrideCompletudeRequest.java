package com.tcc.pjb.backend.model.dto.processual.peticionamento.completude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitação de override de completude pela secretaria — dispensa documentação com justificativa")
public record OverrideCompletudeRequest(
        @NotBlank
        @Size(min = 20, max = 1000)
        @Schema(description = "Justificativa obrigatória para dispensa da documentação (mínimo 20 caracteres)",
                example = "Partes identificadas por certidão judicial. Documentos originais apresentados presencialmente.")
        String justificativa
) {}
