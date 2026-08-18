package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Schema(description = "Evento de auditoria comportamental registrado no Laiane-MP")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpAuditEventDto {
    @Schema(description = "Identificador único do evento de auditoria", example = "01963c1a-7e3f-7000-8000-000000000001")
    private UUID uuid;
    @Schema(description = "Ação auditada", example = "OFICIO_CRIADO")
    private String acao;
    @Schema(description = "ID do usuário que executou a ação", example = "88")
    private Long usuarioId;
    @Schema(description = "ID da entidade referenciada", example = "12345")
    private String referenciaId;
    @Schema(description = "Detalhes adicionais do evento", example = "Ofício enviado para destinatário")
    private String detalhes;
    @Schema(description = "Justificativa informada pelo usuário", example = "Fluxo institucional regular")
    private String justificativa;
    @Schema(description = "Data e hora do evento com timezone (ISO-8601)", example = "2026-05-31T15:00:00-03:00")
    private OffsetDateTime timestamp;
    @Schema(description = "Nível de risco classificado do evento",
            example = "BAIXO", allowableValues = {"BAIXO", "MEDIO", "ALTO", "CRITICO"})
    private String nivelRisco;
    @Schema(description = "Perfil comportamental detectado no evento", example = "PADRAO")
    private String perfilComportamental;
    @Schema(description = "Hash de integridade do registro de auditoria", example = "sha256:abc123...")
    private String hashIntegridade;
}
