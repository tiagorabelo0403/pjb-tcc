package com.tcc.pjb.backend.model.dto.pastadigital;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageRefDTO {
    Integer pageNumber;
    String pageId;
    String fingerprint;
    String preview;
}
