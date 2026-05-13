package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeJudgeInitialDispatchRequest {
    private Long processoId;
    private String rito;
}
