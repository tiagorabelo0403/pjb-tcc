package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeJudgeSanitationChecklistResponse {
    private Long processoId;
    private List<String> checklist;
}
