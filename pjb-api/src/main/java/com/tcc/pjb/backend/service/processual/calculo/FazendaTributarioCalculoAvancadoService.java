package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoParcelaLivreRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.FazendaTributarioCalculoAvancadoRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class FazendaTributarioCalculoAvancadoService {

    private final CalculoJudicialAssistenciaService assistenciaService;

    public FazendaTributarioCalculoAvancadoService(CalculoJudicialAssistenciaService assistenciaService) {
        this.assistenciaService = assistenciaService;
    }

    public CalculoJudicialRelatorio calcular(FazendaTributarioCalculoAvancadoRequest request, CalculoJudicialSolicitantePerfil perfil) {
        Objects.requireNonNull(request);
        CalculoJudicialSolicitantePerfil effectiveProfile = perfil == null ? CalculoJudicialSolicitantePerfil.PROCURADORIA : perfil;
        LinkedList<CalculoJudicialLinha> itens = new LinkedList<>();
        List<String> alertas = new ArrayList<>();
        List<String> fundamentos = new ArrayList<>(List.of(
                "Lei 9.430/1996 art. 61 - multa de mora diária e encargos do regime federal, sem prejuízo de parametrização por ente.",
                "Código Tributário Nacional e legislação do ente - atualização, juros e exigibilidade devem observar o título e o regime de cobrança aplicável.",
                "CPC e execução fiscal exigem memória de cálculo rastreável, auditável e aderente ao título executivo.",
                "A série SELIC deve refletir memória oficial do caso, evitando índice presumido quando o regime concreto divergir."
        ));
        List<String> trilha = new ArrayList<>();

        BigDecimal principal = CalculoJudicialMath.positive(request.principal());
        long diasAtraso = CalculoJudicialMath.diasAtraso(request.vencimento(), request.dataCalculo());
        BigDecimal percentualMoraDia = positiveOrDefault(request.percentualMultaMoraDiaria(), new BigDecimal("0.0033"));
        BigDecimal limiteMora = positiveOrDefault(request.limitePercentualMultaMora(), new BigDecimal("0.20"));
        BigDecimal percentualReducaoMulta = CalculoJudicialMath.positive(request.percentualReducaoMulta());
        BigDecimal percentualDescontoPrograma = CalculoJudicialMath.positive(request.percentualDescontoPrograma());
        String criterioCorrecao = blankOrDefault(request.criterioCorrecaoMonetariaNome(), "SELIC mensal parametrizada");
        String criterioJuros = blankOrDefault(request.criterioJurosNome(), "Multa diária + SELIC acumulada parametrizada");

        itens.add(item("Principal", "PRINCIPAL_TRIBUTARIO", "Principal tributário ou fazendário", principal, BigDecimal.ONE, BigDecimal.ONE, principal, "valor principal declarado", effectiveProfile,
                "Valor principal que servirá de base para as demais rubricas da memória.",
                "O principal representa o núcleo econômico da memória tributária, a partir do qual se aplicam multa, juros, encargos, abatimentos e compensações.",
                "Lançamento, CDA, demonstrativo ou memória da parte"));

        BigDecimal percentualMoraEfetivo = percentualMoraDia.multiply(new BigDecimal(diasAtraso)).min(limiteMora).setScale(6, RoundingMode.HALF_UP);
        BigDecimal multaMoraBruta = CalculoJudicialMath.percent(principal, percentualMoraEfetivo);
        BigDecimal reducaoMora = percentualReducaoMulta.signum() > 0 ? CalculoJudicialMath.percent(multaMoraBruta, percentualReducaoMulta) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal multaMora = multaMoraBruta.subtract(reducaoMora).setScale(2, RoundingMode.HALF_UP);
        itens.add(item("Acessórios", "MULTA_MORA", "Multa de mora", principal, new BigDecimal(diasAtraso), percentualMoraEfetivo, multaMoraBruta, "principal x min(dias de atraso x taxa diária, teto)", effectiveProfile,
                "Penalidade pelo atraso no pagamento, limitada pelo teto informado para o regime aplicável.",
                "Para padrão federal, usa-se 0,33% ao dia com teto de 20%; o sistema mantém o parâmetro editável para refletir o ente e a espécie tributária.",
                "Lei 9.430/1996 art. 61"));
        if (reducaoMora.signum() > 0) {
            itens.add(item("Acessórios", "REDUCAO_MULTA_MORA", "Redução de multa de mora", multaMoraBruta, BigDecimal.ONE, percentualReducaoMulta.negate(), reducaoMora.negate(), "multa de mora x redução programada", effectiveProfile,
                    "Redução parametrizada da multa de mora para refletir programa legal, decisão administrativa ou regra do ente.",
                    "A memória trata a redução como abatimento autônomo, preservando o valor bruto da multa e a parcela efetivamente remanescente para auditoria.",
                    "Lei local, programa de regularização ou ato normativo aplicável"));
        }
        trilha.add("multa_mora=" + multaMora + " diasAtraso=" + diasAtraso + " percentualEfetivo=" + percentualMoraEfetivo);

        BigDecimal multaOficio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (request.percentualMultaOficio() != null && request.percentualMultaOficio().signum() > 0) {
            BigDecimal multaOficioBruta = CalculoJudicialMath.percent(principal, request.percentualMultaOficio());
            BigDecimal reducaoOficio = percentualReducaoMulta.signum() > 0 ? CalculoJudicialMath.percent(multaOficioBruta, percentualReducaoMulta) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            multaOficio = multaOficioBruta.subtract(reducaoOficio).setScale(2, RoundingMode.HALF_UP);
            itens.add(item("Acessórios", "MULTA_OFICIO", "Multa de ofício", principal, BigDecimal.ONE, request.percentualMultaOficio(), multaOficioBruta, "principal x percentual informado", effectiveProfile,
                    "Multa aplicada conforme o cenário de autuação ou lançamento indicado pelo usuário.",
                    "Parcela acessória parametrizada para adequar regimes de multa qualificada, reduzida ou ordinária, sem impor default material inadequado.",
                    "Legislação do tributo e ato de lançamento aplicável"));
            if (reducaoOficio.signum() > 0) {
                itens.add(item("Acessórios", "REDUCAO_MULTA_OFICIO", "Redução de multa de ofício", multaOficioBruta, BigDecimal.ONE, percentualReducaoMulta.negate(), reducaoOficio.negate(), "multa de ofício x redução programada", effectiveProfile,
                        "Redução parametrizada da multa de ofício para refletir benefício legal ou cenário de adesão.",
                        "A memória mantém a multa de ofício bruta e o desconto correspondente em rubricas próprias para facilitar revisão administrativa e judicial.",
                        "Programa de regularização ou legislação do ente"));
            }
            trilha.add("multa_oficio=" + multaOficio + " percentual=" + request.percentualMultaOficio());
        }

        BigDecimal baseSelic = principal.add(multaMora).add(multaOficio);
        BigDecimal fatorSelic = CalculoJudicialMath.fatorAcumuladoMensal(request.taxasSelicMensais());
        BigDecimal selic = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (fatorSelic.signum() > 0) {
            selic = CalculoJudicialMath.percent(baseSelic, fatorSelic);
            itens.add(item("Atualização", "SELIC_ACUMULADA", "Juros/correção SELIC acumulada", baseSelic, BigDecimal.ONE, fatorSelic, selic, "base tributária x fator SELIC acumulado", effectiveProfile,
                    "Atualização monetária e juros calculados pela série SELIC mensal informada na memória.",
                    "Fator acumulado a partir da série mensal parametrizada, mantendo auditabilidade por competência e evitando índice presumido fora do caso.",
                    "Lei 9.430/1996 e regimes federais de SELIC"));
            trilha.add("selic_acumulada=" + selic + " fator=" + fatorSelic);
        } else {
            alertas.add("Sem série SELIC mensal informada, a memória foi emitida sem juros federais acumulados automáticos.");
        }

        BigDecimal adicionalMesPagamento = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (Boolean.TRUE.equals(request.aplicarMaisUmPorCentoNoMesPagamento())) {
            adicionalMesPagamento = CalculoJudicialMath.percent(baseSelic.add(selic), new BigDecimal("0.01"));
            itens.add(item("Atualização", "ADICIONAL_MES_PAGAMENTO", "Adicional de 1% no mês do pagamento", baseSelic.add(selic), BigDecimal.ONE, new BigDecimal("0.01"), adicionalMesPagamento, "base atualizada x 1%", effectiveProfile,
                    "Acréscimo de 1% do mês do pagamento, adotado em regimes federais típicos da Fazenda Nacional.",
                    "Mantido como chave configurável, pois nem todo ente ou espécie tributária replica exatamente o regime federal de 1% no mês do pagamento.",
                    "Legislação tributária federal aplicável ao regime informado"));
            trilha.add("adicional_mes_pagamento=" + adicionalMesPagamento);
        }

        BigDecimal encargoLegal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (request.percentualEncargoLegal() != null && request.percentualEncargoLegal().signum() > 0) {
            encargoLegal = CalculoJudicialMath.percent(principal, request.percentualEncargoLegal());
            itens.add(item("Acessórios", "ENCARGO_LEGAL", "Encargo legal", principal, BigDecimal.ONE, request.percentualEncargoLegal(), encargoLegal, "principal x percentual de encargo", effectiveProfile,
                    "Encargo legal parametrizado para cenários de inscrição em dívida ativa ou regime específico do ente.",
                    "Parcela acessória autônoma e configurável, sem hardcode fixo para evitar erro entre regimes federal, estadual e municipal.",
                    "Lei do ente e regime de cobrança aplicável"));
            trilha.add("encargo_legal=" + encargoLegal);
        }

        BigDecimal descontosPrograma = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (percentualDescontoPrograma.signum() > 0) {
            BigDecimal baseDesconto = multaMora.add(multaOficio).add(encargoLegal);
            descontosPrograma = CalculoJudicialMath.percent(baseDesconto, percentualDescontoPrograma);
            itens.add(item("Acessórios", "DESCONTO_PROGRAMA", "Desconto de programa de regularização", baseDesconto, BigDecimal.ONE, percentualDescontoPrograma.negate(), descontosPrograma.negate(), "base elegível x desconto informado", effectiveProfile,
                    "Desconto ou remissão parcial parametrizada para programa especial, parcelamento ou transação tributária.",
                    "A memória mantém desconto em rubrica redutora autônoma para explicitar o benefício aplicado sobre a base elegível informada.",
                    "Programa de transação, parcelamento ou legislação específica"));
            trilha.add("desconto_programa=" + descontosPrograma);
        }

        BigDecimal honorarios = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (request.percentualHonorarios() != null && request.percentualHonorarios().signum() > 0) {
            BigDecimal baseHonorarios = principal.add(multaMora).add(multaOficio).add(selic).add(adicionalMesPagamento).add(encargoLegal).subtract(descontosPrograma);
            honorarios = CalculoJudicialMath.percent(baseHonorarios.max(BigDecimal.ZERO), request.percentualHonorarios());
            itens.add(item("Acessórios", "HONORARIOS", "Honorários sobre base tributária", baseHonorarios, BigDecimal.ONE, request.percentualHonorarios(), honorarios, "base tributária consolidada x percentual", effectiveProfile,
                    "Honorários calculados sobre a base consolidada conforme o percentual indicado pelo usuário.",
                    "Verba calculada de forma transparente sobre base consolidada, sem impor default de sucumbência ou encargo profissional não informado.",
                    "CPC e título executivo aplicável"));
            trilha.add("honorarios=" + honorarios + " percentual=" + request.percentualHonorarios());
        }

        BigDecimal custas = CalculoJudicialMath.positive(request.custas());
        if (custas.signum() > 0) {
            itens.add(item("Acessórios", "CUSTAS", "Custas e despesas", custas, BigDecimal.ONE, BigDecimal.ONE, custas, "valor informado", effectiveProfile,
                    "Custas e despesas acrescidas conforme a memória do caso.",
                    "Rubrica livre para despesas processuais e custas de cobrança ou embargos.",
                    "Tabela de custas e demonstrativo processual"));
            trilha.add("custas=" + custas);
        }

        BigDecimal compensacoes = aplicarCreditosCompensaveis(itens, request.creditosCompensaveis(), effectiveProfile, trilha);
        BigDecimal depositoGarantia = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (request.valorGarantidoOuDepositado() != null && request.valorGarantidoOuDepositado().signum() > 0) {
            depositoGarantia = CalculoJudicialMath.money(request.valorGarantidoOuDepositado());
            itens.add(item("Acessórios", "GARANTIA_OU_DEPOSITO", "Depósito/garantia vinculada", depositoGarantia, BigDecimal.ONE, BigDecimal.ONE.negate(), depositoGarantia.negate(), "abatimento informado pela garantia vinculada", effectiveProfile,
                    "Valor garantido ou depositado que deve ser abatido do saldo consolidado da memória.",
                    "A memória trata o depósito/garantia como parcela redutora autônoma, útil para execução fiscal, embargos, parcelamentos e levantamento de saldo remanescente.",
                    "Comprovante de depósito, seguro garantia ou penhora"));
            trilha.add("deposito_garantia=" + depositoGarantia);
        }

        BigDecimal subtotalPrincipal = principal;
        BigDecimal subtotalAtualizacao = selic.add(adicionalMesPagamento);
        BigDecimal subtotalAcessorios = multaMora.add(multaOficio).add(encargoLegal).add(honorarios).add(custas)
                .subtract(compensacoes)
                .subtract(depositoGarantia)
                .subtract(descontosPrograma)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotalPrincipal.add(subtotalAtualizacao).add(subtotalAcessorios).setScale(2, RoundingMode.HALF_UP);

        if (request.dataInicioJurosMora() != null && request.dataInicioJurosMora().isAfter(request.dataCalculo())) {
            alertas.add("A data inicial de juros informada é posterior à data-base do cálculo; a memória preservou os totais sem deslocamento automático de marco temporal.");
        }
        if (request.observacoesTecnicas() != null && !request.observacoesTecnicas().isBlank()) {
            alertas.add("Observação técnica declarada pelo usuário: " + request.observacoesTecnicas().trim());
        }
        if (Boolean.TRUE.equals(request.aplicarProRataDie())) {
            alertas.add("A opção pró-rata foi sinalizada; a presente memória mantém a lógica mensal parametrizada e destaca a necessidade de conferência do regime específico do ente.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("diasAtraso", diasAtraso);
        metadata.put("taxaMoraDiaria", percentualMoraDia);
        metadata.put("tetoMora", limiteMora);
        metadata.put("fatorSelic", fatorSelic);
        metadata.put("perfilApresentacao", effectiveProfile.name());
        metadata.put("modo", "ADVANCED_TAX_TREASURY_MEMORY_2026");
        metadata.put("criteriosAplicados", List.of(
                CalculoJudicialMetadataSupport.criterion("Correção monetária", criterioCorrecao),
                CalculoJudicialMetadataSupport.criterion("Juros e mora", criterioJuros),
                CalculoJudicialMetadataSupport.criterion("Regime pró-rata", Boolean.TRUE.equals(request.aplicarProRataDie()) ? "Sinalizado para conferência" : "Não sinalizado"),
                CalculoJudicialMetadataSupport.criterion("Perfil de apresentação", effectiveProfile.name())
        ));
        metadata.put("parameterBlocks", List.of(
                CalculoJudicialMetadataSupport.block("Dados do processo e obrigação", CalculoJudicialMetadataSupport.map(
                        "Processo", blankOrDash(request.numeroProcesso()),
                        "Ente tributante", blankOrDash(request.enteTributante()),
                        "Tributo/obrigação", blankOrDash(request.tributo()),
                        "Principal", CalculoJudicialMetadataSupport.money(principal)
                )),
                CalculoJudicialMetadataSupport.block("Marcos temporais", CalculoJudicialMetadataSupport.map(
                        "Vencimento", request.vencimento(),
                        "Data do cálculo", request.dataCalculo(),
                        "Data inicial dos juros", request.dataInicioJurosMora(),
                        "Dias de atraso", diasAtraso
                )),
                CalculoJudicialMetadataSupport.block("Correção monetária e juros", CalculoJudicialMetadataSupport.map(
                        "Critério de correção", criterioCorrecao,
                        "Critério de juros", criterioJuros,
                        "Taxa diária de mora", CalculoJudicialMetadataSupport.percent(percentualMoraDia),
                        "Teto da multa de mora", CalculoJudicialMetadataSupport.percent(limiteMora),
                        "Adicional 1% no mês do pagamento", Boolean.TRUE.equals(request.aplicarMaisUmPorCentoNoMesPagamento())
                )),
                CalculoJudicialMetadataSupport.block("Descontos, garantias e encargos", CalculoJudicialMetadataSupport.map(
                        "Redução de multa", CalculoJudicialMetadataSupport.percent(percentualReducaoMulta),
                        "Desconto de programa", CalculoJudicialMetadataSupport.percent(percentualDescontoPrograma),
                        "Encargo legal", CalculoJudicialMetadataSupport.percent(request.percentualEncargoLegal()),
                        "Honorários", CalculoJudicialMetadataSupport.percent(request.percentualHonorarios()),
                        "Depósito/garantia", CalculoJudicialMetadataSupport.money(request.valorGarantidoOuDepositado())
                ))
        ));
        metadata.put("indexSeries", CalculoJudicialMetadataSupport.indexSeries(request.taxasSelicMensais()));
        metadata.put("operationalHighlights", List.of(
                "A memória separa principal, mora, atualização, desconto programado, garantia e compensações para leitura contenciosa e consensual.",
                "O desenho serve tanto para execução fiscal quanto para memória administrativa ou pré-contenciosa parametrizada.",
                "Reduções, programas de transação e depósitos ficam em rubricas autônomas para facilitar auditoria e revisão judicial."
        ));
        metadata.put("entryGuide", List.of(
                "Informe a série SELIC oficial do caso quando a memória exigir atualização acumulada por competência.",
                "Use redução de multa e desconto de programa apenas quando houver base normativa ou adesão formal comprovada.",
                "Se houver depósito integral, seguro garantia ou penhora, informe o valor para abatimento controlado do saldo final."
        ));
        metadata.put("uiSections", List.of("Dados do processo", "Correção monetária", "Juros moratórios", "Multas e descontos", "Encargos e honorários", "Compensações e garantias"));
        metadata.putAll(assistenciaService.metadataFazenda(request, effectiveProfile));
        metadata.put("readyNotification", CalculatorHelpMessages.readyNotificationPayload("FAZENDA_TRIBUTARIO", effectiveProfile, total, false));
        metadata.put("readyNotificationIaAssistida", CalculatorHelpMessages.readyNotificationPayload("FAZENDA_TRIBUTARIO", effectiveProfile, total, true));

        return new CalculoJudicialRelatorio(
                "FAZENDA_TRIBUTARIO",
                request.tituloCalculo() == null || request.tituloCalculo().isBlank() ? "Calculadora Fazenda/Tributário PJB 2026" : request.tituloCalculo().trim(),
                request.numeroProcesso(),
                effectiveProfile,
                "A memória tributária foi organizada em principal, atualização, multas, descontos, garantias, encargos, honorários e compensações, com leitura simples para o solicitante.",
                "A memória fazendária preserva regime configurável de multa diária, série SELIC, adicional do mês do pagamento, encargo legal, honorários, programas de desconto, garantias e créditos compensáveis.",
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
        return new CalculoJudicialLinha(secao, codigo, titulo, base, quantidade, aliquota, valor, formula, perfil.citizenLike() ? citizen : technical, technical, baseLegal);
    }

    private BigDecimal aplicarCreditosCompensaveis(List<CalculoJudicialLinha> itens,
                                                   List<CalculoParcelaLivreRequest> creditos,
                                                   CalculoJudicialSolicitantePerfil perfil,
                                                   List<String> trilha) {
        BigDecimal totalCompensado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (creditos == null || creditos.isEmpty()) {
            return totalCompensado;
        }
        for (CalculoParcelaLivreRequest credito : creditos) {
            if (credito == null || credito.valor() == null || credito.valor().signum() == 0) {
                continue;
            }
            BigDecimal valorNegativo = CalculoJudicialMath.money(credito.valor()).negate();
            itens.add(item("Acessórios", "COMPENSACAO_" + CalculoJudicialMath.normalizeCode(credito.codigo()), "Compensação / abatimento - " + credito.descricao(), credito.valor(), BigDecimal.ONE, BigDecimal.ONE.negate(), valorNegativo, "abatimento informado pelo solicitante", perfil,
                    "Crédito compensável abatido do saldo final da memória.",
                    "Parcela redutora mantida com sinal negativo para preservar rastreabilidade de compensação, abatimento ou pagamento parcial já demonstrado.",
                    credito.baseLegal()));
            totalCompensado = totalCompensado.add(credito.valor());
            trilha.add("compensacao=" + credito.codigo() + " valor=" + credito.valor());
        }
        return totalCompensado.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal defaultValue) {
        return value != null && value.signum() > 0 ? value : defaultValue;
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
