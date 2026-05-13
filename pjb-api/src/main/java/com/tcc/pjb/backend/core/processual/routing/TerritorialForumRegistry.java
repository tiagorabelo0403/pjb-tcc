package com.tcc.pjb.backend.core.processual.routing;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;

@Component
public class TerritorialForumRegistry {

    private static final Map<String, String> CAPITAL_BY_UF = Map.ofEntries(
            Map.entry("AC", "Rio Branco"), Map.entry("AL", "Maceió"), Map.entry("AP", "Macapá"), Map.entry("AM", "Manaus"),
            Map.entry("BA", "Salvador"), Map.entry("CE", "Fortaleza"), Map.entry("DF", "Brasília"), Map.entry("ES", "Vitória"),
            Map.entry("GO", "Goiânia"), Map.entry("MA", "São Luís"), Map.entry("MT", "Cuiabá"), Map.entry("MS", "Campo Grande"),
            Map.entry("MG", "Belo Horizonte"), Map.entry("PA", "Belém"), Map.entry("PB", "João Pessoa"), Map.entry("PR", "Curitiba"),
            Map.entry("PE", "Recife"), Map.entry("PI", "Teresina"), Map.entry("RJ", "Rio de Janeiro"), Map.entry("RN", "Natal"),
            Map.entry("RS", "Porto Alegre"), Map.entry("RO", "Porto Velho"), Map.entry("RR", "Boa Vista"), Map.entry("SC", "Florianópolis"),
            Map.entry("SP", "São Paulo"), Map.entry("SE", "Aracaju"), Map.entry("TO", "Palmas")
    );

    private static final Map<String, CityForumSeed> CITY_SEEDS = Map.ofEntries(
            seed("CE", "Fortaleza", "Comarca de Fortaleza", "Fórum Clóvis Beviláqua", "Central de Distribuição de Fortaleza", "METROPOLITANO_CAPITAL", "CE_FORTALEZA_CAPITAL"),
            seed("CE", "Juazeiro do Norte", "Comarca de Juazeiro do Norte", "Fórum de Juazeiro do Norte", "Central Regional do Cariri", "POLO_REGIONAL", "CE_CARIRI"),
            seed("CE", "Sobral", "Comarca de Sobral", "Fórum de Sobral", "Central Regional Norte", "POLO_REGIONAL", "CE_NORTE"),
            seed("SP", "São Paulo", "Foro Central da Comarca de São Paulo", "Fórum João Mendes Júnior", "Distribuição Central Cível e Criminal", "METROPOLITANO_CAPITAL", "SP_CAPITAL"),
            seed("SP", "Campinas", "Comarca de Campinas", "Fórum de Campinas", "Distribuição Regional Campinas", "POLO_REGIONAL", "SP_INTERIOR_CAMPINAS"),
            seed("RJ", "Rio de Janeiro", "Comarca da Capital", "Fórum Central do Rio de Janeiro", "Distribuição Central da Capital", "METROPOLITANO_CAPITAL", "RJ_CAPITAL"),
            seed("MG", "Belo Horizonte", "Comarca de Belo Horizonte", "Fórum Lafayette", "Distribuição Central Belo Horizonte", "METROPOLITANO_CAPITAL", "MG_CAPITAL"),
            seed("RS", "Porto Alegre", "Foro Central da Comarca de Porto Alegre", "Foro Central I", "Distribuição Central Porto Alegre", "METROPOLITANO_CAPITAL", "RS_CAPITAL"),
            seed("PR", "Curitiba", "Foro Central da Comarca da Região Metropolitana de Curitiba", "Fórum Central de Curitiba", "Distribuição Metropolitana Curitiba", "METROPOLITANO_CAPITAL", "PR_CAPITAL"),
            seed("BA", "Salvador", "Comarca de Salvador", "Fórum Ruy Barbosa", "Distribuição Central Salvador", "METROPOLITANO_CAPITAL", "BA_CAPITAL"),
            seed("PE", "Recife", "Comarca do Recife", "Fórum Thomaz de Aquino", "Distribuição Central Recife", "METROPOLITANO_CAPITAL", "PE_CAPITAL"),
            seed("DF", "Brasília", "Circunscrição Judiciária de Brasília", "Fórum Desembargador Milton Sebastião Barbosa", "Distribuição do Distrito Federal", "CAPITAL_DISTRITAL", "DF_CAPITAL"),
            seed("AM", "Manaus", "Comarca de Manaus", "Fórum Ministro Henoch Reis", "Distribuição Central Manaus", "METROPOLITANO_CAPITAL", "AM_CAPITAL"),
            seed("GO", "Goiânia", "Comarca de Goiânia", "Fórum Cível Heitor Moraes Fleury", "Distribuição Central Goiânia", "METROPOLITANO_CAPITAL", "GO_CAPITAL"),
            seed("SC", "Florianópolis", "Comarca da Capital", "Fórum da Capital", "Distribuição Central Florianópolis", "METROPOLITANO_CAPITAL", "SC_CAPITAL"),
            seed("PA", "Belém", "Comarca de Belém", "Fórum Cível de Belém", "Distribuição Central Belém", "METROPOLITANO_CAPITAL", "PA_CAPITAL")
    );

