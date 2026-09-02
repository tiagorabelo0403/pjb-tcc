package com.tcc.pjb.backend.service.processual.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.busca.application.ProcessoBuscaAnalyticsApplicationService;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoAnalyticsAggregate;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoBuscaAggregate;
import com.tcc.pjb.backend.core.processo.dsl.application.ProcessoDslApplicationService;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.application.ProcessoEncaixeFinalApplicationService;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeCarteiraAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeFinalAggregate;
import com.tcc.pjb.backend.core.processo.policy.application.ProcessoPolicyVigenciaApplicationService;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyAggregate;
import com.tcc.pjb.backend.core.processo.posse.application.ProcessoPosseTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.posse.domain.ProcessoPosseAggregate;
import com.tcc.pjb.backend.core.processo.pregravacao.application.ProcessoPreGravacaoApplicationService;
import com.tcc.pjb.backend.core.processo.pregravacao.domain.ProcessoPreGravacaoAggregate;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProcessoSurfaceInsightGovernancaOrchestratorTest {

    private final ProcessoBuscaAnalyticsApplicationService buscaAn = mock(ProcessoBuscaAnalyticsApplicationService.class);
    private final ProcessoEncaixeFinalApplicationService encaixe = mock(ProcessoEncaixeFinalApplicationService.class);
    private final ProcessoDslApplicationService dsl = mock(ProcessoDslApplicationService.class);
    private final ProcessoPolicyVigenciaApplicationService policy = mock(ProcessoPolicyVigenciaApplicationService.class);
    private final ProcessoPosseTrabalhoApplicationService posse = mock(ProcessoPosseTrabalhoApplicationService.class);
    private final ProcessoPreGravacaoApplicationService preGrav = mock(ProcessoPreGravacaoApplicationService.class);
    private final ProcessoSurfaceInsightGovernancaOrchestrator orchestrator = new ProcessoSurfaceInsightGovernancaOrchestrator(
            buscaAn, encaixe, dsl, policy, posse, preGrav);

    @Test
    void buscaEAnalyticsDelegam() {
        var b = mock(ProcessoBuscaAggregate.class);
        var a = mock(ProcessoAnalyticsAggregate.class);
        when(buscaAn.buscar("cpf", "nome", "num", "CE", "For", "CIVIL", "ATIVO", "TJCE", 0, 20)).thenReturn(b);
        when(buscaAn.analytics("CIVIL", "TJCE", "CE", "For")).thenReturn(a);
        assertThat(orchestrator.buscar("cpf", "nome", "num", "CE", "For", "CIVIL", "ATIVO", "TJCE", 0, 20)).isSameAs(b);
        assertThat(orchestrator.analytics("CIVIL", "TJCE", "CE", "For")).isSameAs(a);
    }

    @Test
    void encaixeDelegaFinalECarteira() {
        var f = mock(ProcessoEncaixeFinalAggregate.class);
        var c = mock(ProcessoEncaixeCarteiraAggregate.class);
        when(encaixe.detalhar(1L)).thenReturn(f);
        when(encaixe.varrer(50)).thenReturn(c);
        assertThat(orchestrator.encaixeFinal(1L)).isSameAs(f);
        assertThat(orchestrator.encaixeCarteira(50)).isSameAs(c);
    }

    @Test
    void dslEPosseDelegam() {
        var d = mock(ProcessoDslAggregate.class);
        var p = mock(ProcessoPosseAggregate.class);
        when(dsl.detalhar(1L)).thenReturn(d);
        when(posse.detalhar(1L)).thenReturn(p);
        assertThat(orchestrator.dsl(1L)).isSameAs(d);
        assertThat(orchestrator.posse(1L)).isSameAs(p);
    }

    @Test
    void policyDelegaComEssemData() {
        var sem = mock(ProcessoPolicyAggregate.class);
        var com = mock(ProcessoPolicyAggregate.class);
        LocalDate em = LocalDate.of(2026, 6, 30);
        when(policy.avaliar(1L)).thenReturn(sem);
        when(policy.avaliar(1L, em)).thenReturn(com);
        assertThat(orchestrator.policy(1L)).isSameAs(sem);
        assertThat(orchestrator.policy(1L, em)).isSameAs(com);
    }

    @Test
    void preGravacaoDelega() {
        var pg = mock(ProcessoPreGravacaoAggregate.class);
        when(preGrav.avaliar(1L, "ADV", "PETICIONAR")).thenReturn(pg);
        assertThat(orchestrator.preGravacao(1L, "ADV", "PETICIONAR")).isSameAs(pg);
    }
}
