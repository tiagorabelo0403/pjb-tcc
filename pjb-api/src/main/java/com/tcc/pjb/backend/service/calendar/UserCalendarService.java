package com.tcc.pjb.backend.service.calendar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarCustomEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarCustomEventRequest;
import com.tcc.pjb.backend.model.dto.calendar.CalendarDaySummaryDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventsResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarMarkerRequest;
import com.tcc.pjb.backend.model.dto.calendar.CalendarMonthResponse;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarCustomEvent;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarMarker;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarSystemEvent;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.calendar.UserCalendarCustomEventRepository;
import com.tcc.pjb.backend.model.repository.calendar.UserCalendarMarkerRepository;
import com.tcc.pjb.backend.model.repository.calendar.UserCalendarSystemEventRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;

@Service
public class UserCalendarService {

  private final CurrentUserService currentUser;
  private final ProcessoRepository processoRepo;
  private final AudienciaRepository audienciaRepo;
  private final JulgamentoColegiadoRepository julgamentoRepo;
  private final LaianeProcuracaoRepository procuracaoRepo;
  private final UserCalendarMarkerRepository markerRepo;
  private final UserCalendarCustomEventRepository customRepo;
  private final UserCalendarSystemEventRepository systemRepo;
  private final WorkItemRepository workItemRepository;
  private final CalendarNativeOperationalEventAssemblerService nativeOperationalEventAssemblerService;

