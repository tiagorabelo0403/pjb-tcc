package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QrCodeMandadoService {

    private static final Logger log = LoggerFactory.getLogger(QrCodeMandadoService.class);
    private static final String RESOURCE_TYPE = "QR_MANDADO";
    private static final String DOMAIN_QR = "QR_MANDADO_ATIVO";
    private static final String DOMAIN_QR_VERIFICACAO = "QR_MANDADO_VERIFICACAO";
    private static final int TOKEN_BYTES = 32;

    public record QrCodeMandado(
            String tokenUnico,
            String expedicaoUuid,
            Long processoId,
            String numeroUnificado,
            String urlVerificacao,
            String urlQrCodeSvg,
            Instant geradoEm,
            Instant expiresAt,
            boolean ativo,
            String hashIntegridade
    ) {
    }

    public record VerificacaoQrResult(
            boolean autentico,
            String expedicaoUuid,
            Long processoId,
            String numeroUnificado,
            String tipoAto,
            String instanciaExpedidora,
            Instant expedidaEm,
            Instant verificadaEm,
            String ipVerificador,
            boolean primeiraVerificacao
    ) {
    }

    private final ExpedicaoJudicialRepository expedicaoRepository;
    private final AuditLedgerService auditLedger;
    private final ObjectProvider<WebhookOutboundService> webhookProvider;
    private final ComunicacaoJudicialStateStore stateStore;
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final Duration PRIMEIRA_VERIFICACAO_TTL = Duration.ofDays(7);

    private final SecureRandom secureRandom = new SecureRandom();
    private final Cache<String, QrCodeMandado> qrCodesPorToken = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterAccess(CACHE_TTL)
            .build();
    private final Cache<String, QrCodeMandado> qrCodesPorExpedicao = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterAccess(CACHE_TTL)
            .build();
    private final Cache<String, Boolean> tokensPrimeiraVerificacao = Caffeine.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(PRIMEIRA_VERIFICACAO_TTL)
            .build();

    @Value("${pjb.portal.base-url:https://portal.pjb.jus.br}")
    private String portalBaseUrl;

    public QrCodeMandadoService(ExpedicaoJudicialRepository expedicaoRepository,
                                AuditLedgerService auditLedger,
                                ObjectProvider<WebhookOutboundService> webhookProvider,
                                ComunicacaoJudicialStateStore stateStore) {
        this.expedicaoRepository = Objects.requireNonNull(expedicaoRepository, "expedicaoRepository");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.webhookProvider = Objects.requireNonNull(webhookProvider, "webhookProvider");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    }


    @Transactional
    public QrCodeMandado gerar(String expedicaoUuid) {
        Objects.requireNonNull(expedicaoUuid, "expedicaoUuid");
        ExpedicaoJudicial expedicao = expedicaoRepository.findByExpedicaoUuid(expedicaoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Expedição não encontrada: " + expedicaoUuid));
        QrCodeMandado existente = consultarPorExpedicao(expedicaoUuid).orElse(null);
        if (existente != null) {
            return existente;
        }
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);
        String urlVerif = portalBaseUrl + "/verificar-mandado?t=" + token;
        String hashInteg = sha256Hex(token + expedicaoUuid + Instant.now().toEpochMilli());
        QrCodeMandado qr = new QrCodeMandado(
                token,
                expedicaoUuid,
                expedicao.getProcessoId(),
                expedicao.getNumeroUnificado(),
                urlVerif,
                gerarSvgSimbolico(urlVerif),
                Instant.now(),
                Instant.now().plus(365, ChronoUnit.DAYS),
                true,
                hashInteg
        );
        persistirQr(qr);
        auditLedger.appendSafely("QR_CODE_GERADO", RESOURCE_TYPE, expedicaoUuid, hashInteg, "QR Code gerado para mandado.");
        log.info("[QrCode] Gerado. expedicao={} tokenHash={}", expedicaoUuid, sha256Hex(token));
        return qr;
    }

    @Transactional
    public VerificacaoQrResult verificar(String token, String ipVerificador) {
        Objects.requireNonNull(token, "token");
        QrCodeMandado qr = consultarPorToken(token).orElse(null);
        if (qr == null || !qr.ativo() || qr.expiresAt().isBefore(Instant.now())) {
            auditLedger.appendSafely("QR_CODE_VERIFICACAO_INVALIDA", RESOURCE_TYPE, token.length() > 8 ? token.substring(0, 8) + "..." : token, sha256Hex(token + ipVerificador), "Token inválido, inativo ou expirado. IP: " + ipVerificador);
            return new VerificacaoQrResult(false, null, null, null, null, null, null, Instant.now(), ipVerificador, false);
        }
        boolean primeira = marcarPrimeiraVerificacao(token, qr);
        ExpedicaoJudicial expedicao = expedicaoRepository.findByExpedicaoUuid(qr.expedicaoUuid()).orElse(null);
        auditLedger.appendSafely(primeira ? "QR_CODE_PRIMEIRA_VERIFICACAO" : "QR_CODE_VERIFICACAO_SUBSEQUENTE", RESOURCE_TYPE, qr.expedicaoUuid(), sha256Hex(token + ipVerificador + Instant.now().toEpochMilli()), "Verificação de autenticidade de mandado. IP: " + ipVerificador);
        if (expedicao != null && primeira) {
            expedicao.registrarCienciaJudicial();
            expedicaoRepository.save(expedicao);
            WebhookOutboundService webhook = webhookProvider.getIfAvailable();
            if (webhook != null) {
                webhook.publicarEventoExpedicao(expedicao, WebhookOutboundService.EventoWebhook.CITACAO_EFETIVADA, Map.of("qrPrimeiraVerificacao", "true"));
            }
        }
        return new VerificacaoQrResult(
                true,
                qr.expedicaoUuid(),
                qr.processoId(),
                qr.numeroUnificado(),
                expedicao != null && expedicao.getTipoComunicacao() != null ? expedicao.getTipoComunicacao().getDescricao() : null,
                expedicao != null ? expedicao.getInstanciaExpedidora() : null,
                expedicao != null ? expedicao.getExpedidaEm() : null,
                Instant.now(),
                ipVerificador,
                primeira
        );
    }

    public Optional<QrCodeMandado> consultarPorExpedicao(String expedicaoUuid) {
        QrCodeMandado cache = qrCodesPorExpedicao.getIfPresent(expedicaoUuid);
        if (cache != null) {
            return Optional.of(cache);
        }
        Optional<QrCodeMandado> persisted = stateStore.find(DOMAIN_QR, expedicaoUuid, QrCodeMandado.class);
        persisted.ifPresent(this::cacheQr);
        return persisted;
    }



    private boolean marcarPrimeiraVerificacao(String token, QrCodeMandado qr) {
        if (tokensPrimeiraVerificacao.getIfPresent(token) != null || stateStore.exists(DOMAIN_QR_VERIFICACAO, token)) {
            tokensPrimeiraVerificacao.put(token, Boolean.TRUE);
            return false;
        }
        tokensPrimeiraVerificacao.put(token, Boolean.TRUE);
        stateStore.save(DOMAIN_QR_VERIFICACAO, token, qr.expedicaoUuid(), Map.of("token", token, "verificadaEm", Instant.now().toString()), qr.processoId(), qr.expedicaoUuid(), null, "PRIMEIRA");
        return true;
    }

    private Optional<QrCodeMandado> consultarPorToken(String token) {
        QrCodeMandado cache = qrCodesPorToken.getIfPresent(token);
        if (cache != null) {
            return Optional.of(cache);
        }
        return stateStore.findBySecondaryKey(DOMAIN_QR, token, QrCodeMandado.class).stream()
                .filter(qr -> token.equals(qr.tokenUnico()))
                .findFirst()
                .map(qr -> {
                    cacheQr(qr);
                    return qr;
                });
    }

    private void persistirQr(QrCodeMandado qr) {
        cacheQr(qr);
        stateStore.save(DOMAIN_QR, qr.expedicaoUuid(), qr.tokenUnico(), qr, qr.processoId(), qr.expedicaoUuid(), null, qr.ativo() ? "ATIVO" : "INATIVO");
    }

    private void cacheQr(QrCodeMandado qr) {
        qrCodesPorToken.put(qr.tokenUnico(), qr);
        qrCodesPorExpedicao.put(qr.expedicaoUuid(), qr);
    }

    private String gerarSvgSimbolico(String url) {
        String hash = sha256Hex(url).substring(0, 8);
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\">"
                + "<rect width=\"100\" height=\"100\" fill=\"white\"/>"
                + "<text x=\"10\" y=\"50\" font-size=\"8\" fill=\"black\">PJB QR " + hash + "</text>"
                + "<text x=\"10\" y=\"65\" font-size=\"5\" fill=\"gray\">" + url.substring(0, Math.min(40, url.length())) + "</text>"
                + "</svg>";
    }

    private static String sha256Hex(String raw) {
        return Hashes.sha256Hex(raw);
    }
}
