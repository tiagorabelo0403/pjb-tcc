package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalCaseContext;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassFamily;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalConstraintViolationException;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

class RecursalMeshGuardServiceTest {

    @Test
    void deveRejeitarTribunalDetalhadoIncompativelComTribunalOrigem() {
        RecursalMeshGuardService service = new RecursalMeshGuardService();
        RecursalCaseContext context = new RecursalCaseContext(
                1L,
                "0000001-00.2026.8.26.0001",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "Apelação Cível",
                RecursalClassFamily.CIVIL_CONHECIMENTO,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TRF1,
                InstanceLevel.FIRST_INSTANCE,
                OrgaoJulgadorTipo.MONOCRATICO,
                false,
                false,
                false,
                false,
                false,
                false,
                true
        );

        assertThatThrownBy(() -> service.validateContext(context))
                .isInstanceOf(RecursalConstraintViolationException.class)
                .hasMessageContaining("Tribunal detalhado de origem incompatível");
    }

    @Test
    void deveRejeitarEmbargosSemFundamento() {
        RecursalMeshGuardService service = new RecursalMeshGuardService();

        assertThatThrownBy(() -> service.validateSpecies(new EmbargosDeclaracao(Set.of(), false, false, true)))
                .isInstanceOfAny(RecursalConstraintViolationException.class, IllegalArgumentException.class)
                .hasMessageContaining("ao menos um fundamento");
    }
}
