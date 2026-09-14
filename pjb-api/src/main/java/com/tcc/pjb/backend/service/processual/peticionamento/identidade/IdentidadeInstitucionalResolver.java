package com.tcc.pjb.backend.service.processual.peticionamento.identidade;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Resolve, a partir do cargo ({@link TipoUsuario}) e da UF do usuário, a identidade institucional
 * correta de cada ofício — com o cuidado de não tratar todos os cargos como iguais:
 *
 * <ul>
 *   <li><b>INSTITUCIONAL</b> (magistratura, MP, defensoria, procuradorias): a identidade é do
 *       <em>órgão</em>, não do indivíduo. Aqui o resolver entrega só a estrutura e a nomenclatura
 *       corretas (linha "PODER JUDICIÁRIO", "MINISTÉRIO PÚBLICO DO ESTADO DE …", etc.) e a chave de
 *       curadoria ({@code escopoRef}). O brasão e as cores <em>oficiais</em> nunca são inventados
 *       aqui: vêm da curadoria do próprio órgão; enquanto não vierem, usa-se um default NEUTRO
 *       explicitamente marcado como substituível, jamais alegado como oficial.</li>
 *   <li><b>PROFISSIONAL_INDIVIDUAL</b> (advogado, perito e auxiliares): a peça/laudo não leva brasão
 *       de órgão — leva identificação profissional com o registro do conselho correto (OAB, CRM,
 *       CREA, CRC, CRP…). Dar brasão institucional a um perito seria errado, e este resolver não o faz.</li>
 * </ul>
 */
@Component
public class IdentidadeInstitucionalResolver {

    public enum ClasseIdentidade { INSTITUCIONAL, PROFISSIONAL_INDIVIDUAL }

    public static final String ORIGEM_DEFAULT = "DEFAULT_PJB_SUBSTITUIVEL";

    /** Default neutro e sóbrio do PJB — nunca alegado como cor oficial de nenhum órgão. */
    private static final Map<String, Object> PALETA_NEUTRA_INSTITUCIONAL = Map.of(
            "paletaPrimaria", "#1F2A3D",
            "paletaSecundaria", "#6B7280",
            "origem", ORIGEM_DEFAULT);

    private static final Map<String, Object> PALETA_NEUTRA_INDIVIDUAL = Map.of(
            "paletaPrimaria", "#2B2B2B",
            "paletaSecundaria", "#6B6B6B",
            "origem", ORIGEM_DEFAULT);

    public record IdentidadeInstitucionalDescriptor(
            ClasseIdentidade classe,
            String poderRamo,
            String esfera,
            String escopoRef,
            String nomeOrgao,
            List<String> cabecalhoSugerido,
            String registroLabel,
            Map<String, Object> paletaDefault) {

        public boolean institucional() {
            return classe == ClasseIdentidade.INSTITUCIONAL;
        }
    }

    public IdentidadeInstitucionalDescriptor resolver(TipoUsuario tipo, String uf) {
        return resolver(tipo, uf, null);
    }

    public IdentidadeInstitucionalDescriptor resolver(TipoUsuario tipo, String uf, String comarca) {
        if (tipo == null) {
            return individual("PROFISSIONAL", "Registro profissional");
        }
        String u = normalizeUf(uf);
        if (tipo.isMagistratura()) {
            return magistratura(tipo, u);
        }
        if (tipo.isMinisterioPublico()) {
            return ministerioPublico(tipo, u);
        }
        if (tipo.isDefensoriaPublica()) {
            return defensoria(tipo, u);
        }
        if (tipo.isProcuradoria()) {
            return procuradoria(tipo, u, comarca);
        }
        if (tipo.isAdvocacia()) {
            return individual("PROFISSIONAL", "OAB");
        }
        if (tipo.isPerito()) {
            return individual("PROFISSIONAL", registroDoPerito(tipo));
        }
        return individual("PROFISSIONAL", "Registro profissional");
    }

