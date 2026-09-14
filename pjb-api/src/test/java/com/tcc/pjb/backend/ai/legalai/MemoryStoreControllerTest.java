package com.tcc.pjb.backend.ai.legalai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.legalai.application.MemoryStoreApplicationService;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicApiUnavailableException;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicMemoryStoreClient;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreRepository;
import com.tcc.pjb.backend.configs.EquipeSwitchInterceptor;
import com.tcc.pjb.backend.configs.SecurityConfig;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A garantia central aqui é de proteção de dados, não de camada: memória classificada como
 * {@code SIGILOSO} ou {@code CRITICO} <b>não pode ser transmitida</b> para a API externa da Anthropic,
 * nem na criação nem no arquivamento. {@code MemoryAccessPolicy.podeEnviarParaAnthropicApi} libera
 * apenas {@code PUBLIC} e {@code INSTITUCIONAL}, e essa regra não tinha teste.
 *
 * <p>Os dez casos foram escritos contra o controller antigo, que fazia tudo, e seguem valendo sem uma
 * alteração depois de a lógica migrar para {@link MemoryStoreApplicationService} — por isso o serviço
 * real é importado e só os colaboradores dele são mockados.
 */
@WebMvcTest(
        controllers = MemoryStoreController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, EquipeSwitchInterceptor.class}))
@Import({WebMvcTestSecurityConfig.class, MemoryStoreControllerTest.RelogioFixo.class,
        MemoryStoreApplicationService.class})
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class MemoryStoreControllerTest {

    private static final Instant AGORA = Instant.parse("2026-09-13T12:00:00Z");
    private static final String STORES = "/api/v1/legal-ai/memory-stores";

    @TestConfiguration
    static class RelogioFixo {
        @Bean
        Clock clock() {
            return Clock.fixed(AGORA, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemoryStoreRepository memoryStoreRepository;

    @MockitoBean
    private AnthropicMemoryStoreClient anthropicMemoryStoreClient;

    private String corpoDeCriacao(String sigiloNivel) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "nome", "Memoria do modulo",
                "descricao", "descricao",
                "moduloOrigem", "laiane",
                "sigiloNivel", sigiloNivel));
    }

    private MemoryStore storeSalvo(MemorySigiloNivel nivel) {
        return MemoryStore.criar(MemoryStoreId.gerar(), "Memoria do modulo", "descricao", "laiane", nivel, AGORA);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void memoriaPublicaPodeSerEnviadaParaAApiExterna() throws Exception {
        when(anthropicMemoryStoreClient.criarStore(anyString(), any(), any()))
                .thenReturn(new AnthropicMemoryStoreClient.AnthropicStoreRef("anth-1", "Memoria do modulo", "ativo"));
        when(memoryStoreRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post(STORES).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao("PUBLIC")))
                .andExpect(status().isCreated());

        verify(anthropicMemoryStoreClient).criarStore(eq("Memoria do modulo"), eq("descricao"), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void memoriaSigilosaNaoVaiParaAApiExterna() throws Exception {
        when(memoryStoreRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post(STORES).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao("SIGILOSO")))
                .andExpect(status().isCreated());

        verify(anthropicMemoryStoreClient, never()).criarStore(anyString(), any(), any());
        verify(memoryStoreRepository).salvar(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void memoriaCriticaNaoVaiParaAApiExterna() throws Exception {
        when(memoryStoreRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post(STORES).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao("CRITICO")))
                .andExpect(status().isCreated());

        verify(anthropicMemoryStoreClient, never()).criarStore(anyString(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void nivelDeSigiloDesconhecidoRecebeBadRequestESemPersistir() throws Exception {
        mockMvc.perform(post(STORES).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao("NIVEL_QUE_NAO_EXISTE")))
                .andExpect(status().isBadRequest());

        verify(memoryStoreRepository, never()).salvar(any());
        verify(anthropicMemoryStoreClient, never()).criarStore(anyString(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void apiExternaIndisponivelNaoPersistePelaMetade() throws Exception {
        when(anthropicMemoryStoreClient.criarStore(anyString(), any(), any()))
                .thenThrow(new AnthropicApiUnavailableException("fora do ar", new RuntimeException("timeout")));

        mockMvc.perform(post(STORES).contentType(MediaType.APPLICATION_JSON)
                        .content(corpoDeCriacao("PUBLIC")))
                .andExpect(status().isServiceUnavailable());

        verify(memoryStoreRepository, never()).salvar(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void storeInexistenteRecebeNotFound() throws Exception {
        when(memoryStoreRepository.buscarPorId(any())).thenReturn(Optional.empty());

        mockMvc.perform(get(STORES + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void atualizacaoParcialPreservaOCampoNaoInformado() throws Exception {
        MemoryStore existente = storeSalvo(MemorySigiloNivel.INSTITUCIONAL);
        when(memoryStoreRepository.buscarPorId(any())).thenReturn(Optional.of(existente));
        when(memoryStoreRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch(STORES + "/" + existente.id().value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Nome novo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome novo"))
                .andExpect(jsonPath("$.descricao").value("descricao"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void arquivarMemoriaSigilosaNaoTocaNaApiExterna() throws Exception {
        MemoryStore sigiloso = storeSalvo(MemorySigiloNivel.SIGILOSO).comAnthropicStoreId("anth-legado");
        when(memoryStoreRepository.buscarPorId(any())).thenReturn(Optional.of(sigiloso));
        when(memoryStoreRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(delete(STORES + "/" + sigiloso.id().value()))
                .andExpect(status().isNoContent());

        verify(anthropicMemoryStoreClient, never()).arquivarStore(anyString());
        verify(memoryStoreRepository).salvar(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void arquivarMemoriaPublicaComReferenciaExternaArquivaLaTambem() throws Exception {
        MemoryStore publico = storeSalvo(MemorySigiloNivel.PUBLIC).comAnthropicStoreId("anth-1");
        when(memoryStoreRepository.buscarPorId(any())).thenReturn(Optional.of(publico));
        when(memoryStoreRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(delete(STORES + "/" + publico.id().value()))
                .andExpect(status().isNoContent());

        verify(anthropicMemoryStoreClient).arquivarStore("anth-1");
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void listagemDevolveApenasOsAtivos() throws Exception {
        when(memoryStoreRepository.listarAtivos())
                .thenReturn(List.of(storeSalvo(MemorySigiloNivel.PUBLIC)));

        mockMvc.perform(get(STORES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Memoria do modulo"))
                .andExpect(jsonPath("$[0].ativo").value(true));

        verify(memoryStoreRepository).listarAtivos();
    }
}
