package com.tcc.pjb.backend.core.processo.sigilo.domain;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ProcessoSigiloProbatorioItem(
        UUID documentoId,
        String tituloDocumento,
        NivelSigilo nivelSigiloEfetivo,
        String naturezaProbatoria,
        boolean sensivel,
        boolean exigeReforcoSigilo,
        boolean compartilhadaEntreFeitos,
        List<String> marcadores,
        List<String> fundamentos
) {
    public ProcessoSigiloProbatorioItem {
        tituloDocumento = Objects.toString(tituloDocumento, "").trim();
        nivelSigiloEfetivo = nivelSigiloEfetivo == null ? NivelSigilo.PUBLICO : nivelSigiloEfetivo;
        naturezaProbatoria = Objects.toString(naturezaProbatoria, "").trim();
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
