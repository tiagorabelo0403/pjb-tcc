package com.tcc.pjb.backend.controller.procuradoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.profile.operational.ProcuradoriaParecerRequest;
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
 * familia Procuradoria (parecer). Ver {@code D-institutional-gate-filter-roda-antes-da-auth}.
 */
@AutoConfigureMockMvc
class InstitutionalProcuradoriaGateIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    TrustedDeviceRepository trustedDeviceRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void parecerProcuradoria_comProcuradorAutenticado_passaPeloGateInstitucional() throws Exception {
        Usuario procurador = new Usuario();
        procurador.setNome("Procurador Gate Probe");
        procurador.setEmail("procuradoria.gate@pjb.local");
        procurador.setCpf("66677788899");
        procurador.setAtivo(true);
        procurador.setTipoUsuario(TipoUsuario.PROCURADOR);
        procurador.setPerfil(TipoUsuario.PROCURADOR.name());
        procurador = usuarioRepository.save(procurador);
        long procuradorId = procurador.getId();
        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(procurador);
        passkey.setCredentialId("procuradoria-gate-probe-passkey");
        passkey.setPublicKey("pub-key-procuradoria-gate-probe");
        passkey.setAlias("procuradoria-gate-probe-passkey");
        passkey.setAuthenticatorAttachment("platform");
        passkey.setAttestationFmt("tpm");
        passkey.setAttestationTrusted(true);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);

        String body = objectMapper.writeValueAsString(new ProcuradoriaParecerRequest(
                "parecer de teste", "fundamentacao de teste", null, null, null, null, null, null, null));

        MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/procuradoria/operacional/processos/{processoId}/parecer", 7L)
                                .with(jwt().jwt(j -> j.claim("uid", String.valueOf(procuradorId)))
                                        .authorities(new SimpleGrantedAuthority("ROLE_PROCURADOR")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("Nao pode ser 401 (auth falhou antes do gate) nem 403 (gate barrou procurador legitimo)")
                .isNotIn(401, 403);
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("Gate rodou depois da auth e classificou a operacao de procuradoria")
                .isEqualTo("PROCURADORIA_PARECER");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed"))
                .as("Gate resolveu o procurador JWT via banco e liberou")
                .isEqualTo("true");
    }
}
