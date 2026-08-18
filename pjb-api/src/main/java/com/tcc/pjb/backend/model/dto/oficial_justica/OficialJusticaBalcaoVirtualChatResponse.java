package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record OficialJusticaBalcaoVirtualChatResponse(
        String territorio,
        Instant generatedAt,
        Summary summary,
        List<Room> salas,
        List<String> alerts
) {
    public OficialJusticaBalcaoVirtualChatResponse {
        salas = salas == null ? List.of() : List.copyOf(salas);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    public record Summary(
            int totalRooms,
            int comChatHabilitado,
            int federais,
            int estaduais,
            int varasCobertas,
            int tribunaisCobertos,
            int salasComSlaCritico,
            int mensagensPendentesEstimadas
    ) {
    }

    public record Room(
            String roomKey,
            Long processoId,
            String processoNumero,
            String tribunal,
            String vara,
            String esfera,
            String organDisplay,
            String instance,
            String lane,
            String inboxKey,
            boolean enabled,
            String historyPath,
            String sendPath,
            String routingMode,
            String destinoPrincipal,
            Instant ultimaAtividadeEm,
            int unreadEstimate,
            int responseSlaMinutos,
            String chatPartitionKey,
            @Schema(description = "Contexto da unidade judicial executora — chaves variam por tribunal e tipo de diligencia", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        java.util.Map<String, Object> unidadeContexto,
            boolean reativavelPorReintimacao,
            List<String> templatesRapidos,
            List<MessagePreview> preview,
            List<String> highlights
    ) {
        public Room {
            unidadeContexto = unidadeContexto == null ? java.util.Map.of() : java.util.Map.copyOf(unidadeContexto);
            templatesRapidos = templatesRapidos == null ? List.of() : List.copyOf(templatesRapidos);
            preview = preview == null ? List.of() : List.copyOf(preview);
            highlights = highlights == null ? List.of() : List.copyOf(highlights);
        }
    }

    public record MessagePreview(
            Instant sentAt,
            String sender,
            String perfil,
            String conteudo,
            String canal,
            boolean fromOfficial,
            boolean urgent
    ) {
    }
}


