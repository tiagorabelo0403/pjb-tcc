package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceArrivalCheckpointRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

class DiligenceCheckpointServiceTest {

    @Test
    void confirmaChegadaDentroDaCercaUsandoTelemetriaRecente() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        DiligenceTelemetryService telemetryService = Mockito.mock(DiligenceTelemetryService.class);
        DiligenciaOperadorCheckpointEventoRepository repository = Mockito.mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenceReferenceResolverService referenceResolverService = Mockito.mock(DiligenceReferenceResolverService.class);
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> provider = Mockito.mock(ObjectProvider.class);
        DiligenceCheckpointService service = new DiligenceCheckpointService(currentUserService, telemetryService, repository, referenceResolverService, provider);
        when(currentUserService.getRequired()).thenReturn(usuario());
        when(telemetryService.resolveRecentOriginForCurrentUser(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, java.time.Duration.ofMinutes(20)))
                .thenReturn(Optional.of(new DiligenceTelemetryService.RouteOrigin(-4.2600d, -38.9300d, "OFICIAL_JUSTICA_TELEMETRIA", Instant.parse("2026-03-11T12:01:00Z"), 10d)));
        when(telemetryService.hashDevice("device-1")).thenReturn("ab".repeat(32));
        when(repository.countByOperatorUserIdAndCanalAndDiligenceReference(88L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "99")).thenReturn(2L);
        when(referenceResolverService.resolve(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "99"))
                .thenReturn(Optional.of(new DiligenceReferenceResolverService.ResolvedDiligenceReference(99L, 321L, "0001234-55.2026.8.06.0001", "MANDADO:99", "EXPEDICAO", "PENDENTE", null)));
        when(repository.save(any())).thenAnswer(inv -> {
            DiligenciaOperadorCheckpointEvento entity = inv.getArgument(0);
            entity.setId(10L);
            entity.setCreatedAt(Instant.parse("2026-03-11T12:02:00Z"));
            return entity;
        });

        var response = service.registerArrival(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, "99",
                new DiligenceArrivalCheckpointRequest(-4.2602d, -38.9302d, null, null, 120d, Instant.parse("2026-03-11T12:01:59Z"), null),
                "device-1");

        assertThat(response.dentroDaCerca()).isTrue();
        assertThat(response.classificacao()).isEqualTo("CHEGADA_CONFIRMADA");
        assertThat(response.fonte()).contains("TELEMETRIA");
        assertThat(response.workItemId()).isEqualTo(99L);
        assertThat(response.tentativaSequencia()).isEqualTo(3);
        assertThat(response.assinaturaLocalizacaoSha256()).hasSize(64);
    }

    @Test
    void listaHistoricoDeCheckpoints() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        DiligenceTelemetryService telemetryService = Mockito.mock(DiligenceTelemetryService.class);
        DiligenciaOperadorCheckpointEventoRepository repository = Mockito.mock(DiligenciaOperadorCheckpointEventoRepository.class);
        DiligenceReferenceResolverService referenceResolverService = Mockito.mock(DiligenceReferenceResolverService.class);
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> provider = Mockito.mock(ObjectProvider.class);
        DiligenceCheckpointService service = new DiligenceCheckpointService(currentUserService, telemetryService, repository, referenceResolverService, provider);
        when(currentUserService.getRequired()).thenReturn(usuario());
        when(repository.findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(88L, TelemetriaOperacionalCanal.DELEGADO, "DIL-1"))
                .thenReturn(List.of(DiligenciaOperadorCheckpointEvento.builder()
                        .operatorUserId(88L)
                        .operatorTipoUsuario(TipoUsuario.DELEGADO_POLICIA)
                        .canal(TelemetriaOperacionalCanal.DELEGADO)
                        .diligenceReference("DIL-1")
                        .checkpointTipo(com.tcc.pjb.backend.model.entity.enums.DiligenciaCheckpointTipo.CHEGADA)
                        .targetLatitude(-4.3d)
                        .targetLongitude(-38.9d)
                        .observedLatitude(-4.31d)
                        .observedLongitude(-38.91d)
                        .distanceMeters(90d)
                        .geofenceRadiusMeters(120d)
                        .insideGeofence(true)
                        .classification("CHEGADA_CONFIRMADA")
                        .source("REQUEST")
                        .workItemId(500L)
                        .processoId(800L)
                        .processoNumero("0001111-22.2026.8.06.0001")
                        .tentativaSequencia(1)
                        .locationSignatureSha256("cd".repeat(32))
                        .occurredAt(Instant.parse("2026-03-11T12:00:00Z"))
                        .createdAt(Instant.parse("2026-03-11T12:00:01Z"))
                        .build()));

        var response = service.history(TelemetriaOperacionalCanal.DELEGADO, "DIL-1", 20);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().diligenciaReferencia()).isEqualTo("DIL-1");
        assertThat(response.getFirst().workItemId()).isEqualTo(500L);
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(88L);
        usuario.setTipoUsuario(TipoUsuario.DELEGADO_POLICIA);
        usuario.setPerfil(TipoUsuario.DELEGADO_POLICIA.name());
        usuario.setCpf("12345678901");
        usuario.setEmail("delegado@pjb.test");
        usuario.setSenha("x");
        return usuario;
    }
}
