package com.tcc.pjb.backend.core.lgpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import org.junit.jupiter.api.Test;

class DataClassificationCatalogTest {

    private final DataClassificationCatalog catalog = new DataClassificationCatalog();

    @Test
    void deveCatalogarProcessoComRlsRecomendadoESigiloJudicial() {
        DataClassificationEntry entry = catalog.requireByEntityClass(Processo.class);

        assertEquals("tb_processo", entry.tableName());
        assertTrue(entry.contains(DataClassificationCategory.DADOS_PESSOAIS));
        assertTrue(entry.contains(DataClassificationCategory.DADOS_JUDICIAIS));
        assertTrue(entry.contains(DataClassificationCategory.DADOS_SENSIVEIS));
        assertTrue(entry.rlsRecommended());
        assertTrue(entry.judicialSecrecyAware());
    }

    @Test
    void deveCatalogarInqueritoComoDadoSensivelJudicial() {
        DataClassificationEntry entry = catalog.requireByEntityClass(InqueritoPolicialDigital.class);

        assertEquals("tb_inquerito_policial_digital", entry.tableName());
        assertTrue(entry.contains(DataClassificationCategory.DADOS_SENSIVEIS));
        assertTrue(entry.accessControls().contains("STEP_UP_FORTE"));
    }
}
