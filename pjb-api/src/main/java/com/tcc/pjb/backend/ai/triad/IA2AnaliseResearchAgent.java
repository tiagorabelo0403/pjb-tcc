package com.tcc.pjb.backend.ai.triad;

import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.CognitiveAgent;
import com.tcc.pjb.backend.ai.core.enums.CognitiveStage;
import com.tcc.pjb.backend.ai.core.model.CognitiveContext;
import com.tcc.pjb.backend.model.entity.Processo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component

public class IA2AnaliseResearchAgent implements CognitiveAgent {

    
    

    public static final List<String> IDEIAS = List.of(
            "Construir mapa normativo por ramo: CPC/CPP/CLT/Lei 9.099/Lei 10.259/LEF/LMS/LAJ/LGPD.",
            "Montar trilha de pesquisa: Constituição → lei → decreto/regulamento → jurisprudência → enunciados.",
            "Separar jurisprudência por corte (STF, STJ, TST, TSE, STM) e por tema.",
            "Aplicar 'foco no rito': o que muda no prazo, recursos, competência e provas.",
            "Gerar hipóteses alternativas e critérios objetivos para escolher (sem decidir por você).",
            "Identificar precedentes qualificados (repetitivos, repercussão geral) quando aplicável.",
            "Gerar quadro 'ônus da prova' e riscos processuais por tese.",
            "Gerar lista de documentos essenciais para cada tese/rito (contrato, laudos, extratos, CNIS).",
            "Analisar se há prescrição/decadência (marcos) e como checar datas sem chutar.",
            "Mapear prazos principais por fase e rito (ex.: contestação, réplica, recursos).",
            "Identificar incidentes processuais comuns (impugnação, exceções, embargos).",
            "Em previdenciário: montar trilha CNIS, carência, qualidade de segurado, cálculo RMI.",
            "Em tributário: mapear CDA, nulidades, prescrição intercorrente, garantia do juízo.",
            "Em trabalhista: mapear verbas, prescrição bienal/quinquenal, ônus e provas.",
            "Em penal: mapear cautelares, cadeia de custódia, nulidades e direitos fundamentais.",
            "Em família: mapear guarda, alimentos, visitas, medidas protetivas, sigilo.",
            "Em consumidor: mapear inversão do ônus, danos, prova mínima e competência.",
            "Gerar 'perguntas de auditoria' para checar consistência do andamento com o que foi informado.",
            "Se consulta de movimentação: traduzir movimentações para linguagem clara e orientar próximos passos.",
            "Montar resumo executivo para juiz/servidor: pontos controvertidos, prova, próximos atos.",
            "Montar resumo para cidadão: o que aconteceu, o que vai acontecer, o que fazer agora.",
            "Montar resumo para advogado: status, janelas de prazo, estratégia documental.",
            "Incluir alertas de risco (nulidade, sigilo, competência, prazo) com justificativa objetiva.",
            "Gerar rascunho de 'minuta técnica' (sem decidir) com campos preenchíveis.",
            "Gerar checklist de conformidade LGPD para documentos anexados.",
            "Sugerir classificação de prova (força probatória) e lacunas a suprir.",
            "Usar jurimetria interna (se existir) para apontar padrões sem prometer resultado.",
            "Indicar quando é indispensável atuação humana (ex.: decisão judicial, prova complexa).",
            "Preparar 'plano de verificação em fontes oficiais' para IA3.",
            "Gerar mapa de órgãos/competências (vara, turma, tribunal) para orientar consulta.",
            "Se houver óbito: mapear efeitos processuais (suspensão, habilitação, substituição).",
            "Se houver incapaz: mapear intervenção obrigatória do MP e cautelas.",
            "Se houver tutela/urgência: mapear requisitos (probabilidade/perigo) e provas típicas.",
            "Se execução: mapear meios (penhora, bloqueios, embargos) e ordem legal.",
            "Se recurso: mapear requisitos formais, preparo e efeitos.",
            "Se audiência: mapear pauta, intimação, testemunhas, prazo de rol.",
            "Gerar orientação de linguagem e estrutura de petições para evitar nulidades formais.",
            "Gerar tabela de prazos por rito com base em enums do sistema (fase/rito).",
            "Montar 'roteiro de diligência' para oficial de justiça (endereço, janela, confirmação).",
            "Montar 'roteiro de investigação' para delegado (documentos mínimos, tipificação preliminar).",
            "Montar 'roteiro de parecer' para MP (interesse público, tutela coletiva, incapazes).",
            "Preparar 'roteiro de despacho' para juiz (sem decidir mérito, apenas gestão).",
            "Analisar integridade do processo: campos faltando, status incoerente, dados divergentes.",
            "Criar metadados para indexação (Elastic) e correlação por partes/assuntos.",
            "Preparar 'perguntas de follow-up' prioritárias (top 5) para completar contexto.",
            "Consolidar fundamentos em estrutura: norma → fato → subsunção → consequência.",
            "Gerar lista de 'hipóteses refutáveis' e quais provas derrubam cada uma.",
            "Checar termos técnicos e padronizar nomenclatura (semântica jurídica).",
            "Incluir no output rastro auditável: o que foi assumido, o que foi lido do sistema, o que foi sugerido.",
            "Gerar 'plano de pesquisa' citável e separar o que precisa de fonte oficial.",
            "Preparar instruções de busca em diários/portais sem expor dados sensíveis.",
            "Preparar 'modos de falha' (o que pode dar errado) e mitigação.",
            "Identificar se cabe mediação/conciliação e quais cláusulas mínimas de acordo.",
            "Se acordo: mapear riscos de execução e blindagens (multa, vencimento antecipado, garantias).",
            "Preparar validações financeiras (base, parcelas, multa, correção) para engine financeira.",
            "Consolidar tudo em um pacote único para IA3 (com plano de validação em fontes)."
    );

