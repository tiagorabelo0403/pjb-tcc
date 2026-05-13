package com.tcc.pjb.backend.modules.laiane.dto.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianeProtocolCreateRequest {
    private String title;

    @Builder.Default
    private Map<String, Object> payload = new LinkedHashMap<>();
}
