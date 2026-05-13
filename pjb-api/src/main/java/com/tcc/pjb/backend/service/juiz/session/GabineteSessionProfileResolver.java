package com.tcc.pjb.backend.service.juiz.session;

import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.juiz.CollegiatePublicationReviewResolver;
import com.tcc.pjb.backend.service.juiz.CollegiatePublicationReviewProfile;
import com.tcc.pjb.backend.service.juiz.CollegiateSessionGovernanceProfile;
import com.tcc.pjb.backend.service.juiz.CollegiateSessionGovernanceResolver;

@Component
public class GabineteSessionProfileResolver {

    private final CollegiateSessionGovernanceResolver collegiateSessionGovernanceResolver;
    private final CollegiatePublicationReviewResolver publicationReviewResolver;

    public GabineteSessionProfileResolver(CollegiateSessionGovernanceResolver collegiateSessionGovernanceResolver,
                                          CollegiatePublicationReviewResolver publicationReviewResolver) {
        this.collegiateSessionGovernanceResolver = collegiateSessionGovernanceResolver;
        this.publicationReviewResolver = publicationReviewResolver;
    }

    public GabineteSessionProfile resolve(Usuario usuario,
                                          List<WorkItem> inbox,
                                          List<CalendarEventDto> agenda,
                                          GabineteOperationalProfile operationalProfile) {
        List<WorkItem> safeInbox = inbox == null ? List.of() : List.copyOf(inbox);
        List<CalendarEventDto> safeAgenda = agenda == null ? List.of() : List.copyOf(agenda);
        String base = resolveBase(usuario, operationalProfile);

        int colegiadoEvents = 0;
        int hearingEvents = 0;
        int morningEvents = 0;
        int afternoonEvents = 0;
        int fridayEvents = 0;
        int recursalDrafts = 0;
        int sessionSensitiveItems = 0;

        for (CalendarEventDto event : safeAgenda) {
            if (event == null || event.at() == null) {
                continue;
            }
            String source = ((event.title() == null ? "" : event.title()) + ' ' + (event.eventType() == null ? "" : event.eventType()))
                    .toUpperCase(Locale.ROOT);
            if (containsAny(source, "SESSAO", "JULGAMENTO", "PAUTA", "TURMA", "CAMARA", "PLENARIO")) {
                colegiadoEvents++;
            }
            if (containsAny(source, "AUDIENCIA", "INSTRUCAO", "CONCILIACAO", "UNA")) {
                hearingEvents++;
            }
            if (event.at().getHour() < 12) {
                morningEvents++;
            } else {
                afternoonEvents++;
            }
            if (event.at().getDayOfWeek() == DayOfWeek.FRIDAY) {
                fridayEvents++;
            }
        }

        for (WorkItem item : safeInbox) {
            String source = workItemSource(item);
            if (containsAny(source, "MINUTA", "VOTO", "RELATORIO", "EMENTA", "ACORDAO", "CONTRARRAZ", "RECURSO")) {
                recursalDrafts++;
            }
            if (containsAny(source, "PAUTA", "SESSAO", "AUDIENCIA", "URGENTE", "HC", "LIMINAR")) {
                sessionSensitiveItems++;
            }
        }

        boolean colegiadoActive = colegiadoEvents > 0 || recursalDrafts >= 3 || (operationalProfile != null && operationalProfile.recursalItems() >= 4);
        CollegiateSessionGovernanceProfile governanceProfile = collegiateSessionGovernanceResolver.resolve(usuario, safeInbox, safeAgenda, operationalProfile);
        CollegiatePublicationReviewProfile publicationReviewProfile = publicationReviewResolver.resolve(
                governanceProfile,
                operationalProfile,
                colegiadoActive,
                recursalDrafts,
                sessionSensitiveItems
        );
        String sessionDesk = colegiadoActive ? "SESSAO_COLEGIADA_" + base : "AGENDA_JUDICIAL_" + base;
        String sessionSecretariatDesk = colegiadoActive ? "SECRETARIA_PAUTA_" + base : "SECRETARIA_AGENDA_" + base;
        String draftingDesk = recursalDrafts >= 3 ? "NUCLEO_MINUTA_RECURSAL_" + base : "NUCLEO_MINUTA_DECISORIA_" + base;
        String hearingSupportDesk = hearingEvents > 0 ? "SUPORTE_AUDIENCIA_" + base : "SUPORTE_PAUTA_" + base;
        String hearingWindow = morningEvents > afternoonEvents ? "JANELA_MATUTINA" : afternoonEvents > 0 ? "JANELA_VESPERTINA" : "JANELA_PADRAO";
        String sessionCadence = colegiadoEvents >= 3 || fridayEvents >= 2 ? "CADENCIA_INTENSA" : colegiadoEvents > 0 ? "CADENCIA_PROGRAMADA" : "CADENCIA_SOB_DEMANDA";
        String colegiadoChannel = colegiadoActive ? "COLEGIADO_GABINETE" : hearingEvents > 0 ? "AUDIENCIA_SECRETARIA" : "DESPACHO_MONOCRATICO";
        String escalationMode = sessionSensitiveItems >= 6 ? "ESCALONAMENTO_IMEDIATO"
                : colegiadoEvents > 0 ? "ESCALONAMENTO_PAUTA"
                : hearingEvents > 0 ? "ESCALONAMENTO_AUDIENCIA"
                : "ESCALONAMENTO_PADRAO";
        boolean requiresClerkReinforcement = recursalDrafts >= 4 || colegiadoEvents >= 2 || safeAgenda.size() >= 8;
        boolean requiresSessionReview = sessionSensitiveItems >= 4 || colegiadoEvents > 0;
        sessionSecretariatDesk = firstNonBlank(publicationReviewProfile.publicationDesk(), governanceProfile.publicationDesk(), sessionSecretariatDesk);
        colegiadoChannel = firstNonBlank(governanceProfile.sessionRoom(), colegiadoChannel);
        sessionCadence = firstNonBlank(metadataString(governanceProfile, "sessionTopology.cadenceHint"), metadataString(governanceProfile, "panelComposition.voteCollectionMode"), sessionCadence);

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(sessionCadence);
        labels.add(colegiadoChannel);
        labels.add(escalationMode);
        labels.add(hearingWindow);
        labels.addAll(governanceProfile.labels());
        labels.addAll(publicationReviewProfile.labels());
        if (governanceProfile.deliberationMode() != null) {
            labels.add(governanceProfile.deliberationMode());
        }
        if (governanceProfile.judgmentSequence() != null) {
            labels.add(governanceProfile.judgmentSequence());
        }
        if (colegiadoActive) {
            labels.add("COLEGIADO_ACTIVE");
        }
        if (requiresClerkReinforcement) {
            labels.add("CLERK_REINFORCEMENT");
        }
        if (requiresSessionReview) {
            labels.add("SESSION_REVIEW");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("topologicalBase", metadataString(operationalProfile, "topologicalBase"));
        metadata.put("tribunalCodigo", metadataString(operationalProfile, "tribunalCodigo"));
        metadata.put("instanciaAxis", metadataString(operationalProfile, "instanciaAxis"));
        metadata.put("laneAxis", metadataString(operationalProfile, "laneAxis"));
        metadata.put("colegiadoEvents", colegiadoEvents);
        metadata.put("hearingEvents", hearingEvents);
        metadata.put("morningEvents", morningEvents);
        metadata.put("afternoonEvents", afternoonEvents);
        metadata.put("fridayEvents", fridayEvents);
        metadata.put("recursalDrafts", recursalDrafts);
        metadata.put("sessionSensitiveItems", sessionSensitiveItems);
        metadata.put("agendaSize", safeAgenda.size());
        metadata.putAll(governanceProfile.toMap());
        metadata.put("publicationReview", publicationReviewProfile.toMap());
        metadata.put("governanceDescriptor", governanceProfile.descriptor());
        metadata.put("panelCompositionDescriptor", metadataString(governanceProfile, "panelComposition.descriptor"));
        metadata.put("deliberationDescriptor", metadataString(governanceProfile, "deliberationCycle.descriptor"));
        metadata.put("publicationReviewDescriptor", publicationReviewProfile.descriptor());
        metadata.put("descriptor", sessionDesk + ':' + draftingDesk + ':' + sessionCadence);

        return new GabineteSessionProfile(
                sessionDesk,
                sessionSecretariatDesk,
                draftingDesk,
                hearingSupportDesk,
                hearingWindow,
                sessionCadence,
                colegiadoChannel,
                governanceProfile.chamberLabel(),
                governanceProfile.relatoriaDesk(),
                publicationReviewProfile.publicationDesk(),
                governanceProfile.sessionRoom(),
                governanceProfile.quorumLabel(),
                governanceProfile.publicationMode(),
                governanceProfile.deliberationMode(),
                governanceProfile.reviewerDesk(),
                governanceProfile.divergenceDesk(),
                governanceProfile.voteAuditDesk(),
                governanceProfile.proclamationDesk(),
                governanceProfile.judgmentSequence(),
                escalationMode,
                requiresClerkReinforcement,
                requiresSessionReview,
                List.copyOf(labels),
                metadata
        );
    }


    private static String metadataString(CollegiateSessionGovernanceProfile profile, String dottedPath) {
        if (profile == null || profile.metadata() == null || dottedPath == null || dottedPath.isBlank()) {
            return null;
        }
        Object current = profile.metadata();
        for (String token : dottedPath.split("\\.")) {
            if (!(current instanceof java.util.Map<?, ?> map)) {
                return null;
            }
            current = map.get(token);
            if (current == null) {
                return null;
            }
        }
        return String.valueOf(current);
    }

    private static boolean containsAny(String source, String... tokens) {
        if (source == null || source.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && source.contains(token.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String workItemSource(WorkItem item) {
        if (item == null) {
            return "";
        }
        return ((item.getTitulo() == null ? "" : item.getTitulo()) + ' '
                + (item.getDescricao() == null ? "" : item.getDescricao()) + ' '
                + (item.getQueueCode() == null ? "" : item.getQueueCode()) + ' '
                + (item.getInboxKey() == null ? "" : item.getInboxKey()))
                .toUpperCase(Locale.ROOT);
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

    private static String resolveBase(Usuario usuario, GabineteOperationalProfile operationalProfile) {
        String topologicalBase = metadataString(operationalProfile, "topologicalBase");
        if (topologicalBase != null && !topologicalBase.isBlank()) {
            return topologicalBase.trim();
        }
        String uf = usuario != null && usuario.getUf() != null && !usuario.getUf().isBlank()
                ? usuario.getUf().trim().toUpperCase(Locale.ROOT)
                : "XX";
        String comarca = usuario != null && usuario.getComarca() != null && !usuario.getComarca().isBlank()
                ? usuario.getComarca().trim().toUpperCase(Locale.ROOT).replace(' ', '_')
                : "BASE";
        return uf + '_' + comarca;
    }

    private static String metadataString(GabineteOperationalProfile profile, String key) {
        if (profile == null || profile.metadata() == null || key == null || key.isBlank()) {
            return null;
        }
        Object value = profile.metadata().get(key);
        return value == null ? null : String.valueOf(value);
    }
}
