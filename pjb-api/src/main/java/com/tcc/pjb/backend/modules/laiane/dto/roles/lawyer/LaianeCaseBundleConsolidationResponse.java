package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import java.time.Instant;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeCaseBundleConsolidationResponse {
    private String suggestion;
    private Long bundleId;
    private int totalProcessos;
    private List<Long> processosIds;
    private int openWorkItems;
    private Instant earliestDueAt;
    private String resumo;
}
