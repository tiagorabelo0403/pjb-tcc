package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CalendarNotificationCadencePolicyService {

    public CadenceDecision resolve(Usuario usuario,
                                   String profileCode,
                                   UserCalendarPreferenceResponse preference,
                                   CalendarWorkspaceEventDto event,
                                   Processo processo,
                                   LocalDateTime now) {
        if (event == null || event.at() == null || now == null) {
            return null;
        }
        if (!allowsLane(preference, event.laneCode())) {
            return null;
        }
        long daysUntil = ChronoUnit.DAYS.between(now.toLocalDate(), event.at().toLocalDate());
        if (daysUntil < -1) {
            return null;
        }
        String cadenceMode = normalizeCadenceMode(preference == null ? null : preference.notificationCadenceMode());
        ProfileClass profileClass = classifyProfile(usuario, profileCode);
        String activeContext = normalize(preference == null ? null : preference.selectedInstitutionContextCode());
        EventNature nature = classifyNature(event, processo);
        WindowSpec spec = baseline(event.laneCode(), event.segmentCode(), nature, profileClass, activeContext, cadenceMode, daysUntil);
        if (spec == null) {
            return null;
        }
        LocalDateTime notifyAt = event.at().minusHours(spec.hoursLead());
        return new CadenceDecision(spec.stageCode(), spec.urgency(), spec.windowLabel(), spec.hoursLead(), notifyAt, cadenceMode);
    }

    private WindowSpec baseline(String laneCode,
                                String segmentCode,
                                EventNature nature,
                                ProfileClass profileClass,
                                String activeContext,
                                String cadenceMode,
                                long daysUntil) {
        int horizonDays;
        long hoursLead;
        String stageCode;
        String urgency;
        String windowLabel;
        String lane = normalize(laneCode);
        String segment = normalize(segmentCode);
        if ("PRAZOS".equals(lane)) {
            if (nature == EventNature.EMBARGOS) {
                horizonDays = 4;
                hoursLead = daysUntil >= 2 ? 24L : daysUntil == 1 ? 8L : 2L;
                stageCode = daysUntil >= 2 ? "EMBARGOS_24H" : daysUntil == 1 ? "EMBARGOS_8H" : "EMBARGOS_HOJE";
                urgency = daysUntil <= 0 ? "CRITICA" : "ALTA";
                windowLabel = daysUntil >= 2 ? "Embargos em 24h" : daysUntil == 1 ? "Embargos em 8h" : "Embargos no mesmo dia";
            } else if (nature == EventNature.RECURSAL) {
                horizonDays = 10;
                hoursLead = daysUntil >= 5 ? 120L : daysUntil >= 2 ? 48L : daysUntil == 1 ? 12L : 3L;
                stageCode = daysUntil >= 5 ? "RECURSAL_5D" : daysUntil >= 2 ? "RECURSAL_48H" : daysUntil == 1 ? "RECURSAL_12H" : "RECURSAL_HOJE";
                urgency = daysUntil <= 0 ? "CRITICA" : daysUntil <= 1 ? "ALTA" : "MEDIA";
                windowLabel = daysUntil >= 5 ? "Recursal em 5 dias" : daysUntil >= 2 ? "Recursal em 48h" : daysUntil == 1 ? "Recursal em 12h" : "Recursal no mesmo dia";
            } else if (nature == EventNature.ELEITORAL) {
                horizonDays = 3;
                hoursLead = daysUntil >= 1 ? 12L : 2L;
                stageCode = daysUntil >= 1 ? "ELEITORAL_12H" : "ELEITORAL_HOJE";
                urgency = daysUntil <= 0 ? "CRITICA" : "ALTA";
                windowLabel = daysUntil >= 1 ? "Eleitoral em 12h" : "Eleitoral no mesmo dia";
            } else if (nature == EventNature.PENAL) {
                horizonDays = 5;
                hoursLead = daysUntil >= 2 ? 24L : daysUntil == 1 ? 6L : 2L;
                stageCode = daysUntil >= 2 ? "PENAL_24H" : daysUntil == 1 ? "PENAL_6H" : "PENAL_HOJE";
                urgency = daysUntil <= 0 ? "CRITICA" : "ALTA";
                windowLabel = daysUntil >= 2 ? "Penal em 24h" : daysUntil == 1 ? "Penal em 6h" : "Penal no mesmo dia";
            } else if (nature == EventNature.JUIZADO) {
                horizonDays = 5;
                hoursLead = daysUntil >= 2 ? 24L : daysUntil == 1 ? 4L : 2L;
                stageCode = daysUntil >= 2 ? "JUIZADO_24H" : daysUntil == 1 ? "JUIZADO_4H" : "JUIZADO_HOJE";
                urgency = daysUntil <= 0 ? "CRITICA" : "ALTA";
                windowLabel = daysUntil >= 2 ? "Juizado em 24h" : daysUntil == 1 ? "Juizado em 4h" : "Juizado no mesmo dia";
            } else {
                horizonDays = 7;
                hoursLead = daysUntil >= 3 ? 48L : daysUntil >= 1 ? 12L : 3L;
                stageCode = daysUntil >= 3 ? "PRAZO_48H" : daysUntil >= 1 ? "PRAZO_12H" : "PRAZO_HOJE";
                urgency = daysUntil <= 0 ? "CRITICA" : daysUntil == 1 ? "ALTA" : "MEDIA";
                windowLabel = daysUntil >= 3 ? "Prazo em 48h" : daysUntil >= 1 ? "Prazo em 12h" : "Prazo no mesmo dia";
            }
        } else if ("AGENDA_PROCESSUAL".equals(lane)) {
            switch (nature) {
                case MANDADO_RETORNO -> {
                    horizonDays = 3;
                    hoursLead = daysUntil >= 1 ? 8L : 2L;
                    stageCode = daysUntil >= 1 ? "MANDADO_RETORNO_8H" : "MANDADO_RETORNO_HOJE";
                    urgency = daysUntil <= 0 ? "CRITICA" : "ALTA";
                    windowLabel = daysUntil >= 1 ? "Retorno de mandado em 8h" : "Retorno de mandado no mesmo dia";
                }
                case MANDADO_TENTATIVA -> {
                    horizonDays = 4;
                    hoursLead = daysUntil >= 2 ? 24L : daysUntil == 1 ? 8L : 2L;
                    stageCode = daysUntil >= 2 ? "MANDADO_TENTATIVA_24H" : daysUntil == 1 ? "MANDADO_TENTATIVA_8H" : "MANDADO_TENTATIVA_HOJE";
                    urgency = daysUntil <= 0 ? "ALTA" : "MEDIA";
                    windowLabel = daysUntil >= 2 ? "Tentativa de mandado em 24h" : daysUntil == 1 ? "Tentativa de mandado em 8h" : "Tentativa de mandado no mesmo dia";
                }
                case MANDADO_CERTIDAO -> {
                    horizonDays = 3;
                    hoursLead = daysUntil >= 1 ? 12L : 3L;
                    stageCode = daysUntil >= 1 ? "MANDADO_CERTIDAO_12H" : "MANDADO_CERTIDAO_HOJE";
                    urgency = daysUntil <= 0 ? "ALTA" : "MEDIA";
                    windowLabel = daysUntil >= 1 ? "Certidão de mandado em 12h" : "Certidão de mandado no mesmo dia";
                }
                case PERICIA_LAUDO_PENDENTE -> {
                    horizonDays = 7;
                    hoursLead = daysUntil >= 2 ? 48L : daysUntil >= 1 ? 12L : 3L;
                    stageCode = daysUntil >= 2 ? "PERICIA_LAUDO_48H" : daysUntil >= 1 ? "PERICIA_LAUDO_12H" : "PERICIA_LAUDO_HOJE";
                    urgency = daysUntil <= 0 ? "CRITICA" : "ALTA";
                    windowLabel = daysUntil >= 2 ? "Laudo pendente em 48h" : daysUntil >= 1 ? "Laudo pendente em 12h" : "Laudo pendente no mesmo dia";
                }
                case PERICIA_HONORARIOS -> {
                    horizonDays = 7;
                    hoursLead = daysUntil >= 2 ? 48L : daysUntil >= 1 ? 12L : 4L;
                    stageCode = daysUntil >= 2 ? "PERICIA_HONORARIOS_48H" : daysUntil >= 1 ? "PERICIA_HONORARIOS_12H" : "PERICIA_HONORARIOS_HOJE";
                    urgency = daysUntil <= 0 ? "ALTA" : "MEDIA";
                    windowLabel = daysUntil >= 2 ? "Honorários periciais em 48h" : daysUntil >= 1 ? "Honorários periciais em 12h" : "Honorários periciais no mesmo dia";
                }
                case PERICIA_ACEITE, PERICIA_ENTREGA -> {
                    horizonDays = 7;
                    hoursLead = daysUntil >= 3 ? 72L : daysUntil >= 1 ? 24L : 4L;
                    stageCode = daysUntil >= 3 ? "PERICIA_72H" : daysUntil >= 1 ? "PERICIA_24H" : "PERICIA_HOJE";
                    urgency = daysUntil <= 0 ? "ALTA" : "MEDIA";
                    windowLabel = daysUntil >= 3 ? "Perícia em 72h" : daysUntil >= 1 ? "Perícia em 24h" : "Perícia no mesmo dia";
                }
                case GABINETE_VOTO -> {
                    horizonDays = 4;
                    hoursLead = daysUntil >= 2 ? 24L : daysUntil >= 1 ? 8L : 2L;
                    stageCode = daysUntil >= 2 ? "GABINETE_VOTO_24H" : daysUntil >= 1 ? "GABINETE_VOTO_8H" : "GABINETE_VOTO_HOJE";
                    urgency = daysUntil <= 0 ? "CRITICA" : "ALTA";
                    windowLabel = daysUntil >= 2 ? "Voto em 24h" : daysUntil >= 1 ? "Voto em 8h" : "Voto no mesmo dia";
                }
                case GABINETE_MINUTA, GABINETE_CONCLUSAO -> {
                    horizonDays = 5;
                    hoursLead = daysUntil >= 2 ? 48L : daysUntil >= 1 ? 12L : 3L;
                    stageCode = daysUntil >= 2 ? "GABINETE_48H" : daysUntil >= 1 ? "GABINETE_12H" : "GABINETE_HOJE";
                    urgency = daysUntil <= 0 ? "ALTA" : "MEDIA";
                    windowLabel = daysUntil >= 2 ? "Gabinete em 48h" : daysUntil >= 1 ? "Gabinete em 12h" : "Gabinete no mesmo dia";
                }
                case GABINETE_PAUTA -> {
                    horizonDays = 5;
                    hoursLead = daysUntil >= 2 ? 48L : daysUntil >= 1 ? 12L : 2L;
                    stageCode = daysUntil >= 2 ? "PAUTA_48H" : daysUntil >= 1 ? "PAUTA_12H" : "PAUTA_HOJE";
                    urgency = daysUntil <= 0 ? "ALTA" : "MEDIA";
                    windowLabel = daysUntil >= 2 ? "Pauta em 48h" : daysUntil >= 1 ? "Pauta em 12h" : "Pauta no mesmo dia";
                }
                case SECRETARIA_SLA -> {
                    horizonDays = 4;
                    hoursLead = daysUntil >= 1 ? 8L : 2L;
                    stageCode = daysUntil >= 1 ? "SECRETARIA_SLA_8H" : "SECRETARIA_SLA_HOJE";
                    urgency = daysUntil <= 0 ? "CRITICA" : "ALTA";
                    windowLabel = daysUntil >= 1 ? "SLA da secretaria em 8h" : "SLA da secretaria no mesmo dia";
                }
                case SECRETARIA_AUDIENCIA, SECRETARIA_PAUTA -> {
                    horizonDays = 4;
                    hoursLead = daysUntil >= 2 ? 24L : daysUntil >= 1 ? 8L : 2L;
                    stageCode = daysUntil >= 2 ? "SECRETARIA_24H" : daysUntil >= 1 ? "SECRETARIA_8H" : "SECRETARIA_HOJE";
                    urgency = daysUntil <= 0 ? "ALTA" : "MEDIA";
                    windowLabel = daysUntil >= 2 ? "Secretaria em 24h" : daysUntil >= 1 ? "Secretaria em 8h" : "Secretaria no mesmo dia";
                }
                case AUDIENCIA -> {
                    horizonDays = 5;
                    hoursLead = daysUntil >= 2 ? 48L : daysUntil == 1 ? 12L : 2L;
                    stageCode = daysUntil >= 2 ? "AUDIENCIA_48H" : daysUntil == 1 ? "AUDIENCIA_12H" : "AUDIENCIA_HOJE";
                    urgency = daysUntil <= 1 ? "ALTA" : "MEDIA";
                    windowLabel = daysUntil >= 2 ? "Audiência em 48h" : daysUntil == 1 ? "Audiência em 12h" : "Audiência no mesmo dia";
                }
                case MANDADO -> {
                    horizonDays = 4;
                    hoursLead = daysUntil >= 2 ? 24L : daysUntil >= 1 ? 8L : 2L;
                    stageCode = daysUntil >= 2 ? "MANDADO_24H" : daysUntil >= 1 ? "MANDADO_8H" : "MANDADO_HOJE";
                    urgency = daysUntil <= 0 ? "ALTA" : "MEDIA";
                    windowLabel = daysUntil >= 2 ? "Mandado em 24h" : daysUntil >= 1 ? "Mandado em 8h" : "Mandado no mesmo dia";
                }
                case PERICIA, GABINETE, SECRETARIA -> {
                    horizonDays = 5;
                    hoursLead = daysUntil >= 2 ? 24L : daysUntil >= 1 ? 8L : 2L;
                    stageCode = daysUntil >= 2 ? "OPERACIONAL_24H" : daysUntil >= 1 ? "OPERACIONAL_8H" : "OPERACIONAL_HOJE";
                    urgency = daysUntil <= 0 ? "ALTA" : "MEDIA";
                    windowLabel = daysUntil >= 2 ? "Operação em 24h" : daysUntil >= 1 ? "Operação em 8h" : "Operação no mesmo dia";
                }
                default -> {
                    horizonDays = 7;
                    hoursLead = daysUntil >= 3 ? 72L : daysUntil >= 1 ? 24L : 4L;
                    stageCode = daysUntil >= 3 ? "SESSAO_72H" : daysUntil >= 1 ? "SESSAO_24H" : "SESSAO_HOJE";
                    urgency = daysUntil <= 0 ? "ALTA" : "MEDIA";
                    windowLabel = daysUntil >= 3 ? "Sessão em 72h" : daysUntil >= 1 ? "Sessão em 24h" : "Sessão no mesmo dia";
                }
            }
        } else if ("PRECATORIOS".equals(lane)) {
            if (nature == EventNature.PRECATORIO_LIBERACAO) {
                horizonDays = 10;
                hoursLead = daysUntil >= 3 ? 72L : daysUntil >= 1 ? 24L : 6L;
                stageCode = daysUntil >= 3 ? "PRECATORIO_LIBERACAO_72H" : daysUntil >= 1 ? "PRECATORIO_LIBERACAO_24H" : "PRECATORIO_LIBERACAO_HOJE";
                urgency = daysUntil <= 0 ? "ALTA" : "MEDIA";
                windowLabel = daysUntil >= 3 ? "Liberação em 72h" : daysUntil >= 1 ? "Liberação em 24h" : "Liberação no mesmo dia";
            } else if (nature == EventNature.RPV) {
                horizonDays = 10;
                hoursLead = daysUntil >= 3 ? 72L : daysUntil >= 1 ? 24L : 6L;
                stageCode = daysUntil >= 3 ? "RPV_72H" : daysUntil >= 1 ? "RPV_24H" : "RPV_HOJE";
                urgency = daysUntil <= 0 ? "ALTA" : "INFORMATIVA";
                windowLabel = daysUntil >= 3 ? "RPV em 72h" : daysUntil >= 1 ? "RPV em 24h" : "RPV no mesmo dia";
            } else {
                horizonDays = 20;
                hoursLead = daysUntil >= 7 ? 120L : daysUntil >= 3 ? 72L : 24L;
                stageCode = daysUntil >= 7 ? "PRECATORIO_5D" : daysUntil >= 3 ? "PRECATORIO_72H" : "PRECATORIO_24H";
                urgency = daysUntil <= 0 ? "ALTA" : "INFORMATIVA";
                windowLabel = daysUntil >= 7 ? "Precatório em 5 dias" : daysUntil >= 3 ? "Precatório em 72h" : "Precatório em 24h";
            }
        } else {
            horizonDays = 1;
            hoursLead = 12L;
            stageCode = "PESSOAL_12H";
            urgency = "INFORMATIVA";
            windowLabel = "Lembrete pessoal";
        }
        if (profileClass.isOperational()) {
            horizonDays += 1;
            hoursLead += 6L;
        }
        if (containsAny(activeContext, "GABINETE", "ASSESSORIA") && (nature == EventNature.GABINETE_VOTO || nature == EventNature.GABINETE_MINUTA || nature == EventNature.GABINETE_CONCLUSAO)) {
            hoursLead += 6L;
            urgency = "ALTA";
        }
        if (containsAny(activeContext, "SECRETARIA", "CENTRAL_MANDADOS", "UNIDADE_POLICIAL") && (nature == EventNature.MANDADO_RETORNO || nature == EventNature.MANDADO_TENTATIVA || nature == EventNature.MANDADO_CERTIDAO)) {
            horizonDays += 1;
            hoursLead += 4L;
            urgency = "ALTA";
        }
        if (containsAny(activeContext, "PROCURADORIA") && "PRECATORIOS".equals(lane)) {
            horizonDays += 3;
            hoursLead += 12L;
        }
        if ("STRICT".equals(cadenceMode)) {
            horizonDays += 2;
            hoursLead += 12L;
            if ("INFORMATIVA".equals(urgency) && ("PRAZOS".equals(lane) || "AGENDA_PROCESSUAL".equals(lane))) {
                urgency = "MEDIA";
            }
        } else if ("ESSENTIAL".equals(cadenceMode)) {
            horizonDays = Math.max(1, horizonDays - 2);
            hoursLead = Math.max(2L, hoursLead / 2L);
            if ("PESSOAL".equals(lane)) {
                return null;
            }
        }
        if (daysUntil > horizonDays) {
            return null;
        }
        return new WindowSpec(stageCode, urgency, windowLabel, hoursLead);
    }

    private boolean allowsLane(UserCalendarPreferenceResponse preference, String laneCode) {
        if (preference == null) {
            return true;
        }
        List<String> configured = preference.notificationLaneCodes();
        if (configured == null || configured.isEmpty()) {
            return true;
        }
        String lane = normalize(laneCode);
        for (String item : configured) {
            if (Objects.equals(normalize(item), lane)) {
                return true;
            }
        }
        return false;
    }

    private ProfileClass classifyProfile(Usuario usuario, String profileCode) {
        if (usuario != null && usuario.getTipoUsuario() != null) {
            if (usuario.getTipoUsuario().isMagistratura() || usuario.getTipoUsuario().isServidorJudiciario() || usuario.getTipoUsuario().isMinisterioPublico() || usuario.getTipoUsuario().isDefensoriaPublica() || usuario.getTipoUsuario().isProcuradoria() || usuario.getTipoUsuario().isAdvocacia() || usuario.getTipoUsuario().isAuxiliarJustica() || usuario.getTipoUsuario().isSegurancaPublica()) {
                return ProfileClass.OPERATIONAL;
            }
        }
        String normalized = normalize(profileCode);
        if (normalized != null && (normalized.contains("OPERACIONAL") || normalized.contains("MAGISTRATURA") || normalized.contains("ADVOCACIA") || normalized.contains("DEFENSORIA") || normalized.contains("MINISTERIO_PUBLICO") || normalized.contains("PROCURADORIA"))) {
            return ProfileClass.OPERATIONAL;
        }
        return ProfileClass.GENERAL;
    }

    private EventNature classifyNature(CalendarWorkspaceEventDto event, Processo processo) {
        String eventType = normalize(event.eventType());
        if (containsAny(eventType, "MANDADO_JANELA_RETORNO", "MANDADO_RETORNO")) {
            return EventNature.MANDADO_RETORNO;
        }
        if (containsAny(eventType, "MANDADO_MULTI_TENTATIVA", "MANDADO_TENTATIVA", "MANDADO_ROTA")) {
            return EventNature.MANDADO_TENTATIVA;
        }
        if (containsAny(eventType, "MANDADO_CERTIDAO")) {
            return EventNature.MANDADO_CERTIDAO;
        }
        if (containsAny(eventType, "PERICIA_LAUDO_PENDENTE")) {
            return EventNature.PERICIA_LAUDO_PENDENTE;
        }
        if (containsAny(eventType, "PERICIA_HONORARIOS")) {
            return EventNature.PERICIA_HONORARIOS;
        }
        if (containsAny(eventType, "PERICIA_ENTREGA_TECNICA", "PERICIA_LAUDO")) {
            return EventNature.PERICIA_ENTREGA;
        }
        if (containsAny(eventType, "PERICIA_ACEITE", "PERICIA_NOMEACAO")) {
            return EventNature.PERICIA_ACEITE;
        }
        if (containsAny(eventType, "GABINETE_VOTO")) {
            return EventNature.GABINETE_VOTO;
        }
        if (containsAny(eventType, "GABINETE_MINUTA")) {
            return EventNature.GABINETE_MINUTA;
        }
        if (containsAny(eventType, "GABINETE_CONCLUSAO")) {
            return EventNature.GABINETE_CONCLUSAO;
        }
        if (containsAny(eventType, "GABINETE_PAUTA", "PAUTA_COLEGIADA", "PAUTA_SUSTENTACAO")) {
            return EventNature.GABINETE_PAUTA;
        }
        if (containsAny(eventType, "SECRETARIA_SLA")) {
            return EventNature.SECRETARIA_SLA;
        }
        if (containsAny(eventType, "SECRETARIA_PAUTA_INTERNA")) {
            return EventNature.SECRETARIA_PAUTA;
        }
        if (containsAny(eventType, "SECRETARIA_AUDIENCIA", "SECRETARIA_FILA_AUDIENCIA")) {
            return EventNature.SECRETARIA_AUDIENCIA;
        }
        if (containsAny(eventType, "PRECATORIO_LIBERACAO_OPERACIONAL")) {
            return EventNature.PRECATORIO_LIBERACAO;
        }
        String raw = normalize(event.laneCode(), event.segmentCode(), event.title(), event.subtitle(), event.deadlineRuleSummary(), processo == null ? null : processo.getClasseProcessual());
        if (containsAny(raw, "MANDADO", "DILIGEN", "CUMPRIMENTO")) {
            return EventNature.MANDADO;
        }
        if (containsAny(raw, "PERICIA", "PERÍCIA", "LAUDO", "VISTORIA", "EXAME TECNICO", "EXAME TÉCNICO")) {
            return EventNature.PERICIA;
        }
        if (containsAny(raw, "GABINETE", "MINUTA", "CONCLUSAO", "CONCLUSÃO", "VOTO")) {
            return EventNature.GABINETE;
        }
        if (containsAny(raw, "SECRETARIA", "CARTORIO", "CARTÓRIO", "EXPEDIENTE", "REMESSA")) {
            return EventNature.SECRETARIA;
        }
        if (containsAny(raw, "AUDIEN", "AUDIÊN")) {
            return EventNature.AUDIENCIA;
        }
        if (containsAny(raw, "EMBARGO")) {
            return EventNature.EMBARGOS;
        }
        if (containsAny(raw, "APELA", "AGRAVO", "RECURSO", "RESP", "RECURSO ESPECIAL", "RECURSO EXTRAORDINARIO", "RECURSO EXTRAORDINÁRIO", "CONTRARRAZ", "CONFLITO DE COMPETENCIA", "CONFLITO DE COMPETÊNCIA")) {
            return EventNature.RECURSAL;
        }
        if (containsAny(raw, "RPV", "PEQUENO VALOR")) {
            return EventNature.RPV;
        }
        RamoDireito ramo = processo == null ? null : processo.getRamoDireito();
        RitoProcessual rito = processo == null ? null : processo.getRito();
        if (ramo == RamoDireito.ELEITORAL || (rito != null && rito.isEleitoral())) {
            return EventNature.ELEITORAL;
        }
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR || (rito != null && rito.isPenal())) {
            return EventNature.PENAL;
        }
        if (rito != null && (rito == RitoProcessual.JUIZADO_ESPECIAL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_CIVEL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA
                || rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL)) {
            return EventNature.JUIZADO;
        }
        return EventNature.GENERAL;
    }

    private boolean containsAny(String raw, String... tokens) {
        if (raw == null || raw.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && raw.contains(token.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String... values) {
        StringBuilder builder = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    if (!builder.isEmpty()) {
                        builder.append(' ');
                    }
                    builder.append(value.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        return builder.toString();
    }

    private String normalizeCadenceMode(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return "SMART";
        }
        return switch (normalized) {
            case "STRICT", "ESSENTIAL", "SMART" -> normalized;
            default -> "SMART";
        };
    }

    private enum ProfileClass {
        GENERAL,
        OPERATIONAL;

        boolean isOperational() {
            return this == OPERATIONAL;
        }
    }

    private enum EventNature {
        GENERAL,
        RECURSAL,
        EMBARGOS,
        AUDIENCIA,
        ELEITORAL,
        PENAL,
        JUIZADO,
        RPV,
        PRECATORIO_LIBERACAO,
        MANDADO,
        MANDADO_TENTATIVA,
        MANDADO_RETORNO,
        MANDADO_CERTIDAO,
        PERICIA,
        PERICIA_ACEITE,
        PERICIA_HONORARIOS,
        PERICIA_LAUDO_PENDENTE,
        PERICIA_ENTREGA,
        GABINETE,
        GABINETE_CONCLUSAO,
        GABINETE_MINUTA,
        GABINETE_VOTO,
        GABINETE_PAUTA,
        SECRETARIA,
        SECRETARIA_AUDIENCIA,
        SECRETARIA_PAUTA,
        SECRETARIA_SLA
    }

    private record WindowSpec(String stageCode, String urgency, String windowLabel, long hoursLead) {
    }

    public record CadenceDecision(
            String stageCode,
            String urgency,
            String windowLabel,
            long hoursLead,
            LocalDateTime notifyAt,
            String cadenceMode
    ) {
    }
}
