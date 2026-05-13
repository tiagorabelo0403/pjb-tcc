package com.tcc.pjb.backend.platform.jusos.v2.jurimetria;

import com.tcc.pjb.backend.ai.jurimetria.model.JurimetriaReport;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
class JurimetriaRiskAnalysisSupport {

    JurimetriaEngine.RiscoJuridico calcularRisco(Processo processo,
                                                 RamoDireito ramo,
                                                 GrauJurisdicao grau,
                                                 JurimetriaEngine.PerfisDecisional perfil,
                                                 List<Precedente> precedentes,
                                                 NationalRulePackEngine.ResultadoRegras regras,
                                                 JurimetriaReport relatorioIA,
                                                 NationalPrazoEngine.PrazoCalculado prazoSensivel,
                                                 JurimetriaEngine.BaseLocalAnalitica baseLocal) {
        double probExito = baseProbabilityByRamo(ramo);
        double probAcordo = JurimetriaSupportUtils.clamp(0.14 + baseLocal.taxaAcordo() * 0.55 + (ramo.admiteConciliacao() ? 0.08 : 0.0));
        List<String> positivos = new ArrayList<>();
        List<String> negativos = new ArrayList<>();
        List<String> recomendacoes = new ArrayList<>();

        if (precedentes.size() >= 3) {
            probExito += 0.05;
            positivos.add("Há massa crítica de precedentes localizados para sustentar narrativa jurídica e distinguir o caso.");
        } else if (precedentes.isEmpty()) {
            probExito -= 0.04;
            negativos.add("Baixa densidade de precedentes indexados reduz previsibilidade e exige reforço argumentativo.");
        }
        if (perfil.taxaProvimento() >= 0.52) {
            probExito += 0.04;
            positivos.add("Perfil decisional do recorte indica taxa de provimento acima da média heurística.");
        }
        if (perfil.taxaDesprovimento() >= 0.45) {
            probExito -= 0.05;
            negativos.add("Recorte decisional demonstra resistência relevante a teses revisionais.");
        }
        if (baseLocal.indiceCongestionamento() > 1.50) {
            probAcordo += 0.08;
            negativos.add("Congestionamento elevado pode alongar o ciclo processual e aumentar custo de oportunidade.");
            recomendacoes.add("Explorar janela de acordo antes do pico de custo operacional e recursal.");
        }
        if (baseLocal.taxaRecurso() >= 0.60) {
            probExito -= 0.03;
            probAcordo += 0.03;
            negativos.add("Litigiosidade recursal alta sinaliza desgaste temporal e necessidade de blindagem argumentativa.");
            recomendacoes.add("Preparar tese já com estrutura de contrarrazões, distinguishing e capítulos recursais reutilizáveis.");
        }
        if (regras.temAlertasCriticos()) {
            probExito -= 0.06;
            negativos.add("Rule pack encontrou alertas críticos que podem afetar admissibilidade, competência ou rito.");
            recomendacoes.add("Atacar os alertas críticos antes de avançar para atos de maior exposição.");
        }
        if (prazoSensivel != null && prazoSensivel.diasUteis() <= 5) {
            probExito -= 0.02;
            negativos.add("Janela recursal curta eleva risco operacional de perda de timing.");
            recomendacoes.add("Reservar produção imediata de minuta e checklist de protocolo para reduzir risco temporal.");
        }
        if (processo.getScoreComplexidade() != null && processo.getScoreComplexidade() >= 80) {
            probExito -= 0.05;
            negativos.add("Complexidade elevada aumenta dispersão decisional e custo de prova.");
            recomendacoes.add("Fracionar tese em blocos probatórios e marcos decisórios auditáveis.");
        }
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL || grau == GrauJurisdicao.SEGUNDO_GRAU || grau == GrauJurisdicao.SUPERIOR || grau == GrauJurisdicao.CONSTITUCIONAL) {
            probExito -= 0.06;
            negativos.add("Ambiente recursal reduz elasticidade decisional e exige filtros argumentativos mais severos.");
            recomendacoes.add("Priorizar violações de norma, precedentes qualificados e demonstração de relevância objetiva.");
        }
        if (processo.getStatusProcesso() == StatusProcesso.RECURSO_INTERPOSTO) {
            probExito -= 0.03;
            recomendacoes.add("Monitorar admissibilidade e preparar reação a juízo negativo de seguimento.");
        }

