package com.tcc.pjb.backend.tribunal.regras;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import com.tcc.pjb.backend.tribunal.regras.snapshot.RegraResolvida;
import com.tcc.pjb.backend.tribunal.regras.snapshot.SnapshotDistribuicao;
import com.tcc.pjb.backend.tribunal.regras.snapshot.SnapshotPrazo;
import com.tcc.pjb.backend.tribunal.regras.snapshot.SnapshotTriagem;

@Component
final class TribunalRuleResolutionSupport {

    private final NationalRulePackEngine nationalRulePackEngine;
    private final NationalPrazoEngine nationalPrazoEngine;
    private final SalarioMinimoNacionalService salarioMinimoNacionalService;

    TribunalRuleResolutionSupport(NationalRulePackEngine nationalRulePackEngine,
                                  NationalPrazoEngine nationalPrazoEngine,
                                  SalarioMinimoNacionalService salarioMinimoNacionalService) {
        this.nationalRulePackEngine = Objects.requireNonNull(nationalRulePackEngine);
        this.nationalPrazoEngine = Objects.requireNonNull(nationalPrazoEngine);
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
    }

    Optional<RegraResolvida> resolver(TribunalRuleEngine.ChaveRegra chave,
                                      TribunalRuleEngine.ContextoResolucao contexto,
                                      Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                      List<RegraResolvida> logAuditoria) {
        Objects.requireNonNull(chave, "chave");
        Objects.requireNonNull(contexto, "contexto");

        Map<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>> porNivel = repositorio.get(chave.canonical());
        if (porNivel == null || porNivel.isEmpty()) {
            return Optional.empty();
        }

        List<TribunalRuleEngine.EntradaRegra> cadeia = coletarCadeiaAplicavel(chave, contexto, porNivel);
        if (cadeia.isEmpty()) {
            return Optional.empty();
        }

        EstadoAplicacao estado = null;
        for (TribunalRuleEngine.EntradaRegra entrada : cadeia) {
            if (estado == null) {
                LinkedHashSet<String> trilha = new LinkedHashSet<>();
                trilha.add("BASE " + entrada.nivel().name() + "[" + entrada.escopoId() + "]=" + TribunalRuleEngine.valuePreview(entrada.valor()) + TribunalRuleEngine.fundamentoSuffix(entrada.fundamentacao()));
                estado = new EstadoAplicacao(
                        TribunalRuleEngine.normalizarValorPorTipo(entrada.valor(), entrada.tipoValor()),
                        entrada.tipoValor(),
                        entrada,
                        false,
                        trilha
                );
                continue;
            }
            estado = aplicarEntrada(estado, entrada);
        }

        TribunalRuleEngine.EntradaRegra fonte = estado.fonte();
        boolean usouFallback = fonte.nivel().prioridade() < TribunalRuleEngine.nivelMaisEspecificoInformado(contexto).prioridade();

        RegraResolvida resolvida = new RegraResolvida(
                chave,
                estado.valor(),
                estado.tipoValor(),
                fonte.nivel(),
                fonte.escopoId(),
                fonte.fundamentacao(),
                usouFallback,
                estado.restringirAplicado(),
                List.copyOf(estado.trilha()),
                Instant.now(),
                contexto.tribunalCodigo(),
                contexto.ramo(),
                contexto.grau()
        );

        registrarAuditoria(resolvida, logAuditoria);
        return Optional.of(resolvida);
    }

