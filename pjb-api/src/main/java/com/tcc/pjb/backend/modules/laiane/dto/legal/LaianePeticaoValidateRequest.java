package com.tcc.pjb.backend.modules.laiane.dto.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianePeticaoValidateRequest {
    private String content;
    private String rito;
    private String classeTpu;
    private String ramoDireito;
}
