package com.tcc.pjb.backend.controller.juiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizDespachoRequest;
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
 * familia Juiz — gabinete de decisoes (despacho). Familia distinta de
 * {@code MAGISTRATURA_ATO_PROCESSUAL} (ja coberta por {@code InstitutionalMagistraturaGateIT}) —
 * controller e endpoint diferentes. Ver {@code D-institutional-gate-filter-roda-antes-da-auth}.
 */
@AutoConfigureMockMvc
class InstitutionalJuizGabineteGateIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    TrustedDeviceRepository trustedDeviceRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void despachoJuiz_comJuizAutenticado_passaPeloGateInstitucional() throws Exception {
        Usuario juiz = new Usuario();
        juiz.setNome("Juiz Gabinete Gate Probe");
        juiz.setEmail("juiz.gabinete.gate@pjb.local");
        juiz.setCpf("70707070707");
        juiz.setAtivo(true);
        juiz.setTipoUsuario(TipoUsuario.JUIZ_ESTADUAL);
        juiz.setPerfil(TipoUsuario.JUIZ_ESTADUAL.name());
        juiz = usuarioRepository.save(juiz);
        long juizId = juiz.getId();
        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(juiz);
        passkey.setCredentialId("juiz-gabinete-gate-probe-passkey");
        passkey.setPublicKey("pub-key-juiz-gabinete-gate-probe");
        passkey.setAlias("juiz-gabinete-gate-probe-passkey");
        passkey.setAuthenticatorAttachment("platform");
        passkey.setAttestationFmt("tpm");
        passkey.setAttestationTrusted(true);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);

        String body = objectMapper.writeValueAsString(
                new JuizDespachoRequest("conteudo de teste", "fundamentacao de teste"));

        MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/juiz/gabinete-decisoes/processos/{processoId}/despacho", 7L)
                                .with(jwt().jwt(j -> j.claim("uid", String.valueOf(juizId)))
                                        .authorities(new SimpleGrantedAuthority("ROLE_JUIZ_ESTADUAL"),
                                                new SimpleGrantedAuthority("ROLE_MAGISTRATURA"),
                                                new SimpleGrantedAuthority("ROLE_MAGISTRADO"),
                                                new SimpleGrantedAuthority("ROLE_JUIZ")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("Nao pode ser 401 (auth falhou antes do gate) nem 403 (gate barrou juiz legitimo)")
                .isNotIn(401, 403);
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("Gate rodou depois da auth e classificou a operacao de despacho")
                .isEqualTo("JUIZ_DESPACHO");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed"))
                .as("Gate resolveu o juiz JWT via banco e liberou")
                .isEqualTo("true");
    }
}
