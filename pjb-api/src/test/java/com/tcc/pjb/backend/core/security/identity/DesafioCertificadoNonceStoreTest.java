package com.tcc.pjb.backend.core.security.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

class DesafioCertificadoNonceStoreTest {

    private static final Duration TTL = Duration.ofSeconds(120);

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final DesafioCertificadoNonceStore store =
            new DesafioCertificadoNonceStore(redis, new CertificadoAuthPolicy(TTL, true));

    @Test
    void emitirGeraNonceSeguroComTtlSemPersistirReferenciaEmClaro() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), eq(TTL))).thenReturn(true);
        ArgumentCaptor<String> chave = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valor = ArgumentCaptor.forClass(String.class);

        String nonce = store.emitir("referencia-certificado-opaca");

        assertThat(nonce).matches("[A-Za-z0-9_-]{43}");
        verify(values).setIfAbsent(chave.capture(), valor.capture(), eq(TTL));
        assertThat(chave.getValue()).isEqualTo("pjb:auth:cert:nonce:" + nonce);
        assertThat(valor.getValue())
                .matches("[0-9a-f]{64}")
                .isNotEqualTo("referencia-certificado-opaca");
    }

    @Test
    void consumirDuasVezesRejeitaReplayPorOperacaoAtomica() {
        when(redis.execute(any(RedisScript.class), any(List.class), anyString()))
                .thenReturn(1L, 0L);
        String nonce = nonceValido();
        ArgumentCaptor<RedisScript<Long>> script = captorScript();

        boolean primeiroConsumo = store.consumir(nonce, "referencia-original");
        boolean segundoConsumo = store.consumir(nonce, "referencia-original");

        assertThat(primeiroConsumo).isTrue();
        assertThat(segundoConsumo).isFalse();
        verify(redis, times(2)).execute(script.capture(), eq(List.of(
                "pjb:auth:cert:nonce:" + nonce)), anyString());
        assertThat(script.getValue().getScriptAsString())
                .contains("redis.call('GET', KEYS[1])")
                .contains("v == ARGV[1]")
                .contains("redis.call('DEL', KEYS[1])");
    }

    @Test
    void consumirNonceInexistenteRetornaFalse() {
        when(redis.execute(any(RedisScript.class), any(List.class), anyString()))
                .thenReturn(0L);

        boolean consumido = store.consumir(nonceValido(), "referencia-original");

        assertThat(consumido).isFalse();
    }

    @Test
    void referenciaDivergenteNaoConsomeDesafioDoTitular() {
        when(redis.execute(any(RedisScript.class), any(List.class), anyString()))
                .thenReturn(0L, 1L);
        String nonce = nonceValido();
        ArgumentCaptor<String> hashReferencia = ArgumentCaptor.forClass(String.class);

        boolean referenciaDivergente = store.consumir(nonce, "referencia-divergente");
        boolean referenciaOriginal = store.consumir(nonce, "referencia-original");

        assertThat(referenciaDivergente).isFalse();
        assertThat(referenciaOriginal).isTrue();
        verify(redis, times(2)).execute(
                any(RedisScript.class),
                eq(List.of("pjb:auth:cert:nonce:" + nonce)),
                hashReferencia.capture());
        assertThat(hashReferencia.getAllValues())
                .hasSize(2)
                .doesNotHaveDuplicates()
                .allMatch(hash -> hash.matches("[0-9a-f]{64}"));
    }

    private static String nonceValido() {
        return "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<RedisScript<Long>> captorScript() {
        return ArgumentCaptor.forClass(RedisScript.class);
    }
}
