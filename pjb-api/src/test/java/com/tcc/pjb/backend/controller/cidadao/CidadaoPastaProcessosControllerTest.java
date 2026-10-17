package com.tcc.pjb.backend.controller.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitExceededException;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitProperties;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.security.ratelimit.LocalSlidingWindowRateLimitStore;
import com.tcc.pjb.backend.service.cidadao.CidadaoPastaProcessosService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class CidadaoPastaProcessosControllerTest {

    private final CidadaoPastaProcessosService service = mock(CidadaoPastaProcessosService.class);
    private final Authentication cidadao = UsernamePasswordAuthenticationToken.authenticated("cidadao-7", null, List.of());
    private final CapabilityRateLimiter limiterDeUmaChamada = limiter();
    private final CidadaoPastaProcessosController pasta = new CidadaoPastaProcessosController(service, limiterDeUmaChamada);
    private final CidadaoProcessosController processos = new CidadaoProcessosController(service, limiterDeUmaChamada);

    private static CapabilityRateLimiter limiter() {
        CapabilityRateLimitProperties props = new CapabilityRateLimitProperties();
        props.setEnabled(true);
        props.setDefaultLimitTokens(1);
        props.setWindowSeconds(60);
        Clock clock = Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC);
        return new CapabilityRateLimiter(props, new LocalSlidingWindowRateLimitStore(), clock);
    }

    @Test
    void cotaConsumidaNaListagemDeProcessosBloqueiaAPastaSemConsultarOsProcessos() {
        processos.listar(cidadao, 0, 20, null, null, null);

        assertThatThrownBy(() -> pasta.listar(cidadao, 0, 20, null, null, null))
                .isInstanceOf(CapabilityRateLimitExceededException.class);
        verify(service, times(1)).listar(any(), any(), any(), any());
    }

    @Test
    void cotaConsumidaNaPastaBloqueiaAListagemDeProcessos() {
        pasta.listar(cidadao, 0, 20, null, null, null);

        assertThatThrownBy(() -> processos.listar(cidadao, 0, 20, null, null, null))
                .isInstanceOf(CapabilityRateLimitExceededException.class);
        verify(service, times(1)).listar(any(), any(), any(), any());
    }

    @Test
    void tamanhoDePaginaDaPastaContinuaLimitadoACem() {
        pasta.listar(cidadao, 0, 500, null, null, null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listar(pageable.capture(), any(), any(), any());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }
}
