package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoIndiceMensalRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoParcelaLivreRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.TrabalhistaCalculoAvancadoRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TrabalhistaCalculoAvancadoService {

    private final CalculoJudicialAssistenciaService assistenciaService;

    public TrabalhistaCalculoAvancadoService(CalculoJudicialAssistenciaService assistenciaService) {
        this.assistenciaService = assistenciaService;
    }

    public CalculoJudicialRelatorio calcular(TrabalhistaCalculoAvancadoRequest request, CalculoJudicialSolicitantePerfil perfil) {
        Objects.requireNonNull(request);
        CalculoJudicialSolicitantePerfil effectiveProfile = perfil == null ? CalculoJudicialSolicitantePerfil.ADVOGADO : perfil;
        LinkedList<CalculoJudicialLinha> itens = new LinkedList<>();
        List<String> alertas = new ArrayList<>();
        List<String> fundamentos = new ArrayList<>(List.of(
                "CLT art. 59 e §1º - hora extra com adicional mínimo de 50%.",
                "CLT art. 71, §4º - remuneração do período suprimido do intervalo intrajornada.",
                "Lei 12.506/2011 art. 1º - aviso prévio proporcional até 90 dias.",
                "Lei 8.036/1990 arts. 15 e 18 - FGTS mensal e multa rescisória.",
                "Lei 4.090/1962 - 13º salário proporcional.",
                "Constituição Federal art. 7º, XVII - terço constitucional sobre férias.",
                "CPC e liquidação trabalhista exigem memória de cálculo transparente, auditável e parametrizada."
        ));
        List<String> trilha = new ArrayList<>();

        BigDecimal salarioBase = CalculoJudicialMath.positive(request.salarioBase());
        BigDecimal parcelasFixas = CalculoJudicialMath.positive(request.outrasParcelasFixasMensais());
        BigDecimal remuneracaoMedia = positiveOrDefault(request.remuneracaoMedia(), salarioBase.add(parcelasFixas));
        BigDecimal cargaHorariaMensal = positiveOrDefault(request.cargaHorariaMensalBase(), new BigDecimal("220"));
        BigDecimal valorHoraBase = positiveOrDefault(request.valorHoraBaseInformado(), remuneracaoMedia.divide(cargaHorariaMensal, 10, RoundingMode.HALF_UP));
        int avos = CalculoJudicialMath.avosTrabalhistas(request.admissao(), request.demissao());
        int anosCompletos = CalculoJudicialMath.anosCompletos(request.admissao(), request.demissao());
        int diasTrabalhados = Math.max(0, Math.min(31, request.diasTrabalhadosNoMesRescisao() == null ? 30 : request.diasTrabalhadosNoMesRescisao()));
        boolean dispensaSemJustaCausa = isEmployerTermination(request.tipoDispensa());
        boolean pedidoDemissao = isEmployeeExit(request.tipoDispensa());
        boolean incluirReflexos = flagOn(request.incluirReflexosEmFeriasDecimoTerceiroFgts(), true);
        String criterioAtualizacao = blankOrDefault(request.criterioAtualizacaoNome(), "IPCA-E pré-judicial + SELIC judicial parametrizada");
        String criterioJuros = blankOrDefault(request.criterioJurosNome(), "SELIC judicial parametrizada");

        BigDecimal extras50 = calcularHorasExtras(itens, valorHoraBase, request.quantidadeHorasExtras50(), new BigDecimal("0.50"), effectiveProfile, trilha);
        BigDecimal extras100 = calcularHorasExtras(itens, valorHoraBase, request.quantidadeHorasExtras100(), BigDecimal.ONE, effectiveProfile, trilha);
        BigDecimal intervalo = calcularIntervaloIntrajornada(itens, valorHoraBase, request.quantidadeHorasIntervaloIntrajornada(), effectiveProfile, trilha);
        BigDecimal adicionalNoturno = calcularAdicionalNoturno(itens, valorHoraBase, request.quantidadeHorasNoturnas(), request.percentualAdicionalNoturno(), effectiveProfile, trilha);
        BigDecimal adicionalInsalubridade = calcularInsalubridade(itens, request.grauInsalubridade(), request.baseInsalubridade(), salarioBase, effectiveProfile, alertas, trilha);
        BigDecimal adicionalPericulosidade = calcularPericulosidade(itens, remuneracaoMedia, request.percentualPericulosidade(), effectiveProfile, trilha);
        BigDecimal parcelasHabitualidade = extras50.add(extras100).add(intervalo).add(adicionalNoturno).add(adicionalInsalubridade).add(adicionalPericulosidade);
        BigDecimal dsrExtras = calcularDsr(itens, extras50.add(extras100).add(intervalo), request.diasUteisMediaMes(), request.domingosFeriadosMediaMes(), effectiveProfile, alertas, trilha);
        BigDecimal reflexosHabituais = parcelasHabitualidade.add(dsrExtras);
        BigDecimal baseIntegrada = remuneracaoMedia.add(reflexosHabituais);

        BigDecimal saldoSalario = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (flagOn(request.incluirSaldoSalario(), true)) {
            saldoSalario = CalculoJudicialMath.fraction(baseIntegrada, new BigDecimal(diasTrabalhados), new BigDecimal("30"));
            itens.add(item("Principal", "SALDO_SALARIO", "Saldo de salário", baseIntegrada, new BigDecimal(diasTrabalhados), CalculoJudicialMath.ratio(new BigDecimal(diasTrabalhados), new BigDecimal("30")), saldoSalario, diasTrabalhados + "/30 x remuneração integrada", effectiveProfile,
                    "Parcela do último mês trabalhado proporcional aos dias efetivamente laborados.",
                    "Base integrada dividida por 30, multiplicada pelos dias trabalhados no mês da ruptura.",
                    "CLT arts. 457 e 459"));
            trilha.add("saldo_salario=" + saldoSalario);
        }

        BigDecimal decimoTerceiro = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (flagOn(request.incluirDecimoTerceiro(), true)) {
            decimoTerceiro = CalculoJudicialMath.fraction(baseIntegrada, new BigDecimal(avos), new BigDecimal("12"));
            itens.add(item("Principal", "DECIMO_TERCEIRO_PROP", "13º proporcional", baseIntegrada, new BigDecimal(avos), CalculoJudicialMath.ratio(new BigDecimal(avos), new BigDecimal("12")), decimoTerceiro, avos + "/12 x remuneração integrada", effectiveProfile,
                    "Parcela anual proporcional aos meses com pelo menos 15 dias trabalhados.",
                    "Regra duodecimal com apuração de avos considerando trabalho igual ou superior a 15 dias por competência.",
                    "Lei 4.090/1962"));
            trilha.add("decimo_terceiro=" + decimoTerceiro + " avos=" + avos);
        }

        BigDecimal feriasProp = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tercoFerias = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (flagOn(request.incluirFeriasProporcionais(), true)) {
            feriasProp = CalculoJudicialMath.fraction(baseIntegrada, new BigDecimal(avos), new BigDecimal("12"));
            tercoFerias = CalculoJudicialMath.money(feriasProp.divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP));
            itens.add(item("Principal", "FERIAS_PROP", "Férias proporcionais", baseIntegrada, new BigDecimal(avos), CalculoJudicialMath.ratio(new BigDecimal(avos), new BigDecimal("12")), feriasProp, avos + "/12 x remuneração integrada", effectiveProfile,
                    "Férias proporcionais calculadas pelos avos do período aquisitivo.",
                    "Apuração em duodécimos sobre remuneração integrada, sem abatimentos específicos.",
                    "CLT arts. 130 e 142"));
            itens.add(item("Principal", "TERCO_FERIAS", "1/3 constitucional de férias", feriasProp, BigDecimal.ONE, new BigDecimal("0.333333"), tercoFerias, "férias proporcionais / 3", effectiveProfile,
                    "Adicional constitucional incidente sobre as férias proporcionais.",
                    "Aplicação direta do terço constitucional sobre o valor das férias proporcionais.",
                    "CF art. 7º, XVII"));
            trilha.add("ferias_prop=" + feriasProp + " terco=" + tercoFerias);
        }

        BigDecimal avisoPrevio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (flagOn(request.incluirAvisoPrevio(), true)) {
            int diasAviso = request.diasAvisoPrevioInformado() != null && request.diasAvisoPrevioInformado() >= 0
                    ? Math.min(90, request.diasAvisoPrevioInformado())
                    : 30 + Math.min(60, anosCompletos * 3);
            avisoPrevio = dispensaSemJustaCausa
                    ? CalculoJudicialMath.fraction(baseIntegrada, new BigDecimal(diasAviso), new BigDecimal("30"))
                    : pedidoDemissao
                    ? CalculoJudicialMath.fraction(baseIntegrada.negate(), new BigDecimal("30"), BigDecimal.ONE)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            itens.add(item("Principal", "AVISO_PREVIO", "Aviso prévio", baseIntegrada, new BigDecimal(diasAviso), CalculoJudicialMath.ratio(new BigDecimal(diasAviso), new BigDecimal("30")), avisoPrevio, diasAviso + "/30 x remuneração integrada", effectiveProfile,
                    "Aviso prévio proporcional conforme o tempo de serviço ou desconto simplificado no pedido de demissão.",
                    "Cálculo baseado na Lei 12.506/2011, com 30 dias mais 3 por ano completo, limitado a 90 dias, salvo parametrização expressa.",
                    "Lei 12.506/2011 e CLT art. 487"));
            trilha.add("aviso_previo=" + avisoPrevio + " dias=" + diasAviso + " tipo=" + request.tipoDispensa());
        }

        BigDecimal reflexosFeriasDecimo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal reflexosFgts = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (incluirReflexos) {
            BigDecimal baseReflexos = parcelasHabitualidade.add(dsrExtras);
            if (baseReflexos.signum() > 0) {
                BigDecimal reflexoDecimo = CalculoJudicialMath.fraction(baseReflexos, new BigDecimal(avos), new BigDecimal("12"));
                BigDecimal reflexoFerias = CalculoJudicialMath.fraction(baseReflexos, new BigDecimal(avos), new BigDecimal("12"));
                BigDecimal reflexoTerco = CalculoJudicialMath.money(reflexoFerias.divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP));
                reflexosFeriasDecimo = reflexoDecimo.add(reflexoFerias).add(reflexoTerco).setScale(2, RoundingMode.HALF_UP);
                itens.add(item("Acessórios", "REFLEXOS_13", "Reflexos em 13º salário", baseReflexos, new BigDecimal(avos), CalculoJudicialMath.ratio(new BigDecimal(avos), new BigDecimal("12")), reflexoDecimo, "base habitual x avos / 12", effectiveProfile,
                        "Reflexo das parcelas habituais sobre o 13º salário.",
                        "Integração duodecimal das verbas habituais sobre a gratificação natalina.",
                        "Súmulas e jurisprudência trabalhista sobre integração de parcelas habituais"));
                itens.add(item("Acessórios", "REFLEXOS_FERIAS", "Reflexos em férias + 1/3", baseReflexos, new BigDecimal(avos), CalculoJudicialMath.ratio(new BigDecimal(avos), new BigDecimal("12")), reflexoFerias.add(reflexoTerco), "(férias reflexas + 1/3) sobre base habitual", effectiveProfile,
                        "Reflexo das parcelas habituais sobre férias proporcionais e terço constitucional.",
                        "Integração das verbas habituais na base de férias, com incidência adicional do terço constitucional.",
                        "CF art. 7º, XVII e jurisprudência trabalhista sobre integração"));
                trilha.add("reflexos_ferias_decimo=" + reflexosFeriasDecimo);
            }
        }

        BigDecimal subtotalPrincipal = somarSecao(itens, "Principal");

        BigDecimal fgtsProjetado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (flagOn(request.incluirFgtsMensal(), true)) {
            int mesesFgts = mesesCompetenciaInclusiva(request.admissao(), request.demissao());
            fgtsProjetado = CalculoJudicialMath.percent(baseIntegrada.multiply(new BigDecimal(mesesFgts)), new BigDecimal("0.08"));
            itens.add(item("Acessórios", "FGTS_MENSAL", "FGTS projetado do período", baseIntegrada.multiply(new BigDecimal(mesesFgts)), new BigDecimal(mesesFgts), new BigDecimal("0.08"), fgtsProjetado, "remuneração integrada x competências x 8%", effectiveProfile,
                    "Depósitos mensais estimados de FGTS sobre a remuneração do período projetado.",
                    "Incidência de 8% sobre a remuneração devida em cada competência, inclusive 13º, conforme recorte simplificado do pedido.",
                    "Lei 8.036/1990 art. 15"));
            if (incluirReflexos && reflexosFeriasDecimo.signum() > 0) {
                reflexosFgts = CalculoJudicialMath.percent(reflexosFeriasDecimo, new BigDecimal("0.08"));
                itens.add(item("Acessórios", "FGTS_REFLEXOS", "FGTS sobre reflexos habituais", reflexosFeriasDecimo, BigDecimal.ONE, new BigDecimal("0.08"), reflexosFgts, "reflexos x 8%", effectiveProfile,
                        "FGTS incidente sobre reflexos habituais integrados na memória.",
                        "A memória destaca o FGTS autônomo sobre reflexos para facilitar conferência e auditoria por rubrica.",
                        "Lei 8.036/1990 art. 15"));
            }
            if (flagOn(request.incluirMultaFgts40(), dispensaSemJustaCausa)) {
                BigDecimal massaFgts = fgtsProjetado.add(reflexosFgts);
                BigDecimal multa = CalculoJudicialMath.percent(massaFgts, new BigDecimal("0.40"));
                itens.add(item("Acessórios", "FGTS_MULTA_40", "Multa de 40% do FGTS", massaFgts, BigDecimal.ONE, new BigDecimal("0.40"), multa, "massa de FGTS x 40%", effectiveProfile,
                        "Multa rescisória incidente sobre os depósitos fundiários em hipóteses típicas de dispensa sem justa causa.",
                        "Aplicação do percentual de 40% sobre a massa projetada de depósitos do FGTS, sujeita à prova do extrato real.",
                        "Lei 8.036/1990 art. 18"));
                trilha.add("fgts_multa_40=" + multa);
            }
            trilha.add("fgts_periodo=" + fgtsProjetado + " reflexos_fgts=" + reflexosFgts);
        }

        BigDecimal multa467 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (flagOn(request.aplicarMultaArt467(), false)) {
            BigDecimal base467 = positiveVerbasRescisorias(saldoSalario, decimoTerceiro, feriasProp, tercoFerias, avisoPrevio);
            multa467 = CalculoJudicialMath.percent(base467, new BigDecimal("0.50"));
            itens.add(item("Acessórios", "MULTA_467", "Multa do art. 467 da CLT", base467, BigDecimal.ONE, new BigDecimal("0.50"), multa467, "verbas incontroversas x 50%", effectiveProfile,
                    "Parcela penal incidente sobre verbas rescisórias incontroversas não pagas oportunamente.",
                    "A memória calcula a multa em chave prudencial a partir da massa rescisória positiva, sem substituir a qualificação jurídica do caso concreto.",
                    "CLT art. 467"));
            trilha.add("multa_467=" + multa467);
        }

        BigDecimal multa477 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (flagOn(request.aplicarMultaArt477(), false)) {
            multa477 = CalculoJudicialMath.money(salarioBase);
            itens.add(item("Acessórios", "MULTA_477", "Multa do art. 477, §8º, da CLT", salarioBase, BigDecimal.ONE, BigDecimal.ONE, multa477, "salário-base", effectiveProfile,
                    "Multa equivalente a um salário em hipóteses de atraso no pagamento rescisório reconhecido no caso concreto.",
                    "A memória trata a multa do art. 477 em chave parametrizada, preservando a necessidade de exame dos marcos temporais e da controvérsia válida.",
                    "CLT art. 477, §8º"));
            trilha.add("multa_477=" + multa477);
        }

        BigDecimal honorarios = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (request.percentualHonorariosSucumbenciais() != null && request.percentualHonorariosSucumbenciais().signum() > 0) {
            BigDecimal baseHonorarios = somarSecao(itens, "Principal").add(somarSecao(itens, "Acessórios"));
            honorarios = CalculoJudicialMath.percent(baseHonorarios, request.percentualHonorariosSucumbenciais());
            itens.add(item("Acessórios", "HONORARIOS_SUCUMBENCIAIS", "Honorários sucumbenciais", baseHonorarios, BigDecimal.ONE, request.percentualHonorariosSucumbenciais(), honorarios, "base consolidada x percentual informado", effectiveProfile,
                    "Honorários calculados sobre a base consolidada conforme o percentual informado.",
                    "A memória separa a base honorária e mantém o percentual parametrizado para facilitar revisão judicial e pericial.",
                    "CLT art. 791-A"));
            trilha.add("honorarios_sucumbenciais=" + honorarios);
        }

        BigDecimal baseContribuicoes = somarSecao(itens, "Principal").add(somarSecao(itens, "Acessórios"));
        BigDecimal inssSegurado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (flagOn(request.incluirInssSegurado(), false)) {
            BigDecimal percentual = positiveOrDefault(request.percentualInssSegurado(), new BigDecimal("0.09"));
            inssSegurado = CalculoJudicialMath.percent(baseContribuicoes, percentual);
            itens.add(item("Acessórios", "INSS_SEGURADO_ESTIMADO", "INSS estimado do segurado", baseContribuicoes, BigDecimal.ONE, percentual, inssSegurado.negate(), "base previdenciária estimada x percentual informado", effectiveProfile,
                    "Estimativa previdenciária do segurado destacada para visão econômica do cálculo.",
                    "A dedução previdenciária é projetada com percentual parametrizado, sem substituir apuração por faixas e competências quando exigidas no caso real.",
                    "Lei 8.212/1991 e legislação previdenciária"));
            trilha.add("inss_segurado_estimado=" + inssSegurado);
        }

        BigDecimal irrf = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (flagOn(request.incluirIrrf(), false)) {
            BigDecimal percentual = positiveOrDefault(request.percentualIrrfEfetivo(), new BigDecimal("0.15"));
            BigDecimal baseIrrf = baseContribuicoes.subtract(inssSegurado).max(BigDecimal.ZERO);
            irrf = CalculoJudicialMath.percent(baseIrrf, percentual);
            itens.add(item("Acessórios", "IRRF_ESTIMADO", "IRRF efetivo estimado", baseIrrf, BigDecimal.ONE, percentual, irrf.negate(), "base tributável estimada x percentual efetivo informado", effectiveProfile,
                    "Estimativa de imposto de renda destacada para leitura econômica final do cálculo.",
                    "A memória usa percentual efetivo parametrizado e não substitui a tributação por tabela, regime de competência ou cálculo oficial de retenção.",
                    "Legislação do IRRF e critérios jurisprudenciais aplicáveis"));
            trilha.add("irrf_estimado=" + irrf);
        }

        aplicarParcelasLivres(itens, request.parcelasLivres(), effectiveProfile, trilha);

        BigDecimal subtotalAtualizacao = calcularAtualizacaoTrabalhista(itens, somarSecao(itens, "Principal").add(somarSecao(itens, "Acessórios")), request, effectiveProfile, alertas, trilha);
        BigDecimal subtotalAcessorios = somarSecao(itens, "Acessórios");
        BigDecimal total = subtotalPrincipal.add(subtotalAtualizacao).add(subtotalAcessorios).setScale(2, RoundingMode.HALF_UP);

        if (request.observacoesTecnicas() != null && !request.observacoesTecnicas().isBlank()) {
            alertas.add("Observação técnica declarada pelo usuário: " + request.observacoesTecnicas().trim());
        }
        if (request.dataInicioAtualizacao() != null && request.dataFimAtualizacao() != null && request.dataFimAtualizacao().isBefore(request.dataInicioAtualizacao())) {
            alertas.add("O período de atualização informado está invertido; a memória preservou os totais sem aplicar fator temporal inválido.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("avos", avos);
        metadata.put("anosCompletos", anosCompletos);
        metadata.put("dispensaSemJustaCausa", dispensaSemJustaCausa);
        metadata.put("pedidoDemissao", pedidoDemissao);
        metadata.put("baseIntegrada", baseIntegrada);
        metadata.put("valorHoraBase", valorHoraBase);
        metadata.put("perfilApresentacao", effectiveProfile.name());
        metadata.put("modo", "ADVANCED_WORK_LABOR_MEMORY_2026");
        metadata.put("criteriosAplicados", List.of(
                CalculoJudicialMetadataSupport.criterion("Correção monetária", criterioAtualizacao),
                CalculoJudicialMetadataSupport.criterion("Juros e fase judicial", criterioJuros),
                CalculoJudicialMetadataSupport.criterion("Reflexos habituais", incluirReflexos ? "Ativados para 13º, férias + 1/3 e FGTS" : "Desativados"),
                CalculoJudicialMetadataSupport.criterion("Perfil de apresentação", effectiveProfile.name())
        ));
        metadata.put("parameterBlocks", List.of(
                CalculoJudicialMetadataSupport.block("Dados do processo e partes", CalculoJudicialMetadataSupport.map(
                        "Processo", blankOrDash(request.numeroProcesso()),
                        "Reclamante", blankOrDash(request.reclamanteNome()),
                        "Reclamado", blankOrDash(request.reclamadoNome()),
                        "Tipo de dispensa", blankOrDash(request.tipoDispensa())
                )),
                CalculoJudicialMetadataSupport.block("Parâmetros remuneratórios e jornada", CalculoJudicialMetadataSupport.map(
                        "Salário-base", CalculoJudicialMetadataSupport.money(salarioBase),
                        "Remuneração média", CalculoJudicialMetadataSupport.money(remuneracaoMedia),
                        "Carga horária mensal", CalculoJudicialMetadataSupport.stringify(cargaHorariaMensal),
                        "Valor hora-base", CalculoJudicialMetadataSupport.money(valorHoraBase),
                        "Outras parcelas fixas", CalculoJudicialMetadataSupport.money(parcelasFixas)
                )),
                CalculoJudicialMetadataSupport.block("Rubricas especiais e reflexos", CalculoJudicialMetadataSupport.map(
                        "Horas extras 50%", CalculoJudicialMetadataSupport.stringify(CalculoJudicialMath.positive(request.quantidadeHorasExtras50())),
                        "Horas extras 100%", CalculoJudicialMetadataSupport.stringify(CalculoJudicialMath.positive(request.quantidadeHorasExtras100())),
                        "Intervalo intrajornada", CalculoJudicialMetadataSupport.stringify(CalculoJudicialMath.positive(request.quantidadeHorasIntervaloIntrajornada())),
                        "Horas noturnas", CalculoJudicialMetadataSupport.stringify(CalculoJudicialMath.positive(request.quantidadeHorasNoturnas())),
                        "Insalubridade", blankOrDash(request.grauInsalubridade()),
                        "Periculosidade", CalculoJudicialMetadataSupport.percent(request.percentualPericulosidade())
                )),
                CalculoJudicialMetadataSupport.block("Critérios financeiros e atualizações", CalculoJudicialMetadataSupport.map(
                        "Data inicial da atualização", request.dataInicioAtualizacao(),
                        "Data final da atualização", request.dataFimAtualizacao(),
                        "Fator pré-judicial", CalculoJudicialMetadataSupport.percent(request.fatorPreJudicialIpcae()),
                        "Critério de correção", criterioAtualizacao,
                        "Critério de juros", criterioJuros,
                        "Honorários sucumbenciais", CalculoJudicialMetadataSupport.percent(request.percentualHonorariosSucumbenciais())
                ))
        ));
        metadata.put("indexSeries", CalculoJudicialMetadataSupport.indexSeries(request.taxasSelicMensais()));
        metadata.put("operationalHighlights", List.of(
                "A memória separa principal, acessórios, atualização e descontos estimados para auditoria objetiva.",
                "Reflexos habituais permanecem destacados em rubricas autônomas para conferência do advogado, contador e magistratura.",
                "INSS e IRRF são tratados como projeções econômicas parametrizadas quando o usuário opta por incluí-los."
        ));
        metadata.put("entryGuide", List.of(
                "Preencha apenas os campos que realmente existam no caso concreto; os demais podem ficar nulos.",
                "Use a série mensal de SELIC quando houver memória oficial ou planilha do caso.",
                "Quando houver critério pericial próprio, utilize observações técnicas para registrar a divergência controlada."
        ));
        metadata.put("uiSections", List.of("Dados iniciais", "Jornada e verbas", "Reflexos e FGTS", "Atualização", "Penalidades e encargos", "Observações"));
        metadata.putAll(assistenciaService.metadataTrabalhista(request, effectiveProfile));
        metadata.put("readyNotification", CalculatorHelpMessages.readyNotificationPayload("TRABALHISTA_CLT", effectiveProfile, total, false));
        metadata.put("readyNotificationIaAssistida", CalculatorHelpMessages.readyNotificationPayload("TRABALHISTA_CLT", effectiveProfile, total, true));

        return new CalculoJudicialRelatorio(
                "TRABALHISTA_CLT",
                request.tituloCalculo() == null || request.tituloCalculo().isBlank() ? "Calculadora Trabalhista CLT PJB 2026" : request.tituloCalculo().trim(),
                request.numeroProcesso(),
                effectiveProfile,
                "O cálculo trabalhista foi organizado por verbas principais, reflexos, FGTS, penalidades e atualização, com leitura acessível e trilha auditável.",
                "A memória trabalhista consolida rubricas rescisórias, parcelas de habitualidade, reflexos, FGTS, penalidades e projeções fiscais/previdenciárias em estrutura pericial auditável.",
                subtotalPrincipal,
                subtotalAtualizacao,
                subtotalAcessorios,
                total,
                List.copyOf(itens),
                List.copyOf(alertas),
                List.copyOf(fundamentos),
                List.copyOf(trilha),
                safeMetadata(metadata),
                Instant.now()
        );
    }

    private CalculoJudicialLinha item(String secao, String codigo, String titulo, BigDecimal base, BigDecimal quantidade, BigDecimal aliquota, BigDecimal valor, String formula, CalculoJudicialSolicitantePerfil perfil, String citizen, String technical, String baseLegal) {
        return new CalculoJudicialLinha(secao, codigo, titulo, base, quantidade, aliquota, valor, formula, explain(perfil, citizen, technical), technical, baseLegal);
    }

    private BigDecimal calcularHorasExtras(List<CalculoJudicialLinha> itens,
                                          BigDecimal valorHora,
                                          BigDecimal quantidadeHoras,
                                          BigDecimal adicional,
                                          CalculoJudicialSolicitantePerfil perfil,
                                          List<String> trilha) {
        if (quantidadeHoras == null || quantidadeHoras.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal fator = BigDecimal.ONE.add(adicional);
        BigDecimal valor = CalculoJudicialMath.money(valorHora.multiply(quantidadeHoras).multiply(fator));
        String codigo = adicional.compareTo(BigDecimal.ONE) >= 0 ? "HORAS_EXTRAS_100" : "HORAS_EXTRAS_50";
        itens.add(item("Principal", codigo, "Horas extras " + percentualToText(adicional), valorHora, quantidadeHoras, fator, valor, "valor hora x quantidade x (1 + adicional)", perfil,
                "Horas extras calculadas com o adicional indicado, destacadas em rubrica própria.",
                "Apuração de horas extraordinárias com fator remuneratório composto pelo valor da hora e pelo adicional legal ou convencional informado.",
                "CLT art. 59 e norma coletiva aplicável"));
        trilha.add(codigo.toLowerCase() + "=" + valor + " horas=" + quantidadeHoras);
        return valor;
    }

    private BigDecimal calcularIntervaloIntrajornada(List<CalculoJudicialLinha> itens,
                                                     BigDecimal valorHora,
                                                     BigDecimal quantidadeHoras,
                                                     CalculoJudicialSolicitantePerfil perfil,
                                                     List<String> trilha) {
        if (quantidadeHoras == null || quantidadeHoras.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal fator = new BigDecimal("1.50");
        BigDecimal valor = CalculoJudicialMath.money(valorHora.multiply(quantidadeHoras).multiply(fator));
        itens.add(item("Principal", "INTERVALO_INTRAJORNADA", "Intervalo intrajornada suprimido", valorHora, quantidadeHoras, fator, valor, "valor hora x quantidade x 1,5", perfil,
                "Período de intervalo não usufruído remunerado com adicional legal.",
                "A rubrica calcula a supressão parcial ou total do intervalo intrajornada em chave remuneratória com adicional de 50%, sem substituir a prova do número efetivo de ocorrências.",
                "CLT art. 71, §4º"));
        trilha.add("intervalo_intrajornada=" + valor + " horas=" + quantidadeHoras);
        return valor;
    }

    private BigDecimal calcularAdicionalNoturno(List<CalculoJudicialLinha> itens,
                                                BigDecimal valorHora,
                                                BigDecimal quantidadeHoras,
                                                BigDecimal percentualInformado,
                                                CalculoJudicialSolicitantePerfil perfil,
                                                List<String> trilha) {
        if (quantidadeHoras == null || quantidadeHoras.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal percentual = positiveOrDefault(percentualInformado, new BigDecimal("0.20"));
        BigDecimal valor = CalculoJudicialMath.money(valorHora.multiply(quantidadeHoras).multiply(percentual));
        itens.add(item("Principal", "ADICIONAL_NOTURNO", "Adicional noturno", valorHora, quantidadeHoras, percentual, valor, "valor hora x horas noturnas x percentual", perfil,
                "Adicional noturno calculado conforme percentual informado ou padrão legal.",
                "Rubrica calculada sobre a quantidade de horas noturnas parametrizada, mantendo percentuais convencionais ou legais sem presumir redução ficta automática.",
                "CLT art. 73"));
        trilha.add("adicional_noturno=" + valor + " percentual=" + percentual);
        return valor;
    }

    private BigDecimal calcularInsalubridade(List<CalculoJudicialLinha> itens,
                                             String grau,
                                             BigDecimal baseInformada,
                                             BigDecimal salarioBase,
                                             CalculoJudicialSolicitantePerfil perfil,
                                             List<String> alertas,
                                             List<String> trilha) {
        String normalized = grau == null ? "" : grau.trim().toUpperCase();
        BigDecimal percentual = switch (normalized) {
            case "MINIMO", "GRAU_MINIMO", "10" -> new BigDecimal("0.10");
            case "MEDIO", "GRAU_MEDIO", "20" -> new BigDecimal("0.20");
            case "MAXIMO", "GRAU_MAXIMO", "40" -> new BigDecimal("0.40");
            default -> BigDecimal.ZERO;
        };
        if (percentual.signum() == 0) {
            if (grau != null && !grau.isBlank()) {
                alertas.add("Grau de insalubridade informado não foi reconhecido; a rubrica foi ignorada até confirmação técnica.");
            }
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal base = positiveOrDefault(baseInformada, salarioBase);
        BigDecimal valor = CalculoJudicialMath.percent(base, percentual);
        itens.add(item("Principal", "ADICIONAL_INSALUBRIDADE", "Adicional de insalubridade", base, BigDecimal.ONE, percentual, valor, "base x percentual do grau", perfil,
                "Adicional de insalubridade calculado pelo grau informado e sobre a base parametrizada.",
                "Rubrica parametrizada para permitir aderência à base pericial ou judicial do caso, preservando a controvérsia sobre base diversa do salário mínimo quando existente.",
                "CLT art. 192 e jurisprudência aplicável"));
        trilha.add("insalubridade=" + valor + " grau=" + normalized);
        return valor;
    }

    private BigDecimal calcularPericulosidade(List<CalculoJudicialLinha> itens,
                                              BigDecimal remuneracaoMedia,
                                              BigDecimal percentualInformado,
                                              CalculoJudicialSolicitantePerfil perfil,
                                              List<String> trilha) {
        BigDecimal percentual = positiveOrDefault(percentualInformado, new BigDecimal("0.30"));
        if (percentual.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal valor = CalculoJudicialMath.percent(remuneracaoMedia, percentual);
        itens.add(item("Principal", "ADICIONAL_PERICULOSIDADE", "Adicional de periculosidade", remuneracaoMedia, BigDecimal.ONE, percentual, valor, "remuneração média x percentual", perfil,
                "Parcela adicional atribuída a trabalho em condição perigosa.",
                "Apuração parametrizável do adicional, com default de 30% sobre remuneração-base informada.",
                "CLT art. 193"));
        trilha.add("periculosidade=" + valor);
        return valor;
    }

    private BigDecimal calcularDsr(List<CalculoJudicialLinha> itens,
                                   BigDecimal base,
                                   Integer diasUteis,
                                   Integer domingosFeriados,
                                   CalculoJudicialSolicitantePerfil perfil,
                                   List<String> alertas,
                                   List<String> trilha) {
        int uteis = diasUteis == null ? 26 : Math.max(1, diasUteis);
        int repousos = domingosFeriados == null ? 4 : Math.max(0, domingosFeriados);
        if (base.signum() == 0 || repousos == 0) {
            if (base.signum() > 0 && repousos == 0) {
                alertas.add("Base de horas extras identificada sem domingos/feriados médios; o DSR não foi agregado por ausência de parâmetro de repouso.");
            }
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal valor = CalculoJudicialMath.money(base.divide(new BigDecimal(uteis), 10, RoundingMode.HALF_UP).multiply(new BigDecimal(repousos)));
        itens.add(item("Principal", "DSR_SOBRE_VARIAVEIS", "DSR sobre verbas variáveis", base, new BigDecimal(repousos), CalculoJudicialMath.ratio(new BigDecimal(repousos), new BigDecimal(uteis)), valor, "base variável / dias úteis x repousos", perfil,
                "Reflexo em repouso semanal remunerado calculado sobre parcelas variáveis da jornada.",
                "DSR calculado por proporção entre dias úteis e repousos, sem substituir a memória diária quando o caso exigir granularidade mais fina.",
                "Lei 605/1949 e jurisprudência trabalhista"));
        trilha.add("dsr_variaveis=" + valor + " uteis=" + uteis + " repousos=" + repousos);
        return valor;
    }

    private void aplicarParcelasLivres(List<CalculoJudicialLinha> itens,
                                       List<CalculoParcelaLivreRequest> parcelas,
                                       CalculoJudicialSolicitantePerfil perfil,
                                       List<String> trilha) {
        if (parcelas == null || parcelas.isEmpty()) {
            return;
        }
        for (CalculoParcelaLivreRequest parcela : parcelas) {
            if (parcela == null || parcela.valor() == null || parcela.valor().signum() == 0) {
                continue;
            }
            String codigo = CalculoJudicialMath.normalizeCode(parcela.codigo());
            itens.add(item("Acessórios", codigo.isBlank() ? "PARCELA_LIVRE" : codigo, parcela.descricao(), parcela.valor(), BigDecimal.ONE, BigDecimal.ONE, CalculoJudicialMath.money(parcela.valor()), "valor informado pelo solicitante", perfil,
                    "Parcela livre adicionada manualmente para refletir verba específica do caso concreto.",
                    "Item customizado mantido na memória com base legal vinculada e sem heurística de recálculo automático.",
                    parcela.baseLegal()));
            trilha.add("parcela_livre:" + parcela.codigo() + "=" + parcela.valor());
        }
    }

    private BigDecimal calcularAtualizacaoTrabalhista(List<CalculoJudicialLinha> itens,
                                                      BigDecimal base,
                                                      TrabalhistaCalculoAvancadoRequest request,
                                                      CalculoJudicialSolicitantePerfil perfil,
                                                      List<String> alertas,
                                                      List<String> trilha) {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal fatorPre = request.fatorPreJudicialIpcae() == null ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP) : request.fatorPreJudicialIpcae();
        if (fatorPre.signum() > 0) {
            BigDecimal valor = CalculoJudicialMath.percent(base, fatorPre);
            itens.add(item("Atualização", "PRE_JUDICIAL_IPCAE", "Atualização pré-judicial parametrizada", base, BigDecimal.ONE, fatorPre, valor, "base x fator informado", perfil,
                    "Atualização monetária pré-judicial segundo fator informado na memória.",
                    "Aplicação direta do fator pré-judicial parametrizado, útil para aderir a memória técnica construída fora do sistema.",
                    "STF ADC 58/59 e ADIs 5867/6021"));
            subtotal = subtotal.add(valor);
            trilha.add("pre_judicial_ipcae=" + valor + " fator=" + fatorPre);
        }
        BigDecimal fatorSelic = CalculoJudicialMath.fatorAcumuladoMensal(request.taxasSelicMensais());
        if (fatorSelic.signum() > 0) {
            BigDecimal valor = CalculoJudicialMath.percent(base.add(subtotal), fatorSelic);
            itens.add(item("Atualização", "SELIC_JUDICIAL_PARAM", "Atualização judicial SELIC parametrizada", base.add(subtotal), BigDecimal.ONE, fatorSelic, valor, "base atualizada x fator SELIC acumulado informado", perfil,
                    "Atualização judicial pela SELIC conforme série mensal informada na memória.",
                    "O sistema usa a série mensal fornecida pelo usuário; para a fase judicial trabalhista, o fator deve observar o critério aplicável ao caso concreto sem duplicar juros autônomos.",
                    "STF ADC 58/59 e TST sobre atualização judicial pela SELIC"));
            subtotal = subtotal.add(valor);
            trilha.add("selic_judicial=" + valor + " fator=" + fatorSelic);
        }
        if (request.dataInicioAtualizacao() != null && request.dataFimAtualizacao() != null && !request.dataFimAtualizacao().isBefore(request.dataInicioAtualizacao()) && fatorPre.signum() == 0 && fatorSelic.signum() == 0) {
            long dias = ChronoUnit.DAYS.between(request.dataInicioAtualizacao(), request.dataFimAtualizacao());
            alertas.add("Foi informado período de atualização de " + dias + " dias sem fator/IPCA-E ou série SELIC; o total foi emitido sem recomposição automática para evitar erro de índice presumido.");
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal positiveVerbasRescisorias(BigDecimal... values) {
        BigDecimal out = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (BigDecimal value : values) {
            if (value != null && value.signum() > 0) {
                out = out.add(value);
            }
        }
        return out;
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal defaultValue) {
        return value != null && value.signum() > 0 ? value : defaultValue;
    }

    private boolean flagOn(Boolean explicit, boolean defaultValue) {
        return explicit == null ? defaultValue : explicit;
    }

    private boolean isEmployerTermination(String tipoDispensa) {
        String code = CalculoJudicialMath.normalizeCode(tipoDispensa);
        return code.contains("SEM_JUSTA_CAUSA") || code.contains("RESCISAO_INDIRETA") || code.contains("ACORDO_484A");
    }

    private boolean isEmployeeExit(String tipoDispensa) {
        return CalculoJudicialMath.normalizeCode(tipoDispensa).contains("PEDIDO_DEMISSAO");
    }

    private int mesesCompetenciaInclusiva(LocalDate admissao, LocalDate demissao) {
        if (admissao == null || demissao == null || demissao.isBefore(admissao)) {
            return 0;
        }
        return (demissao.getYear() - admissao.getYear()) * 12 + demissao.getMonthValue() - admissao.getMonthValue() + 1;
    }

    private BigDecimal somarSecao(List<CalculoJudicialLinha> itens, String secao) {
        return itens.stream().filter(item -> secao.equals(item.secao())).map(CalculoJudicialLinha::valor).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private String explain(CalculoJudicialSolicitantePerfil perfil, String citizen, String technical) {
        return perfil.citizenLike() ? citizen : technical;
    }

    private String percentualToText(BigDecimal adicional) {
        return adicional.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String blankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        Map<String, Object> safe = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        safe.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(safe);
    }
}
