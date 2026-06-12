package com.tcc.pjb.backend.service.auth.surface;

import com.tcc.pjb.backend.core.icp.IcpBrasilChainValidator;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilChainValidationDetails;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignaturePolicySnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationResult;
import com.tcc.pjb.backend.core.security.identity.AssinaturaInvalida;
import com.tcc.pjb.backend.core.security.identity.AssinaturaValida;
import com.tcc.pjb.backend.core.security.identity.CertificadoAuthAuditService;
import com.tcc.pjb.backend.core.security.identity.CertificadoAuthPolicy;
import com.tcc.pjb.backend.core.security.identity.CertificadoIdentidadeResolver;
import com.tcc.pjb.backend.core.security.identity.CertificadoX509Support;
import com.tcc.pjb.backend.core.security.identity.ChaveNaoSuportada;
import com.tcc.pjb.backend.core.security.identity.ContextoInstitucionalResolver;
import com.tcc.pjb.backend.core.security.identity.ContextoNegado;
import com.tcc.pjb.backend.core.security.identity.ContextoResolucao;
import com.tcc.pjb.backend.core.security.identity.ContextoResolvido;
import com.tcc.pjb.backend.core.security.identity.DesafioCertificadoNonceStore;
import com.tcc.pjb.backend.core.security.identity.IdentidadeNaoResolvida;
import com.tcc.pjb.backend.core.security.identity.IdentidadeResolucao;
import com.tcc.pjb.backend.core.security.identity.IdentidadeResolvida;
import com.tcc.pjb.backend.core.security.identity.PendenteSelecao;
import com.tcc.pjb.backend.core.security.identity.ResultadoVerificacaoAssinatura;
import com.tcc.pjb.backend.core.security.identity.VerificadorAssinaturaCertificado;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.model.dto.security.CertificadoAuthDtos;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/**
 * Orquestra o login institucional por certificado e emite sessao opaca pelo mecanismo do passkey.
 * A ordem de resposta e contrato de seguranca: validar ICP, recalcular fingerprint server-side,
 * consumir nonce amarrado ao fingerprint, verificar assinatura do nonce e resolver identidade/contexto.
 */
@Service
public class CertificadoAuthFacadeService {

    private static final String ETAPA_DESAFIO = "DESAFIO";
    private static final String ETAPA_RESPOSTA = "RESPOSTA";

    private final IcpBrasilChainValidator chainValidator;
    private final DesafioCertificadoNonceStore nonceStore;
    private final VerificadorAssinaturaCertificado verificadorAssinatura;
    private final CertificadoIdentidadeResolver identidadeResolver;
    private final ContextoInstitucionalResolver contextoResolver;
    private final CertificadoAuthPolicy policy;
    private final CertificadoX509Support x509Support;
    private final CertificadoAuthAuditService auditService;
    private final PasskeySessionService passkeySessionService;
    private final ClientIpResolver clientIpResolver;
    private final Environment environment;

