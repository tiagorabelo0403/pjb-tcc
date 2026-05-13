package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CalendarAudienceProfileService {

    public CalendarProfile resolve(Usuario usuario) {
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        CalendarProfileSeed seed = resolveSeed(tipo);
        return new CalendarProfile(
                seed.code,
                seed.title,
                seed.highlightLane == null ? null : seed.highlightLane.code(),
                seed.visibleLanes.stream().map(CalendarLanePolicy::code).toList(),
                seed.pinnedLanes.stream().map(CalendarLanePolicy::code).toList(),
                prazoTracks(seed),
                colorLegend(),
                suporteEventoPessoal(tipo)
        );
    }

    public record CalendarProfile(
            String profileCode,
            String profileTitle,
            String highlightLaneCode,
            List<String> visibleLaneCodes,
            List<String> pinnedLaneCodes,
            List<DeadlineTrack> prazoTracks,
            List<ColorLegend> colorLegend,
            boolean personalEventsEnabled
    ) {
    }

    public record DeadlineTrack(
            String trackCode,
            String trackTitle,
            String regime,
            String summary,
            boolean highlighted
    ) {
    }

    public record ColorLegend(
            String colorCode,
            String label,
            String meaning
    ) {
    }

    public enum CalendarLanePolicy {
        PRAZOS("PRAZOS", "Prazos", "AMBER"),
        PRECATORIOS("PRECATORIOS", "Precatórios e RPV", "PURPLE"),
        AGENDA_PROCESSUAL("AGENDA_PROCESSUAL", "Agenda processual", "BLUE"),
        PESSOAL("PESSOAL", "Pessoal", "GREEN");

        private final String code;
        private final String title;
        private final String accentColor;

        CalendarLanePolicy(String code, String title, String accentColor) {
            this.code = code;
            this.title = title;
            this.accentColor = accentColor;
        }

        public String code() {
            return code;
        }

        public String title() {
            return title;
        }

        public String accentColor() {
            return accentColor;
        }
    }

    private record CalendarProfileSeed(
            String code,
            String title,
            CalendarLanePolicy highlightLane,
            LinkedHashSet<CalendarLanePolicy> visibleLanes,
            LinkedHashSet<CalendarLanePolicy> pinnedLanes,
            EnumSet<TrackPolicy> tracks
    ) {
    }

    private enum TrackPolicy {
        CPC_GERAL,
        CPC_RECURSAL,
        JUIZADOS,
        TRABALHISTA,
        ELEITORAL,
        PENAL,
        PRECATORIOS
    }

    private CalendarProfileSeed resolveSeed(TipoUsuario tipo) {
        if (tipo == null) {
            return genericProfile("EXTERNO_GERAL", "Agenda geral externa", CalendarLanePolicy.AGENDA_PROCESSUAL,
                    Set.of(TrackPolicy.CPC_GERAL, TrackPolicy.JUIZADOS));
        }
        if (tipo.isMagistratura() || tipo.isAssessor() || tipo.isServidorJudiciario()) {
            return genericProfile("MAGISTRATURA_OPERACIONAL", titleFor(tipo), CalendarLanePolicy.AGENDA_PROCESSUAL,
                    Set.of(TrackPolicy.CPC_GERAL, TrackPolicy.CPC_RECURSAL, TrackPolicy.JUIZADOS, TrackPolicy.TRABALHISTA,
                            TrackPolicy.ELEITORAL, TrackPolicy.PENAL, TrackPolicy.PRECATORIOS));
        }
        if (tipo.isMinisterioPublico()) {
            return genericProfile("MINISTERIO_PUBLICO_OPERACIONAL", titleFor(tipo), CalendarLanePolicy.PRAZOS,
                    Set.of(TrackPolicy.CPC_GERAL, TrackPolicy.CPC_RECURSAL, TrackPolicy.PENAL, TrackPolicy.ELEITORAL,
                            TrackPolicy.TRABALHISTA, TrackPolicy.PRECATORIOS));
        }
        if (tipo.isDefensoriaPublica()) {
            return genericProfile("DEFENSORIA_OPERACIONAL", titleFor(tipo), CalendarLanePolicy.PRAZOS,
                    Set.of(TrackPolicy.CPC_GERAL, TrackPolicy.CPC_RECURSAL, TrackPolicy.JUIZADOS, TrackPolicy.PENAL,
                            TrackPolicy.TRABALHISTA, TrackPolicy.PRECATORIOS));
        }
        if (tipo.isProcuradoria()) {
            return genericProfile("PROCURADORIA_OPERACIONAL", titleFor(tipo), CalendarLanePolicy.PRECATORIOS,
                    Set.of(TrackPolicy.CPC_GERAL, TrackPolicy.CPC_RECURSAL, TrackPolicy.TRABALHISTA, TrackPolicy.PRECATORIOS));
        }
        if (tipo.isAdvocacia()) {
            return genericProfile("ADVOCACIA_OPERACIONAL", titleFor(tipo), CalendarLanePolicy.PRAZOS,
                    Set.of(TrackPolicy.CPC_GERAL, TrackPolicy.CPC_RECURSAL, TrackPolicy.JUIZADOS, TrackPolicy.TRABALHISTA,
                            TrackPolicy.ELEITORAL, TrackPolicy.PENAL, TrackPolicy.PRECATORIOS));
        }
        if (tipo == TipoUsuario.CIDADAO) {
            return genericProfile("CIDADANIA_PROCESSUAL", "Agenda do cidadão", CalendarLanePolicy.AGENDA_PROCESSUAL,
                    Set.of(TrackPolicy.CPC_GERAL, TrackPolicy.JUIZADOS, TrackPolicy.PRECATORIOS));
        }
        if (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            return genericProfile("CUMPRIMENTO_MANDADOS", titleFor(tipo), CalendarLanePolicy.AGENDA_PROCESSUAL,
                    Set.of(TrackPolicy.CPC_GERAL, TrackPolicy.PENAL));
        }
        if (tipo.isPerito()) {
            return genericProfile("AUXILIAR_PERICIAL", titleFor(tipo), CalendarLanePolicy.AGENDA_PROCESSUAL,
                    Set.of(TrackPolicy.CPC_GERAL, TrackPolicy.TRABALHISTA, TrackPolicy.PENAL, TrackPolicy.PRECATORIOS));
        }
        if (tipo.isSegurancaPublica()) {
            return genericProfile("SEGURANCA_PUBLICA", titleFor(tipo), CalendarLanePolicy.AGENDA_PROCESSUAL,
                    Set.of(TrackPolicy.PENAL, TrackPolicy.ELEITORAL));
        }
        if (tipo.isConciliacaoMediacao()) {
            return genericProfile("CONCILIACAO_MEDIACAO", titleFor(tipo), CalendarLanePolicy.AGENDA_PROCESSUAL,
                    Set.of(TrackPolicy.CPC_GERAL, TrackPolicy.JUIZADOS, TrackPolicy.TRABALHISTA));
        }
        return genericProfile(tipo.papelArquitetural() + "_OPERACIONAL", titleFor(tipo), CalendarLanePolicy.AGENDA_PROCESSUAL,
                Set.of(TrackPolicy.CPC_GERAL, TrackPolicy.CPC_RECURSAL, TrackPolicy.PRECATORIOS));
    }

    private CalendarProfileSeed genericProfile(String code,
                                               String title,
                                               CalendarLanePolicy highlightLane,
                                               Set<TrackPolicy> tracks) {
        LinkedHashSet<CalendarLanePolicy> visible = new LinkedHashSet<>();
        visible.add(CalendarLanePolicy.PRAZOS);
        visible.add(CalendarLanePolicy.AGENDA_PROCESSUAL);
        visible.add(CalendarLanePolicy.PRECATORIOS);
        visible.add(CalendarLanePolicy.PESSOAL);
        LinkedHashSet<CalendarLanePolicy> pinned = new LinkedHashSet<>();
        if (highlightLane != null) {
            pinned.add(highlightLane);
        }
        pinned.add(CalendarLanePolicy.AGENDA_PROCESSUAL);
        return new CalendarProfileSeed(code, title, highlightLane, visible, pinned, tracks.isEmpty() ? EnumSet.noneOf(TrackPolicy.class) : EnumSet.copyOf(tracks));
    }

    private List<DeadlineTrack> prazoTracks(CalendarProfileSeed seed) {
        Map<TrackPolicy, DeadlineTrack> all = new LinkedHashMap<>();
        all.put(TrackPolicy.CPC_GERAL, new DeadlineTrack(
                "CPC_GERAL",
                "Prazos cíveis e fazendários",
                "CPC",
                "Contagem em dias úteis, com prorrogação do termo final para o próximo dia útil forense quando necessário.",
                seed.highlightLane == CalendarLanePolicy.PRAZOS
        ));
        all.put(TrackPolicy.CPC_RECURSAL, new DeadlineTrack(
                "CPC_RECURSAL",
                "Recursos e embargos do CPC",
                "CPC recursal",
                "Bloco recursal com apelação, agravos, recursos aos tribunais superiores e embargos de declaração sob regime processual próprio.",
                seed.highlightLane == CalendarLanePolicy.PRAZOS
        ));
        all.put(TrackPolicy.JUIZADOS, new DeadlineTrack(
                "JUIZADOS",
                "Juizados especiais",
                "Lei 9.099 e correlatas",
                "Trilha simplificada para recurso inominado, embargos e atos próprios do microssistema dos juizados.",
                false
        ));
        all.put(TrackPolicy.TRABALHISTA, new DeadlineTrack(
                "TRABALHISTA",
                "Trabalho e execução trabalhista",
                "CLT",
                "Prazos da Justiça do Trabalho, inclusive recursos, execução e incidentes com calendário próprio da unidade judiciária.",
                false
        ));
        all.put(TrackPolicy.ELEITORAL, new DeadlineTrack(
                "ELEITORAL",
                "Eleitoral em período de campanha",
                "TSE e legislação eleitoral",
                "Trilha contínua e peremptória nos períodos eleitorais críticos, sem presumir a lógica geral do CPC.",
                false
        ));
        all.put(TrackPolicy.PENAL, new DeadlineTrack(
                "PENAL",
                "Penal e militar",
                "CPP e legislação especial",
                "Prazos penais e militares com marcos próprios de intimação, resposta, alegações finais e recursos.",
                false
        ));
        all.put(TrackPolicy.PRECATORIOS, new DeadlineTrack(
                "PRECATORIOS",
                "Precatórios e RPV",
                "Constitucional e CNJ",
                "Agenda financeira separada da agenda processual, com classificação, ordem cronológica e controle de pagamento público.",
                seed.highlightLane == CalendarLanePolicy.PRECATORIOS
        ));
        return seed.tracks.stream().map(all::get).filter(Objects::nonNull).toList();
    }

    private List<ColorLegend> colorLegend() {
        return List.of(
                new ColorLegend("RED", "Crítico", "Prazo fatal, superpreferência ou controle sensível que exige ação imediata."),
                new ColorLegend("AMBER", "Atenção", "Prazo útil, ciência, lembrete ou controle financeiro em aproximação."),
                new ColorLegend("BLUE", "Operacional", "Audiências, sessões, agendas internas e eventos de fluxo.") ,
                new ColorLegend("PURPLE", "Colegiado e precatórios", "Sessões colegiadas, fila cronológica e governança de pagamento público."),
                new ColorLegend("GREEN", "Concluído ou pessoal", "Liberação, previsão favorável, agenda pessoal ou marco já consolidado.")
        );
    }

    private boolean suporteEventoPessoal(TipoUsuario tipo) {
        return tipo == null || !tipo.isPerfilCritico();
    }

    private String titleFor(TipoUsuario tipo) {
        if (tipo == null) {
            return "Agenda institucional";
        }
        return switch (tipo) {
            case JUIZ, JUIZ_ESTADUAL, JUIZ_FEDERAL, JUIZ_ESPECIAL, JUIZ_ELEITORAL, JUIZ_TRABALHISTA, JUIZ_MILITAR, MAGISTRADO -> "Agenda da magistratura";
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL -> "Agenda do desembargador";
            case MINISTRO -> "Agenda do ministro";
            case ADVOGADO, OAB_PRESIDENTE_SECCIONAL -> "Agenda da advocacia";
            case DEFENSOR_PUBLICO, DEFENSOR_PUBLICO_FEDERAL -> "Agenda da defensoria";
            case MEMBRO_MINISTERIO_PUBLICO, PROMOTOR_ELEITORAL, PROMOTOR_TRABALHISTA, PROCURADOR_GERAL_REPUBLICA -> "Agenda do Ministério Público";
            case PROCURADOR, PROCURADORIA_ESTADUAL, PROCURADORIA_FEDERAL, PROCURADORIA_MUNICIPAL -> "Agenda da procuradoria";
            case OFICIAL_JUSTICA, OFICIAL_JUSTICA_AVALIADOR -> "Agenda de mandados";
            case DELEGADO_POLICIA, DELEGADO_POLICIA_FEDERAL, AGENTE_POLICIAL, ESCRIVAO_POLICIAL -> "Agenda investigativa";
            case PERITO, PERITO_AMBIENTAL, PERITO_CONTABIL, PERITO_CRIMINAL, PERITO_DIGITAL, PERITO_ENGENHARIA, PERITO_INSS, PERITO_MEDICO, PSICOLOGO_JUDICIAL, ASSISTENTE_SOCIAL_JUDICIAL, ASSISTENTE_TECNICO -> "Agenda pericial";
            default -> titleCase(tipo.name().replace('_', ' '));
        };
    }

    private String titleCase(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Agenda institucional";
        }
        String[] parts = raw.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
