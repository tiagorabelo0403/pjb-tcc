package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarSystemEvent;
import com.tcc.pjb.backend.model.repository.calendar.UserCalendarSystemEventRepository;
import com.tcc.pjb.backend.service.processual.pauta.PautaAudienciaNacionalService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class SecretariatOperationalHearingResourceService {

    private final UserCalendarSystemEventRepository systemEventRepository;

    public SecretariatOperationalHearingResourceService(UserCalendarSystemEventRepository systemEventRepository) {
        this.systemEventRepository = Objects.requireNonNull(systemEventRepository);
    }

    @Transactional(readOnly = true)
    public HearingResourceSnapshot avaliar(Processo processo,
                                           Usuario actor,
                                           SecretariatOperationalRoutingProfile routing,
                                           PautaAudienciaNacionalService.PautaAudienciaDecision pauta,
                                           String preferredLocal) {
        LocalDateTime inicio = pauta.inicio();
        LocalDateTime fim = pauta.fim();
        List<ResourceCandidate> candidates = buildCandidates(routing, preferredLocal, inicio, fim);
        ResourceCandidate selected = candidates.stream().filter(candidate -> candidate.conflicts().isEmpty()).findFirst().orElse(candidates.getFirst());
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Reserva de sala e recurso calculada sobre o mesmo backbone de calendário sistêmico da plataforma.");
        fundamentos.add("Secretaria: " + routing.secretariatCode() + ", recurso selecionado: " + selected.resourceCode() + '.');
        fundamentos.add("Janela de reserva: " + inicio + " até " + fim + '.');
        if (routing.secrecyAware()) {
            fundamentos.add("Audiência sensível exige recurso segregado por sigilo reforçado.");
        }
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("candidateCount", candidates.size());
        metrics.put("selectedType", selected.resourceType());
        metrics.put("selectedCode", selected.resourceCode());
        metrics.put("supportsPhysicalRoom", routing.supportsPhysicalRoom());
        metrics.put("supportsVirtualRoom", routing.supportsVirtualRoom());
        metrics.put("hearingRoomPrefix", routing.hearingRoomPrefix());
        metrics.put("actorId", actor.getId());
        metrics.put("processoId", processo.getId());
        return new HearingResourceSnapshot(selected, List.copyOf(candidates), List.copyOf(fundamentos), Map.copyOf(metrics));
    }

    @Transactional
    public HearingResourceSnapshot reservar(Processo processo,
                                            Usuario actor,
                                            SecretariatOperationalRoutingProfile routing,
                                            PautaAudienciaNacionalService.PautaAudienciaDecision pauta,
                                            String preferredLocal) {
        HearingResourceSnapshot snapshot = avaliar(processo, actor, routing, pauta, preferredLocal);
        ResourceCandidate selected = snapshot.selected();
        Instant now = Instant.now();
        String domainKey = "AUD:RESOURCE:" + selected.resourceCode() + ':' + processo.getId() + ':' + pauta.inicio().truncatedTo(ChronoUnit.MINUTES);
        UserCalendarSystemEvent event = systemEventRepository.findByUsuarioIdAndDomainKey(selected.resourceUserId(), domainKey)
                .orElseGet(UserCalendarSystemEvent::new);
        event.setUsuarioId(selected.resourceUserId());
        event.setProcessoId(processo.getId());
        event.setDomainKey(domainKey);
        event.setEventType("AUDIENCIA_RECURSO_SECRETARIA");
        event.setTitle(selected.resourceCode());
        event.setBody("Reserva de recurso da secretaria " + routing.secretariatCode() + " para o processo " + processo.getId() + " @ " + pauta.inicio());
        event.setAt(pauta.inicio());
        event.setColor("secondary");
        event.setDetailsUrl("/api/v1/secretariat/especializada/processos/" + processo.getId() + "/audiencias/recursos");
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(now);
        }
        event.setUpdatedAt(now);
        systemEventRepository.save(event);
        List<String> fundamentos = new ArrayList<>(snapshot.fundamentos());
        fundamentos.add("Recurso reservado no calendário sistêmico com chave " + domainKey + '.');
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>(snapshot.metrics());
        metrics.put("reserved", true);
        metrics.put("domainKey", domainKey);
        return new HearingResourceSnapshot(selected, snapshot.candidates(), List.copyOf(fundamentos), Map.copyOf(metrics));
    }

    private List<ResourceCandidate> buildCandidates(SecretariatOperationalRoutingProfile routing,
                                                    String preferredLocal,
                                                    LocalDateTime inicio,
                                                    LocalDateTime fim) {
        List<String> codes = new ArrayList<>();
        if (preferredLocal != null && !preferredLocal.isBlank()) {
            codes.add(preferredLocal.trim());
        }
        if (routing.supportsPhysicalRoom()) {
            codes.add(routing.hearingRoomPrefix() + "_SALA_01");
            codes.add(routing.hearingRoomPrefix() + "_SALA_02");
            if (routing.secrecyAware()) {
                codes.add(routing.hearingRoomPrefix() + "_SALA_SIGILO");
            }
        }
        if (routing.supportsVirtualRoom()) {
            codes.add(routing.hearingRoomPrefix() + "_VIRTUAL_01");
            codes.add(routing.hearingRoomPrefix() + "_VIRTUAL_02");
            if (routing.conciliationPreferred()) {
                codes.add(routing.hearingRoomPrefix() + "_CONCILIACAO");
            }
        }
        return codes.stream()
                .distinct()
                .map(code -> candidate(code, inicio, fim))
                .sorted(Comparator.comparingInt((ResourceCandidate v) -> v.conflicts().size()).thenComparing(ResourceCandidate::resourceCode))
                .toList();
    }

    private ResourceCandidate candidate(String resourceCode, LocalDateTime inicio, LocalDateTime fim) {
        long syntheticUserId = syntheticResourceUserId(resourceCode);
        List<String> conflicts = systemEventRepository.findByUsuarioIdBetween(syntheticUserId, inicio.minusMinutes(30), fim.plusMinutes(30)).stream()
                .map(event -> event.getTitle() + " @ " + event.getAt())
                .toList();
        String type = resourceCode.toUpperCase(Locale.ROOT).contains("VIRTUAL") ? "VIRTUAL" : "FISICA";
        return new ResourceCandidate(resourceCode, syntheticUserId, type, List.copyOf(conflicts));
    }

    private long syntheticResourceUserId(String resourceCode) {
        return -Math.abs(resourceCode == null ? 1 : resourceCode.toUpperCase(Locale.ROOT).hashCode());
    }

    public record HearingResourceSnapshot(
            ResourceCandidate selected,
            List<ResourceCandidate> candidates,
            List<String> fundamentos,
            Map<String, Object> metrics
    ) {
    }

    public record ResourceCandidate(
            String resourceCode,
            Long resourceUserId,
            String resourceType,
            List<String> conflicts
    ) {
    }
}
