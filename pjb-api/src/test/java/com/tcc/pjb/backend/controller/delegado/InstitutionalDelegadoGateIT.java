package com.tcc.pjb.backend.controller.delegado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.profile.operational.DelegadoDiligenciaRequest;
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
 * familia Delegado (requisicao de diligencia). Ver {@code D-institutional-gate-filter-roda-antes-da-auth}.
 */
@AutoConfigureMockMvc
class InstitutionalDelegadoGateIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    TrustedDeviceRepository trustedDeviceRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void requisicaoDiligencia_comDelegadoAutenticado_passaPeloGateInstitucional() throws Exception {
        Usuario delegado = new Usuario();
        delegado.setNome("Delegado Gate Probe");
        delegado.setEmail("delegado.gate@pjb.local");
        delegado.setCpf("99900011122");
        delegado.setAtivo(true);
        delegado.setTipoUsuario(TipoUsuario.DELEGADO_POLICIA);
        delegado.setPerfil(TipoUsuario.DELEGADO_POLICIA.name());
        delegado = usuarioRepository.save(delegado);
        long delegadoId = delegado.getId();
        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(delegado);
        passkey.setCredentialId("delegado-gate-probe-passkey");
        passkey.setPublicKey("pub-key-delegado-gate-probe");
        passkey.setAlias("delegado-gate-probe-passkey");
        passkey.setAuthenticatorAttachment("platform");
        passkey.setAttestationFmt("tpm");
        passkey.setAttestationTrusted(true);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);

        String body = objectMapper.writeValueAsString(new DelegadoDiligenciaRequest(
                7L, 1L, 1L, "diligencia de teste", null, null));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/delegado/requisicao/diligencia")
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(delegadoId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_DELEGADO_POLICIA")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("Nao pode ser 401 (auth falhou antes do gate) nem 403 (gate barrou delegado legitimo)")
                .isNotIn(401, 403);
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("Gate rodou depois da auth e classificou a operacao de delegado")
                .isEqualTo("DELEGADO_REQUISICAO_DILIGENCIA");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed"))
                .as("Gate resolveu o delegado JWT via banco e liberou")
                .isEqualTo("true");
    }
}
