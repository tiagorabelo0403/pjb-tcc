package com.tcc.pjb.backend.modules.laiane.dto.legal;

import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianePeticaoValidateResponse {
    private boolean ok;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
