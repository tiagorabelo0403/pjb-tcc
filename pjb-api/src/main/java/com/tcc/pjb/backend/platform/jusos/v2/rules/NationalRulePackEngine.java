package com.tcc.pjb.backend.platform.jusos.v2.rules;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;

@Service
public class NationalRulePackEngine {

    private final SalarioMinimoNacionalService salarioMinimoNacionalService;

    public NationalRulePackEngine(SalarioMinimoNacionalService salarioMinimoNacionalService) {
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
    }

    public record ContextoRegra(
            String classeTPU,
            String assuntoTPU,
            RamoDireito ramo,
            GrauJurisdicao grau,
            String tribunalCodigo,
            Map<String, Object> extras
    ) {
        public ContextoRegra {
            extras = sanitizeMap(extras);
            tribunalCodigo = normalizeText(tribunalCodigo);
            classeTPU = normalizeNullable(classeTPU);
            assuntoTPU = normalizeNullable(assuntoTPU);
        }

        public boolean hasExtra(String key) {
            if (key == null || key.isBlank()) {
                return false;
            }
            return extras.containsKey(key.trim());
        }

        public String extraAsString(String key) {
            Object value = extras.get(key);
            return value == null ? null : String.valueOf(value);
        }

        public boolean extraAsBoolean(String key) {
            Object value = extras.get(key);
            if (value instanceof Boolean b) {
                return b;
            }
            if (value instanceof Number n) {
                return n.intValue() != 0;
            }
            if (value instanceof String s) {
                String token = normalizeToken(s);
                return Set.of("TRUE", "SIM", "YES", "Y", "1", "VERDADEIRO").contains(token);
            }
            return false;
        }

        public BigDecimal extraAsBigDecimal(String key) {
            Object value = extras.get(key);
            if (value == null) {
                return null;
            }
            if (value instanceof BigDecimal bd) {
                return bd;
            }
            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            if (value instanceof String s) {
                try {
                    return new BigDecimal(s.replace(".", "").replace(",", ".").trim());
                } catch (Exception ignored) {
                    return null;
                }
            }
            return null;
        }

        public boolean assuntoContem(String... termos) {
            return containsAny(assuntoTPU, termos);
        }

        public boolean classeContem(String... termos) {
            return containsAny(classeTPU, termos);
        }

        public boolean matchTribunal(String... codigos) {
            if (tribunalCodigo == null || tribunalCodigo.isBlank() || codigos == null || codigos.length == 0) {
                return false;
            }
            String token = normalizeToken(tribunalCodigo);
            for (String codigo : codigos) {
                if (token.equals(normalizeToken(codigo))) {
                    return true;
                }
            }
            return false;
        }
    }

    public sealed interface Regra permits RegraAdmissibilidade, RegraPrazoEspecifico,
            RegraRequisito, RegraAlerta, RegraFluxo {

        String codigo();

        String descricao();

        RamoDireito ramo();
    }

    public record RegraAdmissibilidade(
            String codigo,
            String descricao,
            RamoDireito ramo,
            List<String> requisitos,
            String fundamento
    ) implements Regra {
        public RegraAdmissibilidade {
            requisitos = immutableDistinct(requisitos);
        }
    }

    public record RegraPrazoEspecifico(
            String codigo,
            String descricao,
            RamoDireito ramo,
            String tipoAto,
            int dias,
            boolean uteis,
            String fundamento
    ) implements Regra {
        public boolean compativelCom(NationalPrazoEngine.TipoPrazo tipoPrazo) {
            if (tipoPrazo == null || tipoAto == null || tipoAto.isBlank()) {
                return false;
            }
            return normalizeToken(tipoAto).contains(normalizeToken(tipoPrazo.name()))
                    || normalizeToken(tipoPrazo.name()).contains(normalizeToken(tipoAto));
        }
    }

    public record RegraRequisito(
            String codigo,
            String descricao,
            RamoDireito ramo,
            List<String> documentosObrigatorios,
            boolean bloqueante
    ) implements Regra {
        public RegraRequisito {
            documentosObrigatorios = immutableDistinct(documentosObrigatorios);
        }
    }

    public record RegraAlerta(
            String codigo,
            String descricao,
            RamoDireito ramo,
            String mensagemAlerta,
            String nivel
    ) implements Regra {
        public RegraAlerta {
            nivel = normalizeAlertLevel(nivel);
        }
    }

    public record RegraFluxo(
            String codigo,
            String descricao,
            RamoDireito ramo,
            String faseOrigem,
            String proximaFase,
            boolean exigeAprovacao
    ) implements Regra {}

