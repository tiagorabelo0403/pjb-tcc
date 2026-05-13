package com.tcc.pjb.backend.core.prazos.auditoria;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditHealthView;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditQuery;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditResult;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoTimelineEntry;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoTimelineView;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseEntry;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PrazoAuditTrailService {
    private final CalendarioForenseRepository calendarioRepository;
    public PrazoAuditTrailService(CalendarioForenseRepository calendarioRepository) { this.calendarioRepository = Objects.requireNonNull(calendarioRepository); }
    public PrazoAuditTrail build(Long processoId, String eventoRef, int quantidadeSolicitada, PrazoRegime regimeAplicado, LocalDateTime inicio, LocalDateTime fim, String uf, String comarca) {
        LocalDate ini = inicio == null ? LocalDate.now() : inicio.toLocalDate().minusDays(2);
        LocalDate end = fim == null ? ini.plusDays(1) : fim.toLocalDate().plusDays(2);
        List<CalendarioForenseEntry> entries = calendarioRepository.findApplicableBetween(safeUf(uf), safeComarca(comarca), ini, end).stream().sorted(Comparator.comparing(CalendarioForenseEntry::getDia, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(CalendarioForenseEntry::getTipo, Comparator.nullsLast(String::compareTo))).toList();
        StringBuilder canonical = new StringBuilder();
        for (CalendarioForenseEntry entry : entries) canonical.append(entry.getDia()).append('|').append(entry.getUf()).append('|').append(entry.getComarca()).append('|').append(entry.getTipo()).append('\n');
        return new PrazoAuditTrail(processoId, normalize(eventoRef), quantidadeSolicitada, regimeAplicado == null ? PrazoRegime.UTEIS : regimeAplicado, inicio, fim, safeUf(uf), safeComarca(comarca), entries.stream().map(CalendarioForenseEntry::getDia).filter(Objects::nonNull).distinct().count(), Hashes.sha256Hex(canonical.toString()), Instant.now());
    }
    public PrazoAuditResult query(PrazoAuditQuery query) { return new PrazoAuditResult(build(query.processoId(), query.eventoRef(), query.quantidadeSolicitada(), query.regimeAplicado(), query.inicio(), query.fim(), query.uf(), query.comarca()), true); }
    public PrazoAuditHealthView healthView(PrazoAuditTrail trail) { return new PrazoAuditHealthView(trail.calendarioVersaoHash(), trail.totalFeriadosBloqueados()); }
    public PrazoTimelineView timeline(PrazoAuditTrail trail) { return new PrazoTimelineView(List.of(new PrazoTimelineEntry("inicio", trail.inicio() == null ? null : trail.inicio().toInstant(java.time.ZoneOffset.UTC), String.valueOf(trail.regimeAplicado())), new PrazoTimelineEntry("fim", trail.fim() == null ? null : trail.fim().toInstant(java.time.ZoneOffset.UTC), String.valueOf(trail.regimeAplicado())))); }
    private static String normalize(String value) { if (value == null) return null; String trimmed = value.trim(); return trimmed.isBlank() ? null : trimmed; }
    private static String safeUf(String uf) { if (uf == null) return null; String value = uf.trim().toUpperCase(); return value.isBlank() ? null : value; }
    private static String safeComarca(String comarca) { if (comarca == null) return null; String value = comarca.trim(); return value.isBlank() ? null : value; }
}
