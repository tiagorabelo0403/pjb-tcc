package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeCaseBundleResponse {
    private Long id;
    private Long advogadoId;
    private String status;
    private List<Long> processosIds;
    private Long teseId;
    private String descricao;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