    RegraResolvida resolverOuDefault(TribunalRuleEngine.ChaveRegra chave,
                                     TribunalRuleEngine.ContextoResolucao contexto,
                                     Object valorDefault,
                                     TribunalRuleEngine.TipoValor tipo,
                                     Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                     List<RegraResolvida> logAuditoria) {
        return resolver(chave, contexto, repositorio, logAuditoria).orElseGet(() -> new RegraResolvida(
                chave,
                TribunalRuleEngine.normalizarValorPorTipo(valorDefault, tipo),
                tipo,
                TribunalRuleEngine.NivelRegra.NACIONAL,
                "DEFAULT-SISTEMA",
                "Valor padrão interno",
                true,
                false,
                List.of("DEFAULT " + TribunalRuleEngine.valuePreview(valorDefault)),
                Instant.now(),
                contexto == null ? null : contexto.tribunalCodigo(),
                contexto == null ? null : contexto.ramo(),
                contexto == null ? null : contexto.grau()
        ));
    }

    int resolverPrazoDias(TribunalRuleEngine.ChaveRegra chave,
                          TribunalRuleEngine.ContextoResolucao contexto,
                          int defaultDias,
                          Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                          List<RegraResolvida> logAuditoria) {
        return resolverOuDefault(chave, contexto, defaultDias, TribunalRuleEngine.TipoValor.DURACAO_DIAS, repositorio, logAuditoria).inteiro();
    }

    BigDecimal resolverDecimal(TribunalRuleEngine.ChaveRegra chave,
                               TribunalRuleEngine.ContextoResolucao contexto,
                               BigDecimal defaultValue,
                               Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                               List<RegraResolvida> logAuditoria) {
        return resolverOuDefault(chave, contexto, defaultValue, TribunalRuleEngine.TipoValor.DECIMAL, repositorio, logAuditoria).decimal();
    }

    boolean resolverBooleano(TribunalRuleEngine.ChaveRegra chave,
                             TribunalRuleEngine.ContextoResolucao contexto,
                             boolean defaultValue,
                             Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                             List<RegraResolvida> logAuditoria) {
        return resolverOuDefault(chave, contexto, defaultValue, TribunalRuleEngine.TipoValor.BOOLEANO, repositorio, logAuditoria).booleano();
    }

    String resolverTexto(TribunalRuleEngine.ChaveRegra chave,
                         TribunalRuleEngine.ContextoResolucao contexto,
                         String defaultValue,
                         Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                         List<RegraResolvida> logAuditoria) {
        return resolverOuDefault(chave, contexto, defaultValue, TribunalRuleEngine.TipoValor.TEXTO, repositorio, logAuditoria).texto();
    }

