package com.tcc.pjb.backend.tribunal.regras;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoEventoRequest;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;
import com.tcc.pjb.backend.tribunal.perfil.PerfilInstanciaTribunalService;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.tcc.pjb.backend.tribunal.regras.plugin.BucketRegraPack;
import com.tcc.pjb.backend.tribunal.regras.plugin.PluginRegistrado;
import com.tcc.pjb.backend.tribunal.regras.plugin.PluginSnapshot;
import com.tcc.pjb.backend.tribunal.regras.plugin.ResultadoCarga;
import com.tcc.pjb.backend.tribunal.regras.plugin.ResumoPlugins;
import com.tcc.pjb.backend.tribunal.regras.plugin.StatusPlugin;
import com.tcc.pjb.backend.tribunal.regras.plugin.TipoPlugin;
import com.tcc.pjb.backend.tribunal.regras.spec.CalendarioEntrySpec;
import com.tcc.pjb.backend.tribunal.regras.spec.CalendarioRecessoSpec;
import com.tcc.pjb.backend.tribunal.regras.spec.PluginManifest;
import com.tcc.pjb.backend.tribunal.regras.spec.RulePackSpec;
import com.tcc.pjb.backend.tribunal.regras.spec.TribunalRuleSpec;

@Service
public class PluginResolucaoTribunalService {

    private static final int MAX_HISTORICO_CARGAS = 1000;

