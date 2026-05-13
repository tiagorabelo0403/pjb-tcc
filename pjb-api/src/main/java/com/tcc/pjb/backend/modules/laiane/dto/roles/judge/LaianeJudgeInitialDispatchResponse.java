package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LaianeJudgeInitialDispatchResponse {

    
    Long processId;

    
    String rito;

    
    String minuta;

    
    List<String> travas;
}
