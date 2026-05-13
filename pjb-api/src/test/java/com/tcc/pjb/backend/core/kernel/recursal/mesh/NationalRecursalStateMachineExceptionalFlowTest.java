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

class NationalRecursalStateMachineExceptionalFlowTest {

    private final NationalRecursalStateMachine stateMachine = new NationalRecursalStateMachine();
    private final RecursalRuleCatalog tjCatalog = new RecursalRuleCatalog(List.of(new com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalJusticaRuleProfile()));

    @Test
    void devePercorrerFluxoDePreparoComplementarEDesercao() {
        RecursalCaseContext context = context(RecursalTribunal.TJ, RecursalTribunalDetalhado.TJSP, InstanceLevel.SECOND_INSTANCE, OrgaoJulgadorTipo.CAMARA, false, true, false, true);
        RecursalSpecies species = new RecursoEspecial(true, true, false, false);
        RecursalRoutePlan routePlan = tjCatalog.route(context, species);
        RecursalStateSnapshot snapshot = stateMachine.initialSnapshot("resp-1", context, species, routePlan);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.PROTOCOLAR);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.VALIDAR_TEMPESTIVIDADE);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.INTIMAR_COMPLEMENTACAO_PREPARO);
        assertEquals(RecursalLifecycleState.PREPARO_EM_COMPLEMENTACAO, snapshot.state());
        assertTrue(snapshot.preparoEmComplementacao());
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.DECLARAR_DESERCAO);
        assertEquals(RecursalLifecycleState.DESERTO, snapshot.state());
        assertFalse(snapshot.preparoSatisfeito());
        assertFalse(snapshot.preparoEmComplementacao());
    }

    @Test
    void devePercorrerFluxoDeSobrestamentoRetomadaEDiligencia() {
        RecursalCaseContext context = context(RecursalTribunal.TJ, RecursalTribunalDetalhado.TJSP, InstanceLevel.SECOND_INSTANCE, OrgaoJulgadorTipo.CAMARA, false, true, true, false);
        RecursalSpecies species = new RecursoExtraordinario(true, true, true, false);
        RecursalRoutePlan routePlan = tjCatalog.route(context, species);
        RecursalStateSnapshot snapshot = stateMachine.initialSnapshot("re-1", context, species, routePlan);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.PROTOCOLAR);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.VALIDAR_TEMPESTIVIDADE);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.DISPENSAR_PREPARO);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.INTIMAR_CONTRARRAZOES);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.ENCERRAR_CONTRARRAZOES);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.SOBRESTAR_POR_PRECEDENTE);
        assertEquals(RecursalLifecycleState.SOBRESTADO_POR_PRECEDENTE, snapshot.state());
        assertTrue(snapshot.sobrestadoPorPrecedente());
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.RETOMAR);
        assertEquals(RecursalLifecycleState.AGUARDANDO_APLICACAO_PRECEDENTE, snapshot.state());
        assertFalse(snapshot.sobrestadoPorPrecedente());
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.DISTINGUIR_CASO);
        assertEquals(RecursalLifecycleState.ADMISSIBILIDADE_ORIGEM, snapshot.state());
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.ADMITIR);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.AUTUAR_DESTINO);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.DISTRIBUIR);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.AFETAR_ORGAO_JULGADOR);
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.DETERMINAR_DILIGENCIA);
        assertEquals(RecursalLifecycleState.DILIGENCIA_DETERMINADA, snapshot.state());
        assertTrue(snapshot.diligenciaPendente());
        snapshot = advance(snapshot, context, species, routePlan, RecursalTransitionEvent.CUMPRIR_DILIGENCIA);
        assertTrue(Set.of(RecursalLifecycleState.JULGAMENTO_MONOCRATICO, RecursalLifecycleState.JULGAMENTO_COLEGIADO).contains(snapshot.state()));
        assertFalse(snapshot.diligenciaPendente());
    }

    private RecursalStateSnapshot advance(RecursalStateSnapshot snapshot, RecursalCaseContext context, RecursalSpecies species, RecursalRoutePlan routePlan, RecursalTransitionEvent event) {
        return stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, event, "tester", Instant.parse("2026-03-11T12:00:00Z")), routePlan);
    }

    private RecursalCaseContext context(
            RecursalTribunal tribunal,
            RecursalTribunalDetalhado detalhado,
            InstanceLevel instancia,
            OrgaoJulgadorTipo orgao,
            boolean monocratica,
            boolean materiaFederal,
            boolean materiaConstitucional,
            boolean exigePreparo) {
        return new RecursalCaseContext(
                1L,
                "0000001-00.2026.8.26.0001",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "PROCEDIMENTO_COMUM_CIVEL",
                RecursalClassFamily.CIVIL_CONHECIMENTO,
                tribunal,
                detalhado,
                instancia,
                orgao,
                monocratica,
                !monocratica,
                false,
                !exigePreparo,
                materiaFederal,
                materiaConstitucional,
                true
        );
    }
}
