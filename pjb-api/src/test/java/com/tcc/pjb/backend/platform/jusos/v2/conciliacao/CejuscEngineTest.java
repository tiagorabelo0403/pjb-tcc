package com.tcc.pjb.backend.platform.jusos.v2.conciliacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.repository.AcordoHomologadoRepository;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.AcordoSuggestionPipelineAsyncService;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class CejuscEngineTest {

    @Test
    void shouldTrimResultadoCacheWhenOverflowHappens() throws Exception {
        CejuscEngine engine = new CejuscEngine(
                mock(ProcessoRepository.class),
                mock(AcordoHomologadoRepository.class),
                mock(PropostaAcordoRepository.class),
                mock(MembroEquipeRepository.class),
                mock(AcordoSuggestionPipelineAsyncService.class),
                mock(ApplicationEventPublisher.class),
                mock(AuditLedgerService.class),
                mock(UiHistoryService.class),
                mock(CurrentUserService.class),
                mock(NationalRulePackEngine.class),
                mock(NationalPrazoEngine.class)
        );

        Method remember = CejuscEngine.class.getDeclaredMethod("rememberResultado", UUID.class, CejuscEngine.ResultadoRegistro.class);
        remember.setAccessible(true);

        for (int i = 0; i < 20_500; i++) {
            CejuscEngine.ResultadoRegistro resultado = new CejuscEngine.ResultadoRegistro(
                    UUID.randomUUID(),
                    true,
                    false,
                    false,
                    java.util.List.of("proximo-passo-" + i)
            );
            remember.invoke(engine, resultado.sessaoId(), resultado);
        }

        Field cacheField = CejuscEngine.class.getDeclaredField("cacheResultados");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, ?> cache = (Map<Object, ?>) cacheField.get(engine);

        assertThat(cache).hasSizeLessThanOrEqualTo(20_000);
    }
}