  public UserCalendarService(
      CurrentUserService currentUser,
      ProcessoRepository processoRepo,
      AudienciaRepository audienciaRepo,
      JulgamentoColegiadoRepository julgamentoRepo,
      LaianeProcuracaoRepository procuracaoRepo,
      UserCalendarMarkerRepository markerRepo,
      UserCalendarCustomEventRepository customRepo,
      UserCalendarSystemEventRepository systemRepo,
      WorkItemRepository workItemRepository,
      CalendarNativeOperationalEventAssemblerService nativeOperationalEventAssemblerService) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.audienciaRepo = Objects.requireNonNull(audienciaRepo);
    this.julgamentoRepo = Objects.requireNonNull(julgamentoRepo);
    this.procuracaoRepo = Objects.requireNonNull(procuracaoRepo);
    this.markerRepo = Objects.requireNonNull(markerRepo);
    this.customRepo = Objects.requireNonNull(customRepo);
    this.systemRepo = Objects.requireNonNull(systemRepo);
    this.workItemRepository = Objects.requireNonNull(workItemRepository);
    this.nativeOperationalEventAssemblerService = Objects.requireNonNull(nativeOperationalEventAssemblerService);
  }

  @Transactional(readOnly = true)
  public CalendarEventsResponse list(LocalDate from, LocalDate to) {
    return listForUser(currentUser.getRequired(), from, to);
  }

  @Transactional(readOnly = true)
  public CalendarEventsResponse listForUser(Usuario u, LocalDate from, LocalDate to) {
    long[] processoIds = resolveProcessoIds(u);

    LocalDateTime fromDt = from.atStartOfDay();
    LocalDateTime toDt = to.plusDays(1).atStartOfDay().minusSeconds(1);

    Map<Long, String> numeroById = processoIds.length == 0 ? Map.of() : fetchNumeroUnificado(processoIds);

    List<Audiencia> audiencias = processoIds.length == 0 ? List.of() : audienciaRepo.findUpcomingByProcessoIds(processoIds, fromDt, toDt);
    List<JulgamentoColegiado> julgamentos = processoIds.length == 0 ? List.of() : julgamentoRepo.findUpcomingPautaByProcessoIds(processoIds, fromDt, toDt);

    Map<String, UserCalendarMarker> marks = new HashMap<>();
    if (u != null && u.getId() != null) {
      for (UserCalendarMarker m : markerRepo.findByUsuarioId(u.getId())) {
        if (m.getEventType() == null || m.getEventId() == null) continue;
        marks.put(key(m.getEventType(), m.getEventId()), m);
      }
    }

    List<UserCalendarCustomEvent> customs = u == null || u.getId() == null ? List.of() : customRepo.findByUsuarioIdBetween(u.getId(), fromDt, toDt);
    List<UserCalendarSystemEvent> systemEvents = u == null || u.getId() == null ? List.of() : systemRepo.findByUsuarioIdBetween(u.getId(), fromDt, toDt);
    List<CalendarEventDto> nativeOperationalEvents = nativeOperationalEventAssemblerService.assembleForUser(u, from, to, numeroById);

    Map<LocalDate, List<CalendarEventDto>> grouped = new TreeMap<>();

    for (Audiencia a : audiencias) {
      Long pid = a.getProcesso() != null ? a.getProcesso().getId() : null;
      if (pid == null || a.getDataHora() == null) continue;
      LocalDate day = a.getDataHora().toLocalDate();
      String mk = key("AUDIENCIA", a.getId());
      UserCalendarMarker m = marks.get(mk);
      grouped.computeIfAbsent(day, k -> new ArrayList<>()).add(new CalendarEventDto(
          "AUDIENCIA",
          a.getId(),
          pid,
          numeroById.get(pid),
          buildAudienciaTitle(a),
          a.getDataHora(),
          m != null ? normalizeColor(m.getColor()) : "BLUE",
          m != null,
          buildProcessoDetailsUrl(u, pid),
          null,
          "AUDIENCIA:" + a.getId(),
          "DATABASE"
      ));
    }

    for (JulgamentoColegiado j : julgamentos) {
      Long pid = j.getProcesso() != null ? j.getProcesso().getId() : null;
      if (pid == null || j.getPautaDataHora() == null) continue;
      LocalDate day = j.getPautaDataHora().toLocalDate();
      String mk = key("JULGAMENTO", j.getId());
      UserCalendarMarker m = marks.get(mk);
      grouped.computeIfAbsent(day, k -> new ArrayList<>()).add(new CalendarEventDto(
          "JULGAMENTO",
          j.getId(),
          pid,
          numeroById.get(pid),
          buildJulgamentoTitle(j),
          j.getPautaDataHora(),
          m != null ? normalizeColor(m.getColor()) : "PURPLE",
          m != null,
          buildProcessoDetailsUrl(u, pid),
          null,
          "JULGAMENTO:" + j.getId(),
          "DATABASE"
      ));
    }

    for (UserCalendarCustomEvent c : customs) {
      LocalDate day = c.getAt().toLocalDate();
      Long pid = c.getProcessoId();
      grouped.computeIfAbsent(day, k -> new ArrayList<>()).add(new CalendarEventDto(
          "CUSTOM",
          c.getId(),
          pid,
          pid != null ? numeroById.get(pid) : null,
          c.getTitle(),
          c.getAt(),
          normalizeColor(c.getColor()),
          true,
          pid != null ? buildProcessoDetailsUrl(u, pid) : null,
          null,
          "CUSTOM:" + c.getId(),
          "CUSTOM"
      ));
    }

    for (UserCalendarSystemEvent c : systemEvents) {
      if (c.getAt() == null) continue;
      LocalDate day = c.getAt().toLocalDate();
      Long pid = c.getProcessoId();
      grouped.computeIfAbsent(day, k -> new ArrayList<>()).add(new CalendarEventDto(
          c.getEventType(),
          c.getId(),
          pid,
          pid != null ? numeroById.get(pid) : null,
          c.getTitle(),
          c.getAt(),
          normalizeColor(c.getColor()),
          true,
          c.getDetailsUrl() != null ? c.getDetailsUrl() : (pid != null ? buildProcessoDetailsUrl(u, pid) : null),
          c.getBody(),
          c.getDomainKey(),
          "SYSTEM"
      ));
    }

    for (CalendarEventDto event : nativeOperationalEvents) {
      if (event == null || event.at() == null) continue;
      LocalDate day = event.at().toLocalDate();
      grouped.computeIfAbsent(day, k -> new ArrayList<>()).add(event);
    }

    List<CalendarEventsResponse.CalendarDayDto> days = grouped.entrySet().stream()
        .map(e -> new CalendarEventsResponse.CalendarDayDto(
            e.getKey(),
            e.getValue().stream().sorted(Comparator.comparing(CalendarEventDto::at)).toList()))
        .toList();

    return new CalendarEventsResponse(from, to, days);
  }

  
