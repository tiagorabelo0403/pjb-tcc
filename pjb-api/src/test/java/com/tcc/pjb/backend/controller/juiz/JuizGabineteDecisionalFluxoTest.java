package com.tcc.pjb.backend.controller.juiz;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.BackendApplication;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=test",
                "spring.main.lazy-initialization=true",
                "spring.cache.type=none"
        }
)
@AutoConfigureMockMvc
class JuizGabineteDecisionalFluxoTest {

    private static final String JUIZ_EMAIL = "juiz.gabinete.it@test.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TrustedDeviceRepository trustedDeviceRepository;

    @MockitoBean
    private CapabilityRateLimiter capabilityRateLimiter;

    @BeforeEach
    void setup() {
        trustedDeviceRepository.deleteAll();
        usuarioRepository.deleteAll();
        Usuario juiz = Usuario.builder()
                .nome("Juiz IT")
                .email(JUIZ_EMAIL)
                .senha("x")
                .cpf("11111111199")
                .tipoUsuario(TipoUsuario.MAGISTRADO)
                .perfil(TipoUsuario.MAGISTRADO.name())
                .ativo(true)
                .build();
        juiz = usuarioRepository.save(juiz);

        TrustedDevice passkey = new TrustedDevice();
        passkey.setUsuario(juiz);
        passkey.setCredentialId("cred-juiz-gabinete-it");
        passkey.setPublicKey("pub-key-juiz-gabinete-it-placeholder");
        passkey.setAlias("dispositivo-it");
        passkey.setAttestationTrusted(false);
        passkey.setEnrollSuspectNetwork(false);
        passkey.setRiskScoreEnroll(0);
        trustedDeviceRepository.save(passkey);
    }

    @Test
    @WithMockUser(username = "adv@test.local", authorities = "ROLE_ADVOGADO")
    void fila_retorna_403_para_advogado() throws Exception {
        mockMvc.perform(get("/api/v1/juiz/gabinete-decisoes/fila"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = JUIZ_EMAIL, authorities = "ROLE_MAGISTRADO")
    void fila_retorna_200_para_magistrado_autenticado() throws Exception {
        mockMvc.perform(get("/api/v1/juiz/gabinete-decisoes/fila"))
                .andExpect(status().isOk());
    }
}