    private IdentidadeInstitucionalDescriptor magistratura(TipoUsuario tipo, String uf) {
        return switch (tipo) {
            case JUIZ_FEDERAL, DESEMBARGADOR_FEDERAL -> institucional("PODER_JUDICIARIO", "FEDERAL",
                    "PJ-FED-" + uf, "JUSTIÇA FEDERAL",
                    List.of("PODER JUDICIÁRIO", "Justiça Federal", ufLinha(uf)));
            case JUIZ_TRABALHISTA -> institucional("PODER_JUDICIARIO", "FEDERAL",
                    "PJ-TRAB-" + uf, "JUSTIÇA DO TRABALHO",
                    List.of("PODER JUDICIÁRIO", "Justiça do Trabalho", ufLinha(uf)));
            case JUIZ_ELEITORAL -> institucional("PODER_JUDICIARIO", "FEDERAL",
                    "PJ-ELEIT-" + uf, "JUSTIÇA ELEITORAL",
                    List.of("PODER JUDICIÁRIO", "Justiça Eleitoral", ufLinha(uf)));
            case JUIZ_MILITAR -> institucional("PODER_JUDICIARIO", "ESTADUAL",
                    "PJ-MIL-" + uf, "JUSTIÇA MILITAR",
                    List.of("PODER JUDICIÁRIO", "Justiça Militar", ufLinha(uf)));
            case MINISTRO -> institucional("PODER_JUDICIARIO", "SUPERIOR",
                    "PJ-SUPERIOR", "TRIBUNAL SUPERIOR",
                    List.of("PODER JUDICIÁRIO", "Tribunal Superior"));
            default -> institucional("PODER_JUDICIARIO", "ESTADUAL",
                    "PJ-EST-" + uf, "TRIBUNAL DE JUSTIÇA" + ufSufixo(uf),
                    List.of("PODER JUDICIÁRIO", "Tribunal de Justiça" + ufSufixo(uf)));
        };
    }

    private IdentidadeInstitucionalDescriptor ministerioPublico(TipoUsuario tipo, String uf) {
        return switch (tipo) {
            case PROCURADOR_GERAL_REPUBLICA -> institucional("MINISTERIO_PUBLICO", "FEDERAL",
                    "MP-FED", "MINISTÉRIO PÚBLICO FEDERAL",
                    List.of("MINISTÉRIO PÚBLICO FEDERAL"));
            case PROMOTOR_TRABALHISTA -> institucional("MINISTERIO_PUBLICO", "FEDERAL",
                    "MP-TRAB", "MINISTÉRIO PÚBLICO DO TRABALHO",
                    List.of("MINISTÉRIO PÚBLICO DO TRABALHO"));
            case PROMOTOR_ELEITORAL -> institucional("MINISTERIO_PUBLICO", "FEDERAL",
                    "MP-ELEIT-" + uf, "MINISTÉRIO PÚBLICO ELEITORAL",
                    List.of("MINISTÉRIO PÚBLICO ELEITORAL", ufLinha(uf)));
            default -> institucional("MINISTERIO_PUBLICO", "ESTADUAL",
                    "MP-EST-" + uf, "MINISTÉRIO PÚBLICO" + estadoDeSufixo(uf),
                    List.of("MINISTÉRIO PÚBLICO" + estadoDeSufixo(uf)));
        };
    }

    private IdentidadeInstitucionalDescriptor defensoria(TipoUsuario tipo, String uf) {
        if (tipo == TipoUsuario.DEFENSOR_PUBLICO_FEDERAL) {
            return institucional("DEFENSORIA_PUBLICA", "FEDERAL",
                    "DP-FED", "DEFENSORIA PÚBLICA DA UNIÃO",
                    List.of("DEFENSORIA PÚBLICA DA UNIÃO"));
        }
        return institucional("DEFENSORIA_PUBLICA", "ESTADUAL",
                "DP-EST-" + uf, "DEFENSORIA PÚBLICA" + estadoDeSufixo(uf),
                List.of("DEFENSORIA PÚBLICA" + estadoDeSufixo(uf)));
    }

