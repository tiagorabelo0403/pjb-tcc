package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalOperationalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalSigiloGovernanceService;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalDraftPreviewAssembler;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalProjectionAssembler;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RecursalPeticionamentoFacadeRefinementArchitectureTest {

    @Test
    void facadeDeveReferenciarWorkspaceEOperationalEmSubpacotesDedicados() {
        Constructor<?> constructor = Arrays.stream(RecursalPeticionamentoFacadeService.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(parameterTypes)
                .contains(
                        RecursalDraftPreviewAssembler.class,
                        RecursalProjectionAssembler.class,
                        RecursalSigiloGovernanceService.class,
                        RecursalOperationalAutomationService.class
                );
    }

    @Test
    void workspaceEOperationalDevemViverEmSubpacotesDedicados() {
        assertThat(RecursalDraftPreviewAssembler.class.getPackageName()).endsWith(".workspace");
        assertThat(RecursalProjectionAssembler.class.getPackageName()).endsWith(".workspace");
        assertThat(RecursalSigiloGovernanceService.class.getPackageName()).endsWith(".operational");
        assertThat(RecursalOperationalAutomationService.class.getPackageName()).endsWith(".operational");
    }

    @Test
    void facadeDeveManterCamposApontandoParaSubpacotesRefinados() {
        Map<String, String> fieldPackages = Arrays.stream(RecursalPeticionamentoFacadeService.class.getDeclaredFields())
                .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .collect(Collectors.toMap(Field::getName, field -> field.getType().getPackageName(), (left, right) -> left));

        assertThat(fieldPackages)
                .containsEntry("draftPreviewAssembler", RecursalDraftPreviewAssembler.class.getPackageName())
                .containsEntry("projectionAssembler", RecursalProjectionAssembler.class.getPackageName())
                .containsEntry("recursalSigiloGovernanceService", RecursalSigiloGovernanceService.class.getPackageName())
                .containsEntry("recursalOperationalAutomationService", RecursalOperationalAutomationService.class.getPackageName());
    }
}
