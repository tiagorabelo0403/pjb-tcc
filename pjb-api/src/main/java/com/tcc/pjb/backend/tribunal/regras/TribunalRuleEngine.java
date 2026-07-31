package com.tcc.pjb.backend.tribunal.regras;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import com.tcc.pjb.backend.tribunal.regras.snapshot.AnaliseDesvio;
import com.tcc.pjb.backend.tribunal.regras.snapshot.RegraResolvida;
import com.tcc.pjb.backend.tribunal.regras.snapshot.RelatorioCoberturaTribunal;
import com.tcc.pjb.backend.tribunal.regras.snapshot.SnapshotDistribuicao;
import com.tcc.pjb.backend.tribunal.regras.snapshot.SnapshotPrazo;
import com.tcc.pjb.backend.tribunal.regras.snapshot.SnapshotTriagem;
@Service
public class TribunalRuleEngine {
    public enum NivelRegra {
        NACIONAL(0, "Lei federal / CNJ"),
        TRIBUNAL(1, "Resolução do tribunal"),
        COMARCA(2, "Portaria da comarca"),
        VARA(3, "Ordem de serviço da vara");
        private final int prioridade;
        private final String descricao;
        NivelRegra(int prioridade, String descricao) {
            this.prioridade = prioridade;
            this.descricao = descricao;
        }
        public int prioridade() {
            return prioridade;
        }
        public String descricao() {
            return descricao;
        }
    }

    public enum TipoValor {
        TEXTO,
        INTEIRO,
        DECIMAL,
        BOOLEANO,
        LISTA_TEXTO,
        DURACAO_DIAS
    }

    public enum ModoSobrescrita {
        SUBSTITUIR,
        ESTENDER,
        RESTRINGIR
    }

    public enum NivelDesvio {
        LEVE,
        MODERADO,
        RELEVANTE,
        CRITICO
    }

    public record ChaveRegra(String dominio, String subdominio, String chave) {
        public ChaveRegra {
            dominio = normalizeRequired(dominio, "dominio");
            subdominio = normalizeRequired(subdominio, "subdominio");
            chave = normalizeRequired(chave, "chave");
        }

        public String canonical() {
            return dominio + "." + subdominio + "." + chave;
        }

        public static ChaveRegra de(String path) {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("Caminho de regra não pode ser vazio");
            }
            String[] parts = path.trim().split("\\.", 3);
            if (parts.length != 3) {
                throw new IllegalArgumentException("Chave deve ter formato dominio.subdominio.chave: " + path);
            }
            return new ChaveRegra(parts[0], parts[1], parts[2]);
        }

