package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiligenceRouteOptimizationServiceTest {

    @Test
    void usaOrigemDeTelemetriaQuandoRequestNaoInformarCoordenadas() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        DiligenceTelemetryService telemetryService = Mockito.mock(DiligenceTelemetryService.class);
        PjbTimeService timeService = new PjbTimeService(Clock.fixed(Instant.parse("2026-03-11T12:00:00Z"), ZoneId.of("UTC")), ZoneId.of("America/Fortaleza"));
        DiligenceRouteOptimizationService service = new DiligenceRouteOptimizationService(currentUserService, timeService, telemetryService, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        when(currentUserService.getRequired()).thenReturn(usuario(TipoUsuario.OFICIAL_JUSTICA));
        when(telemetryService.resolveRecentOriginForCurrentUser(Mockito.any()))
                .thenReturn(Optional.of(new DiligenceTelemetryService.RouteOrigin(-4.26d, -38.93d, "OFICIAL_JUSTICA_TELEMETRIA", Instant.parse("2026-03-11T11:55:00Z"), 12d)));

        var response = service.optimize(new DiligenceRouteOptimizationRequest(
                null,
                null,
                15,
                java.util.List.of(
                        new DiligenceRouteOptimizationRequest.StopInput("1", "Mandado 1", "Rua A", -4.30d, -38.95d, 1, Instant.parse("2026-03-11T18:00:00Z"), null, null),
                        new DiligenceRouteOptimizationRequest.StopInput("2", "Mandado 2", "Rua B", -4.31d, -38.96d, 2, Instant.parse("2026-03-11T20:00:00Z"), null, null)
                )
        ));

        assertThat(response.origem().fonte()).isEqualTo("OFICIAL_JUSTICA_TELEMETRIA");
        assertThat(response.warnings()).anyMatch(v -> v.contains("telemetria recente"));
        assertThat(response.rota()).hasSize(2);
    }

    @Test
    void adiaDiligenciaQuandoPrazoFatalJaNaoComportaChegada() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        DiligenceTelemetryService telemetryService = Mockito.mock(DiligenceTelemetryService.class);
        PjbTimeService timeService = new PjbTimeService(Clock.fixed(Instant.parse("2026-03-11T12:00:00Z"), ZoneId.of("UTC")), ZoneId.of("America/Fortaleza"));
        DiligenceRouteOptimizationService service = new DiligenceRouteOptimizationService(currentUserService, timeService, telemetryService, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        when(currentUserService.getRequired()).thenReturn(usuario(TipoUsuario.DELEGADO_POLICIA));
        when(telemetryService.resolveRecentOriginForCurrentUser(Mockito.any()))
                .thenReturn(Optional.empty());

        var response = service.optimize(new DiligenceRouteOptimizationRequest(
                -4.25d,
                -38.92d,
                20,
                java.util.List.of(
                        new DiligenceRouteOptimizationRequest.StopInput("1", "Busca 1", "Rua A", -4.2501d, -38.9201d, 1, Instant.parse("2026-03-11T12:05:00Z"), null, null),
                        new DiligenceRouteOptimizationRequest.StopInput("2", "Busca 2", "Rua B", -4.35d, -39.10d, 1, Instant.parse("2026-03-11T12:03:00Z"), null, null)
                )
        ));

        assertThat(response.adiadas()).isNotEmpty();
        assertThat(response.warnings()).anyMatch(v -> v.contains("adiada"));
    }

    private static Usuario usuario(TipoUsuario tipoUsuario) {
        Usuario usuario = new Usuario();
        usuario.setId(55L);
        usuario.setEmail("operador@pjb.test");
        usuario.setCpf("12345678901");
        usuario.setSenha("x");
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setPerfil(tipoUsuario.name());
        return usuario;
    }
}
