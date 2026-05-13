package com.tcc.pjb.backend.platform.jusos.v2.execucao;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
class ExecucaoCumprimentoPlanningSupport {

    private final NationalPrazoEngine prazoEngine;
    private final NationalRulePackEngine rulePackEngine;

    ExecucaoCumprimentoPlanningSupport(NationalPrazoEngine prazoEngine,
                                       NationalRulePackEngine rulePackEngine) {
        this.prazoEngine = prazoEngine;
        this.rulePackEngine = rulePackEngine;
    }

    ExecucaoCumprimentoEngine.PlanoExecucao montarPlano(Processo processo,
                                                        ExecucaoCumprimentoEngine.TituloExecutivo titulo,
                                                        BigDecimal valorBase) {
        RamoDireito ramo = ramo(processo);
        ExecucaoCumprimentoEngine.ComplianceExecucao compliance = analisarCompliance(processo, titulo, valorBase);
        ExecucaoCumprimentoEngine.MatrizExpropriacao matriz = construirMatriz(processo, titulo, valorBase, compliance);
        List<ExecucaoCumprimentoEngine.MeioExpropriatorio> meios = ordenarMeios(processo, titulo, compliance, matriz);
        List<String> etapas = construirEtapas(titulo, compliance, meios);
        List<String> alertas = new ArrayList<>();
        alertas.addAll(compliance.alertas());
        alertas.addAll(compliance.travas());
        alertas.addAll(extrairAlertasRegras(processo, titulo, valorBase));
        alertas.addAll(alertasPatrimoniais(processo, titulo, meios));

        ExecucaoPrazoResposta prazoResposta = resolverPrazoResposta(processo, titulo);
        NationalPrazoEngine.PrazoCalculado prazoCalculado = prazoResposta.integradoAoMotor()
                ? prazoEngine.calcularPorRamo(LocalDate.now(), prazoResposta.tipoPrazo(), ramo, grau(processo), tribunalCodigo(processo))
                : null;

        BigDecimal base = baseExecucao(processo, valorBase);
        BigDecimal correcao = calcularCorrecao(base, ramo, titulo);
        BigDecimal juros = calcularJuros(base, ramo, titulo);
        BigDecimal honorarios = calcularHonorarios(base, ramo, titulo, compliance.envolveFazendaPublica());
        BigDecimal valorCorrigido = scale(base.add(correcao));
        String fundamento = resolverFundamento(processo, titulo, compliance);
        List<String> checklist = construirChecklistExecucao(processo, titulo, compliance, meios, prazoResposta, prazoCalculado);
        List<String> recomendacoesTecnologicas = construirRecomendacoesTecnologicas(processo, titulo, meios, compliance);

        return new ExecucaoCumprimentoEngine.PlanoExecucao(
                processo.getId(),
                titulo,
                base,
                valorCorrigido,
                juros,
                honorarios,
                meios,
                etapas,
                alertas,
                fundamento,
                compliance.exigeIntimacaoPrevia(),
                prazoResposta.numero(),
                prazoResposta.emHoras(),
                faseDestino(titulo),
                statusDestino(titulo),
                prazoCalculado,
                compliance,
                matriz,
                checklist,
                recomendacoesTecnologicas
        );
    }

