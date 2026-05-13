package com.tcc.pjb.backend.ai.juridica.v3.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector;
import com.tcc.pjb.backend.service.rito.RitoPackService;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LegalRitosEngineTest {

    @Test
    void includesCanonicalSelectionAndPackStages() {
        RitoPackService ritoPackService = mock(RitoPackService.class);
        CanonicalRitoSelector selector = mock(CanonicalRitoSelector.class);
        LegalRitosEngine engine = new LegalRitosEngine(ritoPackService, selector);

        var selected = new CanonicalRitoSelector.SelectedRito(
                Instant.now(),
                "test",
                null,
                null,
                com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.COMUM_ORDINARIO,
                "CANONICAL_RITO_RESOLVED",
                false,
                false,
                Map.of("effectiveRito", "COMUM_ORDINARIO")
        );
        when(selector.select(any(), anyString(), anyString())).thenReturn(selected);
        when(ritoPackService.get(com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.COMUM_ORDINARIO)).thenReturn(Optional.of(
                RitoDefinition.builder()
                        .rito("COMUM_ORDINARIO")
                        .title("Procedimento Comum")
                        .ramoSugerido("CIVIL")
                        .stages(List.of(RitoStage.builder().fase("POSTULATORIA").allowedNext(List.of("SANEAMENTO")).work(List.of()).build()))
                        .build()
        ));

        Map<String, Object> result = engine.inferRito(new LinkedHashMap<>(Map.of("rito", "COMUM_ORDINARIO")));

        assertEquals("COMUM_ORDINARIO", result.get("rito"));
        assertEquals("ok", result.get("status"));
        assertNotNull(result.get("selection"));
        assertInstanceOf(List.class, result.get("stages"));
    }
}
