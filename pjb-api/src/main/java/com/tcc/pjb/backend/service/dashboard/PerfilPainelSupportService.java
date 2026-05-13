package com.tcc.pjb.backend.service.dashboard;

import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.DeviceRiskEngine;
import com.tcc.pjb.backend.core.security.device.RiskEvaluation;
import com.tcc.pjb.backend.core.security.sigilo.repository.SigiloAccessRequestRepository;
import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessStatus;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventsResponse;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.BehavioralAuditResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.ExternalSystemStatus;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.OnboardingResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.PlantaoResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.PrazoRadarItem;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.SessionRiskResumo;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.SigiloAtivoResumo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import com.tcc.pjb.backend.service.calendar.UserCalendarService;
import com.tcc.pjb.backend.service.profile.PerfilBehavioralAuditService;
import com.tcc.pjb.backend.service.profile.PerfilExternalSystemService;
import com.tcc.pjb.backend.service.profile.PerfilOnboardingService;

@Service
public class PerfilPainelSupportService {

    private final CurrentUserService currentUserService;
    private final WorkItemRepository workItemRepository;
    private final DeviceRiskEngine deviceRiskEngine;
    private final UserSecurityProfileRepository userSecurityProfileRepository;
    private final SigiloAccessRequestRepository sigiloAccessRequestRepository;
    private final UserCalendarService userCalendarService;
    private final PerfilOnboardingService onboardingService;
    private final PerfilExternalSystemService externalSystemService;
    private final PerfilBehavioralAuditService behavioralAuditService;
    private final PjbTimeService pjbTimeService;

    public PerfilPainelSupportService(CurrentUserService currentUserService,
                                      WorkItemRepository workItemRepository,
                                      DeviceRiskEngine deviceRiskEngine,
                                      UserSecurityProfileRepository userSecurityProfileRepository,
                                      SigiloAccessRequestRepository sigiloAccessRequestRepository,
                                      UserCalendarService userCalendarService,
                                      PerfilOnboardingService onboardingService,
                                      PerfilExternalSystemService externalSystemService,
                                      PerfilBehavioralAuditService behavioralAuditService,
                                      PjbTimeService pjbTimeService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.deviceRiskEngine = Objects.requireNonNull(deviceRiskEngine);
        this.userSecurityProfileRepository = Objects.requireNonNull(userSecurityProfileRepository);
        this.sigiloAccessRequestRepository = Objects.requireNonNull(sigiloAccessRequestRepository);
        this.userCalendarService = Objects.requireNonNull(userCalendarService);
        this.onboardingService = Objects.requireNonNull(onboardingService);
        this.externalSystemService = Objects.requireNonNull(externalSystemService);
        this.behavioralAuditService = Objects.requireNonNull(behavioralAuditService);
        this.pjbTimeService = Objects.requireNonNull(pjbTimeService);
    }

    public Usuario currentUser() {
        return currentUserService.getRequired();
    }