    ExecucaoCumprimentoEngine.ComplianceExecucao analisarCompliance(Processo processo,
                                                                    ExecucaoCumprimentoEngine.TituloExecutivo titulo,
                                                                    BigDecimal valorBase) {
        RamoDireito ramo = ramo(processo);
        List<String> travas = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        List<String> salvaguardas = new ArrayList<>();

        boolean envolveFazendaPublica = ramo == RamoDireito.PREVIDENCIARIO || ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.ADMINISTRATIVO;
        boolean naturezaAlimentar = ramo == RamoDireito.TRABALHISTA || ramo == RamoDireito.FAMILIA || ramo == RamoDireito.PREVIDENCIARIO;
        boolean exigePriorizacaoHumana = ramo == RamoDireito.PENAL || ramo == RamoDireito.AMBIENTAL || ramo == RamoDireito.INFANCIA_JUVENTUDE || ramo == RamoDireito.CONSTITUCIONAL;
        boolean recomendaPesquisaAmpliada = value(valorBase).compareTo(BigDecimal.valueOf(30000L)) >= 0 || ramo == RamoDireito.EMPRESARIAL || ramo == RamoDireito.TRIBUTARIO;
        boolean exigeIntimacao = switch (ramo) {
            case AMBIENTAL -> false;
            default -> true;
        };

        if (ramo == RamoDireito.PENAL) {
            travas.add("Execução patrimonial penal depende de título e via executiva compatíveis; revisar natureza do capítulo condenatório");
        }
        if (ramo == RamoDireito.CONSTITUCIONAL) {
            alertas.add("Controle concentrado e medidas constitucionais exigem cautela redobrada para liquidez e executividade do título");
        }
        if (ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            salvaguardas.add("Priorizar proteção integral e evitar atos executivos que afetem diretamente o melhor interesse do menor");
        }
        if (ramo == RamoDireito.FAMILIA) {
            alertas.add("Em alimentos, desconto em folha, prisão civil e SISBAJUD exigem calibragem simultânea e urgência material");
            salvaguardas.add("Verificar parcelas recentes, rito coercitivo e rito expropriatório sem cumulação confusa");
        }
        if (ramo == RamoDireito.AMBIENTAL) {
            alertas.add("Priorizar obrigação de fazer, recuperação in natura e fundo de reparação antes da simples monetização do dano");
        }
        if (envolveFazendaPublica) {
            travas.add("Fluxo contra ente público deve respeitar RPV, Precatório, transação legal ou regime especial aplicável");
            salvaguardas.add("Controlar ordem cronológica, requisição de pagamento e compensações admitidas em lei");
        }
        if (titulo == ExecucaoCumprimentoEngine.TituloExecutivo.CDA_CERTIDAO_DIVIDA_ATIVA) {
            alertas.add("CDA exige higidez formal, certeza, liquidez e exigibilidade antes da ofensiva expropriatória mais intensa");
        }
        if (titulo == ExecucaoCumprimentoEngine.TituloExecutivo.TITULO_JUDICIAL_PROVISORIO) {
            alertas.add("Cumprimento provisório recomenda mitigação de irreversibilidade e caução quando exigível");
            salvaguardas.add("Evitar atos de alienação irreversível sem análise do risco recursal");
        }
        if (titulo == ExecucaoCumprimentoEngine.TituloExecutivo.TERMO_CEJUSC || titulo == ExecucaoCumprimentoEngine.TituloExecutivo.ACORDO_EXTRAJUDICIAL_HOMOLOGADO) {
            salvaguardas.add("Controlar vencimento de parcelas, cláusula penal e exigibilidade de obrigações de fazer com prova do descumprimento");
        }
        if (value(valorBase).compareTo(BigDecimal.ZERO) <= 0) {
            alertas.add("Valor-base não informado com precisão; revisar memória do débito antes do protocolo executivo");
        }

        return new ExecucaoCumprimentoEngine.ComplianceExecucao(
                true,
                exigeIntimacao,
                envolveFazendaPublica,
                naturezaAlimentar,
                exigePriorizacaoHumana,
                recomendaPesquisaAmpliada,
                travas,
                alertas,
                salvaguardas
        );
    }

