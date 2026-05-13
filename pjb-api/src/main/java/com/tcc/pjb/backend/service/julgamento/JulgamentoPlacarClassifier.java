package com.tcc.pjb.backend.service.julgamento;

import com.tcc.pjb.backend.model.entity.julgamento.enums.TipoVotoColegiado;
import java.util.EnumSet;
import java.util.Set;

public final class JulgamentoPlacarClassifier {

    private static final Set<TipoVotoColegiado> FAVOR = EnumSet.of(
            TipoVotoColegiado.DAR_PROVIMENTO,
            TipoVotoColegiado.ACOMPANHAR_RELATOR
    );
    private static final Set<TipoVotoColegiado> CONTRA = EnumSet.of(
            TipoVotoColegiado.NEGAR_PROVIMENTO
    );
    private static final Set<TipoVotoColegiado> PARCIAL = EnumSet.of(
            TipoVotoColegiado.PARCIAL_PROVIMENTO,
            TipoVotoColegiado.DAR_PROVIMENTO_EM_PARTE
    );

    private JulgamentoPlacarClassifier() {
    }

    public static JulgamentoPlacarBucket classify(TipoVotoColegiado tipo) {
        if (tipo == null) {
            return JulgamentoPlacarBucket.OUTROS;
        }
        if (FAVOR.contains(tipo)) {
            return JulgamentoPlacarBucket.FAVOR;
        }
        if (CONTRA.contains(tipo)) {
            return JulgamentoPlacarBucket.CONTRA;
        }
        if (PARCIAL.contains(tipo)) {
            return JulgamentoPlacarBucket.PARCIAL;
        }
        return JulgamentoPlacarBucket.OUTROS;
    }
}
