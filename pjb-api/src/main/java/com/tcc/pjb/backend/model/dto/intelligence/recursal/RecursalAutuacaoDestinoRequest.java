package com.tcc.pjb.backend.model.dto.intelligence.recursal;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import jakarta.validation.constraints.Size;

public record RecursalAutuacaoDestinoRequest(
        @Size(max = 64) String numeroAutuacaoDestino,
        InstanceLevel instanciaDestino,
        @Size(max = 64) String tribunalDestino,
        @Size(max = 160) String unidadeDistribuicao,
        @Size(max = 1200) String observacoes) {
}
