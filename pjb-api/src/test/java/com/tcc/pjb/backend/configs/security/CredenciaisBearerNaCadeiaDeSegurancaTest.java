package com.tcc.pjb.backend.configs.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.BackendApplication;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.core.security.webauthn.TermosAceiteService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.OrigemAutenticacaoSessao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.MarketplaceAccessTokenRecordRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceAuditEventRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceClientAppRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import com.tcc.pjb.backend.model.repository.security.TermosAceiteRepository;
import com.tcc.pjb.backend.service.api.oauth.MarketplaceOAuth2Service;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=test",
                "spring.main.lazy-initialization=true"
        }
)
@AutoConfigureMockMvc
class CredenciaisBearerNaCadeiaDeSegurancaTest {

    private static final String ROTA_AUTENTICADA = "/api/v1/workspace/me";
    private static final String ROTA_DO_MARKETPLACE = "/api/marketplace/v1/processos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasskeySessionRepository passkeySessionRepository;

    @Autowired
    private PasskeySessionService passkeySessionService;

    @Autowired
    private TermosAceiteService termosAceiteService;

    @Autowired
    private TermosAceiteRepository termosAceiteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MarketplaceOAuth2Service marketplaceOAuth2Service;

    @Autowired
    private MarketplaceClientAppRepository marketplaceClientAppRepository;

    @Autowired
    private MarketplaceAccessTokenRecordRepository marketplaceAccessTokenRecordRepository;

    @Autowired
    private MarketplaceAuditEventRepository marketplaceAuditEventRepository;

    private final List<Long> usuariosCriados = new ArrayList<>();
    private final List<String> clientesDoMarketplaceCriados = new ArrayList<>();

    @AfterEach
    void limpar() {
        for (Long usuarioId : usuariosCriados) {
            passkeySessionRepository.findAll().stream()
                    .filter(sessao -> sessao.getUsuario() != null && usuarioId.equals(sessao.getUsuario().getId()))
                    .forEach(passkeySessionRepository::delete);
            termosAceiteRepository.findAll().stream()
                    .filter(aceite -> aceite.getUsuario() != null && usuarioId.equals(aceite.getUsuario().getId()))
                    .forEach(termosAceiteRepository::delete);
            usuarioRepository.deleteById(usuarioId);
        }
        usuariosCriados.clear();
        for (String clientId : clientesDoMarketplaceCriados) {
            marketplaceClientAppRepository.findByClientIdIgnoreCase(clientId).ifPresent(cliente -> {
                marketplaceAccessTokenRecordRepository.deleteAll(
                        marketplaceAccessTokenRecordRepository.findTop100ByClientApp_IdOrderByIssuedAtDesc(cliente.getId()));
                marketplaceAuditEventRepository.deleteAll(
                        marketplaceAuditEventRepository.findTop100ByClientApp_IdOrderByCreatedAtDesc(cliente.getId()));
                marketplaceClientAppRepository.delete(cliente);
            });
        }
        clientesDoMarketplaceCriados.clear();
    }

    @ParameterizedTest
    @EnumSource(OrigemAutenticacaoSessao.class)
    void sessaoEmitidaPeloLoginAutenticaRequisicaoNaCadeiaDeSeguranca(OrigemAutenticacaoSessao origem) throws Exception {
        Usuario usuario = usuarioComTermosAceitos();
        String token = passkeySessionService.issue(usuario, null, "127.0.0.1", origem).token();

        mockMvc.perform(get(ROTA_AUTENTICADA).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tokenOpacoDesconhecidoNaoAutentica() throws Exception {
        mockMvc.perform(get(ROTA_AUTENTICADA).header("Authorization", "Bearer " + "x".repeat(43)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }

    @Test
    void jwtSemEmissorConfiguradoNaoAutentica() throws Exception {
        mockMvc.perform(get(ROTA_AUTENTICADA).header("Authorization", "Bearer aaaa.bbbb.cccc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDoMarketplaceChegaAoControllerDoMarketplace() throws Exception {
        String clientId = "cadeia-" + UUID.randomUUID().toString().substring(0, 8);
        clientesDoMarketplaceCriados.add(clientId);
        MarketplaceOAuth2Service.ClientRegistrationResponse cliente = marketplaceOAuth2Service.registrarCliente(
                new MarketplaceOAuth2Service.ClientRegistrationRequest(clientId, "Cliente da cadeia", "Escritorio de teste",
                        "cliente.cadeia@test.local", List.of("processos:protocolar"), null, 300),
                "127.0.0.1");
        String token = marketplaceOAuth2Service.emitirToken(
                new MarketplaceOAuth2Service.TokenRequest(clientId, cliente.clientSecret(), "client_credentials", "processos:protocolar"),
                "127.0.0.1").accessToken();

        mockMvc.perform(post(ROTA_DO_MARKETPLACE)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private Usuario usuarioComTermosAceitos() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome("Sessao Opaca")
                .email("sessao.opaca." + UUID.randomUUID() + "@test.local")
                .senha(passwordEncoder.encode(UUID.randomUUID().toString()))
                .tipoUsuario(TipoUsuario.ADVOGADO)
                .perfil(TipoUsuario.ADVOGADO.name())
                .ativo(true)
                .build());
        usuariosCriados.add(usuario.getId());
        termosAceiteService.registrarAceite(usuario, termosAceiteService.versaoAtual(), "127.0.0.1");
        return usuario;
    }
}
