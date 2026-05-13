package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeCaseBundleUpdateRequest {
    private List<Long> processosIds;
    private Long teseId;
    private String descricao;
    private String status;
    private String justificativa;
}
