package com.tcc.pjb.backend.platform.jusos.v2.prazo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseEntry;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.JurisdicaoRepository;

@Service
public class NationalPrazoEngine {

    public enum TipoPrazo {
        PETICAO_INICIAL(0, false, false),
        CONTESTACAO(15, true, true),
        RECONVENCAO(15, true, true),
        REPLICA(15, true, true),
        EMBARGOS_DECLARACAO(5, true, false),
        APELACAO(15, true, true),
        AGRAVO_INTERNO(15, true, true),
        AGRAVO_INSTRUMENTO(15, true, true),
        AGRAVO_RECURSO_SUPERIOR(15, true, true),
        RECURSO_ESPECIAL(15, true, true),
        RECURSO_EXTRAORDINARIO(15, true, true),
        RECURSO_ORDINARIO_CONSTITUCIONAL(15, true, true),
        EMBARGOS_DIVERGENCIA(15, true, true),
        RECLAMACAO_CONSTITUCIONAL(15, true, true),
        CONFLITO_COMPETENCIA(15, true, true),
        INCIDENTE_REPETITIVO_ASSUNCAO(15, true, true),
        SUSPENSAO_SEGURANCA_LIMINAR(5, false, false),
        EMBARGOS_INFRINGENTES_NULIDADE(10, true, true),
        CONTRARRAZOES_APELACAO(15, true, true),
        CONTRARRAZOES_SUPERIOR(15, true, true),
        HABEAS_CORPUS(0, false, false),
        MANDADO_SEGURANCA(120, false, false),
        ACAO_RESCISORIA(730, false, false),
        CUMPRIMENTO_SENTENCA(15, true, true),
        EMBARGOS_EXECUCAO(15, true, true),
        IMPUGNACAO_CUMPRIMENTO(15, true, true),
        RESPOSTA_TRABALHISTA(0, false, false),
        APRESENTACAO_DEFESA_PENAL(10, false, false),
        ALEGACOES_FINAIS_PENAL(5, false, false),
        RECURSO_TRABALHISTA(8, true, false),
        RECURSO_ELEITORAL(3, false, false),
        RECURSO_MILITAR(10, true, true),
        PRAZO_MP_MANIFESTACAO(30, true, true),
        PRAZO_PERICIA(30, true, true),
        PRAZO_GENERICO(15, true, true);

        public final int diasPadrao;
        public final boolean emDiasUteis;
        public final boolean suspendeNasFerias;

        public static final TipoPrazo RESPOSTA_REU = CONTESTACAO;
        public static final TipoPrazo MANIFESTACAO_GERAL = PRAZO_GENERICO;
        public static final TipoPrazo CUMPRIMENTO_DETERMINACAO = PRAZO_GENERICO;

        TipoPrazo(int diasPadrao, boolean emDiasUteis, boolean suspendeNasFerias) {
            this.diasPadrao = diasPadrao;
            this.emDiasUteis = emDiasUteis;
            this.suspendeNasFerias = suspendeNasFerias;
        }
    }

    public enum PeriodoFeriasForenses {
        RECESSO_FIM_ANO(12, 20, 1, 20),
        RECESSO_CARNAVAL_TRABALHISTA(0, 0, 0, 0);

        public final int mesInicio;
        public final int diaInicio;
        public final int mesFim;
        public final int diaFim;

        PeriodoFeriasForenses(int mesInicio, int diaInicio, int mesFim, int diaFim) {
            this.mesInicio = mesInicio;
            this.diaInicio = diaInicio;
            this.mesFim = mesFim;
            this.diaFim = diaFim;
        }
    }

    public record PrazoCalculado(
            LocalDate inicio,
            LocalDate vencimento,
            int diasCorridos,
            int diasUteis,
            TipoPrazo tipo,
            RamoDireito ramo,
            GrauJurisdicao grau,
            boolean suspenso,
            List<String> advertencias,
            String fundamentoLegal
    ) {}

    public record ConfiguracaoPrazo(
            String tribunalCodigo,
            RamoDireito ramo,
            GrauJurisdicao grau,
            Set<LocalDate> feriadosAdicionais,
            boolean contarSabado,
            boolean integralmenteCorrido
    ) {}

