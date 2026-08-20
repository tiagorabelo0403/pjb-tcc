package com.tcc.pjb.backend.controller.perito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.profile.operational.PeritoLaudoRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova end-to-end que o gate documental institucional roda depois da autenticacao para a
 * familia Perito (laudo). Ver {@code D-institutional-gate-filter-roda-antes-da-auth}.
 */
@AutoConfigureMockMvc
class InstitutionalPeritoGateIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    TrustedDeviceRepository trustedDeviceRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void laudoPerito_comPeritoAutenticado_passaPeloGateInstitucional() throws Exception {
        Usuario perito = new Usuario();
        perito.setNome("Perito Gate Probe");
        perito.setEmail("perito.gate@pjb.local");
        perito.setCpf("77788899900");
        perito.setAtivo(true);
        perito.setTipoUsuario(TipoUsuario.PERITO);
        perito.setPerfil(TipoUsuario.PERITO.name());
        perito = usuarioRepository.save(perito);
        long peritoId = perito.getId();
        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(perito);
        passkey.setCredentialId("perito-gate-probe-passkey");
        passkey.setPublicKey("pub-key-perito-gate-probe");
        passkey.setAlias("perito-gate-probe-passkey");
        passkey.setAuthenticatorAttachment("platform");
        passkey.setAttestationFmt("tpm");
        passkey.setAttestationTrusted(true);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);

        String body = objectMapper.writeValueAsString(new PeritoLaudoRequest(
                "PERICIA_MEDICA", "conclusao de teste", "metodologia de teste",
                null, null, null, null, null, null));

        MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/perito/operacional/processos/{processoId}/laudo", 7L)
                                .with(jwt().jwt(j -> j.claim("uid", String.valueOf(peritoId)))
                                        .authorities(new SimpleGrantedAuthority("ROLE_PERITO")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("Nao pode ser 401 (auth falhou antes do gate) nem 403 (gate barrou perito legitimo)")
                .isNotIn(401, 403);
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("Gate rodou depois da auth e classificou a operacao de perito")
                .isEqualTo("PERITO_LAUDO");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed"))
                .as("Gate resolveu o perito JWT via banco e liberou")
                .isEqualTo("true");
    }
}
