package com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer;

import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeLawyerAttachmentValidationResponse {
    private java.util.List<String> informed;
    private String rito;
    private boolean ok;
    private List<String> required;
    private List<String> present;
    private List<String> missing;
}