    private record ContextoCalendario(
            String tribunalCodigo,
            String uf,
            String comarca,
            boolean contarSabado,
            boolean integralmenteCorrido,
            Set<LocalDate> feriadosAdicionais
    ) {}

    private static final int MAX_FERIADOS_CACHE_ENTRIES = 384;
    private static final int RETAIN_YEAR_BEFORE = 4;
    private static final int RETAIN_YEAR_AFTER = 8;
    private static final long FERIADOS_CACHE_CLEANUP_INTERVAL_NANOS = java.time.Duration.ofMinutes(5).toNanos();

    private final CalendarioForenseRepository calendarioForenseRepository;
    private final JurisdicaoRepository jurisdicaoRepository;
    private final Map<String, Set<LocalDate>> feriadosCache = new ConcurrentHashMap<>();
    private final Map<String, ConfiguracaoPrazo> configuracoes = new ConcurrentHashMap<>();
    private final AtomicLong nextFeriadosCleanupAtNanos = new AtomicLong(System.nanoTime() + FERIADOS_CACHE_CLEANUP_INTERVAL_NANOS);

    public NationalPrazoEngine(CalendarioForenseRepository calendarioForenseRepository,
                               JurisdicaoRepository jurisdicaoRepository) {
        this.calendarioForenseRepository = calendarioForenseRepository;
        this.jurisdicaoRepository = jurisdicaoRepository;
    }

    public PrazoCalculado calcular(LocalDate dataInicio,
                                   TipoPrazo tipo,
                                   RamoDireito ramo,
                                   GrauJurisdicao grau,
                                   String tribunalCodigo) {
        Objects.requireNonNull(dataInicio, "dataInicio");
        Objects.requireNonNull(tipo, "tipo");

        RamoDireito ramoEfetivo = ramo;
        ContextoCalendario contexto = resolverContexto(tribunalCodigo, ramo, grau);
        int diasBase = resolverDias(tipo, ramoEfetivo, grau);
        boolean emDiasUteis = resolverUteis(tipo, ramoEfetivo, contexto);
        String fundamento = resolverFundamento(tipo, ramoEfetivo, grau);
        List<String> advertencias = new ArrayList<>();

        if (diasBase == 0) {
            if (tipo == TipoPrazo.HABEAS_CORPUS) {
                advertencias.add("Medida sem prazo decadencial ordinário: protocolo imediato recomendado");
            } else if (tipo == TipoPrazo.PETICAO_INICIAL || tipo == TipoPrazo.RESPOSTA_TRABALHISTA) {
                advertencias.add("Prazo sem contagem automática padronizada: depende de designação, audiência ou estratégia processual");
            }
            return new PrazoCalculado(
                    dataInicio,
                    dataInicio,
                    0,
                    0,
                    tipo,
                    ramoEfetivo,
                    grau,
                    false,
                    List.copyOf(advertencias),
                    fundamento
            );
        }

        Set<LocalDate> feriados = carregarFeriados(contexto, dataInicio.getYear(), dataInicio.getYear() + 2);

        LocalDate vencimentoBruto = emDiasUteis
                ? adicionarDiasUteis(dataInicio, diasBase, feriados, contexto, advertencias)
                : adicionarDiasCorridos(dataInicio, diasBase, feriados, contexto, advertencias);

        boolean suspenso = verificarSuspensao(vencimentoBruto, tipo, ramoEfetivo, advertencias);
        LocalDate vencimentoFinal = vencimentoBruto;
        if (suspenso) {
            vencimentoFinal = ajustarFimSuspensao(vencimentoBruto, feriados, contexto, tipo, advertencias);
        } else if (!ehDiaUtil(vencimentoFinal, feriados, contexto)) {
            vencimentoFinal = proximoDiaUtil(vencimentoFinal, feriados, contexto);
            advertencias.add("Vencimento ajustado para o próximo dia útil aplicável");
        }

        int diasCorridos = Math.toIntExact(Math.max(0, ChronoUnit.DAYS.between(dataInicio, vencimentoFinal)));
        int diasUteis = contarDiasUteis(dataInicio, vencimentoFinal, feriados, contexto);
        advertencias.addAll(construirAdvertenciasEstruturais(tipo, ramoEfetivo, grau, contexto));

        return new PrazoCalculado(
                dataInicio,
                vencimentoFinal,
                diasCorridos,
                diasUteis,
                tipo,
                ramoEfetivo,
                grau,
                suspenso,
                List.copyOf(new LinkedHashSet<>(advertencias)),
                fundamento
        );
    }

