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
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalRecursalStateMachineDetailedRoutingTest {

    private final NationalRecursalStateMachine stateMachine = new NationalRecursalStateMachine();
    private final RecursalRuleCatalog catalog = new RecursalRuleCatalog(List.of(new TribunalJusticaRuleProfile()));

    @Test
    void deveAtualizarTribunalDetalhadoNoDestinoSuperior() {
        RecursalCaseContext context = new RecursalCaseContext(
                1L,
                "0000300-00.2026.8.06.0001",
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
                true,
                true,
                true,
                true
        );
        RecursalSpecies species = new RecursoExtraordinario(true, true, true, false);
        RecursalRoutePlan routePlan = catalog.route(context, species);
        RecursalStateSnapshot snapshot = stateMachine.initialSnapshot("re-detalhado", context, species, routePlan);
        snapshot = transition(snapshot, context, species, routePlan, RecursalTransitionEvent.PROTOCOLAR);
        snapshot = transition(snapshot, context, species, routePlan, RecursalTransitionEvent.VALIDAR_TEMPESTIVIDADE);
        snapshot = transition(snapshot, context, species, routePlan, RecursalTransitionEvent.DISPENSAR_PREPARO);
        snapshot = transition(snapshot, context, species, routePlan, RecursalTransitionEvent.INTIMAR_CONTRARRAZOES);
        snapshot = transition(snapshot, context, species, routePlan, RecursalTransitionEvent.ENCERRAR_CONTRARRAZOES);
        snapshot = transition(snapshot, context, species, routePlan, RecursalTransitionEvent.ADMITIR);
        snapshot = transition(snapshot, context, species, routePlan, RecursalTransitionEvent.AUTUAR_DESTINO);
        assertEquals(RecursalLifecycleState.AUTUADO_NO_DESTINO, snapshot.state());
        assertSame(RecursalTribunal.STF, snapshot.tribunalAtual());
        assertSame(RecursalTribunalDetalhado.STF, snapshot.tribunalDetalhadoAtual());
    }

    private RecursalStateSnapshot transition(RecursalStateSnapshot snapshot, RecursalCaseContext context, RecursalSpecies species, RecursalRoutePlan routePlan, RecursalTransitionEvent event) {
        return stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, event, "tester", Instant.parse("2026-03-11T14:00:00Z")), routePlan);
    }
}
