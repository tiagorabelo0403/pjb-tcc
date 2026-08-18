package com.tcc.pjb.backend.core.processo.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.kernel.casefile.CaseFileEventStore;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalRelationType;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingRole;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.CaseEdgeRepository;
import com.tcc.pjb.backend.model.repository.CaseFileRepository;
import com.tcc.pjb.backend.model.repository.CaseProceedingRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.casefile.CaseContinuityOrchestratorService;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "spring.cache.type=none")
class ProcessoLifecycleCaseContinuityFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private ProcessoLifecycleMachine processoLifecycleMachine;

    @Autowired
    private CaseContinuityOrchestratorService caseContinuityOrchestratorService;

    @Autowired
    private CaseFileRepository caseFileRepository;

    @Autowired
    private CaseProceedingRepository caseProceedingRepository;

    @Autowired
    private CaseEdgeRepository caseEdgeRepository;

    @MockitoBean
    private CaseFileEventStore caseFileEventStore;

    @MockitoBean
    private AuditLedgerService auditLedgerService;

    @Test
    void deveMaterializarRamificacaoExecutivaEFechamentoNoCasefileAoLongoDoLifecycle() {
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("LIFE-2026-0001")
                .numeroUnificado("0008201-11.2026.8.06.0001")
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .uf("CE")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .faseAtual(FaseProcessual.RECURSAL)
                .statusProcesso(StatusProcesso.TRANSITO_EM_JULGADO)
                .build());

        caseContinuityOrchestratorService.ensureRootCase(processo.getId(), "it-round-124");

        var caseFile = caseFileRepository.findByRootProcessoId(processo.getId()).orElseThrow();
        var rootBefore = caseProceedingRepository.findFirstByLinkedProcessoId(processo.getId()).orElseThrow();
        assertThat(rootBefore.getCaseFileId()).isEqualTo(caseFile.getId());
        assertThat(rootBefore.getProceedingRole()).isEqualTo(CaseProceedingRole.ROOT);
        assertThat(rootBefore.getContinuityTrack()).isEqualTo(CaseContinuityTrack.TRANSITO);

        var cumprimentoDecision = processoLifecycleMachine.apply(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO);
        processoRepository.save(processo);

        assertThat(cumprimentoDecision.permitida()).isTrue();
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.CUMPRIMENTO_SENTENCA);
        assertThat(processo.getFaseAtual()).isEqualTo(FaseProcessual.CUMPRIMENTO_SENTENCA);

        var proceedingsAfterCumprimento = caseProceedingRepository.findAllByCaseFileId(caseFile.getId());
        assertThat(proceedingsAfterCumprimento).hasSize(2);
        var rootAfterCumprimento = proceedingsAfterCumprimento.stream()
                .filter(item -> item.getProceedingRole() == CaseProceedingRole.ROOT)
                .findFirst()
                .orElseThrow();
        var cumprimento = proceedingsAfterCumprimento.stream()
                .filter(item -> item.getProceedingRole() == CaseProceedingRole.CUMPRIMENTO)
                .findFirst()
                .orElseThrow();
        assertThat(rootAfterCumprimento.getStatus()).isEqualTo(CaseProceedingStatus.ACTIVE);
        assertThat(rootAfterCumprimento.getContinuityTrack()).isEqualTo(CaseContinuityTrack.CUMPRIMENTO);
        assertThat(cumprimento.getStatus()).isEqualTo(CaseProceedingStatus.ACTIVE);
        assertThat(cumprimento.getContinuityTrack()).isEqualTo(CaseContinuityTrack.CUMPRIMENTO);
        assertThat(cumprimento.getParentProceedingKey()).isEqualTo(rootAfterCumprimento.getProceedingKey());
        assertThat(caseEdgeRepository.findAllByCaseFileId(caseFile.getId()))
                .anySatisfy(edge -> {
                    assertThat(edge.getFromProceedingKey()).isEqualTo(rootAfterCumprimento.getProceedingKey());
                    assertThat(edge.getToProceedingKey()).isEqualTo(cumprimento.getProceedingKey());
                    assertThat(edge.getRelationType()).isEqualTo(RecursalRelationType.EXECUTION_CONTINUATION);
                });

        var arquivamentoDecision = processoLifecycleMachine.apply(processo, ProcessoLifecycleAction.ARQUIVAR);
        processoRepository.save(processo);

        assertThat(arquivamentoDecision.permitida()).isTrue();
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.ARQUIVADO);
        assertThat(caseProceedingRepository.findAllByCaseFileId(caseFile.getId()))
                .allSatisfy(item -> {
                    assertThat(item.getStatus()).isEqualTo(CaseProceedingStatus.CLOSED);
                    assertThat(item.getContinuityTrack()).isEqualTo(CaseContinuityTrack.ARQUIVADO);
                });
        var rootAfterArchive = caseProceedingRepository.findAllByCaseFileId(caseFile.getId()).stream()
                .filter(item -> item.getProceedingKey().equals(rootAfterCumprimento.getProceedingKey()))
                .findFirst()
                .orElseThrow();
        assertThat(rootAfterArchive.getProceedingRole()).isEqualTo(CaseProceedingRole.TERMINAL);

        var desarquivamentoDecision = processoLifecycleMachine.apply(processo, ProcessoLifecycleAction.DESARQUIVAR);
        processoRepository.save(processo);

        assertThat(desarquivamentoDecision.permitida()).isTrue();
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.EM_ANDAMENTO);
        assertThat(processo.getFaseAtual()).isEqualTo(FaseProcessual.CONHECIMENTO);

        var proceedingsAfterReopen = caseProceedingRepository.findAllByCaseFileId(caseFile.getId());
        assertThat(proceedingsAfterReopen).hasSize(2);
        assertThat(proceedingsAfterReopen)
                .allSatisfy(item -> assertThat(item.getStatus()).isEqualTo(CaseProceedingStatus.ACTIVE));
        var rootAfterReopen = proceedingsAfterReopen.stream()
                .filter(item -> item.getProceedingKey().equals(rootAfterCumprimento.getProceedingKey()))
                .findFirst()
                .orElseThrow();
        assertThat(rootAfterReopen.getProceedingRole()).isEqualTo(CaseProceedingRole.ROOT);
        assertThat(rootAfterReopen.getContinuityTrack()).isEqualTo(CaseContinuityTrack.REATIVADO);
        assertThat(caseEdgeRepository.findAllByCaseFileId(caseFile.getId()).stream()
                .sorted(Comparator.comparing(edge -> edge.getRelationType().name()))
                .map(edge -> edge.getRelationType().name())
                .toList())
                .contains("ARCHIVAL_CONTINUATION", "EXECUTION_CONTINUATION");
    }

    @Test
    void naoDeveAbrirRamificacaoExecutivaQuandoCumprimentoForIncompativelComRitoPenal() {
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("LIFE-2026-0002")
                .numeroUnificado("0008202-22.2026.8.06.0001")
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.PENAL)
                .rito(RitoProcessual.PROCEDIMENTO_PENAL_COMUM)
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .uf("CE")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .faseAtual(FaseProcessual.RECURSAL)
                .statusProcesso(StatusProcesso.TRANSITO_EM_JULGADO)
                .build());

        caseContinuityOrchestratorService.ensureRootCase(processo.getId(), "it-round-124-penal");

        var before = caseFileRepository.findByRootProcessoId(processo.getId()).orElseThrow();
        var decision = processoLifecycleMachine.apply(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO);

        assertThat(decision.permitida()).isFalse();
        assertThat(decision.motivo()).contains("rito penal");
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.TRANSITO_EM_JULGADO);
        assertThat(caseProceedingRepository.findAllByCaseFileId(before.getId())).singleElement().satisfies(item -> {
            assertThat(item.getProceedingRole()).isEqualTo(CaseProceedingRole.ROOT);
            assertThat(item.getStatus()).isEqualTo(CaseProceedingStatus.RECONCILED);
            assertThat(item.getContinuityTrack()).isEqualTo(CaseContinuityTrack.TRANSITO);
        });
        assertThat(caseEdgeRepository.findAllByCaseFileId(before.getId())).isEmpty();
    }
}