    public PrazoCalculado calcularPorRamo(LocalDate inicio,
                                          TipoPrazo tipo,
                                          RamoDireito ramo,
                                          GrauJurisdicao grau,
                                          String tribunal) {
        return calcular(inicio, tipo, ramo, grau, tribunal);
    }

    public PrazoCalculado calcular(Processo processo, TipoPrazo tipo) {
        Objects.requireNonNull(processo, "processo");
        LocalDate inicio = processo.getDataUltimaMovimentacao() != null
                ? processo.getDataUltimaMovimentacao().toLocalDate()
                : processo.getDataCriacao() != null
                ? processo.getDataCriacao().toLocalDate()
                : LocalDate.now();
        GrauJurisdicao grau = processo.getJurisdicao() != null ? processo.getJurisdicao().getGrau() : null;
        String tribunalCodigo = processo.getJurisdicao() != null ? processo.getJurisdicao().getCodigo() : null;
        return calcular(inicio, tipo, processo.getRamoDireito(), grau, tribunalCodigo);
    }

    public boolean ehDiaUtil(LocalDate data, Set<LocalDate> feriados, RamoDireito ramo) {
        return ehDiaUtil(data, feriados, resolverContexto(null, ramo, null));
    }

    public void registrarConfiguracao(ConfiguracaoPrazo configuracao) {
        Objects.requireNonNull(configuracao, "configuracao");
        String chave = chaveConfiguracao(configuracao.tribunalCodigo(), configuracao.ramo(), configuracao.grau());
        configuracoes.put(chave, sanitizarConfiguracao(configuracao));
        if (configuracao.tribunalCodigo() != null && !configuracao.tribunalCodigo().isBlank()) {
            invalidarCacheTribunal(configuracao.tribunalCodigo());
        }
    }


    public void removerConfiguracao(String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau) {
        configuracoes.remove(chaveConfiguracao(tribunalCodigo, ramo, grau));
        if (tribunalCodigo != null && !tribunalCodigo.isBlank()) {
            invalidarCacheTribunal(tribunalCodigo);
        }
    }

    public ConfiguracaoPrazo buscarConfiguracao(String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau) {
        return localizarConfiguracao(tribunalCodigo, ramo, grau);
    }

    public void invalidarCacheTribunal(String tribunalCodigo) {
        if (tribunalCodigo == null || tribunalCodigo.isBlank()) {
            feriadosCache.clear();
            return;
        }
        String token = tribunalCodigo.trim().toUpperCase();
        feriadosCache.keySet().removeIf(k -> k.startsWith(token + ":"));
    }

    public void adicionarFeriadoTribunal(String tribunalCodigo, int ano, LocalDate feriado) {
        if (tribunalCodigo == null || tribunalCodigo.isBlank() || feriado == null) {
            return;
        }
        String token = tribunalCodigo.trim().toUpperCase();
        String chave = token + ":" + ano;
        feriadosCache.computeIfAbsent(chave, k -> carregarFeriadosAno(token, ano)).add(feriado);
        compactarCacheFeriados(chave);
    }

    private boolean ehDiaUtil(LocalDate data, Set<LocalDate> feriados, ContextoCalendario contexto) {
        if (data == null) {
            return false;
        }
        DayOfWeek dia = data.getDayOfWeek();
        if (dia == DayOfWeek.SUNDAY) {
            return false;
        }
        if (dia == DayOfWeek.SATURDAY && !contexto.contarSabado()) {
            return false;
        }
        return feriados == null || !feriados.contains(data);
    }

