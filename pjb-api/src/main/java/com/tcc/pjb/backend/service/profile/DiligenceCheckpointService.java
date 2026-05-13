package com.tcc.pjb.backend.service.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceArrivalCheckpointRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCheckpointResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCheckpointTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;

@Service
public class DiligenceCheckpointService {

    private static final double EARTH_RADIUS_METERS = 6371008.8d;
    private static final HexFormat HEX = HexFormat.of();

    private final CurrentUserService currentUserService;
    private final DiligenceTelemetryService telemetryService;
    private final DiligenciaOperadorCheckpointEventoRepository repository;
    private final DiligenceReferenceResolverService referenceResolverService;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public DiligenceCheckpointService(CurrentUserService currentUserService,
                                      DiligenceTelemetryService telemetryService,
                                      DiligenciaOperadorCheckpointEventoRepository repository,
                                      DiligenceReferenceResolverService referenceResolverService,
                                      ObjectProvider<HttpServletRequest> requestProvider) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.telemetryService = Objects.requireNonNull(telemetryService);
        this.repository = Objects.requireNonNull(repository);
        this.referenceResolverService = Objects.requireNonNull(referenceResolverService);
        this.requestProvider = Objects.requireNonNull(requestProvider);
    }

    @Transactional
    public DiligenceCheckpointResponse registerArrival(TelemetriaOperacionalCanal canal,
                                                       String diligenceReference,
                                                       DiligenceArrivalCheckpointRequest request,
                                                       String deviceId) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        if (request == null || request.destinoLatitude() == null || request.destinoLongitude() == null) {
            throw new IllegalArgumentException("destino_obrigatorio");
        }
        Usuario actor = currentUserService.getRequired();
        String normalizedReference = diligenceReference.trim();
        ObservedPoint observed = resolveObservedPoint(canal, request);
        double radius = normalizeRadius(request.raioMetros());
        double distanceMeters = haversineMeters(request.destinoLatitude(), request.destinoLongitude(), observed.latitude(), observed.longitude());
        boolean inside = distanceMeters <= radius;
        String classification = classify(distanceMeters, radius);
        Instant occurredAt = request.capturadoEm() == null ? Instant.now() : request.capturadoEm();
        int tentativaSequencia = Math.toIntExact(repository.countByOperatorUserIdAndCanalAndDiligenceReference(actor.getId(), canal, normalizedReference) + 1L);
        DiligenceReferenceResolverService.ResolvedDiligenceReference resolved = referenceResolverService.resolve(canal, normalizedReference).orElse(null);
        String locationSignature = sha256(String.join("|",
                canal.name(),
                normalizedReference,
                actor.getId() == null ? "0" : actor.getId().toString(),
                Double.toString(round(request.destinoLatitude())),
                Double.toString(round(request.destinoLongitude())),
                Double.toString(round(observed.latitude())),
                Double.toString(round(observed.longitude())),
                Double.toString(round(distanceMeters)),
                occurredAt.toString(),
                telemetryService.hashDevice(deviceId) == null ? "-" : telemetryService.hashDevice(deviceId)
        ));
        DiligenciaOperadorCheckpointEvento entity = DiligenciaOperadorCheckpointEvento.builder()
                .operatorUserId(actor.getId())
                .operatorTipoUsuario(actor.getTipoUsuario())
                .canal(canal)
                .diligenceReference(normalizedReference)
                .checkpointTipo(DiligenciaCheckpointTipo.CHEGADA)
                .targetLatitude(request.destinoLatitude())
                .targetLongitude(request.destinoLongitude())
                .observedLatitude(observed.latitude())
                .observedLongitude(observed.longitude())
                .distanceMeters(round(distanceMeters))
                .geofenceRadiusMeters(round(radius))
                .insideGeofence(inside)
                .classification(classification)
                .source(observed.source())
                .deviceHash(telemetryService.hashDevice(deviceId))
                .workItemId(resolved != null ? resolved.workItemId() : null)
                .processoId(resolved != null ? resolved.processoId() : null)
                .processoNumero(resolved != null ? resolved.processoNumero() : null)
                .workItemTemplateCode(resolved != null ? resolved.templateCode() : null)
                .workItemType(resolved != null ? resolved.workItemType() : null)
                .workItemStatus(resolved != null ? resolved.workItemStatus() : null)
                .tentativaSequencia(tentativaSequencia)
                .locationSignatureSha256(locationSignature)
                .occurredAt(occurredAt)
                .requestId(RequestContext.getRequestId().orElse(null))
                .ip(resolveIp())
                .build();
        DiligenciaOperadorCheckpointEvento saved = repository.save(entity);
        return toResponse(actor, saved);
    }

    @Transactional(readOnly = true)
    public List<DiligenceCheckpointResponse> history(TelemetriaOperacionalCanal canal,
                                                     String diligenceReference,
                                                     int limit) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        return repository.findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(actor.getId(), canal, diligenceReference.trim()).stream()
                .limit(Math.max(1, Math.min(limit, 50)))
                .map(entry -> toResponse(actor, entry))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<DiligenciaOperadorCheckpointEvento> latestEntity(TelemetriaOperacionalCanal canal,
                                                                     String diligenceReference) {
        Usuario actor = currentUserService.getRequired();
        return repository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(actor.getId(), canal, diligenceReference.trim());
    }

    private ObservedPoint resolveObservedPoint(TelemetriaOperacionalCanal canal,
                                               DiligenceArrivalCheckpointRequest request) {
        if (request.observadaLatitude() != null && request.observadaLongitude() != null) {
            return new ObservedPoint(request.observadaLatitude(), request.observadaLongitude(), normalizeSource(request.fonte()));
        }
        Optional<DiligenceTelemetryService.RouteOrigin> recent = telemetryService.resolveRecentOriginForCurrentUser(canal, Duration.ofMinutes(20));
        if (recent.isPresent()) {
            DiligenceTelemetryService.RouteOrigin point = recent.get();
            return new ObservedPoint(point.latitude(), point.longitude(), point.source());
        }
        throw new IllegalArgumentException("coordenadas_observadas_ou_telemetria_recente_obrigatorias");
    }

    private DiligenceCheckpointResponse toResponse(Usuario actor, DiligenciaOperadorCheckpointEvento entry) {
        return new DiligenceCheckpointResponse(
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                entry.getCanal().name(),
                entry.getDiligenceReference(),
                entry.getCheckpointTipo().name(),
                entry.getTargetLatitude(),
                entry.getTargetLongitude(),
                entry.getObservedLatitude(),
                entry.getObservedLongitude(),
                entry.getDistanceMeters(),
                entry.getGeofenceRadiusMeters(),
                entry.isInsideGeofence(),
                entry.getClassification(),
                entry.getSource(),
                entry.getWorkItemId(),
                entry.getProcessoId(),
                entry.getProcessoNumero(),
                entry.getTentativaSequencia(),
                entry.getLocationSignatureSha256(),
                entry.getOccurredAt(),
                entry.getCreatedAt()
        );
    }

    private String classify(double distanceMeters, double radiusMeters) {
        int bucket = distanceMeters <= radiusMeters ? 0 : distanceMeters <= radiusMeters * 1.75d ? 1 : 2;
        return switch (bucket) {
            case 0 -> "CHEGADA_CONFIRMADA";
            case 1 -> "PROXIMIDADE_OPERACIONAL";
            default -> "FORA_DA_CERCA";
        };
    }

    private double normalizeRadius(Double value) {
        double radius = value == null ? 120d : value;
        return Math.max(10d, Math.min(radius, 2000d));
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

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "REQUEST";
        }
        String normalized = source.trim().toUpperCase();
        return normalized.length() <= 40 ? normalized : normalized.substring(0, 40);
    }

    private String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2d), 2d)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2d), 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return EARTH_RADIUS_METERS * c;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("checkpoint_signature_unavailable", ex);
        }
    }

    private record ObservedPoint(double latitude, double longitude, String source) {
    }
}
