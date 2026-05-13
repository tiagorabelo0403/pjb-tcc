package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalJusticaRuleProfile;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

class NationalRecursalStateMachineRecursalHardeningTest {

    private final NationalRecursalStateMachine stateMachine = new NationalRecursalStateMachine();
    private final RecursalRuleCatalog catalog = new RecursalRuleCatalog(List.of(new TribunalJusticaRuleProfile()));

    @Test
    void deveRastrearRemessaExternaComTransitoEDevolucao() {
        RecursalCaseContext context = upperCourtContext(false);
        RecursalSpecies species = new RecursoEspecial(true, true, false, false);
        RecursalRoutePlan routePlan = catalog.route(context, species);
        RecursalStateSnapshot snapshot = stateMachine.initialSnapshot("resp-remessa", context, species, routePlan);

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.PROTOCOLAR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.VALIDAR_TEMPESTIVIDADE);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.DISPENSAR_PREPARO);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.INTIMAR_CONTRARRAZOES);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.ENCERRAR_CONTRARRAZOES);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.ADMITIR);
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.REMESSA_EM_CURSO);
        assertThat(stateMachine.availableEvents(snapshot, species, routePlan)).contains(RecursalTransitionEvent.REGISTRAR_SAIDA_AUTOS);

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.REGISTRAR_SAIDA_AUTOS);
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.AUTOS_EM_TRANSITO);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.DEVOLVER_REMESSA);
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.REMESSA_DEVOLVIDA);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.ENCAMINHAR_ADMISSIBILIDADE);
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.ADMISSIBILIDADE_ORIGEM);
    }

    @Test
    void deveAbrirJanelaDeSustentacaoOralNoColegiado() {
        RecursalCaseContext context = upperCourtContext(false);
        RecursalSpecies species = new RecursoEspecial(true, true, false, false);
        RecursalRoutePlan routePlan = catalog.route(context, species);
        RecursalStateSnapshot snapshot = stateMachine.initialSnapshot("resp-sustentacao", context, species, routePlan);

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.PROTOCOLAR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.VALIDAR_TEMPESTIVIDADE);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.DISPENSAR_PREPARO);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.INTIMAR_CONTRARRAZOES);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.ENCERRAR_CONTRARRAZOES);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.ADMITIR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.AUTUAR_DESTINO);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.DISTRIBUIR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.AFETAR_ORGAO_JULGADOR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.ADMITIR);
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.JULGAMENTO_COLEGIADO);
        assertThat(stateMachine.availableEvents(snapshot, species, routePlan)).contains(RecursalTransitionEvent.PEDIR_PAUTA_SUSTENTACAO);

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.PEDIR_PAUTA_SUSTENTACAO);
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.PAUTA_SUSTENTACAO_DESIGNADA);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.SUSTENTAR);
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.JULGAMENTO_COLEGIADO);
    }

    @Test
    void deveDispensarPreparoEContrarrazoesNaRemessaNecessaria() {
        RecursalCaseContext context = firstInstanceContext(true);
        RecursalSpecies species = new ApelacaoCivel(true, true, true, false);
        RecursalRoutePlan routePlan = catalog.route(context, species);
        RecursalStateSnapshot snapshot = stateMachine.initialSnapshot("ap-remessa", context, species, routePlan);

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.PROTOCOLAR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.VALIDAR_TEMPESTIVIDADE);
        assertThat(stateMachine.availableEvents(snapshot, species, routePlan))
                .contains(RecursalTransitionEvent.DISPENSAR_PREPARO)
                .doesNotContain(RecursalTransitionEvent.REGISTRAR_PREPARO);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.DISPENSAR_PREPARO);
        assertThat(stateMachine.availableEvents(snapshot, species, routePlan))
                .contains(RecursalTransitionEvent.ENCAMINHAR_ADMISSIBILIDADE)
                .doesNotContain(RecursalTransitionEvent.INTIMAR_CONTRARRAZOES);
    }

    private RecursalCaseContext upperCourtContext(boolean remessaNecessaria) {
        return new RecursalCaseContext(
                21L,
                "0000021-00.2026.8.06.0001",
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
                true,
                true,
                true,
                false,
                true,
                remessaNecessaria
        );
    }

    private RecursalCaseContext firstInstanceContext(boolean remessaNecessaria) {
        return new RecursalCaseContext(
                22L,
                "0000022-00.2026.8.06.0001",
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
                true,
                true,
                true,
                false,
                true,
                remessaNecessaria
        );
    }

    private RecursalStateSnapshot move(RecursalStateSnapshot snapshot, RecursalCaseContext context, RecursalSpecies species, RecursalRoutePlan routePlan, RecursalTransitionEvent event) {
        return stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, event, "tester", Instant.parse("2026-03-11T18:00:00Z")), routePlan);
    }
}
