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

class NationalRecursalStateMachineOperationalAuditTest {

    private final NationalRecursalStateMachine stateMachine = new NationalRecursalStateMachine();
    private final RecursalRuleCatalog catalog = new RecursalRuleCatalog(List.of(new TribunalJusticaRuleProfile()));

    @Test
    void deveManterTrilhaAuditavelDeRemessaSustentacaoEPrecedente() {
        RecursalCaseContext context = upperCourtContext();
        RecursalSpecies species = new RecursoEspecial(true, true, false, false);
        RecursalRoutePlan routePlan = catalog.route(context, species);
        RecursalStateSnapshot snapshot = stateMachine.initialSnapshot("resp-audit", context, species, routePlan);

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.PROTOCOLAR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.VALIDAR_TEMPESTIVIDADE);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.DISPENSAR_PREPARO);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.INTIMAR_CONTRARRAZOES);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.ENCERRAR_CONTRARRAZOES);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.ADMITIR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.REGISTRAR_SAIDA_AUTOS,
                new RecursalTransitionDetails("REM-001", "MNI_ELETRONICO", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        assertThat(snapshot.remessaTrace().saidaRegistrada()).isTrue();
        assertThat(snapshot.remessaTrace().protocoloSaida()).isEqualTo("REM-001");
        assertThat(snapshot.remessaTrace().canalRemessa()).isEqualTo("MNI_ELETRONICO");

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.CONFIRMAR_RECEBIMENTO,
                new RecursalTransitionDetails(null, null, "RCB-778", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        assertThat(snapshot.remessaTrace().recebimentoDestinoConfirmado()).isTrue();
        assertThat(snapshot.remessaTrace().protocoloRecebimentoDestino()).isEqualTo("RCB-778");

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.DISTRIBUIR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.AFETAR_ORGAO_JULGADOR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.ADMITIR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.PEDIR_PAUTA_SUSTENTACAO,
                new RecursalTransitionDetails(null, null, null, null, "PAUTA-2026-041", "SESSAO-11", null, null, null, null, null, null, null, null, null, null, null, null));
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.ADIAR_SESSAO,
                new RecursalTransitionDetails(null, null, null, null, "PAUTA-2026-041", "SESSAO-11", null, "quorum_insuficiente", null, null, null, null, null, null, null, null, null, null));
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.SUSTENTAR,
                new RecursalTransitionDetails(null, null, null, null, "PAUTA-2026-041", "SESSAO-12", "OAB 99999/CE", null, null, null, null, null, null, null, null, null, null, null));
        assertThat(snapshot.sustentacaoOralTrace().solicitada()).isTrue();
        assertThat(snapshot.sustentacaoOralTrace().realizada()).isTrue();
        assertThat(snapshot.sustentacaoOralTrace().totalAdiamentos()).isEqualTo(1);
        assertThat(snapshot.sustentacaoOralTrace().motivoAdiamento()).isEqualTo("quorum_insuficiente");
        assertThat(snapshot.sustentacaoOralTrace().sustentante()).isEqualTo("OAB 99999/CE");

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.SOBRESTAR_POR_PRECEDENTE,
                new RecursalTransitionDetails(null, null, null, null, null, null, null, null, "Tema 1234", "STJ", "Tema repetitivo 1234", null, null, null, null, null, null, null));
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.RETOMAR,
                new RecursalTransitionDetails(null, null, null, null, null, null, null, null, "Tema 1234", "STJ", "Tema repetitivo 1234", null, null, null, null, null, null, null));
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.DISTINGUIR_CASO,
                new RecursalTransitionDetails(null, null, null, null, null, null, null, null, "Tema 1234", "STJ", "Tema repetitivo 1234", "distincao_fatica_da_base_condenatoria", null, null, null, null, null, null));
        assertThat(snapshot.precedentTrace().precedenteCodigo()).isEqualTo("Tema 1234");
        assertThat(snapshot.precedentTrace().precedenteTribunal()).isEqualTo("STJ");
        assertThat(snapshot.precedentTrace().distinguido()).isTrue();
        assertThat(snapshot.precedentTrace().fundamentoDistincao()).isEqualTo("distincao_fatica_da_base_condenatoria");
    }

    @Test
    void deveRemeterAutosAoJuizoCompetenteAposDefinicaoDoConflito() {
        RecursalCaseContext context = conflictContext();
        RecursalSpecies species = new ConflitoCompetencia(true, true, true, true);
        RecursalRoutePlan routePlan = catalog.route(context, species);
        RecursalStateSnapshot snapshot = stateMachine.initialSnapshot("cc-retorno", context, species, routePlan);

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.PROTOCOLAR,
                new RecursalTransitionDetails(null, null, null, null, null, null, null, null, null, null, null, null, "juizo_fazenda_morada_nova", "vara_civel_morada_nova", null, null, null, null));
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.VALIDAR_TEMPESTIVIDADE);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.DISPENSAR_PREPARO);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.ENCAMINHAR_ADMISSIBILIDADE);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.AUTUAR_DESTINO);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.DISTRIBUIR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.AFETAR_ORGAO_JULGADOR);
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.RECEBER_SUSCITADO,
                new RecursalTransitionDetails(null, null, null, null, null, null, null, null, null, null, null, null, "juizo_fazenda_morada_nova", "vara_civel_morada_nova", null, null, null, null));
        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.DEFINIR_COMPETENCIA,
                new RecursalTransitionDetails(null, null, null, null, null, null, null, null, null, null, null, null, "juizo_fazenda_morada_nova", "vara_civel_morada_nova", "vara_civel_morada_nova", "TJCE", null, null));

        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.COMPETENCIA_DEFINIDA);
        assertThat(stateMachine.availableEvents(snapshot, species, routePlan)).containsOnly(RecursalTransitionEvent.REMETER_AUTOS_JUIZO_COMPETENTE);

        snapshot = move(snapshot, context, species, routePlan, RecursalTransitionEvent.REMETER_AUTOS_JUIZO_COMPETENTE,
                new RecursalTransitionDetails(null, null, null, null, null, null, null, null, null, null, null, null, "juizo_fazenda_morada_nova", "vara_civel_morada_nova", "vara_civel_morada_nova", "TJCE", null, null));
        assertThat(snapshot.state()).isEqualTo(RecursalLifecycleState.RETORNO_AO_JUIZO_COMPETENTE);
        assertThat(snapshot.competenciaTrace().competenciaDefinida()).isTrue();
        assertThat(snapshot.competenciaTrace().juizoCompetente()).isEqualTo("vara_civel_morada_nova");
        assertThat(snapshot.competenciaTrace().tribunalCompetente()).isEqualTo("TJCE");
        assertThat(snapshot.competenciaTrace().autosRemetidosAoJuizoCompetente()).isTrue();
    }

    private RecursalCaseContext upperCourtContext() {
        return new RecursalCaseContext(
                41L,
                "0000041-00.2026.8.06.0001",
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
                false
        );
    }

    private RecursalCaseContext conflictContext() {
        return new RecursalCaseContext(
                42L,
                "0000042-00.2026.8.06.0001",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                RitoProcessual.COMUM_ORDINARIO,
                FaseProcessual.RECURSAL,
                "CONFLITO_DE_COMPETENCIA_CIVEL",
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
                false,
                true,
                false
        );
    }

    private RecursalStateSnapshot move(RecursalStateSnapshot snapshot,
                                       RecursalCaseContext context,
                                       RecursalSpecies species,
                                       RecursalRoutePlan routePlan,
                                       RecursalTransitionEvent event) {
        return move(snapshot, context, species, routePlan, event, RecursalTransitionDetails.empty());
    }

    private RecursalStateSnapshot move(RecursalStateSnapshot snapshot,
                                       RecursalCaseContext context,
                                       RecursalSpecies species,
                                       RecursalRoutePlan routePlan,
                                       RecursalTransitionEvent event,
                                       RecursalTransitionDetails details) {
        return stateMachine.transition(new RecursalTransitionCommand(snapshot, context, species, event, "tester", Instant.parse("2026-03-11T18:00:00Z"), details), routePlan);
    }
}
