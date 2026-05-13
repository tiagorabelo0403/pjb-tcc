package com.tcc.pjb.backend.core.lgpd;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.cidadao.CidadaoProcessoNacionalProjection;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.judicial.DjePublicacao;
import com.tcc.pjb.backend.model.entity.security.SigiloProcessoProofChallenge;
import java.util.List;
import org.junit.jupiter.api.Test;

class DataClassificationCatalogCoverageTest {

    private final DataClassificationCatalog catalog = new DataClassificationCatalog();

    @Test
    void deveCobrirEntidadesCriticasDeProcessoSigiloEPublicacao() {
        List<Class<?>> criticalEntities = List.of(
                Processo.class,
                DocumentoProcessual.class,
                InqueritoPolicialDigital.class,
                CidadaoProcessoNacionalProjection.class,
                SigiloAccessRequest.class,
                SigiloProcessoProofChallenge.class,
                DjePublicacao.class
        );

        criticalEntities.forEach(entity -> assertDoesNotThrow(() -> catalog.requireByEntityClass(entity)));
    }
}