    private LocalDate adicionarDiasUteis(LocalDate inicio,
                                         int dias,
                                         Set<LocalDate> feriados,
                                         ContextoCalendario contexto,
                                         List<String> advertencias) {
        LocalDate atual = inicio;
        int contados = 0;
        int seguranca = Math.max(200, dias * 12 + 60);
        while (contados < dias && seguranca-- > 0) {
            atual = atual.plusDays(1);
            if (ehDiaUtil(atual, feriados, contexto)) {
                contados++;
            }
        }
        if (contados < dias) {
            advertencias.add("Cálculo atingiu limite de segurança e pode exigir revisão manual");
        }
        return atual;
    }

    private LocalDate adicionarDiasCorridos(LocalDate inicio,
                                            int dias,
                                            Set<LocalDate> feriados,
                                            ContextoCalendario contexto,
                                            List<String> advertencias) {
        LocalDate vencimento = inicio.plusDays(dias);
        if (!ehDiaUtil(vencimento, feriados, contexto)) {
            vencimento = proximoDiaUtil(vencimento, feriados, contexto);
            advertencias.add("Vencimento prorrogado para o próximo dia útil");
        }
        return vencimento;
    }

    private LocalDate proximoDiaUtil(LocalDate data, Set<LocalDate> feriados, ContextoCalendario contexto) {
        LocalDate atual = data;
        int seguranca = 45;
        while (!ehDiaUtil(atual, feriados, contexto) && seguranca-- > 0) {
            atual = atual.plusDays(1);
        }
        return atual;
    }

    private int contarDiasUteis(LocalDate inicio,
                                LocalDate fim,
                                Set<LocalDate> feriados,
                                ContextoCalendario contexto) {
        if (inicio == null || fim == null || !inicio.isBefore(fim)) {
            return 0;
        }
        int count = 0;
        LocalDate atual = inicio.plusDays(1);
        while (!atual.isAfter(fim)) {
            if (ehDiaUtil(atual, feriados, contexto)) {
                count++;
            }
            atual = atual.plusDays(1);
        }
        return count;
    }

    private boolean verificarSuspensao(LocalDate data,
                                       TipoPrazo tipo,
                                       RamoDireito ramo,
                                       List<String> advertencias) {
        if (data == null || !tipo.suspendeNasFerias) {
            return false;
        }
        if (estaNoRecessoFimDeAno(data)) {
            advertencias.add("Período de recesso forense identificado entre 20/12 e 20/01");
            return true;
        }
        if (ramo == RamoDireito.TRABALHISTA && estaNaSuspensaoCarnaval(data)) {
            advertencias.add("Suspensão especial de carnaval trabalhista aplicada ao vencimento");
            return true;
        }
        return false;
    }

    private LocalDate ajustarFimSuspensao(LocalDate vencimento,
                                          Set<LocalDate> feriados,
                                          ContextoCalendario contexto,
                                          TipoPrazo tipo,
                                          List<String> advertencias) {
        LocalDate ajustado = vencimento;
        if (estaNoRecessoFimDeAno(ajustado)) {
            int anoFim = ajustado.getMonthValue() == Month.DECEMBER.getValue()
                    ? ajustado.getYear() + 1
                    : ajustado.getYear();
            ajustado = LocalDate.of(anoFim, Month.JANUARY, 21);
        }
        if (tipo.suspendeNasFerias && estaNaSuspensaoCarnaval(ajustado)) {
            while (estaNaSuspensaoCarnaval(ajustado)) {
                ajustado = ajustado.plusDays(1);
            }
        }
        ajustado = proximoDiaUtil(ajustado, feriados, contexto);
        advertencias.add("Prazo prorrogado após período de suspensão forense aplicável");
        return ajustado;
    }

