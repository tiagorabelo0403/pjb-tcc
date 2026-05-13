package com.tcc.pjb.backend.model.entity;

import java.util.Collections;
import java.text.Normalizer;
import java.util.*;
import java.util.Locale;

public final class JurisdictionEngine {

    private JurisdictionEngine() {}

    

    public enum Rite {
        COMUM,
        PENAL,
        EXECUCAO_PENAL,
        MILITAR,
        TRIBUTARIO,
        PREVIDENCIARIO,
        TRABALHISTA,
        ELEITORAL,
        DESPORTIVO,
        AGRARIO,
        FAMILIA,
        CONSUMIDOR,
        EMPRESARIAL,
        CONSTITUCIONAL
    }

    
    public interface Engine {
        Result identifyByContext(Context ctx);

        
        static Engine buildDefault() {
            return new DefaultEngine();
        }
    }

    public static final class Context {
        private final String materia;
        private final String orgao;
        private final String pais;
        private final String tratado;
        private final Rite rito;

        public Context(String materia, String orgao, String pais, String tratado, Rite rito) {
            this.materia = materia;
            this.orgao = orgao;
            this.pais = pais;
            this.tratado = tratado;
            this.rito = rito;
        }

        public String getMateria() { return materia; }
        public String getOrgao() { return orgao; }
        public String getPais() { return pais; }
        public String getTratado() { return tratado; }
        public Rite getRito() { return rito; }
    }

    public static final class JurisdictionSpec {
        public final String label;
        public final String category;
        public final Rite rite;
        public final List<Authority> authorities;
        public final List<LegalBase> legalBases;
        public final List<Country> countries;
        public final List<String> treaties;

        private JurisdictionSpec(Builder builder) {
            this.label = builder.label;
            this.category = builder.category;
            this.rite = builder.rite;
            this.authorities = builder.authorities == null ? List.of() : List.copyOf(builder.authorities);
            this.legalBases = builder.legalBases == null ? List.of() : List.copyOf(builder.legalBases);
            this.countries = builder.countries == null ? List.of() : List.copyOf(builder.countries);
            this.treaties = builder.treaties == null ? List.of() : List.copyOf(builder.treaties);
        }

        public static Builder builder() { return new Builder(); }
        public String getLabel() { return label; }
        public String getCategory() { return category; }
        public Rite getRite() { return rite; }
        public List<Authority> getAuthorities() { return authorities; }
        public List<LegalBase> getLegalBases() { return legalBases; }
        public List<Country> getCountries() { return countries; }
        public List<String> getTreaties() { return treaties; }

        public static final class Builder {
            private String label;
            private String category;
            private Rite rite;
            private List<Authority> authorities;
            private List<LegalBase> legalBases;
            private List<Country> countries;
            private List<String> treaties;

            public Builder label(String label) { this.label = label; return this; }
            public Builder category(String category) { this.category = category; return this; }
            public Builder rite(Rite rite) { this.rite = rite; return this; }
            public Builder authorities(List<Authority> authorities) { this.authorities = authorities; return this; }
            public Builder legalBases(List<LegalBase> legalBases) { this.legalBases = legalBases; return this; }
            public Builder countries(List<Country> countries) { this.countries = countries; return this; }
            public Builder treaties(List<String> treaties) { this.treaties = treaties; return this; }
            public JurisdictionSpec build() { return new JurisdictionSpec(this); }
        }

        public static final class Authority {
            private final String name;
            private final String level;

            private Authority(Builder builder) {
                this.name = builder.name;
                this.level = builder.level;
            }

            public static Builder builder() { return new Builder(); }
            public String getName() { return name; }
            public String getLevel() { return level; }

            public static final class Builder {
                private String name;
                private String level;
                public Builder name(String name) { this.name = name; return this; }
                public Builder level(String level) { this.level = level; return this; }
                public Authority build() { return new Authority(this); }
            }
        }

        public static final class LegalBase {
            private final String citation;
            private final String description;

            private LegalBase(Builder builder) {
                this.citation = builder.citation;
                this.description = builder.description;
            }

            public static Builder builder() { return new Builder(); }
            public String getCitation() { return citation; }
            public String getDescription() { return description; }

            public static final class Builder {
                private String citation;
                private String description;
                public Builder citation(String citation) { this.citation = citation; return this; }
                public Builder description(String description) { this.description = description; return this; }
                public LegalBase build() { return new LegalBase(this); }
            }
        }

        public static final class Country {
            private final String name;
            private final String iso;

            private Country(Builder builder) {
                this.name = builder.name;
                this.iso = builder.iso;
            }

