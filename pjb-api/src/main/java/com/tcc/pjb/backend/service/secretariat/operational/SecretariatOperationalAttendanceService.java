package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarSystemEvent;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.calendar.UserCalendarSystemEventRepository;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class SecretariatOperationalAttendanceService {

    private final AudienciaRepository audienciaRepository;
    private final UserCalendarSystemEventRepository systemEventRepository;

    public SecretariatOperationalAttendanceService(AudienciaRepository audienciaRepository,
                                                   UserCalendarSystemEventRepository systemEventRepository) {
        this.audienciaRepository = Objects.requireNonNull(audienciaRepository);
        this.systemEventRepository = Objects.requireNonNull(systemEventRepository);
    }

    @Transactional(readOnly = true)
    public AttendanceSnapshot avaliar(Processo processo,
                                      Usuario actor,
                                      SecretariatOperationalRoutingProfile routing,
                                      LocalDateTime inicio,
                                      Integer duracaoMinutos,
                                      String tipo,
                                      String local) {
        HearingWindow window = resolveWindow(processo, routing, inicio, duracaoMinutos, tipo, local);
        List<ParticipantPresence> participants = buildParticipants(processo, routing, window.hearingKey());
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Confirmação, sala de espera e presença operam no mesmo backbone de calendário sistêmico da secretaria.");
        fundamentos.add("Secretaria competente: " + routing.secretariatCode() + ".");
        fundamentos.add("Sala alvo: " + window.local() + ".");
        fundamentos.add("Janela de audiência: " + window.inicio() + " até " + window.fim() + '.');
        if (routing.conciliationPreferred()) {
            fundamentos.add("Fluxo conciliatório exige confirmação antecipada das partes e ordenação da sala de espera.");
        }
        if (routing.secrecyAware()) {
            fundamentos.add("Audiência sigilosa exige trilha de presença segregada e controle reforçado de admissão.");
        }
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("actorId", actor.getId());
        metrics.put("processoId", processo.getId());
        metrics.put("hearingKey", window.hearingKey());
        metrics.put("waitingRoomToken", routing.audienceInboxKey() + ":WAITING:" + window.hearingKey());
        metrics.put("confirmedCount", participants.stream().filter(p -> "CONFIRMADO".equals(p.status())).count());
        metrics.put("waitingCount", participants.stream().filter(p -> "SALA_ESPERA".equals(p.status())).count());
        metrics.put("presentCount", participants.stream().filter(p -> "PRESENTE".equals(p.status())).count());
        metrics.put("absentCount", participants.stream().filter(p -> "AUSENTE".equals(p.status())).count());
        metrics.put("supportCount", participants.stream().filter(p -> "SUPORTE_TECNICO".equals(p.status())).count());
        metrics.put("local", window.local());
        metrics.put("tipo", window.tipo());
        return new AttendanceSnapshot(window, List.copyOf(participants), List.copyOf(fundamentos), Map.copyOf(metrics));
    }

    @Transactional
    public AttendanceSnapshot registrar(Processo processo,
                                        Usuario actor,
                                        SecretariatOperationalRoutingProfile routing,
                                        LocalDateTime inicio,
                                        Integer duracaoMinutos,
                                        String tipo,
                                        String local,
                                        String papel,
                                        String nome,
                                        String situacao) {
        HearingWindow window = resolveWindow(processo, routing, inicio, duracaoMinutos, tipo, local);
        String participantRole = normalizeRole(papel);
        String participantName = normalizeName(nome, participantRole, processo);
        String status = normalizeStatus(situacao);
        Long syntheticUserId = syntheticParticipantUserId(window.hearingKey(), participantRole, participantName);
        String domainKey = "AUD:ATTENDANCE:" + window.hearingKey() + ':' + participantRole + ':' + slug(participantName);
        Instant now = Instant.now();
        UserCalendarSystemEvent event = systemEventRepository.findByUsuarioIdAndDomainKey(syntheticUserId, domainKey)
                .orElseGet(UserCalendarSystemEvent::new);
        event.setUsuarioId(syntheticUserId);
        event.setProcessoId(processo.getId());
        event.setDomainKey(domainKey);
        event.setEventType("AUDIENCIA_PRESENCA_SECRETARIA");
        event.setTitle(participantRole + " — " + participantName);
        event.setBody(String.join("\n",
                "status=" + status,
                "papel=" + participantRole,
                "nome=" + participantName,
                "hearingKey=" + window.hearingKey(),
                "local=" + window.local(),
                "updatedBy=" + actor.getId(),
                "updatedAt=" + now));
        event.setAt(window.inicio());
        event.setColor(colorForStatus(status));
        event.setDetailsUrl("/api/v1/secretaria/especializada/processos/" + processo.getId() + "/audiencias/presenca");
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(now);
        }
        event.setUpdatedAt(now);
        systemEventRepository.save(event);
        return avaliar(processo, actor, routing, inicio, duracaoMinutos, tipo, local);
    }

    private HearingWindow resolveWindow(Processo processo,
                                        SecretariatOperationalRoutingProfile routing,
                                        LocalDateTime inicio,
                                        Integer duracaoMinutos,
                                        String tipo,
                                        String local) {
        Optional<Audiencia> persisted = audienciaRepository.findTopByProcesso_IdOrderByDataHoraDesc(processo.getId());
        LocalDateTime effectiveStart = inicio;
        Integer effectiveDuration = duracaoMinutos;
        String effectiveType = tipo;
        String effectiveLocal = local;
        if (persisted.isPresent()) {
            Audiencia audiencia = persisted.get();
            if (effectiveStart == null) {
                effectiveStart = audiencia.getDataHora();
            }
            if (effectiveDuration == null || effectiveDuration <= 0) {
                effectiveDuration = audiencia.getDuracaoMin();
            }
            if (effectiveType == null || effectiveType.isBlank()) {
                effectiveType = audiencia.getTipo() == null ? null : audiencia.getTipo().name();
            }
            if (effectiveLocal == null || effectiveLocal.isBlank()) {
                effectiveLocal = audiencia.getLocal();
            }
        }
        if (effectiveStart == null) {
            effectiveStart = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        }
        int duration = effectiveDuration == null || effectiveDuration <= 0 ? routing.audienceDefaultDurationMinutes() : effectiveDuration;
        String resolvedType = effectiveType == null || effectiveType.isBlank() ? "AUDIENCIA_SECRETARIA" : effectiveType.trim().toUpperCase(Locale.ROOT);
        String resolvedLocal = effectiveLocal == null || effectiveLocal.isBlank() ? routing.hearingRoomPrefix() + "_SALA_01" : effectiveLocal.trim();
        LocalDateTime fim = effectiveStart.plusMinutes(duration);
        String hearingKey = processo.getId() + ":" + effectiveStart.truncatedTo(ChronoUnit.MINUTES).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new HearingWindow(hearingKey, effectiveStart, fim, resolvedType, resolvedLocal, duration);
    }

    private List<ParticipantPresence> buildParticipants(Processo processo,
                                                        SecretariatOperationalRoutingProfile routing,
                                                        String hearingKey) {
        List<ParticipantSeed> seeds = new ArrayList<>();
        seeds.add(new ParticipantSeed("SECRETARIA", firstNonBlank(routing.secretariatCode(), "SECRETARIA"), true));
        seeds.add(new ParticipantSeed("MAGISTRADO", resolveMagistrateLabel(processo), true));
        seeds.add(new ParticipantSeed("PARTE_AUTORA", firstNonBlank(processo.getParteAutoraNome(), "PARTE AUTORA"), true));
        seeds.add(new ParticipantSeed("PARTE_RE", firstNonBlank(processo.getParteReuNome(), "PARTE RÉ"), true));
        seeds.add(new ParticipantSeed("ADVOGADO_AUTOR", "DEFESA/AUTOR", false));
        seeds.add(new ParticipantSeed("ADVOGADO_REU", "DEFESA/RÉU", false));
        if (routing.conciliationPreferred()) {
            seeds.add(new ParticipantSeed("CONCILIADOR", "CONCILIADOR", false));
        }
        if (processo.getRamoDireito() != null && processo.getRamoDireito().exigeAtuacaoMP()) {
            seeds.add(new ParticipantSeed("MINISTERIO_PUBLICO", "MINISTÉRIO PÚBLICO", false));
        }
        if (routing.secrecyAware()) {
            seeds.add(new ParticipantSeed("SEGURANCA", "SUPORTE DE SEGURANÇA", false));
        }
        return seeds.stream()
                .map(seed -> toPresence(seed, hearingKey))
                .sorted(Comparator.comparingInt(ParticipantPresence::sortOrder).thenComparing(ParticipantPresence::papel).thenComparing(ParticipantPresence::nome))
                .toList();
    }

    private ParticipantPresence toPresence(ParticipantSeed seed, String hearingKey) {
        Long syntheticUserId = syntheticParticipantUserId(hearingKey, seed.role(), seed.name());
        String domainKey = "AUD:ATTENDANCE:" + hearingKey + ':' + seed.role() + ':' + slug(seed.name());
        UserCalendarSystemEvent event = systemEventRepository.findByUsuarioIdAndDomainKey(syntheticUserId, domainKey).orElse(null);
        String status = event == null ? (seed.required() ? "PENDENTE" : "NAO_APLICADO") : extractStatus(event.getBody());
        String color = event == null ? colorForStatus(status) : event.getColor();
        Instant updatedAt = event == null ? null : event.getUpdatedAt();
        int order = switch (status) {
            case "PRESENTE" -> 1;
            case "SALA_ESPERA" -> 2;
            case "CONFIRMADO" -> 3;
            case "SUPORTE_TECNICO" -> 4;
            case "AUSENTE" -> 5;
            default -> 6;
        };
        return new ParticipantPresence(seed.role(), seed.name(), seed.required(), status, color, updatedAt, order);
    }

    private String resolveMagistrateLabel(Processo processo) {
        if (processo.getVara() != null && !processo.getVara().isBlank()) {
            return "MAGISTRADO DA " + processo.getVara().trim().toUpperCase(Locale.ROOT);
        }
        return "MAGISTRADO DA UNIDADE";
    }

    private String normalizeRole(String papel) {
        if (papel == null || papel.isBlank()) {
            return "PARTE_AUTORA";
        }
        return slug(papel);
    }

    private String normalizeName(String nome, String role, Processo processo) {
        if (nome != null && !nome.isBlank()) {
            return nome.trim();
        }
        return switch (role) {
            case "PARTE_AUTORA" -> firstNonBlank(processo.getParteAutoraNome(), "PARTE AUTORA");
            case "PARTE_RE" -> firstNonBlank(processo.getParteReuNome(), "PARTE RÉ");
            case "SECRETARIA" -> "SECRETARIA";
            case "MAGISTRADO" -> resolveMagistrateLabel(processo);
            default -> role.replace('_', ' ');
        };
    }

    private String normalizeStatus(String situacao) {
        if (situacao == null || situacao.isBlank()) {
            return "CONFIRMADO";
        }
        String normalized = slug(situacao);
        return switch (normalized) {
            case "CONFIRMADO", "SALA_ESPERA", "PRESENTE", "AUSENTE", "SUPORTE_TECNICO", "REMARCACAO_SOLICITADA", "PENDENTE", "NAO_APLICADO" -> normalized;
            default -> "CONFIRMADO";
        };
    }

    private String extractStatus(String body) {
        if (body == null || body.isBlank()) {
            return "PENDENTE";
        }
        for (String line : body.split("\\R")) {
            if (line.startsWith("status=")) {
                return normalizeStatus(line.substring("status=".length()));
            }
        }
        return "PENDENTE";
    }

    private String colorForStatus(String status) {
        return switch (normalizeStatus(status)) {
            case "CONFIRMADO" -> "blue";
            case "SALA_ESPERA" -> "orange";
            case "PRESENTE" -> "green";
            case "AUSENTE" -> "red";
            case "SUPORTE_TECNICO" -> "purple";
            case "REMARCACAO_SOLICITADA" -> "yellow";
            case "PENDENTE" -> "gray";
            case "NAO_APLICADO" -> "secondary";
            default -> "gray";
        };
    }

    private long syntheticParticipantUserId(String hearingKey, String role, String name) {
        return -Math.abs((hearingKey + '|' + role + '|' + name).toUpperCase(Locale.ROOT).hashCode());
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String slug(String input) {
        if (input == null || input.isBlank()) {
            return "ITEM";
        }
        return input.trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    public record AttendanceSnapshot(
            HearingWindow window,
            List<ParticipantPresence> participants,
            List<String> fundamentos,
            Map<String, Object> metrics
    ) {
    }

    public record HearingWindow(
            String hearingKey,
            LocalDateTime inicio,
            LocalDateTime fim,
            String tipo,
            String local,
            Integer duracaoMinutos
    ) {
    }

    public record ParticipantPresence(
            String papel,
            String nome,
            boolean required,
            String status,
            String color,
            Instant updatedAt,
            int sortOrder
    ) {
    }

    private record ParticipantSeed(
            String role,
            String name,
            boolean required
    ) {
    }
}
