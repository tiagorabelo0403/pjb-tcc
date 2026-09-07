package com.tcc.pjb.backend.service.servidor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.calendar.CalendarInstitutionalBridgeService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Prova que os 4 metodos de atos processuais em {@link ServidorSecretariaOperacionalService} sao
 * delegates puros para {@link ServidorSecretariaAtosService} -- extraido em F6 para tirar 5
 * dependencias exclusivas do bean raiz (16 -> 12 deps de construtor). A logica real de cada ato
 * (juntada, intimacao, mandado de citacao, conclusao para despacho) e coberta em
 * {@link ServidorSecretariaAtosServiceTest}.
 */
class ServidorSecretariaOperacionalServiceMandadoCitacaoTest {

    private final ServidorSecretariaAtosService atosService = mock(ServidorSecretariaAtosService.class);

    private final ServidorSecretariaOperacionalService service = new ServidorSecretariaOperacionalService(
            mock(com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory.class),
            mock(PainelServiceCommons.class),
            mock(ProcessoRepository.class),
            mock(WorkItemRepository.class),
            mock(PjbAuthorizationService.class),
            mock(CalendarInstitutionalBridgeService.class),
            atosService,
            mock(PainelSharedExperienceService.class),
            mock(PainelSignalReflectionService.class),
            mock(PainelNativeCollectionCompositionService.class),
            mock(PainelActionSurfaceCompositionService.class),
            mock(PainelExecutionSurfaceCompositionService.class));

    @Test
    void realizarJuntadaDelegaComOsMesmosArgumentos() {
        Map<String, Object> esperado = Map.of("status", "JUNTADA_REALIZADA");
        when(atosService.realizarJuntada(1L, "PETICAO", "descricao", "BALCAO")).thenReturn(esperado);

        assertThat(service.realizarJuntada(1L, "PETICAO", "descricao", "BALCAO")).isSameAs(esperado);
    }

    @Test
    void expedicaoIntimacaoDelegaComOsMesmosArgumentos() {
        Map<String, Object> esperado = Map.of("status", "INTIMACAO_EXPEDIDA");
        when(atosService.expedicaoIntimacao(2L, "Advogado", "conteudo", "15D", 5L, true, "origem", "fundamento", "obs", true))
                .thenReturn(esperado);

        Map<String, Object> resultado = service.expedicaoIntimacao(2L, "Advogado", "conteudo", "15D", 5L, true, "origem", "fundamento", "obs", true);

        assertThat(resultado).isSameAs(esperado);
        verify(atosService).expedicaoIntimacao(2L, "Advogado", "conteudo", "15D", 5L, true, "origem", "fundamento", "obs", true);
    }

    @Test
    void expedirMandadoCitacaoDelegaComOsMesmosArgumentos() {
        Map<String, Object> esperado = Map.of("status", "MANDADO_CITACAO_EXPEDIDO");
        when(atosService.expedirMandadoCitacao(3L, 9L, "endereco", "obs")).thenReturn(esperado);

        assertThat(service.expedirMandadoCitacao(3L, 9L, "endereco", "obs")).isSameAs(esperado);
    }

    @Test
    void conclusaoParaDespachoDelegaComOsMesmosArgumentos() {
        Map<String, Object> esperado = Map.of("status", "CONCLUSO_PARA_DESPACHO");
        when(atosService.conclusaoParaDespacho(4L, "motivo")).thenReturn(esperado);

        assertThat(service.conclusaoParaDespacho(4L, "motivo")).isSameAs(esperado);
    }
}
