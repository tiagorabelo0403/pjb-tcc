package com.tcc.pjb.backend.service.juiz;

import com.tcc.pjb.backend.core.processual.routing.TribunalInternalOrganCatalog;
import com.tcc.pjb.backend.core.processual.routing.TribunalInternalOrganProfile;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.juiz.session.GabineteOperationalProfile;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CollegiateSessionGovernanceResolver {

    private final TribunalInternalOrganCatalog tribunalInternalOrganCatalog;

    public CollegiateSessionGovernanceResolver(TribunalInternalOrganCatalog tribunalInternalOrganCatalog) {
        this.tribunalInternalOrganCatalog = tribunalInternalOrganCatalog;
    }

    public CollegiateSessionGovernanceProfile resolve(Usuario usuario,
                                                      List<WorkItem> inbox,
                                                      List<CalendarEventDto> agenda,
                                                      GabineteOperationalProfile operationalProfile) {
        String source = buildSource(usuario, inbox, agenda, operationalProfile);
        String tribunal = inferTribunal(source, usuario);
        TipoJustica tipoJustica = inferTipoJustica(source, tribunal);
        GrauJurisdicao grau = inferGrau(tribunal, source);
        String axis = inferAxis(source);
        String orgao = inferOrgao(source, tribunal);

        TribunalInternalOrganProfile profile = tribunalInternalOrganCatalog.resolve(tribunal, tipoJustica, grau, orgao, axis);
        String chamberLabel = profile.effectiveSpecificOrgan(firstNonBlank(orgao, "COLEGIADO_GERAL"));
        String relatoriaDesk = profile.effectiveGabineteDesk("RELATORIA_" + normalize(axis, "GERAL"));
        String publicationDesk = firstNonBlank(profile.effectiveSecretariatDesk(null), sessionTopologyValue(profile, "sessionSecretariatDesk"), "PUBLICACAO_" + normalize(tribunal, "TRIBUNAL"));
        String sessionRoom = normalize(firstNonBlank(sessionTopologyValue(profile, "sessionBlock"), profile.effectiveSessionChannel("SESSAO_COLEGIADA")), "SESSAO") + "_ROOM";
        String quorumLabel = firstNonBlank(panelCompositionValue(profile, "panelCompositionLabel"), profile.quorumHint(), sessionTopologyValue(profile, "panelSizeHint"), "QUORUM_REGIMENTAL");
        String publicationMode = firstNonBlank(panelCompositionValue(profile, "publicationSequence"), sessionTopologyValue(profile, "publicationFlow"), chamberLabel.contains("PLENARIO")
                ? "PUBLICACAO_PLENARIA_COORDENADA"
                : chamberLabel.contains("SECAO")
                ? "PUBLICACAO_SECAO_PRIORIZADA"
                : "PUBLICACAO_TURMA_CAMARA");
        String deliberationMode = deliberationValue(profile, "deliberationMode");
        String reviewerDesk = deliberationValue(profile, "reviewerDesk");
        String divergenceDesk = deliberationValue(profile, "divergenceDesk");
        String voteAuditDesk = deliberationValue(profile, "voteAuditDesk");
        String proclamationDesk = deliberationValue(profile, "proclamationDesk");
        String judgmentSequence = deliberationValue(profile, "judgmentSequence");

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(chamberLabel);
        labels.add(quorumLabel);
        labels.add(publicationMode);
        if (sessionTopologyValue(profile, "cadenceHint") != null) {
            labels.add(sessionTopologyValue(profile, "cadenceHint"));
        }
        if (panelCompositionValue(profile, "voteCollectionMode") != null) {
            labels.add(panelCompositionValue(profile, "voteCollectionMode"));
        }
        if ("true".equalsIgnoreCase(sessionTopologyValue(profile, "virtualSessionEligible"))) {
            labels.add("VIRTUAL_SESSION_ELIGIBLE");
        }
        if (deliberationMode != null) {
            labels.add(deliberationMode);
        }
        if (judgmentSequence != null) {
            labels.add(judgmentSequence);
        }
        labels.addAll(profile.fundamentos());

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(profile.toMap());
        metadata.put("tribunalInferido", tribunal);
        metadata.put("tipoJustica", tipoJustica == null ? null : tipoJustica.name());
        metadata.put("grau", grau == null ? null : grau.name());
        metadata.put("axis", axis);
        metadata.put("orgaoInferido", orgao);
        metadata.put("descriptor", chamberLabel + ':' + relatoriaDesk + ':' + sessionRoom);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new CollegiateSessionGovernanceProfile(
                chamberLabel,
                relatoriaDesk,
                publicationDesk,
                sessionRoom,
                quorumLabel,
                publicationMode,
                deliberationMode,
                reviewerDesk,
                divergenceDesk,
                voteAuditDesk,
                proclamationDesk,
                judgmentSequence,
                List.copyOf(labels),
                metadata
        );
    }

    private static String buildSource(Usuario usuario,
                                      List<WorkItem> inbox,
                                      List<CalendarEventDto> agenda,
                                      GabineteOperationalProfile operationalProfile) {
        StringBuilder builder = new StringBuilder();
        if (usuario != null) {
            builder.append(' ').append(firstNonBlank(usuario.getUf(), ""));
            builder.append(' ').append(firstNonBlank(usuario.getComarca(), ""));
            builder.append(' ').append(firstNonBlank(usuario.getPerfil(), ""));
            builder.append(' ').append(usuario.getTipoUsuario() == null ? "" : usuario.getTipoUsuario().name());
        }
        if (inbox != null) {
            for (WorkItem item : inbox) {
                if (item == null) continue;
                builder.append(' ').append(firstNonBlank(item.getTitulo(), ""));
                builder.append(' ').append(firstNonBlank(item.getDescricao(), ""));
                builder.append(' ').append(firstNonBlank(item.getQueueCode(), ""));
            }
        }
        if (agenda != null) {
            for (CalendarEventDto event : agenda) {
                if (event == null) continue;
                builder.append(' ').append(firstNonBlank(event.title(), ""));
                builder.append(' ').append(firstNonBlank(event.eventType(), ""));
                builder.append(' ').append(firstNonBlank(event.detailsUrl(), ""));
            }
        }
        if (operationalProfile != null) {
            builder.append(' ').append(firstNonBlank(operationalProfile.sessionChannel(), ""));
            builder.append(' ').append(firstNonBlank(operationalProfile.recursalSupportDesk(), ""));
        }
        return builder.toString().toUpperCase(Locale.ROOT);
    }

    private static String inferTribunal(String source, Usuario usuario) {
        if (containsAny(source, "STF")) return "STF";
        if (containsAny(source, "STJ")) return "STJ";
        if (containsAny(source, "TST")) return "TST";
        if (containsAny(source, "TSE")) return "TSE";
        if (containsAny(source, "STM")) return "STM";
        if (containsAny(source, "TRF1", "TRF2", "TRF3", "TRF4", "TRF5", "TRF6")) return source.replaceAll(".*(TRF[1-6]).*", "$1");
        if (containsAny(source, "TRT1", "TRT2", "TRT3", "TRT4", "TRT5", "TRT6", "TRT7", "TRT8", "TRT9", "TRT10", "TRT11", "TRT12", "TRT13", "TRT14", "TRT15", "TRT16", "TRT17", "TRT18", "TRT19", "TRT20", "TRT21", "TRT22", "TRT23", "TRT24")) return source.replaceAll(".*(TRT[0-9]{1,2}).*", "$1");
        if (containsAny(source, "TRE")) return "TRE";
        if (containsAny(source, "TJM")) return "TJM";
        if (containsAny(source, "TJ")) return "TJ";
        String uf = usuario != null ? firstNonBlank(usuario.getUf(), null) : null;
        return uf == null ? "TJ" : "TJ" + uf.trim().toUpperCase(Locale.ROOT);
    }

    private static TipoJustica inferTipoJustica(String source, String tribunal) {
        if (containsAny(source, "ELEITORAL", "TRE", "TSE")) return TipoJustica.ELEITORAL;
        if (containsAny(source, "TRABALH", "TRT", "TST")) return TipoJustica.TRABALHO;
        if (containsAny(source, "MILITAR", "STM", "TJM", "AUDITORIA")) return containsAny(source, "STM") ? TipoJustica.MILITAR_FEDERAL : TipoJustica.MILITAR_ESTADUAL;
        if (containsAny(source, "FEDERAL", "TRF", "PREVIDENCI", "FAZENDA NACIONAL")) return TipoJustica.FEDERAL;
        if (tribunal != null && List.of("STF", "STJ", "TST", "TSE", "STM").contains(tribunal)) return TipoJustica.SUPERIOR;
        return TipoJustica.ESTADUAL;
    }

    private static GrauJurisdicao inferGrau(String tribunal, String source) {
        if (tribunal != null && List.of("STF", "STJ", "TST", "TSE", "STM").contains(tribunal)) {
            return GrauJurisdicao.SUPERIOR;
        }
        return containsAny(source, "CAMARA", "TURMA", "SECAO", "PLENARIO", "COLEGIADO") ? GrauJurisdicao.SEGUNDO_GRAU : GrauJurisdicao.PRIMEIRO_GRAU;
    }

    private static String inferAxis(String source) {
        if (containsAny(source, "PENAL", "HC", "HABEAS")) return "PENAL";
        if (containsAny(source, "PREVIDENCI")) return "PREVIDENCIARIO";
        if (containsAny(source, "TRIBUT", "FAZENDA")) return "TRIBUTARIO_PUBLICO";
        if (containsAny(source, "EMPRESAR", "FALENCIA", "RECUPERACAO")) return "EMPRESARIAL";
        if (containsAny(source, "FAMILIA", "SUCESSOES")) return "FAMILIA_SUCESSOES";
        if (containsAny(source, "ELEITORAL")) return "ELEITORAL";
        if (containsAny(source, "TRABALH")) return "TRABALHISTA";
        return "CIVEL_PUBLICO";
    }

    private static String inferOrgao(String source, String tribunal) {
        if (containsAny(source, "PLENARIO", "PLENO")) return "PLENARIO_" + normalize(tribunal, "TRIBUNAL");
        if (containsAny(source, "SECAO", "SDI", "SDC")) return "SECAO_" + normalize(tribunal, "TRIBUNAL");
        if (containsAny(source, "TURMA")) return "TURMA_" + normalize(tribunal, "TRIBUNAL");
        if (containsAny(source, "CAMARA")) return "CAMARA_" + normalize(tribunal, "TRIBUNAL");
        return "COLEGIADO_" + normalize(tribunal, "TRIBUNAL");
    }


    @SuppressWarnings("unchecked")
    private static String sessionTopologyValue(TribunalInternalOrganProfile profile, String key) {
        if (profile == null || profile.metadata() == null || key == null || key.isBlank()) {
            return null;
        }
        Object topology = profile.metadata().get("sessionTopology");
        if (!(topology instanceof java.util.Map<?, ?> map)) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }


    private static String panelCompositionValue(TribunalInternalOrganProfile profile, String key) {
        if (profile == null || profile.metadata() == null || key == null || key.isBlank()) {
            return null;
        }
        Object topology = profile.metadata().get("panelComposition");
        if (!(topology instanceof java.util.Map<?, ?> map)) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static String deliberationValue(TribunalInternalOrganProfile profile, String key) {
        if (profile == null || profile.metadata() == null || key == null || key.isBlank()) {
            return null;
        }
        Object cycle = profile.metadata().get("deliberationCycle");
        if (!(cycle instanceof java.util.Map<?, ?> map)) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
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

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? fallback : normalized;
    }
}
