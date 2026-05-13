package com.tcc.pjb.backend.modules.laiane.dto.legal;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeDraftRequest {

    
    private String kind;

    @Builder.Default
    private Map<String, Object> ctx = new LinkedHashMap<>();
}
