package com.tcc.pjb.backend.core.security.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Mantém desafios criptográficos de uso único no Redis com expiração curta.
 * O nonce possui 256 bits gerados por SecureRandom e é consumido por script Lua atômico.
 * A referência é persistida somente como SHA-256, sem certificado ou identificador em claro.
 * O consumo exige a mesma referência da emissão e preserva o desafio quando ela diverge.
 * A atomicidade real do script ainda requer cobertura de integração com Redis em fase futura.
 */
@Service
public class DesafioCertificadoNonceStore {

    private static final String PREFIXO = "pjb:auth:cert:nonce:";
    private static final int TAMANHO_NONCE_BYTES = 32;
    private static final int MAX_TENTATIVAS_EMISSAO = 5;
    private static final Pattern FORMATO_NONCE = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final DefaultRedisScript<Long> CONSUMIR_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); "
                    + "if v == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redis;
    private final Duration nonceTtl;
    private final SecureRandom secureRandom;

    public DesafioCertificadoNonceStore(
            StringRedisTemplate redis,
            CertificadoAuthPolicy policy
    ) {
        this.redis = Objects.requireNonNull(redis);
        this.nonceTtl = validarTtl(Objects.requireNonNull(policy).nonceTtl());
        this.secureRandom = new SecureRandom();
    }

    public String emitir(String referencia) {
        String valor = hashReferencia(referencia);
        for (int tentativa = 0; tentativa < MAX_TENTATIVAS_EMISSAO; tentativa++) {
            String nonce = gerarNonce();
            Boolean criado = redis.opsForValue().setIfAbsent(chave(nonce), valor, nonceTtl);
            if (Boolean.TRUE.equals(criado)) {
                return nonce;
            }
        }
        throw new IllegalStateException("nao_foi_possivel_emitir_nonce_unico");
    }

    public boolean consumir(String nonce, String referenciaEsperada) {
        if (nonce == null || !FORMATO_NONCE.matcher(nonce).matches()
                || referenciaEsperada == null || referenciaEsperada.isBlank()) {
            return false;
        }
        Long consumido = redis.execute(
                CONSUMIR_SCRIPT,
                Collections.singletonList(chave(nonce)),
                hashReferencia(referenciaEsperada));
        return Long.valueOf(1L).equals(consumido);
    }

    private String gerarNonce() {
        byte[] bytes = new byte[TAMANHO_NONCE_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashReferencia(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("referencia_obrigatoria");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(referencia.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("sha256_indisponivel", ex);
        }
    }

    private static Duration validarTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("nonce_ttl_deve_ser_positivo");
        }
        return ttl;
    }

    private static String chave(String nonce) {
        return PREFIXO + nonce;
    }
}
