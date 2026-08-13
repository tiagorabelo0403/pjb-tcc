package com.tcc.pjb.backend.configs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.advogado.surface.AdvogadoSurfaceFacadeService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Detector de regressão para um bug CONHECIDO e CONFIRMADO: {@code spring.jpa.open-in-view=false}
 * (ativo em todos os profiles, incluindo integration-test) quebra a ativação dos Hibernate
 * {@code @Filter} dentro de {@link EquipeSwitchInterceptor#preHandle}, que roda antes de
 * qualquer {@code @Transactional} abrir. Ciclo real via MockMvc: DispatcherServlet ->
 * HandlerInterceptor.preHandle() -> controller -> serviço transacional.
 *
 * <p>Os dois testes abaixo documentam o comportamento ATUAL (o bug), não o comportamento
 * desejado. A promessa de detectar regressão vale só para {@code probeDireto...}: se algum dia
 * {@code EquipeSwitchInterceptor} for corrigido para ativar o filtro de verdade antes da
 * transação de negócio, aquele teste vai FALHAR (assert de {@code true} vira {@code false} de
 * verdade) — o que é o sinal correto de que a correção aconteceu e que esta classe precisa ser
 * revisada/invertida de volta.
 *
 * <p>{@code enableFilterNaoRegistraFalha...} NÃO tem essa propriedade — não detecta regressão
 * sozinho. Hoje o interceptor não emite nenhum log DEBUG neste fluxo (os únicos
 * {@code log.debug(...)} do interceptor ficam dentro de blocos {@code catch}, nunca alcançados
 * porque {@code enableFilter} não lança exceção nesse cenário — daí o bug ser silencioso). Um
 * interceptor corrigido também não emitiria log de falha, então este teste continuaria verde
 * mesmo sem o bug. Ele existe como evidência CORROBORANTE de um aspecto específico do bug (a
 * ausência de log/exceção), não como detector independente — a prova de regressão real é
 * {@code probeDireto...}.
 *
 * <p>Duas evidências independentes, ambas confirmando o mesmo bug:
 * <ul>
 *   <li>{@code probeDireto...}: prova DIRETA — um endpoint real sob /api/v1/**
 *   com um método @Transactional próprio consulta, via {@code Session.getEnabledFilter},
 *   se filtroEquipe/filtroEquipeProcesso estão realmente ativos no momento em que uma
 *   query de negócio roda. Não depende de captura de log. Resultado confirmado: ambos
 *   {@code false} — o preHandle não conseguiu ativar os filtros.</li>
 *   <li>{@code enableFilterNaoRegistraFalha...}: evidência de log — captura DEBUG do próprio
 *   interceptor. Investigação confirmou que NENHUMA exceção é lançada por
 *   {@code enableFilter} durante o preHandle (ao contrário do que o nome anterior deste teste
 *   sugeria) — por isso também NENHUM log de falha aparece. O filtro simplesmente não é
 *   aplicado à sessão que a transação de negócio (aberta depois) vai usar, sem erro, sem
 *   log, silenciosamente.</li>
 * </ul>
 */
@AutoConfigureMockMvc
@Import(EquipeSwitchInterceptorHibernateFilterIT.FilterProbeConfig.class)
class EquipeSwitchInterceptorHibernateFilterIT extends PjbFlowItBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdvogadoSurfaceFacadeService facadeService;

    @MockitoBean
    private CapabilityRateLimiter capabilityRateLimiter;

    private Logger interceptorLogger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void prepararCapturaDeLogsEUsuario() {
        interceptorLogger = (Logger) LoggerFactory.getLogger(EquipeSwitchInterceptor.class);
        originalLevel = interceptorLogger.getLevel();
        interceptorLogger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        interceptorLogger.addAppender(appender);

        Usuario advogado = new Usuario();
        advogado.setNome("Advogado Filtro Teste");
        advogado.setEmail("adv-filtro@test.local");
        advogado.setSenha("x");
        advogado.setCpf("11122233344");
        advogado.setTipoUsuario(TipoUsuario.ADVOGADO);
        advogado.setPerfil(TipoUsuario.ADVOGADO.name());
        advogado.setAtivo(true);
        usuarioRepository.save(advogado);
    }

    @AfterEach
    void restaurarLogger() {
        interceptorLogger.detachAppender(appender);
        interceptorLogger.setLevel(originalLevel);
    }

    @Test
    @WithMockUser(username = "adv-filtro@test.local", authorities = "ROLE_ADVOGADO")
    void probeDiretoConfirmaQueFiltroContinuaInativoNaTransacaoDeNegocioBugConhecido() throws Exception {
        String body = mockMvc.perform(get("/api/v1/_debug/filtro-probe"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Boolean> resultado = objectMapper.readValue(body, Map.class);

        System.out.println("PROBE DIRETO filtro-probe => " + resultado);

        assertThat(resultado)
                .as("bug conhecido: open-in-view=false faz o preHandle do EquipeSwitchInterceptor "
                        + "rodar sem transacao aberta, entao os Hibernate Filters continuam "
                        + "inativos quando a transacao de negocio real (aberta depois) executa a "
                        + "query — se este assert comecar a falhar (true), o interceptor foi "
                        + "corrigido e esta classe precisa ser revisada")
                .containsEntry("filtroEquipe", false)
                .containsEntry("filtroEquipeProcesso", false);
    }

    @Test
    @WithMockUser(username = "adv-filtro@test.local", authorities = "ROLE_ADVOGADO")
    void enableFilterNaoRegistraFalhaPorqueNenhumaExcecaoEhLancadaBugConhecido() throws Exception {
        when(facadeService.analiticoPorCliente(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/advogado/cockpit/clientes/analitico").param("clienteCpfCnpj", "12345678900"))
                .andExpect(status().isOk());

        List<String> todasAsMensagens = appender.list.stream()
                .map(e -> "[" + e.getLevel() + "] " + e.getFormattedMessage())
                .toList();

        System.out.println("LOGS CAPTURADOS do EquipeSwitchInterceptor: " + todasAsMensagens);

        assertThat(todasAsMensagens)
                .as("evidencia CORROBORANTE (nao detector de regressao independente — ver Javadoc "
                        + "da classe): bug conhecido, enableFilter nao lanca excecao ao ser chamado "
                        + "sem transacao aberta, entao nenhum log de falha e emitido — o filtro "
                        + "apenas nao pega, silenciosamente, sem nenhum rastro de erro nos logs do "
                        + "interceptor. Um interceptor corrigido tambem passaria aqui — a prova de "
                        + "regressao real e o probeDiretoConfirmaQue...BugConhecido")
                .noneMatch(m -> m.contains("falha ao habilitar filtro de equipe")
                        || m.contains("falha ao habilitar filtro de processo"));
    }

    @TestConfiguration
    static class FilterProbeConfig {
        @Bean
        FilterProbeController filterProbeController(EntityManager entityManager) {
            return new FilterProbeController(entityManager);
        }
    }

    @RestController
    @RequestMapping("/api/v1/_debug")
    static class FilterProbeController {

        private final EntityManager entityManager;

        FilterProbeController(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @GetMapping("/filtro-probe")
        @Transactional(readOnly = true)
        public Map<String, Boolean> probe() {
            Session session = entityManager.unwrap(Session.class);
            boolean filtroEquipeAtivo = session.getEnabledFilter(EquipeSwitchInterceptor.HIBERNATE_FILTER_EQUIPE) != null;
            boolean filtroProcessoAtivo = session.getEnabledFilter(EquipeSwitchInterceptor.HIBERNATE_FILTER_PROCESSO) != null;
            return Map.of("filtroEquipe", filtroEquipeAtivo, "filtroEquipeProcesso", filtroProcessoAtivo);
        }
    }
}
