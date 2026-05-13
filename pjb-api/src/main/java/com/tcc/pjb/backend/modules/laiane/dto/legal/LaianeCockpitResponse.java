package com.tcc.pjb.backend.modules.laiane.dto.legal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeCockpitResponse {

    
    @Builder.Default
    private Map<String, Object> ritoInference = new LinkedHashMap<>();

    
    @Builder.Default
    private List<LaianePlaybookItemDto> playbook = new ArrayList<>();

    
    @Builder.Default
    private List<String> differentiators = new ArrayList<>();
}
