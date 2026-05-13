package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoIndiceMensalRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.FederalPrevidenciarioCjfCalculoAvancadoRequest;
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
public class FederalPrevidenciarioCjfCalculoAvancadoService {

    private final CalculoJudicialAssistenciaService assistenciaService;

    public FederalPrevidenciarioCjfCalculoAvancadoService(CalculoJudicialAssistenciaService assistenciaService) {
        this.assistenciaService = Objects.requireNonNull(assistenciaService);
    }

    public CalculoJudicialRelatorio calcular(FederalPrevidenciarioCjfCalculoAvancadoRequest request, CalculoJudicialSolicitantePerfil perfil) {
        Objects.requireNonNull(request);
        CalculoJudicialSolicitantePerfil effectiveProfile = perfil == null ? CalculoJudicialSolicitantePerfil.ADVOGADO : perfil;
        LinkedList<CalculoJudicialLinha> itens = new LinkedList<>();
        List<String> alertas = new ArrayList<>();
        List<String> fundamentos = new ArrayList<>(List.of(
                "O Manual de Cálculos da Justiça Federal e o SICOM do CJF evidenciam a necessidade de memória parametrizada com correção monetária, juros e conferência por competência.",
                "Planilhas e orientações oficiais dos Juizados Federais mostram cálculo de atrasados previdenciários, diferenças de implantação e abatimento de valores já pagos como rotina operacional relevante.",
                "Ferramentas oficiais de contadoria e módulos de cálculo para advogados reforçam a importância de separar parcelas vencidas, abono anual, compensações, honorários e classificação RPV/precatório.",
                "A memória previdenciária deve permanecer parametrizada para refletir entendimento judicial, manual institucional, séries de correção e eventos de implantação sem hardcode rígido."
        ));
        List<String> trilha = new ArrayList<>();

        BigDecimal rendaMensal = CalculoJudicialMath.positive(request.rendaMensalAtual());
        LocalDate dib = request.dib();
        LocalDate dataCalculo = request.dataCalculo();
        LocalDate dataFimBase = request.dip() != null && request.dip().isBefore(dataCalculo) ? request.dip() : dataCalculo;
        if (request.dcb() != null && request.dcb().isBefore(dataFimBase)) {
            dataFimBase = request.dcb();
        }
        LocalDate inicioElegivel = dib;
        if (Boolean.TRUE.equals(request.aplicarPrescricaoQuinquenal()) && request.dataAjuizamento() != null) {
            LocalDate marcoPrescricao = request.dataAjuizamento().minusYears(5);
            if (marcoPrescricao.isAfter(inicioElegivel)) {
                inicioElegivel = marcoPrescricao;
                alertas.add("A memória aplicou corte quinquenal a partir do ajuizamento informado.");
            }
        }
        if (dataFimBase.isBefore(inicioElegivel)) {
            alertas.add("As datas informadas não geram parcelas vencidas positivas após o recorte temporal adotado.");
        }
        long parcelas = dataFimBase.isBefore(inicioElegivel) ? 0 : ChronoUnit.MONTHS.between(inicioElegivel.withDayOfMonth(1), dataFimBase.withDayOfMonth(1)) + 1;
        BigDecimal parcelasVencidas = CalculoJudicialMath.money(rendaMensal.multiply(BigDecimal.valueOf(parcelas)));
        itens.add(item("Principal", "PARCELAS_VENCIDAS", "Parcelas vencidas do benefício", rendaMensal, BigDecimal.valueOf(parcelas), BigDecimal.ONE, parcelasVencidas, "renda mensal x competências vencidas", effectiveProfile,
                "Total das parcelas vencidas do benefício no período considerado.",
                "A memória multiplica a renda mensal parametrizada pela quantidade de competências vencidas após o recorte temporal e previdenciário adotado.",
                "Manual de Cálculos da Justiça Federal e decisões do caso"));
        trilha.add("parcelas_vencidas=" + parcelasVencidas + " competencias=" + parcelas);

        BigDecimal abonoAnual = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (Boolean.TRUE.equals(request.incluirAbonoAnual()) && parcelas > 0) {
            BigDecimal meses = BigDecimal.valueOf(parcelas);
            abonoAnual = CalculoJudicialMath.money(rendaMensal.multiply(meses).divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP));
            itens.add(item("Principal", "ABONO_ANUAL", "Abono anual projetado sobre atrasados", rendaMensal, meses, new BigDecimal("0.083333"), abonoAnual, "renda mensal x competências / 12", effectiveProfile,
                    "Projeção do abono anual proporcional às competências vencidas.",
                    "Rubrica autônoma para o abono anual previdenciário proporcional ao número de competências vencidas na memória.",
                    "Manual institucional e ato concessório"));
            trilha.add("abono_anual=" + abonoAnual);
        }

