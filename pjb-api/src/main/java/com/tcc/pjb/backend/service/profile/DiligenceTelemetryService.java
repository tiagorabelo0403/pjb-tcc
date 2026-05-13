package com.tcc.pjb.backend.service.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryBatchSyncRequest;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryBatchSyncResponse;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetrySnapshotResponse;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryUpsertRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorTelemetria;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorTelemetriaRepository;

@Service
public class DiligenceTelemetryService {

    private static final HexFormat HEX = HexFormat.of();
    private static final Duration RECENT_WINDOW = Duration.ofHours(4);
    private static final Duration DEDUP_WINDOW = Duration.ofSeconds(90);
    private static final double DEDUP_DISTANCE_KM = 0.08d;
    private static final double EARTH_RADIUS_KM = 6371.0088d;

    private final CurrentUserService currentUserService;
    private final DiligenciaOperadorTelemetriaRepository repository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public DiligenceTelemetryService(CurrentUserService currentUserService,
                                     DiligenciaOperadorTelemetriaRepository repository,
                                     ObjectProvider<HttpServletRequest> requestProvider) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.repository = Objects.requireNonNull(repository);
        this.requestProvider = Objects.requireNonNull(requestProvider);
    }

    @Transactional
    public RouteTelemetrySnapshotResponse register(TelemetriaOperacionalCanal canal,
                                                   RouteTelemetryUpsertRequest request,
                                                   String deviceId) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (request == null || request.latitude() == null || request.longitude() == null) {
            throw new IllegalArgumentException("coordenadas_obrigatorias");
        }
        Usuario actor = currentUserService.getRequired();
        Instant capturedAt = request.capturadoEm() == null ? Instant.now() : request.capturadoEm();
        Optional<DiligenciaOperadorTelemetria> latest = repository.findTopByOperatorUserIdAndCanalOrderByCapturadoEmDesc(actor.getId(), canal);
        if (latest.isPresent() && shouldReuse(latest.get(), request, capturedAt, deviceId)) {
            return toResponse(actor, canal, latest.get(), true);
        }
        DiligenciaOperadorTelemetria entity = DiligenciaOperadorTelemetria.builder()
                .operatorUserId(actor.getId())
                .operatorTipoUsuario(actor.getTipoUsuario())
                .canal(canal)
                .deviceHash(hashDevice(deviceId))
                .latitude(request.latitude())
                .longitude(request.longitude())
                .precisaoMetros(sanitizeAccuracy(request.precisaoMetros()))
                .velocidadeKmh(sanitizeSpeed(request.velocidadeKmh()))
                .bateriaPercentual(sanitizeBattery(request.bateriaPercentual()))
                .fonte(normalizeSource(request.fonte()))
                .foreground(Boolean.TRUE.equals(request.foreground()))
                .capturadoEm(capturedAt)
                .requestId(RequestContext.getRequestId().orElse(null))
                .ip(resolveIp())
                .build();
        DiligenciaOperadorTelemetria saved = repository.save(entity);
        return toResponse(actor, canal, saved, false);
    }

    @Transactional
    public RouteTelemetryBatchSyncResponse registerBatch(TelemetriaOperacionalCanal canal,
                                                         RouteTelemetryBatchSyncRequest request,
                                                         String deviceId) {
        if (request == null || request.amostras() == null || request.amostras().isEmpty()) {
            throw new IllegalArgumentException("amostras_obrigatorias");
        }
        Usuario actor = currentUserService.getRequired();
        List<RouteTelemetrySnapshotResponse> snapshots = request.amostras().stream()
                .sorted(Comparator.comparing(RouteTelemetryUpsertRequest::capturadoEm, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(sample -> register(canal, sample, deviceId))
                .toList();
        int reused = (int) snapshots.stream().filter(RouteTelemetrySnapshotResponse::reaproveitada).count();
        Instant latestCapture = snapshots.stream().map(RouteTelemetrySnapshotResponse::capturadoEm).filter(Objects::nonNull).max(Instant::compareTo).orElse(null);
        return new RouteTelemetryBatchSyncResponse(
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                canal.name(),
                snapshots.size(),
                snapshots.size() - reused,
                reused,
                latestCapture,
                snapshots
        );
    }

    @Transactional(readOnly = true)
    public Optional<RouteTelemetrySnapshotResponse> latest(TelemetriaOperacionalCanal canal) {
        Usuario actor = currentUserService.getRequired();
        return repository.findTopByOperatorUserIdAndCanalOrderByCapturadoEmDesc(actor.getId(), canal)
                .map(entity -> toResponse(actor, canal, entity, false));
    }

    @Transactional(readOnly = true)
    public Optional<RouteOrigin> resolveRecentOriginForCurrentUser(TelemetriaOperacionalCanal canal) {
        return resolveRecentOriginForCurrentUser(canal, RECENT_WINDOW);
    }

    @Transactional(readOnly = true)
    public Optional<RouteOrigin> resolveRecentOriginForCurrentUser(TelemetriaOperacionalCanal canal, Duration maxAge) {
        Usuario actor = currentUserService.getRequired();
        Duration effectiveWindow = maxAge == null || maxAge.isNegative() || maxAge.isZero() ? RECENT_WINDOW : maxAge;
        Instant threshold = Instant.now().minus(effectiveWindow);
        return repository.findTopByOperatorUserIdAndCanalAndCapturadoEmAfterOrderByCapturadoEmDesc(actor.getId(), canal, threshold)
                .filter(this::usableForOrigin)
                .map(entity -> new RouteOrigin(entity.getLatitude(), entity.getLongitude(), canal.name() + "_TELEMETRIA", entity.getCapturadoEm(), entity.getPrecisaoMetros()));
    }

    @Transactional(readOnly = true)
    public List<RouteTelemetrySnapshotResponse> history(TelemetriaOperacionalCanal canal, int limit) {
        Usuario actor = currentUserService.getRequired();
        int size = Math.max(1, Math.min(limit, 50));
        return repository.findByOperatorUserIdAndCanalOrderByCapturadoEmDesc(actor.getId(), canal, PageRequest.of(0, size)).stream()
                .map(entity -> toResponse(actor, canal, entity, false))
                .toList();
    }

    private boolean shouldReuse(DiligenciaOperadorTelemetria latest,
                                RouteTelemetryUpsertRequest request,
                                Instant capturedAt,
                                String deviceId) {
        if (!Objects.equals(hashDevice(deviceId), latest.getDeviceHash())) {
            return false;
        }
        if (Duration.between(latest.getCapturadoEm(), capturedAt).abs().compareTo(DEDUP_WINDOW) > 0) {
            return false;
        }
        double distance = haversine(latest.getLatitude(), latest.getLongitude(), request.latitude(), request.longitude());
        return distance <= DEDUP_DISTANCE_KM;
    }

    private boolean usableForOrigin(DiligenciaOperadorTelemetria entity) {
        Double accuracy = entity.getPrecisaoMetros();
        return accuracy == null || accuracy <= 200d;
    }

    private RouteTelemetrySnapshotResponse toResponse(Usuario actor,
                                                      TelemetriaOperacionalCanal canal,
                                                      DiligenciaOperadorTelemetria entity,
                                                      boolean reused) {
        return new RouteTelemetrySnapshotResponse(
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                canal.name(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getPrecisaoMetros(),
                entity.getVelocidadeKmh(),
                entity.getBateriaPercentual(),
                entity.getFonte(),
                entity.isForeground(),
                entity.getDeviceHash() == null ? null : entity.getDeviceHash().substring(0, 12),
                reused,
                entity.getCapturadoEm(),
                entity.getCreatedAt()
        );
    }

    private String resolveIp() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return truncate(xff.split(",")[0].trim(), 80);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return truncate(realIp.trim(), 80);
        }
        return truncate(request.getRemoteAddr(), 80);
    }

    String hashDevice(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        return sha256(deviceId.trim());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("sha256_unavailable", ex);
        }
    }

    private Double sanitizeAccuracy(Double value) {
        if (value == null) {
            return null;
        }
        return Math.max(0d, Math.min(value, 10000d));
    }

    private Double sanitizeSpeed(Double value) {
        if (value == null) {
            return null;
        }
        return Math.max(0d, Math.min(value, 280d));
    }

    private Integer sanitizeBattery(Integer value) {
        if (value == null) {
            return null;
        }
        return Math.max(0, Math.min(value, 100));
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "GPS";
        }
        String normalized = source.trim().toUpperCase();
        return normalized.length() > 32 ? normalized.substring(0, 32) : normalized;
    }

    private String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2d), 2d)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2d), 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return EARTH_RADIUS_KM * c;
    }

    public record RouteOrigin(double latitude,
                              double longitude,
                              String source,
                              Instant capturedAt,
                              Double accuracyMeters) {
    }
}