    private int resolverDias(TipoPrazo tipo, RamoDireito ramo, GrauJurisdicao grau) {
        if (ramo == null) {
            return tipo.diasPadrao;
        }
        return switch (ramo) {
            case TRABALHISTA -> switch (tipo) {
                case CONTESTACAO, RESPOSTA_TRABALHISTA -> 0;
                case EMBARGOS_DECLARACAO -> 5;
                case APELACAO, RECURSO_TRABALHISTA -> 8;
                case AGRAVO_INTERNO -> 8;
                default -> tipo.diasPadrao;
            };
            case PENAL -> switch (tipo) {
                case APELACAO -> 5;
                case EMBARGOS_DECLARACAO -> 2;
                case APRESENTACAO_DEFESA_PENAL -> 10;
                case ALEGACOES_FINAIS_PENAL -> 5;
                default -> tipo.diasPadrao;
            };
            case ELEITORAL -> switch (tipo) {
                case APELACAO, RECURSO_ELEITORAL, EMBARGOS_DECLARACAO, AGRAVO_INTERNO, RECURSO_ORDINARIO_CONSTITUCIONAL -> 3;
                default -> tipo.diasPadrao;
            };
            case PREVIDENCIARIO -> switch (tipo) {
                case CONTESTACAO -> 30;
                default -> tipo.diasPadrao;
            };
            case MILITAR -> switch (tipo) {
                case RECURSO_MILITAR, APELACAO, EMBARGOS_INFRINGENTES_NULIDADE -> 10;
                default -> tipo.diasPadrao;
            };
            default -> tipo.diasPadrao;
        };
    }

    private boolean resolverUteis(TipoPrazo tipo, RamoDireito ramo, ContextoCalendario contexto) {
        if (contexto.integralmenteCorrido()) {
            return false;
        }
        if (ramo == RamoDireito.TRABALHISTA || ramo == RamoDireito.PENAL || ramo == RamoDireito.ELEITORAL || ramo == RamoDireito.MILITAR) {
            return false;
        }
        return tipo.emDiasUteis;
    }

    private String resolverFundamento(TipoPrazo tipo, RamoDireito ramo, GrauJurisdicao grau) {
        if (ramo == null) {
            return "CPC arts. 219 a 224";
        }
        return switch (ramo) {
            case CIVIL, CONSUMIDOR, EMPRESARIAL, AMBIENTAL, ADMINISTRATIVO, AGRARIO -> switch (tipo) {
                case CONTESTACAO -> "CPC art. 335 c/c art. 219";
                case APELACAO -> "CPC art. 1.003, §5º";
                case AGRAVO_INSTRUMENTO, AGRAVO_RECURSO_SUPERIOR -> "CPC art. 1.003, §5º c/c arts. 1.015 e 1.042";
                case RECURSO_ESPECIAL -> "CPC art. 1.003, §5º c/c regime recursal do STJ";
                case RECURSO_EXTRAORDINARIO -> "CPC art. 1.003, §5º c/c regime recursal do STF";
                case RECURSO_ORDINARIO_CONSTITUCIONAL -> "CF/88, CPC subsidiário e regimento interno do tribunal competente";
                case CONTRARRAZOES_SUPERIOR -> "CPC art. 1.003, §5º c/c regime recursal do tribunal superior competente";
                case EMBARGOS_DECLARACAO -> "CPC art. 1.023";
                case EMBARGOS_DIVERGENCIA -> "CPC art. 1.043 e regimento interno do tribunal superior";
                case CONFLITO_COMPETENCIA, RECLAMACAO_CONSTITUCIONAL, INCIDENTE_REPETITIVO_ASSUNCAO, SUSPENSAO_SEGURANCA_LIMINAR -> "Legislação especial e regimento interno do tribunal competente";
                default -> "CPC arts. 219 a 224";
            };
            case TRABALHISTA -> switch (tipo) {
                case RECURSO_TRABALHISTA, APELACAO -> "CLT art. 895";
                case EMBARGOS_DECLARACAO -> "CLT art. 897-A";
                case AGRAVO_INTERNO, RECURSO_ORDINARIO_CONSTITUCIONAL, RECLAMACAO_CONSTITUCIONAL, CONTRARRAZOES_SUPERIOR -> "CLT, CPC subsidiário e RITST";
                default -> "CLT e normativos do TST";
            };
            case PENAL -> switch (tipo) {
                case APELACAO -> "CPP art. 593";
                case HABEAS_CORPUS -> "CPP art. 647";
                case APRESENTACAO_DEFESA_PENAL -> "CPP art. 396-A";
                case EMBARGOS_DECLARACAO -> "CPP art. 382";
                case RECLAMACAO_CONSTITUCIONAL, CONFLITO_COMPETENCIA -> "CPP, legislação especial e regimento interno do tribunal competente";
                default -> "CPP art. 798";
            };
            case ELEITORAL -> switch (tipo) {
                case AGRAVO_INTERNO, RECURSO_ELEITORAL, RECURSO_ORDINARIO_CONSTITUCIONAL, CONTRARRAZOES_SUPERIOR, EMBARGOS_DECLARACAO -> "Código Eleitoral e regimento do TSE";
                default -> "Código Eleitoral e resoluções do TSE";
            };
            case MILITAR -> switch (tipo) {
                case EMBARGOS_INFRINGENTES_NULIDADE -> "CPPM e regimento do STM";
                default -> "CPPM e regulamentos da Justiça Militar";
            };
            case FAMILIA -> "CPC arts. 335 e 694";
            case PREVIDENCIARIO -> "Lei 10.259/2001, Lei 9.099/1995 e CPC subsidiário";
            case INFANCIA_JUVENTUDE -> "ECA art. 198 c/c CPC";
            case CONSTITUCIONAL -> switch (tipo) {
                case MANDADO_SEGURANCA -> "Lei 12.016/2009, art. 23";
                case RECURSO_EXTRAORDINARIO, AGRAVO_RECURSO_SUPERIOR, AGRAVO_INTERNO, CONTRARRAZOES_SUPERIOR, EMBARGOS_DIVERGENCIA, RECLAMACAO_CONSTITUCIONAL, RECURSO_ORDINARIO_CONSTITUCIONAL -> "CF/88, CPC subsidiário e RISTF";
                default -> grau == GrauJurisdicao.CONSTITUCIONAL ? "CF/88 e regimentos dos tribunais constitucionais" : "CF/88 e CPC subsidiário";
            };
            case TRIBUTARIO -> "CTN, LEF e CPC subsidiário";
            case INTERNACIONAL -> "Tratados internacionais, LINDB, CPC subsidiário e cooperação jurídica internacional";
            default -> "CPC arts. 219 a 224";
        };
    }

