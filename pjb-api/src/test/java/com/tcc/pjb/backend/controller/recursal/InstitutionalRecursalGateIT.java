package com.tcc.pjb.backend.controller.recursal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.model.dto.profile.operational.InstitutionalRecursoRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoPerfilRouter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova ponta a ponta, pela cadeia real de seguranca (JWT bearer via jwt()) contra Postgres real,
 * que o {@code InstitutionalCriticalActionHttpGuardFilter} passou a rodar DEPOIS da autenticacao:
 * o gate documental institucional resolve o usuario autenticado e libera a superficie recursal
 * unificada, em vez de estourar 500 por SecurityContext vazio (bug de ordem de filtro).
 */
@AutoConfigureMockMvc
class InstitutionalRecursalGateIT extends PjbFlowItBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    RecursalPeticionamentoPerfilRouter router;

    private String body() throws Exception {
        return objectMapper.writeValueAsString(new InstitutionalRecursoRequest(
                "APELACAO", "razoes recursais", "CPC art. 1009", true, false, "obs"));
    }

    private void stubRouter() {
        when(router.resolverPerfilAtivo()).thenReturn(RecursalPeticionamentoPerfilRouter.Perfil.MINISTERIO_PUBLICO);
        when(router.interporRecurso(any(RecursalPeticionamentoPerfilRouter.Perfil.class),
                anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(Map.of("status", "RECURSO_INTERPOSTO"));
    }

    @Test
    void recursoInstitucional_comUsuarioMpAutenticado_passaPeloGateDocumentalEChegaAoRouter() throws Exception {
        Usuario u = new Usuario();
        u.setNome("Promotor Recursal");
        u.setEmail("recursal.mp@pjb.local");
        u.setCpf("12345678909");
        u.setAtivo(true);
        u.setTipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        u.setPerfil(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO.name());
        u = usuarioRepository.save(u);
        long usuarioId = u.getId();
        stubRouter();

        mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(usuarioId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBRO_MINISTERIO_PUBLICO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-PJB-Institutional-Gate-Operation", "RECURSAL_UNIFICADO"))
                .andExpect(header().string("X-PJB-Institutional-Gate-Allowed", "true"));
    }

    @Test
    void recursoInstitucional_comUsuarioNaoMaterializadoNoBanco_naoEstoura500() throws Exception {
        stubRouter();

        mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .with(jwt().jwt(j -> j.claim("uid", "99999999"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBRO_MINISTERIO_PUBLICO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-PJB-Institutional-Gate-Operation", "RECURSAL_UNIFICADO"));
    }
}
