package com.tcc.pjb.backend.model.dto.processual.validation.material;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;

public record MaterialLegalValidationRequest(
        @NotNull Long processoId,
        @NotNull ProcessoLifecycleAction action,
        boolean exigeDocumentoPrincipal,
        boolean exigeFundamentacao,
        boolean exigeCompetenciaFechada,
        @Size(max = 180) String documentoPrincipalNome,
        String fundamentacao,
        @Size(max = 120) String finalidade
) {
}
