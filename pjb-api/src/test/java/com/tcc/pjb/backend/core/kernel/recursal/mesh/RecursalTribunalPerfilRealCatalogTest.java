package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

class RecursalTribunalPerfilRealCatalogTest {

    @Test
    void shouldReturnConcreteCourtProfile() {
        RecursalCaseContext context = new RecursalCaseContext(
                null,
                "0000000-00.2026.8.26.0001",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "Apelação",
                RecursalClassFamily.CIVIL_CONHECIMENTO,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJSP,
                InstanceLevel.SECOND_INSTANCE,
                OrgaoJulgadorTipo.CAMARA,
                false,
                true,
                false,
                false,
                true,
                true,
                true
        );
        RecursalTribunalPerfilReal perfil = RecursalTribunalPerfilRealCatalog.defaultCatalog().profileOf(context);
        assertThat(perfil.perfilNome()).isEqualTo("TJSP_RULE_PROFILE");
        assertThat(perfil.autoridadeAdmissibilidadeExcepcional()).isEqualTo(RecursalAuthority.VICE_PRESIDENCIA);
    }
}
