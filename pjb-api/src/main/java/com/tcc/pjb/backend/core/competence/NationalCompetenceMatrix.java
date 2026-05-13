package com.tcc.pjb.backend.core.competence;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public enum NationalCompetenceMatrix {
    TJAC("AC", RamoJusticaNacional.ESTADUAL, "TJAC", "Tribunal de Justiça do Acre", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJAL("AL", RamoJusticaNacional.ESTADUAL, "TJAL", "Tribunal de Justiça de Alagoas", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJAP("AP", RamoJusticaNacional.ESTADUAL, "TJAP", "Tribunal de Justiça do Amapá", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJAM("AM", RamoJusticaNacional.ESTADUAL, "TJAM", "Tribunal de Justiça do Amazonas", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJBA("BA", RamoJusticaNacional.ESTADUAL, "TJBA", "Tribunal de Justiça da Bahia", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJCE("CE", RamoJusticaNacional.ESTADUAL, "TJCE", "Tribunal de Justiça do Ceará", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJDFT("DF", RamoJusticaNacional.ESTADUAL, "TJDFT", "Tribunal de Justiça do Distrito Federal e dos Territórios", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJES("ES", RamoJusticaNacional.ESTADUAL, "TJES", "Tribunal de Justiça do Espírito Santo", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJGO("GO", RamoJusticaNacional.ESTADUAL, "TJGO", "Tribunal de Justiça de Goiás", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJMA("MA", RamoJusticaNacional.ESTADUAL, "TJMA", "Tribunal de Justiça do Maranhão", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJMT("MT", RamoJusticaNacional.ESTADUAL, "TJMT", "Tribunal de Justiça de Mato Grosso", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJMS("MS", RamoJusticaNacional.ESTADUAL, "TJMS", "Tribunal de Justiça de Mato Grosso do Sul", JudicialSystem.ESAJ, JudicialSystem.PDPJ),
    TJMG("MG", RamoJusticaNacional.ESTADUAL, "TJMG", "Tribunal de Justiça de Minas Gerais", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJPA("PA", RamoJusticaNacional.ESTADUAL, "TJPA", "Tribunal de Justiça do Pará", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJPB("PB", RamoJusticaNacional.ESTADUAL, "TJPB", "Tribunal de Justiça da Paraíba", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJPR("PR", RamoJusticaNacional.ESTADUAL, "TJPR", "Tribunal de Justiça do Paraná", JudicialSystem.PROJUDI, JudicialSystem.PDPJ),
    TJPE("PE", RamoJusticaNacional.ESTADUAL, "TJPE", "Tribunal de Justiça de Pernambuco", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJPI("PI", RamoJusticaNacional.ESTADUAL, "TJPI", "Tribunal de Justiça do Piauí", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJRJ("RJ", RamoJusticaNacional.ESTADUAL, "TJRJ", "Tribunal de Justiça do Rio de Janeiro", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJRN("RN", RamoJusticaNacional.ESTADUAL, "TJRN", "Tribunal de Justiça do Rio Grande do Norte", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJRS("RS", RamoJusticaNacional.ESTADUAL, "TJRS", "Tribunal de Justiça do Rio Grande do Sul", JudicialSystem.EPROC, JudicialSystem.PDPJ),
    TJRO("RO", RamoJusticaNacional.ESTADUAL, "TJRO", "Tribunal de Justiça de Rondônia", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJRR("RR", RamoJusticaNacional.ESTADUAL, "TJRR", "Tribunal de Justiça de Roraima", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJSC("SC", RamoJusticaNacional.ESTADUAL, "TJSC", "Tribunal de Justiça de Santa Catarina", JudicialSystem.EPROC, JudicialSystem.PDPJ),
    TJSP("SP", RamoJusticaNacional.ESTADUAL, "TJSP", "Tribunal de Justiça de São Paulo", JudicialSystem.ESAJ, JudicialSystem.PDPJ),
    TJSE("SE", RamoJusticaNacional.ESTADUAL, "TJSE", "Tribunal de Justiça de Sergipe", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJTO("TO", RamoJusticaNacional.ESTADUAL, "TJTO", "Tribunal de Justiça do Tocantins", JudicialSystem.PJE, JudicialSystem.PDPJ),

    TRF1("BR", RamoJusticaNacional.FEDERAL, "TRF1", "Tribunal Regional Federal da 1ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRF2("BR", RamoJusticaNacional.FEDERAL, "TRF2", "Tribunal Regional Federal da 2ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRF3("BR", RamoJusticaNacional.FEDERAL, "TRF3", "Tribunal Regional Federal da 3ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRF4("BR", RamoJusticaNacional.FEDERAL, "TRF4", "Tribunal Regional Federal da 4ª Região", JudicialSystem.EPROC, JudicialSystem.PDPJ),
    TRF5("BR", RamoJusticaNacional.FEDERAL, "TRF5", "Tribunal Regional Federal da 5ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRF6("BR", RamoJusticaNacional.FEDERAL, "TRF6", "Tribunal Regional Federal da 6ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),

    TRE_AC("AC", RamoJusticaNacional.ELEITORAL, "TRE-AC", "Tribunal Regional Eleitoral do Acre", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_AL("AL", RamoJusticaNacional.ELEITORAL, "TRE-AL", "Tribunal Regional Eleitoral de Alagoas", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_AP("AP", RamoJusticaNacional.ELEITORAL, "TRE-AP", "Tribunal Regional Eleitoral do Amapá", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_AM("AM", RamoJusticaNacional.ELEITORAL, "TRE-AM", "Tribunal Regional Eleitoral do Amazonas", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_BA("BA", RamoJusticaNacional.ELEITORAL, "TRE-BA", "Tribunal Regional Eleitoral da Bahia", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_CE("CE", RamoJusticaNacional.ELEITORAL, "TRE-CE", "Tribunal Regional Eleitoral do Ceará", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_DF("DF", RamoJusticaNacional.ELEITORAL, "TRE-DF", "Tribunal Regional Eleitoral do Distrito Federal", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_ES("ES", RamoJusticaNacional.ELEITORAL, "TRE-ES", "Tribunal Regional Eleitoral do Espírito Santo", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_GO("GO", RamoJusticaNacional.ELEITORAL, "TRE-GO", "Tribunal Regional Eleitoral de Goiás", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_MA("MA", RamoJusticaNacional.ELEITORAL, "TRE-MA", "Tribunal Regional Eleitoral do Maranhão", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_MT("MT", RamoJusticaNacional.ELEITORAL, "TRE-MT", "Tribunal Regional Eleitoral de Mato Grosso", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_MS("MS", RamoJusticaNacional.ELEITORAL, "TRE-MS", "Tribunal Regional Eleitoral de Mato Grosso do Sul", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_MG("MG", RamoJusticaNacional.ELEITORAL, "TRE-MG", "Tribunal Regional Eleitoral de Minas Gerais", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_PA("PA", RamoJusticaNacional.ELEITORAL, "TRE-PA", "Tribunal Regional Eleitoral do Pará", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_PB("PB", RamoJusticaNacional.ELEITORAL, "TRE-PB", "Tribunal Regional Eleitoral da Paraíba", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_PR("PR", RamoJusticaNacional.ELEITORAL, "TRE-PR", "Tribunal Regional Eleitoral do Paraná", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_PE("PE", RamoJusticaNacional.ELEITORAL, "TRE-PE", "Tribunal Regional Eleitoral de Pernambuco", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_PI("PI", RamoJusticaNacional.ELEITORAL, "TRE-PI", "Tribunal Regional Eleitoral do Piauí", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_RJ("RJ", RamoJusticaNacional.ELEITORAL, "TRE-RJ", "Tribunal Regional Eleitoral do Rio de Janeiro", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_RN("RN", RamoJusticaNacional.ELEITORAL, "TRE-RN", "Tribunal Regional Eleitoral do Rio Grande do Norte", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_RS("RS", RamoJusticaNacional.ELEITORAL, "TRE-RS", "Tribunal Regional Eleitoral do Rio Grande do Sul", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_RO("RO", RamoJusticaNacional.ELEITORAL, "TRE-RO", "Tribunal Regional Eleitoral de Rondônia", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_RR("RR", RamoJusticaNacional.ELEITORAL, "TRE-RR", "Tribunal Regional Eleitoral de Roraima", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_SC("SC", RamoJusticaNacional.ELEITORAL, "TRE-SC", "Tribunal Regional Eleitoral de Santa Catarina", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_SP("SP", RamoJusticaNacional.ELEITORAL, "TRE-SP", "Tribunal Regional Eleitoral de São Paulo", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_SE("SE", RamoJusticaNacional.ELEITORAL, "TRE-SE", "Tribunal Regional Eleitoral de Sergipe", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRE_TO("TO", RamoJusticaNacional.ELEITORAL, "TRE-TO", "Tribunal Regional Eleitoral do Tocantins", JudicialSystem.PJE, JudicialSystem.PDPJ),

    TRT1("RJ", RamoJusticaNacional.TRABALHO, "TRT1", "Tribunal Regional do Trabalho da 1ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT2("SP", RamoJusticaNacional.TRABALHO, "TRT2", "Tribunal Regional do Trabalho da 2ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT3("MG", RamoJusticaNacional.TRABALHO, "TRT3", "Tribunal Regional do Trabalho da 3ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT4("RS", RamoJusticaNacional.TRABALHO, "TRT4", "Tribunal Regional do Trabalho da 4ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT5("BA", RamoJusticaNacional.TRABALHO, "TRT5", "Tribunal Regional do Trabalho da 5ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT6("PE", RamoJusticaNacional.TRABALHO, "TRT6", "Tribunal Regional do Trabalho da 6ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT7("CE", RamoJusticaNacional.TRABALHO, "TRT7", "Tribunal Regional do Trabalho da 7ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT8("PA", RamoJusticaNacional.TRABALHO, "TRT8", "Tribunal Regional do Trabalho da 8ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT9("PR", RamoJusticaNacional.TRABALHO, "TRT9", "Tribunal Regional do Trabalho da 9ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT10("DF", RamoJusticaNacional.TRABALHO, "TRT10", "Tribunal Regional do Trabalho da 10ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT11("AM", RamoJusticaNacional.TRABALHO, "TRT11", "Tribunal Regional do Trabalho da 11ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT12("SC", RamoJusticaNacional.TRABALHO, "TRT12", "Tribunal Regional do Trabalho da 12ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT13("PB", RamoJusticaNacional.TRABALHO, "TRT13", "Tribunal Regional do Trabalho da 13ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT14("RO", RamoJusticaNacional.TRABALHO, "TRT14", "Tribunal Regional do Trabalho da 14ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT15("SP", RamoJusticaNacional.TRABALHO, "TRT15", "Tribunal Regional do Trabalho da 15ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT16("MA", RamoJusticaNacional.TRABALHO, "TRT16", "Tribunal Regional do Trabalho da 16ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT17("ES", RamoJusticaNacional.TRABALHO, "TRT17", "Tribunal Regional do Trabalho da 17ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT18("GO", RamoJusticaNacional.TRABALHO, "TRT18", "Tribunal Regional do Trabalho da 18ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT19("AL", RamoJusticaNacional.TRABALHO, "TRT19", "Tribunal Regional do Trabalho da 19ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT20("SE", RamoJusticaNacional.TRABALHO, "TRT20", "Tribunal Regional do Trabalho da 20ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT21("RN", RamoJusticaNacional.TRABALHO, "TRT21", "Tribunal Regional do Trabalho da 21ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT22("PI", RamoJusticaNacional.TRABALHO, "TRT22", "Tribunal Regional do Trabalho da 22ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT23("MT", RamoJusticaNacional.TRABALHO, "TRT23", "Tribunal Regional do Trabalho da 23ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TRT24("MS", RamoJusticaNacional.TRABALHO, "TRT24", "Tribunal Regional do Trabalho da 24ª Região", JudicialSystem.PJE, JudicialSystem.PDPJ),

    TJM_MG("MG", RamoJusticaNacional.MILITAR_ESTADUAL, "TJM-MG", "Tribunal de Justiça Militar de Minas Gerais", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TJM_RS("RS", RamoJusticaNacional.MILITAR_ESTADUAL, "TJM-RS", "Tribunal de Justiça Militar do Rio Grande do Sul", JudicialSystem.EPROC, JudicialSystem.PDPJ),
    TJM_SP("SP", RamoJusticaNacional.MILITAR_ESTADUAL, "TJM-SP", "Tribunal de Justiça Militar do Estado de São Paulo", JudicialSystem.ESAJ, JudicialSystem.PDPJ),
    TJM_CE("CE", RamoJusticaNacional.MILITAR_ESTADUAL, "TJM-CE", "Justiça Militar Estadual do Ceará", JudicialSystem.PJE, JudicialSystem.PDPJ),

    STJ("BR", RamoJusticaNacional.SUPERIOR, "STJ", "Superior Tribunal de Justiça", JudicialSystem.PJE, JudicialSystem.PDPJ),
    STF("BR", RamoJusticaNacional.SUPERIOR_STF, "STF", "Supremo Tribunal Federal", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TSE("BR", RamoJusticaNacional.ELEITORAL_SUPERIOR, "TSE", "Tribunal Superior Eleitoral", JudicialSystem.PJE, JudicialSystem.PDPJ),
    TST("BR", RamoJusticaNacional.TRABALHO_SUPERIOR, "TST", "Tribunal Superior do Trabalho", JudicialSystem.PJE, JudicialSystem.PDPJ),
    STM("BR", RamoJusticaNacional.MILITAR_SUPERIOR, "STM", "Superior Tribunal Militar", JudicialSystem.PJE, JudicialSystem.PDPJ);

    private static final Map<String, NationalCompetenceMatrix> INDEX_UF_RAMO = buildUfRamoIndex();
    private static final Map<String, NationalCompetenceMatrix> INDEX_CODIGO = buildCodigoIndex();

    private final String uf;
    private final RamoJusticaNacional ramoJusticaNacional;
    private final String codigo;
    private final String nome;
    private final JudicialSystem sistemaJudicialPrimario;
    private final JudicialSystem sistemaJudicialFallback;

    NationalCompetenceMatrix(String uf,
                             RamoJusticaNacional ramoJusticaNacional,
                             String codigo,
                             String nome,
                             JudicialSystem sistemaJudicialPrimario,
                             JudicialSystem sistemaJudicialFallback) {
        this.uf = normalizeUf(uf);
        this.ramoJusticaNacional = Objects.requireNonNull(ramoJusticaNacional, "ramoJusticaNacional");
        this.codigo = Objects.requireNonNull(codigo, "codigo").trim();
        this.nome = Objects.requireNonNull(nome, "nome").trim();
        this.sistemaJudicialPrimario = Objects.requireNonNull(sistemaJudicialPrimario, "sistemaJudicialPrimario");
        this.sistemaJudicialFallback = Objects.requireNonNull(sistemaJudicialFallback, "sistemaJudicialFallback");
    }

    public String uf() {
        return uf;
    }

    public RamoJusticaNacional ramo() {
        return ramoJusticaNacional;
    }

    public RamoJusticaNacional ramoJusticaNacional() {
        return ramoJusticaNacional;
    }

    public String codigo() {
        return codigo;
    }

    public String nome() {
        return nome;
    }

    public JudicialSystem connectorPreferido() {
        return sistemaJudicialPrimario;
    }

    public JudicialSystem sistemaJudicialPrimario() {
        return sistemaJudicialPrimario;
    }

    public JudicialSystem sistemaJudicialFallback() {
        return sistemaJudicialFallback;
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("uf", uf);
        out.put("ramoJusticaNacional", ramoJusticaNacional.name());
        out.put("codigo", codigo);
        out.put("nome", nome);
        out.put("connectorPreferido", sistemaJudicialPrimario.name());
        out.put("sistemaJudicialPrimario", sistemaJudicialPrimario.name());
        out.put("sistemaJudicialFallback", sistemaJudicialFallback.name());
        return out;
    }

    public static Optional<NationalCompetenceMatrix> resolver(String uf, RamoJusticaNacional ramo) {
        if (ramo == null) {
            return Optional.empty();
        }
        String normalizedUf = normalizeUf(uf);
        NationalCompetenceMatrix direct = INDEX_UF_RAMO.get(key(normalizedUf, ramo));
        if (direct != null) {
            return Optional.of(direct);
        }
        NationalCompetenceMatrix national = INDEX_UF_RAMO.get(key("BR", ramo));
        if (national != null) {
            return Optional.of(national);
        }
        return Optional.ofNullable(defaultFor(normalizedUf, ramo));
    }


    public static Optional<NationalCompetenceMatrix> porUF(String uf) {
        String normalizedUf = normalizeUf(uf);
        for (NationalCompetenceMatrix item : values()) {
            if (normalizedUf.equals(item.uf)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public boolean isMilitar() {
        return ramoJusticaNacional == RamoJusticaNacional.MILITAR_ESTADUAL
                || ramoJusticaNacional == RamoJusticaNacional.MILITAR_SUPERIOR;
    }

    public boolean supportsProtocolo() {
        return sistemaJudicialPrimario != null || sistemaJudicialFallback != null;
    }

    public boolean isEleitoral() {
        return ramoJusticaNacional == RamoJusticaNacional.ELEITORAL
                || ramoJusticaNacional == RamoJusticaNacional.ELEITORAL_SUPERIOR;
    }

    public boolean isTrabalhista() {
        return ramoJusticaNacional == RamoJusticaNacional.TRABALHO
                || ramoJusticaNacional == RamoJusticaNacional.TRABALHO_SUPERIOR;
    }

    public boolean isTrabalho() {
        return isTrabalhista();
    }

    public static Optional<NationalCompetenceMatrix> porCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(INDEX_CODIGO.get(normalizeCodigo(codigo)));
    }

    private static NationalCompetenceMatrix defaultFor(String uf, RamoJusticaNacional ramo) {
        return switch (ramo) {
            case ESTADUAL -> switch (uf) {
                case "SP" -> TJSP;
                case "RJ" -> TJRJ;
                case "MG" -> TJMG;
                case "RS" -> TJRS;
                case "SC" -> TJSC;
                case "PR" -> TJPR;
                case "BA" -> TJBA;
                case "PE" -> TJPE;
                case "DF" -> TJDFT;
                default -> TJCE;
            };
            case FEDERAL -> switch (uf) {
                case "ES", "RJ" -> TRF2;
                case "MS", "SP" -> TRF3;
                case "PR", "RS", "SC" -> TRF4;
                case "AL", "CE", "PB", "PE", "RN", "SE" -> TRF5;
                case "MG" -> TRF6;
                default -> TRF1;
            };
            case ELEITORAL -> switch (uf) {
                case "SP" -> TRE_SP;
                case "RJ" -> TRE_RJ;
                case "MG" -> TRE_MG;
                case "RS" -> TRE_RS;
                case "SC" -> TRE_SC;
                case "PR" -> TRE_PR;
                case "DF" -> TRE_DF;
                default -> TRE_CE;
            };
            case TRABALHO -> switch (uf) {
                case "RJ" -> TRT1;
                case "SP" -> TRT2;
                case "MG" -> TRT3;
                case "RS" -> TRT4;
                case "BA" -> TRT5;
                case "PE" -> TRT6;
                case "SC" -> TRT12;
                case "PR" -> TRT9;
                case "DF", "TO" -> TRT10;
                default -> TRT7;
            };
            case MILITAR_ESTADUAL -> switch (uf) {
                case "MG" -> TJM_MG;
                case "RS" -> TJM_RS;
                case "SP" -> TJM_SP;
                default -> TJM_CE;
            };
            case SUPERIOR -> STJ;
            case SUPERIOR_STF -> STF;
            case ELEITORAL_SUPERIOR -> TSE;
            case TRABALHO_SUPERIOR -> TST;
            case MILITAR_SUPERIOR -> STM;
        };
    }

    private static Map<String, NationalCompetenceMatrix> buildUfRamoIndex() {
        LinkedHashMap<String, NationalCompetenceMatrix> out = new LinkedHashMap<>();
        for (NationalCompetenceMatrix item : values()) {
            out.putIfAbsent(key(item.uf, item.ramoJusticaNacional), item);
        }
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, NationalCompetenceMatrix> buildCodigoIndex() {
        LinkedHashMap<String, NationalCompetenceMatrix> out = new LinkedHashMap<>();
        for (NationalCompetenceMatrix item : values()) {
            out.putIfAbsent(normalizeCodigo(item.codigo), item);
        }
        return Collections.unmodifiableMap(out);
    }

    private static String key(String uf, RamoJusticaNacional ramo) {
        return normalizeUf(uf) + '#' + ramo.name();
    }

    private static String normalizeUf(String uf) {
        if (uf == null || uf.isBlank()) {
            return "BR";
        }
        return uf.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeCodigo(String codigo) {
        return codigo == null ? "" : codigo.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    public enum RamoJusticaNacional {
        ESTADUAL,
        FEDERAL,
        ELEITORAL,
        TRABALHO,
        MILITAR_ESTADUAL,
        SUPERIOR,
        SUPERIOR_STF,
        ELEITORAL_SUPERIOR,
        TRABALHO_SUPERIOR,
        MILITAR_SUPERIOR
    }
}