    public record ResultadoRegras(
            List<Regra> aplicadas,
            List<String> alertas,
            List<String> requisitosIdentificados,
            boolean bloqueante,
            int totalRegrasAvaliadas
    ) {
        public ResultadoRegras {
            aplicadas = List.copyOf(distinctByCodigo(aplicadas));
            alertas = immutableDistinct(alertas);
            requisitosIdentificados = immutableDistinct(requisitosIdentificados);
            totalRegrasAvaliadas = Math.max(totalRegrasAvaliadas, aplicadas.size());
        }

        public boolean temAlertasCriticos() {
            return alertas.stream().anyMatch(a -> a != null && normalizeToken(a).contains("CRITICO"));
        }

        public boolean possuiRegra(String codigo) {
            String token = normalizeToken(codigo);
            return aplicadas.stream().anyMatch(r -> normalizeToken(r.codigo()).equals(token));
        }

        public List<RegraPrazoEspecifico> prazosEspecificos() {
            return aplicadas.stream()
                    .filter(RegraPrazoEspecifico.class::isInstance)
                    .map(RegraPrazoEspecifico.class::cast)
                    .toList();
        }
    }

    private static final String KEY_ALL = "TODAS";
    private static final String KEY_ADMIN = "ADMINISTRATIVO";
    private static final Map<String, List<Regra>> REGRAS_NACIONAIS = buildNationalRules();
    private static final int MAX_CUSTOM_BUCKETS = 256;

    private final Map<String, CopyOnWriteArrayList<Regra>> regrasCustomizadas = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<Regra>>> regrasCustomizadasPorOrigem = new ConcurrentHashMap<>();
    private final Map<String, Instant> bucketTouch = new ConcurrentHashMap<>();

    public ResultadoRegras aplicar(ContextoRegra ctx) {
        Objects.requireNonNull(ctx, "ctx");

        LinkedHashMap<String, Regra> candidatas = new LinkedHashMap<>();
        addRules(candidatas, REGRAS_NACIONAIS.get(KEY_ALL));
        if (ctx.ramo() != null) {
            addRules(candidatas, REGRAS_NACIONAIS.get(ctx.ramo().name()));
        }
        if (ctx.grau() != null) {
            addRules(candidatas, REGRAS_NACIONAIS.get(ctx.grau().name()));
        }
        addRules(candidatas, REGRAS_NACIONAIS.get(tribunalKey(ctx.tribunalCodigo())));
        addCustomRules(candidatas, customKey(ctx.tribunalCodigo(), ctx.ramo(), ctx.grau()));
        addCustomRules(candidatas, customKey(ctx.tribunalCodigo(), ctx.ramo(), null));
        addCustomRules(candidatas, customKey(ctx.tribunalCodigo(), null, ctx.grau()));
        addCustomRules(candidatas, customKey(ctx.tribunalCodigo(), null, null));
        addRules(candidatas, inferDynamicRules(ctx));

        List<Regra> aplicadas = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        List<String> requisitos = new ArrayList<>();
        boolean bloqueante = false;
        int total = 0;

        for (Regra regra : candidatas.values()) {
            total++;
            if (!isApplicable(regra, ctx)) {
                continue;
            }
            aplicadas.add(regra);
            switch (regra) {
                case RegraAlerta alerta -> alertas.add("[" + alerta.nivel() + "] " + alerta.mensagemAlerta());
                case RegraRequisito requisito -> {
                    requisitos.addAll(requisito.documentosObrigatorios());
                    if (requisito.bloqueante()) {
                        bloqueante = true;
                    }
                }
                case RegraAdmissibilidade admissibilidade -> {
                    requisitos.addAll(admissibilidade.requisitos());
                    alertas.add("Admissibilidade — " + admissibilidade.descricao() + " (" + admissibilidade.fundamento() + ")");
                }
                case RegraPrazoEspecifico prazo -> alertas.add("Prazo específico — " + prazo.descricao() + " (" + prazo.fundamento() + ")");
                case RegraFluxo fluxo -> {
                    if (fluxo.exigeAprovacao()) {
                        alertas.add("Fluxo controlado — " + fluxo.faseOrigem() + " → " + fluxo.proximaFase() + " exige aprovação");
                    }
                }
            }
        }

        return new ResultadoRegras(aplicadas, alertas, requisitos, bloqueante, total);
    }

    public void registrarRegraCustomizada(String tribunalCodigo, RamoDireito ramo, Regra regra) {
        registrarRegraCustomizada(tribunalCodigo, ramo, null, regra);
    }

    public void registrarRegraCustomizada(String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau, Regra regra) {
        Objects.requireNonNull(regra, "regra");
        String bucket = customKey(tribunalCodigo, ramo, grau);
        touchBucket(bucket);
        regrasCustomizadas.computeIfAbsent(bucket, key -> new CopyOnWriteArrayList<>())
                .add(regra);
        trimCustomBuckets();
    }


