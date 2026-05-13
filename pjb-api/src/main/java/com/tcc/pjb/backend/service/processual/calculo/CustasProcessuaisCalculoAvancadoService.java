package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CustasProcessuaisCalculoAvancadoRequest;
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
public class CustasProcessuaisCalculoAvancadoService {

    private final CalculoJudicialAssistenciaService assistenciaService;

    public CustasProcessuaisCalculoAvancadoService(CalculoJudicialAssistenciaService assistenciaService) {
        this.assistenciaService = Objects.requireNonNull(assistenciaService);
    }

    public CalculoJudicialRelatorio calcular(CustasProcessuaisCalculoAvancadoRequest request, CalculoJudicialSolicitantePerfil perfil) {
        Objects.requireNonNull(request);
        CalculoJudicialSolicitantePerfil effectiveProfile = perfil == null ? CalculoJudicialSolicitantePerfil.ADVOGADO : perfil;
        LinkedList<CalculoJudicialLinha> itens = new LinkedList<>();
        List<String> alertas = new ArrayList<>();
        List<String> fundamentos = new ArrayList<>(List.of(
                "Portais oficiais de custas dos tribunais indicam emissão de guias, depósitos judiciais, diligências e planilhas de conferência como núcleo operacional da cobrança processual.",
                "A memória de custas deve separar taxa judiciária, preparo recursal, despesas, diligências, porte e abatimentos por depósito para conferência auditável.",
                "A base normativa concreta varia por tribunal, competência, ato e exercício financeiro, exigindo parametrização em vez de hardcode rígido de tabela local.",
                "Quando houver unidade de referência estadual, a memória deve permitir exibir a unidade e seu valor monetário para facilitar validação da guia e da tabela vigente."
        ));
        List<String> trilha = new ArrayList<>();

        BigDecimal valorCausa = CalculoJudicialMath.positive(request.valorCausa());
        BigDecimal percentualTaxa = positiveOrDefault(request.percentualTaxaJudiciaria(), new BigDecimal("0.015"));
        BigDecimal minimoTaxa = CalculoJudicialMath.positive(request.valorMinimoTaxaJudiciaria());
        BigDecimal percentualPreparo = CalculoJudicialMath.positive(request.percentualPreparoRecursal());
        BigDecimal despesasPostais = CalculoJudicialMath.positive(request.despesasPostais());
        BigDecimal diligencias = CalculoJudicialMath.positive(request.diligenciasOficialJustica());
        BigDecimal editais = CalculoJudicialMath.positive(request.despesasEditais());
        BigDecimal pesquisas = CalculoJudicialMath.positive(request.pesquisasConveniadas());
        BigDecimal porte = CalculoJudicialMath.positive(request.porteRemessaRetorno());
        BigDecimal custasFinais = CalculoJudicialMath.positive(request.custasFinaisComplementares());
        BigDecimal deposito = CalculoJudicialMath.positive(request.depositoJudicialVinculado());
        BigDecimal fatorAtualizacao = CalculoJudicialMath.positive(request.fatorAtualizacaoCustas());
        String unidadeNome = blankOrDefault(request.unidadeReferenciaNome(), "Unidade local");
        BigDecimal valorUnidade = CalculoJudicialMath.positive(request.valorUnidadeReferencia());

        itens.add(item("Principal", "VALOR_CAUSA", "Valor da causa ou base econômica", valorCausa, BigDecimal.ONE, BigDecimal.ONE, valorCausa, "valor da causa informado", effectiveProfile,
                "Base econômica usada para projetar custas e despesas processuais.",
                "A memória parte do valor da causa ou da base econômica informada para derivar taxa judiciária, preparo e demais componentes da conta.",
                "Petição inicial, recurso ou decisão que fixou a base"));

        BigDecimal taxaBruta = CalculoJudicialMath.percent(valorCausa, percentualTaxa);
        BigDecimal taxaJudiciaria = taxaBruta.max(minimoTaxa).setScale(2, RoundingMode.HALF_UP);
        itens.add(item("Principal", "TAXA_JUDICIARIA", "Taxa judiciária projetada", valorCausa, BigDecimal.ONE, percentualTaxa, taxaJudiciaria, "max(valor da causa x percentual, mínimo informado)", effectiveProfile,
                "Taxa judiciária calculada com o percentual informado e respeitando o piso mínimo quando houver.",
                "A memória permite projetar taxa de ingresso, satisfação da execução ou outra rubrica equivalente segundo o percentual parametrizado e o piso normativo aplicável.",
                "Tabela de custas do tribunal e legislação local"));
        trilha.add("taxa_judiciaria=" + taxaJudiciaria + " percentual=" + percentualTaxa + " minimo=" + minimoTaxa);

        BigDecimal preparo = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (percentualPreparo.signum() > 0) {
            preparo = CalculoJudicialMath.percent(valorCausa, percentualPreparo);
            itens.add(item("Acessórios", "PREPARO_RECURSAL", "Preparo recursal projetado", valorCausa, BigDecimal.ONE, percentualPreparo, preparo, "valor da causa x percentual de preparo", effectiveProfile,
                    "Preparo recursal calculado de forma parametrizada para recursos e impugnações com preparo próprio.",
                    "O preparo é mantido em rubrica autônoma para refletir modelos como preparo recursal, custas de apelação, recurso inominado ou ação originária.",
                    "Tabela recursal ou normativo local"));
            trilha.add("preparo_recursal=" + preparo + " percentual=" + percentualPreparo);
        }

        addPositiveItem(itens, "Acessórios", "DESPESAS_POSTAIS", "Despesas postais", despesasPostais, effectiveProfile,
                "Despesas com citações, intimações ou remessa postal.",
                "Rubrica específica para despesas postais, útil para cenários de custas complementares e conferência de guias.",
                "Tabela de despesas processuais", trilha);
        addPositiveItem(itens, "Acessórios", "DILIGENCIAS_OFICIAL", "Diligências de oficial de justiça", diligencias, effectiveProfile,
                "Despesas de diligência lançadas separadamente para facilitar conferência.",
                "Rubrica autônoma para diligências de oficial de justiça, condução e atos correlatos, alinhada ao modelo visto em portais estaduais de custas.",
                "Tabela de diligências do tribunal", trilha);
        addPositiveItem(itens, "Acessórios", "DESPESAS_EDITAIS", "Publicação de editais", editais, effectiveProfile,
                "Despesas com publicações ou editais do processo.",
                "Rubrica parametrizada para editais e publicações oficiais, frequente em memórias de custas e despesas.",
                "Tabela de despesas processuais", trilha);
        addPositiveItem(itens, "Acessórios", "PESQUISAS_CONVENIADAS", "Pesquisas conveniadas e informações eletrônicas", pesquisas, effectiveProfile,
                "Custos de pesquisas eletrônicas lançados de forma isolada.",
                "Rubrica útil para Infojud, Renajud, pesquisas conveniadas e serviços eletrônicos tarifados.",
                "Tabela de despesas processuais", trilha);
        addPositiveItem(itens, "Acessórios", "PORTE_REMESSA_RETORNO", "Porte de remessa e retorno", porte, effectiveProfile,
                "Porte de remessa/retorno ou despesa equivalente, quando aplicável.",
                "Rubrica autônoma para processos físicos, volumes, remessa ou retorno, preservando compatibilidade com tabelas de custas tradicionais.",
                "Tabela de despesas processuais", trilha);
        addPositiveItem(itens, "Acessórios", "CUSTAS_FINAIS", "Custas finais complementares", custasFinais, effectiveProfile,
                "Custas finais, satisfação da execução ou complemento da conta.",
                "A memória separa custas finais da taxa de ingresso para facilitar conferência de cumprimento de sentença e recolhimentos complementares.",
                "Tabela de custas do tribunal", trilha);

        BigDecimal subtotalSemAtualizacao = taxaJudiciaria.add(preparo).add(despesasPostais).add(diligencias).add(editais).add(pesquisas).add(porte).add(custasFinais);

        BigDecimal atualizacao = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (fatorAtualizacao.signum() > 0) {
            atualizacao = CalculoJudicialMath.percent(subtotalSemAtualizacao, fatorAtualizacao);
            itens.add(item("Atualização", "ATUALIZACAO_CUSTAS", "Atualização monetária parametrizada das custas", subtotalSemAtualizacao, BigDecimal.ONE, fatorAtualizacao, atualizacao, "subtotal de custas x fator de atualização", effectiveProfile,
                    "Atualização monetária aplicada ao subtotal de custas e despesas pela taxa informada.",
                    "A memória mantém o fator de atualização aberto para refletir tabela prática, índice local ou cálculo judicial específico do caso.",
                    "Tabela prática ou índice do tribunal"));
            trilha.add("atualizacao_custas=" + atualizacao + " fator=" + fatorAtualizacao);
        }

        BigDecimal abatimentoDeposito = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (deposito.signum() > 0) {
            abatimentoDeposito = deposito.negate().setScale(2, RoundingMode.HALF_UP);
            itens.add(item("Acessórios", "DEPOSITO_VINCULADO", "Depósito judicial vinculado", deposito, BigDecimal.ONE, BigDecimal.ONE.negate(), abatimentoDeposito, "abatimento do valor depositado", effectiveProfile,
                    "Depósito judicial já recolhido que deve ser abatido do saldo final projetado.",
                    "A memória trata o depósito como redutor autônomo para compatibilizar emissão de guia, saldo remanescente e conferência de recolhimentos anteriores.",
                    "Comprovante de depósito judicial"));
            trilha.add("deposito_vinculado=" + deposito);
        }

        if (request.dataBaseCalculo() != null && request.dataFinalCalculo() != null && request.dataFinalCalculo().isBefore(request.dataBaseCalculo())) {
            alertas.add("A data final do cálculo não pode ser anterior à data-base informada.");
        }
        if (request.dataBaseCalculo() != null && request.dataFinalCalculo() != null) {
            long dias = ChronoUnit.DAYS.between(request.dataBaseCalculo(), request.dataFinalCalculo());
            if (dias > 0) {
                alertas.add("A memória considera um intervalo temporal de " + dias + " dias entre a data-base e a data final para conferência da atualização aplicada.");
            }
        }
        if (valorUnidade.signum() > 0) {
            alertas.add("Unidade de referência informada: " + unidadeNome + " = " + CalculoJudicialMetadataSupport.money(valorUnidade) + ".");
        }
        if (request.observacoesTecnicas() != null && !request.observacoesTecnicas().isBlank()) {
            alertas.add("Observação técnica declarada pelo usuário: " + request.observacoesTecnicas().trim());
        }

        BigDecimal subtotalPrincipal = taxaJudiciaria;
        BigDecimal subtotalAtualizacao = atualizacao;
        BigDecimal subtotalAcessorios = preparo.add(despesasPostais).add(diligencias).add(editais).add(pesquisas).add(porte).add(custasFinais).add(abatimentoDeposito).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotalPrincipal.add(subtotalAtualizacao).add(subtotalAcessorios).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("perfilApresentacao", effectiveProfile.name());
        metadata.put("modo", "ADVANCED_COSTS_AND_FEES_MEMORY_2026");
        metadata.put("criteriosAplicados", List.of(
                CalculoJudicialMetadataSupport.criterion("Sistema de origem", blankOrDefault(request.sistemaOrigem(), "Portal de custas/tribunal")),
                CalculoJudicialMetadataSupport.criterion("Tribunal", blankOrDefault(request.tribunal(), "Tribunal não informado")),
                CalculoJudicialMetadataSupport.criterion("Classe processual", blankOrDefault(request.classeProcessual(), "Classe não informada")),
                CalculoJudicialMetadataSupport.criterion("Unidade de referência", valorUnidade.signum() > 0 ? unidadeNome + " = " + CalculoJudicialMetadataSupport.money(valorUnidade) : "Não informada")
        ));
        metadata.put("parameterBlocks", List.of(
                CalculoJudicialMetadataSupport.block("Dados básicos", CalculoJudicialMetadataSupport.map(
                        "Processo", blankOrDash(request.numeroProcesso()),
                        "Tribunal", blankOrDash(request.tribunal()),
                        "Sistema de origem", blankOrDash(request.sistemaOrigem()),
                        "Classe processual", blankOrDash(request.classeProcessual()),
                        "Valor da causa", CalculoJudicialMetadataSupport.money(valorCausa)
                )),
                CalculoJudicialMetadataSupport.block("Taxa e preparo", CalculoJudicialMetadataSupport.map(
                        "Percentual da taxa judiciária", CalculoJudicialMetadataSupport.percent(percentualTaxa),
                        "Mínimo da taxa", CalculoJudicialMetadataSupport.money(minimoTaxa),
                        "Percentual do preparo", CalculoJudicialMetadataSupport.percent(percentualPreparo),
                        "Unidade de referência", valorUnidade.signum() > 0 ? unidadeNome + " = " + CalculoJudicialMetadataSupport.money(valorUnidade) : "Não informada"
                )),
                CalculoJudicialMetadataSupport.block("Despesas e abatimentos", CalculoJudicialMetadataSupport.map(
                        "Despesas postais", CalculoJudicialMetadataSupport.money(despesasPostais),
                        "Diligências", CalculoJudicialMetadataSupport.money(diligencias),
                        "Editais", CalculoJudicialMetadataSupport.money(editais),
                        "Pesquisas conveniadas", CalculoJudicialMetadataSupport.money(pesquisas),
                        "Porte remessa/retorno", CalculoJudicialMetadataSupport.money(porte),
                        "Custas finais", CalculoJudicialMetadataSupport.money(custasFinais),
                        "Depósito vinculado", CalculoJudicialMetadataSupport.money(deposito)
                )),
                CalculoJudicialMetadataSupport.block("Atualização", CalculoJudicialMetadataSupport.map(
                        "Data-base", request.dataBaseCalculo(),
                        "Data final", request.dataFinalCalculo(),
                        "Fator de atualização", CalculoJudicialMetadataSupport.percent(fatorAtualizacao)
                ))
        ));
        metadata.put("operationalHighlights", List.of(
                "A memória separa taxa judiciária, preparo recursal, despesas, diligências, porte, atualização e abatimento por depósito em rubricas próprias.",
                "O desenho foi pensado para dialogar com rotinas de portal de custas, emissão de guias, conferência simples e custas finais vistas em sistemas judiciais estaduais.",
                "A parametrização evita hardcode local rígido e permite refletir mudanças de tabela, unidade de referência e regra de preparo sem reescrever o motor."
        ));
        metadata.put("entryGuide", List.of(
                "Informe o percentual da taxa judiciária e o mínimo legal quando a tabela do tribunal exigir piso monetário ou em unidade de referência.",
                "Use preparo recursal apenas quando o ato processual realmente exigir preparo autônomo além da taxa principal.",
                "Lance diligências, despesas postais, pesquisas conveniadas e porte em rubricas separadas para facilitar a conferência da guia.",
                "Se já houver depósito judicial vinculado, informe o valor para abatimento controlado do saldo projetado."
        ));
        metadata.put("uiSections", List.of("Dados básicos", "Taxa e preparo", "Despesas processuais", "Atualização", "Abatimentos e depósito", "Observações"));
        metadata.putAll(assistenciaService.metadataCustas(request, effectiveProfile));
        metadata.put("readyNotification", CalculatorHelpMessages.readyNotificationPayload("CUSTAS_PROCESSUAIS", effectiveProfile, total, false));
        metadata.put("readyNotificationIaAssistida", CalculatorHelpMessages.readyNotificationPayload("CUSTAS_PROCESSUAIS", effectiveProfile, total, true));

        return new CalculoJudicialRelatorio(
                "CUSTAS_PROCESSUAIS",
                request.tituloCalculo() == null || request.tituloCalculo().isBlank() ? "Calculadora de Custas e Despesas PJB 2026" : request.tituloCalculo().trim(),
                request.numeroProcesso(),
                effectiveProfile,
                "A memória de custas foi organizada por taxa judiciária, preparo, despesas, atualização e abatimentos, com leitura simples e prática.",
                "A memória de custas e despesas separa taxa, preparo, diligências, porte, atualização e depósito judicial em estrutura auditável e parametrizada.",
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

    private void addPositiveItem(List<CalculoJudicialLinha> itens,
                                 String secao,
                                 String codigo,
                                 String titulo,
                                 BigDecimal valor,
                                 CalculoJudicialSolicitantePerfil perfil,
                                 String citizen,
                                 String technical,
                                 String baseLegal,
                                 List<String> trilha) {
        if (valor.signum() <= 0) {
            return;
        }
        itens.add(item(secao, codigo, titulo, valor, BigDecimal.ONE, BigDecimal.ONE, valor, "valor informado", perfil, citizen, technical, baseLegal));
        trilha.add(codigo.toLowerCase() + "=" + valor);
    }

    private CalculoJudicialLinha item(String secao, String codigo, String titulo, BigDecimal base, BigDecimal quantidade, BigDecimal aliquota, BigDecimal valor, String formula, CalculoJudicialSolicitantePerfil perfil, String citizen, String technical, String baseLegal) {
        return new CalculoJudicialLinha(secao, codigo, titulo, base, quantidade, aliquota, valor, formula, perfil.citizenLike() ? citizen : technical, technical, baseLegal);
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        return value == null || value.signum() <= 0 ? fallback : value;
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
