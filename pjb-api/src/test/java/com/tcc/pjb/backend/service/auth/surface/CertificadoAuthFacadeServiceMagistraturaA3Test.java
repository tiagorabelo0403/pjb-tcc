package com.tcc.pjb.backend.service.auth.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.icp.IcpBrasilCertProfile;
import com.tcc.pjb.backend.core.icp.IcpBrasilChainValidator;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationResult;
import com.tcc.pjb.backend.core.security.identity.AssinaturaValida;
import com.tcc.pjb.backend.core.security.identity.CertificadoAuthAuditService;
import com.tcc.pjb.backend.core.security.identity.CertificadoAuthPolicy;
import com.tcc.pjb.backend.core.security.identity.CertificadoIdentidadeResolver;
import com.tcc.pjb.backend.core.security.identity.CertificadoX509Support;
import com.tcc.pjb.backend.core.security.identity.ContextoInstitucionalResolver;
import com.tcc.pjb.backend.core.security.identity.ContextoNegado;
import com.tcc.pjb.backend.core.security.identity.DesafioCertificadoNonceStore;
import com.tcc.pjb.backend.core.security.identity.IdentidadeResolvida;
import com.tcc.pjb.backend.core.security.identity.MotivoContexto;
import com.tcc.pjb.backend.core.security.identity.VerificadorAssinaturaCertificado;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.model.dto.security.CertificadoAuthDtos;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class CertificadoAuthFacadeServiceMagistraturaA3Test {

    private final IcpBrasilChainValidator chainValidator = mock(IcpBrasilChainValidator.class);
    private final DesafioCertificadoNonceStore nonceStore = mock(DesafioCertificadoNonceStore.class);
    private final VerificadorAssinaturaCertificado verificadorAssinatura = mock(VerificadorAssinaturaCertificado.class);
    private final CertificadoIdentidadeResolver identidadeResolver = mock(CertificadoIdentidadeResolver.class);
    private final ContextoInstitucionalResolver contextoResolver = mock(ContextoInstitucionalResolver.class);
    private final CertificadoAuthPolicy policy = mock(CertificadoAuthPolicy.class);
    private final CertificadoX509Support x509Support = mock(CertificadoX509Support.class);
    private final CertificadoAuthAuditService auditService = mock(CertificadoAuthAuditService.class);
    private final PasskeySessionService passkeySessionService = mock(PasskeySessionService.class);
    private final ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
    private final Environment environment = mock(Environment.class);

    private final CertificadoAuthFacadeService service = new CertificadoAuthFacadeService(
            chainValidator, nonceStore, verificadorAssinatura, identidadeResolver, contextoResolver,
            policy, x509Support, auditService, passkeySessionService, clientIpResolver, environment);

    @Test
    void certificadoA1ParaJuizENegado() throws Exception {
        var resposta = executarFluxoAteResolucaoDeIdentidade("A1", TipoUsuario.JUIZ);

        assertThat(resposta).isInstanceOf(CertificadoAuthDtos.NegadoResponse.class);
        assertThat(((CertificadoAuthDtos.NegadoResponse) resposta).motivo()).isEqualTo("CERTIFICADO_TIPO_INSUFICIENTE");
    }

    @Test
    void certificadoA3ParaJuizPassaDaCheckagemDeTipo() throws Exception {
        var resposta = executarFluxoAteResolucaoDeIdentidade("A3", TipoUsuario.JUIZ);

        boolean bloqueadoPorTipo = resposta instanceof CertificadoAuthDtos.NegadoResponse negado
                && "CERTIFICADO_TIPO_INSUFICIENTE".equals(negado.motivo());
        assertThat(bloqueadoPorTipo).isFalse();
    }

    @Test
    void certificadoA1ParaCidadaoNaoEBloqueadoPorTipo() throws Exception {
        var resposta = executarFluxoAteResolucaoDeIdentidade("A1", TipoUsuario.CIDADAO);

        boolean bloqueadoPorTipo = resposta instanceof CertificadoAuthDtos.NegadoResponse negado
                && "CERTIFICADO_TIPO_INSUFICIENTE".equals(negado.motivo());
        assertThat(bloqueadoPorTipo).isFalse();
    }

    private CertificadoAuthDtos.Resposta executarFluxoAteResolucaoDeIdentidade(String certType, TipoUsuario tipoUsuario) throws Exception {
        X509Certificate certificado = mock(X509Certificate.class);
        when(x509Support.parse(anyString())).thenReturn(certificado);
        when(x509Support.fingerprintSha256(certificado)).thenReturn("fingerprint");

        IcpBrasilCertProfile profile = new IcpBrasilCertProfile(
                "CN=Teste", "CN=AC Teste", "1", "12345678900", null, "Fulano",
                certType, "AC-TESTE", Instant.now(), Instant.now().plusSeconds(3600));
        when(chainValidator.validate(certificado)).thenReturn(IcpBrasilValidationResult.ok(profile));

        when(nonceStore.consumir(anyString(), anyString())).thenReturn(true);
        when(verificadorAssinatura.verificar(any(), any(), any(), any())).thenReturn(new AssinaturaValida("SHA256withRSA"));

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(tipoUsuario);
        when(identidadeResolver.resolver(profile)).thenReturn(new IdentidadeResolvida(usuario));

        when(contextoResolver.resolver(usuario)).thenReturn(new ContextoNegado(MotivoContexto.SEM_LOTACAO_ATIVA));

        CertificadoAuthDtos.RespostaRequest request = new CertificadoAuthDtos.RespostaRequest();
        request.setCertificado("cert-pem");
        request.setNonce("nonce-1");
        request.setAssinatura(Base64.getEncoder().encodeToString("assinatura".getBytes()));
        request.setAlgoritmoAssinatura("SHA256withRSA");

        HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        return service.responder(request, servletRequest);
    }
}
