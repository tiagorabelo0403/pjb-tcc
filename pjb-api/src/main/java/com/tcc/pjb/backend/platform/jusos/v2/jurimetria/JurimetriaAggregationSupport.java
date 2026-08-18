package com.tcc.pjb.backend.platform.jusos.v2.jurimetria;

import com.tcc.pjb.backend.ai.jurimetria.model.JurimetriaReport;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsAggregationService;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
class JurimetriaAggregationSupport {

    JurimetriaEngine.BaseLocalAnalitica carregarBaseLocal(ProcessoAnalyticsAggregationService analyticsService,
                                                          RamoDireito ramo,
                                                          GrauJurisdicao grau,
                                                          String tribunal) {
        List<Object[]> porRamo = analyticsService.agregadosPorRamo(ramo.name());
        List<Object[]> porTribunal = tribunal != null && !tribunal.isBlank()
                ? analyticsService.agregadosPorRamoETribunal(ramo.name(), tribunal)
                : List.of();

        Object[] linhaRamo = porRamo.isEmpty() ? null : porRamo.get(0);
        int totalMesmoRamo = number(linhaRamo, 1);
        int julgados = number(linhaRamo, 2);
        int recursais = number(linhaRamo, 3);
        int ativos = number(linhaRamo, 4);
        double tempoMedio = decimal(linhaRamo, 5, JurimetriaSupportUtils.estimarTempoDias(ramo, grau));

        Object[] linhaTribunal = porTribunal.isEmpty() ? null : porTribunal.get(0);
        int totalMesmoRamoNoTribunal = number(linhaTribunal, 0);
        int comAcordo = number(linhaTribunal, 2);
        int comTutela = number(linhaTribunal, 3);

        int totalMesmoGrau = totalMesmoRamo;
        double taxaRecurso = JurimetriaSupportUtils.ratio(recursais, Math.max(1, totalMesmoRamoNoTribunal));
        double taxaAcordo = JurimetriaSupportUtils.ratio(comAcordo, Math.max(1, totalMesmoRamo));
        double indiceCongestionamento = JurimetriaSupportUtils.ratio(ativos, Math.max(1, julgados));
        double taxaEncerramento = JurimetriaSupportUtils.ratio(julgados, Math.max(1, totalMesmoRamo));
        double indiceUrgencia = JurimetriaSupportUtils.ratio(comTutela, Math.max(1, totalMesmoRamo));

        return new JurimetriaEngine.BaseLocalAnalitica(
                totalMesmoRamo,
                totalMesmoRamoNoTribunal,
                totalMesmoGrau,
                julgados,
                recursais,
                comAcordo,
                comTutela,
                JurimetriaSupportUtils.round(tempoMedio),
                JurimetriaSupportUtils.round(taxaRecurso),
                JurimetriaSupportUtils.round(taxaAcordo),
                JurimetriaSupportUtils.round(indiceCongestionamento),
                JurimetriaSupportUtils.round(taxaEncerramento),
                JurimetriaSupportUtils.round(indiceUrgencia)
        );
    }

