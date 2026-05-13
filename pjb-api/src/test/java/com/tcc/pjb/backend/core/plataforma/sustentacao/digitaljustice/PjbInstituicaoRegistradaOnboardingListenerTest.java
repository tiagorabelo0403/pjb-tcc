package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PjbInstituicaoRegistradaOnboardingListenerTest {

    private final PjbDigitalCoreOnboardingProjectionService projectionService =
            new PjbDigitalCoreOnboardingProjectionService();
    private final PjbInstituicaoRegistradaOnboardingListener listener =
            new PjbInstituicaoRegistradaOnboardingListener(projectionService);

    @Test
    void eventNuloRetornaNulo() {
        assertNull(listener.on(null));
    }

    @Test
    void eventoRegistradoGeraProjecaoComPassos() {
        PjbInstituicaoRegistradaEvent event = new PjbInstituicaoRegistradaEvent(
                "TJSP", "SAO_PAULO", "Tribunal de Justiça de São Paulo", true, Instant.now());
        PjbDigitalCoreOnboardingProjection projection = listener.on(event);
        assertNotNull(projection);
        assertNotNull(projection.steps());
        assertTrue(projection.steps().size() > 0);
    }

    @Test
    void projecaoContemChecklistDeRitosParaTodosOsGrupos() {
        PjbInstituicaoRegistradaEvent event = new PjbInstituicaoRegistradaEvent(
                "TJES", "VITORIA", "Tribunal de Justiça do Espírito Santo", false, Instant.now());
        PjbDigitalCoreOnboardingProjection projection = listener.on(event);
        assertNotNull(projection);
        assertTrue(projection.configurationChecklist().stream()
                .anyMatch(item -> item.startsWith("RITO_GRUPO_")),
                "Checklist deve conter configuração de grupos de rito");
    }

    @Test
    void projecaoIdentificaInstituicao() {
        String nome = "Tribunal Regional do Trabalho da 2ª Região";
        PjbInstituicaoRegistradaEvent event = new PjbInstituicaoRegistradaEvent(
                "TRT2", "SAO_PAULO", nome, false, Instant.now());
        PjbDigitalCoreOnboardingProjection projection = listener.on(event);
        assertNotNull(projection);
        assertTrue(projection.targetInstitution().contains(nome)
                || projection.targetInstitution().equals(nome));
    }
}
