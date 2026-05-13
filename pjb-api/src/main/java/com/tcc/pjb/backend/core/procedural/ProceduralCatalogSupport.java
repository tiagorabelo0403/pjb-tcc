package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import com.tcc.pjb.backend.service.rito.model.WorkTemplate;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class ProceduralCatalogSupport {

    public record PartyRoleSpec(String code, boolean required, boolean external, List<String> aliases) {
        public PartyRoleSpec {
            aliases = aliases == null ? List.of() : List.copyOf(new LinkedHashSet<>(aliases));
        }
    }

    public record DocumentSpec(String code, boolean required, String rationale) {
    }

    public record DefinitionSnapshot(
            RitoProcessual rito,
            String title,
            String ramo,
            List<PartyRoleSpec> parties,
            List<DocumentSpec> documents,
            List<RitoStage> stages,
            List<String> competenceHints,
            boolean generated) {
    }

    private static final List<String> BASE_CIVIL_COMPETENCE = List.of("vara civel", "juizado especial", "camara civel", "tribunal competente");
    private static final List<String> BASE_TRABALHO_COMPETENCE = List.of("vara do trabalho", "trt competente", "tst em sede recursal");
    private static final List<String> BASE_PENAL_COMPETENCE = List.of("vara criminal", "juizado especial criminal", "tribunal do juri", "tribunal competente");
    private static final List<String> BASE_ELEITORAL_COMPETENCE = List.of("zona eleitoral", "tre competente", "tse em sede recursal ou originaria");
    private static final List<String> BASE_MILITAR_COMPETENCE = List.of("auditoria militar", "conselho de justica", "stm ou tjm competente");
    private static final List<String> BASE_TRIBUTARIO_COMPETENCE = List.of("vara da fazenda publica", "vara federal", "juizado fazendario", "camara de direito publico");
    private static final List<String> BASE_PREVIDENCIARIO_COMPETENCE = List.of("vara federal", "jef competente", "trf competente");
    private static final Map<String, List<String>> ROLE_ALIASES = buildRoleAliases();
    private static final List<RitoProcessual> CATALOG_DRIVEN_RITOS = buildCatalogDrivenRitos();

    private ProceduralCatalogSupport() {
    }


    public static DefinitionSnapshot snapshot(RitoProcessual rito) {
        RitoProcessual resolved = rito == null ? RitoProcessual.COMUM_ORDINARIO : rito;
        return ProceduralCatalogDefinitionSupport.snapshot(resolved);
    }

    public static RitoDefinition enrichDefinition(RitoProcessual rito, RitoDefinition base) {
        DefinitionSnapshot snapshot = snapshot(rito);
        RitoDefinition resolved = new RitoDefinition();
        resolved.setRito(rito.name());
        resolved.setTitle(ProceduralCatalogStageSupport.firstNonBlank(base != null ? base.getTitle() : null, snapshot.title(), ProceduralCatalogStageSupport.humanize(rito.name())));
        resolved.setRamoSugerido(ProceduralCatalogStageSupport.firstNonBlank(base != null ? base.getRamoSugerido() : null, snapshot.ramo(), rito.suggestedRamo().name()));
        resolved.setStages(ProceduralCatalogStageSupport.mergeStages(base != null ? base.getStages() : null, snapshot.stages()));
        return resolved;
    }

    public static List<String> requiredDocuments(RitoProcessual rito) {
        return snapshot(rito).documents().stream().filter(DocumentSpec::required).map(DocumentSpec::code).toList();
    }

    public static List<PartyRoleSpec> requiredParties(RitoProcessual rito) {
        return snapshot(rito).parties();
    }

    public static Map<String, Object> describe(RitoProcessual rito) {
        DefinitionSnapshot snapshot = snapshot(rito);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rito", snapshot.rito().name());
        out.put("title", snapshot.title());
        out.put("ramo", snapshot.ramo());
        out.put("generated", snapshot.generated());
        out.put("partySchema", snapshot.parties().stream().map(p -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", p.code());
            item.put("required", p.required());
            item.put("external", p.external());
            item.put("aliases", p.aliases());
            return item;
        }).toList());
        out.put("documentRequirements", snapshot.documents().stream().map(d -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", d.code());
            item.put("required", d.required());
            item.put("rationale", d.rationale());
            return item;
        }).toList());
        out.put("competenceHints", snapshot.competenceHints());
        out.put("stages", snapshot.stages().stream().map(ProceduralCatalogStageSupport::stageToMap).toList());
        return out;
    }

    public static List<Map<String, Object>> catalog() {
        return catalogDrivenRitos().stream()
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .map(ProceduralCatalogSupport::describe)
                .toList();
    }

    public static List<RitoProcessual> catalogDrivenRitos() {
        return CATALOG_DRIVEN_RITOS;
    }

    public static List<RitoProcessual> allKnownRitos() {
        LinkedHashSet<RitoProcessual> ordered = new LinkedHashSet<>(CATALOG_DRIVEN_RITOS);
        ordered.addAll(Arrays.asList(RitoProcessual.values()));
        return List.copyOf(ordered);
    }

    public static List<String> aliasesForRole(String roleCode) {
        return ROLE_ALIASES.getOrDefault(normalizeToken(roleCode), List.of());
    }

    public static RitoProcessual resolveRito(String ritoRaw, String ramoRaw, String classeTpu) {
        return tryResolveRito(ritoRaw, ramoRaw, classeTpu).orElse(RitoProcessual.COMUM_ORDINARIO);
    }

    public static Optional<RitoProcessual> tryResolveRito(String ritoRaw, String ramoRaw, String classeTpu) {
        RitoProcessual fromRito = RitoProcessual.tryParseExact(ritoRaw).orElse(null);
        if (fromRito != null) {
            return Optional.of(fromRito);
        }
        Optional<TpuClasseCnj> classe = resolveClasseTpu(classeTpu);
        if (classe.isPresent()) {
            return Optional.of(ritoFromClasseTpu(classe.get()));
        }
        String joined = normalizeToken(ritoRaw) + ' ' + normalizeToken(ramoRaw) + ' ' + normalizeToken(classeTpu);
        if (joined.contains("MANDADO DE SEGURANCA") || joined.contains("MANDADO_SEGURANCA") || joined.contains("MANDADO_INJUNCAO")) {
            return Optional.of(RitoProcessual.ESPECIAL_MANDADO_SEGURANCA);
        }
        if (joined.contains("HABEAS CORPUS") || joined.contains("HABEAS_CORPUS")) {
            return Optional.of(RitoProcessual.ESPECIAL_HABEAS_CORPUS);
        }
        if (joined.contains("RECURSO CONTRA EXPEDICAO DO DIPLOMA") || joined.contains("RECURSO CONTRA EXPEDIÇÃO DO DIPLOMA") || joined.contains("RCED")) {
            return Optional.of(RitoProcessual.ELEITORAL_RCED);
        }
        if (joined.contains("CAPTACAO ILICITA SUFRAGIO") || joined.contains("CAPTAÇÃO ILÍCITA DE SUFRÁGIO") || joined.contains("COMPRA DE VOTOS") || joined.contains("ART 41 A")) {
            return Optional.of(RitoProcessual.ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO);
        }
        if (joined.contains("ELEITOR")) {
            return Optional.of(joined.contains("CONTAS") ? RitoProcessual.ELEITORAL_PRESTACAO_CONTAS : RitoProcessual.ELEITORAL);
        }
        if (joined.contains("CONSELHO DE JUSTICA") || joined.contains("CONSELHO JUSTICA") || joined.contains("ESCABINATO")) {
            return Optional.of(RitoProcessual.MILITAR_CONSELHO_JUSTICA);
        }
        if (joined.contains("MILIT")) {
            return Optional.of(joined.contains("IPM") ? RitoProcessual.MILITAR_IPM : RitoProcessual.MILITAR);
        }
        if (joined.contains("TRABALH")) {
            if (joined.contains("INQUERITO") && joined.contains("FALTA GRAVE")) {
                return Optional.of(RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE);
            }
            if (joined.contains("ACAO CUMPRIMENTO") || joined.contains("AÇÃO CUMPRIMENTO") || joined.contains("ART 872")) {
                return Optional.of(RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO);
            }
            if (joined.contains("ALCADA") || joined.contains("ALÇADA") || joined.contains("SUMARIO") || joined.contains("LEI 5 584") || joined.contains("LEI 5584")) {
                return Optional.of(RitoProcessual.TRABALHISTA_SUMARIO_ALCADA);
            }
            if (joined.contains("SUMARISSIMO")) {
                return Optional.of(RitoProcessual.TRABALHISTA_SUMARISSIMO);
            }
            return Optional.of(joined.contains("DISSIDIO") ? RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO : RitoProcessual.TRABALHISTA_ORDINARIO);
        }
        if (joined.contains("LOAS") || joined.contains("BPC")) {
            return Optional.of(RitoProcessual.PREVIDENCIARIO_BPC_LOAS);
        }
        if (joined.contains("AUXILIO POR INCAPACIDADE") || joined.contains("AUXILIO DOENCA") || joined.contains("AUXILIO ACIDENTE") || joined.contains("INCAPACIDADE LABORATIVA")) {
            return Optional.of(RitoProcessual.PREVIDENCIARIO_AUXILIO_INCAPACIDADE);
        }
        if (joined.contains("APOSENTADORIA ESPECIAL") || joined.contains("PPP") || joined.contains("LTCAT")) {
            return Optional.of(RitoProcessual.PREVIDENCIARIO_ESPECIAL);
        }
        if (joined.contains("APOSENTADORIA")) {
            return Optional.of(RitoProcessual.PREVIDENCIARIO_APOSENTADORIA);
        }
        if (joined.contains("REVISAO DE BENEFICIO") || joined.contains("REVISIONAL PREVIDENCIARIA")) {
            return Optional.of(RitoProcessual.PREVIDENCIARIO_REVISAO_BENEFICIO);
        }
        if (joined.contains("RESTABELECIMENTO DE BENEFICIO") || joined.contains("CESSACAO INDEVIDA DE BENEFICIO")) {
            return Optional.of(RitoProcessual.PREVIDENCIARIO_RESTABELECIMENTO);
        }
        if (joined.contains("SALARIO MATERNIDADE")) {
            return Optional.of(RitoProcessual.PREVIDENCIARIO_SALARIO_MATERNIDADE);
        }
        if (joined.contains("PENSAO POR MORTE")) {
            return Optional.of(RitoProcessual.PREVIDENCIARIO_PENSAO_MORTE);
        }
        if (joined.contains("SEGURADO ESPECIAL") || joined.contains("TRABALHADOR RURAL")) {
            return Optional.of(RitoProcessual.PREVIDENCIARIO_RURAL);
        }
        if (joined.contains("RPPS") || joined.contains("REGIME PROPRIO DE PREVIDENCIA")) {
            return Optional.of(RitoProcessual.PREVIDENCIARIO_RPPS);
        }
        if (joined.contains("PREVID") || joined.contains("INSS")) {
            return Optional.of(RitoProcessual.PREVIDENCIARIO_COMUM);
        }
        if (joined.contains("PROCESSO ADMINISTRATIVO DISCIPLINAR") || joined.contains("PAD") || joined.contains("SINDICANCIA") || joined.contains("SINDICÂNCIA") || joined.contains("COMISSAO PROCESSANTE") || joined.contains("COMISSÃO PROCESSANTE")) {
            return Optional.of(RitoProcessual.ADMINISTRATIVO_PAD);
        }
        if (joined.contains("CONCURSO PUBLICO") || joined.contains("EDITAL") || joined.contains("NOMEACAO") || joined.contains("NOMEAÇÃO") || joined.contains("POSSE EM CARGO PUBLICO") || joined.contains("POSSE EM CARGO PÚBLICO")) {
            return Optional.of(RitoProcessual.ADMINISTRATIVO_CONCURSO_PUBLICO);
        }
        if (joined.contains("SERVIDOR PUBLICO") || joined.contains("SERVIDOR PÚBLICO") || joined.contains("REENQUADRAMENTO FUNCIONAL") || joined.contains("PROGRESSAO FUNCIONAL") || joined.contains("PROGRESSÃO FUNCIONAL")) {
            return Optional.of(RitoProcessual.ADMINISTRATIVO_SERVIDORES);
        }
        if (joined.contains("TRIBUT") || joined.contains("FAZENDA") || joined.contains("FISCAL")) {
            return Optional.of(joined.contains("EXECUCAO") ? RitoProcessual.EXECUCAO_FISCAL : RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO);
        }
        if (joined.contains("PENAL") || joined.contains("CRIM")) {
            return Optional.of(joined.contains("JURI") ? RitoProcessual.TRIBUNAL_JURI : RitoProcessual.PROCEDIMENTO_PENAL_COMUM);
        }
        if (joined.contains("AGRAR")) {
            return Optional.of(joined.contains("DESAPROPRI") ? RitoProcessual.AGRARIO_DESAPROPRIACAO : RitoProcessual.AGRARIO_POSSE_TERRA);
        }
        if (joined.contains("ATO INFRACIONAL") || joined.contains("MEDIDA SOCIOEDUCATIVA") || joined.contains("APURACAO DE ATO INFRACIONAL") || joined.contains("APURAÇÃO DE ATO INFRACIONAL") || joined.contains("SEMILIBERDADE") || joined.contains("INTERNACAO") || joined.contains("INTERNAÇÃO")) {
            return Optional.of(RitoProcessual.INFANCIA_JUVENTUDE_INFRACIONAL);
        }
        if (joined.contains("ADOCAO") || joined.contains("ADOÇÃO") || joined.contains("HABILITACAO A ADOCAO") || joined.contains("HABILITAÇÃO À ADOÇÃO") || joined.contains("ESTAGIO DE CONVIVENCIA") || joined.contains("ESTÁGIO DE CONVIVÊNCIA")) {
            return Optional.of(RitoProcessual.INFANCIA_JUVENTUDE_ADOCAO);
        }
        if ((joined.contains("GUARDA") || joined.contains("TUTELA") || joined.contains("CURATELA")) && (joined.contains("MENOR") || joined.contains("CRIANCA") || joined.contains("CRIANÇA") || joined.contains("ADOLESCENTE") || joined.contains("ECA") || joined.contains("INFANCIA") || joined.contains("INFÂNCIA"))) {
            return Optional.of(RitoProcessual.INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR);
        }
        if (joined.contains("INFANCIA") || joined.contains("INFÂNCIA") || joined.contains("ADOCAO") || joined.contains("ADOÇÃO") || joined.contains("ECA") || joined.contains("CONSELHO TUTELAR") || joined.contains("ACOLHIMENTO INSTITUCIONAL")) {
            return Optional.of(RitoProcessual.INFANCIA_JUVENTUDE_ECA);
        }
        if (joined.contains("AMBIENT")) {
            return Optional.of(joined.contains("CRIM") ? RitoProcessual.AMBIENTAL_CRIMINAL : RitoProcessual.AMBIENTAL_ACP);
        }
        if (joined.contains("FALEN") || joined.contains("RECUPERACAO") || joined.contains("EMPRESAR")) {
            return Optional.of(joined.contains("FALEN") ? RitoProcessual.FALENCIA : RitoProcessual.RECUPERACAO_JUDICIAL);
        }
        if (joined.contains("ARBITRAG")) {
            return Optional.of(RitoProcessual.ARBITRAGEM);
        }
        if (joined.contains("MEDIAC") || joined.contains("CONCILIAC")) {
            return Optional.of(joined.contains("CONCILIAC") ? RitoProcessual.CONCILIACAO_EXTRAJUDICIAL : RitoProcessual.MEDIACAO);
        }
        if (joined.contains("ACAO DE DESCUMPRIMENTO DE OBRIGACAO") || joined.contains("AÇÃO DE DESCUMPRIMENTO DE OBRIGAÇÃO") || joined.contains("DESCUMPRIMENTO DE OBRIGACAO ESPECIFICA")) {
            return Optional.of(RitoProcessual.ESPECIAL_ACAO_DESCUMPRIMENTO_OBRIGACAO);
        }
        if (joined.contains("ROGATORIA") || joined.contains("ESTRANGEIR") || joined.contains("INTERNACIONAL") || joined.contains("HOMOLOGACAO_SENTENCA")) {
            return Optional.of(joined.contains("ROGATORIA") ? RitoProcessual.CARTA_ROGATORIA : RitoProcessual.COOPERACAO_JURIDICA_INTERNACIONAL);
        }
        return Optional.empty();
    }

    private static List<RitoProcessual> buildCatalogDrivenRitos() {
        LinkedHashSet<RitoProcessual> out = new LinkedHashSet<>();
        for (TpuClasseCnj classe : TpuClasseCnj.todasEmOrdemCodigo()) {
            out.add(ritoFromClasseTpu(classe));
        }
        out.add(RitoProcessual.ELEITORAL_AIRC);
        out.add(RitoProcessual.ELEITORAL_AIJE);
        out.add(RitoProcessual.ELEITORAL_AIME);
        out.add(RitoProcessual.ELEITORAL_RCED);
        out.add(RitoProcessual.ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO);
        out.add(RitoProcessual.ELEITORAL_INELEGIBILIDADE);
        out.add(RitoProcessual.MILITAR_PAD);
        out.add(RitoProcessual.MILITAR_CONSELHO_JUSTICA);
        out.add(RitoProcessual.EXECUCAO_PENAL);
        out.add(RitoProcessual.TRABALHISTA_SUMARIO_ALCADA);
        out.add(RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO);
        out.add(RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE);
        out.add(RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO);
        out.add(RitoProcessual.TRIBUTARIO_CAUTELAR_FISCAL);
        out.add(RitoProcessual.ADMINISTRATIVO_ACAO_POPULAR);
        out.add(RitoProcessual.ADMINISTRATIVO_ACAO_CIVIL_PUBLICA_ADM);
        out.add(RitoProcessual.ARBITRAGEM);
        out.add(RitoProcessual.MEDIACAO);
        out.add(RitoProcessual.CONCILIACAO_EXTRAJUDICIAL);
        out.add(RitoProcessual.COOPERACAO_JURIDICA_INTERNACIONAL);
        out.add(RitoProcessual.CARTA_ROGATORIA);
        out.add(RitoProcessual.HOMOLOGACAO_SENTENCA_ESTRANGEIRA);
        out.add(RitoProcessual.ESPECIAL_MANDADO_SEGURANCA);
        out.add(RitoProcessual.ESPECIAL_HABEAS_CORPUS);
        out.add(RitoProcessual.COMUM_ORDINARIO);
        return List.copyOf(out);
    }

    private static Optional<TpuClasseCnj> resolveClasseTpu(String codigoOuNome) {
        if (codigoOuNome == null || codigoOuNome.isBlank()) {
            return Optional.empty();
        }
        Optional<TpuClasseCnj> byName = TpuClasseCnj.porNome(codigoOuNome);
        if (byName.isPresent()) {
            return byName;
        }
        String digits = codigoOuNome.replaceAll("[^0-9]", "");
        if (!digits.isBlank()) {
            try {
                return TpuClasseCnj.porCodigo(Integer.parseInt(digits));
            } catch (NumberFormatException ignored) {
            }
        }
        return Optional.empty();
    }

    private static RitoProcessual ritoFromClasseTpu(TpuClasseCnj classe) {
        if (classe == null) {
            return RitoProcessual.COMUM_ORDINARIO;
        }
        String descricao = normalizeToken(classe.descricao());
        if (descricao.contains("MANDADO SEGURANCA") || descricao.contains("MANDADO INJUNCAO")) {
            return RitoProcessual.ESPECIAL_MANDADO_SEGURANCA;
        }
        if (descricao.contains("HABEAS CORPUS")) {
            return RitoProcessual.ESPECIAL_HABEAS_CORPUS;
        }
        if (descricao.contains("HABEAS DATA")) {
            return RitoProcessual.ESPECIAL_HABEAS_DATA;
        }
        if (descricao.contains("PRESTACAO CONTAS")) {
            return RitoProcessual.ELEITORAL_PRESTACAO_CONTAS;
        }
        if (descricao.contains("PROCESSO ADMINISTRATIVO DISCIPLINAR") || descricao.contains("PAD")) {
            return RitoProcessual.ADMINISTRATIVO_PAD;
        }
        if (descricao.contains("CONCURSO PUBLICO")) {
            return RitoProcessual.ADMINISTRATIVO_CONCURSO_PUBLICO;
        }
        if (descricao.contains("SERVIDOR")) {
            return RitoProcessual.ADMINISTRATIVO_SERVIDORES;
        }
        if (descricao.contains("ATO INFRACIONAL") || descricao.contains("MEDIDA SOCIOEDUCATIVA")) {
            return RitoProcessual.INFANCIA_JUVENTUDE_INFRACIONAL;
        }
        if (descricao.contains("ADOCAO")) {
            return RitoProcessual.INFANCIA_JUVENTUDE_ADOCAO;
        }
        if (descricao.contains("GUARDA DE MENOR") || descricao.contains("TUTELA DE MENOR")) {
            return RitoProcessual.INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR;
        }
        if (descricao.contains("REGISTRO CANDIDATURA")) {
            return RitoProcessual.ELEITORAL_REGISTRO_CANDIDATURA;
        }
        if (descricao.contains("PROPAGANDA") || descricao.contains("DIREITO RESPOSTA")) {
            return RitoProcessual.ELEITORAL_PROPAGANDA;
        }
        if (descricao.contains("IPM") || descricao.contains("INQUERITO POLICIAL MILITAR")) {
            return RitoProcessual.MILITAR_IPM;
        }
        if (descricao.contains("FALENCIA")) {
            return RitoProcessual.FALENCIA;
        }
        if (descricao.contains("RECUPERACAO EXTRAJUDICIAL")) {
            return RitoProcessual.RECUPERACAO_EXTRAJUDICIAL;
        }
        if (descricao.contains("RECUPERACAO JUDICIAL")) {
            return RitoProcessual.RECUPERACAO_JUDICIAL;
        }
        if (descricao.contains("EXECUCAO FISCAL")) {
            return RitoProcessual.EXECUCAO_FISCAL;
        }
        if (descricao.contains("TRIBUNAL JURI") || descricao.contains("JURI")) {
            return RitoProcessual.TRIBUNAL_JURI;
        }
        return switch (classe.ramoDireito()) {
            case ELEITORAL -> RitoProcessual.ELEITORAL;
            case MILITAR -> RitoProcessual.MILITAR;
            case TRABALHISTA -> RitoProcessual.TRABALHISTA_ORDINARIO;
            case PREVIDENCIARIO -> RitoProcessual.PREVIDENCIARIO_COMUM;
            case TRIBUTARIO, ADMINISTRATIVO -> RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO;
            case PENAL -> classe.faixaProcedimental() == TpuClasseCnj.FaixaProcedimental.SUMARISSIMO_PENAL
                    ? RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL
                    : RitoProcessual.PROCEDIMENTO_PENAL_COMUM;
            case AGRARIO -> RitoProcessual.AGRARIO_POSSE_TERRA;
            case AMBIENTAL -> RitoProcessual.AMBIENTAL_ACP;
            case INTERNACIONAL -> RitoProcessual.COOPERACAO_JURIDICA_INTERNACIONAL;
            case EMPRESARIAL -> RitoProcessual.RECUPERACAO_JUDICIAL;
            case FAMILIA -> classe.faixaProcedimental() == TpuClasseCnj.FaixaProcedimental.SUMARISSIMO
                    ? RitoProcessual.SUMARIO_ESPECIAL
                    : RitoProcessual.COMUM_ORDINARIO;
            default -> switch (classe.faixaProcedimental()) {
                case JUIZADO_SUMARISSIMO -> RitoProcessual.JUIZADO_ESPECIAL_CIVEL;
                case MANDADO_SEGURANCA -> RitoProcessual.ESPECIAL_MANDADO_SEGURANCA;
                case HABEAS_CORPUS -> RitoProcessual.ESPECIAL_HABEAS_CORPUS;
                case TUTELA_URGENCIA -> RitoProcessual.CIVIL_TUTELA_CAUTELAR_ANTECEDENTE;
                case SUMARISSIMO, SUMARISSIMO_PENAL -> RitoProcessual.SUMARIO_ESPECIAL;
                default -> RitoProcessual.COMUM_ORDINARIO;
            };
        };
    }

    private static String normalizeToken(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_{2,}", "_")
                .toUpperCase(Locale.ROOT);
        return normalized.startsWith("_") ? normalized.substring(1) : normalized;
    }

    private static Map<String, List<String>> buildRoleAliases() {
        Map<String, Set<String>> raw = new TreeMap<>();
        register(raw, "AUTOR", "autor", "parte autora", "polo ativo", "demandante", "requerente");
        register(raw, "REU", "reu", "réu", "parte ré", "parte re", "polo passivo", "demandado");
        register(raw, "IMPETRANTE", "impetrante");
        register(raw, "IMPETRADO", "impetrado");
        register(raw, "AUTORIDADE_COATORA", "autoridade coatora", "coatora");
        register(raw, "REPRESENTANTE", "representante");
        register(raw, "REPRESENTADO", "representado");
        register(raw, "IMPUGNANTE", "impugnante");
        register(raw, "IMPUGNADO", "impugnado");
        register(raw, "INVESTIGADO", "investigado");
        register(raw, "BENEFICIARIO", "beneficiário", "beneficiario");
        register(raw, "CANDIDATO", "candidato");
        register(raw, "PARTIDO_FEDERACAO_COLIGACAO", "partido", "federação", "federacao", "coligação", "coligacao");
        register(raw, "PRESTADOR_CONTAS", "prestador de contas", "prestador_contas");
        register(raw, "REQUERENTE", "requerente");
        register(raw, "REQUERIDO", "requerido");
        register(raw, "ACUSACAO", "acusação", "acusacao", "querelante", "autoria", "parte acusadora");
        register(raw, "ACUSADO", "acusado", "réu", "reu", "denunciado", "querelado");
        register(raw, "VITIMA", "vítima", "vitima", "ofendido", "ofendida");
        register(raw, "ASSISTENTE_ACUSACAO", "assistente de acusação", "assistente de acusacao");
        register(raw, "PACIENTE", "paciente");
        register(raw, "SEGURADO", "segurado", "beneficiário", "beneficiario");
        register(raw, "INSS", "inss");
        register(raw, "RECLAMANTE", "reclamante", "empregado");
        register(raw, "RECLAMADA", "reclamada", "empregador");
        register(raw, "SUSCITANTE", "suscitante");
        register(raw, "SUSCITADO", "suscitado");
        register(raw, "FAZENDA_PUBLICA", "fazenda pública", "fazenda publica", "ente público", "ente publico");
        register(raw, "EXEQUENTE", "exequente", "credor");
        register(raw, "EXECUTADO", "executado", "devedor");
        register(raw, "AUTOR_POPULAR", "autor popular", "cidadão", "cidadao");
        register(raw, "AUTORIDADE_MILITAR", "autoridade militar", "comandante", "encarregado ipm");
        register(raw, "INVESTIGADO", "indiciado");
        register(raw, "OFENDIDO", "ofendido", "ofendida", "vítima", "vitima");
        register(raw, "DEFESA", "defesa", "advogado", "defensor");
        register(raw, "MINISTERIO_PUBLICO", "ministério público", "ministerio publico", "mp");
        register(raw, "MINISTERIO_PUBLICO_TRABALHO", "mpt", "ministério público do trabalho", "ministerio publico do trabalho");
        register(raw, "MINISTERIO_PUBLICO_ELEITORAL", "mpe", "ministério público eleitoral", "ministerio publico eleitoral");
        register(raw, "MINISTERIO_PUBLICO_MILITAR", "mpm", "ministério público militar", "ministerio publico militar");
        register(raw, "DEVEDOR", "devedor", "empresa recuperanda");
        register(raw, "CREDOR", "credor");
        register(raw, "ADMINISTRADOR_JUDICIAL", "administrador judicial");
        register(raw, "CRIANCA_ADOLESCENTE", "criança", "crianca", "adolescente", "menor");
        register(raw, "REPRESENTANTE_LEGAL", "representante legal", "pai", "mãe", "mae", "guardião", "guardiao");
        register(raw, "CONSELHO_TUTELAR", "conselho tutelar");
        register(raw, "ORGAO_AMBIENTAL", "órgão ambiental", "orgao ambiental");
        register(raw, "AUTORIDADE_CENTRAL", "autoridade central");
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : raw.entrySet()) {
            out.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(out);
    }

    private static void register(Map<String, Set<String>> raw, String key, String... aliases) {
        raw.computeIfAbsent(normalizeToken(key), ignored -> new LinkedHashSet<>());
        raw.get(normalizeToken(key)).add(key);
        for (String alias : aliases) {
            if (alias != null && !alias.isBlank()) {
                raw.get(normalizeToken(key)).add(alias);
            }
        }
    }
}