        BigDecimal subtotalPrincipal = parcelasVencidas.add(abonoAnual).setScale(2, RoundingMode.HALF_UP);

        BigDecimal fatorCorrecao = positiveOrDefault(request.fatorCorrecaoMonetaria(), somaSeries(request.taxasCorrecaoMensais()));
        BigDecimal correcaoMonetaria = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (fatorCorrecao.signum() > 0 && subtotalPrincipal.signum() > 0) {
            correcaoMonetaria = CalculoJudicialMath.percent(subtotalPrincipal, fatorCorrecao);
            itens.add(item("Atualização", "CORRECAO_MONETARIA", "Correção monetária projetada", subtotalPrincipal, BigDecimal.ONE, fatorCorrecao, correcaoMonetaria, "subtotal principal x fator de correção", effectiveProfile,
                    "Atualização monetária parametrizada sobre as parcelas vencidas e abono anual.",
                    "A memória aceita série mensal de correção ou fator consolidado para refletir a metodologia do manual e do caso concreto.",
                    blankOrDefault(request.criterioAtualizacaoNome(), "Tabela de correção monetária institucional")));
            trilha.add("correcao_monetaria=" + correcaoMonetaria + " fator=" + fatorCorrecao);
        }

        long mesesJuros = request.dataCitacao() != null && !dataCalculo.isBefore(request.dataCitacao())
                ? ChronoUnit.MONTHS.between(request.dataCitacao().withDayOfMonth(1), dataCalculo.withDayOfMonth(1)) + 1
                : parcelas;
        BigDecimal percentualJuros = positiveOrDefault(request.percentualJurosMoraMensal(), new BigDecimal("0.005000"));
        BigDecimal jurosMora = subtotalPrincipal.signum() > 0 && mesesJuros > 0
                ? CalculoJudicialMath.money(subtotalPrincipal.multiply(percentualJuros).multiply(BigDecimal.valueOf(mesesJuros)))
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (jurosMora.signum() > 0) {
            itens.add(item("Atualização", "JUROS_MORA", "Juros de mora projetados", subtotalPrincipal, BigDecimal.valueOf(mesesJuros), percentualJuros, jurosMora, "subtotal principal x juros mensal x competências", effectiveProfile,
                    "Juros de mora parametrizados a partir da citação ou do período de atraso informado.",
                    "Rubrica projetada com base em juros mensais parametrizados e marco temporal da citação ou período equivalente, mantendo a metodologia auditável.",
                    blankOrDefault(request.criterioJurosNome(), "Juros de mora parametrizados")));
            trilha.add("juros_mora=" + jurosMora + " meses=" + mesesJuros + " percentual=" + percentualJuros);
        }

        BigDecimal pagamentoAdmin = CalculoJudicialMath.positive(request.parcelasPagasAdministrativamente());
        BigDecimal pagamentoTutela = CalculoJudicialMath.positive(request.parcelasPagasPorTutela());
        BigDecimal abatimentos = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (pagamentoAdmin.signum() > 0) {
            BigDecimal valor = pagamentoAdmin.negate().setScale(2, RoundingMode.HALF_UP);
            itens.add(item("Acessórios", "ABATIMENTO_ADMINISTRATIVO", "Abatimento por parcelas administrativas pagas", pagamentoAdmin, BigDecimal.ONE, BigDecimal.ONE.negate(), valor, "abatimento do valor informado", effectiveProfile,
                    "Desconto das parcelas já pagas administrativamente.",
                    "A memória segrega abatimentos administrativos para impedir duplicidade no saldo de atrasados.",
                    "Extrato do benefício ou histórico de pagamentos"));
            abatimentos = abatimentos.add(valor);
            trilha.add("abatimento_administrativo=" + pagamentoAdmin);
        }
        if (pagamentoTutela.signum() > 0) {
            BigDecimal valor = pagamentoTutela.negate().setScale(2, RoundingMode.HALF_UP);
            itens.add(item("Acessórios", "ABATIMENTO_TUTELA", "Abatimento por tutela ou implantação provisória", pagamentoTutela, BigDecimal.ONE, BigDecimal.ONE.negate(), valor, "abatimento do valor informado", effectiveProfile,
                    "Desconto das parcelas já pagas por tutela, implantação provisória ou cumprimento parcial.",
                    "A memória mantém o abatimento de tutela em rubrica autônoma para compatibilizar execução, implantação e saldo remanescente.",
                    "Comprovante de implantação ou histórico do benefício"));
            abatimentos = abatimentos.add(valor);
            trilha.add("abatimento_tutela=" + pagamentoTutela);
        }

