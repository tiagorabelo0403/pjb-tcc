package com.tcc.pjb.backend.core.security.identity;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Verifica prova de posse da chave privada associada a um certificado X.509.
 * O algoritmo informado pelo desafio é limitado a SHA-256 ou SHA-512 e deve corresponder
 * à família RSA ou EC da chave pública, impedindo confusão de algoritmo controlada pelo cliente.
 * O algoritmo de assinatura do certificado não é reutilizado porque descreve a assinatura da
 * autoridade certificadora sobre o certificado, não a assinatura do desafio pelo titular.
 * Há três implementações de verificação X.509 no projeto: esta classe, AdvogadoBaptismService e
 * EdgeAttestationService. A consolidação futura em componente criptográfico único é dívida conhecida.
 */
@Service
public class VerificadorAssinaturaCertificado {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(VerificadorAssinaturaCertificado.class);
    private static final String ALGORITMO_DESCONHECIDO = "DESCONHECIDO";

    public ResultadoVerificacaoAssinatura verificar(
            byte[] dadosOriginais,
            byte[] assinatura,
            X509Certificate certificado
    ) {
        return verificar(dadosOriginais, assinatura, certificado, null);
    }

    public ResultadoVerificacaoAssinatura verificar(
            byte[] dadosOriginais,
            byte[] assinatura,
            X509Certificate certificado,
            String algoritmoInformado
    ) {
        if (certificado == null || certificado.getPublicKey() == null) {
            return new ChaveNaoSuportada(ALGORITMO_DESCONHECIDO);
        }
        PublicKey chavePublica = certificado.getPublicKey();
        Optional<TipoChave> tipoChave = identificarTipoChave(chavePublica);
        if (tipoChave.isEmpty()) {
            return new ChaveNaoSuportada(algoritmoChave(chavePublica));
        }

        ResolucaoAlgoritmo resolucao = resolverAlgoritmo(algoritmoInformado, tipoChave.orElseThrow());
        if (resolucao.motivoFalha().isPresent()) {
            return new AssinaturaInvalida(
                    resolucao.motivoFalha().orElseThrow(),
                    resolucao.algoritmo());
        }
        if (dadosOriginais == null || assinatura == null) {
            return new AssinaturaInvalida(
                    MotivoAssinaturaInvalida.DADOS_INVALIDOS,
                    resolucao.algoritmo());
        }

        try {
            Signature verificador = Signature.getInstance(resolucao.algoritmo());
            verificador.initVerify(chavePublica);
            verificador.update(dadosOriginais);
            if (verificador.verify(assinatura)) {
                return new AssinaturaValida(resolucao.algoritmo());
            }
            return new AssinaturaInvalida(
                    MotivoAssinaturaInvalida.ASSINATURA_NAO_CONFERE,
                    resolucao.algoritmo());
        } catch (GeneralSecurityException ex) {
            LOGGER.warn(
                    "Falha criptografica na verificacao de assinatura: tipo={}",
                    ex.getClass().getSimpleName());
            return new AssinaturaInvalida(
                    MotivoAssinaturaInvalida.FALHA_CRIPTOGRAFICA,
                    resolucao.algoritmo());
        }
    }

    private static Optional<TipoChave> identificarTipoChave(PublicKey chavePublica) {
        if (chavePublica instanceof RSAPublicKey) {
            return Optional.of(TipoChave.RSA);
        }
        if (chavePublica instanceof ECPublicKey) {
            return Optional.of(TipoChave.EC);
        }
        return Optional.empty();
    }

    private static ResolucaoAlgoritmo resolverAlgoritmo(
            String algoritmoInformado,
            TipoChave tipoChave
    ) {
        if (algoritmoInformado == null || algoritmoInformado.isBlank()) {
            return ResolucaoAlgoritmo.sucesso(tipoChave.algoritmoPadrao());
        }
        String normalizado = algoritmoInformado
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        if (normalizado.startsWith("SHA1") || normalizado.startsWith("MD5")) {
            return ResolucaoAlgoritmo.falha(
                    algoritmoInformado.trim(),
                    MotivoAssinaturaInvalida.ALGORITMO_FRACO);
        }

        Optional<String> canonico = switch (normalizado) {
            case "SHA256WITHRSA" -> Optional.of("SHA256withRSA");
            case "SHA512WITHRSA" -> Optional.of("SHA512withRSA");
            case "SHA256WITHECDSA" -> Optional.of("SHA256withECDSA");
            case "SHA512WITHECDSA" -> Optional.of("SHA512withECDSA");
            default -> Optional.empty();
        };
        if (canonico.isEmpty()) {
            return ResolucaoAlgoritmo.falha(
                    algoritmoInformado.trim(),
                    MotivoAssinaturaInvalida.ALGORITMO_NAO_PERMITIDO);
        }
        String algoritmoCanonico = canonico.orElseThrow();
        if (!tipoChave.compativel(algoritmoCanonico)) {
            return ResolucaoAlgoritmo.falha(
                    algoritmoCanonico,
                    MotivoAssinaturaInvalida.ALGORITMO_INCOMPATIVEL_COM_CHAVE);
        }
        return ResolucaoAlgoritmo.sucesso(algoritmoCanonico);
    }

    private static String algoritmoChave(PublicKey chavePublica) {
        if (chavePublica == null || chavePublica.getAlgorithm() == null
                || chavePublica.getAlgorithm().isBlank()) {
            return ALGORITMO_DESCONHECIDO;
        }
        return chavePublica.getAlgorithm();
    }

    private enum TipoChave {
        RSA("SHA256withRSA", "withRSA"),
        EC("SHA256withECDSA", "withECDSA");

        private final String algoritmoPadrao;
        private final String sufixo;

        TipoChave(String algoritmoPadrao, String sufixo) {
            this.algoritmoPadrao = algoritmoPadrao;
            this.sufixo = sufixo;
        }

        private String algoritmoPadrao() {
            return algoritmoPadrao;
        }

        private boolean compativel(String algoritmo) {
            return algoritmo.endsWith(sufixo);
        }
    }

    private record ResolucaoAlgoritmo(
            String algoritmo,
            Optional<MotivoAssinaturaInvalida> motivoFalha
    ) {

        private static ResolucaoAlgoritmo sucesso(String algoritmo) {
            return new ResolucaoAlgoritmo(algoritmo, Optional.empty());
        }

        private static ResolucaoAlgoritmo falha(
                String algoritmo,
                MotivoAssinaturaInvalida motivo
        ) {
            return new ResolucaoAlgoritmo(algoritmo, Optional.of(motivo));
        }
    }
}
