package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NationalRecursalStateMachineEffectsTest {

    private final NationalRecursalStateMachine stateMachine = new NationalRecursalStateMachine();
    private final RecursalRuleCatalog catalog = new RecursalRuleCatalog(List.of(new com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalJusticaRuleProfile()));

    @Test
    void deveAlternarEfeitosESinalizarConhecimentoParcial() {
        RecursalCaseContext context = new RecursalCaseContext(
                7L,
                "0000007-00.2026.8.26.0001",
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
        RecursalRoutePlan routePlan = catalog.route(context, species);
        RecursalStateSnapshot snapshot = stateMachine.initialSnapshot("resp-efeitos", context, species, routePlan);
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
        assertEquals(RecursalLifecycleState.JULGAMENTO_COLEGIADO, snapshot.state());
        assertTrue(stateMachine.availableEvents(snapshot, species, routePlan).contains(RecursalTransitionEvent.CONCEDER_EFEITO_SUSPENSIVO));
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.CONCEDER_EFEITO_SUSPENSIVO);
        assertTrue(snapshot.efeitoSuspensivoAtivo());
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.CONCEDER_EFEITO_ATIVO);
        assertTrue(snapshot.efeitoAtivoConcedido());
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.CONHECER_PARCIALMENTE);
        assertEquals(RecursalLifecycleState.PARCIALMENTE_PROVIDO, snapshot.state());
        assertTrue(snapshot.conhecimentoParcial());
        assertTrue(snapshot.efeitoSuspensivoAtivo());
        assertTrue(snapshot.efeitoAtivoConcedido());
    }

    @Test
    void devePermitirRevogacaoDosEfeitosDuranteJulgamento() {
        RecursalStateSnapshot snapshot = new RecursalStateSnapshot(
                "ag-efeitos",
                RecursalLifecycleState.JULGAMENTO_MONOCRATICO,
                3,
                RecursalTribunal.STJ,
                RecursalTribunalDetalhado.STJ,
                InstanceLevel.SUPERIOR,
                RecursalAuthority.RELATOR,
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                false,
                false,
                true,
                true,
                false,
                0,
                Instant.parse("2026-03-11T12:00:00Z")
        );
        RecursalCaseContext context = new RecursalCaseContext(
                8L,
                "0000008-00.2026.4.01.0001",
                TipoJustica.FEDERAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "AGRAVO_INTERNO",
                RecursalClassFamily.CIVIL_CONHECIMENTO,
                RecursalTribunal.STJ,
                RecursalTribunalDetalhado.STJ,
                InstanceLevel.SUPERIOR,
                OrgaoJulgadorTipo.RELATOR,
                true,
                false,
                false,
                true,
                true,
                false,
                true
        );
        RecursalSpecies species = new AgravoInterno(true, false, true);
        RecursalRoutePlan routePlan = new RecursalRuleCatalog(List.of(new com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.SuperiorTribunalJusticaRuleProfile())).route(context, species);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.REVOGAR_EFEITO_SUSPENSIVO);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.REVOGAR_EFEITO_ATIVO);
        assertFalse(snapshot.efeitoSuspensivoAtivo());
        assertFalse(snapshot.efeitoAtivoConcedido());
        assertEquals(RecursalLifecycleState.JULGAMENTO_MONOCRATICO, snapshot.state());
        Set<RecursalTransitionEvent> events = stateMachine.availableEvents(snapshot, species, routePlan);
        assertTrue(events.contains(RecursalTransitionEvent.CONCEDER_EFEITO_SUSPENSIVO));
        assertTrue(events.contains(RecursalTransitionEvent.CONCEDER_EFEITO_ATIVO));
    }

    private RecursalStateSnapshot move(RecursalStateSnapshot snapshot, RecursalCaseContext context, RecursalSpecies species, RecursalRoutePlan routePlan, RecursalTransitionEvent event) {
        return stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, event, "tester", Instant.parse("2026-03-11T12:00:00Z")), routePlan);
    }
}
