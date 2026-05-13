package com.tcc.pjb.backend.service.processual.peticionamento;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaPublicationGateService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaSecurityPipelineService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaStorageShieldService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMultimidiaComposerService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoThreatSentinelService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.PeticionamentoInitialIntakeWorkspaceService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.PeticionamentoJurisprudenciaWorkspaceService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PeticionamentoSessaoFacadeRefinementArchitectureTest {

    @Test
    void facadeDeveReferenciarWorkspaceEMediaEmSubpacotesDedicados() {
        Constructor<?> constructor = Arrays.stream(PeticionamentoSessaoFacadeService.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(parameterTypes)
                .contains(
                        PeticionamentoInitialIntakeWorkspaceService.class,
                        PeticionamentoJurisprudenciaWorkspaceService.class,
                        InstitutionalMultimediaWorkspaceService.class,
                        PeticionamentoMultimidiaComposerService.class,
                        PeticionamentoMediaSecurityPipelineService.class,
                        PeticionamentoThreatSentinelService.class,
                        PeticionamentoMediaStorageShieldService.class,
                        PeticionamentoMediaPublicationGateService.class
                );
    }

    @Test
    void workspaceEMediaDevemViverEmSubpacotesDedicados() {
        assertThat(PeticionamentoInitialIntakeWorkspaceService.class.getPackageName()).endsWith(".workspace");
        assertThat(PeticionamentoJurisprudenciaWorkspaceService.class.getPackageName()).endsWith(".workspace");
        assertThat(InstitutionalMultimediaWorkspaceService.class.getPackageName()).endsWith(".workspace");
        assertThat(PeticionamentoMultimidiaComposerService.class.getPackageName()).endsWith(".media");
        assertThat(PeticionamentoMediaSecurityPipelineService.class.getPackageName()).endsWith(".media");
        assertThat(PeticionamentoThreatSentinelService.class.getPackageName()).endsWith(".media");
        assertThat(PeticionamentoMediaStorageShieldService.class.getPackageName()).endsWith(".media");
        assertThat(PeticionamentoMediaPublicationGateService.class.getPackageName()).endsWith(".media");
    }

    @Test
    void facadeDeveManterCamposDeWorkspaceEMediaApontandoParaPacotesRefinados() {
        Map<String, String> fieldPackages = Arrays.stream(PeticionamentoSessaoFacadeService.class.getDeclaredFields())
                .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .collect(Collectors.toMap(Field::getName, field -> field.getType().getPackageName(), (left, right) -> left));

        assertThat(fieldPackages)
                .containsEntry("intakeWorkspaceService", PeticionamentoInitialIntakeWorkspaceService.class.getPackageName())
                .containsEntry("jurisprudenciaWorkspaceService", PeticionamentoJurisprudenciaWorkspaceService.class.getPackageName())
                .containsEntry("multimidiaComposerService", PeticionamentoMultimidiaComposerService.class.getPackageName())
                .containsEntry("mediaSecurityPipelineService", PeticionamentoMediaSecurityPipelineService.class.getPackageName())
                .containsEntry("threatSentinelService", PeticionamentoThreatSentinelService.class.getPackageName())
                .containsEntry("mediaStorageShieldService", PeticionamentoMediaStorageShieldService.class.getPackageName())
                .containsEntry("mediaPublicationGateService", PeticionamentoMediaPublicationGateService.class.getPackageName());
    }
}
