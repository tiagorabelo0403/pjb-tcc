package com.tcc.pjb.backend.controller.auth;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.icp.IcpBrasilCertProfile;
import com.tcc.pjb.backend.core.icp.IcpBrasilChainValidator;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilChainValidationDetails;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignaturePolicySnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationResult;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.security.identity.CertificadoAuthAuditService;
import com.tcc.pjb.backend.core.security.identity.CertificadoAuthPolicy;
import com.tcc.pjb.backend.core.security.identity.CertificadoIdentidadeResolver;
import com.tcc.pjb.backend.core.security.identity.CertificadoX509Support;
import com.tcc.pjb.backend.core.security.identity.ContextoInstitucional;
import com.tcc.pjb.backend.core.security.identity.ContextoInstitucionalResolver;
import com.tcc.pjb.backend.core.security.identity.ContextoResolvido;
import com.tcc.pjb.backend.core.security.identity.DesafioCertificadoNonceStore;
import com.tcc.pjb.backend.core.security.identity.IdentidadeNaoResolvida;
import com.tcc.pjb.backend.core.security.identity.IdentidadeResolvida;
import com.tcc.pjb.backend.core.security.identity.MotivoIdentidade;
import com.tcc.pjb.backend.core.security.identity.PendenteSelecao;
import com.tcc.pjb.backend.core.security.identity.VerificadorAssinaturaCertificado;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.model.dto.security.CertificadoAuthDtos;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.OrigemAutenticacaoSessao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.service.auth.surface.CertificadoAuthFacadeService;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CertificadoAuthControllerTest {

    private static final String NONCE = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 6, 7, 12, 0);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CertificadoX509Support x509Support = new CertificadoX509Support();
    private final IcpBrasilChainValidator chainValidator = mock(IcpBrasilChainValidator.class);
    private final DesafioCertificadoNonceStore nonceStore = mock(DesafioCertificadoNonceStore.class);
    private final CertificadoIdentidadeResolver identidadeResolver = mock(CertificadoIdentidadeResolver.class);
    private final ContextoInstitucionalResolver contextoResolver = mock(ContextoInstitucionalResolver.class);
    private final CertificadoAuthAuditService auditService = mock(CertificadoAuthAuditService.class);
    private final PasskeySessionService passkeySessionService = mock(PasskeySessionService.class);
    private final ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
    private final MockEnvironment environment = new MockEnvironment();
    private final CertificadoAuthPolicy policy = new CertificadoAuthPolicy(Duration.ofSeconds(120), true);
    private final Map<String, String> nonces = new HashMap<>();
    private final Map<String, IcpBrasilValidationResult> validacoes = new HashMap<>();

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
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
                    IcpBrasilValidationResult.fail("certificado invalido"));
        });
        when(chainValidator.policySnapshot())
                .thenReturn(new IcpBrasilSignaturePolicySnapshot(true, true, "LTA"));
        when(chainValidator.detailsFor(isNull(), isNull()))
                .thenReturn(new IcpBrasilChainValidationDetails(true, true, null, null));
        when(clientIpResolver.resolve(any()))
                .thenReturn("127.0.0.1");
        when(passkeySessionService.issue(any(Usuario.class), isNull(), any(String.class), any(OrigemAutenticacaoSessao.class)))
                .thenReturn(new PasskeySessionService.IssuedPasskeySession("cert-token-1", EXPIRES_AT, 41L, false));
        CertificadoAuthFacadeService facadeService = new CertificadoAuthFacadeService(
                chainValidator,
                nonceStore,
                new VerificadorAssinaturaCertificado(),
                identidadeResolver,
                contextoResolver,
                policy,
                x509Support,
                auditService,
                passkeySessionService,
                clientIpResolver,
                environment);
        mockMvc = MockMvcBuilders.standaloneSetup(new CertificadoAuthController(facadeService)).build();
    }

    @Test
    void desafioRespostaFelizAutenticaComContexto() throws Exception {
        CertMaterial titular = material("12345678901", "111");
        Usuario usuario = usuario("12345678901");
        registrarValidacao(titular);
        when(identidadeResolver.resolver(titular.profile()))
                .thenReturn(new IdentidadeResolvida(usuario));
        when(contextoResolver.resolver(usuario))
                .thenReturn(new ContextoResolvido(new ContextoInstitucional(unidade("Delegacia Centro"), "DELEGADO")));

        emitirDesafio(titular)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DESAFIO_EMITIDO"))
                .andExpect(jsonPath("$.nonce").value(NONCE))
                .andExpect(jsonPath("$.algoritmoAssinatura").value("SHA256withRSA"));
        responder(titular, assinatura(titular, NONCE), "SHA256withRSA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTENTICADO"))
                .andExpect(jsonPath("$.token").value("cert-token-1"))
                .andExpect(jsonPath("$.contexto.unidadeNome").value("Delegacia Centro"))
                .andExpect(jsonPath("$.contexto.papelNaUnidade").value("DELEGADO"));
    }

    @Test
    void nonceReusadoEhRejeitado() throws Exception {
        CertMaterial titular = material("12345678901", "112");
        Usuario usuario = usuario("12345678901");
        registrarValidacao(titular);
        when(identidadeResolver.resolver(titular.profile()))
                .thenReturn(new IdentidadeResolvida(usuario));
        when(contextoResolver.resolver(usuario))
                .thenReturn(new ContextoResolvido(new ContextoInstitucional(unidade("Delegacia Centro"), "DELEGADO")));

        emitirDesafio(titular);
        String assinatura = assinatura(titular, NONCE);
        responder(titular, assinatura, "SHA256withRSA").andExpect(status().isOk());

        responder(titular, assinatura, "SHA256withRSA")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("NEGADO"))
                .andExpect(jsonPath("$.motivo").value("NONCE_INVALIDO_OU_EXPIRADO"));
    }

    @Test
    void assinaturaInvalidaQueimaNonceDoCertificado() throws Exception {
        CertMaterial titular = material("12345678901", "113");
        registrarValidacao(titular);

        emitirDesafio(titular);
        responder(titular, assinaturaAdulterada(titular, NONCE), "SHA256withRSA")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.motivo").value("ASSINATURA_ASSINATURA_NAO_CONFERE"));

        responder(titular, assinatura(titular, NONCE), "SHA256withRSA")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.motivo").value("NONCE_INVALIDO_OU_EXPIRADO"));
    }

    @Test
    void certificadoDivergenteNaoQueimaNonceDoTitular() throws Exception {
        CertMaterial titular = material("12345678901", "114");
        CertMaterial divergente = material("98765432100", "115");
        Usuario usuario = usuario("12345678901");
        registrarValidacao(titular);
        registrarValidacao(divergente);
        when(identidadeResolver.resolver(titular.profile()))
                .thenReturn(new IdentidadeResolvida(usuario));
        when(contextoResolver.resolver(usuario))
                .thenReturn(new ContextoResolvido(new ContextoInstitucional(unidade("Delegacia Centro"), "DELEGADO")));

        emitirDesafio(titular);
        responder(divergente, assinatura(divergente, NONCE), "SHA256withRSA")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.motivo").value("NONCE_INVALIDO_OU_EXPIRADO"));

        responder(titular, assinatura(titular, NONCE), "SHA256withRSA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTENTICADO"));
    }

    @Test
    void usuarioInexistenteEhNegado() throws Exception {
        CertMaterial titular = material("12345678901", "116");
        registrarValidacao(titular);
        when(identidadeResolver.resolver(titular.profile()))
                .thenReturn(new IdentidadeNaoResolvida(MotivoIdentidade.USUARIO_INEXISTENTE));

        emitirDesafio(titular);

        responder(titular, assinatura(titular, NONCE), "SHA256withRSA")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.motivo").value("IDENTIDADE_USUARIO_INEXISTENTE"));
    }

    @Test
    void lotacaoMultiplaRetornaPendenteSelecao() throws Exception {
        CertMaterial titular = material("12345678901", "117");
        Usuario usuario = usuario("12345678901");
        registrarValidacao(titular);
        when(identidadeResolver.resolver(titular.profile()))
                .thenReturn(new IdentidadeResolvida(usuario));
        when(contextoResolver.resolver(usuario))
                .thenReturn(new PendenteSelecao(List.of(
                        lotacao("Delegacia Centro", "DELEGADO"),
                        lotacao("Departamento Regional", "COORDENADOR"))));

        emitirDesafio(titular);

        responder(titular, assinatura(titular, NONCE), "SHA256withRSA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE_SELECAO"))
                .andExpect(jsonPath("$.lotacoes", hasSize(2)));
        verify(passkeySessionService, never()).issue(any(Usuario.class), isNull(), any(String.class), any(OrigemAutenticacaoSessao.class));
    }

    @Test
    void prodSemIcpOperacionalNaoEmiteDesafio() throws Exception {
        CertMaterial titular = material("12345678901", "118");
        environment.setActiveProfiles("prod");
        when(chainValidator.policySnapshot())
                .thenReturn(new IcpBrasilSignaturePolicySnapshot(false, false, "LTA"));
        when(chainValidator.detailsFor(isNull(), isNull()))
                .thenReturn(new IcpBrasilChainValidationDetails(false, true, null, null));

        emitirDesafio(titular)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("INDISPONIVEL"))
                .andExpect(jsonPath("$.motivo").value("ICP_INDISPONIVEL"));
        verify(chainValidator, never()).validate(any(X509Certificate.class));
    }

    private org.springframework.test.web.servlet.ResultActions emitirDesafio(CertMaterial material)
            throws Exception {
        CertificadoAuthDtos.DesafioRequest request = new CertificadoAuthDtos.DesafioRequest();
        request.setCertificado(material.pem());
        return mockMvc.perform(post("/api/v1/auth/certificado/desafio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions responder(
            CertMaterial material,
            String assinatura,
            String algoritmo
    ) throws Exception {
        CertificadoAuthDtos.RespostaRequest request = new CertificadoAuthDtos.RespostaRequest();
        request.setCertificado(material.pem());
        request.setNonce(NONCE);
        request.setAssinatura(assinatura);
        request.setAlgoritmoAssinatura(algoritmo);
        return mockMvc.perform(post("/api/v1/auth/certificado/resposta")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private void registrarValidacao(CertMaterial material) {
        validacoes.put(x509Support.fingerprintSha256(material.certificado()),
                IcpBrasilValidationResult.ok(material.profile()));
    }

    private static Usuario usuario(String cpf) {
        Usuario usuario = new Usuario();
        usuario.setCpf(cpf);
        usuario.setAtivo(true);
        return usuario;
    }

    private static UnidadeInstituicao unidade(String nome) {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setNome(nome);
        unidade.setTipo(TipoUnidadeInstitucional.DELEGACIA);
        return unidade;
    }

    private static LotacaoInstituicao lotacao(String unidadeNome, String papel) {
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUnidade(unidade(unidadeNome));
        lotacao.setPapelNaUnidade(papel);
        return lotacao;
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
        return Base64.getEncoder().encodeToString(assinar(material.keyPair().getPrivate(), nonce));
    }

    private static String assinaturaAdulterada(CertMaterial material, String nonce) throws Exception {
        byte[] assinatura = assinar(material.keyPair().getPrivate(), nonce);
        assinatura[assinatura.length - 1] ^= 1;
        return Base64.getEncoder().encodeToString(assinatura);
    }

    private static byte[] assinar(PrivateKey privateKey, String nonce) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(nonce.getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }

    private record CertMaterial(
            KeyPair keyPair,
            X509Certificate certificado,
            String pem,
            IcpBrasilCertProfile profile
    ) {
    }
}
