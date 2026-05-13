package com.tcc.pjb.backend.core.transito;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TransitoJulgadoArquivamentoRefinementArchitectureTest {

    @Test
    void engineDeveDelegarFluxosPatrimoniaisExpropriatoriosTerminaisEDiagnosticos() {
        Constructor<?> constructor = Arrays.stream(TransitoJulgadoArquivamentoEngine.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();
        Set<Class<?>> parameterTypes = toTypeSet(constructor);

        assertThat(constructor.getParameterCount()).isLessThanOrEqualTo(15);
        assertThat(parameterTypes)
                .contains(
                        TransitoJulgadoPatrimonialWorkflowSupport.class,
                        TransitoJulgadoExpropriationWorkflowSupport.class,
                        TransitoJulgadoTerminalWorkflowSupport.class,
                        TransitoJulgadoExecutionDiagnosticSupport.class)
                .doesNotContain(
                        PatrimonialConstrictionResolver.class,
                        ExternalConstrictionResolver.class,
                        ExternalConstrictionContingencyResolver.class,
                        ExternalConstrictionReconciliationResolver.class,
                        ExpropriationGovernanceResolver.class,
                        ExpropriationAuctionCycleResolver.class,
                        ExpropriationHomologationResolver.class,
                        ExpropriationSettlementResolver.class,
                        ExecutionClosureGovernanceResolver.class,
                        ExecutionSatisfactionResolver.class,
                        TerminalArchiveLinkResolver.class);
    }

    @Test
    void supportsDevemConcentrarResolversEspecializados() {
        assertThat(toTypeSet(firstConstructor(TransitoJulgadoPatrimonialWorkflowSupport.class)))
                .contains(
                        PatrimonialConstrictionResolver.class,
                        ExternalConstrictionResolver.class,
                        ExternalConstrictionContingencyResolver.class,
                        ExternalConstrictionReconciliationResolver.class,
                        ExecutionMeshStateService.class,
                        TransitoJulgadoNarrativeSupport.class);

        assertThat(toTypeSet(firstConstructor(TransitoJulgadoExpropriationWorkflowSupport.class)))
                .contains(
                        ExpropriationGovernanceResolver.class,
                        ExpropriationAuctionCycleResolver.class,
                        ExpropriationHomologationResolver.class,
                        ExpropriationSettlementResolver.class,
                        ExecutionEnforcementResolver.class,
                        ExecutionMeshStateService.class,
                        TransitoJulgadoNarrativeSupport.class);

        assertThat(toTypeSet(firstConstructor(TransitoJulgadoTerminalWorkflowSupport.class)))
                .contains(
                        ExecutionClosureGovernanceResolver.class,
                        ExecutionSatisfactionResolver.class,
                        TerminalArchiveLinkResolver.class,
                        ExecutionMeshStateService.class,
                        TransitoJulgadoNarrativeSupport.class);

        assertThat(toTypeSet(firstConstructor(TransitoJulgadoExecutionDiagnosticSupport.class)))
                .contains(
                        PostJudgmentOperationalResolver.class,
                        WorkItemRepository.class,
                        ExecutionMeshStateService.class,
                        TransitoJulgadoNarrativeSupport.class);
    }

    private Constructor<?> firstConstructor(Class<?> type) {
        return Arrays.stream(type.getDeclaredConstructors()).findFirst().orElseThrow();
    }

    private Set<Class<?>> toTypeSet(Constructor<?> constructor) {
        return Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());
    }
}
