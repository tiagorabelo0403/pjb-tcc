package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Objects;

public record RecursalLocalRegimentalPolicy(
        RecursalAuthority autoridadeAdmissibilidadeExcepcional,
        RecursalAuthority autoridadeAgravoInternoContraPresidencia,
        RecursalAuthority autoridadeEmbargosColegiados,
        RecursalAuthority autoridadeMeritoExcepcional,
        boolean admiteJuizoRetratacaoPosSobrestamento,
        boolean exigeVicePresidenciaNosExcepcionais) {

    public RecursalLocalRegimentalPolicy {
        Objects.requireNonNull(autoridadeAdmissibilidadeExcepcional, "autoridadeAdmissibilidadeExcepcional");
        Objects.requireNonNull(autoridadeAgravoInternoContraPresidencia, "autoridadeAgravoInternoContraPresidencia");
        Objects.requireNonNull(autoridadeEmbargosColegiados, "autoridadeEmbargosColegiados");
        Objects.requireNonNull(autoridadeMeritoExcepcional, "autoridadeMeritoExcepcional");
    }
}
