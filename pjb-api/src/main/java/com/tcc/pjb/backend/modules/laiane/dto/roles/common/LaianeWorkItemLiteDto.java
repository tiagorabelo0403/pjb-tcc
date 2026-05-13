package com.tcc.pjb.backend.modules.laiane.dto.roles.common;

import java.time.Instant;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeWorkItemLiteDto {
    private Long id;
    private Long processoId;
    private String titulo;
    private String status;
    private Integer prioridade;
    private boolean blocking;
    private Instant dueAt;
}
