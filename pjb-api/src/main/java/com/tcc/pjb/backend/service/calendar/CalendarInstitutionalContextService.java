package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CalendarInstitutionalContextService {

    public List<InstitutionalContextOption> availableContexts(Usuario usuario,
                                                              String activeScopeCode,
                                                              Long processoId,
                                                              Long selectedTeamId) {
        LinkedHashMap<String, InstitutionalContextOption> options = new LinkedHashMap<>();
        options.put("GERAL", new InstitutionalContextOption(
                "GERAL",
                "Visão institucional geral",
                "Calendário consolidado do painel",
                "GERAL"
        ));
        if ("PROCESSUAL".equals(normalize(activeScopeCode)) || processoId != null) {
            options.put("PROCESSO_FOCAL", new InstitutionalContextOption(
                    "PROCESSO_FOCAL",
                    "Processo focal",
                    processoId == null ? "Processo em foco" : "Processo " + processoId,
                    "PROCESSUAL"
            ));
        }
        if (selectedTeamId != null) {
            options.put("EQUIPE_ATIVA", new InstitutionalContextOption(
                    "EQUIPE_ATIVA",
                    "Equipe ativa",
                    "Equipe vinculada ao calendário",
                    "EQUIPE"
            ));
        }
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        if (tipo == null) {
            options.putIfAbsent("PESSOAL", new InstitutionalContextOption(
                    "PESSOAL",
                    "Pessoal",
                    "Agenda centrada no usuário",
                    "PESSOAL"
            ));
            return List.copyOf(options.values());
        }
        if (tipo == TipoUsuario.CIDADAO) {
            options.put("PESSOAL", new InstitutionalContextOption(
                    "PESSOAL",
                    "Cidadania pessoal",
                    safeLabel(usuario == null ? null : usuario.getNome(), "Painel gov.br"),
                    "PESSOAL"
            ));
            options.put("ACOMPANHAMENTO_PROCESSUAL", new InstitutionalContextOption(
                    "ACOMPANHAMENTO_PROCESSUAL",
                    "Acompanhamento processual",
                    "Audiências, prazos e intimações do cidadão",
                    "PESSOAL"
            ));
            return List.copyOf(options.values());
        }
        if (tipo.isAdvocacia()) {
            options.put("CARTEIRA_PROCESSUAL", new InstitutionalContextOption(
                    "CARTEIRA_PROCESSUAL",
                    "Carteira processual",
                    "Clientes, prazos, recursos e audiências",
                    "OPERACIONAL"
            ));
            options.put("RECURSAL_ESCRITORIO", new InstitutionalContextOption(
                    "RECURSAL_ESCRITORIO",
                    "Recursal do escritório",
                    "Recursos, embargos e sustentação oral",
                    "OPERACIONAL"
            ));
        }
        if (tipo.isMagistratura()) {
            options.put("GABINETE", new InstitutionalContextOption(
                    "GABINETE",
                    "Gabinete",
                    "Conclusões, votos, minutas e prioridade decisional",
                    "UNIDADE"
            ));
            options.put("ORGAO_JULGADOR", new InstitutionalContextOption(
                    "ORGAO_JULGADOR",
                    "Órgão julgador",
                    "Sessões, pauta e julgamento colegiado",
                    "UNIDADE"
            ));
        }
        if (tipo.isAssessor()) {
            options.put("ASSESSORIA_GABINETE", new InstitutionalContextOption(
                    "ASSESSORIA_GABINETE",
                    "Assessoria de gabinete",
                    "Minutas, conclusões e apoio decisório",
                    "UNIDADE"
            ));
            options.put("PAUTA_COLEGIADO", new InstitutionalContextOption(
                    "PAUTA_COLEGIADO",
                    "Pauta colegiada",
                    "Sessões, votos e sustentação oral",
                    "UNIDADE"
            ));
        }
        if (tipo.isServidorJudiciario()) {
            options.put("SECRETARIA_UNIDADE", new InstitutionalContextOption(
                    "SECRETARIA_UNIDADE",
                    "Secretaria da unidade",
                    "Audiências, cumprimento, remessas e cartório",
                    "UNIDADE"
            ));
            options.put("CARTORIO_OPERACIONAL", new InstitutionalContextOption(
                    "CARTORIO_OPERACIONAL",
                    "Cartório operacional",
                    "Prazos internos, expediente e fila de atos",
                    "UNIDADE"
            ));
        }
        if (tipo.isMinisterioPublico()) {
            options.put("PROMOTORIA_UNIDADE", new InstitutionalContextOption(
                    "PROMOTORIA_UNIDADE",
                    "Promotoria",
                    "Pautas, audiências e pareceres ministeriais",
                    "UNIDADE"
            ));
        }
        if (tipo.isDefensoriaPublica()) {
            options.put("DEFENSORIA_NUCLEO", new InstitutionalContextOption(
                    "DEFENSORIA_NUCLEO",
                    "Núcleo da Defensoria",
                    "Atendimentos, audiências e respostas defensivas",
                    "UNIDADE"
            ));
        }
        if (tipo.isProcuradoria()) {
            options.put("PROCURADORIA_ORGAO", new InstitutionalContextOption(
                    "PROCURADORIA_ORGAO",
                    "Órgão da Procuradoria",
                    "Contestações, prazos fazendários e precatórios",
                    "UNIDADE"
            ));
        }
        if (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            options.put("CENTRAL_MANDADOS", new InstitutionalContextOption(
                    "CENTRAL_MANDADOS",
                    "Central de mandados",
                    "Mandados, diligências e certidões",
                    "OPERACIONAL"
            ));
        }
        if (tipo.isPerito()) {
            options.put("NUCLEO_PERICIAL", new InstitutionalContextOption(
                    "NUCLEO_PERICIAL",
                    "Núcleo pericial",
                    "Perícias, laudos e janelas técnicas",
                    "OPERACIONAL"
            ));
        }
        if (tipo.isSegurancaPublica()) {
            options.put("UNIDADE_POLICIAL", new InstitutionalContextOption(
                    "UNIDADE_POLICIAL",
                    "Unidade policial",
                    "Diligências, cautelares e expedições investigativas",
                    "OPERACIONAL"
            ));
        }
        if (tipo.isConciliacaoMediacao()) {
            options.put("MESA_CONCILIACAO", new InstitutionalContextOption(
                    "MESA_CONCILIACAO",
                    "Mesa de conciliação",
                    "Sessões consensuais e agenda conciliatória",
                    "OPERACIONAL"
            ));
        }
        return List.copyOf(options.values());
    }

    public String normalizeActiveContext(String requested,
                                         List<InstitutionalContextOption> available,
                                         String activeScopeCode) {
        String normalized = normalize(requested);
        if (normalized != null) {
            for (InstitutionalContextOption option : available) {
                if (option.contextCode().equals(normalized)) {
                    return option.contextCode();
                }
            }
        }
        String scope = normalize(activeScopeCode);
        if ("PESSOAL".equals(scope) && contains(available, "PESSOAL")) {
            return "PESSOAL";
        }
        if ("PROCESSUAL".equals(scope) && contains(available, "PROCESSO_FOCAL")) {
            return "PROCESSO_FOCAL";
        }
        if (contains(available, "GABINETE")) {
            return "GABINETE";
        }
        if (contains(available, "SECRETARIA_UNIDADE")) {
            return "SECRETARIA_UNIDADE";
        }
        if (contains(available, "CARTEIRA_PROCESSUAL")) {
            return "CARTEIRA_PROCESSUAL";
        }
        return available.isEmpty() ? "GERAL" : available.get(0).contextCode();
    }

    public boolean allows(String contextCode,
                          String laneCode,
                          String segmentCode,
                          String audienceCode) {
        String context = normalize(contextCode);
        if (context == null || "GERAL".equals(context) || "PROCESSO_FOCAL".equals(context) || "EQUIPE_ATIVA".equals(context)) {
            return true;
        }
        String lane = normalize(laneCode);
        String segment = normalize(segmentCode);
        String audience = normalize(audienceCode);
        return switch (context) {
            case "PESSOAL" -> !"PRECATORIOS".equals(lane) || audience == null || audience.contains("CIDADAO") || audience.contains("ADVOG");
            case "ACOMPANHAMENTO_PROCESSUAL" -> !"PESSOAL".equals(lane);
            case "CARTEIRA_PROCESSUAL" -> !"PESSOAL".equals(lane);
            case "RECURSAL_ESCRITORIO" -> "PRAZOS".equals(lane) && containsAny(segment, "RECURSAL", "EMBARG");
            case "GABINETE", "ASSESSORIA_GABINETE" -> ("AGENDA_PROCESSUAL".equals(lane) && containsAny(segment, "JULGAMENTO", "GABINETE"))
                    || ("PRAZOS".equals(lane) && containsAny(segment, "RECURSAL", "EMBARG", "PENAL", "ELEITORAL"));
            case "ORGAO_JULGADOR", "PAUTA_COLEGIADO" -> "AGENDA_PROCESSUAL".equals(lane) && containsAny(segment, "JULGAMENTO", "AUDIENCIAS");
            case "SECRETARIA_UNIDADE", "CARTORIO_OPERACIONAL" -> ("AGENDA_PROCESSUAL".equals(lane) && containsAny(segment, "SECRETARIA", "AUDIENCIAS", "MANDADOS"))
                    || "PRAZOS".equals(lane);
            case "PROMOTORIA_UNIDADE", "DEFENSORIA_NUCLEO" -> "PRAZOS".equals(lane) || ("AGENDA_PROCESSUAL".equals(lane) && containsAny(segment, "AUDIENCIAS", "JULGAMENTO"));
            case "PROCURADORIA_ORGAO" -> "PRECATORIOS".equals(lane) || "PRAZOS".equals(lane) || ("AGENDA_PROCESSUAL".equals(lane) && containsAny(segment, "AUDIENCIAS", "JULGAMENTO"));
            case "CENTRAL_MANDADOS" -> "AGENDA_PROCESSUAL".equals(lane) && containsAny(segment, "MANDADOS", "AUDIENCIAS", "SECRETARIA");
            case "NUCLEO_PERICIAL" -> "AGENDA_PROCESSUAL".equals(lane) && containsAny(segment, "PERICIAS", "AUDIENCIAS");
            case "UNIDADE_POLICIAL" -> ("AGENDA_PROCESSUAL".equals(lane) && containsAny(segment, "MANDADOS", "SECRETARIA")) || "PRAZOS".equals(lane);
            case "MESA_CONCILIACAO" -> "AGENDA_PROCESSUAL".equals(lane) && containsAny(segment, "AUDIENCIAS", "OPERACIONAL");
            default -> true;
        };
    }

    private boolean contains(List<InstitutionalContextOption> available, String code) {
        return available.stream().anyMatch(option -> option.contextCode().equals(code));
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

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String safeLabel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record InstitutionalContextOption(
            String contextCode,
            String contextTitle,
            String contextLabel,
            String contextKind
    ) {
        public InstitutionalContextOption {
            Objects.requireNonNull(contextCode);
            Objects.requireNonNull(contextTitle);
            Objects.requireNonNull(contextLabel);
            Objects.requireNonNull(contextKind);
        }
    }
}
