package com.tcc.pjb.backend.modules.laiane.dto.legal;

import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeDraftResponse {
    private String kind;
    private String draftMarkdown;

    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
