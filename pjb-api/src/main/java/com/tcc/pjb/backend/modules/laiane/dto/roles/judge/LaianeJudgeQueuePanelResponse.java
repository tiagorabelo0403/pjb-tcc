package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.time.Instant;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeJudgeQueuePanelResponse {
    private Instant generatedAt;
    private String uf;
    private String comarca;
    private int total;
    private List<LaianeJudgeQueueBucketDto> buckets;
}
