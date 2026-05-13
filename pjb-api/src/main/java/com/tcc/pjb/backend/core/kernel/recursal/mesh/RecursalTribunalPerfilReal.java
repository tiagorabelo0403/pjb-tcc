package com.tcc.pjb.backend.core.kernel.recursal.mesh;

public record RecursalTribunalPerfilReal(
        RecursalTribunalDetalhado codigo,
        RecursalAuthority autoridadeAdmissibilidadeExcepcional,
        RecursalAuthority autoridadeAgravoInternoFiltro,
        RecursalAuthority autoridadeEmbargosDivergencia,
        String perfilNome) {
}
