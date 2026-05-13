package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public final class PjbDigitalCoreOnboardingProjectionService {

    public PjbDigitalCoreOnboardingProjection project(PjbInstituicaoRegistradaEvent event) {
        String institution = event == null ? "INSTITUICAO_NAO_IDENTIFICADA" : Objects.toString(event.institutionName(), "INSTITUICAO_NAO_IDENTIFICADA");
        return new PjbDigitalCoreOnboardingProjection(
                "Núcleos Digitais Universais do PJB",
                institution,
                List.of(
                        "Configurar competência territorial e material",
                        "Ativar perfis de rito e painel contextual",
                        "Definir regras de triagem de protocolo",
                        "Habilitar opção de Juízo 100% Digital quando aplicável",
                        "Publicar orientação institucional para advogados, partes e secretaria"),
                buildConfigurationChecklist(),
                List.of(
                        "sem módulo paralelo de juizado",
                        "sem protocolo refeito após redistribuição",
                        "sem decisão automatizada sem revisão humana",
                        "sem exposição pública de dado sigiloso"));
    }

    private static List<String> buildConfigurationChecklist() {
        List<String> checklist = new ArrayList<>(List.of("TRIBUNAL", "COMARCA", "UNIDADE_REFERENCIA"));
        Arrays.stream(RitoGrupoPrincipal.values())
                .map(g -> "RITO_GRUPO_" + g.name())
                .forEach(checklist::add);
        checklist.addAll(List.of("CAPACIDADE_DO_NUCLEO", "CALENDARIO_E_PRAZOS", "CUSTAS_E_GRATUIDADE", "POLITICA_DE_REDISTRIBUICAO"));
        return List.copyOf(checklist);
    }
}
