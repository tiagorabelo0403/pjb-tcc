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

class NationalRecursalStateMachinePublicPaymentFlowTest {

    private final NationalRecursalStateMachine stateMachine = new NationalRecursalStateMachine();
    private final RecursalRuleCatalog catalog = new RecursalRuleCatalog(List.of(new TribunalJusticaRuleProfile()));

    @Test
    void deveEncadearRpvOuPrecatorioAntesDaBaixaQuandoHaRequisicaoPublica() {
        RecursalCaseContext context = new RecursalCaseContext(
                31L,
                "0000031-00.2026.8.06.0001",
                TipoJustica.ESTADUAL,
                RamoDireito.ADMINISTRATIVO,
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
                false,
                true
        );
        RecursalSpecies species = new RecursoEspecial(true, true, false, false);
        RecursalRoutePlan routePlan = catalog.route(context, species);
        RecursalStateSnapshot snapshot = stateMachine.initialSnapshot("resp-pagamento-publico", context, species, routePlan);

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
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.PROVER);

        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.AGUARDANDO_REQUISICAO_PAGAMENTO_PUBLICO);
        assertThat(stateMachine.availableEvents(snapshot, species, routePlan))
                .contains(RecursalTransitionEvent.EXPEDIR_RPV, RecursalTransitionEvent.EXPEDIR_PRECATORIO)
                .doesNotContain(RecursalTransitionEvent.BAIXAR, RecursalTransitionEvent.CERTIFICAR_TRANSITO);

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.EXPEDIR_RPV);
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.RPV_EXPEDIDA);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.REGISTRAR_PAGAMENTO_PUBLICO);
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.PAGAMENTO_PUBLICO_LIBERADO);
        assertThat(stateMachine.availableEvents(snapshot, species, routePlan))
                .contains(RecursalTransitionEvent.BAIXAR, RecursalTransitionEvent.CERTIFICAR_TRANSITO);
    }

    private RecursalStateSnapshot move(RecursalStateSnapshot snapshot, RecursalCaseContext context, RecursalSpecies species, RecursalRoutePlan routePlan, RecursalTransitionEvent event) {
        return stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, event, "tester", Instant.parse("2026-03-11T18:00:00Z")), routePlan);
    }
}
