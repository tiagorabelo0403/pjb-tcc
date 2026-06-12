package com.tcc.pjb.backend.controller.auth;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.security.DbUserDetailsService;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.configs.security.perimeter.SecurityPerimeterProperties;
import com.tcc.pjb.backend.core.icp.IcpBrasilCertProfile;
import com.tcc.pjb.backend.core.icp.IcpBrasilChainValidator;
import com.tcc.pjb.backend.core.security.identity.CertificadoAuthAuditService;
import com.tcc.pjb.backend.core.security.identity.CertificadoAuthPolicy;
import com.tcc.pjb.backend.core.security.identity.CertificadoIdentidadeResolver;
import com.tcc.pjb.backend.core.security.identity.CertificadoX509Support;
import com.tcc.pjb.backend.core.security.identity.ContextoInstitucionalResolver;
import com.tcc.pjb.backend.core.security.identity.DesafioCertificadoNonceStore;
import com.tcc.pjb.backend.core.security.identity.VerificadorAssinaturaCertificado;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.core.security.webauthn.WebAuthnProperties;
import com.tcc.pjb.backend.core.security.webauthn.web.PasskeyAuthenticationFilter;
import com.tcc.pjb.backend.model.dto.security.CertificadoAuthDtos;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import com.tcc.pjb.backend.service.auth.surface.CertificadoAuthFacadeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

@DataJpaTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class CertificadoAuthSessaoE2ETest {

    private static final String NONCE = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CertificadoX509Support x509Support = new CertificadoX509Support();
    private final IcpBrasilChainValidator chainValidator = mock(IcpBrasilChainValidator.class);
    private final DesafioCertificadoNonceStore nonceStore = mock(DesafioCertificadoNonceStore.class);
    private final CertificadoAuthAuditService auditService = mock(CertificadoAuthAuditService.class);
    private final Map<String, String> nonces = new HashMap<>();
    private final Map<String, com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationResult> validacoes = new HashMap<>();

    private final UsuarioRepository usuarioRepository;

    private final InstituicaoRepository instituicaoRepository;

    private final UnidadeInstituicaoRepository unidadeRepository;

    private final LotacaoInstituicaoRepository lotacaoRepository;

    private final PasskeySessionRepository passkeySessionRepository;

    private MockMvc mockMvc;

    CertificadoAuthSessaoE2ETest(
            UsuarioRepository usuarioRepository,
            InstituicaoRepository instituicaoRepository,
            UnidadeInstituicaoRepository unidadeRepository,
            LotacaoInstituicaoRepository lotacaoRepository,
            PasskeySessionRepository passkeySessionRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.instituicaoRepository = instituicaoRepository;
        this.unidadeRepository = unidadeRepository;
        this.lotacaoRepository = lotacaoRepository;
        this.passkeySessionRepository = passkeySessionRepository;
    }

    @BeforeEach
    void setup() {
        nonces.clear();
        validacoes.clear();
        when(nonceStore.emitir(any(String.class))).thenAnswer(invocation -> {
            nonces.put(NONCE, invocation.getArgument(0));
            return NONCE;
        });
        when(nonceStore.consumir(any(String.class), any(String.class))).thenAnswer(invocation -> {
            String nonce = invocation.getArgument(0);
            String referencia = invocation.getArgument(1);
            String registrada = nonces.get(nonce);
            if (registrada != null && registrada.equals(referencia)) {
                nonces.remove(nonce);
                return true;
            }
            return false;
        });
        when(chainValidator.validate(any(X509Certificate.class))).thenAnswer(invocation -> {
            X509Certificate certificado = invocation.getArgument(0);
            return validacoes.getOrDefault(
                    x509Support.fingerprintSha256(certificado),
                    com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationResult.fail("certificado invalido"));
        });
        PasskeySessionService passkeySessionService = new PasskeySessionService(
                passkeySessionRepository,
                new WebAuthnProperties());
        CertificadoAuthFacadeService facadeService = new CertificadoAuthFacadeService(
                chainValidator,
                nonceStore,
                new VerificadorAssinaturaCertificado(),
                new CertificadoIdentidadeResolver(usuarioRepository),
                new ContextoInstitucionalResolver(lotacaoRepository),
                new CertificadoAuthPolicy(Duration.ofSeconds(120), true),
                x509Support,
                auditService,
                passkeySessionService,
                new ClientIpResolver(new SecurityPerimeterProperties()),
                new MockEnvironment());
        PasskeyAuthenticationFilter passkeyFilter = new PasskeyAuthenticationFilter(
                passkeySessionRepository,
                new DbUserDetailsService(usuarioRepository));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new CertificadoAuthController(facadeService),
                        new ProtectedProbeController())
                .addFilters(passkeyFilter, new ProtectedGateFilter())
                .build();
    }

    @Test
    void certificadoEmiteTokenAceitoPeloFiltroPasskeyEMetodoReportadoComoPasskeyDividaOnda151() throws Exception {
        CertMaterial titular = material("12345678901", "211");
        Usuario usuario = salvarUsuario("cert-e2e-1@pjb.test", titular.profile().cpfTitular(), TipoUsuario.DELEGADO_POLICIA);
        salvarLotacao(usuario, "Delegacia Centro", "DELEGADO");
        registrarValidacao(titular);

        emitirDesafio(titular).andExpect(status().isOk());
        MvcResult resposta = responder(titular, assinatura(titular, NONCE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTENTICADO"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(resposta.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/v1/auth/certificado/session-probe"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/certificado/session-probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.method").value("PASSKEY"));
    }

    @Test
    void lotacaoMultiplaNaoEmiteToken() throws Exception {
        CertMaterial titular = material("12345678902", "212");
        Usuario usuario = salvarUsuario("cert-e2e-2@pjb.test", titular.profile().cpfTitular(), TipoUsuario.DELEGADO_POLICIA);
        salvarLotacao(usuario, "Delegacia Centro", "DELEGADO");
        salvarLotacao(usuario, "Departamento Regional", "COORDENADOR");
        registrarValidacao(titular);

        emitirDesafio(titular).andExpect(status().isOk());
        responder(titular, assinatura(titular, NONCE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE_SELECAO"))
                .andExpect(jsonPath("$", not(hasKey("token"))));
    }

    private org.springframework.test.web.servlet.ResultActions emitirDesafio(CertMaterial material)
            throws Exception {
        CertificadoAuthDtos.DesafioRequest request = new CertificadoAuthDtos.DesafioRequest();
        request.setCertificado(material.pem());
        return mockMvc.perform(post("/api/v1/auth/certificado/desafio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions responder(CertMaterial material, String assinatura)
            throws Exception {
        CertificadoAuthDtos.RespostaRequest request = new CertificadoAuthDtos.RespostaRequest();
        request.setCertificado(material.pem());
        request.setNonce(NONCE);
        request.setAssinatura(assinatura);
        request.setAlgoritmoAssinatura("SHA256withRSA");
        return mockMvc.perform(post("/api/v1/auth/certificado/resposta")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private Usuario salvarUsuario(String email, String cpf, TipoUsuario tipoUsuario) {
        Usuario usuario = Usuario.builder()
                .nome("Usuario Certificado")
                .email(email)
                .senha("{noop}senhaValida1")
                .cpf(cpf)
                .tipoUsuario(tipoUsuario)
                .perfil(tipoUsuario.name())
                .ativo(true)
                .build();
        return usuarioRepository.save(usuario);
    }

    private void salvarLotacao(Usuario usuario, String unidadeNome, String papel) {
        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(TipoInstituicao.DELEGACIA_POLICIA);
        instituicao.setNome(unidadeNome + " Instituicao");
        instituicao = instituicaoRepository.save(instituicao);

        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setInstituicao(instituicao);
        unidade.setNome(unidadeNome);
        unidade.setTipo(TipoUnidadeInstitucional.DELEGACIA);
        unidade = unidadeRepository.save(unidade);

        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUsuario(usuario);
        lotacao.setUnidade(unidade);
        lotacao.setInicio(LocalDate.of(2026, 1, 1));
        lotacao.setPapelNaUnidade(papel);
        lotacaoRepository.save(lotacao);
    }

    private void registrarValidacao(CertMaterial material) {
        validacoes.put(
                x509Support.fingerprintSha256(material.certificado()),
                com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationResult.ok(material.profile()));
    }

    private static CertMaterial material(String cpf, String serial) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        X509Certificate certificado = certificado(keyPair, cpf, serial);
        IcpBrasilCertProfile profile = new IcpBrasilCertProfile(
                "CN=Titular " + cpf,
                "CN=AC Teste",
                serial,
                cpf,
                null,
                "Titular " + cpf,
                "A3",
                "AC TESTE",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600));
        return new CertMaterial(keyPair, certificado, pem(certificado), profile);
    }

    private static X509Certificate certificado(KeyPair keyPair, String cpf, String serial) throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        Instant inicio = Instant.now().minusSeconds(60);
        Instant fim = Instant.now().plusSeconds(3600);
        X500Name nome = new X500Name("CN=Titular " + cpf + ", OID.2.16.76.1.3.1=" + cpf);
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                nome,
                new BigInteger(serial),
                Date.from(inicio),
                Date.from(fim),
                nome,
                keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(builder.build(signer));
    }

    private static String pem(X509Certificate certificado) throws Exception {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(certificado.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n" + base64 + "\n-----END CERTIFICATE-----";
    }

    private static String assinatura(CertMaterial material, String nonce) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        PrivateKey privateKey = material.keyPair().getPrivate();
        signature.initSign(privateKey);
        signature.update(nonce.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    @RestController
    static class ProtectedProbeController {

        @GetMapping("/api/v1/auth/certificado/session-probe")
        Map<String, Object> probe(HttpServletRequest request) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return Map.of(
                    "authenticated", authentication != null && authentication.isAuthenticated(),
                    "method", String.valueOf(request.getAttribute("PJB_STRONG_AUTH_METHOD")));
        }
    }

    static class ProtectedGateFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            if (request.getRequestURI().endsWith("/session-probe")
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            filterChain.doFilter(request, response);
        }
    }

    private record CertMaterial(
            KeyPair keyPair,
            X509Certificate certificado,
            String pem,
            IcpBrasilCertProfile profile
    ) {
    }
}
