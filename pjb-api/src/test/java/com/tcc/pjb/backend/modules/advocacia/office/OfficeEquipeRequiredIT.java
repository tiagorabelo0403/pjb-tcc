package com.tcc.pjb.backend.modules.advocacia.office;

import com.tcc.pjb.backend.BackendApplication;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;

@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OfficeEquipeRequiredIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Test
    void semEquipeSelecionada_retorna428ComPayload() throws Exception {
        Usuario u = new Usuario();
        u.setNome("Advogado Teste");
        u.setEmail("adv.teste@pjb.local");
        u.setCpf("00000000000");
        u.setAtivo(true);
        u.setTipoUsuario(TipoUsuario.ADVOGADO);
        u = usuarioRepository.save(u);
        long usuarioId = u.getId();

        mockMvc.perform(
                        get("/api/v1/advogado/escritorio/policy")
                                .with(jwt().jwt(j -> j.claim("uid", String.valueOf(usuarioId)))
                                        .authorities(new SimpleGrantedAuthority("ROLE_ADVOGADO")))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isPreconditionRequired())
                .andExpect(content().string(containsString("EQUIPE_REQUIRED")))
                .andExpect(content().string(containsString("X-Equipe-ID")));
    }
}