            public static Builder builder() { return new Builder(); }
            public String getName() { return name; }
            public String getIso() { return iso; }

            public static final class Builder {
                private String name;
                private String iso;
                public Builder name(String name) { this.name = name; return this; }
                public Builder iso(String iso) { this.iso = iso; return this; }
                public Country build() { return new Country(this); }
            }
        }
    }

    public static final class Result {
        private final boolean found;
        private final double confidence;
        private final String reason;
        private final JurisdictionSpec spec;
        private final Map<String, Object> debug;

        public boolean isFound() { return found; }
        public double getConfidence() { return confidence; }
        public String getReason() { return reason; }
        public JurisdictionSpec getSpec() { return spec; }
        public Map<String, Object> getDebug() { return debug; }

        private Result(boolean found, double confidence, String reason, JurisdictionSpec spec, Map<String, Object> debug) {
            this.found = found;
            this.confidence = confidence;
            this.reason = reason;
            this.spec = spec;
            this.debug = debug == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(debug));
        }

        public static Result notFound(String reason, Map<String, Object> debug) {
            return new Result(false, 0.0, reason, null, debug);
        }

        public static Result found(JurisdictionSpec spec, double confidence, String reason, Map<String, Object> debug) {
            return new Result(true, clamp01(confidence), reason, spec, debug);
        }

        public String toJson() {
            
            return "{\"found\":" + found +
                    ",\"confidence\":" + confidence +
                    ",\"reason\":\"" + safe(reason) + "\"" +
                    ",\"spec\":\"" + (spec == null ? "" : safe(spec.label)) + "\"" +
                    "}";
        }

        private static double clamp01(double v) {
            return Math.max(0.0, Math.min(v, 1.0));
        }

