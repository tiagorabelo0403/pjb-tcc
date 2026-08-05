package com.tcc.pjb.backend.integration.mni.adapter;

import com.tcc.pjb.backend.model.entity.Processo;
import java.util.List;

public record MniAdapterResult(Processo processo, List<MniParteParsed> partes) {

    public MniAdapterResult {
        partes = partes == null ? List.of() : List.copyOf(partes);
    }
}
