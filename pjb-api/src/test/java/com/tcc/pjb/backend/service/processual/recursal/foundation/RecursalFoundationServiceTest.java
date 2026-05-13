package com.tcc.pjb.backend.service.processual.recursal.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalFormalSectionLabels;
import com.tcc.pjb.backend.model.dto.processual.recursal.foundation.RecursalFoundationResponse;
import org.junit.jupiter.api.Test;

class RecursalFoundationServiceTest {

    private final RecursalFoundationService service = new RecursalFoundationService();

    @Test
    void deveExporEstruturaRecursalBaseComApelacaoERecursoAdesivo() {
        RecursalFoundationResponse response = service.describe();

        assertThat(response.classificacaoFundamentacao()).contains("LIVRE", "VINCULADA");
        assertThat(response.classificacaoEfeitos()).contains("DEVOLUTIVO", "SUSPENSIVO");
        assertThat(response.recursoAdesivo().recursosCabiveis())
                .containsExactlyInAnyOrder("APELACAO", "RECURSO_ESPECIAL", "RECURSO_EXTRAORDINARIO");
        assertThat(response.apelacao().pecasObrigatorias())
                .containsExactly(RecursalFormalSectionLabels.PETICAO_INTERPOSICAO, RecursalFormalSectionLabels.RAZOES_RECURSAIS);
        assertThat(response.regrasDePrazo())
                .extracting(rule -> rule.recurso() + ':' + rule.diasUteis())
                .contains("APELACAO:15", "EMBARGOS_DECLARACAO:5");
    }
}
