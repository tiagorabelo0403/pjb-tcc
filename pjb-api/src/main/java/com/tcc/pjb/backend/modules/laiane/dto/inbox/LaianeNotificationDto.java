package com.tcc.pjb.backend.modules.laiane.dto.inbox;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.*;

@Schema(description = "Notificação processual enviada ao usuário pelo Laiane")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeNotificationDto {
    @Schema(description = "Identificador interno da notificação", example = "101")
    private Long id;
    @Schema(description = "ID do processo vinculado à notificação", example = "12345")
    private Long processoId;
    @Schema(description = "Canal de envio da notificação",
            example = "EMAIL", allowableValues = {"EMAIL", "SMS", "PUSH", "IN_APP"})
    private String canal;
    @Size(max = 500)
    @Schema(description = "Título da notificação", example = "Prazo vencendo em 24h")
    private String titulo;
    @Size(max = 2000)
    @Schema(description = "Mensagem da notificação", example = "O processo 0001234-56.2026.8.06.0001 tem prazo amanhã.")
    private String mensagem;
    @Schema(description = "Data e hora de envio da notificação", example = "2026-05-31T08:00:00")
    private OffsetDateTime enviadoEm;
    @Schema(description = "Status da notificação",
            example = "ENTREGUE", allowableValues = {"PENDENTE", "ENTREGUE", "FALHOU", "LIDA"})
    private String status;
}
