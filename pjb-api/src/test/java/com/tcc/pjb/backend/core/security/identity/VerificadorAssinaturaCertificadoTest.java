package com.tcc.pjb.backend.core.security.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.Test;

class VerificadorAssinaturaCertificadoTest {

    private static final byte[] NONCE =
            "nonce-desafio-certificado".getBytes(StandardCharsets.UTF_8);

    private final VerificadorAssinaturaCertificado verificador =
            new VerificadorAssinaturaCertificado();

    @Test
    void assinaturaRsaSha256Valida() throws Exception {
        KeyPair par = gerarPar("RSA", 2048);
        byte[] assinatura = assinar(NONCE, par.getPrivate(), "SHA256withRSA");

        ResultadoVerificacaoAssinatura resultado = verificador.verificar(
                NONCE,
                assinatura,
                certificado(par),
                "SHA256withRSA");

        assertThat(resultado).isEqualTo(new AssinaturaValida("SHA256withRSA"));
    }

    @Test
    void assinaturaRsaSha512Valida() throws Exception {
        KeyPair par = gerarPar("RSA", 2048);
        byte[] assinatura = assinar(NONCE, par.getPrivate(), "SHA512withRSA");

        ResultadoVerificacaoAssinatura resultado = verificador.verificar(
                NONCE,
                assinatura,
                certificado(par),
                "SHA512withRSA");

        assertThat(resultado).isEqualTo(new AssinaturaValida("SHA512withRSA"));
    }

    @Test
    void assinaturaEcSha512Valida() throws Exception {
        KeyPair par = gerarPar("EC", 256);
        byte[] assinatura = assinar(NONCE, par.getPrivate(), "SHA512withECDSA");

        ResultadoVerificacaoAssinatura resultado = verificador.verificar(
                NONCE,
                assinatura,
                certificado(par),
                "SHA512withECDSA");

        assertThat(resultado).isEqualTo(new AssinaturaValida("SHA512withECDSA"));
    }

    @Test
    void assinaturaAdulteradaNaoConfere() throws Exception {
        KeyPair par = gerarPar("RSA", 2048);
        byte[] assinatura = assinar(NONCE, par.getPrivate(), "SHA256withRSA");
        assinatura[assinatura.length - 1] ^= 1;

        ResultadoVerificacaoAssinatura resultado = verificador.verificar(
                NONCE,
                assinatura,
                certificado(par),
                "SHA256withRSA");

        assertThat(resultado).isEqualTo(new AssinaturaInvalida(
                MotivoAssinaturaInvalida.ASSINATURA_NAO_CONFERE,
                "SHA256withRSA"));
    }

    @Test
    void dadosAdulteradosNaoConferem() throws Exception {
        KeyPair par = gerarPar("RSA", 2048);
        byte[] assinatura = assinar(NONCE, par.getPrivate(), "SHA256withRSA");
        byte[] dadosAdulterados = NONCE.clone();
        dadosAdulterados[0] ^= 1;

        ResultadoVerificacaoAssinatura resultado = verificador.verificar(
                dadosAdulterados,
                assinatura,
                certificado(par),
                "SHA256withRSA");

        assertThat(resultado).isEqualTo(new AssinaturaInvalida(
                MotivoAssinaturaInvalida.ASSINATURA_NAO_CONFERE,
                "SHA256withRSA"));
    }

    @Test
    void chavePublicaDeOutroParNaoConfere() throws Exception {
        KeyPair assinante = gerarPar("RSA", 2048);
        KeyPair outro = gerarPar("RSA", 2048);
        byte[] assinatura = assinar(NONCE, assinante.getPrivate(), "SHA256withRSA");

        ResultadoVerificacaoAssinatura resultado = verificador.verificar(
                NONCE,
                assinatura,
                certificado(outro),
                "SHA256withRSA");

        assertThat(resultado).isEqualTo(new AssinaturaInvalida(
                MotivoAssinaturaInvalida.ASSINATURA_NAO_CONFERE,
                "SHA256withRSA"));
    }

    @Test
    void sha1EhRejeitadoComoFraco() throws Exception {
        KeyPair par = gerarPar("RSA", 2048);

        ResultadoVerificacaoAssinatura resultado = verificador.verificar(
                NONCE,
                new byte[] {1},
                certificado(par),
                "SHA1withRSA");

        assertThat(resultado).isEqualTo(new AssinaturaInvalida(
                MotivoAssinaturaInvalida.ALGORITMO_FRACO,
                "SHA1withRSA"));
    }

    @Test
    void md5EhRejeitadoComoFraco() throws Exception {
        KeyPair par = gerarPar("RSA", 2048);

        ResultadoVerificacaoAssinatura resultado = verificador.verificar(
                NONCE,
                new byte[] {1},
                certificado(par),
                "MD5withRSA");

        assertThat(resultado).isEqualTo(new AssinaturaInvalida(
                MotivoAssinaturaInvalida.ALGORITMO_FRACO,
                "MD5withRSA"));
    }

    @Test
    void chaveDsaNaoEhSuportada() throws Exception {
        KeyPair par = gerarPar("DSA", 2048);

        ResultadoVerificacaoAssinatura resultado = verificador.verificar(
                NONCE,
                new byte[] {1},
                certificado(par),
                "SHA256withDSA");

        assertThat(resultado).isEqualTo(new ChaveNaoSuportada("DSA"));
    }

    @Test
    void algoritmoEcdsaComChaveRsaEhRejeitado() throws Exception {
        KeyPair par = gerarPar("RSA", 2048);

        ResultadoVerificacaoAssinatura resultado = verificador.verificar(
                NONCE,
                new byte[] {1},
                certificado(par),
                "SHA256withECDSA");

        assertThat(resultado).isEqualTo(new AssinaturaInvalida(
                MotivoAssinaturaInvalida.ALGORITMO_INCOMPATIVEL_COM_CHAVE,
                "SHA256withECDSA"));
    }

    @Test
    void algoritmoOmitidoUsaSha256CompativelComAChave() throws Exception {
        KeyPair par = gerarPar("EC", 256);
        byte[] assinatura = assinar(NONCE, par.getPrivate(), "SHA256withECDSA");

        ResultadoVerificacaoAssinatura resultado =
                verificador.verificar(NONCE, assinatura, certificado(par));

        assertThat(resultado).isEqualTo(new AssinaturaValida("SHA256withECDSA"));
    }

    private static KeyPair gerarPar(String algoritmo, int tamanho) throws Exception {
        KeyPairGenerator gerador = KeyPairGenerator.getInstance(algoritmo);
        gerador.initialize(tamanho);
        return gerador.generateKeyPair();
    }

    private static byte[] assinar(byte[] dados, PrivateKey chave, String algoritmo)
            throws Exception {
        Signature assinatura = Signature.getInstance(algoritmo);
        assinatura.initSign(chave);
        assinatura.update(dados);
        return assinatura.sign();
    }

    private static X509Certificate certificado(KeyPair par) {
        X509Certificate certificado = mock(X509Certificate.class);
        when(certificado.getPublicKey()).thenReturn(par.getPublic());
        return certificado;
    }
}
