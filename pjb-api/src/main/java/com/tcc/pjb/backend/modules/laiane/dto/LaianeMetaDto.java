package com.tcc.pjb.backend.modules.laiane.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeMetaDto {

    
    private String assistantName;

    
    private String moduleVersion;

    
    private String reasoningMode;

    
    private String externalAiRole;
}
