package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import com.tcc.pjb.backend.modules.laiane.model.LaianeDeadlineDelegationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.*;

@Schema(description = "Delegação de prazo processual entre advogados no Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeDeadlineDelegationResponse {
    @Schema(description = "Identificador interno da delegação", example = "15")
    private Long id;
    @Schema(description = "ID do advogado delegante", example = "10")
    private Long delegatorId;
    @Schema(description = "ID do advogado delegatário", example = "20")
    private Long delegateeId;
    @Schema(description = "ID do item de trabalho vinculado ao prazo", example = "99")
    private Long workItemId;
    @Schema(description = "ID do processo vinculado", example = "1234")
    private Long processoId;
    @Schema(description = "Status da delegação",
            example = "PENDENTE",
            allowableValues = {"PENDENTE", "ACEITA", "CONCLUIDA", "CANCELADA"})
    private LaianeDeadlineDelegationStatus status;
    @Size(max = 1000)
    @Schema(description = "Descrição e justificativa da delegação",
            example = "Delegação por férias do titular — retorno em 15/06/2026")
    private String descricao;
    @Schema(description = "Data e hora de aceitação da delegação", example = "2026-05-31T09:00:00")
    private OffsetDateTime acceptedAt;
    @Schema(description = "Data e hora de conclusão da delegação", example = "2026-06-10T18:00:00")
    private OffsetDateTime completedAt;
    @Schema(description = "Data e hora de criação do registro", example = "2026-05-30T08:00:00")
    private OffsetDateTime createdAt;
    @Schema(description = "Data e hora da última atualização", example = "2026-05-31T09:00:00")
    private OffsetDateTime updatedAt;
}
