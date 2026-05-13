package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.modules.laiane.dto.roles.common.LaianeWorkItemLiteDto;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMpDeadlineMonitorResponse {
    private int upcoming;
    private Instant generatedAt;
    private int horizonDays;
    private int total;
    private int overdue;
    private List<LaianeWorkItemLiteDto> items;
}