        Map<String, Double> iaMap = mapearIndicadoresIA(relatorioIA);
        probExito = JurimetriaSupportUtils.blend(probExito, iaMap.getOrDefault("taxa_sucesso_estimada", probExito), 0.35);
        double probTutela = JurimetriaSupportUtils.clamp(iaMap.getOrDefault("prob_tutela_urgencia", 0.18));
        if (probTutela >= 0.40) {
            positivos.add("Sinal de tutela de urgência acima da linha neutra pode antecipar ganho estratégico.");
            recomendacoes.add("Reforçar periculum e probabilidade do direito para explorar vantagem temporal.");
        }
        if (ramo == RamoDireito.CONSUMIDOR) {
            probExito += 0.04;
            positivos.add("Estrutura normativa do CDC costuma favorecer correção de assimetria probatória e informacional.");
            recomendacoes.add("Consolidar trilha documental de oferta, falha e tentativa extrajudicial de solução.");
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            probExito += 0.02;
            probAcordo += 0.05;
            positivos.add("Natureza alimentar e cultura conciliatória ampliam poder de alavancagem do crédito.");
            recomendacoes.add("Produzir planilha de liquidação desde já para encurtar fechamento negocial ou execução.");
        }
        if (ramo == RamoDireito.TRIBUTARIO) {
            probExito -= 0.07;
            negativos.add("Fazenda Pública e presunções de legitimidade exigem recorte técnico mais rigoroso.");
            recomendacoes.add("Validar suspensão da exigibilidade, garantias e existência de tema repetitivo ou repercussão geral.");
        }
        if (ramo == RamoDireito.PENAL) {
            probExito -= 0.03;
            probAcordo = JurimetriaSupportUtils.clamp(probAcordo - 0.05);
            positivos.add("Garantias processuais penais permitem atacar nulidades, cadeia de custódia e tipicidade.");
            recomendacoes.add("Passar pente fino em legalidade da prova, justa causa e marcos prescricionais.");
        }
        if (ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            probAcordo += 0.10;
            recomendacoes.add("Caminho consensual assistido tende a gerar solução mais estável e menos reincidente.");
        }

        probExito = JurimetriaSupportUtils.clamp(probExito);
        probAcordo = JurimetriaSupportUtils.clamp(probAcordo);
        double probDerrota = JurimetriaSupportUtils.clamp(1.0 - probExito);
        JurimetriaEngine.NivelRisco nivel = JurimetriaEngine.NivelRisco.from(probExito);

        BigDecimal valorBase = JurimetriaSupportUtils.scale(processo.getValorCausa() != null ? processo.getValorCausa() : BigDecimal.valueOf(10000));
        BigDecimal fatorRisco = BigDecimal.valueOf(1.0 + Math.max(0.0, 0.55 - probExito));
        BigDecimal minima = JurimetriaSupportUtils.scale(valorBase.multiply(BigDecimal.valueOf(0.22)));
        BigDecimal media = JurimetriaSupportUtils.scale(valorBase.multiply(BigDecimal.valueOf(Math.max(0.25, probExito))));
        BigDecimal maxima = JurimetriaSupportUtils.scale(valorBase.multiply(fatorRisco.add(BigDecimal.valueOf(0.35))));
        BigDecimal exposicao = JurimetriaSupportUtils.scale(media.add(valorBase.multiply(BigDecimal.valueOf(baseLocal.taxaRecurso() * 0.18))));

        if (nivel == JurimetriaEngine.NivelRisco.MUITO_DESFAVORAVEL || nivel == JurimetriaEngine.NivelRisco.DESFAVORAVEL) {
            recomendacoes.add("Avaliar proposta calibrada de acordo com gatilhos objetivos de fechamento e quitação.");
        }
        if (baseLocal.indiceUrgencia() >= 0.45) {
            recomendacoes.add("Mapear pedidos urgentes e provas críticas para antecipar ganho de tempo processual.");
        }
        if (!regras.requisitosIdentificados().isEmpty()) {
            recomendacoes.add("Sanear documentos obrigatórios detectados pelo rule pack antes de qualquer movimento agressivo.");
        }