    public static final String EVT_TRIBUNAL_PLUGIN_ATUALIZADO = "pjb.tribunal.plugin.atualizado";
    public static final String EVT_TRIBUNAL_PLUGIN_REMOVIDO = "pjb.tribunal.plugin.removido";
    public static final String CLASSPATH_PATTERN = "classpath*:/tribunais/plugins*.json";
    private static final int MAX_TRIBUNAL_RULES = 1000;
    private static final int MAX_RULEPACK_RULES = 1000;
    private static final int MAX_CALENDARIO_ENTRIES = 5000;
    private static final int MAX_RECESSO_PERIODS = 500;
    private static final int MAX_DEPENDENCIAS = 50;
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^A-Z0-9]+") ;

    private final TribunalRuleEngine tribunalRuleEngine;
    private final NationalRulePackEngine nationalRulePackEngine;
    private final NationalPrazoEngine nationalPrazoEngine;
    private final CalendarioForenseTribunalService calendarioForenseTribunalService;
    private final PerfilInstanciaTribunalService perfilInstanciaTribunalService;
    private final FederalismoJudicialEngine federalismoJudicialEngine;
    private final OutboxPublisher outboxPublisher;
    private final PluginResolucaoTribunalManifestSupport manifestSupport;
    private final ResourcePatternResolver resourcePatternResolver;

    private static final int MAX_IDS_POR_TRIBUNAL = 256;

    private final Map<String, PluginSnapshot> pluginsAtivos = new ConcurrentHashMap<>();
    private final Map<String, PluginRegistrado> registrosPorKey = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<String>> idsPorTribunal = new ConcurrentHashMap<>();
    private final List<ResultadoCarga> historicoCargas = new CopyOnWriteArrayList<>();

    public PluginResolucaoTribunalService(TribunalRuleEngine tribunalRuleEngine,
                                          NationalRulePackEngine nationalRulePackEngine,
                                          NationalPrazoEngine nationalPrazoEngine,
                                          CalendarioForenseTribunalService calendarioForenseTribunalService,
                                          PerfilInstanciaTribunalService perfilInstanciaTribunalService,
                                          FederalismoJudicialEngine federalismoJudicialEngine,
                                          OutboxPublisher outboxPublisher,
                                          PluginResolucaoTribunalManifestSupport manifestSupport,
                                          ResourcePatternResolver resourcePatternResolver) {
        this.tribunalRuleEngine = Objects.requireNonNull(tribunalRuleEngine);
        this.nationalRulePackEngine = Objects.requireNonNull(nationalRulePackEngine);
        this.nationalPrazoEngine = Objects.requireNonNull(nationalPrazoEngine);
        this.calendarioForenseTribunalService = Objects.requireNonNull(calendarioForenseTribunalService);
        this.perfilInstanciaTribunalService = Objects.requireNonNull(perfilInstanciaTribunalService);
        this.federalismoJudicialEngine = Objects.requireNonNull(federalismoJudicialEngine);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.manifestSupport = Objects.requireNonNull(manifestSupport);
        this.resourcePatternResolver = Objects.requireNonNull(resourcePatternResolver);
    }

    @PostConstruct
    public void bootstrapClasspath() {
        recarregarClasspath();
    }

    @Transactional
    public List<PluginSnapshot> recarregarClasspath() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(CLASSPATH_PATTERN);
            Arrays.sort(resources, Comparator.comparing(PluginResolucaoTribunalService::resourceDescription));
            LinkedHashSet<String> origensAtuais = new LinkedHashSet<>();
            List<PluginSnapshot> snapshots = new ArrayList<>();
            for (Resource resource : resources) {
                String origem = resourceDescription(resource);
                origensAtuais.add(origem);
                byte[] bytes = resource.getInputStream().readAllBytes();
                snapshots.add(carregarJson(new String(bytes, StandardCharsets.UTF_8), origem, false));
            }
            List<PluginRegistrado> removidosDoClasspath = registrosPorKey.values().stream()
                    .filter(PluginRegistrado::ativo)
                    .filter(item -> item.snapshot() != null)
                    .filter(item -> isClasspathOrigin(item.snapshot().origem()))
                    .filter(item -> !origensAtuais.contains(item.snapshot().origem()))
                    .toList();
            for (PluginRegistrado removido : removidosDoClasspath) {
                desativarInterno(removido, StatusPlugin.DESATIVADO, "Plugin ausente no classpath após recarga", "SYSTEM-CLASSPATH-RELOAD", false);
            }
            return List.copyOf(snapshots);
        } catch (Exception ex) {
            throw new IllegalStateException("plugin_tribunal_classpath_reload", ex);
        }
    }

    @Transactional
    public PluginSnapshot carregarJson(String json, String origem, boolean federar) {
        String payload = requireText(json, "json");
        PluginManifest manifest = manifestSupport.readManifest(payload);
        LoadPlan plan = montarPlano(payload, manifest, origem, federar);
        ResultadoCarga resultado = aplicarPlano(plan, payload);
        registrarHistoricoCarga(resultado);
        PluginRegistrado registrado = registrosPorKey.get(plan.pluginKey());
        if (registrado == null || registrado.snapshot() == null) {
            throw new IllegalStateException("plugin_tribunal_nao_registrado");
        }
        return registrado.snapshot();
    }

    @Transactional
    public Optional<PluginSnapshot> removerPlugin(String tribunalCodigo, String pluginId, boolean federar) {
        String key = pluginKey(tribunalCodigo, pluginId);
        PluginRegistrado registrado = registrosPorKey.get(key);
        if (registrado == null || registrado.snapshot() == null) {
            return Optional.empty();
        }
        PluginSnapshot anterior = registrado.snapshot();
        desativarInterno(registrado, StatusPlugin.DESATIVADO, "Removido", "SYSTEM", federar || registrado.snapshot().federado());
        PluginRegistrado atualizado = registrosPorKey.get(key);
        return Optional.ofNullable(atualizado == null ? anterior : atualizado.snapshot());
    }

    @Transactional
    public boolean desativar(String pluginId, String motivo, String operador) {
        List<PluginRegistrado> alvos = registrosPorKey.values().stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(item.pluginId(), normalizePluginId(pluginId)) || Objects.equals(item.pluginKey(), normalizePluginKey(pluginId)))
                .filter(PluginRegistrado::ativo)
                .toList();
        boolean mudou = false;
        for (PluginRegistrado alvo : alvos) {
            desativarInterno(alvo, StatusPlugin.DESATIVADO, blankToDefault(motivo, "Desativado"), blankToDefault(operador, "SYSTEM"), alvo.snapshot().federado());
            mudou = true;
        }
        return mudou;
    }

    @Transactional(readOnly = true)
    public List<PluginSnapshot> listarAtivos() {
        return pluginsAtivos.values().stream()
                .filter(PluginSnapshot::ativo)
                .sorted(Comparator.comparing(PluginSnapshot::tribunalCodigo).thenComparing(PluginSnapshot::pluginId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PluginSnapshot> buscarPlugin(String tribunalCodigo, String pluginId) {
        return Optional.ofNullable(pluginsAtivos.get(pluginKey(tribunalCodigo, pluginId)));
    }

    @Transactional(readOnly = true)
    public List<PluginRegistrado> pluginsAtivos(String tribunalCodigo) {
        String tribunal = normalizeUpper(requireText(tribunalCodigo, "tribunalCodigo"));
        return registrosPorKey.values().stream()
                .filter(PluginRegistrado::ativo)
                .filter(item -> Objects.equals(item.tribunalCodigo(), tribunal))
                .sorted(Comparator.comparing(PluginRegistrado::pluginId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PluginRegistrado> todosAtivos() {
        return registrosPorKey.values().stream()
                .filter(PluginRegistrado::ativo)
                .sorted(Comparator.comparing(PluginRegistrado::tribunalCodigo).thenComparing(PluginRegistrado::pluginId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PluginRegistrado> buscarRegistro(String tribunalCodigo, String pluginId) {
        return Optional.ofNullable(registrosPorKey.get(pluginKey(tribunalCodigo, pluginId)));
    }

    @Transactional(readOnly = true)
    public ResumoPlugins resumo() {
        Map<String, Long> porTribunal = registrosPorKey.values().stream()
                .filter(PluginRegistrado::ativo)
                .collect(Collectors.groupingBy(PluginRegistrado::tribunalCodigo, Collectors.counting()));
        long ativos = registrosPorKey.values().stream().filter(PluginRegistrado::ativo).count();
        long comErro = registrosPorKey.values().stream().filter(item -> item.snapshot() != null && !item.snapshot().erros().isEmpty()).count();
        return new ResumoPlugins(registrosPorKey.size(), ativos, comErro, Map.copyOf(porTribunal), Instant.now());
    }

    @Transactional(readOnly = true)
    public List<ResultadoCarga> historicoRecente(int limite) {
        int max = Math.max(1, limite);
        List<ResultadoCarga> itens = new ArrayList<>(historicoCargas);
        itens.sort(Comparator.comparing(ResultadoCarga::processadoEm).reversed());
        return itens.stream().limit(max).toList();
    }

    private void registrarHistoricoCarga(ResultadoCarga resultado) {
        historicoCargas.add(resultado);
        int overflow = historicoCargas.size() - MAX_HISTORICO_CARGAS;
        while (overflow > 0 && !historicoCargas.isEmpty()) {
            historicoCargas.remove(0);
            overflow--;
        }
    }

    public String gerarTemplate(String tribunalCodigo) {
        String tribunal = normalizeUpper(requireText(tribunalCodigo, "tribunalCodigo"));
        return """
                {
                  "pluginId": "%s_BASE_2026",
                  "tribunalCodigo": "%s",
                  "resolucao": "Res. %s 001/2026",
                  "versao": "1.0.0",
                  "descricao": "Plugin integrado de regras e calendário do %s",
                  "tipoPlugin": "COMPLETO",
                  "dependencias": [],
                  "origem": "API",
                  "operadorId": "ADMIN-%s",
                  "federar": false,
                  "ramo": null,
                  "grau": null,
                  "prazoConfig": {
                    "contarSabado": false,
                    "integralmenteCorrido": false,
                    "feriadosAdicionais": []
                  },
                  "perfil": {
                    "tribunalNome": "Tribunal %s",
                    "tribunalSigla": "%s",
                    "uf": null,
                    "ramo": null,
                    "grau": null,
                    "tornarAtivo": false,
                    "visual": {
                      "corPrimaria": "#1A3A6B",
                      "corSecundaria": "#2E5FA3",
                      "corAcento": "#C8A951",
                      "corTextoSobrePrimaria": "#FFFFFF",
                      "corFundo": "#F8FAFC",
                      "fonteInstitucional": "Arial",
                      "rodapeTexto": "Tribunal %s",
                      "usaLogoEmDocumentos": true,
                      "usaAssinaturaCertificada": true
                    },
                    "terminologia": {
                      "AUTOR": "Autor",
                      "REU": "Réu",
                      "DISTRIBUIDOR": "Protocolo Geral"
                    },
                    "terminologiaPlural": {
                      "DISTRIBUIDOR": "Protocolos Gerais"
                    },
                    "ux": {
                      "habilitaVideoAudiencia": true,
                      "habilitaNotificacaoWhatsApp": true,
                      "fusoHorario": "America/Sao_Paulo",
                      "itensPorPaginaPadrao": 20,
                      "modoEscuroDisponivel": true,
                      "exibeCalculadoraPrazos": true,
                      "vlibras": true,
                      "nivelConformeWcag": "AA"
                    },
                    "contato": {
                      "site": "https:                      
                      "email": "contato@exemplo.jus.br",
                      "telefone": "(00) 0000-0000"
                    }
                  },
                  "tribunalRules": [
                    {
                      "path": "prazo.despacho.inicial_dias_uteis",
                      "nivel": "TRIBUNAL",
                      "escopoId": null,
                      "valor": 3,
                      "tipoValor": "DURACAO_DIAS",
                      "modo": "RESTRINGIR",
                      "descricao": "Prazo de despacho inicial local",
                      "fundamentacao": "Res. %s 001/2026 art. 5º",
                      "vigenteDesde": "2026-01-01T00:00:00Z",
                      "vigenteAte": null,
                      "versao": "1.0.0",
                      "ramo": null,
                      "grau": null
                    }
                  ],
                  "calendarioEntries": [
                    {
                      "data": "2026-03-25",
                      "tipo": "FERIADO_ESTADUAL",
                      "descricao": "Feriado estadual local",
                      "suspendeExpediente": true,
                      "suspendePrazos": true,
                      "recorrencia": "ANUAL_FIXO",
                      "fundamentacao": "Lei estadual aplicável",
                      "abrangencia": "ESTADUAL-%s",
                      "uf": null,
                      "comarca": null
                    }
                  ],
                  "recessoPeriods": [
                    {
                      "descricao": "Recesso extraordinário",
                      "inicio": "2026-02-16",
                      "fim": "2026-02-18",
                      "suspendePrazos": true,
                      "fundamentacao": "Res. %s 001/2026 art. 20º",
                      "uf": null,
                      "comarca": null
                    }
                  ],
                  "rulePackRules": []
                }
                """.formatted(tribunal, tribunal, tribunal, tribunal, tribunal, tribunal, tribunal, tribunal, tribunal, tribunal, tribunal);
    }

    private ResultadoCarga aplicarPlano(LoadPlan plan, String rawJson) {
        List<String> erros = new ArrayList<>();
        List<String> avisos = new ArrayList<>(plan.avisos());
        List<String> dependenciasAusentes = validarDependencias(plan.dependencias(), plan.pluginKey());
        if (!dependenciasAusentes.isEmpty()) {
            avisos.addAll(dependenciasAusentes.stream().map(dep -> "Dependência não ativa: " + dep).toList());
        }

        PluginRegistrado anteriorMesmoPlugin = registrosPorKey.get(plan.pluginKey());
        if (anteriorMesmoPlugin != null && anteriorMesmoPlugin.snapshot() != null && Objects.equals(anteriorMesmoPlugin.snapshot().hashSha256(), plan.hash())) {
            return new ResultadoCarga(
                    plan.pluginKey(),
                    plan.pluginId(),
                    plan.tribunalCodigo(),
                    plan.resolucao(),
                    true,
                    anteriorMesmoPlugin.snapshot().status(),
                    anteriorMesmoPlugin.snapshot().totalRegrasTribunal(),
                    anteriorMesmoPlugin.snapshot().totalRegrasRulePack(),
                    anteriorMesmoPlugin.snapshot().totalFeriados(),
                    anteriorMesmoPlugin.snapshot().totalCalendarioEntradas(),
                    anteriorMesmoPlugin.snapshot().totalRecessos(),
                    anteriorMesmoPlugin.snapshot().ignorados(),
                    anteriorMesmoPlugin.snapshot().erros(),
                    anteriorMesmoPlugin.snapshot().avisos(),
                    Instant.now()
            );
        }

        if (anteriorMesmoPlugin != null && anteriorMesmoPlugin.snapshot() != null && compareSemanticVersions(plan.versao(), anteriorMesmoPlugin.snapshot().versao()) < 0) {
            avisos.add("Nova carga possui versão inferior à ativa: " + plan.versao() + " < " + anteriorMesmoPlugin.snapshot().versao());
        }

        substituirConflitantesPorResolucao(plan.tribunalCodigo(), plan.resolucao(), plan.pluginKey(), plan.federar());

        try {
            if (plan.tipoPlugin() != TipoPlugin.CALENDARIO) {
                tribunalRuleEngine.substituirRegrasPlugin(plan.pluginKey(), plan.regrasTribunal());
                for (Map.Entry<BucketRegraPack, List<NationalRulePackEngine.Regra>> entry : plan.regrasRulePack().entrySet()) {
                    BucketRegraPack bucket = entry.getKey();
                    nationalRulePackEngine.substituirRegrasCustomizadas(plan.pluginKey(), bucket.tribunalCodigo(), bucket.ramo(), bucket.grau(), entry.getValue());
                }
                if (anteriorMesmoPlugin != null && anteriorMesmoPlugin.snapshot() != null) {
                    for (BucketRegraPack antigo : anteriorMesmoPlugin.snapshot().bucketsRulePack()) {
                        if (!plan.regrasRulePack().containsKey(antigo)) {
                            nationalRulePackEngine.removerRegrasCustomizadas(plan.pluginKey(), antigo.tribunalCodigo(), antigo.ramo(), antigo.grau());
                        }
                    }
                }
                tribunalRuleEngine.sincronizarRulePackAdaptadoTribunal(plan.tribunalCodigo());
            } else {
                tribunalRuleEngine.removerRegrasPlugin(plan.pluginKey());
                if (anteriorMesmoPlugin != null && anteriorMesmoPlugin.snapshot() != null) {
                    for (BucketRegraPack antigo : anteriorMesmoPlugin.snapshot().bucketsRulePack()) {
                        nationalRulePackEngine.removerRegrasCustomizadas(plan.pluginKey(), antigo.tribunalCodigo(), antigo.ramo(), antigo.grau());
                    }
                }
                tribunalRuleEngine.sincronizarRulePackAdaptadoTribunal(plan.tribunalCodigo());
            }

            if (plan.tipoPlugin() != TipoPlugin.REGRAS) {
                calendarioForenseTribunalService.substituirPluginCalendario(plan.pluginKey(), plan.calendarioEntradas(), plan.recessoPeriods());
            } else {
                calendarioForenseTribunalService.removerPluginCalendario(plan.pluginKey());
            }

            if (plan.feriadosPrazoCombinados().isEmpty() && plan.manifesto().prazoConfig() == null) {
                if (anteriorMesmoPlugin != null && anteriorMesmoPlugin.snapshot() != null && anteriorMesmoPlugin.snapshot().possuiConfiguracaoPrazo()) {
                    nationalPrazoEngine.removerConfiguracao(plan.tribunalCodigo(), plan.ramoContexto(), plan.grauContexto());
                }
            } else {
                nationalPrazoEngine.registrarConfiguracao(new NationalPrazoEngine.ConfiguracaoPrazo(
                        plan.tribunalCodigo(),
                        plan.ramoContexto(),
                        plan.grauContexto(),
                        plan.feriadosPrazoCombinados(),
                        plan.contarSabado(),
                        plan.integralmenteCorrido()
                ));
            }

            if (plan.perfilInstancia() != null) {
                perfilInstanciaTribunalService.substituirPerfilPlugin(plan.pluginKey(), plan.perfilInstancia());
                if (plan.ativarPerfil()) {
                    perfilInstanciaTribunalService.definirTribunalAtivo(plan.perfilInstancia().tribunalCodigo());
                }
            } else if (anteriorMesmoPlugin != null && anteriorMesmoPlugin.manifesto() != null && anteriorMesmoPlugin.manifesto().perfil() != null) {
                perfilInstanciaTribunalService.removerPerfilPlugin(plan.pluginKey());
            }
        } catch (Exception ex) {
            erros.add(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }

        StatusPlugin status = (plan.regrasTribunal().isEmpty() && plan.regrasRulePack().isEmpty() && plan.calendarioEntradas().isEmpty() && plan.recessoPeriods().isEmpty() && plan.feriadosPrazoCombinados().isEmpty())
                ? StatusPlugin.ERRO_CARGA
                : (erros.isEmpty() ? StatusPlugin.CARREGADO : StatusPlugin.ERRO_CARGA);

        int ignorados = erros.size();
        PluginSnapshot snapshot = new PluginSnapshot(
                plan.pluginKey(),
                plan.pluginId(),
                plan.tribunalCodigo(),
                plan.resolucao(),
                plan.versao(),
                plan.tipoPlugin(),
                status,
                plan.origem(),
                plan.operadorId(),
                plan.hash(),
                Instant.now(),
                plan.federar(),
                plan.manifesto().prazoConfig() != null || !plan.feriadosPrazoCombinados().isEmpty(),
                plan.ramoContexto(),
                plan.grauContexto(),
                Set.copyOf(plan.regrasRulePack().keySet()),
                plan.regrasTribunal().size(),
                plan.regrasRulePack().values().stream().mapToInt(List::size).sum(),
                manifestSupport.countPrazoFeriadosExplicitos(plan.manifesto().prazoConfig()),
                plan.calendarioEntradas().size(),
                plan.recessoPeriods().size(),
                ignorados,
                List.copyOf(erros),
                List.copyOf(avisos)
        );

        PluginRegistrado registrado = new PluginRegistrado(
                plan.pluginKey(),
                plan.pluginId(),
                plan.tribunalCodigo(),
                plan.resolucao(),
                plan.versao(),
                plan.tipoPlugin(),
                status,
                plan.manifesto(),
                snapshot,
                List.copyOf(plan.dependencias()),
                plan.hash(),
                snapshot.carregadoEm(),
                plan.operadorId(),
                snapshot.erros(),
                snapshot.avisos()
        );

        registrosPorKey.put(plan.pluginKey(), registrado);
        registrarIdTribunal(plan.tribunalCodigo(), plan.pluginKey());
        if (status == StatusPlugin.CARREGADO) {
            pluginsAtivos.put(plan.pluginKey(), snapshot);
            publicarOutboxAtualizacao(snapshot);
            if (plan.federar()) {
                publicarFederacaoAtualizacao(snapshot, rawJson);
            }
        } else {
            pluginsAtivos.remove(plan.pluginKey());
        }

        return new ResultadoCarga(
                plan.pluginKey(),
                plan.pluginId(),
                plan.tribunalCodigo(),
                plan.resolucao(),
                status == StatusPlugin.CARREGADO,
                status,
                snapshot.totalRegrasTribunal(),
                snapshot.totalRegrasRulePack(),
                snapshot.totalFeriados(),
                snapshot.totalCalendarioEntradas(),
                snapshot.totalRecessos(),
                snapshot.ignorados(),
                snapshot.erros(),
                snapshot.avisos(),
                Instant.now()
        );
    }

    private LoadPlan montarPlano(String payload, PluginManifest manifest, String origem, boolean federar) {
        Objects.requireNonNull(manifest, "manifest");
        String tribunalCodigo = normalizeUpper(requireText(manifest.tribunalCodigo(), "tribunalCodigo"));
        String resolucao = blankToDefault(manifest.resolucao(), "PLUGIN " + tribunalCodigo);
        String pluginId = manifestSupport.derivePluginId(manifest, tribunalCodigo, resolucao);
        String pluginKey = pluginKey(tribunalCodigo, pluginId);
        String versao = blankToDefault(manifest.versao(), "1.0.0");
        String operadorId = blankToDefault(coalesce(manifest.operadorId(), manifest.origem()), "PLUGIN-SYSTEM");
        String origemEfetiva = blankToDefault(coalesce(origem, manifest.origem()), "API");
        boolean federarEfetivo = federar || Boolean.TRUE.equals(manifest.federar());
        TipoPlugin tipoPlugin = manifestSupport.resolveTipoPlugin(manifest);
        RamoDireito ramoContexto = PluginResolucaoTribunalManifestSupport.parseRamo(manifest.ramo());
        GrauJurisdicao grauContexto = PluginResolucaoTribunalManifestSupport.parseGrau(manifest.grau());
        String hash = Hashes.sha256Hex(payload);
        List<String> avisos = new ArrayList<>();
        List<String> dependencias = sanitizeDependencias(manifest.dependencias());

        List<TribunalRuleSpec> tribunalSpecs = manifestSupport.mergedTribunalRuleSpecs(manifest);
        List<RulePackSpec> rulePackSpecs = safeList(manifest.rulePackRules());
        List<CalendarioEntrySpec> calendarioSpecs = manifestSupport.mergedCalendarioEntrySpecs(manifest);
        List<CalendarioRecessoSpec> recessoSpecs = manifestSupport.mergedCalendarioRecessoSpecs(manifest);

        validateSize("tribunalRules", tribunalSpecs.size(), MAX_TRIBUNAL_RULES);
        validateSize("rulePackRules", rulePackSpecs.size(), MAX_RULEPACK_RULES);
        validateSize("calendarioEntries", calendarioSpecs.size(), MAX_CALENDARIO_ENTRIES);
        validateSize("recessoPeriods", recessoSpecs.size(), MAX_RECESSO_PERIODS);
        validateSize("dependencias", dependencias.size(), MAX_DEPENDENCIAS);

        List<TribunalRuleEngine.EntradaRegra> regrasTribunal = tipoPlugin == TipoPlugin.CALENDARIO
                ? List.of()
                : converterRegrasTribunal(tribunalSpecs, manifest, tribunalCodigo, ramoContexto, grauContexto);
        Map<BucketRegraPack, List<NationalRulePackEngine.Regra>> regrasRulePack = tipoPlugin == TipoPlugin.CALENDARIO
                ? Map.of()
                : agruparRegrasRulePack(rulePackSpecs, tribunalCodigo, pluginId, ramoContexto, grauContexto);
        List<CalendarioForenseTribunalService.EntradaCalendario> calendarioEntradas = tipoPlugin == TipoPlugin.REGRAS
                ? List.of()
                : converterCalendarioEntradas(calendarioSpecs, tribunalCodigo, pluginKey);
        List<CalendarioForenseTribunalService.PeriodoRecesso> recessoPeriods = tipoPlugin == TipoPlugin.REGRAS
                ? List.of()
                : converterRecessos(recessoSpecs, tribunalCodigo, pluginKey);

        if (tipoPlugin == TipoPlugin.REGRAS && (!calendarioSpecs.isEmpty() || !recessoSpecs.isEmpty())) {
            avisos.add("Calendário informado em plugin de regras foi ignorado por tipoPlugin=REGRAS");
        }
        if (tipoPlugin == TipoPlugin.CALENDARIO && (!tribunalSpecs.isEmpty() || !rulePackSpecs.isEmpty())) {
            avisos.add("Regras informadas em plugin de calendário foram ignoradas por tipoPlugin=CALENDARIO");
        }

        Set<LocalDate> feriadosPrazoCombinados = manifestSupport.mergeFeriadosPrazo(manifest.prazoConfig(), calendarioEntradas, recessoPeriods);
        boolean contarSabado = manifest.prazoConfig() != null && Boolean.TRUE.equals(manifest.prazoConfig().contarSabado());
        boolean integralmenteCorrido = manifest.prazoConfig() != null && Boolean.TRUE.equals(manifest.prazoConfig().integralmenteCorrido());
        PerfilInstanciaTribunalService.PerfilInstancia perfilInstancia = manifestSupport.converterPerfil(manifest.perfil(), tribunalCodigo, ramoContexto, grauContexto);
        boolean ativarPerfil = manifest.perfil() != null && Boolean.TRUE.equals(manifest.perfil().tornarAtivo());

        return new LoadPlan(
                pluginKey,
                pluginId,
                tribunalCodigo,
                resolucao,
                tipoPlugin,
                versao,
                origemEfetiva,
                operadorId,
                hash,
                federarEfetivo,
                ramoContexto,
                grauContexto,
                manifest,
                dependencias,
                regrasTribunal,
                regrasRulePack,
                calendarioEntradas,
                recessoPeriods,
                Set.copyOf(feriadosPrazoCombinados),
                contarSabado,
                integralmenteCorrido,
                perfilInstancia,
                ativarPerfil,
                List.copyOf(avisos)
        );
    }

    private void substituirConflitantesPorResolucao(String tribunalCodigo, String resolucao, String pluginKeyAtual, boolean federar) {
        List<PluginRegistrado> conflitantes = registrosPorKey.values().stream()
                .filter(PluginRegistrado::ativo)
                .filter(item -> Objects.equals(item.tribunalCodigo(), tribunalCodigo))
                .filter(item -> Objects.equals(normalizeToken(item.resolucao()), normalizeToken(resolucao)))
                .filter(item -> !Objects.equals(item.pluginKey(), pluginKeyAtual))
                .toList();
        for (PluginRegistrado conflito : conflitantes) {
            desativarInterno(conflito, StatusPlugin.SUBSTITUIDO, "Substituído por nova versão da mesma resolução", "SYSTEM", federar || conflito.snapshot().federado());
        }
    }

    private void desativarInterno(PluginRegistrado registrado, StatusPlugin novoStatus, String motivo, String operador, boolean federar) {
        PluginSnapshot atual = registrado.snapshot();
        tribunalRuleEngine.removerRegrasPlugin(registrado.pluginKey());
        calendarioForenseTribunalService.removerPluginCalendario(registrado.pluginKey());
        perfilInstanciaTribunalService.removerPerfilPlugin(registrado.pluginKey());
        for (BucketRegraPack bucket : atual.bucketsRulePack()) {
            nationalRulePackEngine.removerRegrasCustomizadas(registrado.pluginKey(), bucket.tribunalCodigo(), bucket.ramo(), bucket.grau());
        }
        tribunalRuleEngine.sincronizarRulePackAdaptadoTribunal(registrado.tribunalCodigo());
        if (atual.possuiConfiguracaoPrazo()) {
            nationalPrazoEngine.removerConfiguracao(atual.tribunalCodigo(), atual.ramoContexto(), atual.grauContexto());
        }
        List<String> avisos = new ArrayList<>(atual.avisos());
        avisos.add(blankToDefault(motivo, "Desativado") + " por " + blankToDefault(operador, "SYSTEM"));
        PluginSnapshot snapshot = new PluginSnapshot(
                atual.pluginKey(),
                atual.pluginId(),
                atual.tribunalCodigo(),
                atual.resolucao(),
                atual.versao(),
                atual.tipoPlugin(),
                novoStatus,
                atual.origem(),
                atual.operadorId(),
                atual.hashSha256(),
                Instant.now(),
                atual.federado(),
                false,
                atual.ramoContexto(),
                atual.grauContexto(),
                atual.bucketsRulePack(),
                atual.totalRegrasTribunal(),
                atual.totalRegrasRulePack(),
                atual.totalFeriados(),
                atual.totalCalendarioEntradas(),
                atual.totalRecessos(),
                atual.ignorados(),
                atual.erros(),
                List.copyOf(avisos)
        );
        PluginRegistrado novo = new PluginRegistrado(
                registrado.pluginKey(),
                registrado.pluginId(),
                registrado.tribunalCodigo(),
                registrado.resolucao(),
                registrado.versao(),
                registrado.tipoPlugin(),
                novoStatus,
                registrado.manifesto(),
                snapshot,
                registrado.dependencias(),
                registrado.hashConteudo(),
                snapshot.carregadoEm(),
                blankToDefault(operador, "SYSTEM"),
                snapshot.erros(),
                snapshot.avisos()
        );
        registrosPorKey.put(novo.pluginKey(), novo);
        pluginsAtivos.remove(novo.pluginKey());
        removerIdTribunal(novo.tribunalCodigo(), novo.pluginKey());
        publicarOutboxRemocao(snapshot);
        if (federar) {
            publicarFederacaoRemocao(snapshot);
        }
    }

    private void registrarIdTribunal(String tribunalCodigo, String pluginKey) {
        String tribunal = normalizeUpper(requireText(tribunalCodigo, "tribunalCodigo"));
        String key = normalizePluginKey(pluginKey);
        CopyOnWriteArrayList<String> ids = idsPorTribunal.computeIfAbsent(tribunal, ignored -> new CopyOnWriteArrayList<>());
        ids.removeIf(id -> {
            PluginRegistrado registrado = registrosPorKey.get(id);
            return registrado == null || !Objects.equals(registrado.tribunalCodigo(), tribunal);
        });
        ids.addIfAbsent(key);
        while (ids.size() > MAX_IDS_POR_TRIBUNAL) {
            ids.remove(0);
        }
    }

    private void removerIdTribunal(String tribunalCodigo, String pluginKey) {
        String tribunal = normalizeUpper(requireText(tribunalCodigo, "tribunalCodigo"));
        String key = normalizePluginKey(pluginKey);
        CopyOnWriteArrayList<String> ids = idsPorTribunal.get(tribunal);
        if (ids == null) {
            return;
        }
        ids.removeIf(id -> Objects.equals(id, key));
        if (ids.isEmpty()) {
            idsPorTribunal.remove(tribunal, ids);
        }
    }

    private List<String> validarDependencias(List<String> dependencias, String pluginKeyAtual) {
        List<String> faltantes = new ArrayList<>();
        for (String dependencia : safeList(dependencias)) {
            String token = normalizeToken(dependencia);
            boolean ativa = pluginsAtivos.values().stream()
                    .filter(PluginSnapshot::ativo)
                    .anyMatch(item -> !Objects.equals(item.pluginKey(), pluginKeyAtual)
                            && (Objects.equals(normalizeToken(item.pluginId()), token)
                            || Objects.equals(normalizeToken(item.pluginKey()), token)));
            if (!ativa) {
                faltantes.add(dependencia);
            }
        }
        return faltantes;
    }

    private List<TribunalRuleEngine.EntradaRegra> converterRegrasTribunal(List<TribunalRuleSpec> specs,
                                                                          PluginManifest manifest,
                                                                          String tribunalCodigo,
                                                                          RamoDireito ramoContexto,
                                                                          GrauJurisdicao grauContexto) {
        List<TribunalRuleEngine.EntradaRegra> regras = new ArrayList<>();
        for (TribunalRuleSpec spec : specs) {
            TribunalRuleEngine.NivelRegra nivel = PluginResolucaoTribunalManifestSupport.parseNivel(spec.nivel());
            TribunalRuleEngine.ChaveRegra chave = TribunalRuleEngine.ChaveRegra.de(requireText(spec.path(), "tribunalRules.path"));
            TribunalRuleEngine.TipoValor tipoValor = PluginResolucaoTribunalManifestSupport.parseTipoValor(spec.tipoValor());
            TribunalRuleEngine.ModoSobrescrita modo = PluginResolucaoTribunalManifestSupport.parseModo(spec.modo());
            String escopoId = manifestSupport.resolverEscopo(spec.escopoId(), nivel, tribunalCodigo);
            RamoDireito ramo = coalesce(PluginResolucaoTribunalManifestSupport.parseRamo(spec.ramo()), ramoContexto);
            GrauJurisdicao grau = coalesce(PluginResolucaoTribunalManifestSupport.parseGrau(spec.grau()), grauContexto);
            regras.add(new TribunalRuleEngine.EntradaRegra(
                    chave,
                    nivel,
                    escopoId,
                    manifestSupport.converterValorTribunal(spec.valor(), tipoValor),
                    tipoValor,
                    modo,
                    blankToDefault(spec.fundamentacao(), blankToDefault(manifest.resolucao(), "PLUGIN")),
                    blankToNull(spec.descricao()),
                    spec.ativa() == null || spec.ativa(),
                    PluginResolucaoTribunalManifestSupport.parseInstant(spec.vigenteDesde()),
                    PluginResolucaoTribunalManifestSupport.parseInstant(spec.vigenteAte()),
                    blankToDefault(manifest.operadorId(), "PLUGIN-SYSTEM"),
                    blankToDefault(spec.versao(), blankToDefault(manifest.versao(), "1.0.0")),
                    ramo,
                    grau
            ));
        }
        return List.copyOf(regras);
    }

    private Map<BucketRegraPack, List<NationalRulePackEngine.Regra>> agruparRegrasRulePack(List<RulePackSpec> specs,
                                                                                            String tribunalCodigo,
                                                                                            String pluginId,
                                                                                            RamoDireito ramoContexto,
                                                                                            GrauJurisdicao grauContexto) {
        LinkedHashMap<BucketRegraPack, List<NationalRulePackEngine.Regra>> buckets = new LinkedHashMap<>();
        for (RulePackSpec spec : specs) {
            RamoDireito ramo = coalesce(PluginResolucaoTribunalManifestSupport.parseRamo(spec.ramo()), ramoContexto);
            GrauJurisdicao grau = coalesce(PluginResolucaoTribunalManifestSupport.parseGrau(spec.grau()), grauContexto);
            BucketRegraPack bucket = new BucketRegraPack(tribunalCodigo, ramo, grau);
            buckets.computeIfAbsent(bucket, key -> new ArrayList<>()).add(converterRegraRulePack(spec, tribunalCodigo, pluginId, ramo));
        }
        return buckets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue()), (a, b) -> a, LinkedHashMap::new));
    }

    private List<CalendarioForenseTribunalService.EntradaCalendario> converterCalendarioEntradas(List<CalendarioEntrySpec> specs,
                                                                                                  String tribunalCodigo,
                                                                                                  String pluginKey) {
        LinkedHashMap<String, CalendarioForenseTribunalService.EntradaCalendario> entradas = new LinkedHashMap<>();
        for (CalendarioEntrySpec spec : specs) {
            CalendarioForenseTribunalService.EntradaCalendario entrada = new CalendarioForenseTribunalService.EntradaCalendario(
                    tribunalCodigo,
                    LocalDate.parse(requireText(spec.data(), "calendarioEntries.data")),
                    PluginResolucaoTribunalManifestSupport.parseTipoEntradaCalendario(spec.tipo()),
                    requireText(spec.descricao(), "calendarioEntries.descricao"),
                    spec.suspendeExpediente() == null || spec.suspendeExpediente(),
                    spec.suspendePrazos() == null || spec.suspendePrazos(),
                    PluginResolucaoTribunalManifestSupport.parseRecorrenciaCalendario(spec.recorrencia()),
                    blankToNull(spec.fundamentacao()),
                    blankToNull(spec.abrangencia()),
                    blankToNull(spec.uf()),
                    blankToNull(spec.comarca()),
                    pluginKey
            );
            entradas.put(calendarioEntryKey(entrada), entrada);
        }
        return List.copyOf(entradas.values());
    }

    private List<CalendarioForenseTribunalService.PeriodoRecesso> converterRecessos(List<CalendarioRecessoSpec> specs,
                                                                                     String tribunalCodigo,
                                                                                     String pluginKey) {
        LinkedHashMap<String, CalendarioForenseTribunalService.PeriodoRecesso> recessos = new LinkedHashMap<>();
        for (CalendarioRecessoSpec spec : specs) {
            CalendarioForenseTribunalService.PeriodoRecesso recesso = new CalendarioForenseTribunalService.PeriodoRecesso(
                    tribunalCodigo,
                    requireText(spec.descricao(), "recessoPeriods.descricao"),
                    LocalDate.parse(requireText(spec.inicio(), "recessoPeriods.inicio")),
                    LocalDate.parse(requireText(spec.fim(), "recessoPeriods.fim")),
                    spec.suspendePrazos() == null || spec.suspendePrazos(),
                    blankToNull(spec.fundamentacao()),
                    blankToNull(spec.uf()),
                    blankToNull(spec.comarca()),
                    pluginKey
            );
            recessos.put(recessoKey(recesso), recesso);
        }
        return List.copyOf(recessos.values());
    }

    private NationalRulePackEngine.Regra converterRegraRulePack(RulePackSpec spec,
                                                                String tribunalCodigo,
                                                                String pluginId,
                                                                RamoDireito ramoPadrao) {
        String tipo = normalizeUpper(requireText(spec.tipo(), "rulePackRules.tipo"));
        String codigo = manifestSupport.canonicalRuleCode(tribunalCodigo, pluginId, requireText(spec.codigo(), "rulePackRules.codigo"));
        String descricao = requireText(spec.descricao(), "rulePackRules.descricao");
        RamoDireito ramo = coalesce(PluginResolucaoTribunalManifestSupport.parseRamo(spec.ramo()), ramoPadrao);
        return switch (tipo) {
            case "ADMISSIBILIDADE" -> new NationalRulePackEngine.RegraAdmissibilidade(codigo, descricao, ramo, safeList(spec.requisitos()), blankToDefault(spec.fundamento(), "PLUGIN"));
            case "PRAZO_ESPECIFICO" -> new NationalRulePackEngine.RegraPrazoEspecifico(codigo, descricao, ramo, requireText(spec.tipoAto(), "rulePackRules.tipoAto"), spec.dias() == null ? 0 : Math.max(0, spec.dias()), spec.uteis() == null || spec.uteis(), blankToDefault(spec.fundamento(), "PLUGIN"));
            case "REQUISITO" -> new NationalRulePackEngine.RegraRequisito(codigo, descricao, ramo, safeList(spec.documentosObrigatorios()), Boolean.TRUE.equals(spec.bloqueante()));
            case "ALERTA" -> new NationalRulePackEngine.RegraAlerta(codigo, descricao, ramo, requireText(spec.mensagemAlerta(), "rulePackRules.mensagemAlerta"), blankToDefault(spec.nivelAlerta(), "INFO"));
            case "FLUXO" -> new NationalRulePackEngine.RegraFluxo(codigo, descricao, ramo, requireText(spec.faseOrigem(), "rulePackRules.faseOrigem"), requireText(spec.proximaFase(), "rulePackRules.proximaFase"), Boolean.TRUE.equals(spec.exigeAprovacao()));
            default -> throw new IllegalArgumentException("Tipo de regra do rule pack não suportado: " + spec.tipo());
        };
    }

    private void publicarOutboxAtualizacao(PluginSnapshot snapshot) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("pluginKey", snapshot.pluginKey());
        payload.put("pluginId", snapshot.pluginId());
        payload.put("tribunalCodigo", snapshot.tribunalCodigo());
        payload.put("resolucao", snapshot.resolucao());
        payload.put("versao", snapshot.versao());
        payload.put("tipoPlugin", snapshot.tipoPlugin().name());
        payload.put("status", snapshot.status().name());
        payload.put("hashSha256", snapshot.hashSha256());
        payload.put("origem", snapshot.origem());
        payload.put("operadorId", snapshot.operadorId());
        payload.put("carregadoEm", snapshot.carregadoEm().toString());
        payload.put("regrasTribunal", snapshot.totalRegrasTribunal());
        payload.put("regrasRulePack", snapshot.totalRegrasRulePack());
        payload.put("feriadosExplicitos", snapshot.totalFeriados());
        payload.put("calendarioEntradas", snapshot.totalCalendarioEntradas());
        payload.put("recessos", snapshot.totalRecessos());
        payload.put("ignorados", snapshot.ignorados());
        payload.put("avisos", snapshot.avisos());
        payload.put("erros", snapshot.erros());
        outboxPublisher.enqueue("tribunal-plugin:" + snapshot.tribunalCodigo(), EVT_TRIBUNAL_PLUGIN_ATUALIZADO, payload, Map.of("source", "plugin_resolucao_tribunal"), "pluginTribunal:" + snapshot.pluginKey() + ":" + snapshot.hashSha256(), "TribunalPlugin", snapshot.pluginKey());
    }

    private void publicarOutboxRemocao(PluginSnapshot snapshot) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("pluginKey", snapshot.pluginKey());
        payload.put("pluginId", snapshot.pluginId());
        payload.put("tribunalCodigo", snapshot.tribunalCodigo());
        payload.put("resolucao", snapshot.resolucao());
        payload.put("versao", snapshot.versao());
        payload.put("status", snapshot.status().name());
        payload.put("hashSha256", snapshot.hashSha256());
        payload.put("removidoEm", Instant.now().toString());
        payload.put("calendarioEntradas", snapshot.totalCalendarioEntradas());
        payload.put("recessos", snapshot.totalRecessos());
        outboxPublisher.enqueue("tribunal-plugin:" + snapshot.tribunalCodigo(), EVT_TRIBUNAL_PLUGIN_REMOVIDO, payload, Map.of("source", "plugin_resolucao_tribunal"), "pluginTribunal:remove:" + snapshot.pluginKey() + ":" + snapshot.hashSha256(), "TribunalPlugin", snapshot.pluginKey());
    }

    private void publicarFederacaoAtualizacao(PluginSnapshot snapshot, String rawJson) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("acao", "UPSERT");
            payload.put("pluginKey", snapshot.pluginKey());
            payload.put("pluginId", snapshot.pluginId());
            payload.put("tribunalCodigo", snapshot.tribunalCodigo());
            payload.put("resolucao", snapshot.resolucao());
            payload.put("versao", snapshot.versao());
            payload.put("tipoPlugin", snapshot.tipoPlugin().name());
            payload.put("hashSha256", snapshot.hashSha256());
            payload.put("conteudo", manifestSupport.objectMapper().readValue(rawJson, Object.class));
            federalismoJudicialEngine.registrarEventoFederado(new FederalismoEventoRequest(snapshot.tribunalCodigo(), FederalismoJudicialEngine.TOPIC_FEDERACAO_EVENTOS, EVT_TRIBUNAL_PLUGIN_ATUALIZADO, null, snapshot.operadorId(), snapshot.pluginKey(), "plugin-federacao:" + snapshot.pluginKey() + ":" + snapshot.hashSha256(), FederalismoJudicialEngine.SCHEMA_VERSION_ATUAL, 8, Boolean.FALSE, manifestSupport.objectMapper().writeValueAsString(payload), Map.of("source", "plugin_resolucao_tribunal")));
        } catch (Exception ex) {
            throw new IllegalStateException("plugin_tribunal_federacao_upsert", ex);
        }
    }

    private void publicarFederacaoRemocao(PluginSnapshot snapshot) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("acao", "REMOVE");
            payload.put("pluginKey", snapshot.pluginKey());
            payload.put("pluginId", snapshot.pluginId());
            payload.put("tribunalCodigo", snapshot.tribunalCodigo());
            payload.put("resolucao", snapshot.resolucao());
            payload.put("versao", snapshot.versao());
            payload.put("tipoPlugin", snapshot.tipoPlugin().name());
            payload.put("hashSha256", snapshot.hashSha256());
            federalismoJudicialEngine.registrarEventoFederado(new FederalismoEventoRequest(snapshot.tribunalCodigo(), FederalismoJudicialEngine.TOPIC_FEDERACAO_EVENTOS, EVT_TRIBUNAL_PLUGIN_REMOVIDO, null, snapshot.operadorId(), snapshot.pluginKey(), "plugin-federacao:remove:" + snapshot.pluginKey() + ":" + snapshot.hashSha256(), FederalismoJudicialEngine.SCHEMA_VERSION_ATUAL, 8, Boolean.FALSE, manifestSupport.objectMapper().writeValueAsString(payload), Map.of("source", "plugin_resolucao_tribunal")));
        } catch (Exception ex) {
            throw new IllegalStateException("plugin_tribunal_federacao_remove", ex);
        }
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        String token = normalizeUpper(String.valueOf(value));
        return Set.of("TRUE", "SIM", "YES", "Y", "1", "VERDADEIRO", "ATIVO").contains(token);
    }

    private static <T> List<T> safeList(Collection<T> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }

    private static String pluginKey(String tribunalCodigo, String pluginId) {
        return normalizeUpper(requireText(tribunalCodigo, "tribunalCodigo")) + "::" + normalizePluginId(pluginId);
    }

    private static <T> T coalesce(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private static void validateSize(String field, int size, int max) {
        if (size > max) {
            throw new IllegalArgumentException("Limite excedido para " + field + ": " + size + " > " + max);
        }
    }

    private static List<String> sanitizeDependencias(Collection<String> dependencias) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String dependencia : safeList(dependencias)) {
            set.add(requireText(dependencia, "dependencia"));
        }
        return List.copyOf(set);
    }

    private static boolean isClasspathOrigin(String origem) {
        String value = blankToNull(origem);
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("/tribunais/plugins/") || lower.contains("\\tribunais\\plugins\\") || lower.startsWith("jar:") || lower.startsWith("file:");
    }

    private static String resourceDescription(Resource resource) {
        try {
            return resource.getURI().toString();
        } catch (Exception ex) {
            return resource.getDescription();
        }
    }

    private static int compareSemanticVersions(String a, String b) {
        int[] left = parseSemver(blankToDefault(a, "0.0.0"));
        int[] right = parseSemver(blankToDefault(b, "0.0.0"));
        int max = Math.max(left.length, right.length);
        for (int i = 0; i < max; i++) {
            int l = i < left.length ? left[i] : 0;
            int r = i < right.length ? right[i] : 0;
            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return 0;
    }

    private static int[] parseSemver(String value) {
        return Arrays.stream(blankToDefault(value, "0.0.0").split("\\.")).mapToInt(part -> {
            try {
                return Integer.parseInt(part.replaceAll("[^0-9]", ""));
            } catch (Exception ex) {
                return 0;
            }
        }).toArray();
    }

    private static String calendarioEntryKey(CalendarioForenseTribunalService.EntradaCalendario entrada) {
        return String.join("|",
                entrada.tribunalCodigo(),
                entrada.data().toString(),
                entrada.tipo().name(),
                safe(entrada.uf()),
                safe(entrada.comarca()),
                safe(entrada.abrangencia()),
                safe(entrada.descricao()),
                safe(entrada.origemId()));
    }

    private static String recessoKey(CalendarioForenseTribunalService.PeriodoRecesso recesso) {
        return String.join("|",
                recesso.tribunalCodigo(),
                recesso.inicio().toString(),
                recesso.fim().toString(),
                safe(recesso.uf()),
                safe(recesso.comarca()),
                safe(recesso.descricao()),
                safe(recesso.origemId()));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + field);
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String blankToDefault(String value, String fallback) {
        return blankToNull(value) == null ? fallback : value.trim();
    }

    private static String normalizeUpper(String value) {
        return requireText(value, "valor").trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeToken(String value) {
        return blankToNull(value) == null ? "" : TOKEN_SPLIT.matcher(value.trim().toUpperCase(Locale.ROOT)).replaceAll("");
    }

    private static String normalizePluginId(String pluginId) {
        return TOKEN_SPLIT.matcher(normalizeUpper(requireText(pluginId, "pluginId"))).replaceAll("_").replaceAll("_+", "_");
    }

    private static String normalizePluginKey(String pluginKey) {
        return normalizeUpper(requireText(pluginKey, "pluginKey"));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