    @Override
    public CognitiveStage stage() {
        return CognitiveStage.ANALISE;
    }

    @Override
    public void process(CognitiveContext context) {

        String intent = String.valueOf(context.memory().getOrDefault("intent", "GERAL"));

        
        context.addFundamento("Estrutura de pesquisa: Constituição → lei aplicável (rito) → jurisprudência qualificada → prática do tribunal → prova/documentos.");

        
        context.memory().put("guidelines_pesquisa", IDEIAS);

        if ("CONSULTA_MOVIMENTACAO".equals(intent)) {
            analisarMovimentacao(context);
        } else {
            
            context.addFundamento("O assistente organiza informações e aponta próximos passos, mas não substitui ato profissional nem decisão judicial.");
        }

        
        context.memory().put("plano_validacao_oficial", List.of(
                "Consultar portais oficiais do tribunal competente (consulta pública/andamentos).",
                "Conferir intimações no diário oficial / painel eletrônico do sistema do tribunal.",
                "Para legislação: consultar Planalto (legislação federal) e portais oficiais de tribunais superiores.",
                "Para jurisprudência: consultar bases oficiais (STF/STJ/TST/TSE/STM) e repositórios do CNJ."
        ));
    }

    private void analisarMovimentacao(CognitiveContext context) {
        Object obj = context.memory().get("processo_obj");

        if (obj instanceof Processo p) {
            String numero = safe(p.getNumeroUnificado());
            String status = p.getStatusProcesso() == null ? "(sem status)" : p.getStatusProcesso().name();
            String fase = p.getFaseAtual() == null ? "(sem fase)" : p.getFaseAtual().name();

            
            
            
            String rito = "(sem rito definido)"; 

            String ultimaMov = p.getDataUltimaMovimentacao() == null
                    ? "(sem data registrada)"
                    : p.getDataUltimaMovimentacao().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            context.memory().put("movimentacao_resumo", Map.of(
                    "numero", numero,
                    "status", status,
                    "fase", fase,
                    "rito", rito,
                    "ultimaMovimentacao", ultimaMov
            ));

            context.addFundamento("Consulta interna: o processo foi localizado no banco local do PJB (não é consulta ao portal externo do tribunal).");
            context.addFundamento("Próximo passo: caso o usuário precise confirmar intimação/prazo, deve validar no portal oficial/diário do tribunal.");
        } else {
            
            context.alertas().add("Processo não localizado no banco local. Verifique o número (CNJ) e tente novamente, ou habilite integração com o sistema do tribunal correspondente (futuro).");
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}