@Transactional(readOnly = true)
public CalendarMonthResponse month(YearMonth month) {
  Usuario u = currentUser.getRequired();
  long[] processoIds = resolveProcessoIds(u);

  LocalDate from = month.atDay(1);
  LocalDate to = month.atEndOfMonth();
  LocalDateTime fromDt = from.atStartOfDay();
  LocalDateTime toDt = to.plusDays(1).atStartOfDay().minusSeconds(1);

  Map<String, UserCalendarMarker> marks = new HashMap<>();
  for (UserCalendarMarker m : markerRepo.findByUsuarioId(u.getId())) {
    if (m.getEventType() == null || m.getEventId() == null) continue;
    marks.put(key(m.getEventType(), m.getEventId()), m);
  }

  Map<LocalDate, DayAgg> agg = new HashMap<>();

  if (processoIds.length > 0) {
    for (Object[] row : audienciaRepo.findCalendarRowsByProcessoIds(processoIds, fromDt, toDt)) {
      if (row == null || row.length < 3) continue;
      Long id = row[0] instanceof Number n ? n.longValue() : null;
      LocalDateTime at = row[2] instanceof LocalDateTime ldt ? ldt : null;
      if (id == null || at == null) continue;
      LocalDate day = at.toLocalDate();
      DayAgg d = agg.computeIfAbsent(day, k -> new DayAgg());
      d.count++;
      String mk = key("AUDIENCIA", id);
      UserCalendarMarker m = marks.get(mk);
      String color = m != null ? normalizeColor(m.getColor()) : "BLUE";
      d.colors.add(color);
      if (m != null) d.marked = true;
    }

    for (Object[] row : julgamentoRepo.findCalendarRowsByProcessoIds(processoIds, fromDt, toDt)) {
      if (row == null || row.length < 3) continue;
      Long id = row[0] instanceof Number n ? n.longValue() : null;
      LocalDateTime at = row[2] instanceof LocalDateTime ldt ? ldt : null;
      if (id == null || at == null) continue;
      LocalDate day = at.toLocalDate();
      DayAgg d = agg.computeIfAbsent(day, k -> new DayAgg());
      d.count++;
      String mk = key("JULGAMENTO", id);
      UserCalendarMarker m = marks.get(mk);
      String color = m != null ? normalizeColor(m.getColor()) : "PURPLE";
      d.colors.add(color);
      if (m != null) d.marked = true;
    }
  }

  for (UserCalendarCustomEvent c : customRepo.findByUsuarioIdBetween(u.getId(), fromDt, toDt)) {
    if (c.getAt() == null) continue;
    LocalDate day = c.getAt().toLocalDate();
    DayAgg d = agg.computeIfAbsent(day, k -> new DayAgg());
    d.count++;
    d.colors.add(normalizeColor(c.getColor()));
    d.marked = true;
  }

  for (UserCalendarSystemEvent c : systemRepo.findByUsuarioIdBetween(u.getId(), fromDt, toDt)) {
    if (c.getAt() == null) continue;
    LocalDate day = c.getAt().toLocalDate();
    DayAgg d = agg.computeIfAbsent(day, k -> new DayAgg());
    d.count++;
    d.colors.add(normalizeColor(c.getColor()));
    d.marked = true;
  }

  Map<Long, String> numeroById = processoIds.length == 0 ? Map.of() : fetchNumeroUnificado(processoIds);
  for (CalendarEventDto event : nativeOperationalEventAssemblerService.assembleForUser(u, from, to, numeroById)) {
    if (event == null || event.at() == null) continue;
    LocalDate day = event.at().toLocalDate();
    DayAgg d = agg.computeIfAbsent(day, k -> new DayAgg());
    d.count++;
    d.colors.add(normalizeColor(event.color()));
  }

  List<CalendarDaySummaryDto> days = new ArrayList<>(month.lengthOfMonth());
  for (int i = 1; i <= month.lengthOfMonth(); i++) {
    LocalDate date = month.atDay(i);
    DayAgg d = agg.get(date);
    if (d == null) {
      days.add(new CalendarDaySummaryDto(date, false, 0, List.of(), false));
    } else {
      days.add(new CalendarDaySummaryDto(date, d.count > 0, d.count, List.copyOf(d.colors), d.marked));
    }
  }

  return new CalendarMonthResponse(month, days);
}

