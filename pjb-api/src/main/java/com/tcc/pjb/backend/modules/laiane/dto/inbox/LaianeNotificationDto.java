package com.tcc.pjb.backend.modules.laiane.dto.inbox;

import java.time.LocalDateTime;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeNotificationDto {
    private Long id;
    private Long processoId;
    private String canal;
    private String titulo;
    private String mensagem;
    private LocalDateTime enviadoEm;
    private String status;
}
