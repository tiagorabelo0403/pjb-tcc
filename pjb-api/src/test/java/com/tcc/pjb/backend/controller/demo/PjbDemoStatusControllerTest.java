package com.tcc.pjb.backend.controller.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.configs.EquipeSwitchInterceptor;
import com.tcc.pjb.backend.configs.SecurityConfig;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import com.tcc.pjb.backend.service.demo.DemoAcervoContagemService;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O serviço real entra no teste e só o {@link JdbcTemplate} é mockado, de propósito: a garantia que
 * importa aqui é de ponta a ponta — uma falha de SQL real, com nome de tabela na mensagem, entra pelo
 * acesso a dado e precisa sair da resposta HTTP sem esse detalhe. Mockar o serviço provaria apenas que
 * o controller copia o que recebe.
 */
@WebMvcTest(
        controllers = PjbDemoStatusController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, EquipeSwitchInterceptor.class}))
@Import({WebMvcTestSecurityConfig.class, DemoAcervoContagemService.class})
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class PjbDemoStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    private void acervoCom(long usuarios, long processos, long documentos) {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM tb_usuario", Long.class)).thenReturn(usuarios);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM tb_processo", Long.class)).thenReturn(processos);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM tb_documento_processual", Long.class))
                .thenReturn(documentos);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void painelDevolveAsContagensDoAcervo() throws Exception {
        acervoCom(7L, 13L, 29L);

        mockMvc.perform(get("/demo/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.stats.usuarios").value(7))
                .andExpect(jsonPath("$.stats.processos").value(13))
                .andExpect(jsonPath("$.stats.documentos").value(29));
    }

    private void bancoRecusandoLeitura() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new BadSqlGrammarException("StatementCallback", "SELECT COUNT(1) FROM tb_usuario",
                        new SQLException("ERROR: relation \"tb_usuario\" does not exist")));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void acervoIlegivelNaoEAnunciadoComoUP() throws Exception {
        // O campo era constante "UP": com o banco fora do ar o painel continuava anunciando UP e
        // escondia a indisponibilidade dentro de stats, onde nenhum monitor olha.
        bancoRecusandoLeitura();

        mockMvc.perform(get("/demo/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void erroDeBancoNaoLevaNomeDeTabelaParaAResposta() throws Exception {
        bancoRecusandoLeitura();

        String corpo = mockMvc.perform(get("/demo/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.erro").value("Dados não disponíveis"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(corpo)
                .as("mensagem de erro de SQL carrega nome de tabela, de coluna e o proprio comando; "
                        + "nada disso pode chegar ao cliente, nem em endpoint administrativo")
                .doesNotContain("tb_usuario")
                .doesNotContain("SELECT")
                .doesNotContain("relation")
                .doesNotContain("SQLException")
                .doesNotContain("BadSqlGrammarException");
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void perfilSemPapelAdministrativoNaoLeOAcervo() throws Exception {
        // O acervo esta disponivel de proposito: o 403 precisa vir da autorizacao, e nao de uma leitura
        // que falhou. E a leitura nao pode nem chegar a acontecer — negar depois de consultar ja seria
        // acesso a dado por quem nao podia.
        acervoCom(7L, 13L, 29L);

        mockMvc.perform(get("/demo/status")).andExpect(status().isForbidden());

        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Long.class));
    }
}
