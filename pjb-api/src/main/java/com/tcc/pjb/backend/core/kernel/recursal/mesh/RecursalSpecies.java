package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDivergencia;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucaoFiscal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosTerceiro;

public interface RecursalSpecies {
    String code();
    String formalName();
    LegalAppealType legacyType();
    boolean sameCaseAutos();
    boolean requiresCounterReasons();
    boolean potentiallyRequiresPreparo();
    boolean requiresCollegiateMerit();
}
