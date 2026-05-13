package com.tcc.pjb.backend.model.dto.advogado.surface;

import java.util.List;

public record AdvogadoOfficeBulkApproveResponse(
        List<Long> approved,
        List<Long> rejected,
        List<String> errors
) {}
