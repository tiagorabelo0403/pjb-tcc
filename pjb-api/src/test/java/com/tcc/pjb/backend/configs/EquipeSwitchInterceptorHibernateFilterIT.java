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
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.advogado.surface.AdvogadoSurfaceFacadeService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
 * Prova de correção (e detector de regressão) para o bug CONFIRMADO de {@code
 * spring.jpa.open-in-view=false}: um {@code entityManager.unwrap(Session.class)} chamado no
 * {@link EquipeSwitchInterceptor#preHandle}, antes de qualquer {@code @Transactional} abrir,
 * nunca afetava a Session que a transação de negócio (aberta depois) de fato usava. A correção
 * move a ativação dos Hibernate {@code @Filter} para {@link EquipeFiltroRepositoryAspect}, que
 * intercepta {@code ProcessoRepository}/{@code ClienteRepository} — join point que só existe
 * depois que a transação de negócio já abriu a Session real.
 *
 * <p>{@code probeDireto...}: prova DIRETA — um endpoint real sob /api/v1/** com um método
 * @Transactional próprio consulta {@code ProcessoRepository}/{@code ClienteRepository} (para
 * disparar o aspecto) e então lê, via {@code Session.getEnabledFilter}, se
 * filtroEquipe/filtroEquipeProcesso estão realmente ativos naquela mesma Session. Chama o
 * endpoint duas vezes seguidas para provar também que o {@code ThreadLocal} em
 * {@link EquipeFiltroContexto} é limpo corretamente entre requisições (sem vazamento de estado
 * de uma requisição para a próxima na mesma thread). Se esta correção regredir (voltar a
 * ativar o filtro só no preHandle, ou parar de limpar o ThreadLocal), este teste falha.
 *
 * <p>{@code enableFilterNaoRegistraFalha...}: evidência corroborante — nenhum log de falha é
 * emitido pelo {@link EquipeSwitchInterceptor} ao resolver o contexto de equipe (o interceptor
 * não tenta mais ativar filtro nenhum diretamente, então esse tipo de falha não pode mais
 * acontecer ali; a ativação real, e sua própria captura de exceção, agora vive no aspecto).
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

    @Autowired
    private EntityManager entityManager;

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
    void filterDefinitionsFiltroEquipeEFiltroEquipeProcessoEstaoRegistradasNaSessionFactory() {
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);

        assertThat(sessionFactory.getDefinedFilterNames())
                .as("guarda contra regressao do bug 'Cliente.filtroEquipe sem @FilterDef "
                        + "correspondente' (UnknownFilterException em runtime) — se este assert "
                        + "comecar a falhar, um @FilterDef foi removido ou o nome divergiu do "
                        + "@Filter que o referencia; ver docs/quality/DEBT_LOG.md, "
                        + "D-equipe-switch-interceptor-noop-quatro-bugs-empilhados")
                .contains(EquipeSwitchInterceptor.HIBERNATE_FILTER_EQUIPE, EquipeSwitchInterceptor.HIBERNATE_FILTER_PROCESSO);
    }

    @Test
    @WithMockUser(username = "adv-filtro@test.local", authorities = "ROLE_ADVOGADO")
    void probeDiretoConfirmaQueFiltroEstaAtivoNaTransacaoDeNegocioAoChamarORepositorio() throws Exception {
        Map<String, Boolean> primeiraChamada = chamarProbe();
        System.out.println("PROBE DIRETO filtro-probe (1a chamada) => " + primeiraChamada);

        assertThat(primeiraChamada)
                .as("correcao: EquipeFiltroRepositoryAspect ativa o filtro no momento em que "
                        + "ProcessoRepository/ClienteRepository sao chamados dentro da transacao de "
                        + "negocio real — se este assert comecar a falhar (false), a correcao regrediu")
                .containsEntry("filtroEquipe", true)
                .containsEntry("filtroEquipeProcesso", true);

        Map<String, Boolean> segundaChamada = chamarProbe();
        System.out.println("PROBE DIRETO filtro-probe (2a chamada) => " + segundaChamada);

        assertThat(segundaChamada)
                .as("EquipeFiltroContexto (ThreadLocal) precisa continuar funcionando numa segunda "
                        + "requisicao na mesma thread — prova de que afterCompletion limpa o estado "
                        + "e o proximo preHandle o repovoa, sem vazamento nem perda entre requisicoes")
                .containsEntry("filtroEquipe", true)
                .containsEntry("filtroEquipeProcesso", true);
    }

    private Map<String, Boolean> chamarProbe() throws Exception {
        String body = mockMvc.perform(get("/api/v1/_debug/filtro-probe"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Boolean> resultado = objectMapper.readValue(body, Map.class);
        return resultado;
    }

    @Test
    @WithMockUser(username = "adv-filtro@test.local", authorities = "ROLE_ADVOGADO")
    void enableFilterNaoRegistraFalhaAoResolverContextoDeEquipe() throws Exception {
        when(facadeService.analiticoPorCliente(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/advogado/cockpit/clientes/analitico").param("clienteCpfCnpj", "12345678900"))
                .andExpect(status().isOk());

        List<String> todasAsMensagens = appender.list.stream()
                .map(e -> "[" + e.getLevel() + "] " + e.getFormattedMessage())
                .toList();

        System.out.println("LOGS CAPTURADOS do EquipeSwitchInterceptor: " + todasAsMensagens);

        assertThat(todasAsMensagens)
                .as("evidencia corroborante: o interceptor so resolve contexto (quem/qual equipe) e "
                        + "nao ativa filtro nenhum diretamente mais, entao nao pode mais emitir esse "
                        + "tipo de falha — a ativacao real, com sua propria captura de excecao, agora "
                        + "vive em EquipeFiltroRepositoryAspect")
                .noneMatch(m -> m.contains("falha ao habilitar filtro de equipe")
                        || m.contains("falha ao habilitar filtro de processo"));
    }

    @TestConfiguration
    static class FilterProbeConfig {
        @Bean
        FilterProbeController filterProbeController(EntityManager entityManager,
                                                     ProcessoRepository processoRepository,
                                                     ClienteRepository clienteRepository) {
            return new FilterProbeController(entityManager, processoRepository, clienteRepository);
        }
    }

    @RestController
    @RequestMapping("/api/v1/_debug")
    static class FilterProbeController {

        private final EntityManager entityManager;
        private final ProcessoRepository processoRepository;
        private final ClienteRepository clienteRepository;

        FilterProbeController(EntityManager entityManager,
                              ProcessoRepository processoRepository,
                              ClienteRepository clienteRepository) {
            this.entityManager = entityManager;
            this.processoRepository = processoRepository;
            this.clienteRepository = clienteRepository;
        }

        @GetMapping("/filtro-probe")
        @Transactional(readOnly = true)
        public Map<String, Boolean> probe() {
            processoRepository.count();
            clienteRepository.count();
            Session session = entityManager.unwrap(Session.class);
            boolean filtroEquipeAtivo = session.getEnabledFilter(EquipeSwitchInterceptor.HIBERNATE_FILTER_EQUIPE) != null;
            boolean filtroProcessoAtivo = session.getEnabledFilter(EquipeSwitchInterceptor.HIBERNATE_FILTER_PROCESSO) != null;
            return Map.of("filtroEquipe", filtroEquipeAtivo, "filtroEquipeProcesso", filtroProcessoAtivo);
        }
    }
}