    public CertificadoAuthFacadeService(
            IcpBrasilChainValidator chainValidator,
            DesafioCertificadoNonceStore nonceStore,
            VerificadorAssinaturaCertificado verificadorAssinatura,
            CertificadoIdentidadeResolver identidadeResolver,
            ContextoInstitucionalResolver contextoResolver,
            CertificadoAuthPolicy policy,
            CertificadoX509Support x509Support,
            CertificadoAuthAuditService auditService,
            PasskeySessionService passkeySessionService,
            ClientIpResolver clientIpResolver,
            Environment environment
    ) {
        this.chainValidator = Objects.requireNonNull(chainValidator);
        this.nonceStore = Objects.requireNonNull(nonceStore);
        this.verificadorAssinatura = Objects.requireNonNull(verificadorAssinatura);
        this.identidadeResolver = Objects.requireNonNull(identidadeResolver);
        this.contextoResolver = Objects.requireNonNull(contextoResolver);
        this.policy = Objects.requireNonNull(policy);
        this.x509Support = Objects.requireNonNull(x509Support);
        this.auditService = Objects.requireNonNull(auditService);
        this.passkeySessionService = Objects.requireNonNull(passkeySessionService);
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver);
        this.environment = Objects.requireNonNull(environment);
    }

    public CertificadoAuthDtos.DesafioResponse emitirDesafio(
            CertificadoAuthDtos.DesafioRequest request
    ) {
        X509Certificate certificado = parseCertificado(request.getCertificado());
        if (certificado == null) {
            return desafioNegado("CERTIFICADO_INVALIDO");
        }
        if (icpIndisponivelEmProd()) {
            auditService.registrar(ETAPA_DESAFIO, false, "ICP_INDISPONIVEL");
            return new CertificadoAuthDtos.DesafioResponse(
                    CertificadoAuthDtos.Status.INDISPONIVEL,
                    "",
                    "",
                    "ICP_INDISPONIVEL");
        }
        IcpBrasilValidationResult validacao = chainValidator.validate(certificado);
        if (!validacao.valid()) {
            return desafioNegado("CERTIFICADO_INVALIDO");
        }
        String algoritmo = x509Support.algoritmoAssinaturaPadrao(certificado).orElse("");
        if (algoritmo.isBlank()) {
            return desafioNegado("CHAVE_NAO_SUPORTADA");
        }
        String fingerprint = x509Support.fingerprintSha256(certificado);
        String nonce = nonceStore.emitir(fingerprint);
        auditService.registrar(ETAPA_DESAFIO, true, "EMITIDO");
        return new CertificadoAuthDtos.DesafioResponse(
                CertificadoAuthDtos.Status.DESAFIO_EMITIDO,
                nonce,
                algoritmo,
                "");
    }

    public CertificadoAuthDtos.Resposta responder(
            CertificadoAuthDtos.RespostaRequest request,
            HttpServletRequest servletRequest
    ) {
        X509Certificate certificado = parseCertificado(request.getCertificado());
        if (certificado == null) {
            return respostaNegada("CERTIFICADO_INVALIDO");
        }
        if (icpIndisponivelEmProd()) {
            return respostaNegada("ICP_INDISPONIVEL");
        }
        IcpBrasilValidationResult validacao = chainValidator.validate(certificado);
        if (!validacao.valid() || validacao.profile() == null) {
            return respostaNegada("CERTIFICADO_INVALIDO");
        }
        String fingerprint = x509Support.fingerprintSha256(certificado);
        if (!nonceStore.consumir(request.getNonce(), fingerprint)) {
            return respostaNegada("NONCE_INVALIDO_OU_EXPIRADO");
        }
        byte[] assinatura = decodificarAssinatura(request.getAssinatura());
        if (assinatura.length == 0) {
            return respostaNegada("ASSINATURA_MALFORMADA");
        }
        ResultadoVerificacaoAssinatura resultadoAssinatura = verificadorAssinatura.verificar(
                request.getNonce().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                assinatura,
                certificado,
                request.getAlgoritmoAssinatura());
        if (!(resultadoAssinatura instanceof AssinaturaValida)) {
            return respostaNegada(motivoAssinatura(resultadoAssinatura));
        }
        IdentidadeResolucao identidade = identidadeResolver.resolver(validacao.profile());
        if (identidade instanceof IdentidadeNaoResolvida naoResolvida) {
            return respostaNegada("IDENTIDADE_" + naoResolvida.motivo().name());
        }
        IdentidadeResolvida resolvida = (IdentidadeResolvida) identidade;
        ContextoResolucao contexto = contextoResolver.resolver(resolvida.usuario());
        if (contexto instanceof ContextoResolvido resolvido) {
            PasskeySessionService.IssuedPasskeySession sessao = passkeySessionService.issue(
                    resolvida.usuario(),
                    null,
                    clientIpResolver.resolve(servletRequest));
            auditService.registrar(ETAPA_RESPOSTA, true, "AUTENTICADO");
            return new CertificadoAuthDtos.AutenticadoResponse(
                    CertificadoAuthDtos.Status.AUTENTICADO,
                    sessao.token(),
                    sessao.expiresAt(),
                    contexto(resolvido.contexto().unidade(), resolvido.contexto().papelNaUnidade()));
        }
        if (contexto instanceof PendenteSelecao pendenteSelecao) {
            auditService.registrar(ETAPA_RESPOSTA, true, "PENDENTE_SELECAO");
            return new CertificadoAuthDtos.PendenteSelecaoResponse(
                    CertificadoAuthDtos.Status.PENDENTE_SELECAO,
                    lotacoes(pendenteSelecao.lotacoesAtivas()));
        }
        ContextoNegado negado = (ContextoNegado) contexto;
        return respostaNegada("CONTEXTO_" + negado.motivo().name());
    }

    private CertificadoAuthDtos.DesafioResponse desafioNegado(String motivo) {
        auditService.registrar(ETAPA_DESAFIO, false, motivo);
        return new CertificadoAuthDtos.DesafioResponse(
                CertificadoAuthDtos.Status.NEGADO,
                "",
                "",
                motivo);
    }

    private CertificadoAuthDtos.NegadoResponse respostaNegada(String motivo) {
        auditService.registrar(ETAPA_RESPOSTA, false, motivo);
        return new CertificadoAuthDtos.NegadoResponse(CertificadoAuthDtos.Status.NEGADO, motivo);
    }

    private X509Certificate parseCertificado(String certificado) {
        try {
            return x509Support.parse(certificado);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean icpIndisponivelEmProd() {
        if (!policy.enforceIcpRealEmProd()
                || !environment.acceptsProfiles(Profiles.of("prod"))) {
            return false;
        }
        IcpBrasilSignaturePolicySnapshot snapshot = chainValidator.policySnapshot();
        IcpBrasilChainValidationDetails details = chainValidator.detailsFor(null, null);
        return !snapshot.enabled() || !snapshot.enforceChainValidation() || !details.trustAnchorsLoaded();
    }

    private static byte[] decodificarAssinatura(String assinatura) {
        try {
            return Base64.getDecoder().decode(assinatura);
        } catch (IllegalArgumentException ex) {
            try {
                return Base64.getUrlDecoder().decode(assinatura);
            } catch (IllegalArgumentException ignored) {
                return new byte[0];
            }
        }
    }

    private static String motivoAssinatura(ResultadoVerificacaoAssinatura resultado) {
        if (resultado instanceof AssinaturaInvalida invalida) {
            return "ASSINATURA_" + invalida.motivo().name();
        }
        if (resultado instanceof ChaveNaoSuportada naoSuportada) {
            return "ASSINATURA_CHAVE_NAO_SUPORTADA_" + naoSuportada.algoritmo();
        }
        return "ASSINATURA_INVALIDA";
    }

    private static CertificadoAuthDtos.ContextoResponse contexto(
            UnidadeInstituicao unidade,
            String papel
    ) {
        return new CertificadoAuthDtos.ContextoResponse(
                unidade.getId(),
                unidade.getNome(),
                unidade.getTipo() == null ? "" : unidade.getTipo().name(),
                papel);
    }

    private static List<CertificadoAuthDtos.LotacaoResponse> lotacoes(
            List<LotacaoInstituicao> lotacoes
    ) {
        return lotacoes.stream()
                .map(CertificadoAuthFacadeService::lotacao)
                .toList();
    }

    private static CertificadoAuthDtos.LotacaoResponse lotacao(LotacaoInstituicao lotacao) {
        UnidadeInstituicao unidade = lotacao.getUnidade();
        return new CertificadoAuthDtos.LotacaoResponse(
                lotacao.getId(),
                unidade.getId(),
                unidade.getNome(),
                unidade.getTipo() == null ? "" : unidade.getTipo().name(),
                lotacao.getPapelNaUnidade());
    }
}
