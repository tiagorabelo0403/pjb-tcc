package com.tcc.pjb.backend.controller.psicossocial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.profile.operational.PsicossocialParecerRequest;
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
 * familia Psicossocial (parecer). Ver {@code D-institutional-gate-filter-roda-antes-da-auth}.
 */
@AutoConfigureMockMvc
class InstitutionalPsicossocialGateIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    TrustedDeviceRepository trustedDeviceRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void parecerPsicossocial_comPsicologoAutenticado_passaPeloGateInstitucional() throws Exception {
        Usuario psicologo = new Usuario();
        psicologo.setNome("Psicologo Gate Probe");
        psicologo.setEmail("psicossocial.gate@pjb.local");
        psicologo.setCpf("33344455566");
        psicologo.setAtivo(true);
        psicologo.setTipoUsuario(TipoUsuario.PSICOLOGO_JUDICIAL);
        psicologo.setPerfil(TipoUsuario.PSICOLOGO_JUDICIAL.name());
        psicologo = usuarioRepository.save(psicologo);
        long psicologoId = psicologo.getId();
        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(psicologo);
        passkey.setCredentialId("psicossocial-gate-probe-passkey");
        passkey.setPublicKey("pub-key-psicossocial-gate-probe");
        passkey.setAlias("psicossocial-gate-probe-passkey");
        passkey.setAuthenticatorAttachment("platform");
        passkey.setAttestationFmt("tpm");
        passkey.setAttestationTrusted(true);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);

        String body = objectMapper.writeValueAsString(new PsicossocialParecerRequest(
                "parecer de teste", "recomendacoes de teste", null, null, null, null, null, null, null));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/psicossocial/processos/{processoId}/parecer", 7L)
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(psicologoId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_PSICOLOGO_JUDICIAL")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("Nao pode ser 401 (auth falhou antes do gate) nem 403 (gate barrou psicologo legitimo)")
                .isNotIn(401, 403);
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("Gate rodou depois da auth e classificou a operacao psicossocial")
                .isEqualTo("PSICOSSOCIAL_PARECER");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed"))
                .as("Gate resolveu o psicologo JWT via banco e liberou")
                .isEqualTo("true");
    }
}
