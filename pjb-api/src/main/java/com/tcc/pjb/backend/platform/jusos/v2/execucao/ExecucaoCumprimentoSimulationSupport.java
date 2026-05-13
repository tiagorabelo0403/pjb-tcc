package com.tcc.pjb.backend.platform.jusos.v2.execucao;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class ExecucaoCumprimentoSimulationSupport {

    ExecucaoCumprimentoEngine.ResultadoPenhora simularPenhora(Processo processo,
                                                              Long processoId,
                                                              ExecucaoCumprimentoEngine.MeioExpropriatorio meio,
                                                              BigDecimal valorAlvo) {
        BigDecimal alvo = scale(valorAlvo != null ? valorAlvo : processo != null ? processo.getValorCausa() : BigDecimal.ZERO);
        List<String> bens = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();
        List<String> provas = new ArrayList<>();
        int score = 48;

        switch (meio) {
            case BACENJUD_BLOQUEIO_CONTA, SISBAJUD_BLOQUEIO_CONTA -> {
                bens.add("Pesquisa de ativos financeiros em contas, aplicações e depósitos");
                advertencias.add("Impenhorabilidade de salários, proventos e verbas protegidas deve ser validada caso a caso");
                provas.add("Extratos, decisão fundamentada e memória do valor executado");
                score += 25;
            }
            case BACENJUD_TRANSFERENCIA, SISBAJUD_TEIMOSINHA -> {
                bens.add("Bloqueio reiterado com teimosinha e transferência judicial de saldo constrito");
                advertencias.add("Execução repetitiva exige calibragem para evitar excesso de bloqueio");
                provas.add("Decisão com janela operacional e logs da ordem eletrônica");
                score += 30;
            }
            case RENAJUD_RESTRICAO_VEICULO -> {
                bens.add("Restrição de circulação, transferência e licenciamento de veículos vinculados ao executado");
                advertencias.add("Veículo essencial ao trabalho pode demandar mitigação da constrição");
                provas.add("Consulta RENAJUD, CRLV e evidências de propriedade útil");
                score += 18;
            }
            case INFOJUD_CONSULTA_BENS -> {
                bens.add("Declarações fiscais, vínculos patrimoniais e lastro de renda declarada");
                advertencias.add("Quebra de sigilo fiscal exige fundamentação e finalidade executiva delimitada");
                provas.add("Despacho fundamentado e delimitação do período de consulta");
                score += 12;
            }
            case CCS_PESQUISA_RELACIONAMENTO -> {
                bens.add("Mapeamento de relacionamento bancário para direcionar ordens patrimoniais futuras");
                advertencias.add("CCS não revela saldo; serve para inteligência executiva complementar");
                provas.add("Despacho autorizativo e correlação com tentativas frustradas anteriores");
                score += 10;
            }
            case SNIPER_PATRIMONIAL -> {
                bens.add("Análise relacional patrimonial e indícios de ocultação de bens");
                advertencias.add("Uso recomenda cadeia de justificativas para privacidade e proporcionalidade");
                provas.add("Histórico de inadimplemento, tentativas frustradas e lastro do crédito");
                score += 22;
            }
            case SERASAJUD_NEGATIVACAO -> {
                bens.add("Negativação judicial para aumento de pressão executiva e indução de pagamento");
                advertencias.add("Medida de pressão não substitui atos de constrição material");
                provas.add("Decisão, memória do débito e comprovação de inadimplemento atual");
                score += 8;
            }
            case CNIB_INDISPONIBILIDADE_IMOVEL, PENHORA_IMOVEL_REGISTRO -> {
                bens.add("Indisponibilidade e averbação registral de imóvel com preparação para avaliação e expropriação");
                advertencias.add("Bem de família, copropriedade e hipotecas anteriores exigem saneamento jurídico prévio");
                provas.add("Matrícula atualizada, certidões dominiais e avaliação preliminar");
                score += 16;
            }
            case PENHORA_QUOTA_SOCIAL -> {
                bens.add("Participação societária ou quotas com potencial de liquidação ou substituição patrimonial");
                advertencias.add("Contrato social e restrições de cessão devem ser verificados antes da constrição");
                provas.add("Contrato social, alterações societárias e balanço recente");
                score += 14;
            }
            case PENHORA_CREDITO_PRECATORIO -> {
                bens.add("Crédito judicial ou precatório passível de reserva/penhora em fluxo controlado");
                advertencias.add("É preciso confirmar exigibilidade, titularidade e cessões prévias do crédito");
                provas.add("Ofício do ente pagador, certidão do precatório e cadeia de titularidade");
                score += 12;
            }
            case PENHORA_FATURAMENTO -> {
                bens.add("Receita operacional recorrente com potencial de constrição parcelada sem inviabilização da atividade");
                advertencias.add("Percentual deve preservar função social da empresa e continuidade mínima do negócio");
                provas.add("Fluxo de caixa, faturamento médio e proposta técnica de percentual exequível");
                score += 20;
            }
            case DESCONTO_FOLHA_ALIMENTOS -> {
                bens.add("Desconto direto em folha para satisfação continuada de obrigação alimentar");
                advertencias.add("Necessário compatibilizar percentual com subsistência mínima do executado");
                provas.add("Fonte pagadora, comprovante de renda e decisão com teto percentual");
                score += 28;
            }
            case PROTESTO_DECISAO_JUDICIAL -> {
                bens.add("Protesto judicial do pronunciamento executivo para pressão reputacional qualificada");
                advertencias.add("Protesto deve recair sobre título certo, líquido e exigível");
                provas.add("Certidão de teor executivo e memória do débito atualizada");
                score += 7;
            }
            case ADJUDICACAO, ALIENACAO_JUDICIAL, ARREMATACAO_HASTA_PUBLICA, EXPROPRIACAO_DIRETA -> {
                bens.add("Conversão de ativo constrito em satisfação direta do crédito");
                advertencias.add("Fase expropriatória exige saneamento prévio de avaliação, intimações e ônus preferenciais");
                provas.add("Auto de penhora, avaliação, edital e certidões de ônus");
                score += 15;
            }
        }

        if (processo != null && processo.getRamoDireito() == RamoDireito.FAMILIA && meio == ExecucaoCumprimentoEngine.MeioExpropriatorio.DESCONTO_FOLHA_ALIMENTOS) {
            score += 12;
        }
        if (processo != null && processo.getRamoDireito() == RamoDireito.PREVIDENCIARIO && meio == ExecucaoCumprimentoEngine.MeioExpropriatorio.PENHORA_CREDITO_PRECATORIO) {
            score += 10;
        }

        BigDecimal estimativa = estimarRecuperacao(meio, alvo, processo);
        boolean suficiente = alvo != null && estimativa.compareTo(alvo) >= 0;

        return new ExecucaoCumprimentoEngine.ResultadoPenhora(
                UUID.randomUUID(),
                meio,
                estimativa,
                suficiente,
                score,
                bens,
                advertencias,
                provas
        );
    }

    ExecucaoCumprimentoEngine.PainelExecucao gerarPainel(Processo processo,
                                                         ExecucaoCumprimentoEngine.PlanoExecucao plano,
                                                         List<ExecucaoCumprimentoEngine.MedidaExpropriatoria> medidas) {
        List<ExecucaoCumprimentoEngine.MedidaExpropriatoria> lista = medidas == null ? List.of() : List.copyOf(medidas);
        int satisfatorias = (int) lista.stream().filter(ExecucaoCumprimentoEngine.MedidaExpropriatoria::satisfatoria).count();
        int pendentes = (int) lista.stream()
                .filter(m -> m.status() == ExecucaoCumprimentoEngine.StatusMedidaExpropriatoria.REQUERIDA
                        || m.status() == ExecucaoCumprimentoEngine.StatusMedidaExpropriatoria.DEFERIDA)
                .count();
        LocalDate vencimentoPrazo = plano.prazoCalculado() != null ? plano.prazoCalculado().vencimento() : null;
        boolean prazoCritico = vencimentoPrazo != null && !vencimentoPrazo.isAfter(LocalDate.now().plusDays(5));

        List<String> gargalos = new ArrayList<>();
        List<String> oportunidades = new ArrayList<>();
        List<String> proximasAcoes = new ArrayList<>(plano.etapas().stream().limit(3).toList());

        if (pendentes > 0) {
            gargalos.add("Há medidas constritivas pendentes de cumprimento ou retorno");
        }
        if (lista.isEmpty()) {
            gargalos.add("Ainda não há medida expropriatória formalizada no painel");
        }
        if (plano.compliance().envolveFazendaPublica()) {
            gargalos.add("Fluxo contra Fazenda Pública exige compatibilização com RPV/Precatório");
        }
        if (plano.matriz().scoreRecuperabilidade() >= 70) {
            oportunidades.add("Recuperabilidade elevada permite ofensiva patrimonial coordenada");
        }
        if (plano.compliance().naturezaAlimentar()) {
            oportunidades.add("Natureza alimentar autoriza trilha prioritária e reforço de urgência");
        }
        if (plano.compliance().recomendaPesquisaPatrimonialAmpliada()) {
            oportunidades.add("Pesquisa patrimonial ampliada pode reduzir tempo de satisfação");
        }
        if (prazoCritico && vencimentoPrazo != null) {
            proximasAcoes.add("Atuar antes do vencimento processual de " + vencimentoPrazo);
        }

        return new ExecucaoCumprimentoEngine.PainelExecucao(
                processo.getId(),
                processo.getNumeroUnificado(),
                plano.valorProjetadoTotal(),
                lista.size(),
                satisfatorias,
                pendentes,
                prazoCritico,
                gargalos,
                oportunidades,
                proximasAcoes
        );
    }

    private BigDecimal estimarRecuperacao(ExecucaoCumprimentoEngine.MeioExpropriatorio meio,
                                          BigDecimal alvo,
                                          Processo processo) {
        BigDecimal fator = switch (meio) {
            case SISBAJUD_TEIMOSINHA -> BigDecimal.valueOf(0.85);
            case SISBAJUD_BLOQUEIO_CONTA, BACENJUD_BLOQUEIO_CONTA, BACENJUD_TRANSFERENCIA -> BigDecimal.valueOf(0.75);
            case DESCONTO_FOLHA_ALIMENTOS -> BigDecimal.valueOf(0.65);
            case PENHORA_FATURAMENTO -> BigDecimal.valueOf(0.55);
            case RENAJUD_RESTRICAO_VEICULO, CNIB_INDISPONIBILIDADE_IMOVEL, PENHORA_IMOVEL_REGISTRO -> BigDecimal.valueOf(0.50);
            case PENHORA_CREDITO_PRECATORIO -> BigDecimal.valueOf(0.62);
            case SNIPER_PATRIMONIAL, CCS_PESQUISA_RELACIONAMENTO, INFOJUD_CONSULTA_BENS -> BigDecimal.valueOf(0.30);
            case SERASAJUD_NEGATIVACAO, PROTESTO_DECISAO_JUDICIAL -> BigDecimal.valueOf(0.18);
            default -> BigDecimal.valueOf(0.42);
        };
        if (processo != null && processo.getRamoDireito() == RamoDireito.FAMILIA && meio == ExecucaoCumprimentoEngine.MeioExpropriatorio.DESCONTO_FOLHA_ALIMENTOS) {
            fator = fator.add(BigDecimal.valueOf(0.10));
        }
        return scale(value(alvo).multiply(fator));
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : scale(value);
    }

    private static List<String> immutableDistinct(List<String> values) {
        return List.copyOf(new LinkedHashSet<>(values == null ? List.of() : values));
    }
}
