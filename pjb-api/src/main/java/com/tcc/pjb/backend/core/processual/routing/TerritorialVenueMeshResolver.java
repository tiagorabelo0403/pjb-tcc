package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.domain.enums.TipoJustica;

@Component
public class TerritorialVenueMeshResolver {

    public TerritorialVenueMeshProfile resolve(NationalProcessRoutingService.RoutingCommand command,
                                               TipoJustica tipoJustica,
                                               String uf) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        String cidadeAutor = trimToNull(command.cidadeAutor());
        String cidadeReu = trimToNull(command.cidadeReu());
        String cidadeFato = firstNonBlank(command.cidadeFato(), command.municipioFato());
        String cidadeBase = firstNonBlank(command.cidade(), cidadeFato, cidadeAutor, cidadeReu, command.comarca());
        boolean territorioExpresso = notBlank(command.foro())
                || notBlank(command.secaoJudiciaria())
                || notBlank(command.subsecaoJudiciaria())
                || notBlank(command.circunscricao());

        String venueMode;
        String competenceAnchor;
        if (territorioExpresso) {
            venueMode = "FORO_EXPRESSO";
            competenceAnchor = firstNonBlank(command.foro(), command.subsecaoJudiciaria(), command.circunscricao(), command.secaoJudiciaria());
        } else if (tipoJustica == TipoJustica.FEDERAL && notBlank(cidadeAutor)) {
            venueMode = "DOMICILIO_AUTOR_FEDERAL";
            competenceAnchor = cidadeAutor;
        } else if (tipoJustica == TipoJustica.TRABALHO && notBlank(cidadeFato)) {
            venueMode = "LOCAL_PRESTACAO_SERVICO";
            competenceAnchor = cidadeFato;
        } else if ((tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL || isPenal(command)) && notBlank(cidadeFato)) {
            venueMode = "LOCAL_FATO";
            competenceAnchor = cidadeFato;
        } else if (notBlank(cidadeAutor)) {
            venueMode = "DOMICILIO_AUTOR";
            competenceAnchor = cidadeAutor;
        } else if (notBlank(cidadeReu)) {
            venueMode = "DOMICILIO_REU";
            competenceAnchor = cidadeReu;
        } else {
            venueMode = "BASE_DECLARADA";
            competenceAnchor = firstNonBlank(command.comarca(), cidadeBase, uf);
        }

        String legalForumType = resolveLegalForumType(tipoJustica);
        String primaryForum = firstNonBlank(
                command.foro(),
                command.subsecaoJudiciaria(),
                command.circunscricao(),
                deriveForum(tipoJustica, firstNonBlank(competenceAnchor, cidadeBase), uf)
        );
        String secondaryForum = deriveSecondaryForum(tipoJustica, cidadeAutor, cidadeReu, cidadeFato, uf, primaryForum);
        String territorialConfidence = resolveConfidence(territorioExpresso, competenceAnchor, cidadeAutor, cidadeReu, cidadeFato);
        boolean forumReviewRequired = "BAIXA".equals(territorialConfidence)
                || (notBlank(cidadeAutor) && notBlank(cidadeReu) && !cidadeAutor.equalsIgnoreCase(cidadeReu))
                || (notBlank(cidadeFato) && notBlank(cidadeAutor) && !cidadeFato.equalsIgnoreCase(cidadeAutor));