    public List<PrazoRadarItem> prazoRadar(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            return List.of();
        }
        Instant now = pjbTimeService.nowUtc();
        Instant limit = now.plus(7, ChronoUnit.DAYS);
        List<WorkItem> workItems = new ArrayList<>(workItemRepository.findDueByAssignedUser(usuario.getId(), limit, PageRequest.of(0, 12)));
        if (workItems.isEmpty() && usuario.getTipoUsuario() != null) {
            workItems = new ArrayList<>(workItemRepository.findDueByRoleAndTerritory(usuario.getTipoUsuario(), usuario.getUf(), usuario.getComarca(), limit, PageRequest.of(0, 12)));
        }
        return workItems.stream()
                .filter(w -> w.getDueAt() != null)
                .sorted(Comparator.comparing(WorkItem::getDueAt))
                .limit(8)
                .map(w -> new PrazoRadarItem(w.getId(), w.getTitulo(), w.getDueAt(), classifyDeadline(now, w.getDueAt(), pjbTimeService.legalZone()), daysUntilDeadline(now, w.getDueAt(), pjbTimeService.legalZone())))
                .toList();
    }

    public SessionRiskResumo sessionRisk(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            return new SessionRiskResumo("UNKNOWN", 0, "UNKNOWN", false, "Usuário não identificado.");
        }
        UserSecurityProfile profile = userSecurityProfileRepository.findByUserId(usuario.getId()).orElse(null);
        String ip = resolveClientIp();
        RiskEvaluation evaluation = deviceRiskEngine.evaluateFirstLink(usuario, ip, profile, false);
        String level = switch (evaluation.decision()) {
            case ALLOW -> evaluation.riskScore() >= 60 ? "MEDIO" : "BAIXO";
            case CHALLENGE -> "ALTO";
            case DENY -> "CRITICO";
        };
        return new SessionRiskResumo(level, evaluation.riskScore(), evaluation.networkLabel(), evaluation.suspectNetwork(), evaluation.reason());
    }

    public SigiloAtivoResumo sigiloAtivo(Usuario usuario) {
        LocalDateTime now = LocalDateTime.ofInstant(pjbTimeService.nowUtc(), pjbTimeService.legalZone());
        if (RequestContext.getSigiloCredential().isPresent()) {
            return new SigiloAtivoResumo(true, 1, now.plusHours(1), 60);
        }
        if (usuario != null && usuario.getTipoUsuario() == com.tcc.pjb.backend.model.entity.enums.TipoUsuario.ADVOGADO && usuario.getId() != null) {
            var active = sigiloAccessRequestRepository.findByAdvogadoIdOrderByRequestedAtDesc(usuario.getId()).stream()
                    .filter(req -> req.getStatus() == SigiloAccessStatus.APROVADA && req.isApprovedAndActive(now))
                    .sorted(Comparator.comparing(req -> req.getExpiresAt(), Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            if (!active.isEmpty()) {
                LocalDateTime expiresAt = active.get(0).getExpiresAt();
                long minutes = expiresAt == null ? 0L : ChronoUnit.MINUTES.between(now, expiresAt);
                return new SigiloAtivoResumo(true, active.size(), expiresAt, Math.max(0L, minutes));
            }
        }
        return new SigiloAtivoResumo(false, 0, null, 0L);
    }

    public PlantaoResumo plantao(Usuario usuario) {
        try {
            LocalDate hoje = LocalDate.ofInstant(pjbTimeService.nowUtc(), pjbTimeService.legalZone());
            CalendarEventsResponse response = userCalendarService.list(hoje, hoje.plusDays(14));
            List<CalendarEventDto> matching = response.days().stream()
                    .flatMap(day -> day.events().stream())
                    .filter(event -> event.title() != null && normalize(event.title()).contains("PLANTAO"))
                    .sorted(Comparator.comparing(CalendarEventDto::at))
                    .toList();
            if (matching.isEmpty()) {
                return new PlantaoResumo("SEM_PLANTAO", null, null, usuario != null ? usuario.getComarca() : null);
            }
            CalendarEventDto current = matching.stream().filter(event -> event.at() != null && event.at().toLocalDate().equals(hoje)).findFirst().orElse(matching.get(0));
            String status = current.at() != null && current.at().toLocalDate().equals(hoje) ? "PLANTAO_ATIVO" : "PROXIMO_PLANTAO";
            return new PlantaoResumo(status, current.at(), current.at() != null ? current.at().plusHours(12) : null, current.title());
        } catch (Exception ex) {
            return new PlantaoResumo("INDISPONIVEL", null, null, "agenda_unavailable");
        }
    }

    public OnboardingResumo onboarding(Usuario usuario) {
        return onboardingService.avaliar(usuario);
    }

    public List<ExternalSystemStatus> externalSystems(Usuario usuario) {
        return externalSystemService.snapshotFor(usuario);
    }

    public BehavioralAuditResumo behavioralAudit(Usuario usuario) {
        return behavioralAuditService.avaliar(usuario);
    }

    public List<String> pendencias(Usuario usuario, OnboardingResumo onboarding, List<PrazoRadarItem> prazoRadar, SessionRiskResumo risk) {
        List<String> pendencias = new ArrayList<>();
        if (onboarding != null && !onboarding.concluido()) {
            pendencias.addAll(onboarding.pendenciasBloqueantes());
        }
        prazoRadar.stream().filter(item -> "FATAL".equals(item.categoria())).map(item -> "Prazo fatal: " + item.titulo()).forEach(pendencias::add);
        if (risk != null && ("ALTO".equals(risk.level()) || "CRITICO".equals(risk.level()))) {
            pendencias.add("Risco de sessão elevado: " + risk.reason());
        }
        return List.copyOf(pendencias.stream().distinct().toList());
    }

    public String etagFor(String prefix, Object... values) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update((prefix == null ? "PJB" : prefix).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (values != null) {
                for (Object value : values) {
                    digest.update(String.valueOf(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            return '"' + HexFormat.of().formatHex(digest.digest()).substring(0, 24) + '"';
        } catch (Exception ex) {
            return '"' + Integer.toHexString(java.util.Objects.hash(values)) + '"';
        }
    }

    private static String classifyDeadline(Instant now, Instant dueAt, ZoneId zoneId) {
        long days = daysUntilDeadline(now, dueAt, zoneId);
        if (days <= 0) {
            return "FATAL";
        }
        if (days <= 3) {
            return "CRITICO";
        }
        return "ATENCAO";
    }

    private static long daysUntilDeadline(Instant now, Instant dueAt, ZoneId zoneId) {
        if (now == null || dueAt == null) {
            return Long.MAX_VALUE;
        }
        ZoneId zone = zoneId == null ? ZoneOffset.UTC : zoneId;
        LocalDate base = LocalDate.ofInstant(now, zone);
        LocalDate due = LocalDate.ofInstant(dueAt, zone);
        return ChronoUnit.DAYS.between(base, due);
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(java.util.Locale.ROOT);
    }

    private static String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "127.0.0.1";
            }
            var request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String real = request.getHeader("X-Real-IP");
            if (real != null && !real.isBlank()) {
                return real.trim();
            }
            String remote = request.getRemoteAddr();
            if (remote != null && !remote.isBlank()) {
                return remote.trim();
            }
            return InetAddress.getLoopbackAddress().getHostAddress();
        } catch (Exception ex) {
            return InetAddress.getLoopbackAddress().getHostAddress();
        }
    }
}
