package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class PjbHardwareSecurityModule {

    private static final Logger log = LoggerFactory.getLogger(PjbHardwareSecurityModule.class);

    public record AssinaturaHsm(
            byte[] bytes,
            String algoritmo,
            String provedorNome,
            boolean mockada,
            String hexResume
    ) {
    }

    private final PjbHsmProperties props;
    private final SSLContext sslContext;
    private final Provider hsmProvider;
    private final PrivateKey chavePrivada;
    private final Semaphore semaphore;

    public PjbHardwareSecurityModule(PjbHsmProperties props) {
        this.props = Objects.requireNonNull(props, "props");
        props.validateIfEnabled();
        this.semaphore = new Semaphore(props.maxConcurrentOps(), true);
        if (!props.enabled() || props.mockEnabled()) {
            log.warn("[HSM] Operando em modo MOCK. Assinaturas não possuem validade jurídica real.");
            this.hsmProvider = null;
            this.chavePrivada = null;
            this.sslContext = buildSslContextMock();
            return;
        }
        try {
            Provider baseProvider = Objects.requireNonNull(Security.getProvider("SunPKCS11"), "Provider SunPKCS11 indisponível");
            Provider provider = baseProvider.configure(props.pkcs11ConfigPath());
            Security.addProvider(provider);
            this.hsmProvider = provider;
            char[] pin = props.pin().toCharArray();
            KeyStore ks = KeyStore.getInstance("PKCS11", provider);
            ks.load(null, pin);
            this.chavePrivada = (PrivateKey) ks.getKey(props.keyAlias(), null);
            if (this.chavePrivada == null) {
                throw new IllegalStateException("Chave privada não encontrada no HSM com alias: " + props.keyAlias());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, pin);
            KeyStore trustStore = KeyStore.getInstance(props.trustStoreType());
            try (InputStream ts = new FileInputStream(props.trustStorePath())) {
                trustStore.load(ts, props.trustStorePassword().toCharArray());
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            SSLContext ctx = SSLContext.getInstance("TLSv1.3");
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            this.sslContext = ctx;
            log.info("[HSM] Módulo inicializado com sucesso. Provider={} Alias={}", provider.getName(), props.keyAlias());
        } catch (Exception e) {
            throw new IllegalStateException("[HSM] Falha crítica na inicialização: " + e.getMessage(), e);
        }
    }

    public AssinaturaHsm assinar(byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(props.operationTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HsmBusyException("HSM indisponível: thread interrompida durante espera por slot.");
        }
        if (!acquired) {
            throw new HsmBusyException("HSM saturado — máximo de operações concorrentes atingido.");
        }
        try {
            if (props.mockEnabled() || !props.enabled()) {
                return assinarMock(payload);
            }
            return assinarReal(payload);
        } finally {
            semaphore.release();
        }
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public boolean isMock() {
        return props.mockEnabled() || !props.enabled();
    }

    private AssinaturaHsm assinarReal(byte[] payload) {
        try {
            Signature sig = Signature.getInstance(props.signatureAlgorithm(), hsmProvider);
            sig.initSign(chavePrivada);
            sig.update(payload);
            byte[] bytes = sig.sign();
            String hex = resumirHex(bytes);
            if (props.auditarOperacoes()) {
                log.info("[HSM] Assinatura gerada. algoritmo={} tamanho={}", props.signatureAlgorithm(), bytes.length);
            }
            return new AssinaturaHsm(bytes, props.signatureAlgorithm(), hsmProvider.getName(), false, hex);
        } catch (Exception e) {
            throw new HsmOperationException("Falha na assinatura HSM: " + e.getMessage(), e);
        }
    }

    private AssinaturaHsm assinarMock(byte[] payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(payload);
            String hex = HexFormat.of().formatHex(hash);
            byte[] mockSig = ("MOCK_SIG::" + hex).getBytes(StandardCharsets.UTF_8);
            return new AssinaturaHsm(mockSig, "SHA256withRSA_MOCK", "MOCK_PROVIDER", true, resumirHex(mockSig));
        } catch (Exception e) {
            throw new HsmOperationException("Falha na assinatura mock: " + e.getMessage(), e);
        }
    }

    private static SSLContext buildSslContextMock() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLSv1.3");
            ctx.init(null, null, new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criar SSLContext mock", e);
        }
    }

    private static String resumirHex(byte[] bytes) {
        String hex = HexFormat.of().formatHex(bytes);
        return hex.substring(0, Math.min(16, hex.length())) + "...";
    }

    public static final class HsmBusyException extends RuntimeException {
        public HsmBusyException(String message) {
            super(message);
        }
    }

    public static final class HsmOperationException extends RuntimeException {
        public HsmOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
