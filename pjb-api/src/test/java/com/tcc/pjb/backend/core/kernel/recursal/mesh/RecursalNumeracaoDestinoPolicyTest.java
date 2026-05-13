package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalJusticaRuleProfile;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucao;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecursalNumeracaoDestinoPolicyTest {

    private final RecursalRuleCatalog catalog = new RecursalRuleCatalog(List.of(new TribunalJusticaRuleProfile()));

    @Test
    void deveManterMesmaNumeracaoComoPadraoNaApelacao() {
        RecursalCaseContext context = new RecursalCaseContext(
                50L,
                "0000050-00.2026.8.06.0001",
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
        RecursalRoutePlan plan = catalog.route(context, new ApelacaoCivel(true, false, false, false));
        assertThat(plan.routeKind()).isEqualTo(RecursalRouteKind.SECOND_INSTANCE_EXTERNAL);
        assertThat(plan.remessa().externa()).isTrue();
        assertThat(plan.remessa().distribuicaoDestino()).isTrue();
        assertThat(plan.remessa().autuacaoDestino()).isFalse();
        assertThat(plan.remessa().mesmaNumeracao()).isTrue();
    }

    @Test
    void deveReservarAutuacaoPropriaParaReclamacaoConstitucionalAutonoma() {
        RecursalCaseContext context = new RecursalCaseContext(
                51L,
                "0000051-00.2026.8.06.0001",
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
        RecursalRoutePlan plan = catalog.route(context, new ReclamacaoConstitucional(true, true, false, true));
        assertThat(plan.routeKind()).isEqualTo(RecursalRouteKind.ORIGINARY_SUPERIOR);
        assertThat(plan.remessa().autuacaoDestino()).isTrue();
        assertThat(plan.remessa().distribuicaoDestino()).isTrue();
        assertThat(plan.remessa().mesmaNumeracao()).isFalse();
    }

    @Test
    void deveAutuarEmApartadoPorDependenciaNosEmbargosDeExecucao() {
        RecursalCaseContext context = new RecursalCaseContext(
                52L,
                "0000052-00.2026.8.06.0001",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.CUMPRIMENTO_SENTENCA,
                "CUMPRIMENTO_DE_SENTENCA",
                RecursalClassFamily.CIVIL_EXECUCAO,
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
        RecursalRoutePlan plan = catalog.route(context, new EmbargosExecucao(true, true, true, true));
        assertThat(plan.routeKind()).isEqualTo(RecursalRouteKind.EXECUTION_INCIDENT_INTERNAL);
        assertThat(plan.remessa().externa()).isFalse();
        assertThat(plan.remessa().autosApartadosDependencia()).isTrue();
        assertThat(plan.remessa().autuacaoDestino()).isTrue();
        assertThat(plan.remessa().mesmaNumeracao()).isFalse();
    }


    @Test
    void deveAutuarAgravoDeInstrumentoEmApartadoNoTribunal() {
        RecursalCaseContext context = new RecursalCaseContext(
                53L,
                "0000053-00.2026.8.06.0001",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.CONHECIMENTO,
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
        RecursalRoutePlan plan = catalog.route(context, new AgravoInstrumento(true, true, false, false));
        assertThat(plan.routeKind()).isEqualTo(RecursalRouteKind.SECOND_INSTANCE_EXTERNAL);
        assertThat(plan.remessa().externa()).isTrue();
        assertThat(plan.remessa().autuacaoDestino()).isTrue();
        assertThat(plan.remessa().mesmaNumeracao()).isFalse();
    }

}