    private Set<LocalDate> carregarFeriados(ContextoCalendario contexto, int anoInicial, int anoFinal) {
        Set<LocalDate> acumulado = new HashSet<>();
        for (int ano = anoInicial; ano <= anoFinal; ano++) {
            acumulado.addAll(carregarFeriadosAno(contexto.tribunalCodigo(), ano));
        }
        if (contexto.feriadosAdicionais() != null && !contexto.feriadosAdicionais().isEmpty()) {
            acumulado.addAll(contexto.feriadosAdicionais());
        }
        return acumulado;
    }

    private Set<LocalDate> carregarFeriadosAno(String tribunalCodigo, int ano) {
        String codigo = normalizarTribunal(tribunalCodigo);
        String chave = codigo + ":" + ano;
        Set<LocalDate> datas = feriadosCache.computeIfAbsent(chave, k -> {
            ContextoCalendario contexto = resolverContexto(codigo, null, null);
            List<CalendarioForenseEntry> entries = calendarioForenseRepository.findApplicableBetween(
                    contexto.uf(),
                    contexto.comarca(),
                    LocalDate.of(ano, 1, 1),
                    LocalDate.of(ano, 12, 31)
            );
            Set<LocalDate> feriados = new LinkedHashSet<>();
            for (CalendarioForenseEntry entry : entries) {
                if (entry != null && entry.getDia() != null) {
                    feriados.add(entry.getDia());
                }
            }
            if (feriados.isEmpty()) {
                feriados.addAll(feriadosNacionais(ano));
            }
            if (contexto.feriadosAdicionais() != null && !contexto.feriadosAdicionais().isEmpty()) {
                feriados.addAll(contexto.feriadosAdicionais());
            }
            return feriados;
        });
        compactarCacheFeriados(chave);
        return datas;
    }

