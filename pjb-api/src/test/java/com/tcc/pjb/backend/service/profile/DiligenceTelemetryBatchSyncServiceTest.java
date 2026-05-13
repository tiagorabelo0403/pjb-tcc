package com.tcc.pjb.backend.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryBatchSyncRequest;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryUpsertRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorTelemetriaRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

class DiligenceTelemetryBatchSyncServiceTest {

    @Test
    void sincronizaLoteDeTelemetria() {
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        DiligenciaOperadorTelemetriaRepository repository = Mockito.mock(DiligenciaOperadorTelemetriaRepository.class);
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> provider = Mockito.mock(ObjectProvider.class);
        DiligenceTelemetryService service = Mockito.spy(new DiligenceTelemetryService(currentUserService, repository, provider));
        when(currentUserService.getRequired()).thenReturn(usuario());
        Mockito.doReturn(new com.tcc.pjb.backend.model.dto.profile.RouteTelemetrySnapshotResponse(
                TipoUsuario.OFICIAL_JUSTICA.name(),
                TelemetriaOperacionalCanal.OFICIAL_JUSTICA.name(),
                -4.26d,
                -38.93d,
                10d,
                null,
                90,
                "GPS",
                true,
                "abcdef123456",
                false,
                Instant.parse("2026-03-11T12:01:00Z"),
                Instant.parse("2026-03-11T12:01:01Z")
        )).when(service).register(Mockito.eq(TelemetriaOperacionalCanal.OFICIAL_JUSTICA), Mockito.any(), Mockito.eq("device-1"));

        var response = service.registerBatch(TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                new RouteTelemetryBatchSyncRequest(java.util.List.of(
                        new RouteTelemetryUpsertRequest(-4.26d, -38.93d, 10d, null, 90, "gps", Instant.parse("2026-03-11T12:01:00Z"), true),
                        new RouteTelemetryUpsertRequest(-4.261d, -38.931d, 10d, null, 89, "gps", Instant.parse("2026-03-11T12:02:00Z"), true)
                )),
                "device-1");

        assertThat(response.recebidas()).isEqualTo(2);
        assertThat(response.persistidas()).isEqualTo(2);
        assertThat(response.amostras()).hasSize(2);
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
}
