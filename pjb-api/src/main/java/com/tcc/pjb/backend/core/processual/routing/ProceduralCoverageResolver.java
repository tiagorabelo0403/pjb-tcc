package com.tcc.pjb.backend.core.processual.routing;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ProceduralCoverageResolver {

    public ProceduralCoverageProfile resolve(NationalProcessRoutingService.RoutingCommand command,
                                             TipoJustica tipoJustica,
                                             NationalCompetenceMatrix competencia,
                                             TerritorialRoutingProfile territorial,
                                             RelationalRoutingProfile relational,
                                             FracionaryOrganRoutingProfile fracionary,
                                             String specializationAxis) {
        Objects.requireNonNull(command, "command");
        RitoProcessual rito = command.rito();
        RamoDireito ramo = command.ramo() == null && rito != null ? rito.suggestedRamo() : command.ramo();
        GrauJurisdicao grau = command.grau();

        String justiceTrack = resolveJusticeTrack(tipoJustica, grau);
        String tribunalTier = resolveTribunalTier(competencia, tipoJustica, grau);
        String riteFamily = resolveRiteFamily(rito);
        String materialityAxis = resolveMaterialityAxis(rito, ramo, specializationAxis);
        String forumScope = firstNonBlank(
                territorial.foro(),
                territorial.secaoJudiciaria(),
                territorial.subsecaoJudiciaria(),
                territorial.comarca(),
                territorial.cidade(),
                territorial.circunscricao(),
                command.comarca(),
                command.cidade(),
                "FORO_BASE_NACIONAL");
        String territorialAnchor = firstNonBlank(
                territorial.territorialLabel(),
                territorial.deskHint(),
                command.foro(),
                command.secaoJudiciaria(),
                command.subsecaoJudiciaria(),
                command.circunscricao(),
                command.comarca(),
                command.cidade(),
                command.uf(),
                "BR");
        String admissibilityChannel = firstNonBlank(
                fracionary.admissibilityDesk(),
                relational.targetDeskProfile(),
                territorial.deskHint(),
                "MESA_ADMISSIBILIDADE_PADRAO");
        String executionTrack = resolveExecutionTrack(rito, grau, tipoJustica);
        String recursalTrack = resolveRecursalTrack(grau, tipoJustica, fracionary, competencia);
        String preventionAnchor = firstNonBlank(
                relational.targetReference(),
                relational.preventionMode(),
                command.preventionReference(),
                command.processoReferencia(),
                territorial.territoryToken(),
                "SEM_PREVENCAO_MATERIAL");
        String concurrencyEnvelope = resolveConcurrencyEnvelope(command, rito, relational, territorial);

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        fundamentos.add("Cobertura material classificada em " + materialityAxis + '.');
        fundamentos.add("Trilha jurisdicional consolidada em " + justiceTrack + '.');
        fundamentos.add("Escopo territorial ancorado em " + territorialAnchor + '.');
        fundamentos.add("Canal de admissibilidade sugerido: " + admissibilityChannel + '.');
        fundamentos.add("Esteira recursal prevista: " + recursalTrack + '.');
        fundamentos.add("Esteira executiva prevista: " + executionTrack + '.');

        if (territorial.foro() == null && territorial.secaoJudiciaria() == null && territorial.comarca() == null) {
            warnings.add("Cobertura territorial depende de saneamento manual entre foro, seção ou comarca.");
            reviewChecklist.add("Conferir foro, subseção ou comarca antes da distribuição automática.");
        }
        if (!"AUTONOMA".equals(firstNonBlank(relational.linkageMode(), "AUTONOMA"))) {
            reviewChecklist.add("Validar prevenção, dependência, conexão ou continência antes da fixação final da unidade.");
        }
        if (command.grau() != GrauJurisdicao.PRIMEIRO_GRAU) {
            reviewChecklist.add("Confirmar órgão fracionário, relatoria e esteira recursal na camada colegiada.");
        }
        if (command.pedidoLiminar() || command.plantaoJudicial()) {
            reviewChecklist.add("Checar aderência do rito à trilha urgente e ao plantão competente.");
        }
        if (command.segredoSolicitado() || (rito != null && rito.requiresSegredoByDefault())) {
            reviewChecklist.add("Aplicar sigilo reforçado em protocolo, secretaria, pauta e gabinete.");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("courtCode", competencia != null ? competencia.codigo() : null);
        metadata.put("courtName", competencia != null ? competencia.nome() : null);
        metadata.put("justiceSegment", tipoJustica != null ? tipoJustica.name() : null);
        metadata.put("grade", grau != null ? grau.name() : null);
        metadata.put("specializationAxis", specializationAxis);
        metadata.put("territorialToken", territorial.territoryToken());
        metadata.put("strictPrevention", relational.strictPrevention());
        metadata.put("virtualSessionEligible", fracionary.virtualSessionEligible());
        metadata.put("descriptor", String.join(":",
                normalize(justiceTrack, "JUSTICA"),
                normalize(tribunalTier, "TRIBUNAL"),
                normalize(materialityAxis, "MATERIALIDADE"),
                normalize(forumScope, "FORO"),
                normalize(concurrencyEnvelope, "CONCORRENCIA")));
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new ProceduralCoverageProfile(
                justiceTrack,
                tribunalTier,
                riteFamily,
                materialityAxis,
                forumScope,
                territorialAnchor,
                admissibilityChannel,
                executionTrack,
                recursalTrack,
                preventionAnchor,
                concurrencyEnvelope,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private String resolveJusticeTrack(TipoJustica tipoJustica, GrauJurisdicao grau) {
        String justice = tipoJustica == null ? "ESTADUAL" : tipoJustica.name();
        String tier = switch (grau) {
            case PRIMEIRO_GRAU -> "1G";
            case SEGUNDO_GRAU -> "2G";
            case SUPERIOR -> "SUPERIOR";
            case CONSTITUCIONAL -> "CONSTITUCIONAL";
        };
        return justice + '_' + tier;
    }

    private String resolveTribunalTier(NationalCompetenceMatrix competencia, TipoJustica tipoJustica, GrauJurisdicao grau) {
        String base = competencia == null ? firstNonBlank(tipoJustica != null ? tipoJustica.name() : null, "TRIBUNAL") : competencia.codigo();
        return base + '_' + switch (grau) {
            case PRIMEIRO_GRAU -> "ORIGEM";
            case SEGUNDO_GRAU -> "SEGUNDO_GRAU";
            case SUPERIOR -> "CORTE_SUPERIOR";
            case CONSTITUCIONAL -> "CORTE_CONSTITUCIONAL";
        };
    }

    private String resolveRiteFamily(RitoProcessual rito) {
        if (rito == null) {
            return "COMUM";
        }
        if (rito.isPenal()) {
            return "PENAL";
        }
        if (rito.isTrabalhista()) {
            return "TRABALHISTA";
        }
        if (rito.isPrevidenciario()) {
            return "PREVIDENCIARIO";
        }
        if (rito.isTribFazenda()) {
            return "FAZENDA_TRIBUTARIO";
        }
        if (rito.isEleitoral()) {
            return "ELEITORAL";
        }
        if (rito.isMilitar()) {
            return "MILITAR";
        }
        if (rito.isEspecialConstitucional()) {
            return "CONSTITUCIONAL";
        }
        if (rito.isAutocompositivo()) {
            return "AUTOCOMPOSITIVO";
        }
        if (rito.isEmpresarial()) {
            return "EMPRESARIAL";
        }
        if (rito.isInternacional()) {
            return "COOPERACAO_INTERNACIONAL";
        }
        if (rito.isAmbiental()) {
            return "AMBIENTAL";
        }
        if (rito.isAdministrativo()) {
            return "ADMINISTRATIVO";
        }
        if (rito.isInfancia()) {
            return "INFANCIA_JUVENTUDE";
        }
        if (rito.isAgrario()) {
            return "AGRARIO";
        }
        if (rito.name().startsWith("JUIZADO")) {
            return "JUIZADOS";
        }
        if (rito.name().startsWith("EXECUCAO") || rito.name().startsWith("CUMPRIMENTO")) {
            return "EXECUTIVO";
        }
        return "CIVIL";
    }

    private String resolveMaterialityAxis(RitoProcessual rito, RamoDireito ramo, String specializationAxis) {
        if (rito == null) {
            return firstNonBlank(specializationAxis, ramo != null ? ramo.name() : null, "CIVIL_PATRIMONIAL");
        }
        if (rito == RitoProcessual.CIVIL_FAMILIA_ALIMENTOS
                || rito == RitoProcessual.CIVIL_FAMILIA_DIVORCIO
                || rito == RitoProcessual.CIVIL_DISSOLUCAO_CASAMENTO
                || rito == RitoProcessual.CIVIL_INVENTARIO_ARROLAMENTO) {
            return "FAMILIA_SUCESSOES";
        }
        if (rito.isInfancia() || rito == RitoProcessual.CIVIL_ADOCAO) {
            return "INFANCIA_PROTECAO";
        }
        if (rito.isPenal() || rito == RitoProcessual.TRIBUNAL_JURI) {
            return "CRIMINAL_PERSECUCAO";
        }
        if (rito.isPrevidenciario()) {
            return "PREVIDENCIARIO_BENEFICIO";
        }
        if (rito.isTribFazenda()) {
            return rito == RitoProcessual.EXECUCAO_FISCAL ? "FAZENDA_EXECUCAO_FISCAL" : "FAZENDA_CONTENCIOSO";
        }
        if (rito.isTrabalhista()) {
            return rito == RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO ? "TRABALHO_COLETIVO" : "TRABALHO_INDIVIDUAL";
        }
        if (rito.isEleitoral()) {
            return "ELEITORAL_CONTENCIOSO";
        }
        if (rito.isMilitar()) {
            return "MILITAR_DISCIPLINA_E_PENAL";
        }
        if (rito.isAmbiental()) {
            return "AMBIENTAL_ESTRUTURAL";
        }
        if (rito.isAdministrativo()) {
            return "ADMINISTRATIVO_CONTROLE";
        }
        if (rito.isAgrario()) {
            return "AGRARIO_CONFLITO_FUNDIARIO";
        }
        if (rito.isEmpresarial()) {
            return "EMPRESARIAL_REESTRUTURACAO";
        }
        if (rito.isInternacional()) {
            return "COOPERACAO_INTERNACIONAL";
        }
        if (rito.isAutocompositivo()) {
            return "AUTOCOMPOSICAO_ASSISTIDA";
        }
        if (rito.name().startsWith("JUIZADO")) {
            return "JUIZADO_MENOR_COMPLEXIDADE";
        }
        if (rito.name().startsWith("EXECUCAO") || rito.name().startsWith("CUMPRIMENTO")) {
            return "EXECUCAO_CUMPRIMENTO";
        }
        return firstNonBlank(specializationAxis, ramo != null ? ramo.name() : null, "CIVIL_PATRIMONIAL");
    }

    private String resolveExecutionTrack(RitoProcessual rito, GrauJurisdicao grau, TipoJustica tipoJustica) {
        if (rito == null) {
            return "CONHECIMENTO_PADRAO";
        }
        if (rito == RitoProcessual.EXECUCAO_FISCAL) {
            return "EXECUCAO_FISCAL_ESPECIALIZADA";
        }
        if (rito == RitoProcessual.EXECUCAO_PENAL) {
            return "EXECUCAO_PENAL_JUÍZO_EXECUCAO";
        }
        if (rito.name().startsWith("EXECUCAO") || rito.name().startsWith("CUMPRIMENTO")) {
            return "EXECUCAO_CUMPRIMENTO_DIRIGIDO";
        }
        if (grau == GrauJurisdicao.SEGUNDO_GRAU || grau == GrauJurisdicao.SUPERIOR || grau == GrauJurisdicao.CONSTITUCIONAL) {
            return "JULGAMENTO_COLEGIADO_COM_FASE_EXECUTIVA_POSTERIOR";
        }
        return firstNonBlank(tipoJustica != null ? tipoJustica.name() : null, "CONHECIMENTO") + "_CONHECIMENTO";
    }

    private String resolveRecursalTrack(GrauJurisdicao grau,
                                        TipoJustica tipoJustica,
                                        FracionaryOrganRoutingProfile fracionary,
                                        NationalCompetenceMatrix competencia) {
        String base = competencia != null ? competencia.codigo() : firstNonBlank(tipoJustica != null ? tipoJustica.name() : null, "TRIBUNAL");
        return switch (grau) {
            case PRIMEIRO_GRAU -> "ORIGEM_PARA_" + base;
            case SEGUNDO_GRAU -> firstNonBlank(fracionary.orgaoFracionario(), "COLEGIADO_SEGUNDO_GRAU") + "_PARA_SUPERIOR";
            case SUPERIOR -> firstNonBlank(fracionary.orgaoFracionario(), "CORTE_SUPERIOR") + "_PARA_CONSTITUCIONAL";
            case CONSTITUCIONAL -> firstNonBlank(fracionary.orgaoFracionario(), "CORTE_CONSTITUCIONAL") + "_FINAL";
        };
    }

    private String resolveConcurrencyEnvelope(NationalProcessRoutingService.RoutingCommand command,
                                              RitoProcessual rito,
                                              RelationalRoutingProfile relational,
                                              TerritorialRoutingProfile territorial) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add(command.plantaoJudicial() ? "PLANTAO" : "ROTINA");
        if (command.pedidoLiminar()) {
            tags.add("TUTELA_URGENTE");
        }
        if (command.segredoSolicitado() || (rito != null && rito.requiresSegredoByDefault())) {
            tags.add("SIGILO_REFORCADO");
        }
        if (command.redistribuicaoImpedimento()) {
            tags.add("REDISTRIBUICAO_IMPEDIMENTO");
        }
        if (!territorial.aptoDistribuicaoAutomatica()) {
            tags.add("SANEAMENTO_TERRITORIAL");
        }
        if (relational.strictPrevention()) {
            tags.add("PREVENCAO_ESTRITA");
        }
        return String.join("_", tags);
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

    private static String normalize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
        return normalized.isBlank() ? fallback : normalized;
    }
}
