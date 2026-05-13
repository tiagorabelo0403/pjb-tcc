package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

class RecursalElectoralAndReclamacaoHardeningTest {

    private final RecursalRuleCatalog catalog = RecursalRuleCatalog.defaultCatalog();
    private final RecursalRouteIntegrityValidator integrityValidator = new RecursalRouteIntegrityValidator();

    @Test
    void deveRotejarRecursoEspecialEleitoralDoTreParaOTseSemQuebrarIntegridade() {
        RecursalCaseContext context = new RecursalCaseContext(
                31L,
                "0000031-00.2026.6.06.0001",
                TipoJustica.ELEITORAL,
                RamoDireito.CONSTITUCIONAL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "PROCESSO_ELEITORAL",
                RecursalClassFamily.CIVIL_CONHECIMENTO,
                RecursalTribunal.TRE,
                RecursalTribunalDetalhado.TRECE,
                InstanceLevel.SECOND_INSTANCE,
                OrgaoJulgadorTipo.PLENARIO,
                false,
                true,
                true,
                true,
                false,
                false,
                true
        );
        RecursalRoutePlan plan = catalog.route(context, new RecursoEspecial(true, true, false, false));
        assertThat(plan.tribunalDestino()).isEqualTo(RecursalTribunal.TSE);
        assertThat(plan.tribunalDetalhadoDestino()).isEqualTo(RecursalTribunalDetalhado.TSE);
        assertThat(plan.autoridadeDestinoAdmissibilidade()).isEqualTo(RecursalAuthority.RELATOR);
        assertDoesNotThrow(() -> integrityValidator.validate(context, new RecursoEspecial(true, true, false, false), plan));
    }

    @Test
    void deveEspecializarReclamacaoConstitucionalParaPresidenciaSemPreparoNoStj() {
        RecursalCaseContext context = new RecursalCaseContext(
                32L,
                "0000032-00.2026.8.06.0001",
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
        ReclamacaoConstitucional species = new ReclamacaoConstitucional(true, true, false, true);
        RecursalRoutePlan plan = catalog.route(context, species);
        assertThat(plan.tribunalDestino()).isEqualTo(RecursalTribunal.STJ);
        assertThat(plan.preparo().dispensadoPorLeiOuRegimento()).isTrue();
        assertThat(plan.admissibilidade().juizoDestino()).isTrue();
        assertThat(plan.admissibilidade().autoridadeDestino()).isEqualTo(RecursalAuthority.PRESIDENCIA);
        assertThat(plan.autoridadeJulgamentoMerito()).isEqualTo(RecursalAuthority.CORTE_ESPECIAL);
        assertDoesNotThrow(() -> integrityValidator.validate(context, species, plan));
    }
}
