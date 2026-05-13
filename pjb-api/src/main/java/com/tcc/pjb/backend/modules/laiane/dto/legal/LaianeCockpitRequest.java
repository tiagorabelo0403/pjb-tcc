package com.tcc.pjb.backend.modules.laiane.dto.legal;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeCockpitRequest {

    
    @Builder.Default
    private Map<String, Object> ctx = new LinkedHashMap<>();

    
    private String rito;

    
    private String role;
}
