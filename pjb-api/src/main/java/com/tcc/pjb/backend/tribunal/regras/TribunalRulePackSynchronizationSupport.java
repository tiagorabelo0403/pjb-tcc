package com.tcc.pjb.backend.tribunal.regras;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import com.tcc.pjb.backend.tribunal.regras.snapshot.RegraResolvida;

@Component
final class TribunalRulePackSynchronizationSupport {

    private final NationalRulePackEngine nationalRulePackEngine;
    private final TribunalRuleResolutionSupport resolutionSupport;

    TribunalRulePackSynchronizationSupport(NationalRulePackEngine nationalRulePackEngine,
                                           TribunalRuleResolutionSupport resolutionSupport) {
        this.nationalRulePackEngine = Objects.requireNonNull(nationalRulePackEngine);
        this.resolutionSupport = Objects.requireNonNull(resolutionSupport);
    }

    Set<String> listarTribunaisComRegrasCustomizadas(List<TribunalRuleEngine.EntradaRegra> entradas) {
        return entradas.stream()
                .map(TribunalRuleEngine.EntradaRegra::escopoId)
                .filter(Objects::nonNull)
                .filter(item -> !"BRASIL".equalsIgnoreCase(item))
                .filter(item -> !item.startsWith("COMARCA-") && !item.startsWith("VARA-"))
                .filter(item -> item.length() <= 20)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    void sincronizarRulePackAdaptadoTribunalInterno(String tribunalCodigo,
                                                    Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                                    List<RegraResolvida> logAuditoria,
                                                    Map<String, Set<RulePackSyncBucket>> rulePackBucketsSincronizados,
                                                    Map<String, Instant> syncBucketTouch,
                                                    int maxSyncedTribunals,
                                                    Set<String> chavesAdaptaveisRulePack,
                                                    List<TribunalRuleEngine.EntradaRegra> todasAsRegras) {
        String tribunal = TribunalRuleEngine.normalizeScope(tribunalCodigo);
        if (tribunal == null || "BRASIL".equals(tribunal)) {
            return;
        }

        Set<RulePackSyncBucket> bucketsAnteriores = new LinkedHashSet<>(rulePackBucketsSincronizados.getOrDefault(tribunal, Set.of()));
        Map<RulePackSyncBucket, List<NationalRulePackEngine.Regra>> adaptadas = new LinkedHashMap<>();
        Set<RulePackSyncBucket> bucketsDesejados = new LinkedHashSet<>();
        bucketsDesejados.add(new RulePackSyncBucket(tribunal, null, null));
        todasAsRegras.stream()
                .filter(item -> chavesAdaptaveisRulePack.contains(item.chave().canonical()))
                .filter(item -> item.nivel() == TribunalRuleEngine.NivelRegra.TRIBUNAL)
                .filter(item -> Objects.equals(item.escopoId(), tribunal))
                .forEach(item -> bucketsDesejados.add(new RulePackSyncBucket(tribunal, item.ramoAlvo(), item.grauAlvo())));

        for (RulePackSyncBucket bucket : bucketsDesejados) {
            List<NationalRulePackEngine.Regra> regras = adaptarRulePackTribunal(bucket.tribunalCodigo(), bucket.ramo(), bucket.grau(), chavesAdaptaveisRulePack, repositorio, logAuditoria);
            if (!regras.isEmpty()) {
                adaptadas.put(bucket, regras);
            }
        }

        Set<RulePackSyncBucket> bucketsFinais = new LinkedHashSet<>(adaptadas.keySet());
        for (RulePackSyncBucket bucket : bucketsAnteriores) {
            if (!bucketsFinais.contains(bucket)) {
                nationalRulePackEngine.removerRegrasCustomizadas(adapterOwnerKey(tribunal), bucket.tribunalCodigo(), bucket.ramo(), bucket.grau());
            }
        }
        for (Map.Entry<RulePackSyncBucket, List<NationalRulePackEngine.Regra>> entry : adaptadas.entrySet()) {
            RulePackSyncBucket bucket = entry.getKey();
            nationalRulePackEngine.substituirRegrasCustomizadas(adapterOwnerKey(tribunal), bucket.tribunalCodigo(), bucket.ramo(), bucket.grau(), entry.getValue());
        }
        if (bucketsFinais.isEmpty()) {
            rulePackBucketsSincronizados.remove(tribunal);
            syncBucketTouch.remove(tribunal);
        } else {
            rulePackBucketsSincronizados.put(tribunal, Set.copyOf(bucketsFinais));
            syncBucketTouch.put(tribunal, Instant.now());
            trimSyncBucketRegistry(rulePackBucketsSincronizados, syncBucketTouch, maxSyncedTribunals);
        }
    }

    private void trimSyncBucketRegistry(Map<String, Set<RulePackSyncBucket>> rulePackBucketsSincronizados,
                                        Map<String, Instant> syncBucketTouch,
                                        int maxSyncedTribunals) {
        if (syncBucketTouch.size() <= maxSyncedTribunals) {
            return;
        }
        int removeCount = syncBucketTouch.size() - maxSyncedTribunals;
        syncBucketTouch.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(removeCount)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(tribunal -> {
                    rulePackBucketsSincronizados.remove(tribunal);
                    syncBucketTouch.remove(tribunal);
                });
    }

    private List<NationalRulePackEngine.Regra> adaptarRulePackTribunal(String tribunalCodigo,
                                                                       RamoDireito ramo,
                                                                       GrauJurisdicao grau,
                                                                       Set<String> chavesAdaptaveisRulePack,
                                                                       Map<String, java.util.EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                                                       List<RegraResolvida> logAuditoria) {
        TribunalRuleEngine.ContextoResolucao contextoTribunal = new TribunalRuleEngine.ContextoResolucao(tribunalCodigo, null, null, ramo, grau, Instant.now());
        TribunalRuleEngine.ContextoResolucao contextoNacional = new TribunalRuleEngine.ContextoResolucao("BRASIL", null, null, ramo, grau, Instant.now());
        LinkedHashMap<String, NationalRulePackEngine.Regra> resultado = new LinkedHashMap<>();
        for (String canonical : chavesAdaptaveisRulePack) {
            TribunalRuleEngine.ChaveRegra chave = TribunalRuleEngine.ChaveRegra.de(canonical);
            Optional<RegraResolvida> resolvidaTribunal = resolutionSupport.resolver(chave, contextoTribunal, repositorio, logAuditoria);
            if (resolvidaTribunal.isEmpty() || resolvidaTribunal.get().nivelUsado() == TribunalRuleEngine.NivelRegra.NACIONAL) {
                continue;
            }
            Optional<RegraResolvida> resolvidaNacional = resolutionSupport.resolver(chave, contextoNacional, repositorio, logAuditoria);
            if (resolvidaNacional.isPresent() && Objects.equals(String.valueOf(resolvidaNacional.get().valor()), String.valueOf(resolvidaTribunal.get().valor()))) {
                continue;
            }
            for (NationalRulePackEngine.Regra regra : adaptarRegraResolvida(resolvidaTribunal.get(), tribunalCodigo, ramo)) {
                if (regra != null && regra.codigo() != null && !regra.codigo().isBlank()) {
                    resultado.put(TribunalRuleEngine.normalizeToken(regra.codigo()), regra);
                }
            }
        }
        return List.copyOf(resultado.values());
    }

    private List<NationalRulePackEngine.Regra> adaptarRegraResolvida(RegraResolvida regra, String tribunalCodigo, RamoDireito ramo) {
        List<NationalRulePackEngine.Regra> regras = new ArrayList<>();
        String codigoBase = adapterRuleCode(tribunalCodigo, ramo, regra.chave().chave());
        RamoDireito ramoEfetivo = ramo != null ? ramo : inferirRamoRulePack(tribunalCodigo);
        String fundamento = TribunalRuleEngine.blankToDefault(regra.fundamentacao(), "TribunalRuleEngine");

        if (Objects.equals(regra.chave().canonical(), TribunalRuleEngine.ChaveRegra.PRAZO_DESPACHO_INICIAL.canonical())) {
            regras.add(new NationalRulePackEngine.RegraPrazoEspecifico(codigoBase, "Prazo local para despacho inicial", ramoEfetivo, "DESPACHO_INICIAL", regra.inteiro(), true, fundamento));
        } else if (Objects.equals(regra.chave().canonical(), TribunalRuleEngine.ChaveRegra.PRAZO_DECISAO_INTERLOCUT.canonical())) {
            regras.add(new NationalRulePackEngine.RegraPrazoEspecifico(codigoBase, "Prazo local para decisão interlocutória", ramoEfetivo, "DECISAO_INTERLOCUTORIA", regra.inteiro(), true, fundamento));
        } else if (Objects.equals(regra.chave().canonical(), TribunalRuleEngine.ChaveRegra.PRAZO_SENTENCA.canonical())) {
            regras.add(new NationalRulePackEngine.RegraPrazoEspecifico(codigoBase, "Prazo local para sentença", ramoEfetivo, "SENTENCA", regra.inteiro(), true, fundamento));
        } else if (Objects.equals(regra.chave().canonical(), TribunalRuleEngine.ChaveRegra.PRAZO_CITACAO.canonical())) {
            regras.add(new NationalRulePackEngine.RegraPrazoEspecifico(codigoBase, "Prazo local para expedição de citação", ramoEfetivo, "CITACAO", regra.inteiro(), true, fundamento));
        } else if (Objects.equals(regra.chave().canonical(), TribunalRuleEngine.ChaveRegra.PRAZO_AUDIENCIA_UNA.canonical())) {
            regras.add(new NationalRulePackEngine.RegraPrazoEspecifico(codigoBase, "Prazo local para audiência una", ramoEfetivo, "AUDIENCIA_UNA", regra.inteiro(), true, fundamento));
        } else if (Objects.equals(regra.chave().canonical(), TribunalRuleEngine.ChaveRegra.AUDIENCIA_CONCIL_PRAZO.canonical())) {
            regras.add(new NationalRulePackEngine.RegraPrazoEspecifico(codigoBase, "Prazo local para audiência de conciliação", ramoEfetivo, "AUDIENCIA_CONCILIACAO", regra.inteiro(), true, fundamento));
        } else if (Objects.equals(regra.chave().canonical(), TribunalRuleEngine.ChaveRegra.TRIAGEM_PRAZO_ANALISE_H.canonical())) {
            regras.add(new NationalRulePackEngine.RegraAlerta(codigoBase, "Prazo operacional de triagem", ramoEfetivo, "Triagem inicial deve observar SLA de " + regra.inteiro() + " hora(s).", "INFO"));
        } else if (Objects.equals(regra.chave().canonical(), TribunalRuleEngine.ChaveRegra.TRIAGEM_OAB_OBRIGATORIA.canonical())) {
            if (regra.booleano()) {
                regras.add(new NationalRulePackEngine.RegraRequisito(codigoBase, "Validação de inscrição profissional obrigatória", ramoEfetivo, List.of("OAB_REGULAR"), true));
            }
        } else if (Objects.equals(regra.chave().canonical(), TribunalRuleEngine.ChaveRegra.TRIAGEM_VALOR_MIN_CAUSA.canonical())) {
            regras.add(new NationalRulePackEngine.RegraAdmissibilidade(codigoBase, "Valor mínimo local da causa", ramoEfetivo, List.of("VALOR_MINIMO_CAUSA >= R$ " + regra.decimal().toPlainString()), fundamento));
        } else if (Objects.equals(regra.chave().canonical(), TribunalRuleEngine.ChaveRegra.AUDIENCIA_CONCIL_OBRIG.canonical())) {
            if (regra.booleano()) {
                regras.add(new NationalRulePackEngine.RegraFluxo(codigoBase, "Fluxo local com audiência de conciliação obrigatória", ramoEfetivo, "TRIAGEM", "AUDIENCIA_CONCILIACAO", true));
            }
        }
        return regras;
    }

    private String adapterOwnerKey(String tribunalCodigo) {
        return "TRIBUNAL_RULE_ENGINE_ADAPTER::" + TribunalRuleEngine.normalizeScope(tribunalCodigo);
    }

    private String adapterRuleCode(String tribunalCodigo, RamoDireito ramo, String sufixo) {
        return "TRIBRULE_" + TribunalRuleEngine.normalizeScope(tribunalCodigo) + "_" + (ramo == null ? "GERAL" : ramo.name()) + "_" + TribunalRuleEngine.normalizeToken(sufixo);
    }

    private RamoDireito inferirRamoRulePack(String tribunalCodigo) {
        String token = TribunalRuleEngine.normalizeScope(tribunalCodigo);
        if (token == null) {
            return RamoDireito.CIVIL;
        }
        if (token.startsWith("TRT") || "TST".equals(token)) {
            return RamoDireito.TRABALHISTA;
        }
        if (token.startsWith("TRF")) {
            return RamoDireito.PREVIDENCIARIO;
        }
        if (token.startsWith("TRE") || "TSE".equals(token)) {
            return RamoDireito.ELEITORAL;
        }
        if (token.startsWith("TJM") || "STM".equals(token)) {
            return RamoDireito.MILITAR;
        }
        return RamoDireito.CIVIL;
    }
}
