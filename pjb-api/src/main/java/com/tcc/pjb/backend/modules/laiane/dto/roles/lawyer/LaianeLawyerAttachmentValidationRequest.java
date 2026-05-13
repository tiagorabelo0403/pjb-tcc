package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeLawyerAttachmentValidationRequest {
    private String rito;
    
    private List<String> anexos;
    
    private String anexosJson;
}
