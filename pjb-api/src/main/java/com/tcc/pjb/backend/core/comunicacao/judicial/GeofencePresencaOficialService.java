package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;

@Service
public class GeofencePresencaOficialService {

    private static final Logger log = LoggerFactory.getLogger(GeofencePresencaOficialService.class);
    private static final String RESOURCE_TYPE = "GEOFENCE_OFICIAL";
    private static final String DOMAIN_ENDERECO = "GEOFENCE_ENDERECO_ALVO";
    private static final double RAIO_PADRAO_METROS = 50.0;
    private static final double RAIO_ZONA_RURAL_METROS = 150.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public enum ResultadoGeofence {
        DENTRO_DO_RAIO,
        FORA_DO_RAIO,
        COORDENADAS_INVALIDAS,
        ENDERECO_NAO_GEOCODIFICADO
    }

    public record EnderecoAlvo(
            String mandadoId,
            String enderecoCompleto,
            double latitudeAlvo,
            double longitudeAlvo,
            boolean zonaRural,
            double raioPersonalizadoMetros
    ) {
        double raioEfetivo() {
            if (raioPersonalizadoMetros > 0) {
                return raioPersonalizadoMetros;
            }
            return zonaRural ? RAIO_ZONA_RURAL_METROS : RAIO_PADRAO_METROS;
        }
    }

    public record ValidacaoPresenca(
            String validacaoUuid,
            String mandadoId,
            Long oficialId,
            ResultadoGeofence resultado,
            double latitudeOficial,
            double longitudeOficial,
            double latitudeAlvo,
            double longitudeAlvo,
            double distanciaMetros,
            double raioPermitidoMetros,
            Instant validadaEm,
            boolean liberadoParaRegistro,
            String hashIntegridade
    ) {
    }

    private final AuditLedgerService auditLedger;
    private final ComunicacaoJudicialStateStore stateStore;
    private final Cache<String, EnderecoAlvo> enderecosPorMandado = Caffeine.newBuilder()
            .maximumSize(20000)
            .expireAfterAccess(Duration.ofHours(12))
            .build();

    public GeofencePresencaOficialService(AuditLedgerService auditLedger,
                                          ComunicacaoJudicialStateStore stateStore) {
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    }

    public void registrarEnderecoAlvo(EnderecoAlvo endereco) {
        Objects.requireNonNull(endereco, "endereco");
        if (endereco.latitudeAlvo() == 0.0 && endereco.longitudeAlvo() == 0.0) {
            throw new IllegalArgumentException("Endereço alvo sem coordenadas GPS válidas: " + endereco.mandadoId());
        }
        persistirEndereco(endereco);
        log.debug(
                "[Geofence] Endereço alvo registrado. mandado={} lat={} lon={}",
                endereco.mandadoId(),
                endereco.latitudeAlvo(),
                endereco.longitudeAlvo()
        );
    }

    public ValidacaoPresenca validar(String mandadoId, Long oficialId, double latOficial, double lonOficial) {
        Objects.requireNonNull(mandadoId, "mandadoId");
        if (!coordenadasValidas(latOficial, lonOficial)) {
            return buildResultado(mandadoId, oficialId, latOficial, lonOficial, 0, 0, ResultadoGeofence.COORDENADAS_INVALIDAS, false);
        }
        EnderecoAlvo alvo = consultarEnderecoAlvo(mandadoId).orElse(null);
        if (alvo == null) {
            log.warn("[Geofence] Endereço alvo não encontrado para mandado={}. Liberando com aviso.", mandadoId);
            return buildResultado(
                    mandadoId,
                    oficialId,
                    latOficial,
                    lonOficial,
                    0,
                    RAIO_PADRAO_METROS,
                    ResultadoGeofence.ENDERECO_NAO_GEOCODIFICADO,
                    true
            );
        }
        double distancia = haversineMetros(latOficial, lonOficial, alvo.latitudeAlvo(), alvo.longitudeAlvo());
        double raio = alvo.raioEfetivo();
        boolean dentro = distancia <= raio;
        ResultadoGeofence resultado = dentro ? ResultadoGeofence.DENTRO_DO_RAIO : ResultadoGeofence.FORA_DO_RAIO;
        ValidacaoPresenca validacao = buildResultado(mandadoId, oficialId, latOficial, lonOficial, distancia, raio, resultado, dentro);
        auditLedger.appendSafely(
                dentro ? "GEOFENCE_DENTRO_RAIO" : "GEOFENCE_FORA_RAIO",
                RESOURCE_TYPE,
                mandadoId,
                validacao.hashIntegridade(),
                "Oficial=%d dist=%.1fm raio=%.1fm liberado=%s".formatted(oficialId, distancia, raio, dentro)
        );
        if (!dentro) {
            log.warn(
                    "[Geofence] Oficial fora do raio. mandado={} oficial={} dist={}m raio={}m",
                    mandadoId,
                    oficialId,
                    String.format(java.util.Locale.ROOT, "%.1f", distancia),
                    String.format(java.util.Locale.ROOT, "%.1f", raio)
            );
        }
        return validacao;
    }

    public boolean isLiberadoParaRegistro(String mandadoId, Long oficialId, double lat, double lon) {
        return validar(mandadoId, oficialId, lat, lon).liberadoParaRegistro();
    }

    public Optional<EnderecoAlvo> consultarEnderecoAlvo(String mandadoId) {
        EnderecoAlvo cache = enderecosPorMandado.getIfPresent(mandadoId);
        if (cache != null) {
            return Optional.of(cache);
        }
        Optional<EnderecoAlvo> persisted = stateStore.find(DOMAIN_ENDERECO, mandadoId, EnderecoAlvo.class);
        persisted.ifPresent(endereco -> enderecosPorMandado.put(mandadoId, endereco));
        return persisted;
    }



    private void persistirEndereco(EnderecoAlvo endereco) {
        enderecosPorMandado.put(endereco.mandadoId(), endereco);
        stateStore.save(DOMAIN_ENDERECO, endereco.mandadoId(), endereco.enderecoCompleto(), endereco, null, null, endereco.mandadoId(), null);
    }

    private ValidacaoPresenca buildResultado(String mandadoId,
                                             Long oficialId,
                                             double latOf,
                                             double lonOf,
                                             double distancia,
                                             double raio,
                                             ResultadoGeofence resultado,
                                             boolean liberado) {
        EnderecoAlvo alvo = consultarEnderecoAlvo(mandadoId).orElse(null);
        double latAlvo = alvo != null ? alvo.latitudeAlvo() : 0;
        double lonAlvo = alvo != null ? alvo.longitudeAlvo() : 0;
        String uuid = UUID.randomUUID().toString();
        String hash = sha256Hex(uuid + mandadoId + oficialId + latOf + lonOf + Instant.now().toEpochMilli());
        return new ValidacaoPresenca(
                uuid,
                mandadoId,
                oficialId,
                resultado,
                latOf,
                lonOf,
                latAlvo,
                lonAlvo,
                distancia,
                raio,
                Instant.now(),
                liberado,
                hash
        );
    }

    private static double haversineMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private static boolean coordenadasValidas(double lat, double lon) {
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0 && !(lat == 0.0 && lon == 0.0);
    }

    private static String sha256Hex(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(raw));
        }
    }
}
