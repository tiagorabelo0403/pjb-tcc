package com.tcc.pjb.backend.controller.magistratura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import java.util.List;
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
 * roda DEPOIS da autenticacao para uma familia radicalmente diferente da recursal —
 * magistratura, um controlador com PathVariable puro, sem router/perfil intermediario,
 * com {@code @PreAuthorize} listando 11 roles de magistratura. Complementa
 * {@link com.tcc.pjb.backend.controller.recursal.InstitutionalRecursalGateIT} — la a prova e
 * para a superficie unificada de recurso; aqui para atos judiciais.
 *
 * <p><b>Regressao que o teste captura:</b> se o filtro voltar a rodar antes da autenticacao,
 * o SecurityContext esta vazio quando {@code InstitutionalDocumentSecurityGateApplicationService.enforce}
 * tenta resolver o usuario — ou explode com NPE, ou (com o fix defensivo do {@code getOrNull})
 * pula o gate silenciosamente e nao seta os headers {@code X-PJB-Institutional-Gate-*}.
 *
 * <p><b>Assertion:</b> os headers {@code X-PJB-Institutional-Gate-Operation=MAGISTRATURA_ATO_PROCESSUAL}
 * e {@code X-PJB-Institutional-Gate-Allowed=true} devem estar presentes no response — o gate rodou,
 * resolveu o usuario JWT seedado no banco (JUIZ_ESTADUAL) e liberou. O status HTTP em si e
 * irrelevante para esta prova: os steps posteriores ao gate (rate limiter capabilities,
 * workbench service, etc) exigem Redis/mocks que sao ortogonais ao contrato de ordenacao
 * de filtro sendo verificado aqui.
 */
@AutoConfigureMockMvc
class InstitutionalMagistraturaGateIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    TrustedDeviceRepository trustedDeviceRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void magistraturaAtos_comJuizAutenticado_passaPeloGateInstitucional() throws Exception {
        Usuario juiz = new Usuario();
        juiz.setNome("Juiz Gate Probe");
        juiz.setEmail("juiz.gate@pjb.local");
        juiz.setCpf("98765432100");
        juiz.setAtivo(true);
        juiz.setTipoUsuario(TipoUsuario.JUIZ_ESTADUAL);
        juiz.setPerfil(TipoUsuario.JUIZ_ESTADUAL.name());
        juiz = usuarioRepository.save(juiz);
        long juizId = juiz.getId();
        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(juiz);
        passkey.setCredentialId("gate-probe-passkey");
        passkey.setPublicKey("pub-key-gate-probe");
        passkey.setAlias("passkey-gate-probe");
        passkey.setAuthenticatorAttachment("platform");
        passkey.setAttestationFmt("tpm");
        passkey.setAttestationTrusted(true);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);

        String body = objectMapper.writeValueAsString(new MagistraturaJudicialActCommandRequest(
                "DESPACHO", "conteudo", "fundamentacao", "dispositivo", "tipo", "local",
                null, null, null, null, null, "obs", null, null, null, null, null, null, List.of(), Boolean.FALSE));

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/magistratura/processos/{processoId}/atos", 7L)
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
                .as("Gate rodou depois da auth e classificou a operacao magistratura")
                .isEqualTo("MAGISTRATURA_ATO_PROCESSUAL");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed"))
                .as("Gate resolveu o juiz JWT via banco e liberou")
                .isEqualTo("true");
    }
}
