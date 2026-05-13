package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalJusticaRuleProfile;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecursalRoutePlanDetailedTribunalTest {

    private final RecursalRuleCatalog catalog = new RecursalRuleCatalog(List.of(new TribunalJusticaRuleProfile()));

    @Test
    void devePreservarTribunalDetalhadoEmApelacaoNoMesmoTribunal() {
        RecursalCaseContext context = new RecursalCaseContext(
                10L,
                "0000100-00.2026.8.06.0001",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "PROCEDIMENTO_COMUM_CIVEL",
                RecursalClassFamily.CIVIL_CONHECIMENTO,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                InstanceLevel.FIRST_INSTANCE,
                OrgaoJulgadorTipo.MONOCRATICO,
                true,
                false,
                false,
                false,
                false,
                false,
                true
        );
        RecursalRoutePlan routePlan = catalog.route(context, new ApelacaoCivel(true, false, false, false));
        assertSame(RecursalTribunalDetalhado.TJCE, routePlan.tribunalDetalhadoOrigem());
        assertSame(RecursalTribunalDetalhado.TJCE, routePlan.tribunalDetalhadoDestino());
        assertEquals(RecursalTribunal.TJ, routePlan.tribunalDestino());
    }

    @Test
    void deveResolverDestinoDetalhadoSuperiorEmRecursoEspecial() {
        RecursalCaseContext context = new RecursalCaseContext(
                11L,
                "0000200-00.2026.8.06.0001",
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
                false,
                true
        );
        RecursalRoutePlan routePlan = catalog.route(context, new RecursoEspecial(true, true, false, false));
        assertSame(RecursalTribunal.TJ, routePlan.tribunalOrigem());
        assertSame(RecursalTribunalDetalhado.TJCE, routePlan.tribunalDetalhadoOrigem());
        assertSame(RecursalTribunal.STJ, routePlan.tribunalDestino());
        assertSame(RecursalTribunalDetalhado.STJ, routePlan.tribunalDetalhadoDestino());
    }
}