        if (notBlank(cidadeAutor) && notBlank(cidadeReu) && !cidadeAutor.equalsIgnoreCase(cidadeReu)) {
            warnings.add("Autor e réu em municípios distintos; validar competência territorial específica do rito.");
            reviewChecklist.add("Revisar foro competente entre domicílio do autor, do réu e local do fato.");
        }
        if (notBlank(cidadeFato) && notBlank(cidadeAutor) && !cidadeFato.equalsIgnoreCase(cidadeAutor)) {
            warnings.add("Local do fato diverge do domicílio do autor; confirmar a âncora territorial prevalente.");
        }
        if (forumReviewRequired) {
            reviewChecklist.add("Conferir regra territorial concreta antes da distribuição automática definitiva.");
        }
        if (territorioExpresso) {
            fundamentos.add("O território expresso informado prevalece como âncora inicial para saneamento e distribuição.");
        }
        if (notBlank(competenceAnchor)) {
            fundamentos.add("Âncora territorial dominante: " + competenceAnchor + '.');
        }
        if (notBlank(primaryForum)) {
            fundamentos.add("Foro primário proposto: " + primaryForum + '.');
        }
        if (notBlank(secondaryForum)) {
            fundamentos.add("Foro secundário de contingência: " + secondaryForum + '.');
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("cidadeAutor", cidadeAutor);
        metadata.put("cidadeReu", cidadeReu);
        metadata.put("cidadeFato", cidadeFato);
        metadata.put("territorioExpresso", territorioExpresso);
        metadata.put("targetUf", normalizeUf(uf));
        metadata.put("anchorPriority", List.of("foro", "subsecao", "circunscricao", "cidadeFato", "cidadeAutor", "cidadeReu", "comarca"));
        metadata.put("forumAlternativo", secondaryForum);
        metadata.put("baseCity", cidadeBase);
        metadata.put("reviewReason", forumReviewRequired ? "CONFLITO_OU_BAIXA_CONFIANCA" : "SEM_DIVERGENCIA_RELEVANTE");
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new TerritorialVenueMeshProfile(
                venueMode,
                competenceAnchor,
                firstNonBlank(cidadeBase, competenceAnchor),
                primaryForum,
                secondaryForum,
                legalForumType,
                territorialConfidence,
                forumReviewRequired,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private String resolveLegalForumType(TipoJustica tipoJustica) {
        if (tipoJustica == null) {
            return "FORO_COMUM";
        }
        return switch (tipoJustica) {
            case FEDERAL -> "SECAO_SUBSECAO_FEDERAL";
            case TRABALHO -> "FORO_TRABALHISTA";
            case ELEITORAL -> "ZONA_ELEITORAL";
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "AUDITORIA_CIRCUNSCRICAO_MILITAR";
            case SUPERIOR -> "ORGAO_ORIGINARIO_SUPERIOR";
            default -> "COMARCA_FORO_ESTADUAL";
        };
    }

    private String deriveForum(TipoJustica tipoJustica, String cidade, String uf) {
        String base = firstNonBlank(cidade, "BASE TERRITORIAL");
        String suffix = isBlank(uf) ? "" : "/" + normalizeUf(uf);
        if (tipoJustica == null) {
            return "Foro de " + base + suffix;
        }
        return switch (tipoJustica) {
            case FEDERAL -> "Subseção Judiciária de " + base + suffix;
            case TRABALHO -> "Foro Trabalhista de " + base + suffix;
            case ELEITORAL -> "Zona Eleitoral de " + base + suffix;
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "Auditoria/Circunscrição Militar de " + base + suffix;
            case SUPERIOR -> "Órgão originário de " + base + suffix;
            default -> "Comarca/Foro de " + base + suffix;
        };
    }

    private String deriveSecondaryForum(TipoJustica tipoJustica,
                                        String cidadeAutor,
                                        String cidadeReu,
                                        String cidadeFato,
                                        String uf,
                                        String primaryForum) {
        String alternativa = firstNonBlank(cidadeFato, cidadeReu, cidadeAutor);
        String derived = isBlank(alternativa) ? null : deriveForum(tipoJustica, alternativa, uf);
        if (derived == null || derived.equalsIgnoreCase(firstNonBlank(primaryForum))) {
            return null;
        }
        return derived;
    }

    private String resolveConfidence(boolean territorioExpresso,
                                     String competenceAnchor,
                                     String cidadeAutor,
                                     String cidadeReu,
                                     String cidadeFato) {
        if (territorioExpresso) {
            return "ALTA";
        }
        if (notBlank(competenceAnchor) && (notBlank(cidadeFato) || notBlank(cidadeAutor))) {
            return "MEDIA";
        }
        if (notBlank(cidadeAutor) || notBlank(cidadeReu) || notBlank(cidadeFato)) {
            return "MEDIA";
        }
        return "BAIXA";
    }

    private boolean isPenal(NationalProcessRoutingService.RoutingCommand command) {
        return command != null && command.rito() != null && command.rito().isPenal();
    }

    private String normalizeUf(String uf) {
        return isBlank(uf) ? null : uf.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (notBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean notBlank(String value) {
        return !isBlank(value);
    }
}
