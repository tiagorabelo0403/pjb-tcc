package com.tcc.pjb.backend.service.rito.model;

import java.util.Map;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RitoPack {

    
    private Map<String, RitoDefinition> definitions;
}
