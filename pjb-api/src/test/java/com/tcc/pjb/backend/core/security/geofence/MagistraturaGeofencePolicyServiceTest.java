package com.tcc.pjb.backend.core.security.geofence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Usuario;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

class MagistraturaGeofencePolicyServiceTest {

    private final GeoIpLookupPort port = mock(GeoIpLookupPort.class);
    private final JudgeTravelExceptionRepository travelRepo = mock(JudgeTravelExceptionRepository.class);
    private final GeofenceProperties props = new GeofenceProperties(null, List.of(), true);
    private final Environment devEnvironment = criarEnvironment("dev");
    private final MagistraturaGeofencePolicyService service =
            new MagistraturaGeofencePolicyService(port, travelRepo, props, devEnvironment);

    @Test
    void ipDoMesmoEstadoDaLotacaoEPermitido() {
        Usuario usuario = usuarioCe();
        when(port.lookup("1.2.3.4")).thenReturn(new GeoLookupResult("BR", "CE", false, true));

        var avaliacao = service.avaliar(usuario, "1.2.3.4");

        assertThat(avaliacao.decisao()).isEqualTo(MagistraturaGeofencePolicyService.Decisao.PERMITIDO);
    }

    @Test
    void ipDeOutroEstadoSemExcecaoEBloqueado() {
        Usuario usuario = usuarioCe();
        when(port.lookup("1.2.3.4")).thenReturn(new GeoLookupResult("BR", "SP", false, true));
        when(travelRepo.existeExcecaoAtivaParaDestino(any(), any(), any(), any())).thenReturn(false);

        var avaliacao = service.avaliar(usuario, "1.2.3.4");

        assertThat(avaliacao.decisao()).isEqualTo(MagistraturaGeofencePolicyService.Decisao.BLOQUEADO_UF);
    }

    @Test
    void ipForaDoBrasilEBloqueado() {
        Usuario usuario = usuarioCe();
        when(port.lookup("1.2.3.4")).thenReturn(new GeoLookupResult("US", null, false, true));
        when(travelRepo.existeExcecaoAtivaParaDestino(any(), any(), any(), any())).thenReturn(false);

        var avaliacao = service.avaliar(usuario, "1.2.3.4");

        assertThat(avaliacao.decisao()).isEqualTo(MagistraturaGeofencePolicyService.Decisao.BLOQUEADO_PAIS);
    }

    @Test
    void ipDeVpnDetectadaEBloqueadoAntesDeQualquerOutraChecagem() {
        Usuario usuario = usuarioCe();
        when(port.lookup("1.2.3.4")).thenReturn(new GeoLookupResult("BR", "CE", true, true));

        var avaliacao = service.avaliar(usuario, "1.2.3.4");

        assertThat(avaliacao.decisao()).isEqualTo(MagistraturaGeofencePolicyService.Decisao.BLOQUEADO_VPN);
    }

    @Test
    void outroEstadoComExcecaoDeViagemAtivaEPermitido() {
        Usuario usuario = usuarioCe();
        when(port.lookup("1.2.3.4")).thenReturn(new GeoLookupResult("BR", "DF", false, true));
        when(travelRepo.existeExcecaoAtivaParaDestino(any(), any(), any(), any())).thenReturn(true);

        var avaliacao = service.avaliar(usuario, "1.2.3.4");

        assertThat(avaliacao.decisao()).isEqualTo(MagistraturaGeofencePolicyService.Decisao.PERMITIDO);
    }

    @Test
    void indisponivelForaDeProdEPermitidoPorPadrao() {
        Usuario usuario = usuarioCe();
        when(port.lookup("1.2.3.4")).thenReturn(GeoLookupResult.indisponivel());

        var avaliacao = service.avaliar(usuario, "1.2.3.4");

        assertThat(avaliacao.decisao()).isEqualTo(MagistraturaGeofencePolicyService.Decisao.PERMITIDO);
    }

    @Test
    void indisponivelEmProdComEnforceAtivoEBloqueado() {
        Usuario usuario = usuarioCe();
        when(port.lookup("1.2.3.4")).thenReturn(GeoLookupResult.indisponivel());
        Environment prodEnvironment = criarEnvironment("prod");
        MagistraturaGeofencePolicyService prodService =
                new MagistraturaGeofencePolicyService(port, travelRepo, props, prodEnvironment);

        var avaliacao = prodService.avaliar(usuario, "1.2.3.4");

        assertThat(avaliacao.decisao()).isEqualTo(MagistraturaGeofencePolicyService.Decisao.INDISPONIVEL);
    }

    private Usuario usuarioCe() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setUf("CE");
        return u;
    }

    private Environment criarEnvironment(String perfil) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(perfil);
        return env;
    }
}
