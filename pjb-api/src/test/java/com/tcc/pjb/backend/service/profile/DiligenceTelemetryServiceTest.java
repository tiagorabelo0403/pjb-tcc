package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryUpsertRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorTelemetria;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorTelemetriaRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

class DiligenceTelemetryServiceTest {

    @Test
    void reaproveitaTelemetriaQuaseIdenticaNoMesmoDispositivo() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        DiligenciaOperadorTelemetriaRepository repository = Mockito.mock(DiligenciaOperadorTelemetriaRepository.class);
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> provider = Mockito.mock(ObjectProvider.class);
        DiligenceTelemetryService service = new DiligenceTelemetryService(currentUserService, repository, provider);
        when(currentUserService.getRequired()).thenReturn(usuario());
        DiligenciaOperadorTelemetria latest = DiligenciaOperadorTelemetria.builder()
                .operatorUserId(77L)
                .operatorTipoUsuario(TipoUsuario.OFICIAL_JUSTICA)
                .canal(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .deviceHash(hash("device-1"))
                .latitude(-4.26d)
                .longitude(-38.93d)
                .fonte("GPS")
                .capturadoEm(Instant.parse("2026-03-11T12:00:00Z"))
                .createdAt(Instant.parse("2026-03-11T12:00:01Z"))
                .build();
        when(repository.findTopByOperatorUserIdAndCanalOrderByCapturadoEmDesc(77L, TelemetriaOperacionalCanal.OFICIAL_JUSTICA)).thenReturn(Optional.of(latest));

        var response = service.register(TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                new RouteTelemetryUpsertRequest(-4.26001d, -38.93001d, 10d, null, 90, "gps", Instant.parse("2026-03-11T12:00:45Z"), true),
                "device-1");

        assertThat(response.reaproveitada()).isTrue();
    }

    @Test
    void persisteQuandoDispositivoOuPosicaoMudam() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        DiligenciaOperadorTelemetriaRepository repository = Mockito.mock(DiligenciaOperadorTelemetriaRepository.class);
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> provider = Mockito.mock(ObjectProvider.class);
        DiligenceTelemetryService service = new DiligenceTelemetryService(currentUserService, repository, provider);
        when(currentUserService.getRequired()).thenReturn(usuario());
        when(repository.findTopByOperatorUserIdAndCanalOrderByCapturadoEmDesc(77L, TelemetriaOperacionalCanal.DELEGADO)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            DiligenciaOperadorTelemetria entity = inv.getArgument(0);
            entity.setId(10L);
            entity.setCreatedAt(Instant.parse("2026-03-11T12:01:00Z"));
            return entity;
        });

        var response = service.register(TelemetriaOperacionalCanal.DELEGADO,
                new RouteTelemetryUpsertRequest(-4.30d, -38.95d, 8d, 50d, 80, "gps", Instant.parse("2026-03-11T12:00:59Z"), false),
                "device-9");

        ArgumentCaptor<DiligenciaOperadorTelemetria> captor = ArgumentCaptor.forClass(DiligenciaOperadorTelemetria.class);
        Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDeviceHash()).isNotBlank();
        assertThat(response.reaproveitada()).isFalse();
        assertThat(response.canal()).isEqualTo("DELEGADO");
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(77L);
        usuario.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        usuario.setPerfil(TipoUsuario.OFICIAL_JUSTICA.name());
        usuario.setCpf("12345678901");
        usuario.setEmail("oficial@pjb.test");
        usuario.setSenha("x");
        return usuario;
    }

    private static String hash(String value) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