        private String safe(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    

    
    public static Engine defaultEngine() {
        return Engine.buildDefault();
    }

    
    static final class DefaultEngine implements Engine {

        @Override
        public Result identifyByContext(Context ctx) {
            Map<String, Object> dbg = new LinkedHashMap<>();
            dbg.put("materia", ctx == null ? null : ctx.getMateria());
            dbg.put("orgao", ctx == null ? null : ctx.getOrgao());
            dbg.put("pais", ctx == null ? null : ctx.getPais());
            dbg.put("tratado", ctx == null ? null : ctx.getTratado());
            dbg.put("rito", ctx == null ? null : ctx.getRito());

            if (ctx == null) {
                return Result.notFound("context is null", dbg);
            }

            String materia = norm(ctx.getMateria());
            String orgao = norm(ctx.getOrgao());
            String pais = norm(ctx.getPais());
            String tratado = norm(ctx.getTratado());

            
            
            
            if (ctx.getRito() == Rite.PENAL ||
                    containsAny(materia,
                            "PENAL", "CRIME", "INQUERITO", "INQUERITO POLICIAL", "DENUNCIA",
                            "FLAGRANTE", "AUDIENCIA DE CUSTODIA", "CUSTODIA",
                            "JURI", "TRIBUNAL DO JURI", "PRONUNCIA", "HOMICIDIO", "CPP")) {

                dbg.put("rule", "penal");
                return Result.found(specPenal(pais), 0.86, "match:materia/rito penal", dbg);
            }

            
            if (ctx.getRito() == Rite.EXECUCAO_PENAL ||
                    containsAny(materia,
                            "EXECUCAO PENAL", "LEP", "PROGRESSAO", "REGRESSAO", "REMICAO",
                            "LIVRAMENTO CONDICIONAL", "SAIDINHA", "INDULTO", "COMUTACAO")) {
                dbg.put("rule", "execucao_penal");
                return Result.found(specExecucaoPenal(pais), 0.85, "match:execucao penal", dbg);
            }

            if (ctx.getRito() == Rite.MILITAR || containsAny(materia, "MILITAR", "IPM", "CPM", "CPPM")) {
                dbg.put("rule", "militar");
                return Result.found(specMilitar(pais), 0.86, "match:materia/rito militar", dbg);
            }

            
            
            
            if (ctx.getRito() == Rite.TRIBUTARIO ||
                    containsAny(materia, "TRIBUT", "TRIBUTARIO", "EXECUCAO FISCAL", "CDA", "ICMS", "ISS", "IPTU", "ITR", "CTN", "LEI 6830")) {

                dbg.put("rule", "tributario");
                return Result.found(specTributario(pais), 0.83, "match:tributario", dbg);
            }

            
            
            
            if (ctx.getRito() == Rite.AGRARIO ||
                    containsAny(materia, "AGRAR", "AGRARIO", "POSSE", "TERRA", "FUNDIAR", "REINTEGRACAO", "MANUTENCAO DE POSSE")) {

                dbg.put("rule", "agrario");
                return Result.found(specAgrario(pais), 0.78, "match:agrario", dbg);
            }

            
            
            
            if (ctx.getRito() == Rite.DESPORTIVO ||
                    containsAny(materia, "DESPORT", "DESPORTIVO", "STJD", "CBJD", "CAS")) {

                dbg.put("rule", "desportivo");
                return Result.found(specDesportivo(pais), 0.74, "match:desportivo", dbg);
            }

            
            
            
            if (ctx.getRito() == Rite.FAMILIA ||
                    containsAny(materia, "FAMIL", "FAMILIA", "ALIMENTOS", "GUARDA", "DIVORC", "UNIAO ESTAVEL", "INTERDICAO", "TUTELA", "CURATELA")) {

                dbg.put("rule", "familia");
                return Result.found(specFamilia(pais), 0.78, "match:familia", dbg);
            }

            
            
            
            if (ctx.getRito() == Rite.TRABALHISTA ||
                    containsAny(materia, "TRABALH", "CLT", "RECLAMATORIA", "RESCISAO INDIRETA", "VERBAS RESCISORIAS", "FGTS", "HORAS EXTRAS") ||
                    containsAny(orgao, "VARA DO TRABALHO", "TRT")) {
                dbg.put("rule", "trabalhista");
                return Result.found(specTrabalhista(pais), 0.79, "match:trabalhista", dbg);
            }

            
            
            
            if (ctx.getRito() == Rite.PREVIDENCIARIO ||
                    containsAny(materia, "PREVID", "INSS", "BPC", "LOAS", "AUXILIO", "APOSENTADORIA", "BENEFICIO") ||
                    containsAny(orgao, "JEF", "JUIZADO ESPECIAL FEDERAL")) {
                dbg.put("rule", "previdenciario");
                return Result.found(specPrevidenciario(pais), 0.77, "match:previdenciario", dbg);
            }

            
            
            
            if (ctx.getRito() == Rite.ELEITORAL ||
                    containsAny(materia, "ELEITOR", "AIJE", "AIME", "AIRC", "RCED", "PROPAGANDA", "PRESTACAO DE CONTAS") ||
                    containsAny(orgao, "TRE", "TSE", "ZONA ELEITORAL")) {
                dbg.put("rule", "eleitoral");
                return Result.found(specEleitoral(pais), 0.78, "match:eleitoral", dbg);
            }

            
            
            
            if (ctx.getRito() == Rite.CONSUMIDOR ||
                    containsAny(materia, "CONSUMID", "CDC", "PROCON", "VICIO", "DEFEITO", "NEGATIVACAO") ) {
                dbg.put("rule", "consumidor");
                return Result.found(specConsumidor(pais), 0.72, "match:consumidor", dbg);
            }

            
            
            
            if (ctx.getRito() == Rite.EMPRESARIAL ||
                    containsAny(materia, "EMPRESAR", "SOCIED", "RECUPERACAO JUDICIAL", "FALENCIA", "ASSEMBLEIA DE CREDORES") ) {
                dbg.put("rule", "empresarial");
                return Result.found(specEmpresarial(pais), 0.74, "match:empresarial", dbg);
            }

            
            
            
            if (ctx.getRito() == Rite.CONSTITUCIONAL ||
                    containsAny(materia, "CONSTITUC", "ADI", "ADC", "ADPF", "RECLAMACAO", "RECURSO EXTRAORDINARIO") ||
                    containsAny(orgao, "STF")) {
                dbg.put("rule", "constitucional");
                return Result.found(specConstitucional(pais), 0.73, "match:constitucional", dbg);
            }

            
            
            
            if (containsAny(orgao, "VARA", "TRIBUNAL", "JUIZADO", "TURMA", "CAMARA", "FORO")) {
                dbg.put("rule", "fallback:orgao");
                return Result.found(specComum(pais, tratado), 0.65, "fallback:orgao", dbg);
            }

            
            
            
            dbg.put("rule", "notFound");
            return Result.notFound("insufficient context", dbg);
        }

        

        private JurisdictionSpec specComum(String pais, String tratado) {
            return JurisdictionSpec.builder()
                    .label("JUSTICA COMUM")
                    .category("COMUM")
                    .rite(Rite.COMUM)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder()
                                    .name("Vara/Tribunal competente")
                                    .level("local")
                                    .build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder()
                                    .citation("CF/88 art. 5º")
                                    .description("devido processo legal")
                                    .build()
                    ))
                    .countries(List.of(
                            JurisdictionSpec.Country.builder()
                                    .name(paisOrDefault(pais))
                                    .iso(iso(pais))
                                    .build()
                    ))
                    .treaties((tratado == null || tratado.isBlank()) ? List.of() : List.of(tratado))
                    .build();
        }

