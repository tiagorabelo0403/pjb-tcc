package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecursalLocalRegimentalPolicyCatalogTest {

    @Test
    void deveResolverPoliticaLocalPorTribunalDetalhado() {
        RecursalLocalRegimentalPolicyCatalog catalog = RecursalLocalRegimentalPolicyCatalog.defaultCatalog();
        RecursalLocalRegimentalPolicy tjsp = catalog.policyOf(RecursalTribunalDetalhado.TJSP);
        RecursalLocalRegimentalPolicy stj = catalog.policyOf(RecursalTribunalDetalhado.STJ);
        assertEquals(RecursalAuthority.VICE_PRESIDENCIA, tjsp.autoridadeAdmissibilidadeExcepcional());
        assertEquals(RecursalAuthority.ORGAO_ESPECIAL, tjsp.autoridadeAgravoInternoContraPresidencia());
        assertEquals(RecursalAuthority.PRESIDENCIA, stj.autoridadeAdmissibilidadeExcepcional());
        assertEquals(RecursalAuthority.CORTE_ESPECIAL, stj.autoridadeAgravoInternoContraPresidencia());
    }

    @Test
    void deveAplicarOverlayLocalNaRotaExcepcionalDoTjsp() {
        RecursalCaseContext context = new RecursalCaseContext(
                9L,
                "0000009-00.2026.8.26.0001",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "PROCEDIMENTO_COMUM_CIVEL",
                RecursalClassFamily.CIVIL_CONHECIMENTO,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJSP,
                InstanceLevel.SECOND_INSTANCE,
                OrgaoJulgadorTipo.CAMARA,
                false,
                true,
                false,
                true,
                true,
                false,
                true
        );
        RecursalSpecies species = new RecursoEspecial(true, true, false, false);
        RecursalRoutePlan routePlan = new RecursalRuleCatalog(List.of(new com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalJusticaRuleProfile())).route(context, species);
        assertEquals(RecursalAuthority.VICE_PRESIDENCIA, routePlan.admissibilidade().autoridadeOrigem());
        assertEquals(RecursalAuthority.SECAO, routePlan.autoridadeJulgamentoMerito());
        assertTrue(routePlan.admissibilidade().admiteRetratacao());
    }
}
