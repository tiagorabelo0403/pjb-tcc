package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoOmissao;

class NationalRecursalStateMachineTest {

    @Test
    void shouldFollowComplementaryPreparoAndJudgmentFlow() {
        RecursalCaseContext context = new RecursalCaseContext(
                10L,
                "0001234-56.2026.8.06.0001",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "Apelação Cível",
                RecursalClassFamily.CIVIL_CONHECIMENTO,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                InstanceLevel.FIRST_INSTANCE,
                OrgaoJulgadorTipo.MONOCRATICO,
                true,
                false,
                false,
                false,
                true,
                true,
                true
        );
        RecursalSpecies species = new RecursoEspecial(true, true, false, false);
        RecursalRoutePlan plan = new RecursalRoutePlan(
                "TJCE_RULE_PROFILE",
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                RecursalAuthority.VICE_PRESIDENCIA,
                RecursalTribunal.STJ,
                RecursalTribunalDetalhado.STJ,
                InstanceLevel.SUPERIOR,
                RecursalAuthority.RELATOR,
                RecursalAuthority.TURMA,
                PreparoDisposition.obrigatorio(true),
                new AdmissibilityDisposition(true, RecursalAuthority.VICE_PRESIDENCIA, true, RecursalAuthority.RELATOR, true, true, true, false),
                new PreventionDisposition(true, false, true, true),
                RemessaDisposition.externaAutuacaoDistribuicao()
        );
        NationalRecursalStateMachine stateMachine = new NationalRecursalStateMachine();
        RecursalStateSnapshot snapshot = stateMachine.initialSnapshot("R-1", context, species, plan);

        snapshot = stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, RecursalTransitionEvent.PROTOCOLAR, "tester", Instant.now()), plan);
        snapshot = stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, RecursalTransitionEvent.VALIDAR_TEMPESTIVIDADE, "tester", Instant.now()), plan);
        snapshot = stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, RecursalTransitionEvent.INTIMAR_COMPLEMENTACAO_PREPARO, "tester", Instant.now()), plan);
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.PREPARO_EM_COMPLEMENTACAO);
        assertThat(snapshot.preparoEmComplementacao()).isTrue();
        snapshot = stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, RecursalTransitionEvent.COMPLEMENTAR_PREPARO, "tester", Instant.now()), plan);
        snapshot = stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, RecursalTransitionEvent.ENCAMINHAR_ADMISSIBILIDADE, "tester", Instant.now()), plan);
        snapshot = stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, RecursalTransitionEvent.ADMITIR, "tester", Instant.now()), plan);
        snapshot = stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, RecursalTransitionEvent.AUTUAR_DESTINO, "tester", Instant.now()), plan);
        snapshot = stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, RecursalTransitionEvent.DISTRIBUIR, "tester", Instant.now()), plan);
        snapshot = stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, RecursalTransitionEvent.AFETAR_ORGAO_JULGADOR, "tester", Instant.now()), plan);
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.ADMISSIBILIDADE_DESTINO);
        assertThat(snapshot.tribunalDetalhadoAtual()).isEqualTo(RecursalTribunalDetalhado.STJ);
        assertThat(stateMachine.availableEvents(snapshot, species, plan)).contains(RecursalTransitionEvent.ADMITIR, RecursalTransitionEvent.SOBRESTAR_POR_PRECEDENTE);
    }

    @Test
    void shouldExposeEmbargosSanctionEvents() {
        NationalRecursalStateMachine stateMachine = new NationalRecursalStateMachine();
        RecursalCaseContext context = new RecursalCaseContext(
                1L,
                "n",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "Embargos de Declaração",
                RecursalClassFamily.CIVIL_CONHECIMENTO,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJSP,
                InstanceLevel.SECOND_INSTANCE,
                OrgaoJulgadorTipo.CAMARA,
                false,
                true,
                false,
                true,
                false,
                false,
                true
        );
        EmbargosDeclaracao species = new EmbargosDeclaracao(Set.of(new EmbargosDeclaracaoOmissao("omissão", false)), false, false, true);
        RecursalRoutePlan plan = new RecursalRoutePlan("TJSP_RULE_PROFILE", RecursalTribunal.TJ, RecursalTribunalDetalhado.TJSP, null, RecursalTribunal.TJ, RecursalTribunalDetalhado.TJSP, InstanceLevel.SECOND_INSTANCE, null, RecursalAuthority.CAMARA, PreparoDisposition.dispensado(), new AdmissibilityDisposition(false, null, false, null, false, false, false, false), PreventionDisposition.strictSameRelator(), RemessaDisposition.internaMesmosAutos());
        RecursalStateSnapshot snapshot = new RecursalStateSnapshot("ED-1", RecursalLifecycleState.JULGAMENTO_COLEGIADO, 4, RecursalTribunal.TJ, RecursalTribunalDetalhado.TJSP, InstanceLevel.SECOND_INSTANCE, RecursalAuthority.CAMARA, true, false, false, false, false, false, false, false, false, false, false, false, 1, false, Instant.now());
        assertThat(stateMachine.availableEvents(snapshot, species, plan)).contains(RecursalTransitionEvent.APLICAR_MULTA_EMBARGOS_PROTELATORIOS);
    }
}
