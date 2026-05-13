package com.tcc.pjb.backend.tribunal.calendario;

import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseEntry;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.JurisdicaoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class CalendarioForenseTribunalService {

    public enum TipoEntrada {
        FIM_DE_SEMANA,
        FERIADO_NACIONAL,
        FERIADO_ESTADUAL,
        FERIADO_MUNICIPAL,
        FERIADO_MOVEL,
        RECESSO_JUDICIAL,
        RECESSO_CARNAVAL,
        SUSPENSAO_PORTARIA,
        PONTO_FACULTATIVO,
        PLANTAO_JUDICIARIO,
        MUTIRAO_ESPECIAL
    }

    public enum Recorrencia {
        UNICA,
        ANUAL_FIXO,
        ANUAL_MOVEL,
        SEMANAL
    }

    public record ContextoCalendario(
            String tribunalCodigo,
            String uf,
            String comarca,
            RamoDireito ramo,
            GrauJurisdicao grau,
            LocalDate referencia
    ) {
        public ContextoCalendario {
            tribunalCodigo = normalizeTribunal(tribunalCodigo);
            uf = normalizeUf(uf);
            comarca = normalizeText(comarca);
            referencia = referencia == null ? LocalDate.now() : referencia;
        }

        public static ContextoCalendario of(String tribunalCodigo) {
            return new ContextoCalendario(tribunalCodigo, null, null, null, null, LocalDate.now());
        }

        public static ContextoCalendario of(String tribunalCodigo, String uf, String comarca) {
            return new ContextoCalendario(tribunalCodigo, uf, comarca, null, null, LocalDate.now());
        }
    }

    public record EntradaCalendario(
            String tribunalCodigo,
            LocalDate data,
            TipoEntrada tipo,
            String descricao,
            boolean suspendeExpediente,
            boolean suspendePrazos,
            Recorrencia recorrencia,
            String fundamentacao,
            String abrangencia,
            String uf,
            String comarca,
            String origemId
    ) {
        public EntradaCalendario {
            tribunalCodigo = normalizeTribunal(tribunalCodigo);
            Objects.requireNonNull(data, "data");
            Objects.requireNonNull(tipo, "tipo");
            descricao = normalizeText(descricao);
            recorrencia = recorrencia == null ? Recorrencia.UNICA : recorrencia;
            fundamentacao = normalizeNullable(fundamentacao);
            abrangencia = normalizeNullable(abrangencia);
            uf = normalizeUf(uf);
            comarca = normalizeText(comarca);
            origemId = normalizeNullable(origemId);
        }

        public boolean ocorreEm(LocalDate consulta) {
            if (consulta == null) {
                return false;
            }
            return switch (recorrencia) {
                case UNICA, ANUAL_MOVEL -> data.equals(consulta);
                case ANUAL_FIXO -> MonthDay.from(data).equals(MonthDay.from(consulta));
                case SEMANAL -> data.getDayOfWeek() == consulta.getDayOfWeek();
            };
        }

        public boolean aplicaAo(ContextoCalendario contexto, LocalDate consulta) {
            if (!ocorreEm(consulta)) {
                return false;
            }
            if (!matchesTribunal(tribunalCodigo, contexto.tribunalCodigo())) {
                return false;
            }
            if (uf != null && !Objects.equals(uf, contexto.uf())) {
                return false;
            }
            if (comarca != null && !Objects.equals(comarca, contexto.comarca())) {
                return false;
            }
            return matchesAbrangencia(abrangencia, contexto);
        }
    }

    public record PeriodoRecesso(
            String tribunalCodigo,
            String descricao,
            LocalDate inicio,
            LocalDate fim,
            boolean suspendePrazos,
            String fundamentacao,
            String uf,
            String comarca,
            String origemId
    ) {
        public PeriodoRecesso {
            tribunalCodigo = normalizeTribunal(tribunalCodigo);
            descricao = normalizeText(descricao);
            Objects.requireNonNull(inicio, "inicio");
            Objects.requireNonNull(fim, "fim");
            if (fim.isBefore(inicio)) {
                throw new IllegalArgumentException("Periodo de recesso inválido");
            }
            fundamentacao = normalizeNullable(fundamentacao);
            uf = normalizeUf(uf);
            comarca = normalizeText(comarca);
            origemId = normalizeNullable(origemId);
        }

        public boolean aplicaAo(ContextoCalendario contexto, LocalDate data) {
            if (data == null || data.isBefore(inicio) || data.isAfter(fim)) {
                return false;
            }
            if (!matchesTribunal(tribunalCodigo, contexto.tribunalCodigo())) {
                return false;
            }
            if (uf != null && !Objects.equals(uf, contexto.uf())) {
                return false;
            }
            if (comarca != null && !Objects.equals(comarca, contexto.comarca())) {
                return false;
            }
            return true;
        }

        public long duracaoDias() {
            return ChronoUnit.DAYS.between(inicio, fim) + 1;
        }
    }

    public record DiaForense(
            LocalDate data,
            boolean diaUtil,
            String motivo,
            TipoEntrada tipoEntrada
    ) {}

    public record DiaDiferido(LocalDate data, String motivo, TipoEntrada tipoEntrada) {}

    public record PrazoCalculado(
            LocalDate dataEvento,
            LocalDate dataInicioConta,
            int diasUteisContratados,
            LocalDate dataVencimento,
            String tribunalCodigo,
            String uf,
            String comarca,
            List<DiaDiferido> diasDescartados,
            String resumo,
            String fundamentacao
    ) {
        public int totalDiasDescontados() {
            return diasDescartados.size();
        }

        public int totalDiasCorridos() {
            return (int) ChronoUnit.DAYS.between(dataInicioConta, dataVencimento) + 1;
        }
    }

    public record ResumoCalendarioAno(
            String tribunal,
            String uf,
            String comarca,
            int ano,
            int totalDias,
            int diasUteis,
            int diasNaoUteis,
            List<EntradaCalendario> feriados,
            List<PeriodoRecesso> recessos
    ) {}

    private record PluginCalendarioState(List<EntradaCalendario> entradas, List<PeriodoRecesso> recessos) {}

    private final TribunalRuleEngine tribunalRuleEngine;
    private final NationalPrazoEngine nationalPrazoEngine;
    private final CalendarioForenseRepository calendarioForenseRepository;
    private final JurisdicaoRepository jurisdicaoRepository;

    private static final int MAX_FERIADOS_MOVEIS_CACHE_ENTRIES = 48;
    private static final int RETAIN_MOVABLE_YEAR_BEFORE = 6;
    private static final int RETAIN_MOVABLE_YEAR_AFTER = 6;
    private static final int MAX_PROCESSED_SEEDS = 2_048;
    private static final long FERIADOS_MOVEIS_CLEANUP_INTERVAL_NANOS = java.time.Duration.ofMinutes(10).toNanos();

    private final Map<String, CopyOnWriteArrayList<EntradaCalendario>> entradasPorTribunal = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<PeriodoRecesso>> recessosPorTribunal = new ConcurrentHashMap<>();
    private final Map<Integer, Map<LocalDate, EntradaCalendario>> feriadosMoveisCache = new ConcurrentHashMap<>();
    private final Map<String, PluginCalendarioState> pluginsRegistrados = new ConcurrentHashMap<>();
    private final AtomicLong nextMovableCleanupAtNanos = new AtomicLong(System.nanoTime() + FERIADOS_MOVEIS_CLEANUP_INTERVAL_NANOS);
    private final ReentrantLock pluginMutationLock = new ReentrantLock();
    private final Set<String> seedsProcessados = ConcurrentHashMap.newKeySet();

    public CalendarioForenseTribunalService(TribunalRuleEngine tribunalRuleEngine,
                                            NationalPrazoEngine nationalPrazoEngine,
                                            CalendarioForenseRepository calendarioForenseRepository,
                                            JurisdicaoRepository jurisdicaoRepository) {
        this.tribunalRuleEngine = Objects.requireNonNull(tribunalRuleEngine);
        this.nationalPrazoEngine = Objects.requireNonNull(nationalPrazoEngine);
        this.calendarioForenseRepository = Objects.requireNonNull(calendarioForenseRepository);
        this.jurisdicaoRepository = Objects.requireNonNull(jurisdicaoRepository);
    }

    public void cadastrarEntrada(EntradaCalendario entrada) {
        Objects.requireNonNull(entrada, "entrada");
        String tribunal = Objects.requireNonNull(entrada.tribunalCodigo(), "tribunalCodigo");
        entradasPorTribunal.computeIfAbsent(tribunal, key -> new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<EntradaCalendario> bucket = entradasPorTribunal.get(tribunal);
        bucket.removeIf(existente -> sameEntrada(existente, entrada));
        bucket.add(entrada);
        bucket.sort(Comparator.comparing(EntradaCalendario::data).thenComparing(e -> e.tipo().name()).thenComparing(e -> safe(e.descricao())));
    }

    public void cadastrarRecesso(PeriodoRecesso recesso) {
        Objects.requireNonNull(recesso, "recesso");
        String tribunal = Objects.requireNonNull(recesso.tribunalCodigo(), "tribunalCodigo");
        recessosPorTribunal.computeIfAbsent(tribunal, key -> new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<PeriodoRecesso> bucket = recessosPorTribunal.get(tribunal);
        bucket.removeIf(existente -> sameRecesso(existente, recesso));
        bucket.add(recesso);
        bucket.sort(Comparator.comparing(PeriodoRecesso::inicio).thenComparing(PeriodoRecesso::fim).thenComparing(e -> safe(e.descricao())));
    }

    public void substituirPluginCalendario(String pluginKey, Collection<EntradaCalendario> entradas, Collection<PeriodoRecesso> recessos) {
        pluginMutationLock.lock();
        try {
            String key = normalizePluginKey(pluginKey);
            removerPluginCalendarioInterno(key);
            List<EntradaCalendario> entradasNormalizadas = entradas == null ? List.of() : entradas.stream().filter(Objects::nonNull).toList();
            List<PeriodoRecesso> recessosNormalizados = recessos == null ? List.of() : recessos.stream().filter(Objects::nonNull).toList();
            entradasNormalizadas.forEach(this::cadastrarEntrada);
            recessosNormalizados.forEach(this::cadastrarRecesso);
            pluginsRegistrados.put(key, new PluginCalendarioState(List.copyOf(entradasNormalizadas), List.copyOf(recessosNormalizados)));
        } finally {
            pluginMutationLock.unlock();
        }
    }

    public void removerPluginCalendario(String pluginKey) {
        pluginMutationLock.lock();
        try {
            removerPluginCalendarioInterno(pluginKey);
        } finally {
            pluginMutationLock.unlock();
        }
    }

    private void removerPluginCalendarioInterno(String pluginKey) {
        String key = normalizePluginKey(pluginKey);
        PluginCalendarioState state = pluginsRegistrados.remove(key);
        if (state == null) {
            return;
        }
        for (EntradaCalendario entrada : state.entradas()) {
            CopyOnWriteArrayList<EntradaCalendario> bucket = entradasPorTribunal.get(entrada.tribunalCodigo());
            if (bucket != null) {
                bucket.removeIf(existente -> sameEntrada(existente, entrada));
                if (bucket.isEmpty()) {
                    entradasPorTribunal.remove(entrada.tribunalCodigo());
                }
            }
        }
        for (PeriodoRecesso recesso : state.recessos()) {
            CopyOnWriteArrayList<PeriodoRecesso> bucket = recessosPorTribunal.get(recesso.tribunalCodigo());
            if (bucket != null) {
                bucket.removeIf(existente -> sameRecesso(existente, recesso));
                if (bucket.isEmpty()) {
                    recessosPorTribunal.remove(recesso.tribunalCodigo());
                }
            }
        }
    }

    public boolean isDiaUtilForense(LocalDate data, String tribunalCodigo) {
        return analisarDia(ContextoCalendario.of(tribunalCodigo), data).diaUtil();
    }

    public boolean isDiaUtilForense(LocalDate data, String tribunalCodigo, String uf, String comarca) {
        return analisarDia(ContextoCalendario.of(tribunalCodigo, uf, comarca), data).diaUtil();
    }

    public DiaForense analisarDia(ContextoCalendario contexto, LocalDate data) {
        Objects.requireNonNull(data, "data");
        ContextoCalendario efetivo = resolverContexto(contexto, data);
        ensureAnoSeeded(efetivo.tribunalCodigo(), data.getYear());
        ensureAnoSeeded(efetivo.tribunalCodigo(), data.plusYears(1).getYear());

        if (isWeekend(data)) {
            return new DiaForense(data, false, "Final de semana", TipoEntrada.FIM_DE_SEMANA);
        }

        List<PeriodoRecesso> recessos = listarRecessosAplicaveis(efetivo, data).stream().filter(PeriodoRecesso::suspendePrazos).toList();
        if (!recessos.isEmpty()) {
            PeriodoRecesso recesso = recessos.get(0);
            return new DiaForense(data, false, "Recesso: " + safe(recesso.descricao()), TipoEntrada.RECESSO_JUDICIAL);
        }

        Optional<EntradaCalendario> nacional = feriadoNacionalFixo(data);
        if (nacional.isPresent()) {
            return new DiaForense(data, false, safe(nacional.get().descricao()), nacional.get().tipo());
        }

        Optional<EntradaCalendario> movel = feriadoMovel(data);
        if (movel.isPresent()) {
            return new DiaForense(data, false, safe(movel.get().descricao()), movel.get().tipo());
        }

        List<EntradaCalendario> entradas = listarEntradasAplicaveis(efetivo, data).stream().filter(EntradaCalendario::suspendePrazos).toList();
        if (!entradas.isEmpty()) {
            EntradaCalendario entrada = entradas.get(0);
            return new DiaForense(data, false, entrada.tipo().name() + ": " + safe(entrada.descricao()), entrada.tipo());
        }

        List<CalendarioForenseEntry> persistidas = calendarioForenseRepository.findApplicableBetween(efetivo.uf(), efetivo.comarca(), data, data).stream()
                .filter(item -> Objects.equals(item.getDia(), data))
                .toList();
        if (!persistidas.isEmpty()) {
            CalendarioForenseEntry item = persistidas.get(0);
            return new DiaForense(data, false, safe(item.getDescricao()), mapearTipoPersistido(item.getTipo()));
        }

        return new DiaForense(data, true, "Dia útil forense", null);
    }

    public Optional<String> motivoNaoUtil(LocalDate data, String tribunalCodigo) {
        DiaForense dia = analisarDia(ContextoCalendario.of(tribunalCodigo), data);
        return dia.diaUtil() ? Optional.empty() : Optional.of(dia.motivo());
    }

    public PrazoCalculado calcularPrazo(LocalDate dataEvento, int diasUteis, String tribunalCodigo) {
        return calcularPrazo(ContextoCalendario.of(tribunalCodigo), dataEvento, diasUteis);
    }

    public PrazoCalculado calcularPrazo(LocalDate dataEvento, int diasUteis, String tribunalCodigo, String uf, String comarca) {
        return calcularPrazo(ContextoCalendario.of(tribunalCodigo, uf, comarca), dataEvento, diasUteis);
    }

    public PrazoCalculado calcularPrazo(ContextoCalendario contexto, LocalDate dataEvento, int diasUteis) {
        Objects.requireNonNull(dataEvento, "dataEvento");
        if (diasUteis <= 0) {
            throw new IllegalArgumentException("diasUteis deve ser positivo");
        }
        ContextoCalendario efetivo = resolverContexto(contexto, dataEvento);
        LocalDate inicio = dataEvento.plusDays(1);
        LocalDate cursor = inicio;
        int contados = 0;
        List<DiaDiferido> descartados = new ArrayList<>();
        while (true) {
            DiaForense dia = analisarDia(efetivo, cursor);
            if (dia.diaUtil()) {
                contados++;
                if (contados == diasUteis) {
                    break;
                }
            } else {
                descartados.add(new DiaDiferido(cursor, dia.motivo(), dia.tipoEntrada()));
            }
            cursor = cursor.plusDays(1);
        }
        LocalDate vencimento = cursor;
        while (!analisarDia(efetivo, vencimento).diaUtil()) {
            DiaForense dia = analisarDia(efetivo, vencimento);
            descartados.add(new DiaDiferido(vencimento, "Prorrogação automática - " + dia.motivo(), dia.tipoEntrada()));
            vencimento = vencimento.plusDays(1);
        }
        return new PrazoCalculado(
                dataEvento,
                inicio,
                diasUteis,
                vencimento,
                efetivo.tribunalCodigo(),
                efetivo.uf(),
                efetivo.comarca(),
                List.copyOf(descartados),
                "Prazo de " + diasUteis + " dias úteis contado de " + dataEvento + " vence em " + vencimento,
                construirFundamentacaoPrazo(descartados)
        );
    }

    public List<LocalDate> proximosDiasUteis(LocalDate inicio, int quantidade, String tribunalCodigo) {
        Objects.requireNonNull(inicio, "inicio");
        if (quantidade <= 0) {
            return List.of();
        }
        ContextoCalendario contexto = ContextoCalendario.of(tribunalCodigo);
        List<LocalDate> resultado = new ArrayList<>();
        LocalDate cursor = inicio;
        while (resultado.size() < quantidade) {
            if (analisarDia(contexto, cursor).diaUtil()) {
                resultado.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return List.copyOf(resultado);
    }

    public int contarDiasUteis(LocalDate inicio, LocalDate fim, String tribunalCodigo) {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (fim.isBefore(inicio)) {
            return 0;
        }
        int total = 0;
        LocalDate cursor = inicio;
        ContextoCalendario contexto = ContextoCalendario.of(tribunalCodigo);
        while (!cursor.isAfter(fim)) {
            if (analisarDia(contexto, cursor).diaUtil()) {
                total++;
            }
            cursor = cursor.plusDays(1);
        }
        return total;
    }

    public void seedCalendarioTribunal(String tribunalCodigo, int ano) {
        pluginMutationLock.lock();
        try {
            String tribunal = normalizeTribunal(tribunalCodigo);
            if (tribunal == null) {
                return;
            }
            trimSeedsIfNeeded(ano);
            String seedKey = tribunal + "::" + ano;
            if (!seedsProcessados.add(seedKey)) {
                return;
            }
            TribunalRuleEngine.ContextoResolucao contexto = TribunalRuleEngine.ContextoResolucao.agora(tribunal, null, null);
            int inicio = tribunalRuleEngine.resolverPrazoDias(TribunalRuleEngine.ChaveRegra.RECESSO_DEZ_INICIO_DIA, contexto, 20);
            int fim = tribunalRuleEngine.resolverPrazoDias(TribunalRuleEngine.ChaveRegra.RECESSO_JAN_FIM_DIA, contexto, 6);
            cadastrarRecesso(new PeriodoRecesso(
                    tribunal,
                    "Recesso de Fim de Ano " + ano + "/" + (ano + 1),
                    LocalDate.of(ano, 12, clampDay(inicio, 12, ano)),
                    LocalDate.of(ano + 1, 1, clampDay(fim, 1, ano + 1)),
                    true,
                    "Lei 5.010/66 art. 62",
                    null,
                    null,
                    "SYSTEM-SEED"
            ));
        } finally {
            pluginMutationLock.unlock();
        }
    }

    private void trimSeedsIfNeeded(int referenceYear) {
        if (seedsProcessados.size() < MAX_PROCESSED_SEEDS) {
            return;
        }
        int minYear = referenceYear - RETAIN_MOVABLE_YEAR_BEFORE - 2;
        int maxYear = referenceYear + RETAIN_MOVABLE_YEAR_AFTER + 2;
        seedsProcessados.removeIf(key -> {
            int sep = key == null ? -1 : key.lastIndexOf("::");
            if (sep < 0 || sep + 2 >= key.length()) {
                return true;
            }
            try {
                int year = Integer.parseInt(key.substring(sep + 2));
                return year < minYear || year > maxYear;
            } catch (NumberFormatException e) {
                return true;
            }
        });
        int overflow = seedsProcessados.size() - MAX_PROCESSED_SEEDS;
        if (overflow <= 0) {
            return;
        }
        java.util.List<String> ordered = new java.util.ArrayList<>(seedsProcessados);
        ordered.sort(java.util.Comparator.comparingInt(this::extractSeedYear));
        for (String key : ordered) {
            if (overflow <= 0) {
                break;
            }
            if (seedsProcessados.remove(key)) {
                overflow--;
            }
        }
    }

    private int extractSeedYear(String key) {
        int sep = key == null ? -1 : key.lastIndexOf("::");
        if (sep < 0 || sep + 2 >= key.length()) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(key.substring(sep + 2));
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    public List<EntradaCalendario> seedFeriadosEstaduaisExemplo(int ano) {
        List<EntradaCalendario> entradas = construirFeriadosEstaduaisExemplo(ano);
        entradas.forEach(this::cadastrarEntrada);
        return List.copyOf(entradas);
    }

    public List<EntradaCalendario> construirFeriadosEstaduaisExemplo(int ano) {
        LocalDate cirio = calcularSegundoDomingo(ano, 10);
        LocalDate pascoa = calcularPascoa(ano);
        LocalDate carnavalTerca = pascoa.minusDays(47);
        List<EntradaCalendario> entradas = new ArrayList<>();
        entradas.add(new EntradaCalendario("TJSP", LocalDate.of(ano, 1, 25), TipoEntrada.FERIADO_MUNICIPAL, "Aniversário de São Paulo", true, true, Recorrencia.ANUAL_FIXO, "Lei Municipal SP 14.485/2007", "MUNICIPAL-SP-SAO_PAULO", "SP", "SAO_PAULO", "EXEMPLO-SEED"));
        entradas.add(new EntradaCalendario("TJAM", LocalDate.of(ano, 9, 5), TipoEntrada.FERIADO_ESTADUAL, "Elevação do Amazonas à categoria de Província", true, true, Recorrencia.ANUAL_FIXO, "Lei Estadual AM", "ESTADUAL-AM", "AM", null, "EXEMPLO-SEED"));
        entradas.add(new EntradaCalendario("TJCE", LocalDate.of(ano, 3, 25), TipoEntrada.FERIADO_ESTADUAL, "Abolição da Escravatura no Ceará", true, true, Recorrencia.ANUAL_FIXO, "Lei Estadual CE 9.891/1974", "ESTADUAL-CE", "CE", null, "EXEMPLO-SEED"));
        entradas.add(new EntradaCalendario("TJPA", cirio, TipoEntrada.FERIADO_MUNICIPAL, "Círio de Nazaré", true, true, Recorrencia.UNICA, "Lei Municipal Belém 8.655/2008", "MUNICIPAL-PA-BELEM", "PA", "BELEM", "EXEMPLO-SEED"));
        for (int offset = -1; offset <= 1; offset++) {
            LocalDate data = carnavalTerca.plusDays(offset);
            entradas.add(new EntradaCalendario("TRT8", data, TipoEntrada.RECESSO_CARNAVAL, "Carnaval TRT8", true, true, Recorrencia.UNICA, "Ato TRT8", "REGIONAL-TRT8", "PA", null, "EXEMPLO-SEED"));
        }
        return List.copyOf(entradas);
    }

    public List<EntradaCalendario> listarEntradasTribunalNoAno(String tribunalCodigo, int ano) {
        String tribunal = normalizeTribunal(tribunalCodigo);
        if (tribunal == null) {
            return List.of();
        }
        return entradasPorTribunal.getOrDefault(tribunal, new CopyOnWriteArrayList<>()).stream()
                .filter(item -> item.data().getYear() == ano || item.recorrencia() == Recorrencia.ANUAL_FIXO)
                .sorted(Comparator.comparing(EntradaCalendario::data).thenComparing(item -> item.tipo().name()).thenComparing(item -> safe(item.descricao())))
                .toList();
    }

    public List<EntradaCalendario> feriadosPeriodo(String tribunalCodigo, LocalDate inicio, LocalDate fim) {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (fim.isBefore(inicio)) {
            return List.of();
        }
        ContextoCalendario contexto = ContextoCalendario.of(tribunalCodigo);
        LinkedHashMap<String, EntradaCalendario> mapa = new LinkedHashMap<>();
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fim)) {
            LocalDate data = cursor;
            feriadoNacionalFixo(data).ifPresent(item -> mapa.putIfAbsent(chaveEntrada(item, data), item));
            feriadoMovel(data).ifPresent(item -> mapa.putIfAbsent(chaveEntrada(item, data), item));
            listarEntradasAplicaveis(contexto, data).stream().filter(EntradaCalendario::suspendePrazos).forEach(item -> mapa.putIfAbsent(chaveEntrada(item, data), materializar(item, data)));
            calendarioForenseRepository.findApplicableBetween(contexto.uf(), contexto.comarca(), data, data).stream().filter(item -> Objects.equals(item.getDia(), data)).forEach(item -> {
                EntradaCalendario e = new EntradaCalendario(contexto.tribunalCodigo(), data, mapearTipoPersistido(item.getTipo()), item.getDescricao(), true, true, Recorrencia.UNICA, null, item.getUf() == null ? "NACIONAL" : "REPOSITORY", item.getUf(), item.getComarca(), "REPOSITORY");
                mapa.putIfAbsent(chaveEntrada(e, data), e);
            });
            cursor = cursor.plusDays(1);
        }
        return mapa.values().stream().sorted(Comparator.comparing(EntradaCalendario::data).thenComparing(i -> i.tipo().name()).thenComparing(i -> safe(i.descricao()))).toList();
    }

    public List<PeriodoRecesso> recessosDoAno(String tribunalCodigo, int ano) {
        ensureAnoSeeded(tribunalCodigo, ano);
        ensureAnoSeeded(tribunalCodigo, ano - 1);
        ContextoCalendario contexto = ContextoCalendario.of(tribunalCodigo);
        return recessosPorTribunal.getOrDefault(normalizeTribunal(tribunalCodigo), new CopyOnWriteArrayList<>()).stream()
                .filter(item -> item.inicio().getYear() == ano || item.fim().getYear() == ano)
                .filter(item -> item.aplicaAo(contexto, item.inicio()) || item.aplicaAo(contexto, item.fim()))
                .sorted(Comparator.comparing(PeriodoRecesso::inicio).thenComparing(PeriodoRecesso::fim))
                .toList();
    }

    public ResumoCalendarioAno resumoAno(String tribunalCodigo, int ano) {
        LocalDate inicio = LocalDate.of(ano, 1, 1);
        LocalDate fim = LocalDate.of(ano, 12, 31);
        int totalDias = (int) (ChronoUnit.DAYS.between(inicio, fim) + 1);
        int diasUteis = contarDiasUteis(inicio, fim, tribunalCodigo);
        return new ResumoCalendarioAno(tribunalCodigo, null, null, ano, totalDias, diasUteis, totalDias - diasUteis, feriadosPeriodo(tribunalCodigo, inicio, fim), recessosDoAno(tribunalCodigo, ano));
    }

    public Set<LocalDate> coletarDatasSuspensivas(ContextoCalendario contexto, LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            return Set.of();
        }
        LinkedHashSet<LocalDate> datas = new LinkedHashSet<>();
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fim)) {
            DiaForense dia = analisarDia(contexto, cursor);
            if (!dia.diaUtil() && dia.tipoEntrada() != TipoEntrada.FIM_DE_SEMANA && dia.tipoEntrada() != TipoEntrada.FERIADO_NACIONAL && dia.tipoEntrada() != TipoEntrada.FERIADO_MOVEL) {
                datas.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return Set.copyOf(datas);
    }

    public void espelharNoNationalPrazo(ContextoCalendario contexto, LocalDate inicio, LocalDate fim) {
        ContextoCalendario efetivo = resolverContexto(contexto, inicio == null ? LocalDate.now() : inicio);
        if (efetivo.tribunalCodigo() == null) {
            return;
        }
        NationalPrazoEngine.ConfiguracaoPrazo anterior = nationalPrazoEngine.buscarConfiguracao(efetivo.tribunalCodigo(), efetivo.ramo(), efetivo.grau());
        LinkedHashSet<LocalDate> feriados = new LinkedHashSet<>();
        if (anterior != null && anterior.feriadosAdicionais() != null) {
            feriados.addAll(anterior.feriadosAdicionais());
        }
        feriados.addAll(coletarDatasSuspensivas(efetivo, inicio == null ? LocalDate.now() : inicio, fim == null ? (inicio == null ? LocalDate.now() : inicio).plusYears(1) : fim));
        nationalPrazoEngine.registrarConfiguracao(new NationalPrazoEngine.ConfiguracaoPrazo(efetivo.tribunalCodigo(), efetivo.ramo(), efetivo.grau(), Set.copyOf(feriados), anterior != null && anterior.contarSabado(), anterior != null && anterior.integralmenteCorrido()));
    }

    private ContextoCalendario resolverContexto(ContextoCalendario contexto, LocalDate referencia) {
        ContextoCalendario base = contexto == null ? ContextoCalendario.of((String) null) : contexto;
        if (base.tribunalCodigo() == null || base.uf() != null || base.comarca() != null) {
            return new ContextoCalendario(base.tribunalCodigo(), base.uf(), base.comarca(), base.ramo(), base.grau(), referencia);
        }
        Optional<Jurisdicao> jurisdicao = jurisdicaoRepository.findByCodigo(base.tribunalCodigo())
                .or(() -> jurisdicaoRepository.findBySiglaIgnoreCase(base.tribunalCodigo()));
        if (jurisdicao.isPresent()) {
            String uf = normalizeUf(jurisdicao.get().getEstado());
            String comarca = normalizeText(jurisdicao.get().getComarca());
            return new ContextoCalendario(base.tribunalCodigo(), uf, comarca, base.ramo(), base.grau(), referencia);
        }
        return new ContextoCalendario(base.tribunalCodigo(), null, null, base.ramo(), base.grau(), referencia);
    }

    private List<EntradaCalendario> listarEntradasAplicaveis(ContextoCalendario contexto, LocalDate data) {
        if (contexto.tribunalCodigo() == null) {
            return List.of();
        }
        return entradasPorTribunal.getOrDefault(contexto.tribunalCodigo(), new CopyOnWriteArrayList<>()).stream()
                .filter(item -> item.aplicaAo(contexto, data))
                .sorted(Comparator.comparing((EntradaCalendario item) -> scopeSpecificity(item.uf(), item.comarca(), item.abrangencia())).reversed().thenComparing(item -> safe(item.descricao())))
                .toList();
    }

    private List<PeriodoRecesso> listarRecessosAplicaveis(ContextoCalendario contexto, LocalDate data) {
        if (contexto.tribunalCodigo() == null) {
            return List.of();
        }
        return recessosPorTribunal.getOrDefault(contexto.tribunalCodigo(), new CopyOnWriteArrayList<>()).stream()
                .filter(item -> item.aplicaAo(contexto, data))
                .sorted(Comparator.comparing((PeriodoRecesso item) -> scopeSpecificity(item.uf(), item.comarca(), null)).reversed().thenComparing(PeriodoRecesso::inicio).thenComparing(PeriodoRecesso::fim))
                .toList();
    }

    private Optional<EntradaCalendario> feriadoNacionalFixo(LocalDate data) {
        String descricao = descricaoFeriadoNacionalFixo(data);
        if (descricao == null) {
            return Optional.empty();
        }
        return Optional.of(new EntradaCalendario("BRASIL", data, TipoEntrada.FERIADO_NACIONAL, descricao, true, true, Recorrencia.UNICA, fundamentoFeriadoNacionalFixo(data), "NACIONAL", null, null, "CALENDARIO_BASE"));
    }

    private Optional<EntradaCalendario> feriadoMovel(LocalDate data) {
        return Optional.ofNullable(feriadosMoveisAno(data.getYear()).get(data));
    }

    private Map<LocalDate, EntradaCalendario> feriadosMoveisAno(int ano) {
        Map<LocalDate, EntradaCalendario> mapa = feriadosMoveisCache.computeIfAbsent(ano, this::calcularFeriadosMoveis);
        compactarFeriadosMoveis(ano);
        return mapa;
    }

    private void compactarFeriadosMoveis(int protectedYear) {
        long now = System.nanoTime();
        long scheduled = nextMovableCleanupAtNanos.get();
        if (feriadosMoveisCache.size() <= MAX_FERIADOS_MOVEIS_CACHE_ENTRIES && now < scheduled) {
            return;
        }
        if (!nextMovableCleanupAtNanos.compareAndSet(scheduled, now + FERIADOS_MOVEIS_CLEANUP_INTERVAL_NANOS)) {
            return;
        }
        int currentYear = LocalDate.now().getYear();
        feriadosMoveisCache.keySet().removeIf(year -> year != protectedYear && (year < currentYear - RETAIN_MOVABLE_YEAR_BEFORE || year > currentYear + RETAIN_MOVABLE_YEAR_AFTER));
        if (feriadosMoveisCache.size() <= MAX_FERIADOS_MOVEIS_CACHE_ENTRIES) {
            return;
        }
        feriadosMoveisCache.keySet().stream()
                .filter(year -> year != protectedYear)
                .sorted((left, right) -> Integer.compare(Math.abs(right - currentYear), Math.abs(left - currentYear)))
                .limit(Math.max(0, feriadosMoveisCache.size() - MAX_FERIADOS_MOVEIS_CACHE_ENTRIES))
                .toList()
                .forEach(feriadosMoveisCache::remove);
    }

    private Map<LocalDate, EntradaCalendario> calcularFeriadosMoveis(int ano) {
        LocalDate pascoa = calcularPascoa(ano);
        Map<LocalDate, EntradaCalendario> mapa = new LinkedHashMap<>();
        mapa.put(pascoa.minusDays(48), new EntradaCalendario("BRASIL", pascoa.minusDays(48), TipoEntrada.FERIADO_MOVEL, "Carnaval", true, true, Recorrencia.UNICA, "Calendário forense nacional", "NACIONAL", null, null, "CALENDARIO_BASE"));
        mapa.put(pascoa.minusDays(47), new EntradaCalendario("BRASIL", pascoa.minusDays(47), TipoEntrada.FERIADO_MOVEL, "Carnaval", true, true, Recorrencia.UNICA, "Calendário forense nacional", "NACIONAL", null, null, "CALENDARIO_BASE"));
        mapa.put(pascoa.minusDays(2), new EntradaCalendario("BRASIL", pascoa.minusDays(2), TipoEntrada.FERIADO_MOVEL, "Sexta-feira da Paixão", true, true, Recorrencia.UNICA, "Calendário forense nacional", "NACIONAL", null, null, "CALENDARIO_BASE"));
        mapa.put(pascoa.plusDays(60), new EntradaCalendario("BRASIL", pascoa.plusDays(60), TipoEntrada.FERIADO_MOVEL, "Corpus Christi", true, true, Recorrencia.UNICA, "Calendário forense nacional", "NACIONAL", null, null, "CALENDARIO_BASE"));
        return Map.copyOf(mapa);
    }

    private static LocalDate calcularPascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }

    private static LocalDate calcularSegundoDomingo(int ano, int mes) {
        LocalDate primeiroDia = LocalDate.of(ano, mes, 1);
        int deslocamento = (DayOfWeek.SUNDAY.getValue() - primeiroDia.getDayOfWeek().getValue() + 7) % 7;
        return primeiroDia.plusDays(deslocamento).plusWeeks(1);
    }

    private void ensureAnoSeeded(String tribunalCodigo, int ano) {
        if (tribunalCodigo != null && ano >= 1900 && ano <= 2600) {
            seedCalendarioTribunal(tribunalCodigo, ano);
        }
    }

    private static boolean isWeekend(LocalDate data) {
        DayOfWeek day = data.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private static String descricaoFeriadoNacionalFixo(LocalDate data) {
        int mes = data.getMonthValue();
        int dia = data.getDayOfMonth();
        if (mes == 1 && dia == 1) return "Confraternização Universal";
        if (mes == 4 && dia == 21) return "Tiradentes";
        if (mes == 5 && dia == 1) return "Dia do Trabalho";
        if (mes == 9 && dia == 7) return "Independência do Brasil";
        if (mes == 10 && dia == 12) return "Nossa Senhora Aparecida";
        if (mes == 11 && dia == 2) return "Finados";
        if (mes == 11 && dia == 15) return "Proclamação da República";
        if (mes == 11 && dia == 20) return "Dia da Consciência Negra";
        if (mes == 12 && dia == 25) return "Natal";
        return null;
    }

    private static String fundamentoFeriadoNacionalFixo(LocalDate data) {
        if (data.getMonthValue() == 11 && data.getDayOfMonth() == 20) return "Lei 14.759/2023";
        if (data.getMonthValue() == 5 && data.getDayOfMonth() == 1) return "CLT art. 1º";
        return "Lei 9.093/95";
    }

    private static TipoEntrada mapearTipoPersistido(String tipo) {
        String token = normalizeNullable(tipo);
        if (token == null) return TipoEntrada.SUSPENSAO_PORTARIA;
        try {
            return TipoEntrada.valueOf(token.replace('-', '_').replace(' ', '_'));
        } catch (Exception ex) {
            return switch (token) {
                case "FERIADO" -> TipoEntrada.FERIADO_NACIONAL;
                case "RECESSO" -> TipoEntrada.RECESSO_JUDICIAL;
                case "PORTARIA" -> TipoEntrada.SUSPENSAO_PORTARIA;
                default -> TipoEntrada.SUSPENSAO_PORTARIA;
            };
        }
    }

    private static boolean sameEntrada(EntradaCalendario a, EntradaCalendario b) {
        return Objects.equals(a.tribunalCodigo(), b.tribunalCodigo()) && Objects.equals(a.data(), b.data()) && a.tipo() == b.tipo() && Objects.equals(a.descricao(), b.descricao()) && a.suspendeExpediente() == b.suspendeExpediente() && a.suspendePrazos() == b.suspendePrazos() && a.recorrencia() == b.recorrencia() && Objects.equals(a.fundamentacao(), b.fundamentacao()) && Objects.equals(a.abrangencia(), b.abrangencia()) && Objects.equals(a.uf(), b.uf()) && Objects.equals(a.comarca(), b.comarca()) && Objects.equals(a.origemId(), b.origemId());
    }

    private static boolean sameRecesso(PeriodoRecesso a, PeriodoRecesso b) {
        return Objects.equals(a.tribunalCodigo(), b.tribunalCodigo()) && Objects.equals(a.descricao(), b.descricao()) && Objects.equals(a.inicio(), b.inicio()) && Objects.equals(a.fim(), b.fim()) && a.suspendePrazos() == b.suspendePrazos() && Objects.equals(a.fundamentacao(), b.fundamentacao()) && Objects.equals(a.uf(), b.uf()) && Objects.equals(a.comarca(), b.comarca()) && Objects.equals(a.origemId(), b.origemId());
    }

    private static boolean matchesTribunal(String entradaTribunal, String contextoTribunal) {
        return entradaTribunal == null || "BRASIL".equals(entradaTribunal) || Objects.equals(entradaTribunal, contextoTribunal);
    }

    private static boolean matchesAbrangencia(String abrangencia, ContextoCalendario contexto) {
        if (abrangencia == null || abrangencia.isBlank()) {
            return true;
        }
        String token = normalizeNullable(abrangencia);
        if (token == null || "NACIONAL".equals(token)) {
            return true;
        }
        if (token.startsWith("ESTADUAL-")) {
            return Objects.equals(token.substring("ESTADUAL-".length()), contexto.uf());
        }
        if (token.startsWith("MUNICIPAL-")) {
            String[] parts = token.split("-", 3);
            return parts.length == 3 && Objects.equals(parts[1], contexto.uf()) && Objects.equals(normalizeText(parts[2]), contexto.comarca());
        }
        if (token.startsWith("REGIONAL-") || token.startsWith("TRIBUNAL-")) {
            int idx = token.indexOf('-');
            return idx > -1 && Objects.equals(token.substring(idx + 1), contexto.tribunalCodigo());
        }
        return true;
    }

    private static int scopeSpecificity(String uf, String comarca, String abrangencia) {
        int score = abrangencia == null ? 0 : 1;
        if (uf != null) score += 2;
        if (comarca != null) score += 3;
        return score;
    }

    private static String construirFundamentacaoPrazo(List<DiaDiferido> descartados) {
        String base = "CPC art. 224";
        if (descartados == null || descartados.isEmpty()) {
            return base;
        }
        String amostra = descartados.stream().limit(5).map(item -> item.data() + "=" + safe(item.motivo())).collect(Collectors.joining(", "));
        if (descartados.size() > 5) {
            amostra += ", +" + (descartados.size() - 5) + " dia(s)";
        }
        return base + " | " + amostra;
    }

    private static EntradaCalendario materializar(EntradaCalendario entrada, LocalDate data) {
        return new EntradaCalendario(entrada.tribunalCodigo(), data, entrada.tipo(), entrada.descricao(), entrada.suspendeExpediente(), entrada.suspendePrazos(), Recorrencia.UNICA, entrada.fundamentacao(), entrada.abrangencia(), entrada.uf(), entrada.comarca(), entrada.origemId());
    }

    private static String chaveEntrada(EntradaCalendario entrada, LocalDate data) {
        return data + "::" + entrada.tipo().name() + "::" + safe(entrada.descricao()) + "::" + safe(entrada.uf()) + "::" + safe(entrada.comarca()) + "::" + safe(entrada.origemId());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeTribunal(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeUf(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim().replace('-', ' ').replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizePluginKey(String pluginKey) {
        if (pluginKey == null || pluginKey.isBlank()) {
            throw new IllegalArgumentException("pluginKey obrigatório");
        }
        return pluginKey.trim().toUpperCase(Locale.ROOT);
    }

    private static int clampDay(int day, int month, int year) {
        int max = LocalDate.of(year, month, 1).lengthOfMonth();
        return Math.max(1, Math.min(day, max));
    }
}
