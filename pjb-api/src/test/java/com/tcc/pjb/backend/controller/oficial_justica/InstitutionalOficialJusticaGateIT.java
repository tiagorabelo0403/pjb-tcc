package com.tcc.pjb.backend.controller.oficial_justica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRequest;
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
 * familia Oficial de Justica (oficio). Ver {@code D-institutional-gate-filter-roda-antes-da-auth}.
 *
 * <p>O controller exige header {@code unlockToken} para o fluxo de negocio completo; sem ele a
 * chamada retorna 428 (Precondition Required), nunca 401/403 — irrelevante para a garantia de
 * ordem de filtro sendo provada aqui, entao o teste nao fornece esse header.
 */
@AutoConfigureMockMvc
class InstitutionalOficialJusticaGateIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    TrustedDeviceRepository trustedDeviceRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void oficioOficialJustica_comOficialAutenticado_passaPeloGateInstitucional() throws Exception {
        Usuario oficial = new Usuario();
        oficial.setNome("Oficial Gate Probe");
        oficial.setEmail("oficial.gate@pjb.local");
        oficial.setCpf("88899900011");
        oficial.setAtivo(true);
        oficial.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        oficial.setPerfil(TipoUsuario.OFICIAL_JUSTICA.name());
        oficial = usuarioRepository.save(oficial);
        long oficialId = oficial.getId();
        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(oficial);
        passkey.setCredentialId("oficial-gate-probe-passkey");
        passkey.setPublicKey("pub-key-oficial-gate-probe");
        passkey.setAlias("oficial-gate-probe-passkey");
        passkey.setAuthenticatorAttachment("platform");
        passkey.setAttestationFmt("tpm");
        passkey.setAttestationTrusted(true);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);

        String body = objectMapper.writeValueAsString(new OficialJusticaOficioRequest(
                "assunto de teste", "destinatario de teste", "conteudo de teste",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        MockHttpServletResponse response = mockMvc.perform(
                        post("/api/v1/oficial-justica/processos/{processoId}/oficios", 7L)
                                .with(jwt().jwt(j -> j.claim("uid", String.valueOf(oficialId)))
                                        .authorities(new SimpleGrantedAuthority("ROLE_OFICIAL_JUSTICA")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("Nao pode ser 401 (auth falhou antes do gate) nem 403 (gate barrou oficial legitimo)")
                .isNotIn(401, 403);
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("Gate rodou depois da auth e classificou a operacao de oficio")
                .isEqualTo("OFICIAL_OFICIO");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed"))
                .as("Gate resolveu o oficial JWT via banco e liberou")
                .isEqualTo("true");
    }
}