    private IdentidadeInstitucionalDescriptor procuradoria(TipoUsuario tipo, String uf, String comarca) {
        return switch (tipo) {
            case PROCURADORIA_FEDERAL -> institucional("ADVOCACIA_PUBLICA", "FEDERAL",
                    "PROC-FED", "ADVOCACIA-GERAL DA UNIÃO",
                    List.of("ADVOCACIA-GERAL DA UNIÃO"));
            case PROCURADORIA_MUNICIPAL -> procuradoriaMunicipal(uf, comarca);
            default -> institucional("ADVOCACIA_PUBLICA", "ESTADUAL",
                    "PROC-EST-" + uf, "PROCURADORIA-GERAL DO ESTADO" + estadoDeSufixo(uf),
                    List.of("PROCURADORIA-GERAL DO ESTADO" + estadoDeSufixo(uf)));
        };
    }

    /**
     * Procuradoria municipal é do MUNICÍPIO, não da UF: quando o município (comarca) do procurador é
     * conhecido, o timbre e a chave de curadoria descem ao município real; sem ele, cai para a UF.
     */
    private IdentidadeInstitucionalDescriptor procuradoriaMunicipal(String uf, String comarca) {
        String slug = slugMunicipio(comarca);
        if (slug != null) {
            return institucional("ADVOCACIA_PUBLICA", "MUNICIPAL",
                    "PROC-MUN-" + uf + "-" + slug, "PROCURADORIA-GERAL DO MUNICÍPIO DE " + comarca.trim().toUpperCase(Locale.ROOT),
                    List.of("PROCURADORIA-GERAL DO MUNICÍPIO DE " + comarca.trim().toUpperCase(Locale.ROOT), ufLinha(uf)));
        }
        return institucional("ADVOCACIA_PUBLICA", "MUNICIPAL",
                "PROC-MUN-" + uf, "PROCURADORIA-GERAL DO MUNICÍPIO",
                List.of("PROCURADORIA-GERAL DO MUNICÍPIO", ufLinha(uf)));
    }

    private static String slugMunicipio(String comarca) {
        if (comarca == null || comarca.isBlank()) {
            return null;
        }
        String slug = java.text.Normalizer.normalize(comarca.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? null : slug;
    }

    private static String registroDoPerito(TipoUsuario tipo) {
        return switch (tipo) {
            case PERITO_MEDICO, PERITO_INSS -> "CRM";
            case PERITO_ENGENHARIA -> "CREA";
            case PERITO_CONTABIL -> "CRC";
            default -> "Registro profissional";
        };
    }

    private static IdentidadeInstitucionalDescriptor institucional(String poderRamo, String esfera,
                                                                   String escopoRef, String nomeOrgao,
                                                                   List<String> cabecalho) {
        return new IdentidadeInstitucionalDescriptor(ClasseIdentidade.INSTITUCIONAL, poderRamo, esfera,
                escopoRef, nomeOrgao, List.copyOf(cabecalho), null, PALETA_NEUTRA_INSTITUCIONAL);
    }

    private static IdentidadeInstitucionalDescriptor individual(String poderRamo, String registroLabel) {
        return new IdentidadeInstitucionalDescriptor(ClasseIdentidade.PROFISSIONAL_INDIVIDUAL, poderRamo, null,
                null, null, List.of(), registroLabel, PALETA_NEUTRA_INDIVIDUAL);
    }

    private static String normalizeUf(String uf) {
        if (uf == null || uf.isBlank()) {
            return "BR";
        }
        String trimmed = uf.trim().toUpperCase(Locale.ROOT);
        return trimmed.length() == 2 ? trimmed : "BR";
    }

    private static String ufLinha(String uf) {
        return "BR".equals(uf) ? "" : "Seção/UF: " + uf;
    }

    private static String ufSufixo(String uf) {
        return "BR".equals(uf) ? "" : " do Estado (" + uf + ")";
    }

    private static String estadoDeSufixo(String uf) {
        return "BR".equals(uf) ? "" : " DO ESTADO (" + uf + ")";
    }
}
