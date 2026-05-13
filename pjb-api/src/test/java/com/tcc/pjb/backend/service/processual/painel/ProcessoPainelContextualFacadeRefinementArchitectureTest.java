package com.tcc.pjb.backend.service.processual.painel;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.painel.contextual.ProcessoPainelContextualWorkspaceAssembler;
import com.tcc.pjb.backend.service.processual.painel.fonte.ProcessoPainelFonteOficialAssembler;
import com.tcc.pjb.backend.service.processual.painel.previdenciario.ProcessoPainelPrevidenciarioTrilhoAssembler;
import com.tcc.pjb.backend.service.processual.painel.rota.ProcessoPainelRotaTaticaAssembler;
import com.tcc.pjb.backend.service.processual.painel.telemetria.ProcessoPainelTelemetriaConectorAssembler;
import com.tcc.pjb.backend.service.processual.painel.trabalhista.ProcessoPainelBndtAssembler;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ProcessoPainelContextualFacadeRefinementArchitectureTest {

    @Test
    void facadeDeveReferenciarSubpacotesEspecializadosDoPainel() {
        Constructor<?> constructor = Arrays.stream(ProcessoPainelContextualFacadeService.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(parameterTypes)
                .contains(
                        ProcessoPainelContextualWorkspaceAssembler.class,
                        ProcessoPainelTelemetriaConectorAssembler.class,
                        ProcessoPainelFonteOficialAssembler.class,
                        ProcessoPainelBndtAssembler.class,
                        ProcessoPainelPrevidenciarioTrilhoAssembler.class,
                        ProcessoPainelRotaTaticaAssembler.class
                );
    }

    @Test
    void assemblersDevemViverEmSubpacotesDedicadosPorTipoDePainel() {
        assertThat(ProcessoPainelContextualWorkspaceAssembler.class.getPackageName()).endsWith(".contextual");
        assertThat(ProcessoPainelTelemetriaConectorAssembler.class.getPackageName()).endsWith(".telemetria");
        assertThat(ProcessoPainelFonteOficialAssembler.class.getPackageName()).endsWith(".fonte");
        assertThat(ProcessoPainelBndtAssembler.class.getPackageName()).endsWith(".trabalhista");
        assertThat(ProcessoPainelPrevidenciarioTrilhoAssembler.class.getPackageName()).endsWith(".previdenciario");
        assertThat(ProcessoPainelRotaTaticaAssembler.class.getPackageName()).endsWith(".rota");
    }

    @Test
    void facadeDeveManterCamposApontandoParaSubpacotesRefinados() {
        Map<String, String> fieldPackages = Arrays.stream(ProcessoPainelContextualFacadeService.class.getDeclaredFields())
                .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .collect(Collectors.toMap(Field::getName, field -> field.getType().getPackageName(), (left, right) -> left));

        assertThat(fieldPackages)
                .containsEntry("processoPainelContextualWorkspaceAssembler", ProcessoPainelContextualWorkspaceAssembler.class.getPackageName())
                .containsEntry("processoPainelTelemetriaConectorAssembler", ProcessoPainelTelemetriaConectorAssembler.class.getPackageName())
                .containsEntry("processoPainelFonteOficialAssembler", ProcessoPainelFonteOficialAssembler.class.getPackageName())
                .containsEntry("processoPainelBndtAssembler", ProcessoPainelBndtAssembler.class.getPackageName())
                .containsEntry("processoPainelPrevidenciarioTrilhoAssembler", ProcessoPainelPrevidenciarioTrilhoAssembler.class.getPackageName())
                .containsEntry("processoPainelRotaTaticaAssembler", ProcessoPainelRotaTaticaAssembler.class.getPackageName());
    }
}
