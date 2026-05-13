package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

class RecursalTribunalDetalhadoCatalogTest {

    @Test
    void deveResolverTribunalDetalhadoPorFamilia() {
        assertSame(RecursalTribunalDetalhado.TJSP, RecursalTribunalDetalhado.fromFamily(RecursalTribunal.TJ));
        assertSame(RecursalTribunalDetalhado.TRF1, RecursalTribunalDetalhado.fromFamily(RecursalTribunal.TRF));
        assertSame(RecursalTribunalDetalhado.STJ, RecursalTribunalDetalhado.fromFamily(RecursalTribunal.STJ));
    }

    @Test
    void deveResolverPerfilRealPorTribunalDetalhado() {
        RecursalCaseContext context = new RecursalCaseContext(
                99L,
                "0000002-00.2026.8.06.0001",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "PROCEDIMENTO_COMUM_CIVEL",
                RecursalClassFamily.CIVIL_CONHECIMENTO,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
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
        assertEquals(RecursalTribunalDetalhado.TJCE, perfil.codigo());
        assertEquals("TJCE_RULE_PROFILE", perfil.perfilNome());
        assertEquals(RecursalAuthority.PRESIDENCIA, perfil.autoridadeAdmissibilidadeExcepcional());
    }
}