    public void substituirRegrasCustomizadas(String origem, String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau, Collection<Regra> regras) {
        String bucket = customKey(tribunalCodigo, ramo, grau);
        String owner = ownerKey(origem);
        touchBucket(bucket);
        Map<String, List<Regra>> porOrigem = regrasCustomizadasPorOrigem.computeIfAbsent(bucket, key -> new ConcurrentHashMap<>());
        if (regras == null || regras.isEmpty()) {
            porOrigem.remove(owner);
        } else {
            porOrigem.put(owner, List.copyOf(distinctByCodigoUltimaGanha(regras)));
        }
        rebuildBucketCustomizado(bucket);
        trimCustomBuckets();
    }

    public void removerRegrasCustomizadas(String origem, String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau) {
        String bucket = customKey(tribunalCodigo, ramo, grau);
        Map<String, List<Regra>> porOrigem = regrasCustomizadasPorOrigem.get(bucket);
        if (porOrigem == null || porOrigem.isEmpty()) {
            removeBucket(bucket);
            return;
        }
        porOrigem.remove(ownerKey(origem));
        rebuildBucketCustomizado(bucket);
    }

    private void rebuildBucketCustomizado(String bucket) {
        Map<String, List<Regra>> porOrigem = regrasCustomizadasPorOrigem.get(bucket);
        if (porOrigem == null || porOrigem.isEmpty()) {
            removeBucket(bucket);
            return;
        }
        LinkedHashMap<String, Regra> merged = new LinkedHashMap<>();
        porOrigem.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    for (Regra regra : entry.getValue()) {
                        if (regra != null && regra.codigo() != null && !regra.codigo().isBlank()) {
                            merged.put(normalizeToken(regra.codigo()), regra);
                        }
                    }
                });
        if (merged.isEmpty()) {
            removeBucket(bucket);
            return;
        }
        touchBucket(bucket);
        regrasCustomizadas.put(bucket, new CopyOnWriteArrayList<>(merged.values()));
        trimCustomBuckets();
    }

    public List<Regra> listarRegras(String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau) {
        LinkedHashMap<String, Regra> regras = new LinkedHashMap<>();
        addRules(regras, REGRAS_NACIONAIS.get(KEY_ALL));
        if (ramo != null) {
            addRules(regras, REGRAS_NACIONAIS.get(ramo.name()));
        }
        if (grau != null) {
            addRules(regras, REGRAS_NACIONAIS.get(grau.name()));
        }
        addRules(regras, REGRAS_NACIONAIS.get(tribunalKey(tribunalCodigo)));
        addCustomRules(regras, customKey(tribunalCodigo, ramo, grau));
        addCustomRules(regras, customKey(tribunalCodigo, ramo, null));
        addCustomRules(regras, customKey(tribunalCodigo, null, grau));
        addCustomRules(regras, customKey(tribunalCodigo, null, null));
        return List.copyOf(regras.values());
    }

    private void addCustomRules(LinkedHashMap<String, Regra> regras, String bucket) {
        touchBucket(bucket);
        addRules(regras, regrasCustomizadas.get(bucket));
    }

    private void touchBucket(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            return;
        }
        bucketTouch.put(bucket, Instant.now());
    }

    private void removeBucket(String bucket) {
        regrasCustomizadas.remove(bucket);
        regrasCustomizadasPorOrigem.remove(bucket);
        bucketTouch.remove(bucket);
    }

    private void trimCustomBuckets() {
        while (Math.max(bucketTouch.size(), regrasCustomizadas.size()) > MAX_CUSTOM_BUCKETS) {
            String bucket = bucketTouch.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .filter(regrasCustomizadas::containsKey)
                    .findFirst()
                    .orElseGet(() -> regrasCustomizadas.keySet().stream().sorted().findFirst().orElse(null));
            if (bucket == null || bucket.isBlank()) {
                bucketTouch.clear();
                regrasCustomizadas.clear();
                regrasCustomizadasPorOrigem.clear();
                return;
            }
            removeBucket(bucket);
        }
        bucketTouch.keySet().removeIf(key -> !regrasCustomizadas.containsKey(key));
    }

    private boolean isApplicable(Regra regra, ContextoRegra ctx) {
        if (regra == null) {
            return false;
        }
        RamoDireito ramoRegra = regra.ramo();
        return ramoRegra == null || ramoRegra == ctx.ramo();
    }

    private List<Regra> inferDynamicRules(ContextoRegra ctx) {
        List<Regra> regras = new ArrayList<>();
        RamoDireito ramo = ctx.ramo();
        BigDecimal valorCausa = ctx.extraAsBigDecimal("valorCausa");

        if (ramo == RamoDireito.CIVIL || ramo == RamoDireito.CONSUMIDOR) {
            if (valorCausa != null && valorCausa.compareTo(salarioMinimoNacionalService.multiplicar(new BigDecimal("40"), LocalDate.now())) <= 0) {
                regras.add(new RegraAlerta(
                        "JEC_COMPETENCIA_POTENCIAL",
                        "Competência potencial do Juizado Especial",
                        ramo,
                        "Valor compatível com rito dos Juizados Especiais. Verificar complexidade da prova, pedido e competência local.",
                        "INFO"
                ));
            }
        }

        if (ramo == RamoDireito.PREVIDENCIARIO && valorCausa != null
                && valorCausa.compareTo(salarioMinimoNacionalService.multiplicar(new BigDecimal("60"), LocalDate.now())) <= 0) {
            regras.add(new RegraAlerta(
                    "JEF_COMPETENCIA_POTENCIAL",
                    "Competência potencial do JEF",
                    RamoDireito.PREVIDENCIARIO,
                    "Valor compatível com Juizado Especial Federal. Verificar competência absoluta, renúncia e matéria excluída.",
                    "INFO"
            ));
        }

        if (ctx.extraAsBoolean("tutelaUrgencia") || ctx.assuntoContem("urgencia", "liminar", "antecipada")) {
            regras.add(new RegraAlerta(
                    "TUTELA_URGENCIA_CHECK",
                    "Tutela de urgência",
                    ramo,
                    "Há indício de tutela de urgência. Validar probabilidade do direito, perigo de dano e reversibilidade prática.",
                    "CRITICO"
            ));
        }

        if (ctx.extraAsBoolean("violenciaDomestica") || ctx.assuntoContem("violencia domestica", "maria da penha")) {
            regras.add(new RegraAlerta(
                    "VIOLENCIA_DOMESTICA_PRIORIDADE",
                    "Violência doméstica com prioridade reforçada",
                    ramo == null ? RamoDireito.PENAL : ramo,
                    "Caso com indícios de violência doméstica. Reforçar sigilo, prioridade de tramitação e proteção de dados pessoais sensíveis.",
                    "CRITICO"
            ));
            regras.add(new RegraRequisito(
                    "VIOLENCIA_DOMESTICA_DOCS",
                    "Documentos mínimos para tutela protetiva",
                    ramo == null ? RamoDireito.PENAL : ramo,
                    List.of("BOLETIM_OCORRENCIA_SE_HOUVER", "RELATO_CIRCUNSTANCIADO", "INDICACAO_RISCO_ATUAL"),
                    false
            ));
        }

        if (ctx.extraAsBoolean("provaDigital") || ctx.assuntoContem("prova digital", "whatsapp", "email", "metadados")) {
            regras.add(new RegraRequisito(
                    "PROVA_DIGITAL_INTEGRIDADE",
                    "Integridade de prova digital",
                    ramo,
                    List.of("HASH_ARQUIVOS", "CADEIA_CUSTODIA_DIGITAL", "METADADOS_ORIGEM"),
                    ramo == RamoDireito.PENAL
            ));
        }

        if (ctx.extraAsBoolean("incapaz") || ctx.assuntoContem("incapaz", "menor", "curatela", "interdicao")) {
            regras.add(new RegraAlerta(
                    "INCAPAZ_INTERVENCAO_MP",
                    "Atuação institucional reforçada",
                    ramo == null ? RamoDireito.FAMILIA : ramo,
                    "Há indicativo de incapaz ou vulnerável. Validar intervenção obrigatória do Ministério Público e reforço de sigilo quando cabível.",
                    "CRITICO"
            ));
        }

        if (ctx.extraAsBoolean("recuperacaoJudicial") || ctx.classeContem("recuperacao judicial", "falencia")) {
            regras.add(new RegraRequisito(
                    "RECUPERACAO_DOCS_MINIMOS",
                    "Documentação econômica mínima",
                    RamoDireito.EMPRESARIAL,
                    List.of("DEMONSTRACOES_CONTABEIS", "RELACAO_CREDORES", "FLUXO_CAIXA_PROJETADO", "RELATORIO_GOVERNANCA"),
                    true
            ));
        }

        if (ctx.extraAsBoolean("execucaoFiscal") || ctx.classeContem("execucao fiscal")) {
            regras.add(new RegraRequisito(
                    "EXECUCAO_FISCAL_TITULO",
                    "Título para execução fiscal",
                    RamoDireito.TRIBUTARIO,
                    List.of("CERTIDAO_DIVIDA_ATIVA", "DEMONSTRATIVO_ATUALIZADO_DEBITO"),
                    true
            ));
        }

        if (ctx.grau() == GrauJurisdicao.SUPERIOR) {
            regras.add(new RegraAlerta(
                    "FILTRO_SUPERIOR_PRECEDENTES",
                    "Filtro de admissibilidade em tribunal superior",
                    ramo,
                    "Recurso em tribunal superior exige aderência fina a precedentes, prequestionamento, tempestividade e dialeticidade recursal.",
                    "CRITICO"
            ));
        }

        if (ctx.grau() == GrauJurisdicao.CONSTITUCIONAL) {
            regras.add(new RegraAlerta(
                    "FILTRO_CONSTITUCIONAL_IMPACTO",
                    "Controle constitucional de alto impacto",
                    ramo == null ? RamoDireito.CONSTITUCIONAL : ramo,
                    "Caso constitucional exige delimitação precisa da controvérsia, pertinência temática, legitimidade ativa e impactos sistêmicos da decisão.",
                    "CRITICO"
            ));
        }

        if (ctx.matchTribunal("TST") && ramo == RamoDireito.TRABALHISTA) {
            regras.add(new RegraAlerta(
                    "TST_TRANSCENDENCIA",
                    "Transcendência recursal trabalhista",
                    RamoDireito.TRABALHISTA,
                    "No TST, avaliar transcendência econômica, política, social ou jurídica do recurso.",
                    "CRITICO"
            ));
        }

        if (ctx.matchTribunal("STJ") && (ramo == RamoDireito.CIVIL || ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.EMPRESARIAL)) {
            regras.add(new RegraAlerta(
                    "STJ_PREQUESTIONAMENTO",
                    "Filtro de admissibilidade no STJ",
                    ramo,
                    "No STJ, verificar prequestionamento, demonstração de dissídio quando cabível e impugnação específica de todos os fundamentos.",
                    "CRITICO"
            ));
        }

        if (ctx.matchTribunal("STF") || ctx.ramo() == RamoDireito.CONSTITUCIONAL) {
            if (ctx.classeContem("recurso extraordinario") || ctx.extraAsBoolean("repercussaoGeral")) {
                regras.add(new RegraAdmissibilidade(
                        "STF_REPERCUSSAO_GERAL",
                        "Repercussão geral no recurso extraordinário",
                        RamoDireito.CONSTITUCIONAL,
                        List.of("PREQUESTIONAMENTO_CONSTITUCIONAL", "DEMONSTRACAO_REPERCUSSAO_GERAL"),
                        "CF art. 102, §3º"
                ));
            }
        }

        return regras;
    }

    private static Map<String, List<Regra>> buildNationalRules() {
        Map<String, List<Regra>> rules = new LinkedHashMap<>();

        rules.put(KEY_ALL, List.of(
                new RegraAlerta("ALERTA_SIGILO_AUTO", "Verificação de sigilo automático", null,
                        "Verifique se o ramo, a matéria ou a vulnerabilidade das partes impõem sigilo automático ou reforçado.", "INFO"),
                new RegraAdmissibilidade("ADMIS_GERAL", "Requisitos gerais de admissibilidade", null,
                        List.of("PETICAO_INICIAL", "DOCUMENTOS_ESSENCIAIS", "REPRESENTACAO_PROCESSUAL"),
                        "CPC arts. 319 e 320"),
                new RegraFluxo("FLUXO_SANEAMENTO_BASE", "Saneamento processual obrigatório", null,
                        "POSTULATORIA", "SANEAMENTO", false)
        ));

        rules.put(KEY_ADMIN, List.of(
                new RegraAlerta("ADM_CONTROLE_LEGALIDADE", "Controle de legalidade administrativa", RamoDireito.ADMINISTRATIVO,
                        "Validar competência do ato, motivação, contraditório, proporcionalidade e eventual necessidade de suspensão liminar.", "CRITICO"),
                new RegraRequisito("ADM_DOCS_MINIMOS", "Documentos administrativos mínimos", RamoDireito.ADMINISTRATIVO,
                        List.of("ATO_ADMINISTRATIVO_IMPUGNADO", "PROVA_DA_CIENCIA", "PROCESSO_ADMINISTRATIVO_SE_HOUVER"), false)
        ));

        rules.put(GrauJurisdicao.SEGUNDO_GRAU.name(), List.of(
                new RegraAlerta("SEGUNDO_GRAU_DIALETICIDADE", "Dialeticidade recursal", null,
                        "No segundo grau, conferir impugnação específica dos fundamentos, regularidade formal e preparo quando exigível.", "CRITICO")
        ));

        rules.put(GrauJurisdicao.SUPERIOR.name(), List.of(
                new RegraAlerta("SUPERIOR_PRECEDENTES", "Filtro de precedentes", null,
                        "Em tribunal superior, conferir aderência a precedentes, demonstração analítica de distinção e eventual superação.", "CRITICO")
        ));

        rules.put(GrauJurisdicao.CONSTITUCIONAL.name(), List.of(
                new RegraAlerta("CONSTITUCIONAL_IMPACTO", "Impacto sistêmico constitucional", RamoDireito.CONSTITUCIONAL,
                        "No controle constitucional, delimitar objeto, parâmetros, efeitos e eventual modulação temporal da decisão.", "CRITICO")
        ));

        rules.put(RamoDireito.PENAL.name(), List.of(
                new RegraAlerta("PENAL_PRESCRICAO", "Verificação de prescrição penal", RamoDireito.PENAL,
                        "Obrigatório verificar prescrição conforme pena máxima abstrata, marcos interruptivos, suspensivos e reincidência quando pertinente.", "CRITICO"),
                new RegraAlerta("PENAL_CUSTODIA", "Audiência de custódia", RamoDireito.PENAL,
                        "Prisão em flagrante exige análise célere de audiência de custódia e legalidade da captura.", "CRITICO"),
                new RegraRequisito("PENAL_CADEIA_CUSTODIA", "Cadeia de custódia", RamoDireito.PENAL,
                        List.of("INDICACAO_PROVAS", "CADEIA_CUSTODIA_DIGITAL", "AUTO_PRISAO_OU_PORTARIA"), true),
                new RegraAdmissibilidade("PENAL_ADMIS", "Admissibilidade da denúncia ou queixa", RamoDireito.PENAL,
                        List.of("DESCRICAO_FATO", "QUALIFICACAO_ACUSADO", "JUSTA_CAUSA_MINIMA"), "CPP art. 41"),
                new RegraPrazoEspecifico("PENAL_RESPOSTA", "Resposta à acusação", RamoDireito.PENAL,
                        "APRESENTACAO_DEFESA_PENAL", 10, false, "CPP art. 396-A"),
                new RegraFluxo("PENAL_FLUXO_INICIAL", "Fluxo inicial penal", RamoDireito.PENAL,
                        "DISTRIBUICAO", "CITACAO_RESPOSTA_ACUSACAO", false)
        ));

        rules.put(RamoDireito.TRABALHISTA.name(), List.of(
                new RegraAlerta("TRAB_PRESCRICAO", "Prescrição trabalhista", RamoDireito.TRABALHISTA,
                        "Conferir prescrição bienal pós-extinção e quinquenal sobre parcelas, além de eventuais marcos interruptivos.", "CRITICO"),
                new RegraRequisito("TRAB_DOCS", "Documentação trabalhista mínima", RamoDireito.TRABALHISTA,
                        List.of("CTPS", "CONTRATO_TRABALHO", "HOLERITES", "TERMO_RESCISAO_SE_HOUVER"), false),
                new RegraPrazoEspecifico("TRAB_RECURSO", "Recurso ordinário trabalhista", RamoDireito.TRABALHISTA,
                        "RECURSO_TRABALHISTA", 8, false, "CLT art. 895"),
                new RegraPrazoEspecifico("TRAB_EMBARGOS", "Embargos de declaração trabalhistas", RamoDireito.TRABALHISTA,
                        "EMBARGOS_DECLARACAO", 5, false, "CLT art. 897-A"),
                new RegraAlerta("TRAB_AUDIENCIA_UNA", "Audiência una", RamoDireito.TRABALHISTA,
                        "A audiência una concentra instrução e julgamento; organizar prova e estratégia já no início do caso.", "INFO")
        ));

        rules.put(RamoDireito.PREVIDENCIARIO.name(), List.of(
                new RegraAlerta("PREV_COMPETENCIA", "Competência previdenciária", RamoDireito.PREVIDENCIARIO,
                        "Verificar competência do JEF, valor da causa, matéria excluída e prova técnica necessária.", "INFO"),
                new RegraAlerta("PREV_REFORMA", "Reforma previdenciária", RamoDireito.PREVIDENCIARIO,
                        "Analisar regras de transição, DER, DIB, DCB e impactos da EC 103/2019 no benefício pretendido.", "CRITICO"),
                new RegraRequisito("PREV_DOCS", "Documentos previdenciários mínimos", RamoDireito.PREVIDENCIARIO,
                        List.of("CNIS_COMPLETO", "CARTA_CONCESSAO_OU_NEGATIVA", "DOCUMENTO_MEDICO_SE_INCAPACIDADE"), false)
        ));

        rules.put(RamoDireito.AMBIENTAL.name(), List.of(
                new RegraAlerta("AMB_IMPRESCRITIBILIDADE", "Dano ambiental", RamoDireito.AMBIENTAL,
                        "Conferir regime de imprescritibilidade, responsabilidade objetiva e tutela inibitória ou reparatória cabível.", "CRITICO"),
                new RegraRequisito("AMB_ACP", "Documentos ambientais mínimos", RamoDireito.AMBIENTAL,
                        List.of("LAUDO_TECNICO", "INDICACAO_DANO", "LEGITIMIDADE_ATIVA"), false)
        ));

        rules.put(RamoDireito.FAMILIA.name(), List.of(
                new RegraAlerta("FAM_SIGILO", "Sigilo obrigatório", RamoDireito.FAMILIA,
                        "Processos de família exigem sigilo, tratamento minimizado de dados e controle reforçado de documentos sensíveis.", "CRITICO"),
                new RegraAlerta("FAM_ALIMENTOS", "Urgência em alimentos", RamoDireito.FAMILIA,
                        "Pedidos alimentares podem justificar tutela liminar e organização célere de prova documental básica.", "CRITICO"),
                new RegraAdmissibilidade("FAM_MP", "Intervenção obrigatória do Ministério Público", RamoDireito.FAMILIA,
                        List.of("PARECER_MP_SE_INCAPAZ_OU_INTERESSE_PUBLICO"), "CC art. 178")
        ));

        rules.put(RamoDireito.CONSTITUCIONAL.name(), List.of(
                new RegraAlerta("CONST_ERGA_OMNES", "Controle concentrado", RamoDireito.CONSTITUCIONAL,
                        "Ações do controle concentrado exigem legitimidade do art. 103, pertinência temática e análise de modulação de efeitos.", "CRITICO"),
                new RegraAdmissibilidade("CONST_LEGITIMIDADE", "Legitimidade ativa constitucional", RamoDireito.CONSTITUCIONAL,
                        List.of("LEGITIMACAO_CONSTITUCIONAL", "PARAMETRO_CONSTITUCIONAL_INVOCADO"), "CF art. 103"),
                new RegraPrazoEspecifico("CONST_MS", "Mandado de segurança", RamoDireito.CONSTITUCIONAL,
                        "MANDADO_SEGURANCA", 120, false, "Lei 12.016/2009, art. 23")
        ));

        rules.put(RamoDireito.TRIBUTARIO.name(), List.of(
                new RegraAlerta("TRIB_DECADENCIA", "Prazos tributários", RamoDireito.TRIBUTARIO,
                        "Conferir decadência, prescrição, eventual suspensão da exigibilidade e liquidez da CDA ou do crédito impugnado.", "CRITICO"),
                new RegraAlerta("TRIB_EXECUCAO_FISCAL", "Execução fiscal", RamoDireito.TRIBUTARIO,
                        "Na execução fiscal, validar CDA, prescrição intercorrente, garantia e adequação do rito da LEF.", "CRITICO"),
                new RegraRequisito("TRIB_DOCS", "Documentos tributários mínimos", RamoDireito.TRIBUTARIO,
                        List.of("DOCUMENTO_TRIBUTARIO_PRINCIPAL", "PLANILHA_ATUALIZADA", "COMPROVANTE_ADMINISTRATIVO_SE_HOUVER"), false)
        ));

        rules.put(RamoDireito.ELEITORAL.name(), List.of(
                new RegraAlerta("ELEITORAL_PRAZOS", "Prazos eleitorais", RamoDireito.ELEITORAL,
                        "No eleitoral, conferir calendário oficial, contagem corrida e janelas processuais extremamente curtas.", "CRITICO"),
                new RegraAdmissibilidade("ELEITORAL_AIRC", "Impugnação de registro", RamoDireito.ELEITORAL,
                        List.of("LEGITIMIDADE_AIRC", "CAUSA_DE_INELEGIBILIDADE"), "Código Eleitoral e LC 64/1990")
        ));

        rules.put(RamoDireito.MILITAR.name(), List.of(
                new RegraAlerta("MILITAR_SIGILO", "Sigilo reforçado militar", RamoDireito.MILITAR,
                        "Processos militares exigem habilitação adequada, necessidade de conhecer e proteção reforçada de dados institucionais.", "CRITICO"),
                new RegraPrazoEspecifico("MILITAR_RECURSO", "Recurso militar", RamoDireito.MILITAR,
                        "RECURSO_MILITAR", 10, false, "CPPM"),
                new RegraAdmissibilidade("MILITAR_COMPETENCIA", "Competência da Justiça Militar", RamoDireito.MILITAR,
                        List.of("FATO_TIPICO_MILITAR", "DEMONSTRACAO_DA_COMPETENCIA"), "CF art. 124")
        ));

        rules.put(RamoDireito.INFANCIA_JUVENTUDE.name(), List.of(
                new RegraAlerta("ECA_PRIORIDADE", "Prioridade absoluta", RamoDireito.INFANCIA_JUVENTUDE,
                        "Casos envolvendo criança ou adolescente devem receber prioridade máxima, sigilo e proteção integral.", "CRITICO"),
                new RegraAlerta("ECA_SIGILO", "Sigilo absoluto", RamoDireito.INFANCIA_JUVENTUDE,
                        "Evitar identificação pública e reforçar proteção de dados em todos os atos do processo.", "CRITICO"),
                new RegraAdmissibilidade("ECA_REDE_PROTECAO", "Rede de proteção", RamoDireito.INFANCIA_JUVENTUDE,
                        List.of("INTERVENCAO_MP", "INFORMACAO_DA_REDE_PROTECAO_SE_HOUVER"), "ECA")
        ));

        rules.put(RamoDireito.AGRARIO.name(), List.of(
                new RegraAlerta("AGRARIO_FUNCAO_SOCIAL", "Função social da propriedade", RamoDireito.AGRARIO,
                        "Validar posse, produtividade, função social, risco coletivo e eventual necessidade de audiência prévia em conflitos massivos.", "CRITICO"),
                new RegraAlerta("AGRARIO_CONFLITO_COLETIVO", "Conflito fundiário coletivo", RamoDireito.AGRARIO,
                        "Conflitos coletivos exigem atenção a mediação, segurança institucional e participação de órgãos públicos competentes.", "INFO")
        ));

        rules.put(RamoDireito.CONSUMIDOR.name(), List.of(
                new RegraAlerta("CDC_INVERSAO_ONUS", "Inversão do ônus da prova", RamoDireito.CONSUMIDOR,
                        "Avaliar hipossuficiência, verossimilhança e necessidade de conservação de provas digitais e contratuais.", "INFO"),
                new RegraAlerta("CDC_PRESCRICAO", "Prescrição e decadência consumeristas", RamoDireito.CONSUMIDOR,
                        "Verificar prazos de fato do serviço ou produto e prazos decadenciais para vícios aparentes ou ocultos.", "CRITICO"),
                new RegraAdmissibilidade("CDC_FORO_CONSUMIDOR", "Competência protetiva do consumidor", RamoDireito.CONSUMIDOR,
                        List.of("DOMICILIO_CONSUMIDOR", "RELACAO_DE_CONSUMO_MINIMA"), "CDC art. 101")
        ));

        rules.put(RamoDireito.EMPRESARIAL.name(), List.of(
                new RegraAlerta("EMP_GOVERNANCA", "Governança e risco empresarial", RamoDireito.EMPRESARIAL,
                        "Analisar representação societária, poderes de administração, regularidade registral e eventual insolvência.", "CRITICO"),
                new RegraAlerta("EMP_DESCONSIDERACAO", "Desconsideração da personalidade jurídica", RamoDireito.EMPRESARIAL,
                        "Aferir abuso, fraude, confusão patrimonial e adequação procedimental do incidente correspondente.", "INFO")
        ));

        return Collections.unmodifiableMap(rules);
    }

    private static void addRules(Map<String, Regra> target, Collection<Regra> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Regra regra : source) {
            if (regra != null && regra.codigo() != null && !regra.codigo().isBlank()) {
                target.putIfAbsent(normalizeToken(regra.codigo()), regra);
            }
        }
    }

    private static List<Regra> distinctByCodigo(Collection<Regra> regras) {
        LinkedHashMap<String, Regra> result = new LinkedHashMap<>();
        if (regras != null) {
            for (Regra regra : regras) {
                if (regra != null && regra.codigo() != null && !regra.codigo().isBlank()) {
                    result.putIfAbsent(normalizeToken(regra.codigo()), regra);
                }
            }
        }
        return List.copyOf(result.values());
    }


    private static List<Regra> distinctByCodigoUltimaGanha(Collection<Regra> regras) {
        LinkedHashMap<String, Regra> result = new LinkedHashMap<>();
        if (regras != null) {
            for (Regra regra : regras) {
                if (regra != null && regra.codigo() != null && !regra.codigo().isBlank()) {
                    result.put(normalizeToken(regra.codigo()), regra);
                }
            }
        }
        return List.copyOf(result.values());
    }

    private static List<String> immutableDistinct(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null) {
                String cleaned = value.trim();
                if (!cleaned.isBlank()) {
                    sanitized.add(cleaned);
                }
            }
        }
        return List.copyOf(sanitized);
    }

    private static Map<String, Object> sanitizeMap(Map<String, Object> extras) {
        if (extras == null || extras.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        extras.forEach((k, v) -> {
            if (k != null && !k.isBlank() && v != null) {
                sanitized.put(k.trim(), v);
            }
        });
        return Map.copyOf(sanitized);
    }

    private static String tribunalKey(String tribunalCodigo) {
        String normalized = normalizeToken(tribunalCodigo);
        return normalized.isBlank() ? null : "TRIBUNAL::" + normalized;
    }

    private static String customKey(String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau) {
        String tribunal = normalizeToken(tribunalCodigo);
        String ramoToken = ramo != null ? ramo.name() : "GERAL";
        String grauToken = grau != null ? grau.name() : "GERAL";
        return tribunal + "::" + ramoToken + "::" + grauToken;
    }


    private static String ownerKey(String origem) {
        if (origem == null || origem.isBlank()) {
            return "DEFAULT";
        }
        return normalizeToken(origem);
    }

    private static boolean containsAny(String value, String... terms) {
        if (value == null || value.isBlank() || terms == null || terms.length == 0) {
            return false;
        }
        String normalized = normalizeToken(value);
        for (String term : terms) {
            String token = normalizeToken(term);
            if (!token.isBlank() && normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeAlertLevel(String level) {
        String normalized = normalizeToken(level);
        return switch (normalized) {
            case "CRITICO", "CRITICAL", "ALTO" -> "CRITICO";
            case "WARN", "WARNING", "MEDIO" -> "ALERTA";
            default -> "INFO";
        };
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{Alnum}]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase(Locale.ROOT);
        return normalized;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
