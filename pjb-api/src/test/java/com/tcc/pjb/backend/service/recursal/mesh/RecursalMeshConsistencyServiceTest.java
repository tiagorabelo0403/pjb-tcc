package com.tcc.pjb.backend.service.recursal.mesh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AdmissibilityDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.PreparoDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.PreventionDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRoutePlan;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RemessaDisposition;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshConsistencyView;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalTransitionLedgerEntry;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalAggregateStateRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalTransitionLedgerRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecursalMeshConsistencyServiceTest {

    @Mock
    private RecursalAggregateStateRepository aggregateRepository;

    @Mock
    private RecursalProcessIntegrationStateRepository projectionRepository;

    @Mock
    private RecursalTransitionLedgerRepository ledgerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final RecursalMeshFingerprintService fingerprintService = new RecursalMeshFingerprintService();

    @Test
    void deveReconhecerConsistenciaGlobalQuandoEstadosCoincidem() throws Exception {
        RecursalMeshConsistencyService service = new RecursalMeshConsistencyService(aggregateRepository, projectionRepository, ledgerRepository, objectMapper, fingerprintService);
        Processo processo = processo(StatusProcesso.RECURSO_INTERPOSTO, FaseProcessual.RECURSAL);
        RecursalStateSnapshot snapshot = snapshot(2, RecursalLifecycleState.JULGAMENTO_COLEGIADO);
        RecursalRoutePlan routePlan = routePlan();
        RecursalAggregateState aggregate = aggregate(processo, snapshot, routePlan, "RESP", "TJSP_RULE_PROFILE");
        RecursalProcessIntegrationState projection = projection(processo, snapshot, routePlan, RecursalTransitionEvent.AFETAR_ORGAO_JULGADOR, 3);
        RecursalTransitionLedgerEntry ledger = ledger(snapshot, routePlan, RecursalTransitionEvent.AFETAR_ORGAO_JULGADOR);
        when(aggregateRepository.findById("resp-99")).thenReturn(Optional.of(aggregate));
        when(projectionRepository.findById("resp-99")).thenReturn(Optional.of(projection));
        when(ledgerRepository.findTop100ByRecursoIdOrderByToRevisionDesc("resp-99")).thenReturn(List.of(ledger));

        RecursalMeshConsistencyView view = service.verify("resp-99").orElseThrow();

        assertThat(view.overallConsistent()).isTrue();
        assertThat(view.inconsistencies()).isEmpty();
        assertThat(view.routePlanConsistent()).isTrue();
        assertThat(view.aggregateFingerprintConsistent()).isTrue();
        assertThat(view.projectionFingerprintConsistent()).isTrue();
        assertThat(view.ledgerFingerprintConsistent()).isTrue();
    }

    @Test
    void deveApontarDivergenciasQuandoProjectionELedgerNaoBatirem() throws Exception {
        RecursalMeshConsistencyService service = new RecursalMeshConsistencyService(aggregateRepository, projectionRepository, ledgerRepository, objectMapper, fingerprintService);
        Processo processo = processo(StatusProcesso.DISTRIBUIDO, FaseProcessual.CONHECIMENTO);
        RecursalStateSnapshot snapshot = snapshot(4, RecursalLifecycleState.TRANSITADO_EM_JULGADO);
        RecursalRoutePlan routePlan = routePlan();
        RecursalAggregateState aggregate = aggregate(processo, snapshot, routePlan, "RE", "TJRJ_RULE_PROFILE");
        RecursalProcessIntegrationState projection = projection(processo, snapshot(3, RecursalLifecycleState.BAIXADO), routePlan, RecursalTransitionEvent.BAIXAR, 2);
        RecursalTransitionLedgerEntry ledger = ledger(snapshot(3, RecursalLifecycleState.BAIXADO), routePlan, RecursalTransitionEvent.BAIXAR);
        projection.setIntegrityFingerprint("divergente");
        ledger.setIntegrityFingerprint("divergente");
        when(aggregateRepository.findById("resp-99")).thenReturn(Optional.of(aggregate));
        when(projectionRepository.findById("resp-99")).thenReturn(Optional.of(projection));
        when(ledgerRepository.findTop100ByRecursoIdOrderByToRevisionDesc("resp-99")).thenReturn(List.of(ledger));

        RecursalMeshConsistencyView view = service.verify("resp-99").orElseThrow();

        assertThat(view.overallConsistent()).isFalse();
        assertThat(view.inconsistencies()).isNotEmpty();
        assertThat(view.projectionConsistent()).isFalse();
        assertThat(view.ledgerConsistent()).isFalse();
        assertThat(view.processConsistent()).isFalse();
        assertThat(view.projectionFingerprintConsistent()).isFalse();
        assertThat(view.ledgerFingerprintConsistent()).isFalse();
    }

    private Processo processo(StatusProcesso status, FaseProcessual fase) {
        Processo processo = new Processo();
        processo.setId(1L);
        processo.setNumeroProcesso("0000001-00.2026.8.26.0001");
        processo.setStatusProcesso(status);
        processo.setFaseAtual(fase);
        return processo;
    }

    private RecursalStateSnapshot snapshot(int revision, RecursalLifecycleState state) {
        return new RecursalStateSnapshot(
                "resp-99",
                state,
                revision,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJSP,
                InstanceLevel.SECOND_INSTANCE,
                RecursalAuthority.CAMARA,
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                0,
                Instant.parse("2026-03-11T15:00:00Z")
        );
    }

    private RecursalRoutePlan routePlan() {
        return new RecursalRoutePlan(
                "TJSP_RULE_PROFILE",
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJSP,
                RecursalAuthority.PRESIDENCIA,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJSP,
                InstanceLevel.SECOND_INSTANCE,
                RecursalAuthority.PRESIDENCIA,
                RecursalAuthority.CAMARA,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(true, RecursalAuthority.PRESIDENCIA, false, null, false, false, false, false),
                PreventionDisposition.strictSameRelator(),
                RemessaDisposition.internaMesmosAutos()
        );
    }

    private RecursalAggregateState aggregate(Processo processo, RecursalStateSnapshot snapshot, RecursalRoutePlan routePlan, String speciesCode, String profileName) throws Exception {
        RecursalAggregateState aggregate = new RecursalAggregateState();
        aggregate.setRecursoId("resp-99");
        aggregate.setProcesso(processo);
        aggregate.setNumeroProcesso(processo.getNumeroProcesso());
        aggregate.setSpeciesCode(speciesCode);
        aggregate.setSpeciesName(speciesCode);
        aggregate.setProfileName(profileName);
        aggregate.setCurrentState(snapshot.state());
        aggregate.setTribunalAtual(snapshot.tribunalAtual());
        aggregate.setTribunalDetalhadoAtual(snapshot.tribunalDetalhadoAtual());
        aggregate.setInstanciaAtual(snapshot.instanciaAtual());
        aggregate.setAutoridadeAtual(snapshot.autoridadeAtual());
        aggregate.setPreparoSatisfeito(snapshot.preparoSatisfeito());
        aggregate.setAdmissibilidadePositiva(snapshot.admissibilidadePositiva());
        aggregate.setRemetido(snapshot.remetido());
        aggregate.setAutuadoDestino(snapshot.autuadoDestino());
        aggregate.setDistribuidoDestino(snapshot.distribuidoDestino());
        aggregate.setPreparoEmComplementacao(snapshot.preparoEmComplementacao());
        aggregate.setDiligenciaPendente(snapshot.diligenciaPendente());
        aggregate.setMultaEmbargos(snapshot.multaEmbargosProtelatoriosAplicada());
        aggregate.setSobrestadoPrecedente(snapshot.sobrestadoPorPrecedente());
        aggregate.setEfeitoSuspensivoAtivo(snapshot.efeitoSuspensivoAtivo());
        aggregate.setEfeitoAtivoConcedido(snapshot.efeitoAtivoConcedido());
        aggregate.setConhecimentoParcial(snapshot.conhecimentoParcial());
        aggregate.setIteracoesEmbargos(snapshot.iteracoesEmbargosDeclaracao());
        aggregate.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        aggregate.setRoutePlanJson(objectMapper.writeValueAsString(routePlan));
        aggregate.setContextJson(objectMapper.writeValueAsString(Set.of("ctx")));
        aggregate.setIntegrityFingerprint(fingerprintService.aggregateFingerprint(aggregate));
        return aggregate;
    }

    private RecursalProcessIntegrationState projection(Processo processo, RecursalStateSnapshot snapshot, RecursalRoutePlan routePlan, RecursalTransitionEvent lastEvent, int totalTransitions) throws Exception {
        RecursalProcessIntegrationState projection = new RecursalProcessIntegrationState();
        projection.setRecursoId("resp-99");
        projection.setProcesso(processo);
        projection.setNumeroProcesso(processo.getNumeroProcesso());
        projection.setSpeciesCode("RESP");
        projection.setProfileName("TJSP_RULE_PROFILE");
        projection.setCurrentState(snapshot.state());
        projection.setTribunalAtual(snapshot.tribunalAtual());
        projection.setTribunalDetalhadoAtual(snapshot.tribunalDetalhadoAtual());
        projection.setInstanciaAtual(snapshot.instanciaAtual());
        projection.setAutoridadeAtual(snapshot.autoridadeAtual());
        projection.setLastEvent(lastEvent);
        projection.setCurrentRevision(snapshot.revision());
        projection.setTotalTransitions(totalTransitions);
        projection.setIteracoesEmbargos(snapshot.iteracoesEmbargosDeclaracao());
        projection.setTransitadoEmJulgado(snapshot.state() == RecursalLifecycleState.TRANSITADO_EM_JULGADO);
        projection.setLastActor("tester");
        projection.setLastTransitionAt(snapshot.atualizadoEm());
        projection.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        projection.setRoutePlanJson(objectMapper.writeValueAsString(routePlan));
        projection.setIntegrityFingerprint(fingerprintService.projectionFingerprint(projection));
        return projection;
    }

    private RecursalTransitionLedgerEntry ledger(RecursalStateSnapshot snapshot, RecursalRoutePlan routePlan, RecursalTransitionEvent event) throws Exception {
        RecursalTransitionLedgerEntry entry = new RecursalTransitionLedgerEntry();
        entry.setRecursoId("resp-99");
        entry.setProcessoId(1L);
        entry.setSpeciesCode("RESP");
        entry.setProfileName("TJSP_RULE_PROFILE");
        entry.setCommandId("cmd-1");
        entry.setEventCode(event);
        entry.setFromState(RecursalLifecycleState.ADMISSIBILIDADE_ORIGEM);
        entry.setToState(snapshot.state());
        entry.setFromRevision(Math.max(snapshot.revision() - 1, 0));
        entry.setToRevision(snapshot.revision());
        entry.setActor("tester");
        entry.setOccurredAt(snapshot.atualizadoEm());
        entry.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        entry.setRoutePlanJson(objectMapper.writeValueAsString(routePlan));
        entry.setContextJson(objectMapper.writeValueAsString(Set.of("ctx")));
        entry.setIntegrityFingerprint(fingerprintService.ledgerFingerprint(entry));
        return entry;
    }
}