private static final class DayAgg {
  int count;
  boolean marked;
  LinkedHashSet<String> colors = new LinkedHashSet<>();
}

@Transactional
  public void mark(CalendarMarkerRequest req) {
    Usuario u = currentUser.getRequired();
    String type = normalizeType(req.eventType());
    Long id = req.eventId();
    if (type == null || id == null) throw new IllegalArgumentException("event");
    String color = normalizeColor(req.color());
    UserCalendarMarker existing = markerRepo.findByUsuarioIdAndEventTypeAndEventId(u.getId(), type, id).orElse(null);
    if (existing != null) {
      existing.setColor(color);
      markerRepo.save(existing);
      return;
    }
    markerRepo.save(UserCalendarMarker.builder()
        .usuarioId(u.getId())
        .eventType(type)
        .eventId(id)
        .color(color)
        .createdAt(Instant.now())
        .build());
  }

  @Transactional
  public void unmark(String eventType, Long eventId) {
    Usuario u = currentUser.getRequired();
    String type = normalizeType(eventType);
    if (type == null || eventId == null) return;
    markerRepo.findByUsuarioIdAndEventTypeAndEventId(u.getId(), type, eventId).ifPresent(markerRepo::delete);
  }

  @Transactional
  public CalendarCustomEventDto createCustomEvent(CalendarCustomEventRequest req) {
    Usuario u = currentUser.getRequired();
    String title = normalizeTitle(req.title());
    LocalDateTime at = req.at();
    if (title == null || at == null) throw new IllegalArgumentException("custom_event");
    Long processoId = req.processoId();
    if (processoId != null) {
      if (!belongsToUser(u, processoId)) throw new IllegalArgumentException("processo");
    }
    String color = normalizeColor(req.color());
    Instant now = Instant.now();
    UserCalendarCustomEvent e = customRepo.save(UserCalendarCustomEvent.builder()
        .usuarioId(u.getId())
        .processoId(processoId)
        .title(title)
        .at(at)
        .color(color)
        .createdAt(now)
        .updatedAt(now)
        .build());
    String processoNumero = null;
    if (processoId != null) {
      processoNumero = processoRepo.findNumeroUnificadoByIds(List.of(processoId)).stream().findFirst().map(r -> r[1] != null ? r[1].toString() : null).orElse(null);
    }
    return new CalendarCustomEventDto(e.getId(), e.getTitle(), e.getAt(), e.getColor(), e.getProcessoId(), processoNumero);
  }

  @Transactional
  public CalendarCustomEventDto updateCustomEvent(Long id, CalendarCustomEventRequest req) {
    Usuario u = currentUser.getRequired();
    UserCalendarCustomEvent e = customRepo.findByIdAndUsuarioId(id, u.getId()).orElseThrow();
    String title = normalizeTitle(req.title());
    LocalDateTime at = req.at();
    if (title == null || at == null) throw new IllegalArgumentException("custom_event");
    Long processoId = req.processoId();
    if (processoId != null) {
      if (!belongsToUser(u, processoId)) throw new IllegalArgumentException("processo");
    }
    e.setTitle(title);
    e.setAt(at);
    e.setColor(normalizeColor(req.color()));
    e.setProcessoId(processoId);
    e.setUpdatedAt(Instant.now());
    customRepo.save(e);
    String processoNumero = null;
    if (processoId != null) {
      processoNumero = processoRepo.findNumeroUnificadoByIds(List.of(processoId)).stream().findFirst().map(r -> r[1] != null ? r[1].toString() : null).orElse(null);
    }
    return new CalendarCustomEventDto(e.getId(), e.getTitle(), e.getAt(), e.getColor(), e.getProcessoId(), processoNumero);
  }

  @Transactional
  public void deleteCustomEvent(Long id) {
    Usuario u = currentUser.getRequired();
    customRepo.findByIdAndUsuarioId(id, u.getId()).ifPresent(customRepo::delete);
  }


  @Transactional(readOnly = true)
  public CalendarEventsResponse listForProcesso(LocalDate from, LocalDate to, Long processoId) {
    return listForProcessoForUser(currentUser.getRequired(), from, to, processoId);
  }

  @Transactional(readOnly = true)
  public CalendarEventsResponse listForProcessoForUser(Usuario usuario, LocalDate from, LocalDate to, Long processoId) {
    CalendarEventsResponse all = listForUser(usuario, from, to);
    List<CalendarEventsResponse.CalendarDayDto> days = all.days().stream()
        .map(day -> new CalendarEventsResponse.CalendarDayDto(
            day.day(),
            day.events().stream().filter(event -> Objects.equals(event.processoId(), processoId)).toList()))
        .filter(day -> !day.events().isEmpty())
        .toList();
    return new CalendarEventsResponse(from, to, days);
  }

  @Transactional(readOnly = true)
  public String ics(LocalDate from, LocalDate to, Long processoId) {
    CalendarEventsResponse r = processoId != null ? listForProcesso(from, to, processoId) : list(from, to);
    StringBuilder sb = new StringBuilder(8192);
    sb.append("BEGIN:VCALENDAR\r\n");
    sb.append("VERSION:2.0\r\n");
    sb.append("PRODID:-//PJB//Calendar//PT-BR\r\n");
    for (var day : r.days()) {
      for (var e : day.events()) {
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:").append(uid(e)).append("\r\n");
        sb.append("DTSTAMP:").append(formatIcsUtc(LocalDateTime.now())).append("\r\n");
        sb.append("DTSTART:").append(formatIcsUtc(e.at())).append("\r\n");
        sb.append("SUMMARY:").append(escapeIcs(e.title())).append("\r\n");
        if (e.processoNumero() != null) {
          sb.append("DESCRIPTION:").append(escapeIcs("Processo " + e.processoNumero())).append("\r\n");
        }
        sb.append("END:VEVENT\r\n");
      }
    }
    sb.append("END:VCALENDAR\r\n");
    return sb.toString();
  }

  private long[] resolveProcessoIds(Usuario u) {
    if (u == null || u.getTipoUsuario() == null) {
      return new long[0];
    }
    if (u.getTipoUsuario() == TipoUsuario.CIDADAO) {
      String cpf = u.getCpf();
      if (cpf == null || cpf.isBlank()) return new long[0];
      List<Processo> processos = processoRepo.findAllByPartesCpf(cpf);
      return processos.stream().map(Processo::getId).filter(Objects::nonNull).mapToLong(Long::longValue).toArray();
    }
    if (u.getTipoUsuario().isAdvocacia()) {
      List<Long> ids = procuracaoRepo.findProcessoIdsByAdvogadoAndStatus(u.getId(), LaianeProcuracaoStatus.ATIVA);
      return ids.stream().filter(Objects::nonNull).mapToLong(Long::longValue).toArray();
    }
    LinkedHashSet<Long> ids = new LinkedHashSet<>();
    collectProcessosFromWorkItems(ids, u);
    return ids.stream().mapToLong(Long::longValue).toArray();
  }

  private void collectProcessosFromWorkItems(LinkedHashSet<Long> ids, Usuario usuario) {
    if (usuario == null) {
      return;
    }
    if (usuario.getId() != null) {
      workItemRepository.inboxByUser(usuario.getId(), org.springframework.data.domain.PageRequest.of(0, 200)).getContent().stream()
          .map(item -> item.getProcesso() != null ? item.getProcesso().getId() : null)
          .filter(Objects::nonNull)
          .forEach(ids::add);
    }
    if (usuario.getTipoUsuario() != null) {
      workItemRepository.inboxByRoleAndTerritory(usuario.getTipoUsuario(), usuario.getUf(), usuario.getComarca(), org.springframework.data.domain.PageRequest.of(0, 200))
          .getContent().stream()
          .map(item -> item.getProcesso() != null ? item.getProcesso().getId() : null)
          .filter(Objects::nonNull)
          .forEach(ids::add);
    }
  }

  private boolean belongsToUser(Usuario u, Long processoId) {
    if (u == null || processoId == null) return false;
    if (u.getTipoUsuario() == TipoUsuario.CIDADAO) {
      String cpf = u.getCpf();
      if (cpf == null || cpf.isBlank()) return false;
      Processo p = processoRepo.findById(processoId).orElse(null);
      if (p == null) return false;
      if (cpf.equals(p.getParteAutoraCpf()) || cpf.equals(p.getParteReuCpf())) return true;
      return p.getUsuario() != null && cpf.equals(p.getUsuario().getCpf());
    }
    if (u.getTipoUsuario().isAdvocacia()) {
      return procuracaoRepo.existsByAdvogado_IdAndProcessoIdAndStatus(u.getId(), processoId, LaianeProcuracaoStatus.ATIVA);
    }
    return resolveProcessoIds(u).length == 0 ? false : java.util.Arrays.stream(resolveProcessoIds(u)).anyMatch(id -> id == processoId);
  }

  private Map<Long, String> fetchNumeroUnificado(long[] processoIds) {
    List<Long> ids = new ArrayList<>(processoIds.length);
    for (long id : processoIds) ids.add(id);
    Map<Long, String> m = new HashMap<>();
    for (Object[] row : processoRepo.findNumeroUnificadoByIds(ids)) {
      if (row == null || row.length < 2) continue;
      Long pid = row[0] instanceof Long l ? l : null;
      String num = row[1] != null ? row[1].toString() : null;
      if (pid != null) m.put(pid, num);
    }
    return m;
  }

  private static String buildAudienciaTitle(Audiencia a) {
    String t = a.getTipo() != null ? a.getTipo().name() : "AUDIENCIA";
    String m = a.getModalidade() != null ? a.getModalidade().name() : null;
    if (m != null && !m.isBlank()) return t + " (" + m + ")";
    return t;
  }

  private static String buildJulgamentoTitle(JulgamentoColegiado j) {
    String o = j.getOrgaoJulgador() != null ? j.getOrgaoJulgador() : "JULGAMENTO";
    return "Sessão " + o;
  }

  private static String buildProcessoDetailsUrl(Usuario u, Long processoId) {
    if (u != null && u.getTipoUsuario() == TipoUsuario.CIDADAO) {
      return "/api/v1/cidadao/processos/" + processoId;
    }
    return "/api/v1/processos/" + processoId;
  }

  private static String key(String type, Long id) {
    return type + ":" + id;
  }

  private static String normalizeType(String t) {
    if (t == null) return null;
    String s = t.trim().toUpperCase();
    if (s.equals("AUDIENCIA")
        || s.equals("JULGAMENTO")
        || s.equals("COMUNICACAO_JUDICIAL")
        || s.equals("AUDIENCIA_PROCESSUAL")
        || s.equals("AUDIENCIA_RECURSO_SECRETARIA")
        || s.equals("AUDIENCIA_PRESENCA_SECRETARIA")
        || s.equals("MANDADO_DILIGENCIA")
        || s.equals("PERICIA_OPERACIONAL")
        || s.equals("PERICIA_NOMEACAO")
        || s.equals("SECRETARIA_OPERACIONAL")
        || s.equals("GABINETE_DECISORIO")
        || s.equals("PRAZO_INSTITUCIONAL")
        || s.equals("PRECATORIO_OPERACIONAL")
        || s.equals("PAUTA_COLEGIADA")
        || s.equals("AGENDA_OPERACIONAL")) return s;
    return null;
  }

  private static String normalizeColor(String c) {
    if (c == null || c.isBlank()) return "BLUE";
    String s = c.trim().toUpperCase();
    if (s.length() > 16) s = s.substring(0, 16);
    return s;
  }

  private static String normalizeTitle(String s) {
    if (s == null) return null;
    String t = s.trim();
    if (t.isEmpty()) return null;
    if (t.length() > 180) t = t.substring(0, 180);
    return t;
  }

  private static String uid(CalendarEventDto e) {
    return e.eventType() + "-" + e.eventId() + "@pjb";
  }

  private static String formatIcsUtc(LocalDateTime dt) {
    return dt.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
  }

  private static String escapeIcs(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n").replace("\r", "");
  }
}