    public TerritorialForumRegistryProfile resolve(TipoJustica tipoJustica,
                                                   String uf,
                                                   String cidade,
                                                   String comarca,
                                                   String foro,
                                                   String secao,
                                                   String subsecao,
                                                   String circunscricao,
                                                   GrauJurisdicao grau) {
        String normalizedUf = normalizeUf(uf);
        String normalizedCity = normalizeCity(cidade);
        CityForumSeed seed = CITY_SEEDS.get(key(normalizedUf, normalizedCity));
        String capital = CAPITAL_BY_UF.get(normalizedUf);
        boolean capitalFallback = seed == null && normalizedUf != null && normalizedCity != null && normalizedCity.equals(normalizeCity(capital));
        String municipalAnchor = firstNonBlank(cidade, seed != null ? seed.city() : null, capitalFallback ? capital : null);
        String judicialDistrict = firstNonBlank(comarca, seed != null ? seed.judicialDistrict() : null, municipalAnchor == null ? null : defaultDistrict(tipoJustica, municipalAnchor, normalizedUf));
        String primaryForum = firstNonBlank(foro, seed != null ? seed.primaryForum() : null, derivePrimaryForum(tipoJustica, municipalAnchor, normalizedUf, secao, subsecao, circunscricao, grau));
        String secondaryForum = seed != null ? seed.secondaryForum() : deriveSecondaryForum(tipoJustica, municipalAnchor, normalizedUf, secao, subsecao, circunscricao);
        String venueClass = seed != null ? seed.venueClass() : (capitalFallback ? "CAPITAL_DEFAULT" : "GENERAL_DEFAULT");
        String distributionCluster = seed != null ? seed.distributionCluster() : defaultDistributionCluster(tipoJustica, normalizedUf, capitalFallback);
        String supportDesk = deriveSupportDesk(tipoJustica, normalizedUf, gradeToken(grau), distributionCluster);

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        if (seed == null && capitalFallback) {
            fundamentos.add("Cidade âncora coincide com a capital da UF e recebeu malha de foro capitalizada por fallback controlado.");
        }
        if (seed == null && municipalAnchor != null && !municipalAnchor.isBlank()) {
            warnings.add("Cidade sem semente territorial específica; foro e comarca foram derivados por malha genérica controlada.");
            reviewChecklist.add("Confirmar foro e unidade local quando a cidade não estiver no catálogo territorial detalhado.");
        }
        if (municipalAnchor == null && judicialDistrict == null && primaryForum == null) {
            warnings.add("Registro territorial sem âncora municipal, comarca ou foro explícito.");
            reviewChecklist.add("Completar cidade, comarca ou foro para elevar a precisão do roteamento territorial.");
        }
        if (tipoJustica == TipoJustica.FEDERAL && subsecao == null && municipalAnchor != null) {
            fundamentos.add("Na Justiça Federal o catálogo usa a cidade para derivar subseção e foro federal quando o dado não vem expresso.");
        }
        if (tipoJustica == TipoJustica.TRABALHO && municipalAnchor != null) {
            fundamentos.add("No eixo trabalhista o foro trabalhista foi correlacionado à praça de prestação de serviços ou sede informada.");
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            fundamentos.add("No eixo eleitoral o cartório/zona funciona como âncora de venue e de mesa de triagem cartorária.");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("uf", normalizedUf);
        metadata.put("citySeedApplied", seed != null);
        metadata.put("capitalFallback", capitalFallback);
        metadata.put("capitalAnchor", capital);
        metadata.put("gradeToken", gradeToken(grau));
        metadata.put("source", seed != null ? "CITY_SEED" : capitalFallback ? "CAPITAL_FALLBACK" : "GENERIC_MESH");

        return new TerritorialForumRegistryProfile(
                municipalAnchor,
                judicialDistrict,
                primaryForum,
                secondaryForum,
                supportDesk,
                venueClass,
                distributionCluster,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private static String derivePrimaryForum(TipoJustica tipoJustica,
                                             String municipalAnchor,
                                             String uf,
                                             String secao,
                                             String subsecao,
                                             String circunscricao,
                                             GrauJurisdicao grau) {
        if (municipalAnchor == null || municipalAnchor.isBlank()) {
            return null;
        }
        String suffix = uf == null || uf.isBlank() ? "" : "/" + uf;
        if (tipoJustica == null) {
            return "Fórum de " + municipalAnchor + suffix;
        }
        if (grau == GrauJurisdicao.SEGUNDO_GRAU || grau == GrauJurisdicao.SUPERIOR || grau == GrauJurisdicao.CONSTITUCIONAL) {
            return switch (tipoJustica) {
                case FEDERAL -> firstNonBlank(subsecao, secao, "Secretaria Judiciária Federal de " + municipalAnchor + suffix);
                case TRABALHO -> "Secretaria de Turmas Trabalhistas de " + municipalAnchor + suffix;
                case ELEITORAL -> "Secretaria Regional Eleitoral de " + municipalAnchor + suffix;
                case MILITAR_ESTADUAL, MILITAR_FEDERAL -> firstNonBlank(circunscricao, "Secretaria Militar de " + municipalAnchor + suffix);
                case SUPERIOR -> "Secretaria de Tribunal Superior";
                default -> "Secretaria de Câmaras de " + municipalAnchor + suffix;
            };
        }
        return switch (tipoJustica) {
            case FEDERAL -> firstNonBlank(subsecao, "Foro Federal de " + municipalAnchor + suffix);
            case TRABALHO -> "Foro Trabalhista de " + municipalAnchor + suffix;
            case ELEITORAL -> firstNonBlank(circunscricao, "Cartório Eleitoral de " + municipalAnchor + suffix);
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> firstNonBlank(circunscricao, "Auditoria Militar de " + municipalAnchor + suffix);
            case SUPERIOR -> "Secretaria do Tribunal Superior";
            default -> "Fórum de " + municipalAnchor + suffix;
        };
    }

    private static String deriveSecondaryForum(TipoJustica tipoJustica,
                                               String municipalAnchor,
                                               String uf,
                                               String secao,
                                               String subsecao,
                                               String circunscricao) {
        String suffix = uf == null || uf.isBlank() ? "" : "/" + uf;
        return switch (tipoJustica == null ? TipoJustica.ESTADUAL : tipoJustica) {
            case FEDERAL -> firstNonBlank(secao, subsecao, municipalAnchor == null ? null : "Distribuição Federal de " + municipalAnchor + suffix);
            case TRABALHO -> municipalAnchor == null ? null : "Distribuição Trabalhista de " + municipalAnchor + suffix;
            case ELEITORAL -> firstNonBlank(circunscricao, municipalAnchor == null ? null : "Zona Eleitoral de " + municipalAnchor + suffix);
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> firstNonBlank(circunscricao, municipalAnchor == null ? null : "Circunscrição Militar de " + municipalAnchor + suffix);
            case SUPERIOR -> "Secretaria de Protocolo Superior";
            default -> municipalAnchor == null ? null : "Distribuição Judicial de " + municipalAnchor + suffix;
        };
    }

    private static String defaultDistrict(TipoJustica tipoJustica, String municipalAnchor, String uf) {
        String suffix = uf == null || uf.isBlank() ? "" : "/" + uf;
        return switch (tipoJustica == null ? TipoJustica.ESTADUAL : tipoJustica) {
            case FEDERAL -> "Subseção/Seção Judiciária de " + municipalAnchor + suffix;
            case TRABALHO -> "Circunscrição Trabalhista de " + municipalAnchor + suffix;
            case ELEITORAL -> "Zona Eleitoral de " + municipalAnchor + suffix;
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "Circunscrição Militar de " + municipalAnchor + suffix;
            case SUPERIOR -> "Distrito Superior";
            default -> "Comarca de " + municipalAnchor;
        };
    }

    private static String defaultDistributionCluster(TipoJustica tipoJustica, String uf, boolean capitalFallback) {
        String prefix = switch (tipoJustica == null ? TipoJustica.ESTADUAL : tipoJustica) {
            case FEDERAL -> "JF";
            case TRABALHO -> "JT";
            case ELEITORAL -> "JE";
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "JM";
            case SUPERIOR -> "SUP";
            default -> "TJ";
        };
        return prefix + '_' + firstNonBlank(uf, "BR") + '_' + (capitalFallback ? "CAPITAL" : "MESH");
    }

    private static String deriveSupportDesk(TipoJustica tipoJustica, String uf, String gradeToken, String distributionCluster) {
        String branch = switch (tipoJustica == null ? TipoJustica.ESTADUAL : tipoJustica) {
            case FEDERAL -> "SECRETARIA_JF";
            case TRABALHO -> "SECRETARIA_JT";
            case ELEITORAL -> "SECRETARIA_JE";
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "SECRETARIA_JM";
            case SUPERIOR -> "SECRETARIA_SUP";
            default -> "SECRETARIA_TJ";
        };
        return branch + ':' + firstNonBlank(uf, "BR") + ':' + gradeToken + ':' + firstNonBlank(distributionCluster, "BASE");
    }

    private static Map.Entry<String, CityForumSeed> seed(String uf,
                                                         String city,
                                                         String judicialDistrict,
                                                         String primaryForum,
                                                         String secondaryForum,
                                                         String venueClass,
                                                         String distributionCluster) {
        return Map.entry(key(normalizeUf(uf), normalizeCity(city)), new CityForumSeed(city, judicialDistrict, primaryForum, secondaryForum, venueClass, distributionCluster));
    }

    private static String key(String uf, String normalizedCity) {
        return firstNonBlank(uf, "BR") + ':' + firstNonBlank(normalizedCity, "BASE");
    }

    private static String gradeToken(GrauJurisdicao grau) {
        if (grau == null) {
            return "1G";
        }
        return switch (grau) {
            case SEGUNDO_GRAU -> "2G";
            case SUPERIOR -> "SUP";
            case CONSTITUCIONAL -> "CONST";
            default -> "1G";
        };
    }

    private static String normalizeUf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeCity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
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

    private record CityForumSeed(
            String city,
            String judicialDistrict,
            String primaryForum,
            String secondaryForum,
            String venueClass,
            String distributionCluster) {
    }
}
