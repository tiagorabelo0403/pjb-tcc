package com.tcc.pjb.backend.service.material.model;

import java.util.Map;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialPack {
    
    private Map<String, MaterialProfile> byRamo;
    
    private Map<String, MaterialProfile> byRito;
}
