package com.tcc.pjb.backend.modules.laiane.dto.inbox;

import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Schema(description = "Inbox do Laiane com itens de trabalho e notificações do usuário autenticado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeInboxResponse {

    @Size(max = 500)
    @Schema(description = "Itens de trabalho pendentes para o usuário (máx. 500)")
    @Builder.Default
    private List<WorkItemDto> workItems = new ArrayList<>();

    @Size(max = 100)
    @Schema(description = "Notificações pendentes para o usuário (máx. 100)")
    @Builder.Default
    private List<LaianeNotificationDto> notifications = new ArrayList<>();
}
