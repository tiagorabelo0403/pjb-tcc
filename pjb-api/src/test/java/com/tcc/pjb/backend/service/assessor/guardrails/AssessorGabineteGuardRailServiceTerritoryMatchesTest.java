package com.tcc.pjb.backend.service.assessor.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AssessorGabineteGuardRailServiceTerritoryMatchesTest {

    @Test
    void mesmaComarcaPorIdBateMesmoComTextoDivergente() throws Exception {
        Usuario assessor = new Usuario();
        assessor.setComarcaEntidade(comarcaComId(1L));
        assessor.setUf("CE");
        assessor.setComarca("FORTALEZA");

        Processo processo = new Processo();
        processo.setComarcaEntidade(comarcaComId(1L));
        processo.setUf("CE");
        processo.setComarca("FORTALEZA - CAPITAL");

        WorkItem item = new WorkItem();

        assertThat(invokeTerritoryMatches(assessor, processo, item)).isTrue();
    }

    @Test
    void comarcasDiferentesPorIdNaoBatemMesmoComTextoIgual() throws Exception {
        Usuario assessor = new Usuario();
        assessor.setComarcaEntidade(comarcaComId(1L));
        assessor.setUf("CE");
        assessor.setComarca("FORTALEZA");

        Processo processo = new Processo();
        processo.setComarcaEntidade(comarcaComId(2L));
        processo.setUf("CE");
        processo.setComarca("FORTALEZA");

        WorkItem item = new WorkItem();

        assertThat(invokeTerritoryMatches(assessor, processo, item)).isFalse();
    }

    @Test
    void fallbackTextualBateQuandoNenhumLadoResolveuComarca() throws Exception {
        Usuario assessor = new Usuario();
        assessor.setUf("CE");
        assessor.setComarca("Fortaleza");

        Processo processo = new Processo();
        processo.setUf("ce");
        processo.setComarca("FORTALEZA");

        WorkItem item = new WorkItem();

        assertThat(invokeTerritoryMatches(assessor, processo, item)).isTrue();
    }

    @Test
    void fallbackTextualNaoBateQuandoUfDivergeENenhumLadoResolveuComarca() throws Exception {
        Usuario assessor = new Usuario();
        assessor.setUf("CE");
        assessor.setComarca("Fortaleza");

        Processo processo = new Processo();
        processo.setUf("MG");
        processo.setComarca("Fortaleza");

        WorkItem item = new WorkItem();

        assertThat(invokeTerritoryMatches(assessor, processo, item)).isFalse();
    }

    @Test
    void workItemComComarcaTextualDivergenteNaoHerdaFkDoProcesso() throws Exception {
        Usuario assessor = new Usuario();
        assessor.setComarcaEntidade(comarcaComId(1L));
        assessor.setUf("CE");
        assessor.setComarca("Fortaleza");

        Processo processo = new Processo();
        processo.setComarcaEntidade(comarcaComId(1L));
        processo.setUf("CE");
        processo.setComarca("Fortaleza");

        WorkItem item = new WorkItem();
        item.setUf("CE");
        item.setComarca("Sobral");

        assertThat(invokeTerritoryMatches(assessor, processo, item)).isFalse();
    }

    @Test
    void workItemSemComarcaAlgumaAindaHerdaFkDoProcesso() throws Exception {
        Usuario assessor = new Usuario();
        assessor.setComarcaEntidade(comarcaComId(1L));
        assessor.setUf("CE");
        assessor.setComarca("Fortaleza");

        Processo processo = new Processo();
        processo.setComarcaEntidade(comarcaComId(1L));
        processo.setUf("CE");
        processo.setComarca("Fortaleza - Capital");

        WorkItem item = new WorkItem();
        item.setUf("CE");

        assertThat(invokeTerritoryMatches(assessor, processo, item)).isTrue();
    }

    private Comarca comarcaComId(Long id) {
        Comarca comarca = mock(Comarca.class);
        when(comarca.getId()).thenReturn(id);
        return comarca;
    }

    private boolean invokeTerritoryMatches(Usuario assessor, Processo processo, WorkItem item) throws Exception {
        AssessorGabineteGuardRailService service = mock(
                AssessorGabineteGuardRailService.class,
                Mockito.CALLS_REAL_METHODS
        );
        Method method = AssessorGabineteGuardRailService.class
                .getDeclaredMethod("territoryMatches", Usuario.class, Processo.class, WorkItem.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, assessor, processo, item);
    }
}