    ExecucaoCumprimentoEngine.MatrizExpropriacao construirMatriz(Processo processo,
                                                                 ExecucaoCumprimentoEngine.TituloExecutivo titulo,
                                                                 BigDecimal valorBase,
                                                                 ExecucaoCumprimentoEngine.ComplianceExecucao compliance) {
        RamoDireito ramo = ramo(processo);
        List<String> positivos = new ArrayList<>();
        List<String> riscos = new ArrayList<>();
        List<ExecucaoCumprimentoEngine.MeioExpropriatorio> prioritarios = new ArrayList<>();
        List<ExecucaoCumprimentoEngine.MeioExpropriatorio> restringidos = new ArrayList<>();

        int scoreRecuperabilidade = 52;
        int scorePressao = 40;
        int scoreComplexidade = 35;

        if (compliance.naturezaAlimentar()) {
            scoreRecuperabilidade += 8;
            scorePressao += 14;
            positivos.add("Crédito alimentar favorece prioridade material e medidas coercitivas intensificadas");
        }
        if (compliance.envolveFazendaPublica()) {
            scoreRecuperabilidade -= 16;
            scoreComplexidade += 25;
            riscos.add("Pagamento depende de trilha pública controlada por requisição e orçamento");
            restringidos.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA);
            restringidos.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.BACENJUD_BLOQUEIO_CONTA);
        }
        if (titulo == ExecucaoCumprimentoEngine.TituloExecutivo.TITULO_JUDICIAL_PROVISORIO) {
            scoreRecuperabilidade -= 7;
            scoreComplexidade += 12;
            riscos.add("Provisoriedade do título pode limitar expropriação irreversível");
            restringidos.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.ALIENACAO_JUDICIAL);
            restringidos.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.ARREMATACAO_HASTA_PUBLICA);
        }
        if (titulo == ExecucaoCumprimentoEngine.TituloExecutivo.CDA_CERTIDAO_DIVIDA_ATIVA) {
            scorePressao += 10;
            positivos.add("Rito da execução fiscal favorece pressão executiva estruturada");
        }
        if (value(valorBase).compareTo(BigDecimal.valueOf(200000L)) >= 0) {
            scoreComplexidade += 14;
            prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SNIPER_PATRIMONIAL);
            prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.CCS_PESQUISA_RELACIONAMENTO);
        }

        switch (ramo) {
            case CIVIL, CONSUMIDOR, EMPRESARIAL -> {
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA);
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.RENAJUD_RESTRICAO_VEICULO);
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.INFOJUD_CONSULTA_BENS);
                positivos.add("Crédito patrimonial típico admite escalonamento executivo clássico com alta previsibilidade");
            }
            case TRABALHISTA -> {
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_TEIMOSINHA);
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_FATURAMENTO);
                scorePressao += 12;
                positivos.add("Crédito trabalhista com natureza alimentar reforça urgência e preferência material");
            }
            case FAMILIA -> {
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.DESCONTO_FOLHA_ALIMENTOS);
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA);
                scorePressao += 18;
                positivos.add("Fluxo alimentar permite medidas coercitivas personalizadas e recorrentes");
            }
            case TRIBUTARIO -> {
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PROTESTO_DECISAO_JUDICIAL);
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_IMOVEL_REGISTRO);
                riscos.add("Executado costuma litigar sobre garantia, substituição e prescrição intercorrente");
            }
            case PREVIDENCIARIO -> {
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_CREDITO_PRECATORIO);
                riscos.add("Fluxo depende do regime constitucional de pagamentos públicos");
            }
            case AMBIENTAL -> {
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.EXPROPRIACAO_DIRETA);
                scoreComplexidade += 18;
                riscos.add("Preferência por recomposição específica pode reduzir utilidade da simples execução pecuniária");
            }
            case INTERNACIONAL -> {
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.INFOJUD_CONSULTA_BENS);
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SNIPER_PATRIMONIAL);
                scoreComplexidade += 16;
                riscos.add("Execução com elemento transnacional pode exigir cooperação jurídica, rastreamento multijurisdicional e validação de executabilidade");
            }
            case PENAL, MILITAR, ELEITORAL, CONSTITUCIONAL, ADMINISTRATIVO, INFANCIA_JUVENTUDE, AGRARIO -> {
                prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.INFOJUD_CONSULTA_BENS);
                scoreComplexidade += 10;
                riscos.add("O ramo exige filtragem humana para compatibilizar rito e executividade material");
            }
            default -> {
                if (ramo.admiteConciliacao()) {
                    prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA);
                } else {
                    prioritarios.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.INFOJUD_CONSULTA_BENS);
                }
                scoreComplexidade += 8;
                riscos.add("Sub-ramo derivado sem matriz executiva específica exige calibração assistida antes da escalada patrimonial");
            }
        }

        BigDecimal estimativa = estimarRecuperacaoBase(value(valorBase), compliance, scoreRecuperabilidade);
        return new ExecucaoCumprimentoEngine.MatrizExpropriacao(
                clamp(scoreRecuperabilidade),
                clamp(scorePressao),
                clamp(scoreComplexidade),
                estimativa,
                prioritarios,
                restringidos,
                positivos,
                riscos
        );
    }

    List<ExecucaoCumprimentoEngine.MeioExpropriatorio> ordenarMeios(Processo processo,
                                                                    ExecucaoCumprimentoEngine.TituloExecutivo titulo,
                                                                    ExecucaoCumprimentoEngine.ComplianceExecucao compliance,
                                                                    ExecucaoCumprimentoEngine.MatrizExpropriacao matriz) {
        LinkedHashSet<ExecucaoCumprimentoEngine.MeioExpropriatorio> base = new LinkedHashSet<>(matriz.meiosPrioritarios());
        RamoDireito ramo = ramo(processo);

        switch (ramo) {
            case CIVIL, CONSUMIDOR -> {
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.RENAJUD_RESTRICAO_VEICULO);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.INFOJUD_CONSULTA_BENS);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SNIPER_PATRIMONIAL);
            }
            case EMPRESARIAL -> {
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_TEIMOSINHA);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_FATURAMENTO);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_QUOTA_SOCIAL);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.CCS_PESQUISA_RELACIONAMENTO);
            }
            case TRABALHISTA -> {
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_TEIMOSINHA);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_FATURAMENTO);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.RENAJUD_RESTRICAO_VEICULO);
            }
            case FAMILIA -> {
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.DESCONTO_FOLHA_ALIMENTOS);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PROTESTO_DECISAO_JUDICIAL);
            }
            case TRIBUTARIO -> {
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PROTESTO_DECISAO_JUDICIAL);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_IMOVEL_REGISTRO);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.INFOJUD_CONSULTA_BENS);
            }
            case PREVIDENCIARIO, ADMINISTRATIVO -> {
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_CREDITO_PRECATORIO);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.PROTESTO_DECISAO_JUDICIAL);
            }
            case AMBIENTAL -> {
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.EXPROPRIACAO_DIRETA);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.CNIB_INDISPONIBILIDADE_IMOVEL);
            }
            case PENAL, MILITAR, ELEITORAL, CONSTITUCIONAL, INFANCIA_JUVENTUDE, AGRARIO, INTERNACIONAL -> {
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.INFOJUD_CONSULTA_BENS);
                base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SNIPER_PATRIMONIAL);
            }
            default -> {
                if (ramo.admiteConciliacao()) {
                    base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SISBAJUD_BLOQUEIO_CONTA);
                    base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.INFOJUD_CONSULTA_BENS);
                } else {
                    base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.INFOJUD_CONSULTA_BENS);
                    base.add(ExecucaoCumprimentoEngine.MeioExpropriatorio.SNIPER_PATRIMONIAL);
                }
            }
        }

        if (titulo == ExecucaoCumprimentoEngine.TituloExecutivo.TITULO_JUDICIAL_PROVISORIO) {
            base.remove(ExecucaoCumprimentoEngine.MeioExpropriatorio.ALIENACAO_JUDICIAL);
            base.remove(ExecucaoCumprimentoEngine.MeioExpropriatorio.ARREMATACAO_HASTA_PUBLICA);
        }
        base.removeAll(matriz.meiosRestringidos());

        return base.stream()
                .sorted((left, right) -> Integer.compare(prioridade(right, compliance, matriz), prioridade(left, compliance, matriz)))
                .toList();
    }

    private List<String> construirEtapas(ExecucaoCumprimentoEngine.TituloExecutivo titulo,
                                         ExecucaoCumprimentoEngine.ComplianceExecucao compliance,
                                         List<ExecucaoCumprimentoEngine.MeioExpropriatorio> meios) {
        List<String> etapas = new ArrayList<>();
        if (compliance.exigeIntimacaoPrevia()) {
            etapas.add("Consolidar memória do débito e intimar/citar executado no rito compatível com o título");
        }
        etapas.add("Qualificar exigibilidade, liquidez e extensão do título executivo antes dos atos de constrição");
        etapas.add("Selecionar ofensiva patrimonial inicial: " + meios.stream().limit(3).map(Enum::name).toList());
        if (compliance.recomendaPesquisaPatrimonialAmpliada()) {
            etapas.add("Abrir trilha de inteligência patrimonial com SISBAJUD, SNIPER, INFOJUD e CCS conforme proporcionalidade");
        }
        if (compliance.naturezaAlimentar()) {
            etapas.add("Aplicar rota prioritária de satisfação de crédito alimentar com reforço coercitivo calibrado");
        }
        if (compliance.envolveFazendaPublica()) {
            etapas.add("Encaminhar para regime de requisição pública, compensação legal ou fluxo de precatório/RPV");
        } else {
            etapas.add("Converter bens constritos em satisfação via adjudicação, alienação ou levantamento judicial");
        }
        if (titulo == ExecucaoCumprimentoEngine.TituloExecutivo.TITULO_JUDICIAL_PROVISORIO) {
            etapas.add("Mitigar irreversibilidade com cautelas específicas em atos expropriatórios finais");
        }
        etapas.add("Registrar rastreabilidade integral no ledger, UI history e evento interno de execução");
        return immutableDistinct(etapas);
    }

    private List<String> extrairAlertasRegras(Processo processo,
                                              ExecucaoCumprimentoEngine.TituloExecutivo titulo,
                                              BigDecimal valorBase) {
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("fase", faseDestino(titulo).name());
        extras.put("tituloExecutivo", titulo.name());
        extras.put("valorCausa", value(processo.getValorCausa()));
        extras.put("valorExecucao", value(valorBase));
        extras.put("envolveFazendaPublica", processo.getRamoDireito() == RamoDireito.PREVIDENCIARIO
                || processo.getRamoDireito() == RamoDireito.TRIBUTARIO
                || processo.getRamoDireito() == RamoDireito.ADMINISTRATIVO);
        NationalRulePackEngine.ResultadoRegras regras = rulePackEngine.aplicar(new NationalRulePackEngine.ContextoRegra(
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getRamoDireito(),
                grau(processo),
                tribunalCodigo(processo),
                extras
        ));
        return regras.alertas();
    }

    private List<String> alertasPatrimoniais(Processo processo,
                                             ExecucaoCumprimentoEngine.TituloExecutivo titulo,
                                             List<ExecucaoCumprimentoEngine.MeioExpropriatorio> meios) {
        List<String> alertas = new ArrayList<>();
        RamoDireito ramo = ramo(processo);
        if (meios.contains(ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_IMOVEL_REGISTRO)
                || meios.contains(ExecucaoCumprimentoEngine.MeioExpropriatorio.CNIB_INDISPONIBILIDADE_IMOVEL)) {
            alertas.add("Revisar bem de família, copropriedade e ônus registrais antes da expropriação imobiliária");
        }
        if (meios.contains(ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_FATURAMENTO)) {
            alertas.add("Penhora de faturamento exige calibragem para não inviabilizar a empresa executada");
        }
        if (ramo == RamoDireito.TRIBUTARIO && titulo == ExecucaoCumprimentoEngine.TituloExecutivo.CDA_CERTIDAO_DIVIDA_ATIVA) {
            alertas.add("Acompanhar prescrição intercorrente e eventual redirecionamento pessoal com lastro fático suficiente");
        }
        if (ramo == RamoDireito.FAMILIA) {
            alertas.add("Separar rito coercitivo de alimentos e rito expropriatório para evitar nulidades operacionais");
        }
        if (ramo == RamoDireito.PREVIDENCIARIO) {
            alertas.add("Controlar teto de RPV, cronologia do precatório e eventual incidência de juros de mora pós-expedição");
        }
        return immutableDistinct(alertas);
    }

    ExecucaoPrazoResposta resolverPrazoResposta(Processo processo,
                                                ExecucaoCumprimentoEngine.TituloExecutivo titulo) {
        RamoDireito ramo = ramo(processo);
        NationalPrazoEngine.TipoPrazo tipoPrazo = tituloJudicial(titulo)
                ? NationalPrazoEngine.TipoPrazo.IMPUGNACAO_CUMPRIMENTO
                : NationalPrazoEngine.TipoPrazo.EMBARGOS_EXECUCAO;
        if (ramo == RamoDireito.FAMILIA) {
            return new ExecucaoPrazoResposta(3, false, true, NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO);
        }
        if (ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.EXECUCAO_FISCAL) {
            return new ExecucaoPrazoResposta(5, false, true, NationalPrazoEngine.TipoPrazo.EMBARGOS_EXECUCAO);
        }
        if (ramo == RamoDireito.PREVIDENCIARIO) {
            return new ExecucaoPrazoResposta(30, false, true, NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO);
        }
        if (ramo == RamoDireito.CONSTITUCIONAL
                || ramo == RamoDireito.INFANCIA_JUVENTUDE
                || ramo == RamoDireito.ELEITORAL
                || ramo == RamoDireito.PROCESSUAL_ELEITORAL
                || ramo.isPenalLike()) {
            return new ExecucaoPrazoResposta(10, false, true, NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO);
        }
        return switch (ramo.verticalPrincipal()) {
            case "TRABALHISTA" -> new ExecucaoPrazoResposta(48, true, false, NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO);
            case "FAZENDA", "DIFUSO", "CIVEL" -> new ExecucaoPrazoResposta(15, false, true, tipoPrazo);
            case "PENAL", "ELEITORAL" -> new ExecucaoPrazoResposta(10, false, true, NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO);
            default -> new ExecucaoPrazoResposta(15, false, true, tipoPrazo);
        };
    }

    private String resolverFundamento(Processo processo,
                                      ExecucaoCumprimentoEngine.TituloExecutivo titulo,
                                      ExecucaoCumprimentoEngine.ComplianceExecucao compliance) {
        RamoDireito ramo = ramo(processo);
        if (ramo == RamoDireito.PREVIDENCIARIO) {
            return compliance.envolveFazendaPublica()
                    ? "CF art. 100, Lei 10.259/2001, Lei 9.099/1995 e CPC subsidiário"
                    : "CPC e regime executivo comum aplicável";
        }
        if (ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.EXECUCAO_FISCAL) {
            return "Lei 6.830/1980, CTN arts. 183-193 e CPC subsidiário";
        }
        if (ramo == RamoDireito.FAMILIA) {
            return "CPC arts. 528-533, 529, 536-538 e 831-913, conforme a obrigação executada";
        }
        if (ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            return "ECA, CPC e regime protetivo com prioridade absoluta";
        }
        if (ramo == RamoDireito.INTERNACIONAL) {
            return "CPC, tratados internacionais, cooperação jurídica internacional e carta rogatória ou auxílio direto quando cabível";
        }
        return switch (ramo.verticalPrincipal()) {
            case "TRABALHISTA" -> "CLT arts. 876-892, 880 e aplicação subsidiária do CPC quando compatível";
            case "PENAL" -> ramo == RamoDireito.MILITAR
                    ? "CPPM, legislação especial e CPC subsidiário quando admitido"
                    : "CPP, capítulo condenatório civil, legislação especial e execução patrimonial apenas no que for compatível";
            case "ELEITORAL" -> "Código Eleitoral, resoluções do TSE e CPC subsidiário quando compatível";
            case "FAZENDA" -> ramo == RamoDireito.CONSTITUCIONAL
                    ? "CF/88, legislação constitucional específica e CPC subsidiário para cumprimento patrimonial cabível"
                    : "CF/88, legislação administrativa especial, execução contra Fazenda Pública e CPC subsidiário";
            case "DIFUSO" -> "Lei 7.347/1985, CPC arts. 536-538, tutela específica e execução de obrigações estruturais";
            case "CIVEL" -> tituloJudicial(titulo)
                    ? "CPC arts. 513-538, 771-925 e 835"
                    : "CPC arts. 771-925, 824-925 e 835";
            default -> tituloJudicial(titulo)
                    ? "CPC arts. 513-538, 771-925 e 835"
                    : "CPC arts. 771-925, 824-925 e 835";
        };
    }

    private List<String> construirChecklistExecucao(Processo processo,
                                                   ExecucaoCumprimentoEngine.TituloExecutivo titulo,
                                                   ExecucaoCumprimentoEngine.ComplianceExecucao compliance,
                                                   List<ExecucaoCumprimentoEngine.MeioExpropriatorio> meios,
                                                   ExecucaoPrazoResposta prazoResposta,
                                                   NationalPrazoEngine.PrazoCalculado prazoCalculado) {
        List<String> checklist = new ArrayList<>();
        checklist.add("Conferir liquidez, exigibilidade e memória do débito com atualização monetária rastreável");
        checklist.add("Validar legitimidade ativa/passiva e eventual sucessão ou redirecionamento executório");
        checklist.add("Checar impenhorabilidades legais, excesso de execução e proporcionalidade do meio eleito");
        checklist.add("Preparar pacote probatório mínimo para os três primeiros meios executivos selecionados");
        checklist.add("Consolidar pedido de honorários, multa, astreintes ou consectários compatíveis com o rito");
        if (prazoResposta.integradoAoMotor() && prazoCalculado != null) {
            checklist.add("Controlar vencimento processual calculado pelo motor nacional em " + prazoCalculado.vencimento());
        } else if (prazoResposta.emHoras()) {
            checklist.add("Controlar prazo operacional em horas: " + prazoResposta.numero() + "h");
        }
        if (compliance.envolveFazendaPublica()) {
            checklist.add("Separar trilha de RPV/Precatório, ordem cronológica e compensação legalmente admitida");
        }
        if (compliance.naturezaAlimentar()) {
            checklist.add("Aplicar camada de urgência material e preferência de satisfação do crédito alimentar");
        }
        if (titulo == ExecucaoCumprimentoEngine.TituloExecutivo.TITULO_JUDICIAL_PROVISORIO) {
            checklist.add("Revisar risco de irreversibilidade e eventual necessidade de caução antes da expropriação final");
        }
        if (processo.getRamoDireito() == RamoDireito.AMBIENTAL) {
            checklist.add("Priorizar tutela específica de recomposição e métricas de efetividade ambiental antes da mera monetização");
        }
        if (meios.contains(ExecucaoCumprimentoEngine.MeioExpropriatorio.SNIPER_PATRIMONIAL)
                || meios.contains(ExecucaoCumprimentoEngine.MeioExpropriatorio.CCS_PESQUISA_RELACIONAMENTO)) {
            checklist.add("Fundamentar proporcionalidade, necessidade e finalidade da inteligência patrimonial ampliada");
        }
        return immutableDistinct(checklist);
    }

    private List<String> construirRecomendacoesTecnologicas(Processo processo,
                                                            ExecucaoCumprimentoEngine.TituloExecutivo titulo,
                                                            List<ExecucaoCumprimentoEngine.MeioExpropriatorio> meios,
                                                            ExecucaoCumprimentoEngine.ComplianceExecucao compliance) {
        List<String> ideias = new ArrayList<>();
        ideias.add("Orquestração em ondas de constrição com tolerância a falhas e ledger por etapa");
        ideias.add("Score de recuperabilidade com reordenação dinâmica de meios conforme insucesso anterior");
        ideias.add("Detecção de conflito entre rito alimentar, Fazenda Pública e títulos provisórios antes do protocolo");
        ideias.add("Painel operacional com gargalos, SLA de prazo e calor patrimonial por processo");
        ideias.add("Trilha de prova executiva mínima por meio expropriatório para reduzir indeferimento judicial");
        ideias.add("Eventos internos de execução para integração futura com notificações, work items e bots de secretaria");
        if (compliance.recomendaPesquisaPatrimonialAmpliada()) {
            ideias.add("Encadear SISBAJUD, SNIPER, INFOJUD e CCS com política de escalonamento orientada a custo-benefício");
        }
        if (titulo == ExecucaoCumprimentoEngine.TituloExecutivo.TERMO_CEJUSC || titulo == ExecucaoCumprimentoEngine.TituloExecutivo.ACORDO_EXTRAJUDICIAL_HOMOLOGADO) {
            ideias.add("Monitor de vencimento parcelado para converter inadimplemento em gatilho automático de execução qualificada");
        }
        if (processo.getRamoDireito() == RamoDireito.EMPRESARIAL) {
            ideias.add("Rota societária com quota social, faturamento e vínculos relacionais para fraude executiva complexa");
        }
        if (meios.contains(ExecucaoCumprimentoEngine.MeioExpropriatorio.DESCONTO_FOLHA_ALIMENTOS)) {
            ideias.add("Automação de ofício para fonte pagadora com conferência periódica de desconto alimentar");
        }
        return immutableDistinct(ideias);
    }

    String construirMensagemMudancaExecucao(Processo processo,
                                            ExecucaoCumprimentoEngine.PlanoExecucao plano,
                                            String operador) {
        return "Execução ativada para o processo " + processo.getNumeroUnificado()
                + " com título " + plano.tipoTitulo().name()
                + ", fase=" + plano.faseDestino().name()
                + ", status=" + plano.statusDestino().name()
                + ", operador=" + operador;
    }

    FaseProcessual faseDestino(ExecucaoCumprimentoEngine.TituloExecutivo titulo) {
        return tituloJudicial(titulo) ? FaseProcessual.CUMPRIMENTO_SENTENCA : FaseProcessual.EXECUCAO;
    }

    StatusProcesso statusDestino(ExecucaoCumprimentoEngine.TituloExecutivo titulo) {
        return tituloJudicial(titulo) ? StatusProcesso.CUMPRIMENTO_SENTENCA : StatusProcesso.EM_ANDAMENTO;
    }

    private boolean tituloJudicial(ExecucaoCumprimentoEngine.TituloExecutivo titulo) {
        return switch (titulo) {
            case SENTENCA_CONDENATORIA,
                 ACORDAO,
                 SENTENCA_HOMOLOGATORIA_ACORDO,
                 SENTENCA_ARBITRAL,
                 ACORDO_EXTRAJUDICIAL_HOMOLOGADO,
                 DECISAO_LIMINAR_ASTREINTES,
                 TERMO_CEJUSC,
                 TITULO_JUDICIAL_PROVISORIO -> true;
            default -> false;
        };
    }

    private BigDecimal baseExecucao(Processo processo, BigDecimal valorBase) {
        if (valorBase != null && valorBase.compareTo(BigDecimal.ZERO) > 0) {
            return scale(valorBase);
        }
        if (processo.getValorCausa() != null && processo.getValorCausa().compareTo(BigDecimal.ZERO) > 0) {
            return scale(processo.getValorCausa());
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularCorrecao(BigDecimal base, RamoDireito ramo, ExecucaoCumprimentoEngine.TituloExecutivo titulo) {
        BigDecimal fator = switch (ramo) {
            case TRABALHISTA -> BigDecimal.valueOf(0.08);
            case TRIBUTARIO -> BigDecimal.valueOf(0.06);
            case FAMILIA -> BigDecimal.valueOf(0.04);
            case PREVIDENCIARIO, ADMINISTRATIVO -> BigDecimal.valueOf(0.03);
            case AMBIENTAL -> BigDecimal.valueOf(0.09);
            default -> BigDecimal.valueOf(0.05);
        };
        if (titulo == ExecucaoCumprimentoEngine.TituloExecutivo.TITULO_JUDICIAL_PROVISORIO) {
            fator = fator.multiply(BigDecimal.valueOf(0.7));
        }
        return scale(base.multiply(fator));
    }

    private BigDecimal calcularJuros(BigDecimal base, RamoDireito ramo, ExecucaoCumprimentoEngine.TituloExecutivo titulo) {
        BigDecimal fator = switch (ramo) {
            case TRABALHISTA, FAMILIA -> BigDecimal.valueOf(0.02);
            case TRIBUTARIO -> BigDecimal.valueOf(0.015);
            case PREVIDENCIARIO, ADMINISTRATIVO -> BigDecimal.valueOf(0.01);
            default -> BigDecimal.valueOf(0.012);
        };
        if (titulo == ExecucaoCumprimentoEngine.TituloExecutivo.DECISAO_LIMINAR_ASTREINTES) {
            fator = fator.add(BigDecimal.valueOf(0.005));
        }
        return scale(base.multiply(fator));
    }

    private BigDecimal calcularHonorarios(BigDecimal base,
                                          RamoDireito ramo,
                                          ExecucaoCumprimentoEngine.TituloExecutivo titulo,
                                          boolean envolveFazendaPublica) {
        if (envolveFazendaPublica && (ramo == RamoDireito.PREVIDENCIARIO || ramo == RamoDireito.ADMINISTRATIVO)) {
            return scale(base.multiply(BigDecimal.valueOf(0.05)));
        }
        BigDecimal fator = tituloJudicial(titulo) ? BigDecimal.valueOf(0.10) : BigDecimal.valueOf(0.12);
        if (ramo == RamoDireito.TRABALHISTA) {
            fator = BigDecimal.valueOf(0.08);
        }
        return scale(base.multiply(fator));
    }

    private int prioridade(ExecucaoCumprimentoEngine.MeioExpropriatorio meio,
                           ExecucaoCumprimentoEngine.ComplianceExecucao compliance,
                           ExecucaoCumprimentoEngine.MatrizExpropriacao matriz) {
        int base = switch (meio) {
            case SISBAJUD_TEIMOSINHA -> 98;
            case SISBAJUD_BLOQUEIO_CONTA, BACENJUD_BLOQUEIO_CONTA -> 95;
            case BACENJUD_TRANSFERENCIA -> 92;
            case DESCONTO_FOLHA_ALIMENTOS -> 94;
            case SNIPER_PATRIMONIAL -> 89;
            case CCS_PESQUISA_RELACIONAMENTO -> 86;
            case INFOJUD_CONSULTA_BENS -> 82;
            case RENAJUD_RESTRICAO_VEICULO -> 80;
            case PENHORA_FATURAMENTO -> 79;
            case PENHORA_QUOTA_SOCIAL -> 74;
            case CNIB_INDISPONIBILIDADE_IMOVEL, PENHORA_IMOVEL_REGISTRO -> 72;
            case PENHORA_CREDITO_PRECATORIO -> 68;
            case SERASAJUD_NEGATIVACAO, PROTESTO_DECISAO_JUDICIAL -> 60;
            case ADJUDICACAO, ALIENACAO_JUDICIAL, ARREMATACAO_HASTA_PUBLICA, EXPROPRIACAO_DIRETA -> 58;
        };
        if (compliance.naturezaAlimentar() && meio == ExecucaoCumprimentoEngine.MeioExpropriatorio.DESCONTO_FOLHA_ALIMENTOS) {
            base += 6;
        }
        if (compliance.envolveFazendaPublica() && (meio == ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_CREDITO_PRECATORIO || meio == ExecucaoCumprimentoEngine.MeioExpropriatorio.PROTESTO_DECISAO_JUDICIAL)) {
            base += 8;
        }
        return clamp(base + (matriz.scoreRecuperabilidade() - matriz.scoreComplexidade()) / 10);
    }

    private BigDecimal estimarRecuperacaoBase(BigDecimal valorBase,
                                              ExecucaoCumprimentoEngine.ComplianceExecucao compliance,
                                              int scoreRecuperabilidade) {
        BigDecimal fator = BigDecimal.valueOf(Math.max(20, Math.min(scoreRecuperabilidade, 95))).movePointLeft(2);
        if (compliance.envolveFazendaPublica()) {
            fator = fator.multiply(BigDecimal.valueOf(0.65));
        }
        if (compliance.naturezaAlimentar()) {
            fator = fator.multiply(BigDecimal.valueOf(1.08));
        }
        return scale(valorBase.multiply(fator));
    }

    private GrauJurisdicao grau(Processo processo) {
        return processo.getJurisdicao() != null ? processo.getJurisdicao().getGrau() : null;
    }

    private String tribunalCodigo(Processo processo) {
        return processo.getJurisdicao() != null ? processo.getJurisdicao().getCodigo() : null;
    }

    private RamoDireito ramo(Processo processo) {
        return processo.getRamoDireito() != null ? processo.getRamoDireito() : RamoDireito.CIVIL;
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : scale(value);
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(score, 100));
    }

    private static List<String> immutableDistinct(List<String> values) {
        return List.copyOf(new LinkedHashSet<>(values == null ? List.of() : values));
    }
}
