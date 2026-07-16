package com.tcc.pjb.backend.model.entity.enums.processual;

import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport;
import com.tcc.pjb.backend.core.util.EnumText;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public enum RitoProcessual {
COMUM_ORDINARIO,
SUMARIO,
SUMARIO_ESPECIAL,
JUIZADO_ESPECIAL,
JUIZADO_ESPECIAL_CIVEL,
JUIZADO_ESPECIAL_FAZENDA_PUBLICA,
JUIZADO_ESPECIAL_FEDERAL,
JUIZADO_ESPECIAL_CRIMINAL,
EXECUCAO_TITULO_EXTRAJUDICIAL,
EXECUCAO_TITULO_JUDICIAL,
EXECUCAO_FISCAL,
EXECUCAO_PENAL,
CUMPRIMENTO_SENTENCA,
CUMPRIMENTO_PROVISORIO,
CIVIL_TUTELA_URGENTE,
CIVIL_TUTELA_CAUTELAR_ANTECEDENTE,
CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE,
CIVIL_FAMILIA_ALIMENTOS,
CIVIL_FAMILIA_DIVORCIO,
CIVIL_INVENTARIO_ARROLAMENTO,
CIVIL_ACAO_CIVIL_PUBLICA,
CIVIL_ACAO_MONITORIA,
CIVIL_USUCAPIAO,
CIVIL_POSSESSORIA,
CIVIL_CONSIGNACAO_PAGAMENTO,
CIVIL_DISSOLUCAO_CASAMENTO,
CIVIL_INVESTIGACAO_PATERNIDADE,
CIVIL_RECONHECIMENTO_PATERNIDADE,
CIVIL_ADOCAO,
CIVIL_TUTELA_CURATELA,
CIVIL_INTERDITO_PROIBITORIO,
CIVIL_RETIFICACAO_REGISTRO,
CIVIL_NUNCIACAO_OBRA_NOVA,
ESPECIAL_MANDADO_SEGURANCA,
ESPECIAL_MANDADO_SEGURANCA_COLETIVO,
ESPECIAL_HABEAS_CORPUS,
ESPECIAL_HABEAS_DATA,
ESPECIAL_ACAO_POPULAR,
ESPECIAL_MANDADO_INJUNCAO,
ESPECIAL_MANDADO_INJUNCAO_COLETIVO,
ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE,
ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE,
ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL,
ESPECIAL_ACAO_DESCUMPRIMENTO_OBRIGACAO,
IMPROBIDADE_ADMINISTRATIVA,
ADMINISTRATIVO_PAD,
ADMINISTRATIVO_ACAO_POPULAR,
ADMINISTRATIVO_ACAO_CIVIL_PUBLICA_ADM,
ADMINISTRATIVO_CONCURSO_PUBLICO,
ADMINISTRATIVO_SERVIDORES,
PROCEDIMENTO_PENAL_COMUM,
PROCEDIMENTO_PENAL_SUMARIO,
PROCEDIMENTO_PENAL_SUMARISSIMO,
TRIBUNAL_JURI,
PENAL_LEI_DROGAS,
PENAL_MARIA_DA_PENHA,
PENAL_CRIMES_TRANSITO,
PENAL_ESTATUTO_IDOSO,
PENAL_ECA_INFRACIONAL,
PENAL_LAVAGEM_DINHEIRO,
PENAL_ORGANIZACAO_CRIMINOSA,
PENAL_VIOLENCIA_POLITICA,
PENAL_CRIMES_CIBERNETICOS,
PENAL_RACISMO,
PENAL_TORTURA,
PENAL_TERRORISMO,
PENAL_CRIMES_CONTRA_HONRA,
PENAL_HABEAS_CORPUS_PREVENTIVO,
PENAL_REVISAO_CRIMINAL,
PENAL_RECLAMACAO_CRIMINAL,
TRABALHISTA_ORDINARIO,
TRABALHISTA_SUMARISSIMO,
TRABALHISTA_SUMARIO_ALCADA,
TRABALHISTA_DISSIDIO_COLETIVO,
TRABALHISTA_INQUERITO_FALTA_GRAVE,
TRABALHISTA_ACAO_CUMPRIMENTO,
TRABALHISTA_ACAO_RESCISORIA,
TRABALHISTA_MANDADO_SEGURANCA,
TRABALHISTA_CUMPRIMENTO_SENTENCA,
TRABALHISTA_EXECUCAO,
TRABALHISTA_TUTELA_CAUTELAR,
TRABALHISTA_ACIDENTE_TRABALHO,
FAZENDA_PUBLICA_CONHECIMENTO,
FAZENDA_PUBLICA_EXECUCAO,
TRIBUTARIO_ANULATORIA_DEBITO,
TRIBUTARIO_REPETICAO_INDEBITO,
TRIBUTARIO_MANDADO_SEGURANCA,
TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL,
TRIBUTARIO_DECLARATORIA,
TRIBUTARIO_CAUTELAR_FISCAL,
PREVIDENCIARIO_JEF,
PREVIDENCIARIO_COMUM,
PREVIDENCIARIO_BPC_LOAS,
PREVIDENCIARIO_AUXILIO_INCAPACIDADE,
PREVIDENCIARIO_APOSENTADORIA,
PREVIDENCIARIO_REVISAO_BENEFICIO,
PREVIDENCIARIO_RESTABELECIMENTO,
PREVIDENCIARIO_ACIDENTARIO,
PREVIDENCIARIO_SALARIO_MATERNIDADE,
PREVIDENCIARIO_PENSAO_MORTE,
PREVIDENCIARIO_RURAL,
PREVIDENCIARIO_ESPECIAL,
PREVIDENCIARIO_RPPS,
MILITAR,
MILITAR_IPM,
MILITAR_PROCESSO_PENAL_MILITAR,
MILITAR_PAD,
MILITAR_CONSELHO_JUSTICA,
MILITAR_HABEAS_CORPUS_MILITAR,
ELEITORAL,
ELEITORAL_REGISTRO_CANDIDATURA,
ELEITORAL_AIRC,
ELEITORAL_AIJE,
ELEITORAL_AIME,
ELEITORAL_RCED,
ELEITORAL_PROPAGANDA,
ELEITORAL_DIREITO_RESPOSTA,
ELEITORAL_PRESTACAO_CONTAS,
ELEITORAL_INELEGIBILIDADE,
ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO,
AMBIENTAL_ACP,
AMBIENTAL_CRIMINAL,
AMBIENTAL_TUTELA_URGENTE,
ARBITRAGEM,
MEDIACAO,
CONCILIACAO_EXTRAJUDICIAL,
RECUPERACAO_JUDICIAL,
RECUPERACAO_EXTRAJUDICIAL,
FALENCIA,
INCIDENTE_DESCONSIDERACAO_PERSONALIDADE_JURIDICA,
HOMOLOGACAO_SENTENCA_ESTRANGEIRA,
CARTA_ROGATORIA,
COOPERACAO_JURIDICA_INTERNACIONAL,
INFANCIA_JUVENTUDE_ECA,
INFANCIA_JUVENTUDE_ADOCAO,
INFANCIA_JUVENTUDE_INFRACIONAL,
INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR,
AGRARIO_DESAPROPRIACAO,
AGRARIO_USUCAPIAO_RURAL,
AGRARIO_ACP_AGRARIA,
AGRARIO_POSSE_TERRA,
CIVIL_ACAO_EXIGIR_CONTAS,
CIVIL_DIVISAO_DEMARCACAO,
CIVIL_EMBARGOS_TERCEIRO,
CIVIL_OPOSICAO,
CIVIL_HABILITACAO,
CIVIL_FAMILIA_GUARDA,
JUIZADO_ESPECIAL_FEDERAL_CRIMINAL,
ELEITORAL_RECURSO_ELEITORAL;

private static final Map<String, String> ALIASES = buildAliases();

public static final RitoProcessual PROCEDIMENTO_COMUM = COMUM_ORDINARIO;
public static final RitoProcessual RECLAMACAO_TRABALHISTA = TRABALHISTA_ORDINARIO;

public static RitoProcessual fromString(String raw) {
    return tryParse(raw).orElse(COMUM_ORDINARIO);
}

public static Optional<RitoProcessual> tryParse(String raw) {
    Optional<RitoProcessual> exact = tryParseExact(raw);
    if (exact.isPresent()) {
        return exact;
    }
    return ProceduralCatalogSupport.tryResolveRito(raw, raw, raw);
}

public static Optional<RitoProcessual> tryParseExact(String raw) {
    if (raw == null || raw.isBlank()) return Optional.empty();
    String token = EnumText.normalizeToken(raw);
    if (token.isBlank()) return Optional.empty();
    String canonical = ALIASES.getOrDefault(token, token);
    try {
        return Optional.of(RitoProcessual.valueOf(canonical));
    } catch (Exception ignored) {
        return Optional.empty();
    }
}

public boolean isPenal() {
    String n = name();
    return n.startsWith("PENAL") || n.startsWith("PROCEDIMENTO_PENAL") || n.equals("TRIBUNAL_JURI") || n.equals("EXECUCAO_PENAL");
}

public boolean isTrabalhista() {
    return name().startsWith("TRABALHISTA");
}

public boolean isPrevidenciario() {
    return name().startsWith("PREVIDENCIARIO");
}

public boolean isTribFazenda() {
    String n = name();
    return n.startsWith("TRIBUTARIO") || n.equals("EXECUCAO_FISCAL") || n.equals("FAZENDA_PUBLICA_CONHECIMENTO") || n.equals("FAZENDA_PUBLICA_EXECUCAO");
}

public boolean isEleitoral() {
    return name().startsWith("ELEITORAL");
}

public boolean isMilitar() {
    return name().startsWith("MILITAR");
}

public boolean isEspecialConstitucional() {
    return name().startsWith("ESPECIAL");
}

public boolean isAdministrativo() {
    String n = name();
    return n.startsWith("IMPROBIDADE") || n.startsWith("ADMINISTRATIVO");
}

public boolean isAmbiental() {
    return name().startsWith("AMBIENTAL");
}

public boolean isInfancia() {
    return name().startsWith("INFANCIA");
}

public boolean isAgrario() {
    return name().startsWith("AGRARIO");
}

public boolean isEmpresarial() {
    return this == RECUPERACAO_JUDICIAL || this == RECUPERACAO_EXTRAJUDICIAL || this == FALENCIA || this == INCIDENTE_DESCONSIDERACAO_PERSONALIDADE_JURIDICA;
}

public boolean isInternacional() {
    return this == HOMOLOGACAO_SENTENCA_ESTRANGEIRA || this == CARTA_ROGATORIA || this == COOPERACAO_JURIDICA_INTERNACIONAL;
}

public boolean isAutocompositivo() {
    return this == ARBITRAGEM || this == MEDIACAO || this == CONCILIACAO_EXTRAJUDICIAL;
}

public boolean isJuizado() {
    String n = name();
    return n.startsWith("JUIZADO_") || n.contains("_JEF") || this == PREVIDENCIARIO_JEF;
}

public boolean isFamiliaSucessoes() {
    return this == CIVIL_FAMILIA_ALIMENTOS
            || this == CIVIL_FAMILIA_DIVORCIO
            || this == CIVIL_INVENTARIO_ARROLAMENTO
            || this == CIVIL_DISSOLUCAO_CASAMENTO
            || this == CIVIL_INVESTIGACAO_PATERNIDADE
            || this == CIVIL_RECONHECIMENTO_PATERNIDADE
            || this == CIVIL_ADOCAO
            || this == CIVIL_TUTELA_CURATELA
            || this == CIVIL_FAMILIA_GUARDA;
}

public boolean isExecucaoFiscalEstrita() {
    return this == EXECUCAO_FISCAL || this == TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL;
}

public boolean isDireitoRealImovel() {
    return this == CIVIL_USUCAPIAO
            || this == CIVIL_POSSESSORIA
            || this == CIVIL_INTERDITO_PROIBITORIO
            || this == CIVIL_DIVISAO_DEMARCACAO
            || this == CIVIL_NUNCIACAO_OBRA_NOVA
            || this == AGRARIO_USUCAPIAO_RURAL
            || this == AGRARIO_POSSE_TERRA;
}

public Optional<CriterioTerritorial> criterioTerritorial() {
    if (isTrabalhista()) {
        return Optional.of(CriterioTerritorial.LOCAL_PRESTACAO_SERVICO);
    }
    if (isPenal() || isMilitar()) {
        return Optional.of(CriterioTerritorial.LOCAL_DO_FATO);
    }
    if (isDireitoRealImovel()) {
        return Optional.of(CriterioTerritorial.SITUACAO_DA_COISA);
    }
    if (this == CIVIL_INVENTARIO_ARROLAMENTO) {
        return Optional.of(CriterioTerritorial.DOMICILIO_AUTOR_HERANCA);
    }
    if (this == CIVIL_FAMILIA_ALIMENTOS) {
        return Optional.of(CriterioTerritorial.DOMICILIO_ALIMENTANDO);
    }
    return Optional.empty();
}

public RitoGrupoPrincipal getGrupoPrincipal() {
    if (isExecucaoFiscalEstrita()) return RitoGrupoPrincipal.EXECUCAO_FISCAL;
    if (this == EXECUCAO_PENAL) return RitoGrupoPrincipal.EXECUCAO_PENAL;
    if (isJuizado()) return RitoGrupoPrincipal.JUIZADO;
    if (isFamiliaSucessoes()) return RitoGrupoPrincipal.FAMILIA;
    if (isPrevidenciario()) return RitoGrupoPrincipal.PREVIDENCIARIO;
    if (isTrabalhista()) return RitoGrupoPrincipal.TRABALHISTA;
    if (isEleitoral()) return RitoGrupoPrincipal.ELEITORAL;
    if (isMilitar()) return RitoGrupoPrincipal.MILITAR;
    if (isEmpresarial()) return RitoGrupoPrincipal.FALENCIA_RECUPERACAO;
    if (isPenal()) return RitoGrupoPrincipal.PENAL;
    return RitoGrupoPrincipal.CIVIL;
}

public boolean requiresSegredoByDefault() {
    return isPenal() || isMilitar() || isInfancia()
            || this == CIVIL_ADOCAO
            || this == CIVIL_INVESTIGACAO_PATERNIDADE
            || this == CIVIL_RECONHECIMENTO_PATERNIDADE
            || this == CIVIL_TUTELA_CURATELA
            || this == PENAL_MARIA_DA_PENHA;
}

public RamoDireito suggestedRamo() {
    if (isPenal()) return RamoDireito.PENAL;
    if (isTrabalhista()) return RamoDireito.TRABALHISTA;
    if (isPrevidenciario()) return RamoDireito.PREVIDENCIARIO;
    if (isTribFazenda()) return RamoDireito.TRIBUTARIO;
    if (isEleitoral()) return RamoDireito.ELEITORAL;
    if (isMilitar()) return RamoDireito.MILITAR;
    if (isAdministrativo()) return RamoDireito.ADMINISTRATIVO;
    if (isAmbiental()) return RamoDireito.AMBIENTAL;
    if (isInfancia()) return RamoDireito.INFANCIA_JUVENTUDE;
    if (isAgrario()) return RamoDireito.AGRARIO;
    if (isEmpresarial()) return RamoDireito.EMPRESARIAL;
    if (this == CIVIL_FAMILIA_ALIMENTOS
            || this == CIVIL_FAMILIA_DIVORCIO
            || this == CIVIL_INVENTARIO_ARROLAMENTO
            || this == CIVIL_DISSOLUCAO_CASAMENTO
            || this == CIVIL_INVESTIGACAO_PATERNIDADE
            || this == CIVIL_RECONHECIMENTO_PATERNIDADE
            || this == CIVIL_ADOCAO
            || this == CIVIL_TUTELA_CURATELA) {
        return RamoDireito.FAMILIA;
    }
    return RamoDireito.CIVIL;
}

public String group() {
    if (isPenal()) return "PENAL";
    if (isTrabalhista()) return "TRABALHISTA";
    if (isPrevidenciario()) return "PREVIDENCIARIO";
    if (isTribFazenda()) return "TRIBUTARIO_FAZENDA";
    if (isEleitoral()) return "ELEITORAL";
    if (isMilitar()) return "MILITAR";
    if (isEspecialConstitucional()) return "ESPECIAL_CONSTITUCIONAL";
    if (isAdministrativo()) return "ADMINISTRATIVO";
    if (isAmbiental()) return "AMBIENTAL";
    if (isInfancia()) return "INFANCIA_JUVENTUDE";
    if (isAgrario()) return "AGRARIO";
    if (isEmpresarial()) return "EMPRESARIAL";
    if (isInternacional()) return "INTERNACIONAL";
    if (isAutocompositivo()) return "METODOS_AUTOCOMPOSITIVOS";
    return "CIVIL";
}

public String suggestedProtocolSystem(String esfera) {
    String e = esfera == null ? "" : esfera.trim().toUpperCase(Locale.ROOT);
    if ("FEDERAL".equals(e)) {
        return switch (this) {
            case JUIZADO_ESPECIAL_FEDERAL, PREVIDENCIARIO_JEF, PREVIDENCIARIO_BPC_LOAS -> "e-Proc JEF / PJe JF";
            default -> "PJe JF ou e-Proc TRF";
        };
    }
    return switch (this) {
        case TRABALHISTA_ORDINARIO, TRABALHISTA_SUMARISSIMO, TRABALHISTA_SUMARIO_ALCADA,
                TRABALHISTA_DISSIDIO_COLETIVO, TRABALHISTA_INQUERITO_FALTA_GRAVE,
                TRABALHISTA_ACAO_CUMPRIMENTO, TRABALHISTA_ACAO_RESCISORIA,
                TRABALHISTA_MANDADO_SEGURANCA, TRABALHISTA_CUMPRIMENTO_SENTENCA,
                TRABALHISTA_EXECUCAO, TRABALHISTA_TUTELA_CAUTELAR,
                TRABALHISTA_ACIDENTE_TRABALHO -> "PJe TRT";
        case ELEITORAL, ELEITORAL_REGISTRO_CANDIDATURA, ELEITORAL_AIRC, ELEITORAL_AIJE, ELEITORAL_AIME,
                ELEITORAL_RCED, ELEITORAL_PROPAGANDA, ELEITORAL_DIREITO_RESPOSTA,
                ELEITORAL_PRESTACAO_CONTAS, ELEITORAL_INELEGIBILIDADE,
                ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO -> "PJe TSE / TRE";
        case MILITAR, MILITAR_IPM, MILITAR_PROCESSO_PENAL_MILITAR, MILITAR_PAD, MILITAR_CONSELHO_JUSTICA,
                MILITAR_HABEAS_CORPUS_MILITAR -> "Sistema Justiça Militar";
        case ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE, ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE,
                ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL, ESPECIAL_MANDADO_INJUNCAO,
                ESPECIAL_MANDADO_INJUNCAO_COLETIVO -> "e-STF";
        case HOMOLOGACAO_SENTENCA_ESTRANGEIRA, CARTA_ROGATORIA -> "e-STJ";
        default -> "PJe Estadual / eproc / ESAJ";
    };
}

public static List<RitoWithGroup> listRitosWithGroup() {
    return ProceduralCatalogSupport.allKnownRitos().stream()
            .map(r -> new RitoWithGroup(r.name(), r.group()))
            .toList();
}

private static Map<String, String> buildAliases() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("COMUM", "COMUM_ORDINARIO");
    map.put("ORDINARIO", "COMUM_ORDINARIO");
    map.put("PROCEDIMENTO_COMUM", "COMUM_ORDINARIO");
    map.put("CIVIL_COMUM", "COMUM_ORDINARIO");
    map.put("JUIZADO", "JUIZADO_ESPECIAL_CIVEL");
    map.put("JEC", "JUIZADO_ESPECIAL_CIVEL");
    map.put("JECIVEL", "JUIZADO_ESPECIAL_CIVEL");
    map.put("JEF", "JUIZADO_ESPECIAL_FEDERAL");
    map.put("JUIZADO_FEDERAL", "JUIZADO_ESPECIAL_FEDERAL");
    map.put("JECFAZ", "JUIZADO_ESPECIAL_FAZENDA_PUBLICA");
    map.put("JUIZADO_FAZENDA", "JUIZADO_ESPECIAL_FAZENDA_PUBLICA");
    map.put("FAZENDA", "JUIZADO_ESPECIAL_FAZENDA_PUBLICA");
    map.put("JECRIM", "JUIZADO_ESPECIAL_CRIMINAL");
    map.put("EXECUCAO", "EXECUCAO_TITULO_EXTRAJUDICIAL");
    map.put("EXECUCAO_TITULO", "EXECUCAO_TITULO_EXTRAJUDICIAL");
    map.put("EXECUCAO_EXT", "EXECUCAO_TITULO_EXTRAJUDICIAL");
    map.put("CUMPRIMENTO", "CUMPRIMENTO_SENTENCA");
    map.put("EXE_FISCAL", "EXECUCAO_FISCAL");
    map.put("FISCAL", "EXECUCAO_FISCAL");
    map.put("MS", "ESPECIAL_MANDADO_SEGURANCA");
    map.put("MANDADO_DE_SEGURANCA", "ESPECIAL_MANDADO_SEGURANCA");
    map.put("MANDADO_SEGURANCA", "ESPECIAL_MANDADO_SEGURANCA");
    map.put("MS_COL", "ESPECIAL_MANDADO_SEGURANCA_COLETIVO");
    map.put("MANDADO_SEGURANCA_COLETIVO", "ESPECIAL_MANDADO_SEGURANCA_COLETIVO");
    map.put("HC", "ESPECIAL_HABEAS_CORPUS");
    map.put("HABEAS_CORPUS", "ESPECIAL_HABEAS_CORPUS");
    map.put("HD", "ESPECIAL_HABEAS_DATA");
    map.put("HABEAS_DATA", "ESPECIAL_HABEAS_DATA");
    map.put("AP", "ESPECIAL_ACAO_POPULAR");
    map.put("ACAO_POPULAR", "ESPECIAL_ACAO_POPULAR");
    map.put("MI", "ESPECIAL_MANDADO_INJUNCAO");
    map.put("MANDADO_INJUNCAO", "ESPECIAL_MANDADO_INJUNCAO");
    map.put("ADI", "ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE");
    map.put("ACAO_DIRETA", "ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE");
    map.put("ADC", "ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE");
    map.put("ADPF", "ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL");
    map.put("ACP", "CIVIL_ACAO_CIVIL_PUBLICA");
    map.put("ACAO_CIVIL_PUBLICA", "CIVIL_ACAO_CIVIL_PUBLICA");
    map.put("JURI", "TRIBUNAL_JURI");
    map.put("TRIBUNAL_DO_JURI", "TRIBUNAL_JURI");
    map.put("LEP", "EXECUCAO_PENAL");
    map.put("PENAL_COMUM", "PROCEDIMENTO_PENAL_COMUM");
    map.put("PENAL_SUMARIO", "PROCEDIMENTO_PENAL_SUMARIO");
    map.put("PENAL_SUMARISSIMO", "PROCEDIMENTO_PENAL_SUMARISSIMO");
    map.put("TRAB_ORDINARIO", "TRABALHISTA_ORDINARIO");
    map.put("TRAB_SUMARISSIMO", "TRABALHISTA_SUMARISSIMO");
    map.put("TRAB_SUMARIO", "TRABALHISTA_SUMARIO_ALCADA");
    map.put("TRAB_ALCADA", "TRABALHISTA_SUMARIO_ALCADA");
    map.put("RITO_ALCADA_TRABALHISTA", "TRABALHISTA_SUMARIO_ALCADA");
    map.put("DISSIDIO", "TRABALHISTA_DISSIDIO_COLETIVO");
    map.put("INQUERITO_FALTA_GRAVE", "TRABALHISTA_INQUERITO_FALTA_GRAVE");
    map.put("INQUERITO_JUDICIAL_FALTA_GRAVE", "TRABALHISTA_INQUERITO_FALTA_GRAVE");
    map.put("ACAO_CUMPRIMENTO_TRABALHISTA", "TRABALHISTA_ACAO_CUMPRIMENTO");
    map.put("JEF_PREVIDENCIARIO", "PREVIDENCIARIO_JEF");
    map.put("PREV_JEF", "PREVIDENCIARIO_JEF");
    map.put("PREVIDENCIARIO_JEF", "PREVIDENCIARIO_JEF");
    map.put("RJ", "RECUPERACAO_JUDICIAL");
    map.put("RECUPERACAO_JUD", "RECUPERACAO_JUDICIAL");
    map.put("RE", "RECUPERACAO_EXTRAJUDICIAL");
    map.put("RECUPERACAO_EXT", "RECUPERACAO_EXTRAJUDICIAL");
    map.put("FAL", "FALENCIA");
    map.put("DECRETO_FALENCIA", "FALENCIA");
    map.put("IDPJ", "INCIDENTE_DESCONSIDERACAO_PERSONALIDADE_JURIDICA");
    map.put("DESCONSIDERACAO", "INCIDENTE_DESCONSIDERACAO_PERSONALIDADE_JURIDICA");
    map.put("PROCEDIMENTO_JURI", "TRIBUNAL_JURI");
    map.put("MANDADO_SEGURANCA_TRIBUTARIO", "TRIBUTARIO_MANDADO_SEGURANCA");
    map.put("MANDADO_SEGURANCA_TRABALHISTA", "TRABALHISTA_MANDADO_SEGURANCA");
    map.put("PRESTACAO_CONTAS", "ELEITORAL_PRESTACAO_CONTAS");
    map.put("AIJE", "ELEITORAL_AIJE");
    map.put("AIRC", "ELEITORAL_AIRC");
    map.put("AIME", "ELEITORAL_AIME");
    map.put("PAD", "ADMINISTRATIVO_PAD");
    map.put("PROCESSO_DISCIPLINAR_ADMINISTRATIVO", "ADMINISTRATIVO_PAD");
    map.put("CONCURSO_PUBLICO_ADMINISTRATIVO", "ADMINISTRATIVO_CONCURSO_PUBLICO");
    map.put("SERVIDOR_PUBLICO_ADMINISTRATIVO", "ADMINISTRATIVO_SERVIDORES");
    map.put("INFANCIA_ECA", "INFANCIA_JUVENTUDE_ECA");
    map.put("PROTECAO_ECA", "INFANCIA_JUVENTUDE_ECA");
    map.put("ADOCAO_ECA", "INFANCIA_JUVENTUDE_ADOCAO");
    map.put("ATO_INFRACIONAL_ECA", "INFANCIA_JUVENTUDE_INFRACIONAL");
    map.put("GUARDA_MENOR_ECA", "INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR");
    map.put("RCED", "ELEITORAL_RCED");
    map.put("RECURSO_EXTRAORDINARIO", "ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE");
    return Map.copyOf(map);
}
}
