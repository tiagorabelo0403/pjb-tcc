package com.tcc.pjb.backend.controller.mp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova end-to-end, contra a cadeia real de seguranca do Spring, que o
 * {@link com.tcc.pjb.backend.configs.security.InstitutionalCriticalActionHttpGuardFilter}
 * roda DEPOIS da autenticacao para a familia MP (manifestacao), fechando a extensao de
 * cobertura registrada em {@code D-institutional-gate-filter-roda-antes-da-auth} no DEBT_LOG.
 *
 * <p><b>Assertion:</b> os headers {@code X-PJB-Institutional-Gate-Operation=MP_MANIFESTACAO}
 * e {@code X-PJB-Institutional-Gate-Allowed=true} devem estar presentes no response. O status
 * HTTP final e irrelevante — o rate limiter (Redis) e a regra de negocio downstream sao
 * ortogonais ao contrato de ordenacao de filtro sendo verificado aqui.
 */
@AutoConfigureMockMvc
class InstitutionalMpGateIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    TrustedDeviceRepository trustedDeviceRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void manifestacaoMp_comPromotorAutenticado_passaPeloGateInstitucional() throws Exception {
        Usuario promotor = new Usuario();
        promotor.setNome("Promotor Gate Probe");
        promotor.setEmail("mp.gate@pjb.local");
        promotor.setCpf("60606060606");
        promotor.setAtivo(true);
        promotor.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        promotor.setPerfil(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO.name());
        promotor = usuarioRepository.save(promotor);
        long promotorId = promotor.getId();
        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(promotor);
        passkey.setCredentialId("mp-gate-probe-passkey");
        passkey.setPublicKey("pub-key-mp-gate-probe");
        passkey.setAlias("mp-gate-probe-passkey");
        passkey.setAuthenticatorAttachment("platform");
        passkey.setAttestationFmt("tpm");
        passkey.setAttestationTrusted(true);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);

        String body = objectMapper.writeValueAsString(Map.of("texto", "manifestacao de teste"));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/mp/manifestacao/{processoId}", 7L)
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(promotorId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBRO_MINISTERIO_PUBLICO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("Nao pode ser 401 (auth falhou antes do gate) nem 403 (gate barrou MP legitimo)")
                .isNotIn(401, 403);
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("Gate rodou depois da auth e classificou a operacao MP")
                .isEqualTo("MP_MANIFESTACAO");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed"))
                .as("Gate resolveu o MP JWT via banco e liberou")
                .isEqualTo("true");
    }
}
