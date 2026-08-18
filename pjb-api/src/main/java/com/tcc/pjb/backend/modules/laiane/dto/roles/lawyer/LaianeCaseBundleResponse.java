package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import com.tcc.pjb.backend.modules.laiane.model.LaianeCaseBundleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.*;

@Schema(description = "Bundle de processos agrupados pelo advogado no Laiane")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeCaseBundleResponse {
    @Schema(description = "Identificador interno do bundle", example = "42")
    private Long id;
    @Schema(description = "ID do advogado titular do bundle", example = "88")
    private Long advogadoId;
    @Schema(description = "Status do bundle",
            example = "ABERTO",
            allowableValues = {"ABERTO", "EM_ANALISE", "CONSOLIDADO", "ARQUIVADO"})
    private LaianeCaseBundleStatus status;
    @Size(max = 500)
    @Schema(description = "Lista de IDs de processos agrupados no bundle (máx. 500)")
    private List<Long> processosIds;
    @Schema(description = "ID da tese jurídica vinculada ao bundle", example = "7")
    private Long teseId;
    @Size(max = 2000)
    @Schema(description = "Descrição do bundle", example = "Processos envolvendo o mesmo réu — comarca de Fortaleza")
    private String descricao;
    @Schema(description = "Data e hora de criação do bundle (ISO-8601 com timezone)", example = "2026-05-31T14:00:00-03:00")
    private OffsetDateTime createdAt;
    @Schema(description = "Data e hora da última atualização (ISO-8601 com timezone)", example = "2026-05-31T15:00:00-03:00")
    private OffsetDateTime updatedAt;
}
