package com.tcc.pjb.backend.tribunal.perfil;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

@Service
public class PerfilInstanciaTribunalService {

    public enum RamoJustica {
        ESTADUAL,
        FEDERAL,
        TRABALHO,
        ELEITORAL,
        MILITAR_ESTADUAL,
        MILITAR_UNIAO,
        SUPERIOR,
        STF
    }

    public enum GrauInstancia {
        PRIMEIRO_GRAU,
        SEGUNDO_GRAU,
        TRIBUNAL_SUPERIOR,
        STF
    }

    public enum TermoPadrao {
        AUTOR("Autor"),
        REU("Réu"),
        REQUERENTE("Requerente"),
        REQUERIDO("Requerido"),
        IMPETRANTE("Impetrante"),
        IMPETRADO("Impetrado"),
        TERCEIRO_INTERESSADO("Terceiro Interessado"),
        ASSISTENTE_LITISCONSORCIAL("Assistente Litisconsorcial"),
        JUIZ("Juiz"),
        JUIZ_TITULAR("Juiz Titular"),
        JUIZ_SUBSTITUTO("Juiz Substituto"),
        DESEMBARGADOR("Desembargador"),
        MINISTRO("Ministro"),
        RELATOR("Relator"),
        REVISOR("Revisor"),
        VARA("Vara"),
        CAMARA("Câmara"),
        TURMA("Turma"),
        SECAO("Seção"),
        PLENARIO("Plenário"),
        COMARCA("Comarca"),
        SUBSECAO("Subseção Judiciária"),
        FORO("Foro"),
        SERVIDOR_FORUM("Servidor do Fórum"),
        ESCRIVAO("Escrivão"),
        OFICIAL_JUSTICA("Oficial de Justiça"),
        DISTRIBUIDOR("Distribuidor"),
        PROTOCOLO("Protocolo"),
        SECRETARIA("Secretaria"),
        CARTORIO("Cartório"),
        CENTRAL_MANDADOS("Central de Mandados"),
        PETICAO_INICIAL("Petição Inicial"),
        CONTESTACAO("Contestação"),
        REPLICA("Réplica"),
        SENTENCA("Sentença"),
        ACORDAO("Acórdão"),
        DESPACHO("Despacho"),
        DECISAO_INTERLOCUTORIA("Decisão Interlocutória"),
        EMBARGOS_DECLARACAO("Embargos de Declaração"),
        MANDADO("Mandado"),
        CARTA_PRECATORIA("Carta Precatória"),
        CARTA_ROGATORIA("Carta Rogatória"),
        EDITAL("Edital"),
        CERTIDAO("Certidão"),
        AUDIENCIA("Audiência"),
        AUDIENCIA_CONCILIACAO("Audiência de Conciliação"),
        AUDIENCIA_INSTRUCAO("Audiência de Instrução"),
        SESSAO_JULGAMENTO("Sessão de Julgamento"),
        SUSTENTACAO_ORAL("Sustentação Oral"),
        APELACAO("Apelação"),
        AGRAVO("Agravo"),
        RECURSO_ESPECIAL("Recurso Especial"),
        RECURSO_EXTRAORDINARIO("Recurso Extraordinário"),
        EMBARGOS_DIVERGENCIA("Embargos de Divergência");

        private final String padrao;

        TermoPadrao(String padrao) {
            this.padrao = padrao;
        }

        public String termoPadrao() {
            return padrao;
        }

        public static Optional<TermoPadrao> fromKey(String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            String token = normalizeToken(value);
            for (TermoPadrao termo : values()) {
                if (termo.name().equals(token)) {
                    return Optional.of(termo);
                }
            }
            return Optional.empty();
        }
    }

    public record IdentidadeVisual(
            String corPrimaria,
            String corSecundaria,
            String corAcento,
            String corTextoSobrePrimaria,
            String corFundo,
            String brasaoUrl,
            String logoHorizontalUrl,
            String faviconUrl,
            String fonteInstitucional,
            String rodapeTexto,
            boolean usaLogoEmDocumentos,
            boolean usaAssinaturaCertificada
    ) {
        public IdentidadeVisual {
            corPrimaria = normalizeHex(corPrimaria, "#1A3A6B");
            corSecundaria = normalizeHex(corSecundaria, "#2E5FA3");
            corAcento = normalizeHex(corAcento, "#C8A951");
            corTextoSobrePrimaria = normalizeHex(corTextoSobrePrimaria, "#FFFFFF");
            corFundo = normalizeHex(corFundo, "#F8FAFC");
            brasaoUrl = blankToNull(brasaoUrl);
            logoHorizontalUrl = blankToNull(logoHorizontalUrl);
            faviconUrl = blankToNull(faviconUrl);
            fonteInstitucional = blankToDefault(fonteInstitucional, "Arial");
            rodapeTexto = blankToDefault(rodapeTexto, "PJB — Plataforma Judicial Brasileira");
        }

        public static IdentidadeVisual padrao() {
            return new IdentidadeVisual(
                    "#1A3A6B",
                    "#2E5FA3",
                    "#C8A951",
                    "#FFFFFF",
                    "#F8FAFC",
                    null,
                    null,
                    null,
                    "Arial",
                    "PJB — Plataforma Judicial Brasileira",
                    false,
                    false
            );
        }
    }

