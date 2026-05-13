package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public class TerritorialRoutingResolver {

    private final JudicialTerritorialCatalog judicialTerritorialCatalog;
    private final TerritorialVenueMeshResolver territorialVenueMeshResolver;

    public TerritorialRoutingResolver(JudicialTerritorialCatalog judicialTerritorialCatalog,
                                      TerritorialVenueMeshResolver territorialVenueMeshResolver) {
        this.judicialTerritorialCatalog = Objects.requireNonNull(judicialTerritorialCatalog);
        this.territorialVenueMeshResolver = Objects.requireNonNull(territorialVenueMeshResolver);
    }

    public TerritorialRoutingProfile resolve(NationalProcessRoutingService.RoutingCommand command,
                                            TipoJustica tipoJustica,
                                            NationalCompetenceMatrix competencia) {
        Objects.requireNonNull(command, "command");
        String uf = normalizeUf(firstNonBlank(command.uf(), suffixUf(command.tribunalCodigoHint())));
        TerritorialVenueMeshProfile venueMesh = territorialVenueMeshResolver.resolve(command, tipoJustica, uf);
        String cidadeBase = firstNonBlank(command.cidade(), venueMesh.primaryCity(), command.cidadeFato(), command.municipioFato(), command.cidadeAutor(), command.cidadeReu(), command.comarca());
        JudicialTerritorialProfile catalog = judicialTerritorialCatalog.resolve(
                tipoJustica,
                uf,
                cidadeBase,
                command.comarca(),
                command.foro(),
                command.secaoJudiciaria(),
                command.subsecaoJudiciaria(),
                command.circunscricao(),
                command.grau()
        );
        String comarca = firstNonBlank(command.comarca(), catalog.comarca(), command.foro(), cidadeBase);
        String foro = firstNonBlank(command.foro(), venueMesh.primaryForum(), catalog.foro(), deriveForo(tipoJustica, cidadeBase, uf));
        String secao = firstNonBlank(command.secaoJudiciaria(), catalog.secaoJudiciaria(), deriveSecao(tipoJustica, uf));
        String subsecao = firstNonBlank(command.subsecaoJudiciaria(), catalog.subsecaoJudiciaria(), deriveSubsecao(tipoJustica, cidadeBase, uf));
        String circunscricao = firstNonBlank(command.circunscricao(), catalog.circunscricao(), deriveCircunscricao(tipoJustica, cidadeBase, uf));
        String mode = venueMesh.effectiveMode(resolveMode(command, tipoJustica));
        String preventionMode = command.preventionReference() == null || command.preventionReference().isBlank()
                ? "NENHUM_SINAL"
                : "PREVENCAO_REFERENCIADA:" + command.preventionReference().trim();
        String unidadeBase = firstNonBlank(catalog.unidadeBase(), resolveUnidadeBase(tipoJustica, command.grau(), command.rito()));

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        warnings.addAll(catalog.warnings());
        warnings.addAll(venueMesh.warnings());
        if (uf == null) {
            warnings.add("UF territorial ausente; distribuição automática depende de saneamento territorial.");
            reviewChecklist.add("Informar UF da causa, do fato ou do órgão de prevenção.");
        }
        if (cidadeBase == null && comarca == null && subsecao == null && circunscricao == null) {
            warnings.add("Âncora territorial insuficiente para fechar foro, comarca, seção ou circunscrição.");
            reviewChecklist.add("Informar cidade/comarca/foro ou subseção judiciária antes da distribuição final.");
        }
        if (tipoJustica == TipoJustica.FEDERAL && subsecao == null) {
            warnings.add("Justiça Federal sem subseção explícita; malha territorial federal exige revisão humana complementar.");
            reviewChecklist.add("Confirmar seção e subseção judiciária de competência territorial.");
        }
        if (tipoJustica == TipoJustica.ELEITORAL && circunscricao == null) {
            warnings.add("Fluxo eleitoral sem zona/circunscrição explícita; confirmar territorialidade eleitoral.");
            reviewChecklist.add("Confirmar zona eleitoral ou TRE competente antes do protocolo.");
        }
        if ((tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) && circunscricao == null) {
            warnings.add("Fluxo militar sem auditoria/circunscrição explícita; revisão manual recomendada.");
            reviewChecklist.add("Confirmar auditoria militar, circunscrição e conselho competente.");
        }
        if (venueMesh.forumReviewRequired()) {
            reviewChecklist.addAll(venueMesh.reviewChecklist());
        }
        if (catalog.specialTerritorialReview()) {
            reviewChecklist.add("Revisar catálogo territorial especializado antes da distribuição automática definitiva.");
        }
        if (command.grau() == GrauJurisdicao.SEGUNDO_GRAU || command.grau() == GrauJurisdicao.SUPERIOR || command.grau() == GrauJurisdicao.CONSTITUCIONAL) {
            reviewChecklist.add("Conferir prevenção, distribuição por dependência e órgão fracionário competente.");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(catalog.toMap());
        metadata.put("tribunalCodigo", competencia != null ? competencia.codigo() : null);
        metadata.put("tribunalNome", competencia != null ? competencia.nome() : null);
        metadata.put("numeroProcessoHint", command.numeroProcesso());
        metadata.put("tribunalCodigoHint", command.tribunalCodigoHint());
        metadata.put("tipoJustica", tipoJustica != null ? tipoJustica.name() : null);
        metadata.put("grau", command.grau() != null ? command.grau().name() : null);
        metadata.put("rito", command.rito() != null ? command.rito().name() : null);
        metadata.put("cidadeAutor", command.cidadeAutor());
        metadata.put("cidadeReu", command.cidadeReu());
        metadata.put("cidadeFato", command.cidadeFato());
        metadata.put("foroExpressamenteInformado", command.foro());
        metadata.put("secaoJudiciariaExpressamenteInformada", command.secaoJudiciaria());
        metadata.put("subsecaoJudiciariaExpressamenteInformada", command.subsecaoJudiciaria());
        metadata.put("circunscricaoExpressamenteInformada", command.circunscricao());
        metadata.put("preventionReference", command.preventionReference());
        metadata.put("territorialRegistry", catalog.territorialRegistry());
        metadata.put("venueMesh", venueMesh.toMap());
        metadata.put("territorialLabel", firstNonBlank(foro, subsecao, comarca, cidadeBase, uf));
        metadata.put("territoryToken", normalizeUf(firstNonBlank(uf, "BR")) + "_" + normalizeToken(firstNonBlank(subsecao, comarca, cidadeBase, foro, circunscricao, "BASE")));
        metadata.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);

        boolean automatico = uf != null
                && firstNonBlank(subsecao, comarca, cidadeBase, circunscricao) != null
                && !catalog.specialTerritorialReview()
                && !venueMesh.forumReviewRequired();
        return new TerritorialRoutingProfile(
                mode,
                uf,
                cidadeBase,
                comarca,
                foro,
                secao,
                subsecao,
                circunscricao,
                unidadeBase,
                preventionMode,
                automatico,
                List.copyOf(warnings),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private String resolveMode(NationalProcessRoutingService.RoutingCommand command, TipoJustica tipoJustica) {
        if (!isBlank(command.foro()) || !isBlank(command.secaoJudiciaria()) || !isBlank(command.subsecaoJudiciaria()) || !isBlank(command.circunscricao())) {
            return "TERRITORIO_EXPRESSO";
        }
        if (tipoJustica == TipoJustica.FEDERAL && !isBlank(command.cidadeAutor())) {
            return "DOMICILIO_AUTOR_FEDERAL";
        }
        if ((tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL || (command.rito() != null && command.rito().isPenal()))
                && !isBlank(firstNonBlank(command.cidadeFato(), command.municipioFato()))) {
            return "LOCAL_FATO";
        }
        if (tipoJustica == TipoJustica.TRABALHO && !isBlank(command.cidadeFato())) {
            return "LOCAL_PRESTACAO_SERVICO";
        }
        if (!isBlank(command.comarca()) || !isBlank(command.cidade())) {
            return "BASE_TERRITORIAL_DECLARADA";
        }
        if (!isBlank(command.cidadeAutor())) {
            return "DOMICILIO_AUTOR";
        }
        if (!isBlank(command.cidadeReu())) {
            return "DOMICILIO_REU";
        }
        return "INDEFINIDO";
    }

    private String deriveForo(TipoJustica tipoJustica, String cidade, String uf) {
        String base = firstNonBlank(cidade, "BASE TERRITORIAL");
        String suffix = isBlank(uf) ? "" : "/" + uf;
        if (tipoJustica == null) {
            return "Foro de " + base + suffix;
        }
        return switch (tipoJustica) {
            case FEDERAL -> "Subseção Judiciária de " + base + suffix;
            case TRABALHO -> "Foro Trabalhista de " + base + suffix;
            case ELEITORAL -> "Zona/Foro Eleitoral de " + base + suffix;
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "Auditoria/Circunscrição Militar de " + base + suffix;
            case SUPERIOR -> "Órgão de origem territorial de " + base + suffix;
            default -> "Comarca/Foro de " + base + suffix;
        };
    }

    private String deriveSecao(TipoJustica tipoJustica, String uf) {
        if (tipoJustica != TipoJustica.FEDERAL || isBlank(uf)) {
            return null;
        }
        return "Seção Judiciária do " + uf;
    }

    private String deriveSubsecao(TipoJustica tipoJustica, String cidade, String uf) {
        if (tipoJustica != TipoJustica.FEDERAL || isBlank(cidade)) {
            return null;
        }
        return "Subseção Judiciária de " + cidade + (isBlank(uf) ? "" : "/" + uf);
    }

    private String deriveCircunscricao(TipoJustica tipoJustica, String cidade, String uf) {
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return isBlank(cidade) ? null : "Zona Eleitoral de " + cidade + (isBlank(uf) ? "" : "/" + uf);
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
            return isBlank(cidade) ? null : "Circunscrição/Auditoria de " + cidade + (isBlank(uf) ? "" : "/" + uf);
        }
        return null;
    }

    private String resolveUnidadeBase(TipoJustica tipoJustica, GrauJurisdicao grau, RitoProcessual rito) {
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            return "PLENARIO";
        }
        if (grau == GrauJurisdicao.SUPERIOR) {
            return "TRIBUNAL_SUPERIOR";
        }
        if (grau == GrauJurisdicao.SEGUNDO_GRAU) {
            return tipoJustica == TipoJustica.TRABALHO ? "TURMA_REGIONAL_TRABALHISTA"
                    : tipoJustica == TipoJustica.ELEITORAL ? "PLENARIO_REGIONAL_ELEITORAL"
                    : "CAMARA_RECURSAL";
        }
        if (rito != null && rito.name().startsWith("JUIZADO")) {
            return "JUIZADO";
        }
        return switch (tipoJustica == null ? TipoJustica.ESTADUAL : tipoJustica) {
            case FEDERAL -> "VARA_FEDERAL";
            case TRABALHO -> "VARA_TRABALHO";
            case ELEITORAL -> "ZONA_ELEITORAL";
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "AUDITORIA_MILITAR";
            case SUPERIOR -> "TRIBUNAL_SUPERIOR";
            default -> "VARA_ESTADUAL";
        };
    }

    private String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
    }

    private String suffixUf(String tribunalCodigoHint) {
        if (isBlank(tribunalCodigoHint)) {
            return null;
        }
        String trimmed = tribunalCodigoHint.trim().toUpperCase(Locale.ROOT);
        return trimmed.length() >= 2 ? trimmed.substring(trimmed.length() - 2) : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
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

    private String normalizeUf(String uf) {
        if (isBlank(uf)) {
            return null;
        }
        return uf.trim().toUpperCase(Locale.ROOT);
    }
}
