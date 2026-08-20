package com.tcc.pjb.backend.controller.extrajudicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.extrajudicial.EscrituraLavraturaRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova end-to-end que o gate documental institucional roda depois da autenticacao para a
 * familia Extrajudicial (lavratura de escritura). Ver {@code D-institutional-gate-filter-roda-antes-da-auth}.
 */
@AutoConfigureMockMvc
class InstitutionalExtrajudicialGateIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    TrustedDeviceRepository trustedDeviceRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void lavraturaEscritura_comTabeliaoAutenticado_passaPeloGateInstitucional() throws Exception {
        Usuario tabeliao = new Usuario();
        tabeliao.setNome("Tabeliao Gate Probe");
        tabeliao.setEmail("extrajudicial.gate@pjb.local");
        tabeliao.setCpf("22233344455");
        tabeliao.setAtivo(true);
        tabeliao.setTipoUsuario(TipoUsuario.TABELIAO);
        tabeliao.setPerfil(TipoUsuario.TABELIAO.name());
        tabeliao = usuarioRepository.save(tabeliao);
        long tabeliaoId = tabeliao.getId();
        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(tabeliao);
        passkey.setCredentialId("extrajudicial-gate-probe-passkey");
        passkey.setPublicKey("pub-key-extrajudicial-gate-probe");
        passkey.setAlias("extrajudicial-gate-probe-passkey");
        passkey.setAuthenticatorAttachment("platform");
        passkey.setAttestationFmt("tpm");
        passkey.setAttestationTrusted(true);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);

        String body = objectMapper.writeValueAsString(new EscrituraLavraturaRequest(
                "COMPRA_VENDA", "resumo do ato", "partes resumo", "bens resumo", BigDecimal.TEN));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/extrajudicial/escrituras")
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(tabeliaoId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_TABELIAO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("Nao pode ser 401 (auth falhou antes do gate) nem 403 (gate barrou tabeliao legitimo)")
                .isNotIn(401, 403);
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("Gate rodou depois da auth e classificou a operacao extrajudicial")
                .isEqualTo("EXTRAJUDICIAL_LAVRAR_ESCRITURA");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed"))
                .as("Gate resolveu o tabeliao JWT via banco e liberou")
                .isEqualTo("true");
    }
}