    public record ConfiguracaoUx(
            boolean exibeNupPadrao,
            boolean exibeQrCodeNosDocumentos,
            String formatoNumeroLocal,
            boolean habilitaChatProcesso,
            boolean habilitaVideoAudiencia,
            boolean habilitaAssinaturaDigital,
            boolean habilitaNotificacaoWhatsApp,
            boolean habilitaProcessoFisico,
            String fusoHorario,
            String formatoData,
            String moeda,
            int itensPorPaginaPadrao,
            boolean modoEscuroDisponivel,
            boolean exibeCalculadoraPrazos,
            boolean vlibras,
            String nivelConformeWcag
    ) {
        public ConfiguracaoUx {
            fusoHorario = validateZone(blankToDefault(fusoHorario, "America/Sao_Paulo"));
            formatoNumeroLocal = blankToNull(formatoNumeroLocal);
            formatoData = blankToDefault(formatoData, "dd/MM/yyyy");
            moeda = blankToDefault(moeda, "BRL");
            itensPorPaginaPadrao = itensPorPaginaPadrao <= 0 ? 20 : itensPorPaginaPadrao;
            nivelConformeWcag = normalizeWcag(nivelConformeWcag);
        }

        public static ConfiguracaoUx padrao() {
            return new ConfiguracaoUx(
                    true,
                    true,
                    null,
                    false,
                    false,
                    true,
                    false,
                    false,
                    "America/Sao_Paulo",
                    "dd/MM/yyyy",
                    "BRL",
                    20,
                    true,
                    true,
                    true,
                    "AA"
            );
        }
    }

    public record ContatoInstitucional(
            String site,
            String email,
            String telefone,
            String endereco,
            String cep,
            String cidade,
            String uf,
            String horarioAtendimento,
            String ouvidoria
    ) {
        public ContatoInstitucional {
            site = blankToNull(site);
            email = blankToNull(email);
            telefone = blankToNull(telefone);
            endereco = blankToNull(endereco);
            cep = blankToNull(cep);
            cidade = blankToNull(cidade);
            uf = normalizeUf(uf);
            horarioAtendimento = blankToNull(horarioAtendimento);
            ouvidoria = blankToNull(ouvidoria);
        }

        public static ContatoInstitucional vazio() {
            return new ContatoInstitucional(null, null, null, null, null, null, null, null, null);
        }
    }

    public record PerfilInstancia(
            String tribunalCodigo,
            String tribunalNome,
            String tribunalSigla,
            String uf,
            RamoJustica ramo,
            GrauInstancia grau,
            IdentidadeVisual visual,
            Map<TermoPadrao, String> terminologia,
            Map<TermoPadrao, String> terminologiaPlural,
            ConfiguracaoUx ux,
            ContatoInstitucional contato
    ) {
        public PerfilInstancia {
            tribunalCodigo = normalizeUpper(requireText(tribunalCodigo, "tribunalCodigo"));
            tribunalNome = requireText(tribunalNome, "tribunalNome");
            tribunalSigla = normalizeUpper(requireText(tribunalSigla, "tribunalSigla"));
            uf = normalizeUf(uf);
            if (ramo == null) throw new NullPointerException("ramo");
            if (grau == null) throw new NullPointerException("grau");
            visual = visual == null ? IdentidadeVisual.padrao() : visual;
            terminologia = immutableEnumMap(terminologia);
            terminologiaPlural = immutableEnumMap(terminologiaPlural);
            ux = ux == null ? ConfiguracaoUx.padrao() : ux;
            contato = contato == null ? ContatoInstitucional.vazio() : contato;
        }

        public String termo(TermoPadrao padrao) {
            return terminologia.getOrDefault(Objects.requireNonNull(padrao), padrao.termoPadrao());
        }

        public String termoPl(TermoPadrao padrao) {
            String custom = terminologiaPlural.get(padrao);
            if (custom != null && !custom.isBlank()) {
                return custom;
            }
            return pluralizar(termo(padrao));
        }

        public boolean temTermoPersonalizado(TermoPadrao padrao) {
            return terminologia.containsKey(padrao);
        }

        public int totalTermosPersonalizados() {
            return terminologia.size() + terminologiaPlural.size();
        }

        public Map<String, String> bindingsDocumento() {
            LinkedHashMap<String, String> bindings = new LinkedHashMap<>();
            bindings.put("TRIBUNAL_CODIGO", tribunalCodigo);
            bindings.put("TRIBUNAL_NOME", tribunalNome);
            bindings.put("TRIBUNAL_SIGLA", tribunalSigla);
            bindings.put("TRIBUNAL_UF", uf == null ? "" : uf);
            bindings.put("TRIBUNAL_FUSO", ux.fusoHorario());
            bindings.put("TRIBUNAL_FORMATO_DATA", ux.formatoData());
            bindings.put("TRIBUNAL_MOEDA", ux.moeda());
            for (TermoPadrao termo : TermoPadrao.values()) {
                bindings.put(termo.name(), termo(termo));
                bindings.put(termo.name() + "_PL", termoPl(termo));
            }
            return Collections.unmodifiableMap(bindings);
        }
    }

