package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TurmaNacionalUniformizacaoRuleProfile;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

class RecursalUniformizacaoFederalHardeningTest {

    private final RecursalRuleCatalog catalog = RecursalRuleCatalog.defaultCatalog();
    private final RecursalRouteIntegrityValidator integrityValidator = new RecursalRouteIntegrityValidator();

    @Test
    void deveRotejarPedidoUniformizacaoFederalDaTurmaRecursalParaTnuSemQuebrarIntegridade() {
        RecursalCaseContext context = new RecursalCaseContext(
                41L,
                "0000041-00.2026.4.05.8100",
                TipoJustica.FEDERAL,
                RamoDireito.PREVIDENCIARIO,
                RitoProcessual.JUIZADO_ESPECIAL_FEDERAL,
                FaseProcessual.RECURSAL,
                "Pedido de uniformização",
                RecursalClassFamily.JUIZADO_ESPECIAL,
                RecursalTribunal.TRF,
                RecursalTribunalDetalhado.TRF5,
                InstanceLevel.SECOND_INSTANCE,
                OrgaoJulgadorTipo.TURMA,
                false,
                true,
                true,
                true,
                true,
                false,
                true
        );
        PedidoUniformizacaoFederal species = new PedidoUniformizacaoFederal(true, true, true, true);
        RecursalRoutePlan plan = catalog.route(context, species);
        assertThat(plan.tribunalDestino()).isEqualTo(RecursalTribunal.TNU);
        assertThat(plan.tribunalDetalhadoDestino()).isEqualTo(RecursalTribunalDetalhado.TNU);
        assertThat(plan.preparo().dispensadoPorLeiOuRegimento()).isTrue();
        assertThat(plan.admissibilidade().juizoOrigem()).isTrue();
        assertThat(plan.admissibilidade().autoridadeOrigem()).isEqualTo(RecursalAuthority.PRESIDENCIA);
        assertThat(plan.admissibilidade().admiteSobrestamento()).isTrue();
        assertDoesNotThrow(() -> integrityValidator.validate(context, species, plan));
    }

    @Test
    void deveRotejarAgravoEmReDaTnuParaStf() {
        TurmaNacionalUniformizacaoRuleProfile profile = new TurmaNacionalUniformizacaoRuleProfile();
        RecursalCaseContext context = new RecursalCaseContext(
                42L,
                "0000042-00.2026.4.05.8100",
                TipoJustica.FEDERAL,
                RamoDireito.CONSTITUCIONAL,
                RitoProcessual.JUIZADO_ESPECIAL_FEDERAL,
                FaseProcessual.RECURSAL,
                "Agravo em recurso extraordinário",
                RecursalClassFamily.JUIZADO_ESPECIAL,
                RecursalTribunal.TNU,
                RecursalTribunalDetalhado.TNU,
                InstanceLevel.SECOND_INSTANCE,
                OrgaoJulgadorTipo.TURMA,
                false,
                true,
                false,
                true,
                false,
                true,
                true
        );
        AgravoRecursoExtraordinario species = new AgravoRecursoExtraordinario(true, true, true, true, true, true);
        RecursalRoutePlan plan = profile.route(context, species);
        assertThat(plan.tribunalDestino()).isEqualTo(RecursalTribunal.STF);
        assertThat(plan.tribunalDetalhadoDestino()).isEqualTo(RecursalTribunalDetalhado.STF);
        assertThat(plan.instanciaDestino()).isEqualTo(InstanceLevel.EXTRAORDINARY);
        assertThat(plan.autoridadeDestinoAdmissibilidade()).isEqualTo(RecursalAuthority.RELATOR);
        assertThat(plan.autoridadeJulgamentoMerito()).isEqualTo(RecursalAuthority.TURMA);
    }
}
