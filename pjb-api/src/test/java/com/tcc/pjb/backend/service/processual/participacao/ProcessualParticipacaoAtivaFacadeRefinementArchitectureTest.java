package com.tcc.pjb.backend.service.processual.participacao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.participacao.submission.ProcessualParticipacaoAtivaSubmissionSupport;
import com.tcc.pjb.backend.service.processual.participacao.workspace.ProcessualParticipacaoAtivaWorkspaceSupport;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ProcessualParticipacaoAtivaFacadeRefinementArchitectureTest {

    @Test
    void facadeDeveDelegarWorkspaceESubmissaoParaSuportesDedicados() {
        Constructor<?> constructor = Arrays.stream(ProcessualParticipacaoAtivaFacadeService.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(constructor.getParameterCount()).isLessThanOrEqualTo(9);
        assertThat(parameterTypes)
                .contains(ProcessualParticipacaoAtivaWorkspaceSupport.class, ProcessualParticipacaoAtivaSubmissionSupport.class);
    }

    @Test
    void workspaceESubmissionDevemViverEmSubpacotesDedicados() {
        assertThat(ProcessualParticipacaoAtivaWorkspaceSupport.class.getPackageName())
                .endsWith(".workspace");
        assertThat(ProcessualParticipacaoAtivaSubmissionSupport.class.getPackageName())
                .endsWith(".submission");
    }

    @Test
    void facadeNaoDeveReabsorverHeuristicasDeWorkspaceESubmissao() {
        Set<String> methodNames = Arrays.stream(ProcessualParticipacaoAtivaFacadeService.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertThat(methodNames)
                .doesNotContain(
                        "buildCapabilityMatrix",
                        "resolveActionCatalog",
                        "buildPendingViews",
                        "buildRecentSubmissions",
                        "buildRouting",
                        "buildSignaturePolicy",
                        "buildRepresentationGuard",
                        "buildSecurityGuard",
                        "buildDeadlineGuard",
                        "buildExperienceDifferential",
                        "preparePrimaryDocument",
                        "prepareAttachments",
                        "ensureNoDuplicate",
                        "buildReceptionDescription",
                        "summarizeAck"
                );
    }
}