        private JurisdictionSpec specPenal(String pais) {
            return JurisdictionSpec.builder()
                    .label("JUSTICA CRIMINAL")
                    .category("PENAL")
                    .rite(Rite.PENAL)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("Vara Criminal").level("1o grau").build(),
                            JurisdictionSpec.Authority.builder().name("Tribunal de Justiça").level("2o grau").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("CPP").description("processo penal").build(),
                            JurisdictionSpec.LegalBase.builder().citation("CF/88 art. 5º, LIV/LV").description("DPL/contraditório").build()
                    ))
                    .countries(List.of(JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()))
                    .treaties(List.of())
                    .build();
        }

        private JurisdictionSpec specExecucaoPenal(String pais) {
            return JurisdictionSpec.builder()
                    .label("EXECUCAO PENAL")
                    .category("EXECUCAO_PENAL")
                    .rite(Rite.EXECUCAO_PENAL)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("Vara de Execuções Penais").level("1o grau").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("Lei 7.210/84 (LEP)").description("execução penal").build(),
                            JurisdictionSpec.LegalBase.builder().citation("CF/88 art. 5º").description("garantias fundamentais").build()
                    ))
                    .countries(List.of(JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()))
                    .treaties(List.of())
                    .build();
        }

        private JurisdictionSpec specMilitar(String pais) {
            return JurisdictionSpec.builder()
                    .label("JUSTICA MILITAR")
                    .category("MILITAR")
                    .rite(Rite.MILITAR)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("Conselho de Justiça").level("colegiado").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("CPM/CPPM").description("direito penal militar").build()
                    ))
                    .countries(List.of(JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()))
                    .treaties(List.of())
                    .build();
        }

        private JurisdictionSpec specTributario(String pais) {
            return JurisdictionSpec.builder()
                    .label("FAZENDA PUBLICA / TRIBUTARIO")
                    .category("TRIBUTARIO")
                    .rite(Rite.TRIBUTARIO)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("Vara de Execução Fiscal").level("1o grau").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("Lei 6.830/80").description("execução fiscal").build(),
                            JurisdictionSpec.LegalBase.builder().citation("CTN").description("normas gerais tributárias").build()
                    ))
                    .countries(List.of(
                            JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()
                    ))
                    .treaties(List.of())
                    .build();
        }

        private JurisdictionSpec specAgrario(String pais) {
            return JurisdictionSpec.builder()
                    .label("AGRARIO / FUNDIARIO")
                    .category("AGRARIO")
                    .rite(Rite.AGRARIO)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("Vara Agrária").level("especializada").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("CF/88 art. 186").description("função social").build(),
                            JurisdictionSpec.LegalBase.builder().citation("CPC").description("tutelas possessórias").build()
                    ))
                    .countries(List.of(
                            JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()
                    ))
                    .treaties(List.of())
                    .build();
        }

        private JurisdictionSpec specDesportivo(String pais) {
            return JurisdictionSpec.builder()
                    .label("JUSTICA DESPORTIVA")
                    .category("DESPORTIVO")
                    .rite(Rite.DESPORTIVO)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("STJD/Comissão Disciplinar").level("privada").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("CBJD").description("justiça desportiva").build()
                    ))
                    .countries(List.of(
                            JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()
                    ))
                    .treaties(List.of("CAS"))
                    .build();
        }

        private JurisdictionSpec specFamilia(String pais) {
            return JurisdictionSpec.builder()
                    .label("FAMILIA")
                    .category("FAMILIA")
                    .rite(Rite.FAMILIA)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("Vara de Família").level("1o grau").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("CPC art. 189, II").description("segredo de justiça").build(),
                            JurisdictionSpec.LegalBase.builder().citation("CC").description("direito de família").build()
                    ))
                    .countries(List.of(
                            JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()
                    ))
                    .treaties(List.of())
                    .build();
        }

        private JurisdictionSpec specTrabalhista(String pais) {
            return JurisdictionSpec.builder()
                    .label("JUSTICA DO TRABALHO")
                    .category("TRABALHISTA")
                    .rite(Rite.TRABALHISTA)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("Vara do Trabalho").level("1o grau").build(),
                            JurisdictionSpec.Authority.builder().name("TRT").level("2o grau").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("CLT").description("processo do trabalho").build(),
                            JurisdictionSpec.LegalBase.builder().citation("CF/88 art. 7º").description("direitos sociais").build()
                    ))
                    .countries(List.of(JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()))
                    .treaties(List.of())
                    .build();
        }

        private JurisdictionSpec specPrevidenciario(String pais) {
            return JurisdictionSpec.builder()
                    .label("PREVIDENCIARIO")
                    .category("PREVIDENCIARIO")
                    .rite(Rite.PREVIDENCIARIO)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("Vara Federal/JEF").level("1o grau").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("Lei 8.213/91").description("benefícios previdenciários").build(),
                            JurisdictionSpec.LegalBase.builder().citation("Lei 10.259/01").description("JEF").build()
                    ))
                    .countries(List.of(JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()))
                    .treaties(List.of())
                    .build();
        }

        private JurisdictionSpec specEleitoral(String pais) {
            return JurisdictionSpec.builder()
                    .label("JUSTICA ELEITORAL")
                    .category("ELEITORAL")
                    .rite(Rite.ELEITORAL)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("Zona Eleitoral/TRE").level("regional").build(),
                            JurisdictionSpec.Authority.builder().name("TSE").level("superior").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("Código Eleitoral").description("processo eleitoral").build(),
                            JurisdictionSpec.LegalBase.builder().citation("Lei 9.504/97").description("eleições").build()
                    ))
                    .countries(List.of(JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()))
                    .treaties(List.of())
                    .build();
        }

        private JurisdictionSpec specConsumidor(String pais) {
            return JurisdictionSpec.builder()
                    .label("CONSUMIDOR")
                    .category("CONSUMIDOR")
                    .rite(Rite.CONSUMIDOR)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("Juizado/Justiça Comum").level("conforme valor/competência").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("CDC (Lei 8.078/90)").description("proteção ao consumidor").build()
                    ))
                    .countries(List.of(JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()))
                    .treaties(List.of())
                    .build();
        }

        private JurisdictionSpec specEmpresarial(String pais) {
            return JurisdictionSpec.builder()
                    .label("EMPRESARIAL")
                    .category("EMPRESARIAL")
                    .rite(Rite.EMPRESARIAL)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("Vara Empresarial/Falências").level("especializada").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("Lei 11.101/05").description("recuperação/falência").build(),
                            JurisdictionSpec.LegalBase.builder().citation("CC").description("direito societário/obrigacional").build()
                    ))
                    .countries(List.of(JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()))
                    .treaties(List.of())
                    .build();
        }

        private JurisdictionSpec specConstitucional(String pais) {
            return JurisdictionSpec.builder()
                    .label("CONSTITUCIONAL")
                    .category("CONSTITUCIONAL")
                    .rite(Rite.CONSTITUCIONAL)
                    .authorities(List.of(
                            JurisdictionSpec.Authority.builder().name("STF").level("corte constitucional").build()
                    ))
                    .legalBases(List.of(
                            JurisdictionSpec.LegalBase.builder().citation("CF/88").description("controle de constitucionalidade").build(),
                            JurisdictionSpec.LegalBase.builder().citation("Lei 9.868/99").description("ADI/ADC").build(),
                            JurisdictionSpec.LegalBase.builder().citation("Lei 9.882/99").description("ADPF").build()
                    ))
                    .countries(List.of(JurisdictionSpec.Country.builder().name(paisOrDefault(pais)).iso(iso(pais)).build()))
                    .treaties(List.of())
                    .build();
        }

        

        private String norm(String s) {
            if (s == null) return "";
            String t = s.trim().toUpperCase(Locale.ROOT);

            
            t = Normalizer.normalize(t, Normalizer.Form.NFD);
            t = t.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

            
            t = t.replace('\u00A0', ' '); 
            t = t.replaceAll("\\s+", " ");

            return t;
        }

        private boolean containsAny(String text, String... needles) {
            if (text == null) return false;
            String t = norm(text);
            for (String n : needles) {
                if (t.contains(norm(n))) return true;
            }
            return false;
        }

        private String paisOrDefault(String pais) {
            return (pais == null || pais.isBlank()) ? "BRASIL" : pais;
        }

        private String iso(String pais) {
            if (pais == null) return "BR";
            String p = norm(pais);
            if (p.startsWith("BR") || p.contains("BRASIL")) return "BR";
            if (p.startsWith("US") || p.contains("UNITED STATES") || p.contains("EUA")) return "US";
            if (p.startsWith("PT") || p.contains("PORTUGAL")) return "PT";
            return "XX";
        }
    }
}
