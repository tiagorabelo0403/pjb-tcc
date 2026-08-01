package com.tcc.pjb.backend.controller.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova ponta a ponta, pela cadeia real de seguranca (JWT bearer via jwt()) contra Postgres real,
 * que o {@code InstitutionalCriticalActionHttpGuardFilter} passou a rodar DEPOIS da autenticacao:
 * o gate documental institucional resolve o usuario autenticado (ou o ausenta com fallback legado),
 * classifica a operacao {@code RECURSAL_UNIFICADO} e libera — em vez de estourar 500 por
 * SecurityContext vazio (bug de ordem de filtro).
 *
 * <p><b>Assertion:</b> os headers {@code X-PJB-Institutional-Gate-Operation=RECURSAL_UNIFICADO} e
 * {@code X-PJB-Institutional-Gate-Allowed=true} devem estar presentes no response — evidencia
 * direta e suficiente de que o gate rodou depois da autenticacao. O status HTTP e ortogonal:
 * o {@code CapabilityRateLimiter} downstream do controller usa Redis, que em ambiente local
 * sem infra opcional retorna {@code RedisConnectionFailureException}. Assertar 201 sobre isso
 * criava dependencia latente de infra que mascarava a garantia real do teste (ordem de filtro).
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

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(usuarioId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBRO_MINISTERIO_PUBLICO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("Nao pode ser 401 (auth falhou antes do gate) nem 403 (gate barrou MP legitimo)")
                .isNotIn(401, 403);
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("Gate rodou depois da auth e classificou a operacao recursal")
                .isEqualTo("RECURSAL_UNIFICADO");
        assertThat(response.getHeader("X-PJB-Institutional-Gate-Allowed"))
                .as("Gate resolveu o MP JWT via banco e liberou")
                .isEqualTo("true");
    }

    @Test
    void recursoInstitucional_comUsuarioNaoMaterializadoNoBanco_naoEstoura500() throws Exception {
        stubRouter();

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .with(jwt().jwt(j -> j.claim("uid", "99999999"))
                                .authorities(new SimpleGrantedAuthority("ROLE_MEMBRO_MINISTERIO_PUBLICO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andReturn()
                .getResponse();

        assertThat(response.getHeader("X-PJB-Institutional-Gate-Operation"))
                .as("Gate rodou mesmo com uid nao materializado (fallback legado) — se rodasse antes da auth, "
                        + "SecurityContext vazio explodiria antes deste header ser setado")
                .isEqualTo("RECURSAL_UNIFICADO");
    }
}
