package com.tcc.pjb.backend.modules.advocacia.office.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRequest {
    @Size(max = 240)
    private String reason;
}