    private void compactarCacheFeriados(String protectedKey) {
        long now = System.nanoTime();
        long scheduled = nextFeriadosCleanupAtNanos.get();
        if (feriadosCache.size() <= MAX_FERIADOS_CACHE_ENTRIES && now < scheduled) {
            return;
        }
        if (!nextFeriadosCleanupAtNanos.compareAndSet(scheduled, now + FERIADOS_CACHE_CLEANUP_INTERVAL_NANOS)) {
            return;
        }
        int currentYear = LocalDate.now().getYear();
        feriadosCache.keySet().removeIf(key -> !Objects.equals(key, protectedKey) && foraDaJanelaDeRetencao(key, currentYear));
        if (feriadosCache.size() <= MAX_FERIADOS_CACHE_ENTRIES) {
            return;
        }
        feriadosCache.keySet().stream()
                .filter(key -> !Objects.equals(key, protectedKey))
                .sorted((left, right) -> Integer.compare(distanciaAno(right, currentYear), distanciaAno(left, currentYear)))
                .limit(Math.max(0, feriadosCache.size() - MAX_FERIADOS_CACHE_ENTRIES))
                .toList()
                .forEach(feriadosCache::remove);
    }

    private boolean foraDaJanelaDeRetencao(String key, int currentYear) {
        int year = extrairAno(key);
        if (year == Integer.MIN_VALUE) {
            return false;
        }
        return year < currentYear - RETAIN_YEAR_BEFORE || year > currentYear + RETAIN_YEAR_AFTER;
    }

    private int distanciaAno(String key, int currentYear) {
        int year = extrairAno(key);
        if (year == Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return Math.abs(year - currentYear);
    }

    private int extrairAno(String key) {
        if (key == null || key.isBlank()) {
            return Integer.MIN_VALUE;
        }
        int idx = key.lastIndexOf(':');
        if (idx < 0 || idx == key.length() - 1) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(key.substring(idx + 1));
        } catch (NumberFormatException ex) {
            return Integer.MIN_VALUE;
        }
    }

    private Set<LocalDate> feriadosNacionais(int ano) {
        Set<LocalDate> f = new HashSet<>();
        f.add(LocalDate.of(ano, 1, 1));
        f.add(LocalDate.of(ano, 4, 21));
        f.add(LocalDate.of(ano, 5, 1));
        f.add(LocalDate.of(ano, 9, 7));
        f.add(LocalDate.of(ano, 10, 12));
        f.add(LocalDate.of(ano, 11, 2));
        f.add(LocalDate.of(ano, 11, 15));
        f.add(LocalDate.of(ano, 11, 20));
        f.add(LocalDate.of(ano, 12, 25));
        adicionarFeriadosMoveis(f, ano);
        return f;
    }

    private void adicionarFeriadosMoveis(Set<LocalDate> feriados, int ano) {
        LocalDate pascoa = calcularPascoa(ano);
        feriados.add(pascoa.minusDays(2));
        feriados.add(pascoa.plusDays(60));
    }