    Map<String, RegraResolvida> resolverBatch(List<TribunalRuleEngine.ChaveRegra> chaves,
                                              TribunalRuleEngine.ContextoResolucao contexto,
                                              Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                              List<RegraResolvida> logAuditoria) {
        if (chaves == null || chaves.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, RegraResolvida> resultado = new LinkedHashMap<>();
        for (TribunalRuleEngine.ChaveRegra chave : chaves) {
            resolver(chave, contexto, repositorio, logAuditoria).ifPresent(regra -> resultado.put(chave.canonical(), regra));
        }
        return Collections.unmodifiableMap(resultado);
    }

    Map<String, Integer> resolverTodosPrazos(TribunalRuleEngine.ContextoResolucao contexto,
                                             Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                             List<RegraResolvida> logAuditoria) {
        LinkedHashMap<String, Integer> mapa = new LinkedHashMap<>();
        repositorio.keySet().stream()
                .filter(key -> key.startsWith("prazo."))
                .sorted()
                .forEach(key -> resolver(TribunalRuleEngine.ChaveRegra.de(key), contexto, repositorio, logAuditoria).ifPresent(resolvida -> {
                    try {
                        mapa.put(resolvida.chave().canonical(), resolvida.inteiro());
                    } catch (Exception ignored) {
                    }
                }));
        return Collections.unmodifiableMap(mapa);
    }

    SnapshotDistribuicao resolverSnapshotDistribuicao(TribunalRuleEngine.ContextoResolucao contexto,
                                                      Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                                      List<RegraResolvida> logAuditoria) {
        BigDecimal salarios = resolverDecimal(TribunalRuleEngine.ChaveRegra.DIST_LIMITE_JEC_SALARIOS, contexto, new BigDecimal("40"), repositorio, logAuditoria);
        BigDecimal reais = resolverLimiteJuizadoEmReais(contexto, salarios);
        BigDecimal limiar = resolverDecimal(TribunalRuleEngine.ChaveRegra.DIST_LIMIAR_CONGESTION, contexto, new BigDecimal("0.85"), repositorio, logAuditoria);
        int capacidade = resolverOuDefault(TribunalRuleEngine.ChaveRegra.DIST_CAPACIDADE_MAXIMA, contexto, 3000, TribunalRuleEngine.TipoValor.INTEIRO, repositorio, logAuditoria).inteiro();
        return new SnapshotDistribuicao(
                salarios.setScale(2, RoundingMode.HALF_UP),
                reais.setScale(2, RoundingMode.HALF_UP),
                limiar.setScale(4, RoundingMode.HALF_UP),
                capacidade,
                Instant.now()
        );
    }

    SnapshotTriagem resolverSnapshotTriagem(TribunalRuleEngine.ContextoResolucao contexto,
                                            Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                            List<RegraResolvida> logAuditoria) {
        return new SnapshotTriagem(
                resolverOuDefault(TribunalRuleEngine.ChaveRegra.TRIAGEM_PRAZO_ANALISE_H, contexto, 48, TribunalRuleEngine.TipoValor.INTEIRO, repositorio, logAuditoria).inteiro(),
                resolverBooleano(TribunalRuleEngine.ChaveRegra.TRIAGEM_OAB_OBRIGATORIA, contexto, true, repositorio, logAuditoria),
                resolverDecimal(TribunalRuleEngine.ChaveRegra.TRIAGEM_VALOR_MIN_CAUSA, contexto, new BigDecimal("1.00"), repositorio, logAuditoria),
                resolverBooleano(TribunalRuleEngine.ChaveRegra.AUDIENCIA_CONCIL_OBRIG, contexto, true, repositorio, logAuditoria),
                resolverPrazoDias(TribunalRuleEngine.ChaveRegra.AUDIENCIA_CONCIL_PRAZO, contexto, 30, repositorio, logAuditoria),
                resolverBooleano(TribunalRuleEngine.ChaveRegra.SIGILO_FAMILIA_AUTO, contexto, true, repositorio, logAuditoria),
                resolverBooleano(TribunalRuleEngine.ChaveRegra.SIGILO_MENOR_AUTO, contexto, true, repositorio, logAuditoria),
                resolverTexto(TribunalRuleEngine.ChaveRegra.NOTIF_CANAL_PADRAO, contexto, "EMAIL", repositorio, logAuditoria),
                resolverBooleano(TribunalRuleEngine.ChaveRegra.NOTIF_WHATSAPP_ATIVO, contexto, false, repositorio, logAuditoria),
                Instant.now()
        );
    }

    SnapshotPrazo resolverSnapshotPrazo(TribunalRuleEngine.ContextoResolucao contexto,
                                        Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                        List<RegraResolvida> logAuditoria) {
        return new SnapshotPrazo(
                resolverTodosPrazos(contexto, repositorio, logAuditoria),
                resolverOuDefault(TribunalRuleEngine.ChaveRegra.RECESSO_DEZ_INICIO_DIA, contexto, 20, TribunalRuleEngine.TipoValor.INTEIRO, repositorio, logAuditoria).inteiro(),
                resolverOuDefault(TribunalRuleEngine.ChaveRegra.RECESSO_JAN_FIM_DIA, contexto, 6, TribunalRuleEngine.TipoValor.INTEIRO, repositorio, logAuditoria).inteiro(),
                Instant.now()
        );
    }

    BigDecimal resolverLimiarCongestionamento(TribunalRuleEngine.ContextoResolucao contexto,
                                              BigDecimal fallback,
                                              Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                              List<RegraResolvida> logAuditoria) {
        BigDecimal base = fallback == null ? new BigDecimal("0.85") : fallback;
        return resolverDecimal(TribunalRuleEngine.ChaveRegra.DIST_LIMIAR_CONGESTION, contexto, base, repositorio, logAuditoria).setScale(4, RoundingMode.HALF_UP);
    }

    int resolverCapacidadeMaximaVara(TribunalRuleEngine.ContextoResolucao contexto,
                                     int fallback,
                                     Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                     List<RegraResolvida> logAuditoria) {
        return resolverOuDefault(TribunalRuleEngine.ChaveRegra.DIST_CAPACIDADE_MAXIMA, contexto, fallback, TribunalRuleEngine.TipoValor.INTEIRO, repositorio, logAuditoria).inteiro();
    }

    BigDecimal resolverLimiteJuizadoEmReais(TribunalRuleEngine.ContextoResolucao contexto,
                                            Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                            List<RegraResolvida> logAuditoria) {
        BigDecimal salarios = resolverDecimal(TribunalRuleEngine.ChaveRegra.DIST_LIMITE_JEC_SALARIOS, contexto, new BigDecimal("40"), repositorio, logAuditoria);
        return resolverLimiteJuizadoEmReais(contexto, salarios);
    }

    Map<String, Object> resolverExtrasIntegracao(TribunalRuleEngine.ContextoResolucao contexto,
                                                 Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                                 List<RegraResolvida> logAuditoria) {
        SnapshotTriagem triagem = resolverSnapshotTriagem(contexto, repositorio, logAuditoria);
        SnapshotDistribuicao distribuicao = resolverSnapshotDistribuicao(contexto, repositorio, logAuditoria);
        SnapshotPrazo prazo = resolverSnapshotPrazo(contexto, repositorio, logAuditoria);

        LinkedHashMap<String, Object> extras = new LinkedHashMap<>();
        extras.put("prazoAnaliseHoras", triagem.prazoAnaliseHoras());
        extras.put("validacaoOabObrigatoria", triagem.validacaoOabObrigatoria());
        extras.put("valorMinimoCausa", triagem.valorMinimoCausa());
        extras.put("conciliacaoObrigatoria", triagem.conciliacaoObrigatoria());
        extras.put("prazoDesignacaoConciliacaoDias", triagem.prazoDesignacaoConciliacaoDias());
        extras.put("sigiloFamiliaAutomatico", triagem.sigiloFamiliaAutomatico());
        extras.put("sigiloMenorAutomatico", triagem.sigiloMenorAutomatico());
        extras.put("canalNotificacaoPadrao", triagem.canalNotificacaoPadrao());
        extras.put("notificacaoWhatsAppAtivo", triagem.notificacaoWhatsappAtivo());
        extras.put("limiteJuizadoSalariosMinimos", distribuicao.limiteJuizadoSalariosMinimos());
        extras.put("limiteJuizadoReais", distribuicao.limiteJuizadoReais());
        extras.put("limiarCongestionamento", distribuicao.limiarCongestionamento());
        extras.put("capacidadeMaximaVara", distribuicao.capacidadeMaximaVara());
        extras.put("recessoInicioDezembroDia", prazo.recessoInicioDezembroDia());
        extras.put("recessoFimJaneiroDia", prazo.recessoFimJaneiroDia());
        extras.put("prazosTribunal", prazo.prazosDias());
        return Collections.unmodifiableMap(extras);
    }

    NationalRulePackEngine.ContextoRegra enriquecerContextoNationalRulePack(NationalRulePackEngine.ContextoRegra contexto,
                                                                            String comarcaId,
                                                                            String varaId,
                                                                            Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                                                            List<RegraResolvida> logAuditoria) {
        Objects.requireNonNull(contexto, "contexto");
        TribunalRuleEngine.ContextoResolucao resolucao = TribunalRuleEngine.ContextoResolucao.fromNationalContexto(contexto, comarcaId, varaId);
        LinkedHashMap<String, Object> extras = new LinkedHashMap<>();
        if (contexto.extras() != null) {
            extras.putAll(contexto.extras());
        }
        extras.putAll(resolverExtrasIntegracao(resolucao, repositorio, logAuditoria));
        return new NationalRulePackEngine.ContextoRegra(
                contexto.classeTPU(),
                contexto.assuntoTPU(),
                contexto.ramo(),
                contexto.grau(),
                contexto.tribunalCodigo(),
                extras
        );
    }

    NationalRulePackEngine.ResultadoRegras aplicarRegraPackEnriquecido(NationalRulePackEngine.ContextoRegra contexto,
                                                                       String comarcaId,
                                                                       String varaId,
                                                                       Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                                                       List<RegraResolvida> logAuditoria) {
        return nationalRulePackEngine.aplicar(enriquecerContextoNationalRulePack(contexto, comarcaId, varaId, repositorio, logAuditoria));
    }

    void sincronizarConfiguracaoPrazo(TribunalRuleEngine.ContextoResolucao contexto,
                                      Set<LocalDate> feriadosAdicionais,
                                      boolean contarSabado,
                                      boolean integralmenteCorrido) {
        Objects.requireNonNull(contexto, "contexto");
        nationalPrazoEngine.registrarConfiguracao(new NationalPrazoEngine.ConfiguracaoPrazo(
                contexto.tribunalCodigo(),
                contexto.ramo(),
                contexto.grau(),
                feriadosAdicionais == null ? Set.of() : Set.copyOf(feriadosAdicionais),
                contarSabado,
                integralmenteCorrido
        ));
    }

    private List<TribunalRuleEngine.EntradaRegra> coletarCadeiaAplicavel(TribunalRuleEngine.ChaveRegra chave,
                                                                          TribunalRuleEngine.ContextoResolucao contexto,
                                                                          Map<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>> porNivel) {
        List<TribunalRuleEngine.EntradaRegra> cadeia = new ArrayList<>();
        appendMelhor(porNivel, TribunalRuleEngine.NivelRegra.NACIONAL, "BRASIL", contexto).ifPresent(cadeia::add);
        appendMelhor(porNivel, TribunalRuleEngine.NivelRegra.TRIBUNAL, contexto.tribunalCodigo(), contexto).ifPresent(cadeia::add);
        appendMelhor(porNivel, TribunalRuleEngine.NivelRegra.COMARCA, contexto.comarcaId(), contexto).ifPresent(cadeia::add);
        appendMelhor(porNivel, TribunalRuleEngine.NivelRegra.VARA, contexto.varaId(), contexto).ifPresent(cadeia::add);
        return cadeia.stream().filter(item -> Objects.equals(item.chave().canonical(), chave.canonical())).toList();
    }

    private Optional<TribunalRuleEngine.EntradaRegra> appendMelhor(Map<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>> porNivel,
                                                                   TribunalRuleEngine.NivelRegra nivel,
                                                                   String escopo,
                                                                   TribunalRuleEngine.ContextoResolucao contexto) {
        if (escopo == null) {
            return Optional.empty();
        }
        Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>> porEscopo = porNivel.get(nivel);
        if (porEscopo == null) {
            return Optional.empty();
        }
        CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra> entradas = porEscopo.get(TribunalRuleEngine.escopoKey(nivel, escopo));
        if (entradas == null || entradas.isEmpty()) {
            return Optional.empty();
        }
        return entradas.stream()
                .filter(item -> item.vigenteEm(contexto.momento()))
                .filter(item -> item.aplicaAo(contexto))
                .sorted(Comparator
                        .comparingInt(TribunalRuleEngine.EntradaRegra::especificidadeContextual).reversed()
                        .thenComparing(item -> item.vigenteDesde() == null ? Instant.EPOCH : item.vigenteDesde(), Comparator.reverseOrder()))
                .findFirst();
    }

    private EstadoAplicacao aplicarEntrada(EstadoAplicacao atual, TribunalRuleEngine.EntradaRegra nova) {
        LinkedHashSet<String> trilha = new LinkedHashSet<>(atual.trilha());
        boolean restringirAplicado = atual.restringirAplicado();
        Object valorAtual = atual.valor();
        Object valorNovo = TribunalRuleEngine.normalizarValorPorTipo(nova.valor(), nova.tipoValor());

        if (nova.modo() == TribunalRuleEngine.ModoSobrescrita.SUBSTITUIR) {
            trilha.add("SUBSTITUIR " + nova.nivel().name() + "[" + nova.escopoId() + "] " + TribunalRuleEngine.valuePreview(valorAtual) + " -> " + TribunalRuleEngine.valuePreview(valorNovo) + TribunalRuleEngine.fundamentoSuffix(nova.fundamentacao()));
            return new EstadoAplicacao(valorNovo, nova.tipoValor(), nova, restringirAplicado, trilha);
        }

        if (nova.modo() == TribunalRuleEngine.ModoSobrescrita.ESTENDER) {
            Object combinado = combinarValores(atual.tipoValor(), valorAtual, valorNovo);
            trilha.add("ESTENDER " + nova.nivel().name() + "[" + nova.escopoId() + "] " + TribunalRuleEngine.valuePreview(valorAtual) + " + " + TribunalRuleEngine.valuePreview(valorNovo) + " -> " + TribunalRuleEngine.valuePreview(combinado) + TribunalRuleEngine.fundamentoSuffix(nova.fundamentacao()));
            return new EstadoAplicacao(combinado, atual.tipoValor(), nova, restringirAplicado, trilha);
        }

        if (nova.modo() == TribunalRuleEngine.ModoSobrescrita.RESTRINGIR) {
            RestrictionOutcome outcome = aplicarRestricao(atual.tipoValor(), valorAtual, valorNovo);
            trilha.add("RESTRINGIR " + nova.nivel().name() + "[" + nova.escopoId() + "] " + TribunalRuleEngine.valuePreview(valorAtual) + " x " + TribunalRuleEngine.valuePreview(valorNovo) + " -> " + TribunalRuleEngine.valuePreview(outcome.valor()) + (outcome.aplicou() ? " aplicado" : " ignorado") + TribunalRuleEngine.fundamentoSuffix(nova.fundamentacao()));
            return new EstadoAplicacao(outcome.valor(), atual.tipoValor(), outcome.aplicou() ? nova : atual.fonte(), restringirAplicado || outcome.aplicou(), trilha);
        }

        trilha.add("SUBSTITUIR " + nova.nivel().name() + "[" + nova.escopoId() + "] " + TribunalRuleEngine.valuePreview(valorAtual) + " -> " + TribunalRuleEngine.valuePreview(valorNovo) + TribunalRuleEngine.fundamentoSuffix(nova.fundamentacao()));
        return new EstadoAplicacao(valorNovo, nova.tipoValor(), nova, restringirAplicado, trilha);
    }

    private Object combinarValores(TribunalRuleEngine.TipoValor tipoValor, Object valorAtual, Object valorNovo) {
        if (valorAtual == null) {
            return valorNovo;
        }
        if (valorNovo == null) {
            return valorAtual;
        }
        return switch (tipoValor) {
            case LISTA_TEXTO -> {
                LinkedHashSet<String> set = new LinkedHashSet<>();
                set.addAll(TribunalRuleEngine.toStringList(valorAtual));
                set.addAll(TribunalRuleEngine.toStringList(valorNovo));
                yield List.copyOf(set);
            }
            case TEXTO -> {
                LinkedHashSet<String> set = new LinkedHashSet<>();
                set.addAll(TribunalRuleEngine.toStringList(valorAtual));
                set.addAll(TribunalRuleEngine.toStringList(valorNovo));
                yield String.join(", ", set);
            }
            case DECIMAL -> TribunalRuleEngine.toBigDecimal(valorAtual).add(TribunalRuleEngine.toBigDecimal(valorNovo)).setScale(2, RoundingMode.HALF_UP);
            case INTEIRO, DURACAO_DIAS -> Integer.valueOf(TribunalRuleEngine.toBigDecimal(valorAtual).intValue() + TribunalRuleEngine.toBigDecimal(valorNovo).intValue());
            case BOOLEANO -> TribunalRuleEngine.toBoolean(valorAtual) || TribunalRuleEngine.toBoolean(valorNovo);
        };
    }

    private RestrictionOutcome aplicarRestricao(TribunalRuleEngine.TipoValor tipoValor, Object valorAtual, Object valorNovo) {
        if (valorAtual == null) {
            return new RestrictionOutcome(valorNovo, true);
        }
        if (valorNovo == null) {
            return new RestrictionOutcome(valorAtual, false);
        }
        try {
            return switch (tipoValor) {
                case DECIMAL -> {
                    BigDecimal atual = TribunalRuleEngine.toBigDecimal(valorAtual);
                    BigDecimal novo = TribunalRuleEngine.toBigDecimal(valorNovo);
                    boolean aplica = novo.compareTo(atual) <= 0;
                    yield new RestrictionOutcome(aplica ? novo.setScale(2, RoundingMode.HALF_UP) : atual.setScale(2, RoundingMode.HALF_UP), aplica);
                }
                case INTEIRO, DURACAO_DIAS -> {
                    int atual = TribunalRuleEngine.toBigDecimal(valorAtual).intValue();
                    int novo = TribunalRuleEngine.toBigDecimal(valorNovo).intValue();
                    boolean aplica = novo <= atual;
                    yield new RestrictionOutcome(aplica ? novo : atual, aplica);
                }
                case BOOLEANO -> {
                    boolean atual = TribunalRuleEngine.toBoolean(valorAtual);
                    boolean novo = TribunalRuleEngine.toBoolean(valorNovo);
                    boolean aplica = atual && !novo;
                    yield new RestrictionOutcome(aplica ? Boolean.FALSE : atual, aplica);
                }
                case LISTA_TEXTO -> {
                    List<String> atual = TribunalRuleEngine.toStringList(valorAtual);
                    List<String> novo = TribunalRuleEngine.toStringList(valorNovo);
                    LinkedHashSet<String> setAtual = new LinkedHashSet<>(atual);
                    LinkedHashSet<String> setNovo = new LinkedHashSet<>(novo);
                    boolean aplica = setAtual.containsAll(setNovo);
                    yield new RestrictionOutcome(aplica ? List.copyOf(setNovo) : List.copyOf(setAtual), aplica);
                }
                case TEXTO -> {
                    String atual = String.valueOf(valorAtual);
                    String novo = String.valueOf(valorNovo);
                    boolean aplica = atual.equalsIgnoreCase(novo) || novo.length() <= atual.length();
                    yield new RestrictionOutcome(aplica ? novo : atual, aplica);
                }
            };
        } catch (Exception e) {
            return new RestrictionOutcome(valorAtual, false);
        }
    }

    private void registrarAuditoria(RegraResolvida resolvida, List<RegraResolvida> logAuditoria) {
        synchronized (logAuditoria) {
            if (logAuditoria.size() >= 10_000) {
                logAuditoria.remove(0);
            }
            logAuditoria.add(resolvida);
        }
    }

    private BigDecimal resolverLimiteJuizadoEmReais(TribunalRuleEngine.ContextoResolucao contexto, BigDecimal quantidadeSalarios) {
        LocalDate data = LocalDate.ofInstant(contexto == null ? Instant.now() : contexto.momento(), ZoneId.systemDefault());
        return salarioMinimoNacionalService.multiplicar(quantidadeSalarios, data).setScale(2, RoundingMode.HALF_UP);
    }
}
