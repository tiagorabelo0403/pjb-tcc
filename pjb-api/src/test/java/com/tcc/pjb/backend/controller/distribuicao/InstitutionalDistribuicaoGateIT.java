package com.tcc.pjb.backend.controller.distribuicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova end-to-end que o gate documental institucional roda depois da autenticacao para a
 * familia Distribuicao (redistribuicao critica). Ver {@code D-institutional-gate-filter-roda-antes-da-auth}.
 */
@AutoConfigureMockMvc
class InstitutionalDistribuicaoGateIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    TrustedDeviceRepository trustedDeviceRepository;

    @Test
    void redistribuicaoCritica_comServidorAutenticado_passaPeloGateInstitucional() throws Exception {
        Usuario servidor = new Usuario();
        servidor.setNome("Servidor Gate Probe");
        servidor.setEmail("distribuicao.gate@pjb.local");
        servidor.setCpf("44455566677");
        servidor.setAtivo(true);
        servidor.setTipoUsuario(TipoUsuario.SERVIDOR);
        servidor.setPerfil(TipoUsuario.SERVIDOR.name());
        servidor = usuarioRepository.save(servidor);
        long servidorId = servidor.getId();
        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(servidor);
        passkey.setCredentialId("distribuicao-gate-probe-passkey");
        passkey.setPublicKey("pub-key-distribuicao-gate-probe");
        passkey.setAlias("distribuicao-gate-probe-passkey");
        passkey.setAuthenticatorAttachment("platform");
        passkey.setAttestationFmt("tpm");
        passkey.setAttestationTrusted(true);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);

        MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/distribuicao/processual/processos/{processoId}/redistribuicao", 7L)
                                .param("motivoImpedimento", "impedimento de teste")
                                .with(jwt().jwt(j -> j.claim("uid", String.valueOf(servidorId)))
                                        .authorities(new SimpleGrantedAuthority("ROLE_SERVIDOR"))))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("Nao pode ser 401 (auth falhou antes do gate) nem 403 (gate barrou servidor legitimo)")
                .isNotIn(401, 403);
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("Gate rodou depois da auth e classificou a operacao de distribuicao")
                .isEqualTo("DISTRIBUICAO_REDISCRITICA");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed"))
                .as("Gate resolveu o servidor JWT via banco e liberou")
                .isEqualTo("true");
    }
}