    private LocalDate calcularPascoa(int ano) {
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

    private ContextoCalendario resolverContexto(String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau) {
        String codigo = normalizarTribunal(tribunalCodigo);
        ConfiguracaoPrazo configuracao = localizarConfiguracao(codigo, ramo, grau);
        Jurisdicao jurisdicao = localizarJurisdicao(codigo);
        String uf = jurisdicao != null ? normalizarUf(jurisdicao.getEstado()) : null;
        String comarca = jurisdicao != null ? normalizarTexto(jurisdicao.getComarca()) : null;
        boolean contarSabado = configuracao != null && configuracao.contarSabado();
        boolean integralmenteCorrido = configuracao != null && configuracao.integralmenteCorrido();
        Set<LocalDate> feriadosAdicionais = configuracao != null && configuracao.feriadosAdicionais() != null
                ? Set.copyOf(configuracao.feriadosAdicionais())
                : Set.of();
        return new ContextoCalendario(codigo, uf, comarca, contarSabado, integralmenteCorrido, feriadosAdicionais);
    }

    private ConfiguracaoPrazo localizarConfiguracao(String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau) {
        String codigo = normalizarTribunal(tribunalCodigo);
        String exata = chaveConfiguracao(codigo, ramo, grau);
        ConfiguracaoPrazo cfg = configuracoes.get(exata);
        if (cfg != null) {
            return cfg;
        }
        cfg = configuracoes.get(chaveConfiguracao(codigo, ramo, null));
        if (cfg != null) {
            return cfg;
        }
        cfg = configuracoes.get(chaveConfiguracao(codigo, null, grau));
        if (cfg != null) {
            return cfg;
        }
        cfg = configuracoes.get(chaveConfiguracao(codigo, null, null));
        if (cfg != null) {
            return cfg;
        }
        return configuracoes.get(chaveConfiguracao("NACIONAL", ramo, grau));
    }

    private Jurisdicao localizarJurisdicao(String tribunalCodigo) {
        if (tribunalCodigo == null || tribunalCodigo.isBlank() || "NACIONAL".equals(tribunalCodigo)) {
            return null;
        }
        return jurisdicaoRepository.findByCodigo(tribunalCodigo)
                .or(() -> jurisdicaoRepository.findBySiglaIgnoreCase(tribunalCodigo))
                .orElse(null);
    }

    private ConfiguracaoPrazo sanitizarConfiguracao(ConfiguracaoPrazo configuracao) {
        Set<LocalDate> feriados = configuracao.feriadosAdicionais() == null
                ? Set.of()
                : Set.copyOf(configuracao.feriadosAdicionais());
        return new ConfiguracaoPrazo(
                normalizarTribunal(configuracao.tribunalCodigo()),
                configuracao.ramo(),
                configuracao.grau(),
                feriados,
                configuracao.contarSabado(),
                configuracao.integralmenteCorrido()
        );
    }

    private String chaveConfiguracao(String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau) {
        return normalizarTribunal(tribunalCodigo) + "|" + (ramo != null ? ramo.name() : "*") + "|" + (grau != null ? grau.name() : "*");
    }

    private String normalizarTribunal(String tribunalCodigo) {
        if (tribunalCodigo == null || tribunalCodigo.isBlank()) {
            return "NACIONAL";
        }
        return tribunalCodigo.trim().toUpperCase();
    }

    private String normalizarUf(String uf) {
        if (uf == null || uf.isBlank()) {
            return null;
        }
        String v = uf.trim().toUpperCase();
        return v.length() == 2 ? v : null;
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String v = texto.trim();
        return v.isBlank() ? null : v;
    }

    private boolean estaNoRecessoFimDeAno(LocalDate data) {
        int mes = data.getMonthValue();
        int dia = data.getDayOfMonth();
        return (mes == 12 && dia >= 20) || (mes == 1 && dia <= 20);
    }

    private boolean estaNaSuspensaoCarnaval(LocalDate data) {
        LocalDate pascoa = calcularPascoa(data.getYear());
        LocalDate segundaCarnaval = pascoa.minusDays(48);
        LocalDate quartaCinzas = pascoa.minusDays(46);
        return !data.isBefore(segundaCarnaval) && !data.isAfter(quartaCinzas);
    }

    private List<String> construirAdvertenciasEstruturais(TipoPrazo tipo,
                                                          RamoDireito ramo,
                                                          GrauJurisdicao grau,
                                                          ContextoCalendario contexto) {
        Map<String, String> avisos = new LinkedHashMap<>();
        if (ramo == RamoDireito.ELEITORAL) {
            avisos.put("eleitoral", "Prazos eleitorais podem sofrer aceleração por calendário específico do TSE");
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            avisos.put("trabalhista", "Na Justiça do Trabalho, audiência designada e atos ordinatórios podem redefinir a contagem prática");
        }
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            avisos.put("penal", "Prazos penais e militares exigem conferência do marco intimatório real e da forma de ciência");
        }
        if (grau == GrauJurisdicao.SUPERIOR || grau == GrauJurisdicao.CONSTITUCIONAL) {
            avisos.put("superior", "Tribunais superiores podem impor filtros regimentais e marcos próprios de admissibilidade");
        }
        if (tipo == TipoPrazo.RECURSO_ESPECIAL || tipo == TipoPrazo.RECURSO_EXTRAORDINARIO) {
            avisos.put("especial_extraordinario", "Recurso excepcional demanda conferência adicional de tempestividade, preparo e admissibilidade");
        }
        if (!"NACIONAL".equals(contexto.tribunalCodigo()) && (contexto.uf() == null || contexto.uf().isBlank())) {
            avisos.put("tribunal_sem_contexto", "Tribunal informado sem UF/comarca resolvida; feriados locais dependem de cadastro institucional");
        }
        return List.copyOf(avisos.values());
    }
}
