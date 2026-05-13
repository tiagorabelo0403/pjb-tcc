package com.tcc.pjb.backend.model.dto.pastadigital;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageSearchResponse {
    Long processoId;
    String query;
    int limit;
    List<PageSearchHitDTO> hits;
}