    public record DiferencaTerminologica(
            TermoPadrao padrao,
            String termoPadrao,
            String termoA,
            String termoB,
            String tribunalA,
            String tribunalB,
            boolean aPersonalizado,
            boolean bPersonalizado
    ) {
        public String descricao() {
            return padrao.name() + ": " + tribunalA + " usa '" + termoA + "'" + (aPersonalizado ? " (custom)" : " (padrão)")
                    + ", " + tribunalB + " usa '" + termoB + "'" + (bPersonalizado ? " (custom)" : " (padrão)");
        }
    }

    public record ResumoPerfil(
            String tribunalCodigo,
            String tribunalNome,
            String tribunalSigla,
            String uf,
            RamoJustica ramo,
            GrauInstancia grau,
            int totalTermosPersonalizados,
            boolean temPerfilEspecifico,
            String perfilFallbackUsado,
            String fusoHorario,
            boolean habilitaVideoAudiencia,
            boolean habilitaNotificacaoWhatsApp,
            boolean usaLogoEmDocumentos,
            int itensPorPaginaPadrao
    ) {}

    private record PluginBinding(String pluginKey, String tribunalCodigo) {}

    private static final Pattern HEX_COLOR = Pattern.compile("^#?[0-9A-Fa-f]{1,6}$");
    private final Map<String, PerfilInstancia> perfis = new ConcurrentHashMap<>();
    private final Map<String, PerfilInstancia> perfisBase = new ConcurrentHashMap<>();
    private final Map<String, PluginBinding> pluginsPerfil = new ConcurrentHashMap<>();
    private final ReentrantLock profileMutationLock = new ReentrantLock();
    private volatile String tribunalAtivo;

    @PostConstruct
    public void init() {
        if (perfisBase.isEmpty()) {
            seedPerfisBasicos();
        }
    }

    public void cadastrar(PerfilInstancia perfil) {
        profileMutationLock.lock();
        try {
            PerfilInstancia normalized = Objects.requireNonNull(perfil);
            perfis.put(normalized.tribunalCodigo(), normalized);
            perfisBase.put(normalized.tribunalCodigo(), normalized);
            if (tribunalAtivo == null) {
                tribunalAtivo = normalized.tribunalCodigo();
            }
        } finally {
            profileMutationLock.unlock();
        }
    }

    public void substituirPerfilPlugin(String pluginKey, PerfilInstancia perfil) {
        profileMutationLock.lock();
        try {
            String key = normalizeUpper(requireText(pluginKey, "pluginKey"));
            removerPerfilPluginInterno(key);
            PerfilInstancia normalized = Objects.requireNonNull(perfil);
            removerBindingsPluginMesmoTribunal(normalized.tribunalCodigo(), key);
            perfis.put(normalized.tribunalCodigo(), normalized);
            pluginsPerfil.put(key, new PluginBinding(key, normalized.tribunalCodigo()));
        } finally {
            profileMutationLock.unlock();
        }
    }

    public void removerPerfilPlugin(String pluginKey) {
        profileMutationLock.lock();
        try {
            removerPerfilPluginInterno(pluginKey);
        } finally {
            profileMutationLock.unlock();
        }
    }

    private void removerPerfilPluginInterno(String pluginKey) {
        String key = normalizeUpper(requireText(pluginKey, "pluginKey"));
        PluginBinding binding = pluginsPerfil.remove(key);
        if (binding == null) {
            return;
        }
        PerfilInstancia base = perfisBase.get(binding.tribunalCodigo());
        if (base != null) {
            perfis.put(binding.tribunalCodigo(), base);
        } else {
            perfis.remove(binding.tribunalCodigo());
        }
        if (Objects.equals(tribunalAtivo, binding.tribunalCodigo()) && !perfis.containsKey(binding.tribunalCodigo())) {
            tribunalAtivo = perfis.keySet().stream().sorted().findFirst().orElse(null);
        }
    }

    private void removerBindingsPluginMesmoTribunal(String tribunalCodigo, String pluginKeyPreservado) {
        if (tribunalCodigo == null || tribunalCodigo.isBlank()) {
            return;
        }
        String tribunal = normalizeUpper(tribunalCodigo);
        pluginsPerfil.entrySet().removeIf(entry -> !Objects.equals(entry.getKey(), pluginKeyPreservado)
                && entry.getValue() != null
                && Objects.equals(entry.getValue().tribunalCodigo(), tribunal));
    }

