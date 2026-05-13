package com.tcc.pjb.backend.modules.advocacia.office.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkApproveRequest {
    @NotEmpty
    private List<Long> ids;

    private String reason;
}
