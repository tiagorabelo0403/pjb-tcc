package com.tcc.pjb.backend.modules.laiane.dto.legal;

import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeRitosCoverageResponse {
    @Builder.Default
    private List<String> supported = new ArrayList<>();
    @Builder.Default
    private List<String> missingPackDefinition = new ArrayList<>();
}
