package com.tcc.pjb.backend.integration.mni.adapter;

import com.tcc.pjb.backend.model.entity.Processo;
import java.util.List;

public record MniAdapterResult(Processo processo, List<MniParteParsed> partes, List<MniMovimentoParsed> movimentos,
                                List<MniDocumentoParsed> documentos) {

    public MniAdapterResult {
        partes = partes == null ? List.of() : List.copyOf(partes);
        movimentos = movimentos == null ? List.of() : List.copyOf(movimentos);
        documentos = documentos == null ? List.of() : List.copyOf(documentos);
    }
}
