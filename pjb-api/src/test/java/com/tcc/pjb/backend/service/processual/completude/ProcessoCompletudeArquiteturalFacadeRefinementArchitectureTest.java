package com.tcc.pjb.backend.service.processual.completude;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.completude.apisurface.ProcessoApiSurfaceIssueResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.apisurface.ProcessoApiSurfaceSanityResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.certificacao.ProcessoCertificacaoOperacionalItemResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.certificacao.ProcessoCertificacaoOperacionalResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseCriticalFlowResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseLearningBlueprintResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseLearningHotspotResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseLearningLaneResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseLearningResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseSanityIssueResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseSanityResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.infraestrutura.ProcessoInfraestruturaSoberanaResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.substituicao.ProcessoSubstituicaoLegadosProvaResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.substituicao.ProcessoSubstituicaoLegadosResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.substituicao.ProcessoSubstituicaoLegadosSistemaResponse;
import com.tcc.pjb.backend.service.processual.completude.assembler.ProcessoCompletudeArquiteturalResponseAssembler;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ProcessoCompletudeArquiteturalFacadeRefinementArchitectureTest {

    @Test
    void dtoDeCompletudeDevemViverEmSubpacotesEspecializados() {
        assertThat(ProcessoApiSurfaceIssueResponse.class.getPackageName()).endsWith(".apisurface");
        assertThat(ProcessoApiSurfaceSanityResponse.class.getPackageName()).endsWith(".apisurface");
        assertThat(ProcessoCertificacaoOperacionalItemResponse.class.getPackageName()).endsWith(".certificacao");
        assertThat(ProcessoCertificacaoOperacionalResponse.class.getPackageName()).endsWith(".certificacao");
        assertThat(ProcessoCodebaseCriticalFlowResponse.class.getPackageName()).endsWith(".codebase");
        assertThat(ProcessoCodebaseLearningBlueprintResponse.class.getPackageName()).endsWith(".codebase");
        assertThat(ProcessoCodebaseLearningHotspotResponse.class.getPackageName()).endsWith(".codebase");
        assertThat(ProcessoCodebaseLearningLaneResponse.class.getPackageName()).endsWith(".codebase");
        assertThat(ProcessoCodebaseLearningResponse.class.getPackageName()).endsWith(".codebase");
        assertThat(ProcessoCodebaseSanityIssueResponse.class.getPackageName()).endsWith(".codebase");
        assertThat(ProcessoCodebaseSanityResponse.class.getPackageName()).endsWith(".codebase");
        assertThat(ProcessoInfraestruturaSoberanaResponse.class.getPackageName()).endsWith(".infraestrutura");
        assertThat(ProcessoSubstituicaoLegadosProvaResponse.class.getPackageName()).endsWith(".substituicao");
        assertThat(ProcessoSubstituicaoLegadosResponse.class.getPackageName()).endsWith(".substituicao");
        assertThat(ProcessoSubstituicaoLegadosSistemaResponse.class.getPackageName()).endsWith(".substituicao");
    }

    @Test
    void facadeDeveReferenciarAssemblerEmSubpacoteDedicado() {
        Constructor<?> constructor = Arrays.stream(ProcessoCompletudeArquiteturalFacadeService.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(parameterTypes).contains(ProcessoCompletudeArquiteturalResponseAssembler.class);
        assertThat(ProcessoCompletudeArquiteturalResponseAssembler.class.getPackageName()).endsWith(".assembler");
    }

    @Test
    void facadeDeveManterCampoApontandoParaAssemblerRefinado() {
        Map<String, String> fieldPackages = Arrays.stream(ProcessoCompletudeArquiteturalFacadeService.class.getDeclaredFields())
                .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .collect(Collectors.toMap(Field::getName, field -> field.getType().getPackageName(), (left, right) -> left));

        assertThat(fieldPackages)
                .containsEntry("responseAssembler", ProcessoCompletudeArquiteturalResponseAssembler.class.getPackageName());
    }
}