        return new JurimetriaEngine.RiscoJuridico(
                JurimetriaSupportUtils.round(probExito),
                JurimetriaSupportUtils.round(probDerrota),
                JurimetriaSupportUtils.round(probAcordo),
                nivel,
                positivos,
                negativos,
                recomendacoes,
                minima,
                maxima,
                media,
                exposicao
        );
    }

    List<String> gerarAlertasEstrategicos(Processo processo,
                                          RamoDireito ramo,
                                          GrauJurisdicao grau,
                                          JurimetriaEngine.RiscoJuridico risco,
                                          JurimetriaEngine.PerfisDecisional perfil,
                                          NationalRulePackEngine.ResultadoRegras regras,
                                          NationalPrazoEngine.PrazoCalculado prazoSensivel,
                                          JurimetriaEngine.BaseLocalAnalitica baseLocal,
                                          List<Precedente> precedentes) {
        List<String> alertas = new ArrayList<>();
        if (risco.nivel() == JurimetriaEngine.NivelRisco.MUITO_DESFAVORAVEL || risco.nivel() == JurimetriaEngine.NivelRisco.DESFAVORAVEL) {
            alertas.add("Risco alto detectado: calibrar custo de prova, janela de acordo e travas de admissibilidade antes de avançar.");
        }
        if (prazoSensivel != null && prazoSensivel.diasUteis() <= 5) {
            alertas.add("Janela de reação curta: priorizar protocolo, minuta de recurso e checklist documental imediatamente.");
        }
        if (regras.temAlertasCriticos()) {
            alertas.add("Rule pack nacional identificou alertas críticos que podem comprometer rito, competência ou viabilidade da tese.");
        }
        if (perfil.indiceCongestionamento() > 1.50) {
            alertas.add("Congestionamento local acima do ideal: o custo temporal da litigância tende a subir.");
        }
        if (baseLocal.taxaAcordo() >= 0.30 && risco.probabilidadeAcordo() >= 0.35) {
            alertas.add("Ambiente de composição favorável: existe janela material para tentativa negocial tecnicamente vantajosa.");
        }
        if (precedentes.isEmpty()) {
            alertas.add("Sem precedentes indexados no recorte pesquisado: ampliar pesquisa doutrinária e jurisprudencial específica.");
        }
        if (ramo == RamoDireito.TRIBUTARIO && processo.getValorCausa() != null && processo.getValorCausa().compareTo(BigDecimal.valueOf(50000)) > 0) {
            alertas.add("Exposição tributária relevante: validar suspensão da exigibilidade e estratégia de garantia do juízo.");
        }
        if (ramo == RamoDireito.PENAL) {
            alertas.add("Mapear nulidades, cadeia de custódia e prescrição antes de cristalizar linha defensiva.");
        }
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            alertas.add("Ambiente constitucional exige filtro argumentativo de repercussão sistêmica e aderência qualificada a precedentes.");
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().name().contains("SIGILO")) {
            alertas.add("Processo sigiloso: monitoramento estratégico deve respeitar trilha mínima de acesso e segmentação de evidências.");
        }
        alertas.add("Relatório combina heurística de IA, rule pack, prazo nacional, precedentes indexados e histórico local do PJB.");
        return List.copyOf(new LinkedHashSet<>(alertas));
    }

    List<JurimetriaEngine.CenarioEstrategico> construirCenarios(Processo processo,
                                                                JurimetriaEngine.RiscoJuridico risco,
                                                                JurimetriaEngine.BaseLocalAnalitica baseLocal,
                                                                JurimetriaEngine.PerfisDecisional perfil,
                                                                NationalPrazoEngine.PrazoCalculado prazoSensivel) {
        BigDecimal valorBase = JurimetriaSupportUtils.scale(processo.getValorCausa() != null ? processo.getValorCausa() : BigDecimal.valueOf(10000));
        return List.of(
                new JurimetriaEngine.CenarioEstrategico(
                        "LITIGANCIA_ORIENTADA_A_PRECEDENTE",
                        JurimetriaSupportUtils.round(risco.probabilidadeExito()),
                        risco.probabilidadeExito() >= 0.55 ? "Prosseguir com reforço probatório e tese escalável" : "Prosseguir apenas com blindagem tática e controle de exposição",
                        JurimetriaSupportUtils.scale(valorBase.multiply(BigDecimal.valueOf(1.10 + baseLocal.taxaRecurso() * 0.20))),
                        List.of(
                                "Montar dossiê de precedentes aderentes e distinguishing desde a primeira peça relevante",
                                "Priorizar fatos incontroversos e documentos com alta força de convencimento",
                                "Preparar desde cedo módulo recursal reutilizável"
                        ),
                        JurimetriaSupportUtils.buildDeadlineTriggers(prazoSensivel, "Prazo sensível de resposta recursal ou impugnação")
                ),
                new JurimetriaEngine.CenarioEstrategico(
                        "ACORDO_ESTRATEGICO",
                        JurimetriaSupportUtils.round(risco.probabilidadeAcordo()),
                        risco.probabilidadeAcordo() >= 0.35 ? "Acordo tem racionalidade econômica e temporal relevante" : "Acordo só é recomendável se destravar risco processual específico",
                        JurimetriaSupportUtils.scale(valorBase.multiply(BigDecimal.valueOf(Math.max(0.25, 0.60 - risco.probabilidadeExito() * 0.20)))),
                        List.of(
                                "Estruturar âncoras de valor, quitação, confidencialidade e gatilhos de inadimplemento",
                                "Usar congestionamento e custo recursal como vetor de fechamento",
                                "Segregar pontos incontroversos para acordo parcial quando necessário"
                        ),
                        List.of(
                                "Mudança abrupta de perfil decisional no órgão julgador",
                                "Negativa de tutela ou produção de prova crítica",
                                "Elevação de custo executivo ou recursal"
                        )
                ),
                new JurimetriaEngine.CenarioEstrategico(
                        "PRESSAO_RECURSAL_CONTROLADA",
                        JurimetriaSupportUtils.round(JurimetriaSupportUtils.clamp(perfil.taxaDesprovimento() * 0.55 + baseLocal.taxaRecurso() * 0.35)),
                        "Operar com peça enxuta, aderência a precedentes qualificados e foco em admissibilidade",
                        JurimetriaSupportUtils.scale(valorBase.multiply(BigDecimal.valueOf(0.18 + perfil.taxaDesprovimento() * 0.12))),
                        List.of(
                                "Reduzir dispersão argumentativa e concentrar em vícios nucleares",
                                "Amarrar capítulos à jurisprudência qualificada do tribunal-fonte",
                                "Preparar contramedidas para juízo negativo de admissibilidade"
                        ),
                        List.of(
                                "Publicação de decisão com capítulo omisso ou contraditório",
                                "Mudança de órgão julgador ou distribuição interna",
                                "Abertura de prazo para contrarrazões"
                        )
                )
        );
    }

    private Map<String, Double> mapearIndicadoresIA(JurimetriaReport relatorioIA) {
        Map<String, Double> map = new LinkedHashMap<>();
        if (relatorioIA == null || relatorioIA.getIndicadores() == null) {
            return map;
        }
        for (JurimetriaReport.Indicador indicador : relatorioIA.getIndicadores()) {
            if (indicador != null && indicador.getNome() != null && indicador.getValor() != null) {
                map.put(JurimetriaSupportUtils.normalizeToken(indicador.getNome()).toLowerCase(Locale.ROOT), indicador.getValor());
            }
        }
        return map;
    }

    private double baseProbabilityByRamo(RamoDireito ramo) {
        if (ramo == null) {
            return 0.50d;
        }
        return switch (ramo) {
            case CONSUMIDOR -> 0.61;
            case TRABALHISTA, PROCESSUAL_TRABALHISTA, ACIDENTARIO -> 0.58;
            case PREVIDENCIARIO, INFANCIA_JUVENTUDE -> 0.56;
            case FAMILIA, SUCESSOES -> 0.53;
            case CIVIL, PROCESSUAL_CIVIL, CONTRATUAL, RESPONSABILIDADE_CIVIL, IMOBILIARIO,
                    BANCARIO, REGISTRAL_NOTARIAL, ARBITRAGEM_MEDIACAO, DIGITAL_PROTECAO_DADOS,
                    SAUDE_SUPLEMENTAR -> 0.50;
            case EMPRESARIAL, FALIMENTAR_RECUPERACIONAL, AGRARIO, MINERARIO, ENERGETICO -> 0.46;
            case ADMINISTRATIVO, LICITACOES_CONTRATOS, IMPROBIDADE_ADMINISTRATIVA,
                    SERVIDOR_PUBLICO, REGULATORIO, ADUANEIRO, INTERNACIONAL -> 0.43;
            case TRIBUTARIO, EXECUCAO_FISCAL -> 0.38;
            case AMBIENTAL, URBANISTICO, CIVIL_PUBLICA_COLETIVO -> 0.41;
            case PENAL, PROCESSUAL_PENAL, EXECUCAO_PENAL -> 0.36;
            case ELEITORAL, PROCESSUAL_ELEITORAL -> 0.42;
            case MILITAR -> 0.34;
            case CONSTITUCIONAL -> 0.35;
            default -> 0.50;
        };
    }
}