        public static final ChaveRegra PRAZO_DESPACHO_INICIAL = de("prazo.despacho.inicial_dias_uteis");
        public static final ChaveRegra PRAZO_DECISAO_INTERLOCUT = de("prazo.decisao.interlocutoria_dias_uteis");
        public static final ChaveRegra PRAZO_SENTENCA = de("prazo.sentenca.dias_uteis");
        public static final ChaveRegra PRAZO_CITACAO = de("prazo.citacao.dias_uteis");
        public static final ChaveRegra PRAZO_AUDIENCIA_UNA = de("prazo.audiencia.una_trabalhista_dias");
        public static final ChaveRegra DIST_LIMITE_JEC = de("distribuicao.valor.juizado_limite_reais");
        public static final ChaveRegra DIST_LIMITE_JEC_SALARIOS = de("distribuicao.valor.juizado_limite_salarios_minimos");
        public static final ChaveRegra DIST_LIMIAR_CONGESTION = de("distribuicao.redistribuicao.limiar_congestionamento");
        public static final ChaveRegra DIST_CAPACIDADE_MAXIMA = de("distribuicao.vara.capacidade_maxima_processos");
        public static final ChaveRegra TRIAGEM_PRAZO_ANALISE_H = de("triagem.admissibilidade.prazo_analise_horas");
        public static final ChaveRegra TRIAGEM_OAB_OBRIGATORIA = de("triagem.oab.validacao_obrigatoria");
        public static final ChaveRegra TRIAGEM_VALOR_MIN_CAUSA = de("triagem.valor.minimo_causa_reais");
        public static final ChaveRegra AUDIENCIA_CONCIL_OBRIG = de("audiencia.conciliacao.obrigatoria");
        public static final ChaveRegra AUDIENCIA_CONCIL_PRAZO = de("audiencia.conciliacao.prazo_designacao_dias");
        public static final ChaveRegra SIGILO_FAMILIA_AUTO = de("sigilo.familia.automatico");
        public static final ChaveRegra SIGILO_MENOR_AUTO = de("sigilo.menor.automatico");
        public static final ChaveRegra RECESSO_DEZ_INICIO_DIA = de("recesso.dezembro.inicio_dia");
        public static final ChaveRegra RECESSO_JAN_FIM_DIA = de("recesso.janeiro.fim_dia");
        public static final ChaveRegra NOTIF_CANAL_PADRAO = de("notificacao.canal.padrao");
        public static final ChaveRegra NOTIF_WHATSAPP_ATIVO = de("notificacao.whatsapp.ativo");
    }

    public record EntradaRegra(
            ChaveRegra chave,
            NivelRegra nivel,
            String escopoId,
            Object valor,
            TipoValor tipoValor,
            ModoSobrescrita modo,
            String fundamentacao,
            String descricao,
            boolean ativa,
            Instant vigenteDesde,
            Instant vigenteAte,
            String criadoPor,
            String versao,
            RamoDireito ramoAlvo,
            GrauJurisdicao grauAlvo
    ) {
        public EntradaRegra {
            Objects.requireNonNull(chave, "chave");
            Objects.requireNonNull(nivel, "nivel");
            escopoId = normalizeScope(escopoId);
            Objects.requireNonNull(tipoValor, "tipoValor");
            modo = modo == null ? ModoSobrescrita.SUBSTITUIR : modo;
            fundamentacao = blankToNull(fundamentacao);
            descricao = blankToNull(descricao);
            criadoPor = blankToNull(criadoPor);
            versao = blankToNull(versao) == null ? "1.0.0" : versao.trim();
        }

        public static EntradaRegra nacional(ChaveRegra chave, Object valor, TipoValor tipo, String fundamentacao, String descricao) {
            return new EntradaRegra(
                    chave,
                    NivelRegra.NACIONAL,
                    "BRASIL",
                    valor,
                    tipo,
                    ModoSobrescrita.SUBSTITUIR,
                    fundamentacao,
                    descricao,
                    true,
                    Instant.now(),
                    null,
                    "SISTEMA-SEED",
                    "2.0.0",
                    null,
                    null
            );
        }

        public boolean vigenteEm(Instant momento) {
            Instant base = momento == null ? Instant.now() : momento;
            if (!ativa) {
                return false;
            }
            if (vigenteDesde != null && base.isBefore(vigenteDesde)) {
                return false;
            }
            if (vigenteAte != null && base.isAfter(vigenteAte)) {
                return false;
            }
            return true;
        }

        public boolean expirada() {
            return vigenteAte != null && Instant.now().isAfter(vigenteAte);
        }

        public boolean aplicaAo(ContextoResolucao contexto) {
            if (contexto == null) {
                return true;
            }
            if (ramoAlvo != null && contexto.ramo() != null && ramoAlvo != contexto.ramo()) {
                return false;
            }
            if (ramoAlvo != null && contexto.ramo() == null) {
                return false;
            }
            if (grauAlvo != null && contexto.grau() != null && grauAlvo != contexto.grau()) {
                return false;
            }
            if (grauAlvo != null && contexto.grau() == null) {
                return false;
            }
            return true;
        }

        public int especificidadeContextual() {
            int score = 0;
            if (ramoAlvo != null) {
                score += 2;
            }
            if (grauAlvo != null) {
                score += 2;
            }
            return score;
        }
    }

    public record ContextoResolucao(
            String tribunalCodigo,
            String comarcaId,
            String varaId,
            RamoDireito ramo,
            GrauJurisdicao grau,
            Instant momento
    ) {
        public ContextoResolucao {
            tribunalCodigo = normalizeScope(tribunalCodigo);
            comarcaId = normalizeScope(comarcaId);
            varaId = normalizeScope(varaId);
            momento = momento == null ? Instant.now() : momento;
        }

        public static ContextoResolucao agora(String tribunal, String comarca, String vara) {
            return new ContextoResolucao(tribunal, comarca, vara, null, null, Instant.now());
        }

        public static ContextoResolucao agora(String tribunal, String comarca, String vara, RamoDireito ramo, GrauJurisdicao grau) {
            return new ContextoResolucao(tribunal, comarca, vara, ramo, grau, Instant.now());
        }

        public static ContextoResolucao fromNationalContexto(NationalRulePackEngine.ContextoRegra ctx, String comarcaId, String varaId) {
            Objects.requireNonNull(ctx, "ctx");
            return new ContextoResolucao(
                    ctx.tribunalCodigo(),
                    comarcaId,
                    varaId,
                    ctx.ramo(),
                    ctx.grau(),
                    Instant.now()
            );
        }
    }

    private final NationalRulePackEngine nationalRulePackEngine;
    private final NationalPrazoEngine nationalPrazoEngine;
    private final SalarioMinimoNacionalService salarioMinimoNacionalService;
    private final TribunalRuleResolutionSupport resolutionSupport;
    private final TribunalRulePackSynchronizationSupport rulePackSynchronizationSupport;
    private final TribunalRuleAnalyticsSupport analyticsSupport;

    private final Map<String, EnumMap<NivelRegra, Map<String, CopyOnWriteArrayList<EntradaRegra>>>> repositorio = new ConcurrentHashMap<>();
    private final List<RegraResolvida> logAuditoria = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, List<EntradaRegra>> pluginsRegistrados = new ConcurrentHashMap<>();
    private final Map<String, Instant> pluginTouch = new ConcurrentHashMap<>();
    private final ReentrantLock pluginMutationLock = new ReentrantLock();
    private final Map<String, Set<RulePackSyncBucket>> rulePackBucketsSincronizados = new ConcurrentHashMap<>();
    private final Map<String, Instant> syncBucketTouch = new ConcurrentHashMap<>();
    private static final Set<String> CHAVES_ADAPTAVEIS_RULE_PACK = Set.of(
            ChaveRegra.PRAZO_DESPACHO_INICIAL.canonical(),
            ChaveRegra.PRAZO_DECISAO_INTERLOCUT.canonical(),
            ChaveRegra.PRAZO_SENTENCA.canonical(),
            ChaveRegra.PRAZO_CITACAO.canonical(),
            ChaveRegra.PRAZO_AUDIENCIA_UNA.canonical(),
            ChaveRegra.TRIAGEM_PRAZO_ANALISE_H.canonical(),
            ChaveRegra.TRIAGEM_OAB_OBRIGATORIA.canonical(),
            ChaveRegra.TRIAGEM_VALOR_MIN_CAUSA.canonical(),
            ChaveRegra.AUDIENCIA_CONCIL_OBRIG.canonical(),
            ChaveRegra.AUDIENCIA_CONCIL_PRAZO.canonical()
    );
    private static final int MAX_LOG = 10_000;
    private static final int MAX_REGISTERED_PLUGINS = 256;
    private static final int MAX_SYNCED_TRIBUNALS = 128;

    public TribunalRuleEngine(NationalRulePackEngine nationalRulePackEngine,
                              NationalPrazoEngine nationalPrazoEngine,
                              SalarioMinimoNacionalService salarioMinimoNacionalService,
                              TribunalRuleResolutionSupport resolutionSupport,
                              TribunalRulePackSynchronizationSupport rulePackSynchronizationSupport) {
        this.nationalRulePackEngine = Objects.requireNonNull(nationalRulePackEngine);
        this.nationalPrazoEngine = Objects.requireNonNull(nationalPrazoEngine);
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
        this.resolutionSupport = Objects.requireNonNull(resolutionSupport);
        this.rulePackSynchronizationSupport = Objects.requireNonNull(rulePackSynchronizationSupport);
        this.analyticsSupport = new TribunalRuleAnalyticsSupport();
    }

    @PostConstruct
    public void inicializar() {
        seedRegrasPadraoNacional();
        sincronizarRulePackAdaptadoTodosTribunais();
    }

    public void cadastrar(EntradaRegra regra) {
        Objects.requireNonNull(regra, "regra");
        repositorio
                .computeIfAbsent(regra.chave().canonical(), key -> new EnumMap<>(NivelRegra.class))
                .computeIfAbsent(regra.nivel(), key -> new ConcurrentHashMap<>())
                .computeIfAbsent(escopoKey(regra.nivel(), regra.escopoId()), key -> new CopyOnWriteArrayList<>());

        CopyOnWriteArrayList<EntradaRegra> lista = repositorio.get(regra.chave().canonical())
                .get(regra.nivel())
                .get(escopoKey(regra.nivel(), regra.escopoId()));

        lista.removeIf(existente ->
                Objects.equals(existente.chave().canonical(), regra.chave().canonical())
                        && existente.nivel() == regra.nivel()
                        && Objects.equals(existente.escopoId(), regra.escopoId())
                        && existente.ramoAlvo() == regra.ramoAlvo()
                        && existente.grauAlvo() == regra.grauAlvo());

        lista.add(regra);
        lista.sort(Comparator
                .comparingInt(EntradaRegra::especificidadeContextual).reversed()
                .thenComparing(e -> e.vigenteDesde() == null ? Instant.EPOCH : e.vigenteDesde(), Comparator.reverseOrder()));
    }

    public void substituirRegrasPlugin(String pluginKey, Collection<EntradaRegra> regras) {
        pluginMutationLock.lock();
        try {
            String key = normalizePluginKey(pluginKey);
            removerRegrasPluginInterno(key);
            List<EntradaRegra> lista = regras == null ? List.of() : regras.stream().filter(Objects::nonNull).toList();
            for (EntradaRegra regra : lista) {
                cadastrar(regra);
            }
            pluginsRegistrados.put(key, List.copyOf(lista));
            pluginTouch.put(key, Instant.now());
            trimPluginRegistry();
        } finally {
            pluginMutationLock.unlock();
        }
    }

    public void removerRegrasPlugin(String pluginKey) {
        pluginMutationLock.lock();
        try {
            removerRegrasPluginInterno(pluginKey);
        } finally {
            pluginMutationLock.unlock();
        }
    }

    private void removerRegrasPluginInterno(String pluginKey) {
        String key = normalizePluginKey(pluginKey);
        pluginTouch.remove(key);
        List<EntradaRegra> anteriores = pluginsRegistrados.remove(key);
        if (anteriores == null || anteriores.isEmpty()) {
            return;
        }
        for (EntradaRegra regra : anteriores) {
            removerRegistroInterno(regra);
        }
    }

    public List<EntradaRegra> listarRegrasPlugin(String pluginKey) {
        String key = normalizePluginKey(pluginKey);
        pluginTouch.put(key, Instant.now());
        return List.copyOf(pluginsRegistrados.getOrDefault(key, List.of()));
    }

    public void cadastrarNacional(ChaveRegra chave, Object valor, TipoValor tipo, String fundamentacao, String descricao) {
        cadastrar(EntradaRegra.nacional(chave, valor, tipo, fundamentacao, descricao));
    }

    public void cadastrarNacional(ChaveRegra chave, Object valor, TipoValor tipo, String fundamentacao, String descricao, RamoDireito ramo, GrauJurisdicao grau) {
        cadastrar(new EntradaRegra(
                chave,
                NivelRegra.NACIONAL,
                "BRASIL",
                valor,
                tipo,
                ModoSobrescrita.SUBSTITUIR,
                fundamentacao,
                descricao,
                true,
                Instant.now(),
                null,
                "SISTEMA-SEED",
                "2.0.0",
                ramo,
                grau
        ));
    }

    public void cadastrarParaTribunal(String codigo, ChaveRegra chave, Object valor, TipoValor tipo, String fundamentacao, String descricao) {
        cadastrarParaTribunal(codigo, chave, valor, tipo, ModoSobrescrita.SUBSTITUIR, fundamentacao, descricao, null, null);
    }

    public void cadastrarParaTribunal(String codigo, ChaveRegra chave, Object valor, TipoValor tipo, ModoSobrescrita modo,
                                      String fundamentacao, String descricao, RamoDireito ramo, GrauJurisdicao grau) {
        cadastrar(new EntradaRegra(
                chave,
                NivelRegra.TRIBUNAL,
                codigo,
                valor,
                tipo,
                modo,
                fundamentacao,
                descricao,
                true,
                Instant.now(),
                null,
                codigo,
                "1.0.0",
                ramo,
                grau
        ));
    }

    public void cadastrarParaComarca(String comarcaId, ChaveRegra chave, Object valor, TipoValor tipo, String fundamentacao, String descricao) {
        cadastrarParaComarca(comarcaId, chave, valor, tipo, ModoSobrescrita.SUBSTITUIR, fundamentacao, descricao, null, null);
    }

    public void cadastrarParaComarca(String comarcaId, ChaveRegra chave, Object valor, TipoValor tipo, ModoSobrescrita modo,
                                     String fundamentacao, String descricao, RamoDireito ramo, GrauJurisdicao grau) {
        cadastrar(new EntradaRegra(
                chave,
                NivelRegra.COMARCA,
                comarcaId,
                valor,
                tipo,
                modo,
                fundamentacao,
                descricao,
                true,
                Instant.now(),
                null,
                comarcaId,
                "1.0.0",
                ramo,
                grau
        ));
    }

    public void cadastrarParaVara(String varaId, ChaveRegra chave, Object valor, TipoValor tipo, ModoSobrescrita modo, String fundamentacao, String descricao) {
        cadastrarParaVara(varaId, chave, valor, tipo, modo, fundamentacao, descricao, null, null);
    }

    public void cadastrarParaVara(String varaId, ChaveRegra chave, Object valor, TipoValor tipo, ModoSobrescrita modo,
                                  String fundamentacao, String descricao, RamoDireito ramo, GrauJurisdicao grau) {
        cadastrar(new EntradaRegra(
                chave,
                NivelRegra.VARA,
                varaId,
                valor,
                tipo,
                modo,
                fundamentacao,
                descricao,
                true,
                Instant.now(),
                null,
                varaId,
                "1.0.0",
                ramo,
                grau
        ));
    }

    public Optional<RegraResolvida> resolver(ChaveRegra chave, ContextoResolucao contexto) {
        return resolutionSupport.resolver(chave, contexto, repositorio, logAuditoria);
    }

    public RegraResolvida resolverOuDefault(ChaveRegra chave, ContextoResolucao contexto, Object valorDefault, TipoValor tipo) {
        return resolutionSupport.resolverOuDefault(chave, contexto, valorDefault, tipo, repositorio, logAuditoria);
    }

    public int resolverPrazoDias(ChaveRegra chave, ContextoResolucao contexto, int defaultDias) {
        return resolutionSupport.resolverPrazoDias(chave, contexto, defaultDias, repositorio, logAuditoria);
    }

    public BigDecimal resolverDecimal(ChaveRegra chave, ContextoResolucao contexto, BigDecimal defaultValue) {
        return resolutionSupport.resolverDecimal(chave, contexto, defaultValue, repositorio, logAuditoria);
    }

    public boolean resolverBooleano(ChaveRegra chave, ContextoResolucao contexto, boolean defaultValue) {
        return resolutionSupport.resolverBooleano(chave, contexto, defaultValue, repositorio, logAuditoria);
    }

    public String resolverTexto(ChaveRegra chave, ContextoResolucao contexto, String defaultValue) {
        return resolutionSupport.resolverTexto(chave, contexto, defaultValue, repositorio, logAuditoria);
    }

    public Map<String, RegraResolvida> resolverBatch(List<ChaveRegra> chaves, ContextoResolucao contexto) {
        return resolutionSupport.resolverBatch(chaves, contexto, repositorio, logAuditoria);
    }

    public Map<String, Integer> resolverTodosPrazos(ContextoResolucao contexto) {
        return resolutionSupport.resolverTodosPrazos(contexto, repositorio, logAuditoria);
    }

    public SnapshotDistribuicao resolverSnapshotDistribuicao(ContextoResolucao contexto) {
        return resolutionSupport.resolverSnapshotDistribuicao(contexto, repositorio, logAuditoria);
    }

    public SnapshotTriagem resolverSnapshotTriagem(ContextoResolucao contexto) {
        return resolutionSupport.resolverSnapshotTriagem(contexto, repositorio, logAuditoria);
    }

    public SnapshotPrazo resolverSnapshotPrazo(ContextoResolucao contexto) {
        return resolutionSupport.resolverSnapshotPrazo(contexto, repositorio, logAuditoria);
    }

    public BigDecimal resolverLimiarCongestionamento(ContextoResolucao contexto, BigDecimal fallback) {
        return resolutionSupport.resolverLimiarCongestionamento(contexto, fallback, repositorio, logAuditoria);
    }

    public int resolverCapacidadeMaximaVara(ContextoResolucao contexto, int fallback) {
        return resolutionSupport.resolverCapacidadeMaximaVara(contexto, fallback, repositorio, logAuditoria);
    }

    public BigDecimal resolverLimiteJuizadoEmReais(ContextoResolucao contexto) {
        return resolutionSupport.resolverLimiteJuizadoEmReais(contexto, repositorio, logAuditoria);
    }

    public Map<String, Object> resolverExtrasIntegracao(ContextoResolucao contexto) {
        return resolutionSupport.resolverExtrasIntegracao(contexto, repositorio, logAuditoria);
    }

    public NationalRulePackEngine.ContextoRegra enriquecerContextoNationalRulePack(NationalRulePackEngine.ContextoRegra contexto,
                                                                                   String comarcaId,
                                                                                   String varaId) {
        return resolutionSupport.enriquecerContextoNationalRulePack(contexto, comarcaId, varaId, repositorio, logAuditoria);
    }

    public NationalRulePackEngine.ResultadoRegras aplicarRegraPackEnriquecido(NationalRulePackEngine.ContextoRegra contexto,
                                                                              String comarcaId,
                                                                              String varaId) {
        return resolutionSupport.aplicarRegraPackEnriquecido(contexto, comarcaId, varaId, repositorio, logAuditoria);
    }

    public void sincronizarConfiguracaoPrazo(ContextoResolucao contexto,
                                             Set<LocalDate> feriadosAdicionais,
                                             boolean contarSabado,
                                             boolean integralmenteCorrido) {
        resolutionSupport.sincronizarConfiguracaoPrazo(contexto, feriadosAdicionais, contarSabado, integralmenteCorrido);
    }

    public Set<String> listarTribunaisComRegrasCustomizadas() {
        return rulePackSynchronizationSupport.listarTribunaisComRegrasCustomizadas(listarTodas());
    }

    public void sincronizarRulePackAdaptadoTodosTribunais() {
        pluginMutationLock.lock();
        try {
            for (String tribunalCodigo : listarTribunaisComRegrasCustomizadas()) {
                rulePackSynchronizationSupport.sincronizarRulePackAdaptadoTribunalInterno(
                        tribunalCodigo,
                        repositorio,
                        logAuditoria,
                        rulePackBucketsSincronizados,
                        syncBucketTouch,
                        MAX_SYNCED_TRIBUNALS,
                        CHAVES_ADAPTAVEIS_RULE_PACK,
                        listarTodas());
            }
        } finally {
            pluginMutationLock.unlock();
        }
    }

    public void sincronizarRulePackAdaptadoTribunal(String tribunalCodigo) {
        pluginMutationLock.lock();
        try {
            rulePackSynchronizationSupport.sincronizarRulePackAdaptadoTribunalInterno(
                    tribunalCodigo,
                    repositorio,
                    logAuditoria,
                    rulePackBucketsSincronizados,
                    syncBucketTouch,
                    MAX_SYNCED_TRIBUNALS,
                    CHAVES_ADAPTAVEIS_RULE_PACK,
                    listarTodas());
        } finally {
            pluginMutationLock.unlock();
        }
    }

    void trimPluginRegistry() {
        if (pluginTouch.size() <= MAX_REGISTERED_PLUGINS) {
            return;
        }
        int removeCount = pluginTouch.size() - MAX_REGISTERED_PLUGINS;
        pluginTouch.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(removeCount)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(this::removerRegrasPlugin);
    }

    public List<AnaliseDesvio> analisarDesviosTribunal(String tribunalCodigo) {
        return analyticsSupport.analisarDesviosTribunal(tribunalCodigo, null, null, repositorio, logAuditoria, resolutionSupport);
    }

    public List<AnaliseDesvio> analisarDesviosTribunal(String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau) {
        return analyticsSupport.analisarDesviosTribunal(tribunalCodigo, ramo, grau, repositorio, logAuditoria, resolutionSupport);
    }

    public RelatorioCoberturaTribunal relatorioCobertura(String tribunalCodigo) {
        return analyticsSupport.relatorioCobertura(tribunalCodigo, repositorio);
    }

    public List<RegraResolvida> consultarLog(String filtroEscopoOuTribunal, int limite) {
        return analyticsSupport.consultarLog(filtroEscopoOuTribunal, limite, logAuditoria);
    }

    public long totalRegrasFallback(String tribunalCodigo) {
        return analyticsSupport.totalRegrasFallback(tribunalCodigo, logAuditoria);
    }

    public List<EntradaRegra> listarPorTribunal(String tribunalCodigo) {
        return analyticsSupport.listarPorTribunal(tribunalCodigo, listarTodas());
    }

    public List<EntradaRegra> listarExpiradas() {
        return analyticsSupport.listarExpiradas(listarTodas());
    }

    public int totalRegrasAtivas() {
        return analyticsSupport.totalRegrasAtivas(listarTodas());
    }

    public void seedRegrasPadraoNacional() {
        cadastrarNacional(ChaveRegra.PRAZO_DESPACHO_INICIAL, 2, TipoValor.DURACAO_DIAS, "CPC art. 226, I", "Despachos simples: 2 dias úteis");
        cadastrarNacional(ChaveRegra.PRAZO_DECISAO_INTERLOCUT, 10, TipoValor.DURACAO_DIAS, "CPC art. 226, II", "Decisões interlocutórias: 10 dias úteis");
        cadastrarNacional(ChaveRegra.PRAZO_SENTENCA, 30, TipoValor.DURACAO_DIAS, "CPC art. 226, III", "Sentença: 30 dias úteis");
        cadastrarNacional(ChaveRegra.PRAZO_CITACAO, 2, TipoValor.DURACAO_DIAS, "CPC art. 238", "Expedir citação: 2 dias úteis");
        cadastrarNacional(ChaveRegra.PRAZO_AUDIENCIA_UNA, 0, TipoValor.DURACAO_DIAS, "CLT art. 848", "Audiência una trabalhista: conforme pauta do juízo");

        cadastrarNacional(ChaveRegra.DIST_LIMITE_JEC_SALARIOS, new BigDecimal("40"), TipoValor.DECIMAL, "Lei 9.099/95 art. 3º, I", "Limite JEC em salários mínimos");
        cadastrarNacional(ChaveRegra.DIST_LIMITE_JEC, resolverLimiteJuizadoEmReais(new ContextoResolucao("BRASIL", null, null, null, null, Instant.now())), TipoValor.DECIMAL, "Lei 9.099/95 art. 3º, I", "Limite JEC em reais calculado a partir do salário mínimo vigente");
        cadastrarNacional(ChaveRegra.DIST_LIMIAR_CONGESTION, new BigDecimal("0.85"), TipoValor.DECIMAL, "Res. CNJ 219/2016", "Redistribuição a partir de 85% de ocupação");
        cadastrarNacional(ChaveRegra.DIST_CAPACIDADE_MAXIMA, 3000, TipoValor.INTEIRO, "Res. CNJ 219/2016", "Capacidade padrão por vara");

        cadastrarNacional(ChaveRegra.TRIAGEM_PRAZO_ANALISE_H, 48, TipoValor.INTEIRO, "Parâmetro nacional PJB", "Triagem inicial em até 48 horas");
        cadastrarNacional(ChaveRegra.TRIAGEM_OAB_OBRIGATORIA, true, TipoValor.BOOLEANO, "Lei 8.906/94 art. 1º", "Validação OAB obrigatória na triagem");
        cadastrarNacional(ChaveRegra.TRIAGEM_VALOR_MIN_CAUSA, new BigDecimal("1.00"), TipoValor.DECIMAL, "CPC art. 292", "Valor mínimo da causa");

        cadastrarNacional(ChaveRegra.AUDIENCIA_CONCIL_OBRIG, true, TipoValor.BOOLEANO, "CPC art. 334", "Conciliação obrigatória");
        cadastrarNacional(ChaveRegra.AUDIENCIA_CONCIL_PRAZO, 30, TipoValor.DURACAO_DIAS, "CPC art. 334", "Prazo para designação da audiência");

        cadastrarNacional(ChaveRegra.SIGILO_FAMILIA_AUTO, true, TipoValor.BOOLEANO, "CPC art. 189, II", "Sigilo automático em família", RamoDireito.FAMILIA, null);
        cadastrarNacional(ChaveRegra.SIGILO_MENOR_AUTO, true, TipoValor.BOOLEANO, "ECA art. 143", "Sigilo automático em casos com menor");

        cadastrarNacional(ChaveRegra.RECESSO_DEZ_INICIO_DIA, 20, TipoValor.INTEIRO, "Lei 5.010/66 art. 62", "Início do recesso de dezembro");
        cadastrarNacional(ChaveRegra.RECESSO_JAN_FIM_DIA, 6, TipoValor.INTEIRO, "Lei 5.010/66 art. 62", "Fim do recesso de janeiro");

        cadastrarNacional(ChaveRegra.NOTIF_CANAL_PADRAO, "EMAIL", TipoValor.TEXTO, "Lei 11.419/2006", "Canal padrão de notificação");
        cadastrarNacional(ChaveRegra.NOTIF_WHATSAPP_ATIVO, false, TipoValor.BOOLEANO, "Res. CNJ 455/2022", "WhatsApp desativado por padrão");

        cadastrarParaTribunal("TJAM", ChaveRegra.PRAZO_DESPACHO_INICIAL, 5, TipoValor.DURACAO_DIAS, ModoSobrescrita.SUBSTITUIR,
                "Res. TJAM 003/2023 art. 4º", "TJAM interior: despacho em 5 dias", null, GrauJurisdicao.PRIMEIRO_GRAU);

        cadastrarParaTribunal("TST", ChaveRegra.PRAZO_AUDIENCIA_UNA, 10, TipoValor.DURACAO_DIAS, ModoSobrescrita.SUBSTITUIR,
                "CLT art. 848 c/c IN TST 41/2018", "TST: audiência una em até 10 dias", RamoDireito.TRABALHISTA, null);

        cadastrarParaTribunal("TJSP", ChaveRegra.NOTIF_WHATSAPP_ATIVO, true, TipoValor.BOOLEANO, ModoSobrescrita.SUBSTITUIR,
                "Res. TJSP 772/2024", "TJSP: WhatsApp habilitado", null, null);

        cadastrarParaTribunal("TRF3", ChaveRegra.DIST_CAPACIDADE_MAXIMA, 4500, TipoValor.INTEIRO, ModoSobrescrita.SUBSTITUIR,
                "Portaria TRF3 Presi 12/2024", "TRF3: capacidade ampliada", null, GrauJurisdicao.PRIMEIRO_GRAU);
    }

    List<EntradaRegra> listarTodas() {
        return repositorio.values().stream()
                .flatMap(mapaNivel -> mapaNivel.values().stream())
                .flatMap(mapaEscopo -> mapaEscopo.values().stream())
                .flatMap(List::stream)
                .toList();
    }

    static boolean containsEscopo(Map<NivelRegra, Map<String, CopyOnWriteArrayList<EntradaRegra>>> porNivel,
                                   NivelRegra nivel,
                                   String escopo) {
        Map<String, CopyOnWriteArrayList<EntradaRegra>> porEscopo = porNivel.get(nivel);
        if (porEscopo == null) {
            return false;
        }
        CopyOnWriteArrayList<EntradaRegra> lista = porEscopo.get(escopoKey(nivel, escopo));
        return lista != null && !lista.isEmpty();
    }

    static NivelRegra nivelMaisEspecificoInformado(ContextoResolucao contexto) {
        if (contexto == null) {
            return NivelRegra.NACIONAL;
        }
        if (contexto.varaId() != null) {
            return NivelRegra.VARA;
        }
        if (contexto.comarcaId() != null) {
            return NivelRegra.COMARCA;
        }
        if (contexto.tribunalCodigo() != null) {
            return NivelRegra.TRIBUNAL;
        }
        return NivelRegra.NACIONAL;
    }

    static Object normalizarValorPorTipo(Object valor, TipoValor tipoValor) {
        if (valor == null) {
            return null;
        }
        return switch (tipoValor) {
            case TEXTO -> String.valueOf(valor).trim();
            case INTEIRO, DURACAO_DIAS -> {
                if (valor instanceof Number n) {
                    yield n.intValue();
                }
                yield Integer.parseInt(String.valueOf(valor).replaceAll("[^0-9\\-]", "").trim());
            }
            case DECIMAL -> toBigDecimal(valor).setScale(2, RoundingMode.HALF_UP);
            case BOOLEANO -> toBoolean(valor);
            case LISTA_TEXTO -> List.copyOf(toStringList(valor));
        };
    }

    static List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .distinct()
                    .toList();
        }
        if (value instanceof String s) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .distinct()
                    .toList();
        }
        return List.of(String.valueOf(value));
    }

    static BigDecimal toBigDecimal(Object valor) {
        if (valor instanceof BigDecimal bd) {
            return bd;
        }
        if (valor instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(normalizeDecimalString(String.valueOf(valor)));
    }

    static boolean toBoolean(Object valor) {
        if (valor instanceof Boolean b) {
            return b;
        }
        if (valor instanceof Number n) {
            return n.intValue() != 0;
        }
        String token = normalizeToken(String.valueOf(valor));
        return Set.of("TRUE", "SIM", "YES", "Y", "1", "VERDADEIRO", "ATIVO").contains(token);
    }

    static String normalizeDecimalString(String value) {
        String cleaned = value == null ? "0" : value.trim();
        if (cleaned.isBlank()) {
            return "0";
        }
        int lastDot = cleaned.lastIndexOf('.');
        int lastComma = cleaned.lastIndexOf(',');
        if (lastDot >= 0 && lastComma >= 0) {
            if (lastComma > lastDot) {
                return cleaned.replace(".", "").replace(",", ".");
            }
            return cleaned.replace(",", "");
        }
        if (lastComma >= 0) {
            return cleaned.replace(",", ".");
        }
        return cleaned;
    }

    static double calcularPercentDesvio(Object base, Object novo) {
        try {
            double b = Double.parseDouble(String.valueOf(base).replace(",", "."));
            double n = Double.parseDouble(String.valueOf(novo).replace(",", "."));
            if (b == 0d) {
                return n == 0d ? 0d : 100d;
            }
            return Math.abs((n - b) / b) * 100d;
        } catch (Exception e) {
            return 0d;
        }
    }

    static String escopoKey(NivelRegra nivel, String escopoId) {
        return nivel.name() + "::" + normalizeScope(escopoId);
    }

    void removerRegistroInterno(EntradaRegra regra) {
        Map<NivelRegra, Map<String, CopyOnWriteArrayList<EntradaRegra>>> porNivel = repositorio.get(regra.chave().canonical());
        if (porNivel == null) {
            return;
        }
        Map<String, CopyOnWriteArrayList<EntradaRegra>> porEscopo = porNivel.get(regra.nivel());
        if (porEscopo == null) {
            return;
        }
        String chaveEscopo = escopoKey(regra.nivel(), regra.escopoId());
        CopyOnWriteArrayList<EntradaRegra> lista = porEscopo.get(chaveEscopo);
        if (lista == null) {
            return;
        }
        lista.removeIf(existente ->
                Objects.equals(existente.chave().canonical(), regra.chave().canonical())
                        && existente.nivel() == regra.nivel()
                        && Objects.equals(existente.escopoId(), regra.escopoId())
                        && existente.ramoAlvo() == regra.ramoAlvo()
                        && existente.grauAlvo() == regra.grauAlvo());
        if (lista.isEmpty()) {
            porEscopo.remove(chaveEscopo);
        }
        if (porEscopo.isEmpty()) {
            porNivel.remove(regra.nivel());
        }
        if (porNivel.isEmpty()) {
            repositorio.remove(regra.chave().canonical());
        }
    }

    private String normalizePluginKey(String pluginKey) {
        if (pluginKey == null || pluginKey.isBlank()) {
            throw new IllegalArgumentException("pluginKey obrigatório");
        }
        return pluginKey.trim().toUpperCase(Locale.ROOT);
    }

    static String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Valor obrigatório ausente: " + field);
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeScope(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static String fundamentoSuffix(String fundamento) {
        return fundamento == null || fundamento.isBlank() ? "" : " | " + fundamento.trim();
    }

    static String valuePreview(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
        }
        return String.valueOf(value);
    }

    static double round2(double value) {
        return new BigDecimal(Double.toString(value)).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
