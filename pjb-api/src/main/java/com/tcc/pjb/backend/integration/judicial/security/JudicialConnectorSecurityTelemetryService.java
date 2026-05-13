package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorSecurityTelemetryService {

    private static final int MAX_FAILURE_COUNTERS = 256;
    private static final int MAX_HANDSHAKE_TIMERS = 128;
    private static final int MAX_CERTIFICATE_COUNTERS = 128;
    private static final int MAX_SIGNATURE_COUNTERS = 128;
    private static final int MAX_TRIBUNAL_TAGS = 96;
    private static final int MAX_OPERATION_TAGS = 64;
    private static final int MAX_OUTCOME_TAGS = 32;

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> failureCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> certificateCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> signatureCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> handshakeTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> tribunalTags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> operationTags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> outcomeTags = new ConcurrentHashMap<>();

    public JudicialConnectorSecurityTelemetryService(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
    }

    public void recordCryptographicFailure(JudicialSystem system,
                                           String tribunalCodigo,
                                           JudicialConnectorCryptographicFailureType failureType,
                                           String operationName) {
        String tribunal = tribunalTag(tribunalCodigo);
        String operation = operationTag(operationName);
        String failure = failureType != null ? failureType.name() : JudicialConnectorCryptographicFailureType.UNCLASSIFIED_CRYPTOGRAPHIC_FAILURE.name();
        String key = nameOf(system) + '|' + tribunal + '|' + failure + '|' + operation;
        failureCounter(key, system, tribunal, failure, operation).increment();
    }

    public void recordHandshake(JudicialSystem system,
                                String tribunalCodigo,
                                String outcome,
                                Duration duration) {
        String tribunal = tribunalTag(tribunalCodigo);
        String normalizedOutcome = outcomeTag(outcome);
        String key = nameOf(system) + '|' + tribunal + '|' + normalizedOutcome;
        handshakeTimer(key, system, tribunal, normalizedOutcome).record(duration == null ? Duration.ZERO : duration);
    }

    public void recordCertificateValidation(JudicialSystem system,
                                            String tribunalCodigo,
                                            String status,
                                            boolean hardwareBacked,
                                            boolean revocationAttempted) {
        String tribunal = tribunalTag(tribunalCodigo);
        String normalizedStatus = outcomeTag(status);
        String key = nameOf(system) + '|' + tribunal + '|' + normalizedStatus + '|' + hardwareBacked + '|' + revocationAttempted;
        certificateCounter(key, system, tribunal, normalizedStatus, hardwareBacked, revocationAttempted).increment();
    }

    public void recordSignature(JudicialSystem system,
                                String tribunalCodigo,
                                String outcome,
                                boolean hardwareBacked) {
        String tribunal = tribunalTag(tribunalCodigo);
        String normalizedOutcome = outcomeTag(outcome);
        String key = nameOf(system) + '|' + tribunal + '|' + normalizedOutcome + '|' + hardwareBacked;
        signatureCounter(key, system, tribunal, normalizedOutcome, hardwareBacked).increment();
    }

    private Counter failureCounter(String key,
                                   JudicialSystem system,
                                   String tribunal,
                                   String failure,
                                   String operation) {
        Counter existing = failureCounters.get(key);
        if (existing != null) {
            return existing;
        }
        if (failureCounters.size() >= MAX_FAILURE_COUNTERS) {
            return failureCounters.computeIfAbsent("OVERFLOW", ignored -> Counter.builder("pjb.judicial.security.failure.total")
                    .tag("system", JudicialSystem.OUTRO.name())
                    .tag("tribunal", "OTHER")
                    .tag("failureType", "OTHER")
                    .tag("operation", "OTHER")
                    .register(meterRegistry));
        }
        return failureCounters.computeIfAbsent(key, ignored -> Counter.builder("pjb.judicial.security.failure.total")
                .tag("system", nameOf(system))
                .tag("tribunal", tribunal)
                .tag("failureType", failure)
                .tag("operation", operation)
                .register(meterRegistry));
    }

    private Timer handshakeTimer(String key,
                                 JudicialSystem system,
                                 String tribunal,
                                 String outcome) {
        Timer existing = handshakeTimers.get(key);
        if (existing != null) {
            return existing;
        }
        if (handshakeTimers.size() >= MAX_HANDSHAKE_TIMERS) {
            return handshakeTimers.computeIfAbsent("OVERFLOW", ignored -> Timer.builder("pjb.judicial.security.handshake.duration")
                    .tag("system", JudicialSystem.OUTRO.name())
                    .tag("tribunal", "OTHER")
                    .tag("outcome", "OTHER")
                    .register(meterRegistry));
        }
        return handshakeTimers.computeIfAbsent(key, ignored -> Timer.builder("pjb.judicial.security.handshake.duration")
                .tag("system", nameOf(system))
                .tag("tribunal", tribunal)
                .tag("outcome", outcome)
                .register(meterRegistry));
    }

    private Counter certificateCounter(String key,
                                       JudicialSystem system,
                                       String tribunal,
                                       String status,
                                       boolean hardwareBacked,
                                       boolean revocationAttempted) {
        Counter existing = certificateCounters.get(key);
        if (existing != null) {
            return existing;
        }
        if (certificateCounters.size() >= MAX_CERTIFICATE_COUNTERS) {
            return certificateCounters.computeIfAbsent("OVERFLOW", ignored -> Counter.builder("pjb.judicial.security.certificate.validation.total")
                    .tag("system", JudicialSystem.OUTRO.name())
                    .tag("tribunal", "OTHER")
                    .tag("status", "OTHER")
                    .tag("hardwareBacked", "other")
                    .tag("revocationAttempted", "other")
                    .register(meterRegistry));
        }
        return certificateCounters.computeIfAbsent(key, ignored -> Counter.builder("pjb.judicial.security.certificate.validation.total")
                .tag("system", nameOf(system))
                .tag("tribunal", tribunal)
                .tag("status", status)
                .tag("hardwareBacked", Boolean.toString(hardwareBacked))
                .tag("revocationAttempted", Boolean.toString(revocationAttempted))
                .register(meterRegistry));
    }

    private Counter signatureCounter(String key,
                                     JudicialSystem system,
                                     String tribunal,
                                     String outcome,
                                     boolean hardwareBacked) {
        Counter existing = signatureCounters.get(key);
        if (existing != null) {
            return existing;
        }
        if (signatureCounters.size() >= MAX_SIGNATURE_COUNTERS) {
            return signatureCounters.computeIfAbsent("OVERFLOW", ignored -> Counter.builder("pjb.judicial.security.signature.total")
                    .tag("system", JudicialSystem.OUTRO.name())
                    .tag("tribunal", "OTHER")
                    .tag("outcome", "OTHER")
                    .tag("hardwareBacked", "other")
                    .register(meterRegistry));
        }
        return signatureCounters.computeIfAbsent(key, ignored -> Counter.builder("pjb.judicial.security.signature.total")
                .tag("system", nameOf(system))
                .tag("tribunal", tribunal)
                .tag("outcome", outcome)
                .tag("hardwareBacked", Boolean.toString(hardwareBacked))
                .register(meterRegistry));
    }

    private String tribunalTag(String value) {
        return boundedTag(tribunalTags, normalize(value), MAX_TRIBUNAL_TAGS);
    }

    private String operationTag(String value) {
        return boundedTag(operationTags, normalize(value), MAX_OPERATION_TAGS);
    }

    private String outcomeTag(String value) {
        return boundedTag(outcomeTags, normalize(value), MAX_OUTCOME_TAGS);
    }

    private String nameOf(JudicialSystem system) {
        return system == null ? JudicialSystem.OUTRO.name() : system.name();
    }

    private static String boundedTag(ConcurrentHashMap<String, Boolean> seen, String value, int max) {
        if (seen.containsKey(value)) {
            return value;
        }
        if (seen.size() >= max) {
            return "OTHER";
        }
        seen.putIfAbsent(value, Boolean.TRUE);
        return seen.size() > max ? "OTHER" : value;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "UNSPECIFIED";
        }
        String normalized = value.trim().toUpperCase().replaceAll("[^A-Z0-9_.:-]", "_");
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }
}
