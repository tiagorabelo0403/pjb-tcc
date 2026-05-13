package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class SecretariatOperationalBottleneckRadarService {

    private static final List<String> ACTIVE_STATUSES = List.of("PENDENTE", "EM_EXECUCAO");

    private final SecretariatQueueItemRepository queueItemRepository;
    private final WorkItemRepository workItemRepository;

    public SecretariatOperationalBottleneckRadarService(SecretariatQueueItemRepository queueItemRepository,
                                                        WorkItemRepository workItemRepository) {
        this.queueItemRepository = Objects.requireNonNull(queueItemRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
    }

    @Transactional(readOnly = true)
    public BottleneckRadarSnapshot avaliar(SecretariatOperationalRoutingProfile routing) {
        LinkedHashSet<String> inboxes = new LinkedHashSet<>();
        inboxes.add(routing.receiptInboxKey());
        inboxes.add(routing.saneamentoInboxKey());
        inboxes.add(routing.audienceInboxKey());
        inboxes.add(routing.executionInboxKey());
        List<UnitInboxSummary> unitSummaries = inboxes.stream().filter(Objects::nonNull).map(this::buildUnitSummary).toList();
        List<DeskSummary> deskSummaries = inboxes.stream().filter(Objects::nonNull).flatMap(inbox -> buildDeskSummaries(inbox).stream()).toList();
        List<ServerSummary> serverSummaries = inboxes.stream().filter(Objects::nonNull).flatMap(inbox -> buildServerSummaries(inbox).stream()).toList();
        int totalActive = unitSummaries.stream().mapToInt(UnitInboxSummary::active).sum();
        int totalOverdue = unitSummaries.stream().mapToInt(UnitInboxSummary::overdue).sum();
        String band = totalOverdue >= 20 || totalActive >= 220 ? "CRITICA" : totalOverdue >= 8 || totalActive >= 100 ? "PRESSAO" : "CONTROLADA";
        List<BottleneckIndicator> indicators = new ArrayList<>();
        deskSummaries.stream().filter(summary -> !"LIVRE".equals(summary.band())).limit(6).forEach(summary -> indicators.add(new BottleneckIndicator(
                "DESK",
                summary.deskAxis(),
                summary.band(),
                "Desk com carga ativa=" + summary.active() + ", atrasos=" + summary.overdue() + ", bloqueios=" + summary.blocking() + "."
        )));
        serverSummaries.stream().filter(summary -> !"LIVRE".equals(summary.band())).limit(6).forEach(summary -> indicators.add(new BottleneckIndicator(
                "SERVIDOR",
                summary.nome(),
                summary.band(),
                "Servidor com carga ativa=" + summary.active() + ", atrasos=" + summary.overdue() + ", primeira entrega=" + summary.firstDueAt() + "."
        )));
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Radar de gargalo mede unidade, desk e servidor nas filas reais da secretaria.");
        fundamentos.add("Secretaria monitorada: " + routing.secretariatCode() + ".");
        fundamentos.add("Trilha organizacional: " + routing.organizationalPath() + ".");
        fundamentos.add("Banda consolidada: " + band + ".");
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("secretariatCode", routing.secretariatCode());
        metrics.put("unitInboxCount", unitSummaries.size());
        metrics.put("deskCount", deskSummaries.size());
        metrics.put("serverCount", serverSummaries.size());
        metrics.put("totalActive", totalActive);
        metrics.put("totalOverdue", totalOverdue);
        metrics.put("band", band);
        return new BottleneckRadarSnapshot(band, List.copyOf(unitSummaries), List.copyOf(deskSummaries), List.copyOf(serverSummaries), List.copyOf(indicators), List.copyOf(fundamentos), Map.copyOf(metrics));
    }

    private UnitInboxSummary buildUnitSummary(String inboxKey) {
        Object[] row = queueItemRepository.workload(inboxKey, ACTIVE_STATUSES, Instant.now());
        int active = asInt(row, 0);
        int overdue = asInt(row, 1);
        int expedited = asInt(row, 2);
        String band = active >= 100 || overdue >= 12 ? "CRITICA" : active >= 40 || overdue >= 4 ? "PRESSAO" : "LIVRE";
        return new UnitInboxSummary(inboxKey, active, overdue, expedited, band);
    }

    private List<DeskSummary> buildDeskSummaries(String inboxKey) {
        return queueItemRepository.deskWorkload(inboxKey, ACTIVE_STATUSES, Instant.now()).stream()
                .map(row -> new DeskSummary(
                        inboxKey,
                        String.valueOf(row[0]),
                        asInt(row, 1),
                        asInt(row, 2),
                        asInt(row, 3),
                        asInt(row, 4),
                        asInt(row, 5),
                        deskBand(asInt(row, 1), asInt(row, 2), asInt(row, 3))))
                .toList();
    }

    private List<ServerSummary> buildServerSummaries(String inboxKey) {
        return workItemRepository.radarByInboxAssignedUser(inboxKey, Instant.now()).stream()
                .map(row -> new ServerSummary(
                        inboxKey,
                        row[0] == null ? null : ((Number) row[0]).longValue(),
                        row[1] == null ? "NAO_ATRIBUIDO" : String.valueOf(row[1]),
                        asInt(row, 2),
                        asInt(row, 3),
                        asInt(row, 4),
                        row[5] instanceof Instant instant ? instant : null,
                        serverBand(asInt(row, 2), asInt(row, 3), asInt(row, 4))))
                .toList();
    }

    private String deskBand(int active, int overdue, int blocking) {
        if (overdue >= 10 || blocking >= 8 || active >= 80) {
            return "CRITICA";
        }
        if (overdue >= 4 || blocking >= 3 || active >= 30) {
            return "PRESSAO";
        }
        return "LIVRE";
    }

    private String serverBand(int active, int overdue, int blocking) {
        if (overdue >= 8 || blocking >= 5 || active >= 30) {
            return "CRITICA";
        }
        if (overdue >= 3 || blocking >= 2 || active >= 12) {
            return "PRESSAO";
        }
        return "LIVRE";
    }

    private int asInt(Object[] row, int index) {
        if (row == null || index < 0 || index >= row.length || row[index] == null) {
            return 0;
        }
        return ((Number) row[index]).intValue();
    }

    public record BottleneckRadarSnapshot(
            String band,
            List<UnitInboxSummary> units,
            List<DeskSummary> desks,
            List<ServerSummary> servers,
            List<BottleneckIndicator> indicators,
            List<String> fundamentos,
            Map<String, Object> metrics
    ) {
    }

    public record UnitInboxSummary(
            String inboxKey,
            int active,
            int overdue,
            int expedited,
            String band
    ) {
    }

    public record DeskSummary(
            String inboxKey,
            String deskAxis,
            int active,
            int overdue,
            int blocking,
            int secrecyReviewRequired,
            int hearingSensitive,
            String band
    ) {
    }

    public record ServerSummary(
            String inboxKey,
            Long usuarioId,
            String nome,
            int active,
            int overdue,
            int blocking,
            Instant firstDueAt,
            String band
    ) {
    }

    public record BottleneckIndicator(
            String level,
            String code,
            String band,
            String rationale
    ) {
    }
}
