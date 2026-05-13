package com.tcc.pjb.backend.service.juiz.session;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.tcc.pjb.backend.core.forum.routing.SecretariatInboxKeyParser;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;

@Component
public class GabineteOperationalProfileResolver {

    public GabineteOperationalProfile resolve(Usuario usuario,
                                              List<WorkItem> inbox,
                                              List<CalendarEventDto> agenda) {
        List<WorkItem> safeInbox = inbox == null ? List.of() : List.copyOf(inbox);
        List<CalendarEventDto> safeAgenda = agenda == null ? List.of() : List.copyOf(agenda);
        Instant now = Instant.now();

        int urgentItems = 0;
        int blockingItems = 0;
        int recursalItems = 0;
        int hearingItems = safeAgenda.size();
        int secrecyItems = 0;
        int minutaItems = 0;
        int conclusos = 0;

        for (WorkItem item : safeInbox) {
            if (isUrgent(item, now)) {
                urgentItems++;
            }
            if (item != null && item.isBlocking()) {
                blockingItems++;
            }
            if (matches(item, "RECURSO", "APEL", "AGRAVO", "RESP", "EXTRAORD", "CONTRARRAZ")) {
                recursalItems++;
            }
            if (matches(item, "AUDIENCIA", "SESSAO", "PAUTA")) {
                hearingItems++;
            }
            if (matches(item, "SIGILO", "SEGREDO", "RESTRITO", "CREDENCIAL")) {
                secrecyItems++;
            }
            if (matches(item, "MINUTA", "RASCUNHO", "DRAFT", "MEMORIAL")) {
                minutaItems++;
            }
            if (matches(item, "CONCLUSO", "CONCLUSOS", "INSTRUCAO_CONCLUIDA", "SENTENCA", "DESPACHO", "DECISAO")) {
                conclusos++;
            }
        }

        TopologicalBase base = resolveTopologicalBase(usuario, safeInbox);
        String decisionDesk = "GABINETE_DECISORIO_" + base.compactKey();
        String advisoryDesk = recursalItems > Math.max(3, safeInbox.size() / 5)
                ? "ASSESSORIA_RECURSAL_" + base.compactKey()
                : hearingItems > Math.max(4, safeInbox.size() / 4)
                ? "ASSESSORIA_AUDIENCIA_" + base.compactKey()
                : "ASSESSORIA_MINUTAS_" + base.compactKey();
        String recursalSupportDesk = recursalItems > 0 ? "NUCLEO_RECURSAL_" + base.compactKey() : "SUPORTE_PREVENTIVO_" + base.compactKey();
        String hearingDesk = hearingItems > 0 ? "COORD_AUDIENCIA_" + base.compactKey() : "COORD_GABINETE_" + base.compactKey();
        String coordinationDesk = blockingItems > 0 || secrecyItems > 0 ? "COORD_CONTROLE_" + base.compactKey() : "COORD_FLUXO_" + base.compactKey();
        String sessionChannel = recursalItems > 0 ? "SESSAO_RECURSAL" : hearingItems > 0 ? "AGENDA_AUDIENCIA" : "MESA_DECISORIA";
        String loadBand = resolveLoadBand(safeInbox.size(), urgentItems, blockingItems, recursalItems);
        String coordinationMode = resolveCoordinationMode(urgentItems, blockingItems, recursalItems, hearingItems, secrecyItems, minutaItems, conclusos);

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(loadBand);
        labels.add(coordinationMode);
        labels.add(sessionChannel);
        if (recursalItems > 0) {
            labels.add("RECURSAL_ACTIVE");
        }
        if (hearingItems > 0) {
            labels.add("AUDIENCIA_ACTIVE");
        }
        if (secrecyItems > 0) {
            labels.add("SIGILO_REFORCADO");
        }
        if (blockingItems > 0) {
            labels.add("BLOCKING_PRESENT");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("territorialBase", base.territorialBase());
        metadata.put("topologicalBase", base.compactKey());
        metadata.put("gabineteKey", "GABINETE_" + base.compactKey());
        metadata.put("tribunalCodigo", base.organCode());
        metadata.put("instanciaAxis", base.instanceAxis());
        metadata.put("laneAxis", base.laneAxis());
        metadata.put("inboxSize", safeInbox.size());
        metadata.put("minutaItems", minutaItems);
        metadata.put("conclusos", conclusos);
        metadata.put("hearingAgendaSize", safeAgenda.size());
        metadata.put("recursalDensity", density(recursalItems, safeInbox.size()));
        metadata.put("urgencyDensity", density(urgentItems, safeInbox.size()));
        metadata.put("blockingDensity", density(blockingItems, safeInbox.size()));
        metadata.put("secrecyDensity", density(secrecyItems, safeInbox.size()));
        metadata.put("descriptor", decisionDesk + ':' + advisoryDesk + ':' + sessionChannel);

        return new GabineteOperationalProfile(
                decisionDesk,
                advisoryDesk,
                recursalSupportDesk,
                hearingDesk,
                coordinationDesk,
                sessionChannel,
                loadBand,
                coordinationMode,
                urgentItems,
                blockingItems,
                recursalItems,
                hearingItems,
                secrecyItems,
                List.copyOf(labels),
                metadata
        );
    }

    private static boolean isUrgent(WorkItem item, Instant now) {
        if (item == null) {
            return false;
        }
        if (item.getPrioridade() != null && item.getPrioridade() <= 1) {
            return true;
        }
        return item.getDueAt() != null && !item.getDueAt().isAfter(now.plus(24, ChronoUnit.HOURS));
    }

    private static boolean matches(WorkItem item, String... tokens) {
        if (item == null || tokens == null || tokens.length == 0) {
            return false;
        }
        String source = ((item.getTitulo() == null ? "" : item.getTitulo()) + ' '
                + (item.getDescricao() == null ? "" : item.getDescricao()) + ' '
                + (item.getQueueCode() == null ? "" : item.getQueueCode()) + ' '
                + (item.getInboxKey() == null ? "" : item.getInboxKey()))
                .toUpperCase(Locale.ROOT);
        for (String token : tokens) {
            if (token != null && !token.isBlank() && source.contains(token.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static TopologicalBase resolveTopologicalBase(Usuario usuario, List<WorkItem> inbox) {
        String defaultUf = usuario != null && usuario.getUf() != null && !usuario.getUf().isBlank()
                ? usuario.getUf().trim().toUpperCase(Locale.ROOT)
                : "XX";
        String defaultComarca = usuario != null && usuario.getComarca() != null && !usuario.getComarca().isBlank()
                ? usuario.getComarca().trim().toUpperCase(Locale.ROOT).replace(' ', '_')
                : "BASE";
        for (WorkItem item : inbox) {
            if (item == null || item.getInboxKey() == null || item.getInboxKey().isBlank()) {
                continue;
            }
            Optional<SecretariatInboxKeyParser.Parts> parsed = parseGabineteOrSecretariaInbox(item.getInboxKey());
            if (parsed.isPresent()) {
                SecretariatInboxKeyParser.Parts parts = parsed.get();
                String organ = normalize(firstNonBlank(parts.org(), "ORG"));
                String instance = normalize(firstNonBlank(parts.instance(), "1G"));
                String lane = normalize(firstNonBlank(parts.lane(), "COM"));
                String uf = normalize(firstNonBlank(parts.uf(), defaultUf));
                String comarca = normalize(firstNonBlank(parts.comarca(), defaultComarca));
                return new TopologicalBase(organ, instance, lane, uf, comarca);
            }
        }
        return new TopologicalBase("ORG", "1G", "COM", normalize(defaultUf), normalize(defaultComarca));
    }

    private static Optional<SecretariatInboxKeyParser.Parts> parseGabineteOrSecretariaInbox(String inboxKey) {
        if (inboxKey == null || inboxKey.isBlank()) {
            return Optional.empty();
        }
        String normalized = inboxKey.trim();
        if (normalized.startsWith("GAB:")) {
            return SecretariatInboxKeyParser.parse("SEC:" + normalized.substring(4));
        }
        if (normalized.startsWith("SEC:")) {
            return SecretariatInboxKeyParser.parse(normalized);
        }
        return Optional.empty();
    }

    private static String firstNonBlank(String... values) {
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

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "BASE";
        }
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
    }

    private record TopologicalBase(String organCode, String instanceAxis, String laneAxis, String uf, String comarca) {
        String compactKey() {
            return organCode + '_' + instanceAxis + '_' + laneAxis + '_' + uf + '_' + comarca;
        }

        String territorialBase() {
            return uf + '_' + comarca;
        }
    }

    private static String resolveLoadBand(int total, int urgent, int blocking, int recursal) {
        if (total >= 40 || urgent >= 12 || blocking >= 8) {
            return "CRITICA";
        }
        if (total >= 24 || urgent >= 6 || recursal >= 5) {
            return "PRESSIONADA";
        }
        if (total >= 10) {
            return "ESTAVEL";
        }
        return "LEVE";
    }

    private static String resolveCoordinationMode(int urgent,
                                                  int blocking,
                                                  int recursal,
                                                  int hearing,
                                                  int secrecy,
                                                  int minuta,
                                                  int conclusos) {
        if (secrecy > 0 && blocking > 0) {
            return "CONTROLE_RESTRITO";
        }
        if (urgent >= 6) {
            return "CELERIDADE_DECISORIA";
        }
        if (recursal >= 4) {
            return "REFORCO_RECURSAL";
        }
        if (hearing >= 6) {
            return "ORQUESTRACAO_AUDIENCIA";
        }
        if (minuta > conclusos) {
            return "PULMAO_MINUTAS";
        }
        return "FLUXO_PADRAO";
    }

    private static double density(int numerator, int denominator) {
        if (denominator <= 0 || numerator <= 0) {
            return 0D;
        }
        return Math.min(1D, ((double) numerator) / (double) denominator);
    }
}