        BigDecimal baseHonorarios = subtotalPrincipal.add(correcaoMonetaria).add(jurosMora).add(abatimentos).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal honorarios = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (request.percentualHonorarios() != null && request.percentualHonorarios().signum() > 0 && baseHonorarios.signum() > 0) {
            honorarios = CalculoJudicialMath.percent(baseHonorarios, request.percentualHonorarios());
            itens.add(item("Acessórios", "HONORARIOS", "Honorários projetados sobre atrasados", baseHonorarios, BigDecimal.ONE, request.percentualHonorarios(), honorarios, "base elegível x percentual informado", effectiveProfile,
                    "Honorários projetados sobre a base previdenciária parametrizada.",
                    "A memória mantém honorários em rubrica autônoma e parametrizada para seguir a técnica do processo concreto e da fase executiva.",
                    "Decisão judicial, manual institucional ou orientação aplicável"));
            trilha.add("honorarios=" + honorarios + " percentual=" + request.percentualHonorarios());
        }

        BigDecimal subtotalAtualizacao = correcaoMonetaria.add(jurosMora).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotalAcessorios = abatimentos.add(honorarios).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotalPrincipal.add(subtotalAtualizacao).add(subtotalAcessorios).setScale(2, RoundingMode.HALF_UP);

        BigDecimal salarioMinimo = CalculoJudicialMath.positive(request.salarioMinimoReferencia());
        BigDecimal tetoRpvSm = positiveOrDefault(request.tetoRpvEmSalariosMinimos(), new BigDecimal("60"));
        BigDecimal tetoRpvValor = salarioMinimo.signum() > 0 ? CalculoJudicialMath.money(salarioMinimo.multiply(tetoRpvSm)) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String classificacaoPagamento = tetoRpvValor.signum() > 0 ? (total.compareTo(tetoRpvValor) <= 0 ? "RPV" : "PRECATORIO") : "CLASSIFICACAO_PARAMETRIZADA";
        if (tetoRpvValor.signum() > 0) {
            alertas.add("Classificação projetada do pagamento: " + classificacaoPagamento + ".");
        }
        if (request.observacoesTecnicas() != null && !request.observacoesTecnicas().isBlank()) {
            alertas.add("Observação técnica declarada pelo usuário: " + request.observacoesTecnicas().trim());
        }
        if (request.dataCitacao() == null) {
            alertas.add("Data de citação não informada; a projeção de juros foi ancorada no período vencido parametrizado.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("perfilApresentacao", effectiveProfile.name());
        metadata.put("modo", "FEDERAL_PREVIDENCIARIO_CJF_2026");
        metadata.put("classificacaoPagamento", classificacaoPagamento);
        metadata.put("officialReferenceSignals", List.of(
                "Manual de Cálculos da Justiça Federal / CJF",
                "SICOM com tabelas de correção monetária do CJF",
                "Planilhas oficiais dos Juizados Especiais Federais para atrasados previdenciários",
                "Módulos de contadoria e cálculo simplificado em ecossistemas oficiais"
        ));
        metadata.put("criteriosAplicados", List.of(
                CalculoJudicialMetadataSupport.criterion("Tribunal", blankOrDefault(request.tribunal(), "Justiça Federal/JEF")),
                CalculoJudicialMetadataSupport.criterion("Sistema de origem", blankOrDefault(request.sistemaOrigem(), "Manual/planilha institucional")),
                CalculoJudicialMetadataSupport.criterion("Tipo de benefício", blankOrDefault(request.tipoBeneficio(), "Benefício previdenciário")),
                CalculoJudicialMetadataSupport.criterion("Classificação do pagamento", classificacaoPagamento)
        ));
        metadata.put("parameterBlocks", List.of(
                CalculoJudicialMetadataSupport.block("Dados do benefício", CalculoJudicialMetadataSupport.map(
                        "Processo", blankOrDash(request.numeroProcesso()),
                        "Tribunal", blankOrDash(request.tribunal()),
                        "Sistema de origem", blankOrDash(request.sistemaOrigem()),
                        "Tipo de benefício", blankOrDash(request.tipoBeneficio()),
                        "Renda mensal atual", CalculoJudicialMetadataSupport.money(rendaMensal)
                )),
                CalculoJudicialMetadataSupport.block("Marco temporal", CalculoJudicialMetadataSupport.map(
                        "DIB", dib,
                        "DIP", request.dip(),
                        "DCB", request.dcb(),
                        "Ajuizamento", request.dataAjuizamento(),
                        "Citação", request.dataCitacao(),
                        "Data do cálculo", dataCalculo,
                        "Prescrição quinquenal", Boolean.TRUE.equals(request.aplicarPrescricaoQuinquenal()) ? "Sim" : "Não"
                )),
                CalculoJudicialMetadataSupport.block("Atualização e juros", CalculoJudicialMetadataSupport.map(
                        "Critério de atualização", blankOrDefault(request.criterioAtualizacaoNome(), "Tabela institucional"),
                        "Fator de correção", CalculoJudicialMetadataSupport.percent(fatorCorrecao),
                        "Critério de juros", blankOrDefault(request.criterioJurosNome(), "Juros parametrizados"),
                        "Juros mensais", CalculoJudicialMetadataSupport.percent(percentualJuros),
                        "Abono anual", Boolean.TRUE.equals(request.incluirAbonoAnual()) ? "Sim" : "Não"
                )),
                CalculoJudicialMetadataSupport.block("Abatimentos e pagamento", CalculoJudicialMetadataSupport.map(
                        "Pagamentos administrativos", CalculoJudicialMetadataSupport.money(pagamentoAdmin),
                        "Pagamentos por tutela", CalculoJudicialMetadataSupport.money(pagamentoTutela),
                        "Honorários", request.percentualHonorarios() == null ? "0,00%" : CalculoJudicialMetadataSupport.percent(request.percentualHonorarios()),
                        "Salário mínimo de referência", CalculoJudicialMetadataSupport.money(salarioMinimo),
                        "Teto RPV em salários mínimos", tetoRpvSm.toPlainString(),
                        "Teto projetado em moeda", CalculoJudicialMetadataSupport.money(tetoRpvValor)
                ))
        ));
        metadata.put("indexSeries", CalculoJudicialMetadataSupport.indexSeries(request.taxasCorrecaoMensais()));
        metadata.put("operationalHighlights", List.of(
                "A memória separa parcelas vencidas, abono anual, correção, juros, abatimentos, honorários e classificação RPV/precatório em rubricas distintas.",
                "O desenho conversa com o Manual de Cálculos da Justiça Federal, com planilhas de atrasados previdenciários e com rotinas de contadoria/jef observadas em fontes oficiais.",
                "A parametrização evita hardcode rígido de índice, juros, corte prescricional e teto de pagamento, permitindo aderência ao caso concreto e ao órgão julgador."
        ));
        metadata.put("entryGuide", List.of(
                "Preencha DIB, data do cálculo e renda mensal antes de configurar correção, juros e honorários.",
                "Use a data de ajuizamento quando quiser aplicar prescrição quinquenal de forma controlada.",
                "Lance parcelas administrativas e tutela em rubricas separadas para impedir duplicidade na memória.",
                "Informe salário mínimo de referência e teto em salários mínimos se quiser classificar o resultado entre RPV e precatório."
        ));
        metadata.put("uiSections", List.of("Dados do benefício", "Marco temporal", "Parcelas e abono", "Atualização e juros", "Abatimentos", "Classificação do pagamento", "Observações"));
        metadata.putAll(assistenciaService.metadataFederalPrevidenciario(request, effectiveProfile));
        metadata.put("readyNotification", CalculatorHelpMessages.readyNotificationPayload("FEDERAL_PREVIDENCIARIO_CJF", effectiveProfile, total, false));
        metadata.put("readyNotificationIaAssistida", CalculatorHelpMessages.readyNotificationPayload("FEDERAL_PREVIDENCIARIO_CJF", effectiveProfile, total, true));

        return new CalculoJudicialRelatorio(
                "FEDERAL_PREVIDENCIARIO_CJF",
                request.tituloCalculo() == null || request.tituloCalculo().isBlank() ? "Calculadora Federal/JEF Previdenciária PJB 2026" : request.tituloCalculo().trim(),
                request.numeroProcesso(),
                effectiveProfile,
                "A memória federal previdenciária foi organizada por parcelas vencidas, abono anual, atualização, juros, abatimentos e classificação de pagamento.",
                "A memória previdenciária federal consolida atrasados, abono anual, correção, juros, compensações e enquadramento de pagamento em estrutura auditável e parametrizada.",
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

    private BigDecimal somaSeries(List<CalculoIndiceMensalRequest> series) {
        if (series == null || series.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (CalculoIndiceMensalRequest indice : series) {
            if (indice != null && indice.taxaPercentualMensal() != null) {
                total = total.add(indice.taxaPercentualMensal());
            }
        }
        return total.setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        return value == null || value.signum() <= 0 ? fallback : value;
    }

    private CalculoJudicialLinha item(String secao, String codigo, String titulo, BigDecimal base, BigDecimal quantidade, BigDecimal aliquota, BigDecimal valor, String formula, CalculoJudicialSolicitantePerfil perfil, String citizen, String technical, String baseLegal) {
        return new CalculoJudicialLinha(secao, codigo, titulo, base, quantidade, aliquota, valor, formula, perfil.citizenLike() ? citizen : technical, technical, baseLegal);
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
