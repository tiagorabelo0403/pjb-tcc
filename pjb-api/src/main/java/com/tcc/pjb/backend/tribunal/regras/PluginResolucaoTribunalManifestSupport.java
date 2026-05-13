package com.tcc.pjb.backend.tribunal.regras;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;
import com.tcc.pjb.backend.tribunal.perfil.PerfilInstanciaTribunalService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import com.tcc.pjb.backend.tribunal.regras.plugin.TipoPlugin;
import com.tcc.pjb.backend.tribunal.regras.spec.CalendarioEntrySpec;
import com.tcc.pjb.backend.tribunal.regras.spec.CalendarioRecessoSpec;
import com.tcc.pjb.backend.tribunal.regras.spec.ContatoSpec;
import com.tcc.pjb.backend.tribunal.regras.spec.FeriadoJSON;
import com.tcc.pjb.backend.tribunal.regras.spec.PerfilSpec;
import com.tcc.pjb.backend.tribunal.regras.spec.PluginManifest;
import com.tcc.pjb.backend.tribunal.regras.spec.PrazoConfig;
import com.tcc.pjb.backend.tribunal.regras.spec.RecessoJSON;
import com.tcc.pjb.backend.tribunal.regras.spec.RegraJSON;
import com.tcc.pjb.backend.tribunal.regras.spec.TribunalRuleSpec;
import com.tcc.pjb.backend.tribunal.regras.spec.UxSpec;
import com.tcc.pjb.backend.tribunal.regras.spec.VisualSpec;

