package com.tcc.pjb.backend.tribunal.regras;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import com.tcc.pjb.backend.tribunal.regras.snapshot.AnaliseDesvio;
import com.tcc.pjb.backend.tribunal.regras.snapshot.RegraResolvida;
import com.tcc.pjb.backend.tribunal.regras.snapshot.RelatorioCoberturaTribunal;

final class TribunalRuleAnalyticsSupport {

    List<AnaliseDesvio> analisarDesviosTribunal(String tribunalCodigo,
                                                RamoDireito ramo,
                                                GrauJurisdicao grau,
                                                Map<String, EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio,
                                                List<RegraResolvida> logAuditoria,
                                                TribunalRuleResolutionSupport resolutionSupport) {
        String tribunal = TribunalRuleEngine.normalizeScope(tribunalCodigo);
        if (tribunal == null) {
            return List.of();
        }

        TribunalRuleEngine.ContextoResolucao contextoTribunal = new TribunalRuleEngine.ContextoResolucao(tribunal, null, null, ramo, grau, Instant.now());
        TribunalRuleEngine.ContextoResolucao contextoNacional = new TribunalRuleEngine.ContextoResolucao("BRASIL", null, null, ramo, grau, Instant.now());
        List<AnaliseDesvio> desvios = new ArrayList<>();
        for (String key : repositorio.keySet()) {
            Optional<RegraResolvida> nacional = resolutionSupport.resolver(TribunalRuleEngine.ChaveRegra.de(key), contextoNacional, repositorio, logAuditoria);
            Optional<RegraResolvida> tribunalRegra = resolutionSupport.resolver(TribunalRuleEngine.ChaveRegra.de(key), contextoTribunal, repositorio, logAuditoria);
            if (nacional.isEmpty() || tribunalRegra.isEmpty()) {
                continue;
            }
            RegraResolvida n = nacional.get();
            RegraResolvida t = tribunalRegra.get();
            if (t.nivelUsado() == TribunalRuleEngine.NivelRegra.NACIONAL) {
                continue;
            }
            if (Objects.equals(String.valueOf(n.valor()), String.valueOf(t.valor()))) {
                continue;
            }
            double pct = TribunalRuleEngine.calcularPercentDesvio(n.valor(), t.valor());
            TribunalRuleEngine.NivelDesvio nivel = pct > 100 ? TribunalRuleEngine.NivelDesvio.CRITICO
                    : pct > 50 ? TribunalRuleEngine.NivelDesvio.RELEVANTE
                    : pct > 20 ? TribunalRuleEngine.NivelDesvio.MODERADO
                    : TribunalRuleEngine.NivelDesvio.LEVE;

            desvios.add(new AnaliseDesvio(
                    n.chave(),
                    tribunal,
                    String.valueOf(n.valor()),
                    String.valueOf(t.valor()),
                    pct,
                    nivel,
                    n.fundamentacao(),
                    t.fundamentacao()
            ));
        }
        desvios.sort(Comparator.comparingDouble(AnaliseDesvio::percentDesvio).reversed());
        return Collections.unmodifiableList(desvios);
    }

    RelatorioCoberturaTribunal relatorioCobertura(String tribunalCodigo,
                                                  Map<String, EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> repositorio) {
        String tribunal = TribunalRuleEngine.normalizeScope(tribunalCodigo);
        if (tribunal == null) {
            return new RelatorioCoberturaTribunal(null, 0, 0, 0, 0, List.of(), List.of(), Instant.now());
        }
        int totalNacionais = 0;
        int personalizadas = 0;
        List<String> chavesPersonalizadas = new ArrayList<>();
        List<String> chavesHerdadas = new ArrayList<>();

        for (Map.Entry<String, EnumMap<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>>> entry : repositorio.entrySet()) {
            Map<TribunalRuleEngine.NivelRegra, Map<String, CopyOnWriteArrayList<TribunalRuleEngine.EntradaRegra>>> porNivel = entry.getValue();
            boolean temNacional = TribunalRuleEngine.containsEscopo(porNivel, TribunalRuleEngine.NivelRegra.NACIONAL, "BRASIL");
            if (!temNacional) {
                continue;
            }
            totalNacionais++;
            boolean temTribunal = TribunalRuleEngine.containsEscopo(porNivel, TribunalRuleEngine.NivelRegra.TRIBUNAL, tribunal);
            if (temTribunal) {
                personalizadas++;
                chavesPersonalizadas.add(entry.getKey());
            } else {
                chavesHerdadas.add(entry.getKey());
            }
        }

        double percentual = totalNacionais == 0 ? 0d : (100d * personalizadas / totalNacionais);
        return new RelatorioCoberturaTribunal(
                tribunal,
                totalNacionais,
                personalizadas,
                Math.max(0, totalNacionais - personalizadas),
                TribunalRuleEngine.round2(percentual),
                List.copyOf(chavesPersonalizadas),
                List.copyOf(chavesHerdadas),
                Instant.now()
        );
    }

    List<RegraResolvida> consultarLog(String filtroEscopoOuTribunal, int limite, List<RegraResolvida> logAuditoria) {
        String filtro = TribunalRuleEngine.blankToNull(filtroEscopoOuTribunal);
        int max = Math.max(1, limite);
        synchronized (logAuditoria) {
            return logAuditoria.stream()
                    .filter(item -> filtro == null || filtro.equalsIgnoreCase(item.escopoUsado()) || filtro.equalsIgnoreCase(item.tribunalCodigoContexto()))
                    .sorted(Comparator.comparing(RegraResolvida::resolvidoEm).reversed())
                    .limit(max)
                    .toList();
        }
    }

    long totalRegrasFallback(String tribunalCodigo, List<RegraResolvida> logAuditoria) {
        String tribunal = TribunalRuleEngine.normalizeScope(tribunalCodigo);
        synchronized (logAuditoria) {
            return logAuditoria.stream()
                    .filter(RegraResolvida::usouFallback)
                    .filter(item -> Objects.equals(item.tribunalCodigoContexto(), tribunal))
                    .count();
        }
    }

    List<TribunalRuleEngine.EntradaRegra> listarPorTribunal(String tribunalCodigo, List<TribunalRuleEngine.EntradaRegra> todas) {
        String tribunal = TribunalRuleEngine.normalizeScope(tribunalCodigo);
        return todas.stream()
                .filter(item -> item.nivel() == TribunalRuleEngine.NivelRegra.NACIONAL || Objects.equals(item.escopoId(), tribunal))
                .sorted(Comparator.comparing((TribunalRuleEngine.EntradaRegra item) -> item.chave().canonical())
                        .thenComparing(item -> item.nivel().prioridade()))
                .toList();
    }

    List<TribunalRuleEngine.EntradaRegra> listarExpiradas(List<TribunalRuleEngine.EntradaRegra> todas) {
        return todas.stream()
                .filter(TribunalRuleEngine.EntradaRegra::expirada)
                .sorted(Comparator.comparing(item -> item.vigenteAte() == null ? Instant.EPOCH : item.vigenteAte(), Comparator.reverseOrder()))
                .toList();
    }

    int totalRegrasAtivas(Collection<TribunalRuleEngine.EntradaRegra> todas) {
        return Math.toIntExact(todas.stream().filter(item -> item.ativa() && !item.expirada()).count());
    }
}
