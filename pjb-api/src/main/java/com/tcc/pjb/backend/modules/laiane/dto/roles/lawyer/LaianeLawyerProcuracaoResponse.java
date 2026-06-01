package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.*;

@Schema(description = "Procuração judicial do advogado habilitada no Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeLawyerProcuracaoResponse {
    @Schema(description = "Identificador interno da procuração", example = "33")
    private Long id;
    @Schema(description = "ID do cliente representado", example = "100")
    private Long clienteId;
    @Schema(description = "ID do processo ao qual a procuração está vinculada", example = "5678")
    private Long processoId;
    @Schema(description = "Status da procuração",
            example = "ATIVA",
            allowableValues = {"ATIVA", "PENDENTE_HABILITACAO", "INDEFERIDA", "REVOGADA", "EXPIRADA", "SUSPENSA"})
    private LaianeProcuracaoStatus status;
    @Schema(description = "Data de início da vigência da procuração", example = "2026-01-01")
    private LocalDate inicioVigencia;
    @Schema(description = "Data de fim da vigência da procuração", example = "2026-12-31")
    private LocalDate fimVigencia;
    @Size(max = 5000)
    @Schema(description = "Poderes outorgados na procuração", example = "Ad judicia et extra — poderes plenos")
    private String poderes;
    @Schema(description = "JSON dos anexos da procuração (IDs ou metadados)", example = "[\"doc-1\",\"doc-2\"]")
    private String anexosJson;
    @Schema(description = "Política de representação processual — mapa de regras de sigilo e acesso")
    private Map<String, Object> representacaoPolicy;
    @Schema(description = "Data e hora de criação do registro", example = "2026-01-01T10:00:00")
    private OffsetDateTime createdAt;
    @Schema(description = "Data e hora da última atualização", example = "2026-05-31T15:00:00")
    private OffsetDateTime updatedAt;
}