    List<JurimetriaEngine.IndicadorJurimetrico> calcularIndicadores(Processo processo,
                                                                    RamoDireito ramo,
                                                                    GrauJurisdicao grau,
                                                                    String tribunal,
                                                                    JurimetriaEngine.BaseLocalAnalitica baseLocal,
                                                                    JurimetriaReport relatorioIA,
                                                                    List<Precedente> precedentes,
                                                                    NationalRulePackEngine.ResultadoRegras regras,
                                                                    NationalPrazoEngine.PrazoCalculado prazoSensivel) {
        List<JurimetriaEngine.IndicadorJurimetrico> lista = new ArrayList<>();
        lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                "total_processos_mesmo_ramo",
                baseLocal.totalMesmoRamo(),
                "processos",
                JurimetriaSupportUtils.confidenceFromVolume(baseLocal.totalMesmoRamo()),
                "ProcessoRepository",
                "Estoque local do mesmo ramo no PJB"
        ));
        lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                "total_processos_mesmo_ramo_tribunal",
                baseLocal.totalMesmoRamoNoTribunal(),
                "processos",
                JurimetriaSupportUtils.confidenceFromVolume(baseLocal.totalMesmoRamoNoTribunal()),
                "ProcessoRepository",
                "Base local segmentada por tribunal de tramitação"
        ));
        lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                "tempo_medio_local_dias",
                JurimetriaSupportUtils.round(baseLocal.tempoMedioDias()),
                "dias",
                JurimetriaSupportUtils.confidenceFromVolume(baseLocal.totalJulgados()),
                "Histórico local do PJB",
                JurimetriaSupportUtils.interpretarTempo(baseLocal.tempoMedioDias())
        ));
        lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                "taxa_recurso_local",
                JurimetriaSupportUtils.round(baseLocal.taxaRecurso()),
                "ratio",
                JurimetriaSupportUtils.confidenceFromVolume(baseLocal.totalMesmoRamoNoTribunal()),
                "Histórico local do PJB",
                baseLocal.taxaRecurso() >= 0.50 ? "Recorribilidade estruturalmente elevada" : "Recorribilidade sob controle"
        ));
        lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                "taxa_acordo_local",
                JurimetriaSupportUtils.round(baseLocal.taxaAcordo()),
                "ratio",
                JurimetriaSupportUtils.confidenceFromVolume(baseLocal.totalMesmoRamo()),
                "Histórico local do PJB",
                baseLocal.taxaAcordo() >= 0.25 ? "Ambiente favorável a composição" : "Composição tende a exigir provocação estratégica"
        ));
        lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                "indice_congestionamento_local",
                JurimetriaSupportUtils.round(baseLocal.indiceCongestionamento()),
                "ratio",
                JurimetriaSupportUtils.confidenceFromVolume(baseLocal.totalMesmoRamoNoTribunal()),
                "Histórico local do PJB",
                baseLocal.indiceCongestionamento() > 1.4 ? "Fila local pressionada" : "Fila local dentro do esperado"
        ));
        lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                "taxa_encerramento_local",
                JurimetriaSupportUtils.round(baseLocal.taxaEncerramento()),
                "ratio",
                JurimetriaSupportUtils.confidenceFromVolume(baseLocal.totalMesmoRamo()),
                "Histórico local do PJB",
                baseLocal.taxaEncerramento() >= 0.45 ? "Taxa de saída razoável" : "Baixa velocidade de encerramento"
        ));
        lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                "densidade_precedentes_relevantes",
                precedentes.size(),
                "precedentes",
                precedentes.isEmpty() ? 0.35 : 0.78,
                "JurisprudenciaService",
                precedentes.isEmpty() ? "Sem precedentes indexados no recorte pesquisado" : "Existe lastro jurisprudencial para orientar estratégia"
        ));
        lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                "alertas_criticos_regras",
                regras.temAlertasCriticos() ? 1.0 : 0.0,
                "flag",
                0.84,
                "NationalRulePackEngine",
                regras.temAlertasCriticos() ? "Existem travas jurídicas relevantes na matéria" : "Sem travas críticas detectadas no rule pack"
        ));
        if (prazoSensivel != null) {
            lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                    "janela_recursal_dias_uteis",
                    prazoSensivel.diasUteis(),
                    "dias_uteis",
                    0.82,
                    "NationalPrazoEngine",
                    "Janela temporal para reação estratégica e eventual resposta recursal"
            ));
        }
        if (processo.getScoreComplexidade() != null) {
            lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                    "complexidade_processual",
                    processo.getScoreComplexidade(),
                    "score",
                    0.72,
                    "Processo.scoreComplexidade",
                    processo.getScoreComplexidade() >= 75 ? "Processo de alta complexidade probatória ou decisional" : "Complexidade sob nível administrável"
            ));
        }
        if (processo.getValorCausa() != null) {
            lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                    "valor_causa_normalizado",
                    processo.getValorCausa().doubleValue(),
                    "BRL",
                    0.96,
                    "Processo.valorCausa",
                    "Base financeira primária para modelagem de exposição e acordo"
            ));
        }
        for (JurimetriaReport.Indicador indicador : safeIndicadores(relatorioIA)) {
            if (indicador == null || indicador.getNome() == null || indicador.getValor() == null) {
                continue;
            }
            lista.add(new JurimetriaEngine.IndicadorJurimetrico(
                    "ia_" + JurimetriaSupportUtils.normalizeToken(indicador.getNome()).toLowerCase(Locale.ROOT),
                    indicador.getValor(),
                    indicador.getUnidade() != null ? indicador.getUnidade() : "ratio",
                    0.60,
                    "JurimetriaService",
                    "Sinal oriundo de heurística de IA calibrada para apoio tático"
            ));
        }
        return dedupeIndicadores(lista);
    }

    JurimetriaEngine.PerfisDecisional construirPerfilTribunal(String tribunal,
                                                              TribunalFonte fonteTribunal,
                                                              RamoDireito ramo,
                                                              JurimetriaEngine.BaseLocalAnalitica baseLocal,
                                                              List<Precedente> precedentes) {
        double taxaParcial = precedentes.stream().anyMatch(p -> p.getTipo() != null && p.getTipo().name().contains("ACORDAO")) ? 0.12 : 0.08;
        double provimento = JurimetriaSupportUtils.clamp(0.30 + baseLocal.taxaEncerramento() * 0.22 + baseLocal.taxaAcordo() * 0.10);
        double desprovimento = JurimetriaSupportUtils.clamp(0.28 + baseLocal.taxaRecurso() * 0.25 + (ramo == RamoDireito.TRIBUTARIO ? 0.10 : 0.0));
        double ajuste = provimento + desprovimento + taxaParcial;
        if (ajuste > 1.0) {
            provimento = provimento / ajuste;
            desprovimento = desprovimento / ajuste;
            taxaParcial = taxaParcial / ajuste;
        }
        List<String> teses = precedentes.stream()
                .sorted(Comparator.comparing(Precedente::getDataPublicacao, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(JurimetriaSupportUtils::resumirPrecedente)
                .filter(s -> !s.isBlank())
                .limit(5)
                .toList();
        return new JurimetriaEngine.PerfisDecisional(
                tribunal,
                resolverOrgaoJulgador(tribunal, fonteTribunal),
                JurimetriaSupportUtils.round(provimento),
                JurimetriaSupportUtils.round(desprovimento),
                JurimetriaSupportUtils.round(taxaParcial),
                baseLocal.totalJulgados(),
                JurimetriaSupportUtils.round(baseLocal.tempoMedioDias()),
                JurimetriaSupportUtils.round(baseLocal.indiceCongestionamento()),
                JurimetriaSupportUtils.round(baseLocal.taxaAcordo()),
                teses
        );
    }

    private List<JurimetriaEngine.IndicadorJurimetrico> dedupeIndicadores(List<JurimetriaEngine.IndicadorJurimetrico> indicadores) {
        Map<String, JurimetriaEngine.IndicadorJurimetrico> map = new LinkedHashMap<>();
        for (JurimetriaEngine.IndicadorJurimetrico indicador : indicadores) {
            if (indicador == null || indicador.nome() == null || indicador.nome().isBlank()) {
                continue;
            }
            map.putIfAbsent(JurimetriaSupportUtils.normalizeToken(indicador.nome()), indicador);
        }
        return List.copyOf(map.values());
    }

    private List<JurimetriaReport.Indicador> safeIndicadores(JurimetriaReport relatorioIA) {
        return relatorioIA == null || relatorioIA.getIndicadores() == null ? List.of() : relatorioIA.getIndicadores();
    }

    private String resolverOrgaoJulgador(String tribunal, TribunalFonte fonteTribunal) {
        if (tribunal == null || tribunal.isBlank()) {
            return "Órgão julgador não identificado";
        }
        String base = switch (fonteTribunal) {
            case STF -> "Plenário/Turma do STF";
            case STJ -> "Turma/Seção do STJ";
            case TST -> "Turma/Seção Especializada do TST";
            case TSE, TRE -> "Colegiado eleitoral";
            case STM -> "Colegiado militar";
            case TRF -> "Turma/Seção regional federal";
            case TRT -> "Turma regional do trabalho";
            case TJ -> "Câmara/Turma do Tribunal de Justiça";
            default -> "Órgão julgador do tribunal";
        };
        return base + " — " + tribunal;
    }

    private int number(Object[] linha, int index) {
        if (linha == null || index < 0 || index >= linha.length || linha[index] == null) {
            return 0;
        }
        return ((Number) linha[index]).intValue();
    }

    private double decimal(Object[] linha, int index, double fallback) {
        if (linha == null || index < 0 || index >= linha.length || linha[index] == null) {
            return fallback;
        }
        return ((Number) linha[index]).doubleValue();
    }
}