@Component
class PluginResolucaoTribunalManifestSupport {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^A-Z0-9]+");

    private final ObjectMapper objectMapper;
    private final PerfilInstanciaTribunalService perfilInstanciaTribunalService;

    PluginResolucaoTribunalManifestSupport(ObjectMapper objectMapper,
                                           PerfilInstanciaTribunalService perfilInstanciaTribunalService) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.perfilInstanciaTribunalService = Objects.requireNonNull(perfilInstanciaTribunalService);
    }

    PluginManifest readManifest(String json) {
        try {
            return objectMapper.readValue(json, PluginManifest.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("json_plugin_tribunal_invalido", ex);
        }
    }

    List<TribunalRuleSpec> mergedTribunalRuleSpecs(PluginManifest manifest) {
        List<TribunalRuleSpec> specs = new ArrayList<>(safeList(manifest.tribunalRules()));
        for (RegraJSON regra : safeList(manifest.regras())) {
            specs.add(new TribunalRuleSpec(
                    regra.chave(),
                    regra.nivel(),
                    regra.escopoId(),
                    regra.valor(),
                    regra.tipo(),
                    regra.modo(),
                    regra.fundamentacao(),
                    regra.descricao(),
                    Boolean.TRUE,
                    regra.vigenteDesde(),
                    regra.vigenteAte(),
                    manifest.versao(),
                    regra.ramo(),
                    regra.grau()
            ));
        }
        return List.copyOf(specs);
    }

    List<CalendarioEntrySpec> mergedCalendarioEntrySpecs(PluginManifest manifest) {
        List<CalendarioEntrySpec> specs = new ArrayList<>(safeList(manifest.calendarioEntries()));
        for (FeriadoJSON feriado : safeList(manifest.feriados())) {
            specs.add(new CalendarioEntrySpec(
                    feriado.data(),
                    feriado.tipo(),
                    feriado.descricao(),
                    feriado.suspendeExpediente(),
                    feriado.suspendePrazos(),
                    feriado.recorrencia(),
                    feriado.fundamentacao(),
                    feriado.abrangencia(),
                    feriado.uf(),
                    feriado.comarca()
            ));
        }
        return List.copyOf(specs);
    }

    List<CalendarioRecessoSpec> mergedCalendarioRecessoSpecs(PluginManifest manifest) {
        List<CalendarioRecessoSpec> specs = new ArrayList<>(safeList(manifest.recessoPeriods()));
        for (RecessoJSON recesso : safeList(manifest.recessos())) {
            specs.add(new CalendarioRecessoSpec(
                    recesso.descricao(),
                    recesso.inicio(),
                    recesso.fim(),
                    recesso.suspendePrazos(),
                    recesso.fundamentacao(),
                    recesso.uf(),
                    recesso.comarca()
            ));
        }
        return List.copyOf(specs);
    }

    Set<LocalDate> mergeFeriadosPrazo(PrazoConfig prazoConfig,
                                      List<CalendarioForenseTribunalService.EntradaCalendario> entradas,
                                      List<CalendarioForenseTribunalService.PeriodoRecesso> recessos) {
        LinkedHashSet<LocalDate> datas = new LinkedHashSet<>();
        if (prazoConfig != null) {
            for (String valor : safeList(prazoConfig.feriadosAdicionais())) {
                datas.add(LocalDate.parse(requireText(valor, "feriadoAdicional")));
            }
        }
        for (CalendarioForenseTribunalService.EntradaCalendario entrada : safeList(entradas)) {
            if (entrada.suspendePrazos() && entrada.recorrencia() == CalendarioForenseTribunalService.Recorrencia.UNICA) {
                datas.add(entrada.data());
            }
        }
        for (CalendarioForenseTribunalService.PeriodoRecesso recesso : safeList(recessos)) {
            if (!recesso.suspendePrazos()) {
                continue;
            }
            LocalDate data = recesso.inicio();
            while (!data.isAfter(recesso.fim()) && ChronoUnit.DAYS.between(recesso.inicio(), data) <= 370) {
                datas.add(data);
                data = data.plusDays(1);
            }
        }
        return Set.copyOf(datas);
    }

    int countPrazoFeriadosExplicitos(PrazoConfig config) {
        return config == null || config.feriadosAdicionais() == null ? 0 : (int) config.feriadosAdicionais().stream().filter(Objects::nonNull).count();
    }

    Object converterValorTribunal(Object valor, TribunalRuleEngine.TipoValor tipoValor) {
        if (valor == null) {
            return null;
        }
        return switch (tipoValor) {
            case TEXTO -> String.valueOf(valor).trim();
            case INTEIRO, DURACAO_DIAS -> {
                if (valor instanceof Number number) {
                    yield number.intValue();
                }
                yield Integer.parseInt(String.valueOf(valor).replaceAll("[^0-9\\-]", "").trim());
            }
            case DECIMAL -> {
                if (valor instanceof BigDecimal bd) {
                    yield bd;
                }
                if (valor instanceof Number number) {
                    yield BigDecimal.valueOf(number.doubleValue());
                }
                yield new BigDecimal(String.valueOf(valor).replace(".", "").replace(",", ".").trim());
            }
            case BOOLEANO -> toBoolean(valor);
            case LISTA_TEXTO -> {
                if (valor instanceof Collection<?> collection) {
                    yield collection.stream().filter(Objects::nonNull).map(String::valueOf).map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
                }
                yield Arrays.stream(String.valueOf(valor).split(",")).map(String::trim).filter(v -> !v.isBlank()).distinct().toList();
            }
        };
    }

    String resolverEscopo(String escopoId, TribunalRuleEngine.NivelRegra nivel, String tribunalCodigo) {
        String escopo = blankToNull(escopoId);
        if (escopo != null) {
            return normalizeUpper(escopo);
        }
        return switch (nivel) {
            case NACIONAL -> "BRASIL";
            case TRIBUNAL -> tribunalCodigo;
            case COMARCA, VARA -> throw new IllegalArgumentException("escopoId obrigatório para nível " + nivel.name());
        };
    }

    String derivePluginId(PluginManifest manifest, String tribunalCodigo, String resolucao) {
        if (blankToNull(manifest.pluginId()) != null) {
            return normalizePluginId(manifest.pluginId());
        }
        String base = tribunalCodigo + "_" + resolucao + "_" + blankToDefault(manifest.tipoPlugin(), "COMPLETO");
        String token = TOKEN_SPLIT.matcher(normalizeUpper(base)).replaceAll("_");
        token = token.replaceAll("_+", "_");
        token = token.length() > 72 ? token.substring(0, 72) : token;
        return normalizePluginId(token + "_" + Hashes.sha256Hex(base).substring(0, 8).toUpperCase(Locale.ROOT));
    }

    TipoPlugin resolveTipoPlugin(PluginManifest manifest) {
        String informado = blankToNull(manifest.tipoPlugin());
        if (informado != null) {
            return TipoPlugin.valueOf(normalizeUpper(informado));
        }
        boolean temRegras = !mergedTribunalRuleSpecs(manifest).isEmpty() || !safeList(manifest.rulePackRules()).isEmpty();
        boolean temCalendario = !mergedCalendarioEntrySpecs(manifest).isEmpty() || !mergedCalendarioRecessoSpecs(manifest).isEmpty() || manifest.prazoConfig() != null;
        if (temRegras && temCalendario) {
            return TipoPlugin.COMPLETO;
        }
        if (temCalendario) {
            return TipoPlugin.CALENDARIO;
        }
        return TipoPlugin.REGRAS;
    }

    PerfilInstanciaTribunalService.PerfilInstancia converterPerfil(PerfilSpec spec, String tribunalCodigo, RamoDireito ramoContexto, GrauJurisdicao grauContexto) {
        if (spec == null) {
            return null;
        }
        PerfilInstanciaTribunalService.PerfilInstancia base = perfilInstanciaTribunalService.resolverPorCodigoOuPadrao(tribunalCodigo);
        PerfilInstanciaTribunalService.RamoJustica ramo = parsePerfilRamo(spec.ramo(), tribunalCodigo, ramoContexto, base.ramo());
        PerfilInstanciaTribunalService.GrauInstancia grau = parsePerfilGrau(spec.grau(), grauContexto, base.grau());
        Map<PerfilInstanciaTribunalService.TermoPadrao, String> termos = mergeTermos(base.terminologia(), spec.terminologia());
        Map<PerfilInstanciaTribunalService.TermoPadrao, String> termosPlural = mergeTermos(base.terminologiaPlural(), spec.terminologiaPlural());
        return new PerfilInstanciaTribunalService.PerfilInstancia(
                tribunalCodigo,
                blankToDefault(spec.tribunalNome(), base.tribunalNome()),
                blankToDefault(spec.tribunalSigla(), tribunalCodigo),
                coalesce(spec.uf(), base.uf()),
                ramo,
                grau,
                mergeVisual(base.visual(), spec.visual()),
                termos,
                termosPlural,
                mergeUx(base.ux(), spec.ux()),
                mergeContato(base.contato(), spec.contato())
        );
    }

    private Map<PerfilInstanciaTribunalService.TermoPadrao, String> mergeTermos(Map<PerfilInstanciaTribunalService.TermoPadrao, String> base, Map<String, String> extras) {
        EnumMap<PerfilInstanciaTribunalService.TermoPadrao, String> merged = new EnumMap<>(PerfilInstanciaTribunalService.TermoPadrao.class);
        if (base != null) {
            merged.putAll(base);
        }
        if (extras != null) {
            extras.forEach((key, value) -> resolveTermoPadrao(key).ifPresent(termo -> {
                if (value != null && !value.isBlank()) {
                    merged.put(termo, value.trim());
                }
            }));
        }
        return Map.copyOf(merged);
    }


    private java.util.Optional<PerfilInstanciaTribunalService.TermoPadrao> resolveTermoPadrao(String key) {
        java.util.Optional<PerfilInstanciaTribunalService.TermoPadrao> direct = PerfilInstanciaTribunalService.TermoPadrao.fromKey(key);
        if (direct.isPresent()) {
            return direct;
        }
        String token = key == null ? null : normalizeUpper(key);
        if ("MAGISTRADO".equals(token) || "MAGISTRADA".equals(token)) {
            return java.util.Optional.of(PerfilInstanciaTribunalService.TermoPadrao.JUIZ);
        }
        return java.util.Optional.empty();
    }

    private PerfilInstanciaTribunalService.IdentidadeVisual mergeVisual(PerfilInstanciaTribunalService.IdentidadeVisual base, VisualSpec spec) {
        if (spec == null) {
            return base;
        }
        return new PerfilInstanciaTribunalService.IdentidadeVisual(
                coalesce(spec.corPrimaria(), base.corPrimaria()),
                coalesce(spec.corSecundaria(), base.corSecundaria()),
                coalesce(spec.corAcento(), base.corAcento()),
                coalesce(spec.corTextoSobrePrimaria(), base.corTextoSobrePrimaria()),
                coalesce(spec.corFundo(), base.corFundo()),
                coalesce(spec.brasaoUrl(), base.brasaoUrl()),
                coalesce(spec.logoHorizontalUrl(), base.logoHorizontalUrl()),
                coalesce(spec.faviconUrl(), base.faviconUrl()),
                coalesce(spec.fonteInstitucional(), base.fonteInstitucional()),
                coalesce(spec.rodapeTexto(), base.rodapeTexto()),
                spec.usaLogoEmDocumentos() == null ? base.usaLogoEmDocumentos() : spec.usaLogoEmDocumentos(),
                spec.usaAssinaturaCertificada() == null ? base.usaAssinaturaCertificada() : spec.usaAssinaturaCertificada()
        );
    }

    private PerfilInstanciaTribunalService.ConfiguracaoUx mergeUx(PerfilInstanciaTribunalService.ConfiguracaoUx base, UxSpec spec) {
        if (spec == null) {
            return base;
        }
        return new PerfilInstanciaTribunalService.ConfiguracaoUx(
                spec.exibeNupPadrao() == null ? base.exibeNupPadrao() : spec.exibeNupPadrao(),
                spec.exibeQrCodeNosDocumentos() == null ? base.exibeQrCodeNosDocumentos() : spec.exibeQrCodeNosDocumentos(),
                coalesce(spec.formatoNumeroLocal(), base.formatoNumeroLocal()),
                spec.habilitaChatProcesso() == null ? base.habilitaChatProcesso() : spec.habilitaChatProcesso(),
                spec.habilitaVideoAudiencia() == null ? base.habilitaVideoAudiencia() : spec.habilitaVideoAudiencia(),
                spec.habilitaAssinaturaDigital() == null ? base.habilitaAssinaturaDigital() : spec.habilitaAssinaturaDigital(),
                spec.habilitaNotificacaoWhatsApp() == null ? base.habilitaNotificacaoWhatsApp() : spec.habilitaNotificacaoWhatsApp(),
                spec.habilitaProcessoFisico() == null ? base.habilitaProcessoFisico() : spec.habilitaProcessoFisico(),
                coalesce(spec.fusoHorario(), base.fusoHorario()),
                coalesce(spec.formatoData(), base.formatoData()),
                coalesce(spec.moeda(), base.moeda()),
                spec.itensPorPaginaPadrao() == null ? base.itensPorPaginaPadrao() : spec.itensPorPaginaPadrao(),
                spec.modoEscuroDisponivel() == null ? base.modoEscuroDisponivel() : spec.modoEscuroDisponivel(),
                spec.exibeCalculadoraPrazos() == null ? base.exibeCalculadoraPrazos() : spec.exibeCalculadoraPrazos(),
                spec.vlibras() == null ? base.vlibras() : spec.vlibras(),
                coalesce(spec.nivelConformeWcag(), base.nivelConformeWcag())
        );
    }

    private PerfilInstanciaTribunalService.ContatoInstitucional mergeContato(PerfilInstanciaTribunalService.ContatoInstitucional base, ContatoSpec spec) {
        if (spec == null) {
            return base;
        }
        return new PerfilInstanciaTribunalService.ContatoInstitucional(
                coalesce(spec.site(), base.site()),
                coalesce(spec.email(), base.email()),
                coalesce(spec.telefone(), base.telefone()),
                coalesce(spec.endereco(), base.endereco()),
                coalesce(spec.cep(), base.cep()),
                coalesce(spec.cidade(), base.cidade()),
                coalesce(spec.uf(), base.uf()),
                coalesce(spec.horarioAtendimento(), base.horarioAtendimento()),
                coalesce(spec.ouvidoria(), base.ouvidoria())
        );
    }

    private PerfilInstanciaTribunalService.RamoJustica parsePerfilRamo(String perfilRamo,
                                                                        String tribunalCodigo,
                                                                        RamoDireito contexto,
                                                                        PerfilInstanciaTribunalService.RamoJustica fallback) {
        if (perfilRamo != null && !perfilRamo.isBlank()) {
            return PerfilInstanciaTribunalService.RamoJustica.valueOf(normalizeUpper(perfilRamo));
        }
        if (contexto != null) {
            return switch (contexto) {
                case TRABALHISTA -> PerfilInstanciaTribunalService.RamoJustica.TRABALHO;
                case ELEITORAL -> PerfilInstanciaTribunalService.RamoJustica.ELEITORAL;
                case MILITAR -> PerfilInstanciaTribunalService.RamoJustica.MILITAR_UNIAO;
                default -> inferirRamoPorTribunal(tribunalCodigo, fallback);
            };
        }
        return inferirRamoPorTribunal(tribunalCodigo, fallback);
    }

    private PerfilInstanciaTribunalService.RamoJustica inferirRamoPorTribunal(String tribunalCodigo,
                                                                               PerfilInstanciaTribunalService.RamoJustica fallback) {
        String codigo = blankToNull(tribunalCodigo) == null ? null : normalizeUpper(tribunalCodigo);
        if (codigo == null) {
            return fallback;
        }
        if (codigo.startsWith("STF")) {
            return PerfilInstanciaTribunalService.RamoJustica.STF;
        }
        if (codigo.startsWith("STJ")) {
            return PerfilInstanciaTribunalService.RamoJustica.SUPERIOR;
        }
        if (codigo.startsWith("TST") || codigo.startsWith("TRT")) {
            return PerfilInstanciaTribunalService.RamoJustica.TRABALHO;
        }
        if (codigo.startsWith("TRF")) {
            return PerfilInstanciaTribunalService.RamoJustica.FEDERAL;
        }
        if (codigo.startsWith("TRE") || codigo.startsWith("TSE")) {
            return PerfilInstanciaTribunalService.RamoJustica.ELEITORAL;
        }
        if (codigo.startsWith("STM")) {
            return PerfilInstanciaTribunalService.RamoJustica.MILITAR_UNIAO;
        }
        if (codigo.startsWith("TJM")) {
            return PerfilInstanciaTribunalService.RamoJustica.MILITAR_ESTADUAL;
        }
        if (codigo.startsWith("TJ")) {
            return PerfilInstanciaTribunalService.RamoJustica.ESTADUAL;
        }
        return fallback;
    }

    private PerfilInstanciaTribunalService.GrauInstancia parsePerfilGrau(String perfilGrau, GrauJurisdicao contexto, PerfilInstanciaTribunalService.GrauInstancia fallback) {
        if (perfilGrau != null && !perfilGrau.isBlank()) {
            return PerfilInstanciaTribunalService.GrauInstancia.valueOf(normalizeUpper(perfilGrau));
        }
        if (contexto == null) {
            return fallback;
        }
        return switch (contexto) {
            case PRIMEIRO_GRAU -> PerfilInstanciaTribunalService.GrauInstancia.PRIMEIRO_GRAU;
            case SEGUNDO_GRAU -> PerfilInstanciaTribunalService.GrauInstancia.SEGUNDO_GRAU;
            case SUPERIOR -> PerfilInstanciaTribunalService.GrauInstancia.TRIBUNAL_SUPERIOR;
            case CONSTITUCIONAL -> PerfilInstanciaTribunalService.GrauInstancia.STF;
            default -> fallback;
        };
    }

    static TribunalRuleEngine.NivelRegra parseNivel(String valor) {
        return TribunalRuleEngine.NivelRegra.valueOf(normalizeUpper(requireText(valor, "nivelRegra")));
    }

    static TribunalRuleEngine.TipoValor parseTipoValor(String valor) {
        return TribunalRuleEngine.TipoValor.valueOf(normalizeUpper(requireText(valor, "tipoValor")));
    }

    static TribunalRuleEngine.ModoSobrescrita parseModo(String valor) {
        return TribunalRuleEngine.ModoSobrescrita.valueOf(normalizeUpper(blankToDefault(valor, "SUBSTITUIR")));
    }

    static CalendarioForenseTribunalService.TipoEntrada parseTipoEntradaCalendario(String valor) {
        return CalendarioForenseTribunalService.TipoEntrada.valueOf(normalizeUpper(requireText(valor, "calendario.tipo")));
    }

    static CalendarioForenseTribunalService.Recorrencia parseRecorrenciaCalendario(String valor) {
        return CalendarioForenseTribunalService.Recorrencia.valueOf(normalizeUpper(blankToDefault(valor, "UNICA")));
    }

    static RamoDireito parseRamo(String valor) {
        return blankToNull(valor) == null ? null : RamoDireito.fromString(valor);
    }

    static GrauJurisdicao parseGrau(String valor) {
        return blankToNull(valor) == null ? null : GrauJurisdicao.fromString(valor);
    }

    static Instant parseInstant(String valor) {
        return blankToNull(valor) == null ? null : Instant.parse(valor.trim());
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

    private static <T> T coalesce(T value, T fallback) {
        return value != null ? value : fallback;
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

    private static String normalizePluginId(String pluginId) {
        return TOKEN_SPLIT.matcher(normalizeUpper(requireText(pluginId, "pluginId"))).replaceAll("_").replaceAll("_+", "_");
    }

    ObjectMapper objectMapper() {
        return objectMapper;
    }

    String canonicalRuleCode(String tribunalCodigo, String pluginId, String codigo) {
        return String.join("::",
                blankToDefault(tribunalCodigo, "TRIBUNAL"),
                blankToDefault(pluginId, "PLUGIN"),
                blankToDefault(codigo, "REGRA"));
    }
}
