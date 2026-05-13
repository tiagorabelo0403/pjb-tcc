package com.tcc.pjb.backend.service.julgamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tcc.pjb.backend.model.entity.julgamento.enums.TipoVotoColegiado;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class JulgamentoPlacarClassifierTest {

    @Test
    void deveClassificarTiposCanonicosNosBucketsEsperados() {
        assertEquals(JulgamentoPlacarBucket.FAVOR, JulgamentoPlacarClassifier.classify(TipoVotoColegiado.DAR_PROVIMENTO));
        assertEquals(JulgamentoPlacarBucket.FAVOR, JulgamentoPlacarClassifier.classify(TipoVotoColegiado.ACOMPANHAR_RELATOR));
        assertEquals(JulgamentoPlacarBucket.CONTRA, JulgamentoPlacarClassifier.classify(TipoVotoColegiado.NEGAR_PROVIMENTO));
        assertEquals(JulgamentoPlacarBucket.PARCIAL, JulgamentoPlacarClassifier.classify(TipoVotoColegiado.PARCIAL_PROVIMENTO));
        assertEquals(JulgamentoPlacarBucket.PARCIAL, JulgamentoPlacarClassifier.classify(TipoVotoColegiado.DAR_PROVIMENTO_EM_PARTE));
        assertEquals(JulgamentoPlacarBucket.OUTROS, JulgamentoPlacarClassifier.classify(TipoVotoColegiado.PEDIR_VISTA));
        assertEquals(JulgamentoPlacarBucket.OUTROS, JulgamentoPlacarClassifier.classify(TipoVotoColegiado.OUTRO));
        assertEquals(JulgamentoPlacarBucket.OUTROS, JulgamentoPlacarClassifier.classify(null));
    }

    @Test
    void todoTipoDeVotoColegiadoDeveCairEmAlgumBucket() {
        for (TipoVotoColegiado tipo : EnumSet.allOf(TipoVotoColegiado.class)) {
            assertNotNull(JulgamentoPlacarClassifier.classify(tipo), () -> "Tipo sem bucket: " + tipo);
        }
    }
}