    public void definirTribunalAtivo(String codigo) {
        String tribunal = normalizeUpper(requireText(codigo, "codigo"));
        if (resolverInterno(tribunal).isEmpty()) {
            throw new IllegalStateException("Tribunal não cadastrado: " + tribunal);
        }
        this.tribunalAtivo = tribunal;
    }

    public Optional<PerfilInstancia> perfilAtivo() {
        return tribunalAtivo == null ? Optional.empty() : resolverInterno(tribunalAtivo);
    }

    public Optional<PerfilInstancia> porCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        return resolverInterno(codigo);
    }

    public PerfilInstancia resolverPorCodigoOuPadrao(String codigo) {
        return resolverInterno(codigo).orElseGet(() -> perfisBase.get("PJB_PADRAO"));
    }

    public String termo(TermoPadrao padrao) {
        return perfilAtivo().map(item -> item.termo(padrao)).orElse(padrao.termoPadrao());
    }

    public String termo(String tribunalCodigo, TermoPadrao padrao) {
        return resolverPorCodigoOuPadrao(tribunalCodigo).termo(padrao);
    }

    public String termoPlural(TermoPadrao padrao) {
        return perfilAtivo().map(item -> item.termoPl(padrao)).orElse(pluralizar(padrao.termoPadrao()));
    }

    public String termoPlural(String tribunalCodigo, TermoPadrao padrao) {
        return resolverPorCodigoOuPadrao(tribunalCodigo).termoPl(padrao);
    }

    public ResumoPerfil resumo(String codigo) {
        PerfilInstancia perfil = resolverPorCodigoOuPadrao(codigo);
        Optional<String> fallback = codigo == null || codigo.isBlank() ? Optional.of("PJB_PADRAO") : fallbackCode(codigo);
        boolean especifico = codigo != null && !codigo.isBlank() && perfis.containsKey(normalizeUpper(codigo));
        return new ResumoPerfil(
                perfil.tribunalCodigo(),
                perfil.tribunalNome(),
                perfil.tribunalSigla(),
                perfil.uf(),
                perfil.ramo(),
                perfil.grau(),
                perfil.totalTermosPersonalizados(),
                especifico,
                especifico ? null : fallback.orElse("PJB_PADRAO"),
                perfil.ux().fusoHorario(),
                perfil.ux().habilitaVideoAudiencia(),
                perfil.ux().habilitaNotificacaoWhatsApp(),
                perfil.visual().usaLogoEmDocumentos(),
                perfil.ux().itensPorPaginaPadrao()
        );
    }

    public Map<String, String> bindingsDocumento(String tribunalCodigo) {
        return resolverPorCodigoOuPadrao(tribunalCodigo).bindingsDocumento();
    }

    public String aplicarTerminologia(String texto, String tribunalCodigo) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        String result = texto;
        Map<String, String> bindings = bindingsDocumento(tribunalCodigo);
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    public Map<String, DiferencaTerminologica> compararTerminologia(String codigoA, String codigoB) {
        PerfilInstancia a = resolverPorCodigoOuPadrao(codigoA);
        PerfilInstancia b = resolverPorCodigoOuPadrao(codigoB);
        LinkedHashMap<String, DiferencaTerminologica> difs = new LinkedHashMap<>();
        for (TermoPadrao termo : TermoPadrao.values()) {
            String termoA = a.termo(termo);
            String termoB = b.termo(termo);
            if (!Objects.equals(termoA, termoB)) {
                difs.put(termo.name(), new DiferencaTerminologica(
                        termo,
                        termo.termoPadrao(),
                        termoA,
                        termoB,
                        a.tribunalCodigo(),
                        b.tribunalCodigo(),
                        a.temTermoPersonalizado(termo),
                        b.temTermoPersonalizado(termo)
                ));
            }
        }
        return Collections.unmodifiableMap(difs);
    }

    public List<PerfilInstancia> listarTodos() {
        return perfis.values().stream()
                .sorted(Comparator.comparing(PerfilInstancia::tribunalSigla).thenComparing(PerfilInstancia::tribunalCodigo))
                .toList();
    }

    public List<Map.Entry<String, Integer>> rankingPersonalizacao() {
        return perfis.values().stream()
                .map(item -> Map.entry(item.tribunalCodigo(), item.totalTermosPersonalizados()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .toList();
    }

    public List<DiferencaTerminologica> todasAsDiferencas() {
        List<PerfilInstancia> lista = listarTodos();
        List<DiferencaTerminologica> resultado = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            for (int j = i + 1; j < lista.size(); j++) {
                resultado.addAll(compararTerminologia(lista.get(i).tribunalCodigo(), lista.get(j).tribunalCodigo()).values());
            }
        }
        return Collections.unmodifiableList(resultado);
    }

    public void seedPerfisBasicos() {
        cadastrarInterno(criarPerfilPadrao());
        cadastrarInterno(criarPerfilStf());
        cadastrarInterno(criarPerfilStj());
        cadastrarInterno(criarPerfilTst());
        cadastrarInterno(criarPerfilTjGenerico());
        cadastrarInterno(criarPerfilTrfGenerico());
        cadastrarInterno(criarPerfilTrtGenerico());
        cadastrarInterno(criarPerfilTjsp());
        cadastrarInterno(criarPerfilTjrj());
        cadastrarInterno(criarPerfilTjam());
        cadastrarInterno(criarPerfilTrt8());
        if (tribunalAtivo == null) {
            tribunalAtivo = "PJB_PADRAO";
        }
    }

    private void cadastrarInterno(PerfilInstancia perfil) {
        perfis.put(perfil.tribunalCodigo(), perfil);
        perfisBase.put(perfil.tribunalCodigo(), perfil);
    }

    private Optional<PerfilInstancia> resolverInterno(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.ofNullable(perfisBase.get("PJB_PADRAO"));
        }
        String tribunal = normalizeUpper(codigo);
        PerfilInstancia direto = perfis.get(tribunal);
        if (direto != null) {
            return Optional.of(direto);
        }
        return fallbackCode(tribunal).map(perfisBase::get).or(() -> Optional.ofNullable(perfisBase.get("PJB_PADRAO")));
    }

    private Optional<String> fallbackCode(String codigo) {
        String tribunal = normalizeUpper(codigo);
        if (tribunal.startsWith("TJ")) {
            return Optional.of("TJ_GENERICO");
        }
        if (tribunal.startsWith("TRF")) {
            return Optional.of("TRF_GENERICO");
        }
        if (tribunal.startsWith("TRT")) {
            return Optional.of("TRT_GENERICO");
        }
        if (Set.of("STF", "STJ", "TST").contains(tribunal)) {
            return Optional.of(tribunal);
        }
        return Optional.of("PJB_PADRAO");
    }

    private static PerfilInstancia criarPerfilPadrao() {
        return new PerfilInstancia(
                "PJB_PADRAO",
                "Plataforma Judicial Brasileira",
                "PJB",
                null,
                RamoJustica.ESTADUAL,
                GrauInstancia.PRIMEIRO_GRAU,
                IdentidadeVisual.padrao(),
                Map.of(),
                Map.of(),
                ConfiguracaoUx.padrao(),
                ContatoInstitucional.vazio()
        );
    }

    private static PerfilInstancia criarPerfilStf() {
        EnumMap<TermoPadrao, String> termos = new EnumMap<>(TermoPadrao.class);
        termos.put(TermoPadrao.AUTOR, "Requerente");
        termos.put(TermoPadrao.REU, "Requerido");
        termos.put(TermoPadrao.JUIZ, "Ministro");
        termos.put(TermoPadrao.SENTENCA, "Acórdão");
        termos.put(TermoPadrao.VARA, "Relatoria");
        termos.put(TermoPadrao.CAMARA, "Turma");
        termos.put(TermoPadrao.PLENARIO, "Plenário");
        termos.put(TermoPadrao.AUDIENCIA, "Sustentação Oral");
        termos.put(TermoPadrao.DISTRIBUIDOR, "Protocolo Geral");
        termos.put(TermoPadrao.SESSAO_JULGAMENTO, "Sessão Plenária");
        return new PerfilInstancia(
                "STF",
                "Supremo Tribunal Federal",
                "STF",
                "DF",
                RamoJustica.STF,
                GrauInstancia.STF,
                new IdentidadeVisual("#1B2F4E", "#2C5F8A", "#C8A951", "#FFFFFF", "#F5F5F0", "https://portal.stf.jus.br/brasao.png", null, null, "Arial", "Supremo Tribunal Federal — Praça dos Três Poderes, Brasília-DF", true, true),
                termos,
                Map.of(),
                new ConfiguracaoUx(true, true, null, false, true, true, false, false, "America/Sao_Paulo", "dd/MM/yyyy", "BRL", 10, true, false, true, "AA"),
                new ContatoInstitucional("https://portal.stf.jus.br", "stf@stf.jus.br", "(61) 3217-3000", "Praça dos Três Poderes", "70175-900", "Brasília", "DF", "Segunda a Sexta, 12h às 19h", "https://portal.stf.jus.br/ouvidoria")
        );
    }

    private static PerfilInstancia criarPerfilStj() {
        EnumMap<TermoPadrao, String> termos = new EnumMap<>(TermoPadrao.class);
        termos.put(TermoPadrao.JUIZ, "Ministro");
        termos.put(TermoPadrao.SENTENCA, "Acórdão");
        termos.put(TermoPadrao.VARA, "Relatoria");
        termos.put(TermoPadrao.CAMARA, "Turma");
        termos.put(TermoPadrao.SECAO, "Seção");
        termos.put(TermoPadrao.PLENARIO, "Corte Especial");
        termos.put(TermoPadrao.DISTRIBUIDOR, "Protocolo Judiciário");
        return new PerfilInstancia(
                "STJ",
                "Superior Tribunal de Justiça",
                "STJ",
                "DF",
                RamoJustica.SUPERIOR,
                GrauInstancia.TRIBUNAL_SUPERIOR,
                new IdentidadeVisual("#003366", "#004A99", "#D4A830", "#FFFFFF", "#F0F4F8", null, null, null, "Arial", "Superior Tribunal de Justiça — SAFS Quadra 06, Brasília-DF", true, true),
                termos,
                Map.of(),
                ConfiguracaoUx.padrao(),
                new ContatoInstitucional("https://www.stj.jus.br", "stj@stj.jus.br", "(61) 3319-8000", "SAFS Quadra 06 Lote 1", "70095-900", "Brasília", "DF", "Segunda a Sexta, 12h às 18h", "https://www.stj.jus.br/ouvidoria")
        );
    }

    private static PerfilInstancia criarPerfilTst() {
        EnumMap<TermoPadrao, String> termos = new EnumMap<>(TermoPadrao.class);
        termos.put(TermoPadrao.AUTOR, "Reclamante");
        termos.put(TermoPadrao.REU, "Reclamado");
        termos.put(TermoPadrao.JUIZ, "Ministro");
        termos.put(TermoPadrao.PETICAO_INICIAL, "Reclamação Trabalhista");
        termos.put(TermoPadrao.CONTESTACAO, "Defesa");
        termos.put(TermoPadrao.VARA, "Vara do Trabalho");
        termos.put(TermoPadrao.COMARCA, "Região");
        termos.put(TermoPadrao.SUBSECAO, "Vara do Trabalho");
        termos.put(TermoPadrao.AUDIENCIA, "Audiência Trabalhista");
        termos.put(TermoPadrao.PROTOCOLO, "Protocolo PJe-JT");
        termos.put(TermoPadrao.DISTRIBUIDOR, "SECON — Secretaria de Controle");
        EnumMap<TermoPadrao, String> plurais = new EnumMap<>(TermoPadrao.class);
        plurais.put(TermoPadrao.AUTOR, "Reclamantes");
        plurais.put(TermoPadrao.REU, "Reclamados");
        return new PerfilInstancia(
                "TST",
                "Tribunal Superior do Trabalho",
                "TST",
                "DF",
                RamoJustica.TRABALHO,
                GrauInstancia.TRIBUNAL_SUPERIOR,
                new IdentidadeVisual("#1A3A5C", "#2E6DA4", "#E8A020", "#FFFFFF", "#F0F5FA", "https://www.tst.jus.br/brasao.png", null, null, "Arial", "Tribunal Superior do Trabalho — SGAS 702/904, Brasília-DF", true, true),
                termos,
                plurais,
                new ConfiguracaoUx(true, true, null, false, true, true, false, false, "America/Sao_Paulo", "dd/MM/yyyy", "BRL", 20, true, true, true, "AA"),
                new ContatoInstitucional("https://www.tst.jus.br", "presidencia@tst.jus.br", "(61) 3043-4000", "SGAS Qd 702/904", "70390-700", "Brasília", "DF", "Segunda a Sexta, 12h às 18h", "https://www.tst.jus.br/ouvidoria")
        );
    }

    private static PerfilInstancia criarPerfilTjGenerico() {
        EnumMap<TermoPadrao, String> termos = new EnumMap<>(TermoPadrao.class);
        termos.put(TermoPadrao.DISTRIBUIDOR, "Central de Distribuição");
        termos.put(TermoPadrao.PROTOCOLO, "Central de Protocolo");
        termos.put(TermoPadrao.CARTORIO, "Cartório Judicial");
        return new PerfilInstancia(
                "TJ_GENERICO",
                "Tribunal de Justiça",
                "TJ",
                null,
                RamoJustica.ESTADUAL,
                GrauInstancia.SEGUNDO_GRAU,
                new IdentidadeVisual("#1A3A6B", "#2E5FA3", "#C8A951", "#FFFFFF", "#F8FAFC", null, null, null, "Arial", "Tribunal de Justiça", false, false),
                termos,
                Map.of(),
                ConfiguracaoUx.padrao(),
                ContatoInstitucional.vazio()
        );
    }

    private static PerfilInstancia criarPerfilTrfGenerico() {
        EnumMap<TermoPadrao, String> termos = new EnumMap<>(TermoPadrao.class);
        termos.put(TermoPadrao.COMARCA, "Subseção Judiciária");
        termos.put(TermoPadrao.VARA, "Vara Federal");
        termos.put(TermoPadrao.SUBSECAO, "Subseção Judiciária");
        termos.put(TermoPadrao.DISTRIBUIDOR, "Protocolo eproc");
        termos.put(TermoPadrao.SERVIDOR_FORUM, "Técnico Judiciário");
        termos.put(TermoPadrao.ESCRIVAO, "Técnico Judiciário");
        termos.put(TermoPadrao.OFICIAL_JUSTICA, "Oficial de Justiça Avaliador Federal");
        return new PerfilInstancia(
                "TRF_GENERICO",
                "Tribunal Regional Federal",
                "TRF",
                null,
                RamoJustica.FEDERAL,
                GrauInstancia.SEGUNDO_GRAU,
                new IdentidadeVisual("#1C3A2A", "#2E6B4E", "#D4A830", "#FFFFFF", "#F0F7F0", null, null, null, "Arial", "Tribunal Regional Federal", false, false),
                termos,
                Map.of(),
                new ConfiguracaoUx(true, true, null, false, true, true, false, false, "America/Sao_Paulo", "dd/MM/yyyy", "BRL", 20, false, true, true, "AA"),
                ContatoInstitucional.vazio()
        );
    }

    private static PerfilInstancia criarPerfilTrtGenerico() {
        EnumMap<TermoPadrao, String> termos = new EnumMap<>(TermoPadrao.class);
        termos.put(TermoPadrao.AUTOR, "Reclamante");
        termos.put(TermoPadrao.REU, "Reclamado");
        termos.put(TermoPadrao.PETICAO_INICIAL, "Reclamação Trabalhista");
        termos.put(TermoPadrao.CONTESTACAO, "Defesa");
        termos.put(TermoPadrao.VARA, "Vara do Trabalho");
        termos.put(TermoPadrao.COMARCA, "Região");
        termos.put(TermoPadrao.PROTOCOLO, "Protocolo PJe-JT");
        return new PerfilInstancia(
                "TRT_GENERICO",
                "Tribunal Regional do Trabalho",
                "TRT",
                null,
                RamoJustica.TRABALHO,
                GrauInstancia.SEGUNDO_GRAU,
                new IdentidadeVisual("#1A3A5C", "#2E6DA4", "#E8A020", "#FFFFFF", "#F0F5FA", null, null, null, "Arial", "Tribunal Regional do Trabalho", false, false),
                termos,
                Map.of(),
                ConfiguracaoUx.padrao(),
                ContatoInstitucional.vazio()
        );
    }

    private static PerfilInstancia criarPerfilTjsp() {
        EnumMap<TermoPadrao, String> termos = new EnumMap<>(TermoPadrao.class);
        termos.put(TermoPadrao.DISTRIBUIDOR, "Distribuidor");
        termos.put(TermoPadrao.PROTOCOLO, "Protocolo Geral");
        termos.put(TermoPadrao.CARTORIO, "Cartório Judicial");
        return new PerfilInstancia(
                "TJSP",
                "Tribunal de Justiça do Estado de São Paulo",
                "TJSP",
                "SP",
                RamoJustica.ESTADUAL,
                GrauInstancia.SEGUNDO_GRAU,
                new IdentidadeVisual("#17375E", "#2E5FA3", "#C7A34B", "#FFFFFF", "#F7F9FC", null, null, null, "Arial", "Tribunal de Justiça do Estado de São Paulo", true, true),
                termos,
                Map.of(),
                new ConfiguracaoUx(true, true, null, true, true, true, true, true, "America/Sao_Paulo", "dd/MM/yyyy", "BRL", 30, true, true, true, "AA"),
                new ContatoInstitucional("https://www.tjsp.jus.br", null, null, "Praça da Sé", null, "São Paulo", "SP", null, null)
        );
    }

    private static PerfilInstancia criarPerfilTjrj() {
        EnumMap<TermoPadrao, String> termos = new EnumMap<>(TermoPadrao.class);
        termos.put(TermoPadrao.DISTRIBUIDOR, "Protocolo Geral");
        termos.put(TermoPadrao.PROTOCOLO, "Protocolo Geral");
        termos.put(TermoPadrao.CARTORIO, "Serventia Judicial");
        return new PerfilInstancia(
                "TJRJ",
                "Tribunal de Justiça do Estado do Rio de Janeiro",
                "TJRJ",
                "RJ",
                RamoJustica.ESTADUAL,
                GrauInstancia.SEGUNDO_GRAU,
                new IdentidadeVisual("#123B63", "#2B5F8A", "#C8A951", "#FFFFFF", "#F8FAFC", null, null, null, "Arial", "Tribunal de Justiça do Estado do Rio de Janeiro", true, true),
                termos,
                Map.of(),
                new ConfiguracaoUx(true, true, null, true, true, true, true, true, "America/Sao_Paulo", "dd/MM/yyyy", "BRL", 30, true, true, true, "AA"),
                new ContatoInstitucional("https://www.tjrj.jus.br", null, null, "Centro", null, "Rio de Janeiro", "RJ", null, null)
        );
    }

    private static PerfilInstancia criarPerfilTjam() {
        EnumMap<TermoPadrao, String> termos = new EnumMap<>(TermoPadrao.class);
        termos.put(TermoPadrao.VARA, "Vara Mista");
        termos.put(TermoPadrao.DISTRIBUIDOR, "Distribuição Judicial");
        return new PerfilInstancia(
                "TJAM",
                "Tribunal de Justiça do Estado do Amazonas",
                "TJAM",
                "AM",
                RamoJustica.ESTADUAL,
                GrauInstancia.SEGUNDO_GRAU,
                new IdentidadeVisual("#20466D", "#2C6B90", "#C8A951", "#FFFFFF", "#F5FAFF", null, null, null, "Arial", "Tribunal de Justiça do Estado do Amazonas", true, true),
                termos,
                Map.of(),
                new ConfiguracaoUx(true, true, null, true, true, true, true, true, "America/Manaus", "dd/MM/yyyy", "BRL", 20, true, true, true, "AA"),
                new ContatoInstitucional("https://www.tjam.jus.br", null, null, "Manaus", null, "Manaus", "AM", null, null)
        );
    }

    private static PerfilInstancia criarPerfilTrt8() {
        EnumMap<TermoPadrao, String> termos = new EnumMap<>(TermoPadrao.class);
        termos.put(TermoPadrao.AUTOR, "Reclamante");
        termos.put(TermoPadrao.REU, "Reclamado");
        termos.put(TermoPadrao.VARA, "Vara do Trabalho");
        termos.put(TermoPadrao.COMARCA, "Região");
        termos.put(TermoPadrao.PROTOCOLO, "Protocolo PJe-JT");
        return new PerfilInstancia(
                "TRT8",
                "Tribunal Regional do Trabalho da 8ª Região",
                "TRT8",
                "PA",
                RamoJustica.TRABALHO,
                GrauInstancia.SEGUNDO_GRAU,
                new IdentidadeVisual("#1A3A5C", "#2E6DA4", "#E8A020", "#FFFFFF", "#F0F5FA", null, null, null, "Arial", "Tribunal Regional do Trabalho da 8ª Região", true, true),
                termos,
                Map.of(),
                new ConfiguracaoUx(true, true, null, true, true, true, true, false, "America/Belem", "dd/MM/yyyy", "BRL", 20, true, true, true, "AA"),
                new ContatoInstitucional("https://www.trt8.jus.br", null, null, "Belém", null, "Belém", "PA", null, null)
        );
    }

    private static Map<TermoPadrao, String> immutableEnumMap(Map<TermoPadrao, String> source) {
        EnumMap<TermoPadrao, String> target = new EnumMap<>(TermoPadrao.class);
        if (source != null) {
            source.forEach((key, value) -> {
                if (key != null && value != null && !value.isBlank()) {
                    target.put(key, value.trim());
                }
            });
        }
        return Collections.unmodifiableMap(target);
    }

    private static String pluralizar(String singular) {
        String value = requireText(singular, "singular");
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.endsWith("ão")) {
            return value.substring(0, value.length() - 2) + "ões";
        }
        if (lower.endsWith("m")) {
            return value.substring(0, value.length() - 1) + "ns";
        }
        if (lower.endsWith("r") || lower.endsWith("z") || lower.endsWith("s")) {
            return value + "es";
        }
        return value + "s";
    }

    private static String normalizeWcag(String value) {
        String token = normalizeUpper(blankToDefault(value, "AA"));
        return Set.of("A", "AA", "AAA").contains(token) ? token : "AA";
    }

    private static String validateZone(String zone) {
        return ZoneId.of(zone).getId();
    }

    private static String normalizeHex(String color, String fallback) {
        String raw = blankToDefault(color, fallback).trim();
        if (!HEX_COLOR.matcher(raw).matches()) {
            throw new IllegalArgumentException("Cor hexadecimal inválida: " + raw);
        }
        String value = raw.startsWith("#") ? raw.substring(1) : raw;
        if (value.length() == 1) {
            value = value.repeat(6);
        } else if (value.length() == 2) {
            value = value.repeat(3);
        } else if (value.length() == 3) {
            return "#" + value.toUpperCase(Locale.ROOT);
        } else if (value.length() < 6) {
            value = String.format(Locale.ROOT, "%-6s", value).replace(' ', '0');
        }
        return "#" + value.toUpperCase(Locale.ROOT);
    }

    private static String normalizeUf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String uf = value.trim().toUpperCase(Locale.ROOT);
        return uf.length() > 2 ? uf.substring(0, 2) : uf;
    }

    private static String normalizeUpper(String value) {
        return requireText(value, "value").trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeToken(String value) {
        return requireText(value, "value").trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String blankToDefault(String value, String fallback) {
        return blankToNull(value) == null ? fallback : value.trim();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + field);
        }
        return value.trim();
    }
}
