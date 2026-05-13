package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.util.List;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeWorkItemLiteDto;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeJudgeUrgencyPanelResponse {
    private List<LaianeWorkItemLiteDto> items;
    private String hint;
}
