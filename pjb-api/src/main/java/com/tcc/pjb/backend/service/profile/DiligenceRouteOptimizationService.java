package com.tcc.pjb.backend.service.profile;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;

@Service
public class DiligenceRouteOptimizationService {

    private static final double EARTH_RADIUS_KM = 6371.0088d;
    private static final double AVERAGE_SPEED_KM_H = 32.0d;
    private static final Duration ROUTE_SCORING_TIMEOUT = Duration.ofSeconds(3);

    private final CurrentUserService currentUserService;
    private final PjbTimeService pjbTimeService;
    private final DiligenceTelemetryService telemetryService;
    private final ExecutorService ioExecutor;

    public DiligenceRouteOptimizationService(CurrentUserService currentUserService,
                                             PjbTimeService pjbTimeService,
                                             DiligenceTelemetryService telemetryService,
                                             @Qualifier("pjbIoExecutorService") ExecutorService ioExecutor) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.pjbTimeService = Objects.requireNonNull(pjbTimeService);
        this.telemetryService = Objects.requireNonNull(telemetryService);
        this.ioExecutor = Objects.requireNonNull(ioExecutor);
    }

    public DiligenceRouteOptimizationResponse optimize(DiligenceRouteOptimizationRequest request) {
        if (request == null || request.diligencias() == null || request.diligencias().isEmpty()) {
            throw new IllegalArgumentException("diligencias_obrigatorias");
        }
        Usuario actor = currentUserService.getRequired();
        List<Stop> pool = request.diligencias().stream().map(this::toStop).toList();
        List<String> warnings = new ArrayList<>();
        ResolvedOrigin origin = resolveOrigin(request, pool, actor, warnings);
        int stopMinutes = request.tempoMedioParadaMinutos() == null || request.tempoMedioParadaMinutos() < 1 ? 18 : Math.min(request.tempoMedioParadaMinutos(), 180);
        List<DiligenceRouteOptimizationResponse.OptimizedStop> route = new ArrayList<>();
        List<DiligenceRouteOptimizationResponse.DeferredStop> deferred = new ArrayList<>();
        List<Stop> pending = new ArrayList<>(pool);
        Coordinates current = origin.coordinates();
        double totalKm = 0d;
        long totalMinutes = 0L;
        Instant cursor = pjbTimeService.nowUtc();
        int ordem = 1;
        try {
            while (!pending.isEmpty()) {
                Coordinates pivot = current;
                Instant arrivalBase = cursor;
                List<Callable<ScoredStop>> tasks = pending.stream()
                        .map(stop -> (Callable<ScoredStop>) () -> scoreStop(pivot, stop, arrivalBase))
                        .toList();
                List<ScoredStop> scored = ioExecutor.invokeAll(tasks, ROUTE_SCORING_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).stream().map(future -> {
                    if (future.isCancelled()) {
                        return null;
                    }
                    try {
                        return future.get();
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                }).filter(Objects::nonNull).sorted(Comparator.comparingDouble(ScoredStop::score)).toList();
                if (scored.isEmpty()) {
                    throw new IllegalStateException("roteirizacao_timeout_controlado");
                }
                ScoredStop selected = scored.getFirst();
                Stop stop = selected.stop();
                long travelMinutes = Math.max(1L, Math.round(selected.distanceKm() / AVERAGE_SPEED_KM_H * 60d));
                Instant arrival = cursor.plus(travelMinutes, ChronoUnit.MINUTES);
                String classification = classify(arrival, stop.prazoFatalEm(), stop.janelaInicioHora(), stop.janelaFimHora());
                if ("INVIAVEL".equals(classification)) {
                    deferred.add(new DiligenceRouteOptimizationResponse.DeferredStop(stop.id(), stop.titulo(), "janela de prazo ou atendimento comprometida para a rota atual"));
                    pending.remove(stop);
                    warnings.add("Diligência adiada por risco operacional: " + stop.titulo());
                    continue;
                }
                route.add(new DiligenceRouteOptimizationResponse.OptimizedStop(
                        ordem++,
                        stop.id(),
                        stop.titulo(),
                        stop.endereco(),
                        stop.coordinates().latitude(),
                        stop.coordinates().longitude(),
                        stop.prioridade(),
                        round(selected.distanceKm()),
                        travelMinutes,
                        arrival,
                        classification
                ));
                totalKm += selected.distanceKm();
                totalMinutes += travelMinutes + stopMinutes;
                cursor = arrival.plus(stopMinutes, ChronoUnit.MINUTES);
                current = stop.coordinates();
                pending.remove(stop);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("roteirizacao_interrompida", ex);
        }
        return new DiligenceRouteOptimizationResponse(
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                new DiligenceRouteOptimizationResponse.OriginSnapshot(
                        origin.source(),
                        origin.coordinates().latitude(),
                        origin.coordinates().longitude(),
                        origin.capturedAt(),
                        origin.accuracyMeters()
                ),
                round(totalKm),
                totalMinutes,
                cursor,
                List.copyOf(warnings.stream().distinct().toList()),
                List.copyOf(route),
                List.copyOf(deferred),
                Instant.now()
        );
    }

    private ResolvedOrigin resolveOrigin(DiligenceRouteOptimizationRequest request,
                                         List<Stop> pool,
                                         Usuario actor,
                                         List<String> warnings) {
        if (request.origemLatitude() != null && request.origemLongitude() != null) {
            return new ResolvedOrigin(new Coordinates(request.origemLatitude(), request.origemLongitude()), "REQUEST", null, null);
        }
        TelemetriaOperacionalCanal canal = actor.getTipoUsuario() != null && actor.getTipoUsuario().isDelegadoOuAgente()
                ? TelemetriaOperacionalCanal.DELEGADO
                : TelemetriaOperacionalCanal.OFICIAL_JUSTICA;
        var telemetryOrigin = telemetryService.resolveRecentOriginForCurrentUser(canal).orElse(null);
        if (telemetryOrigin != null) {
            warnings.add("Origem operacional assumida a partir da telemetria recente do dispositivo.");
            return new ResolvedOrigin(new Coordinates(telemetryOrigin.latitude(), telemetryOrigin.longitude()), telemetryOrigin.source(), telemetryOrigin.capturedAt(), telemetryOrigin.accuracyMeters());
        }
        warnings.add("Origem da rota assumida pelo primeiro ponto elegível; telemetria recente indisponível.");
        return new ResolvedOrigin(pool.getFirst().coordinates(), "FIRST_STOP_FALLBACK", null, null);
    }

    private ScoredStop scoreStop(Coordinates origin, Stop stop, Instant cursor) {
        double distance = haversine(origin.latitude(), origin.longitude(), stop.coordinates().latitude(), stop.coordinates().longitude());
        long urgencyPenalty = urgencyPenalty(cursor, stop.prazoFatalEm(), stop.janelaInicioHora(), stop.janelaFimHora());
        int priorityWeight = Math.max(0, 6 - stop.prioridade()) * 3;
        double score = distance + urgencyPenalty - priorityWeight;
        return new ScoredStop(stop, score, distance);
    }

    private long urgencyPenalty(Instant cursor, Instant prazoFatalEm, Integer janelaInicioHora, Integer janelaFimHora) {
        long penalty = 0L;
        if (prazoFatalEm == null) {
            penalty += 25L;
        } else {
            long minutes = ChronoUnit.MINUTES.between(cursor, prazoFatalEm);
            if (minutes <= 0) {
                penalty -= 500L;
            } else if (minutes <= 240) {
                penalty -= 250L;
            } else if (minutes <= 720) {
                penalty -= 120L;
            } else if (minutes <= 1440) {
                penalty -= 60L;
            }
        }
        if (janelaInicioHora != null || janelaFimHora != null) {
            int hour = pjbTimeService.nowUtc().atZone(pjbTimeService.legalZone()).getHour();
            if (janelaInicioHora != null && hour < janelaInicioHora) {
                penalty += (janelaInicioHora - hour) * 6L;
            }
            if (janelaFimHora != null && hour > janelaFimHora) {
                penalty += 220L;
            }
        }
        return penalty;
    }

    private Stop toStop(DiligenceRouteOptimizationRequest.StopInput input) {
        if (input == null) {
            throw new IllegalArgumentException("diligencia_nula");
        }
        String id = hasText(input.id()) ? input.id().trim() : "DIL-" + Math.abs(Objects.hash(input.titulo(), input.endereco(), input.prazoFatalEm()));
        String titulo = hasText(input.titulo()) ? input.titulo().trim() : "Diligência sem título";
        String endereco = hasText(input.endereco()) ? input.endereco().trim() : "endereco_nao_informado";
        Coordinates coordinates = resolveCoordinates(input);
        int prioridade = input.prioridade() == null ? 3 : Math.max(1, Math.min(input.prioridade(), 5));
        Integer janelaInicio = normalizeHour(input.janelaInicioHora());
        Integer janelaFim = normalizeHour(input.janelaFimHora());
        return new Stop(id, titulo, endereco, coordinates, prioridade, input.prazoFatalEm(), janelaInicio, janelaFim);
    }

    private Integer normalizeHour(Integer value) {
        if (value == null) {
            return null;
        }
        return Math.max(0, Math.min(value, 23));
    }

    private Coordinates resolveCoordinates(DiligenceRouteOptimizationRequest.StopInput input) {
        if (input.latitude() != null && input.longitude() != null) {
            return new Coordinates(input.latitude(), input.longitude());
        }
        double seed = Math.abs(Objects.hashCode(input.endereco()) % 10000) / 10000d;
        double latitude = -4.0d + seed;
        double longitude = -38.8d + (seed / 2d);
        return new Coordinates(latitude, longitude);
    }

    private String classify(Instant arrival, Instant prazoFatalEm, Integer janelaInicioHora, Integer janelaFimHora) {
        int arrivalHour = arrival.atZone(pjbTimeService.legalZone()).getHour();
        if (janelaFimHora != null && arrivalHour > janelaFimHora) {
            return "INVIAVEL";
        }
        if (prazoFatalEm == null) {
            if (janelaInicioHora != null && arrivalHour < janelaInicioHora) {
                return "PRIORIDADE_ELEVADA";
            }
            return "NORMAL";
        }
        long minutes = ChronoUnit.MINUTES.between(arrival, prazoFatalEm);
        if (minutes < 0) {
            return "INVIAVEL";
        }
        if (minutes <= 120) {
            return "JANELA_CRITICA";
        }
        if (minutes <= 480 || (janelaInicioHora != null && arrivalHour < janelaInicioHora)) {
            return "PRIORIDADE_ELEVADA";
        }
        return "NORMAL";
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2d), 2d)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2d), 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return EARTH_RADIUS_KM * c;
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record Coordinates(double latitude, double longitude) {
    }

    private record Stop(String id,
                        String titulo,
                        String endereco,
                        Coordinates coordinates,
                        int prioridade,
                        Instant prazoFatalEm,
                        Integer janelaInicioHora,
                        Integer janelaFimHora) {
    }

    private record ScoredStop(Stop stop, double score, double distanceKm) {
    }

    private record ResolvedOrigin(Coordinates coordinates,
                                  String source,
                                  Instant capturedAt,
                                  Double accuracyMeters) {
    }
}
