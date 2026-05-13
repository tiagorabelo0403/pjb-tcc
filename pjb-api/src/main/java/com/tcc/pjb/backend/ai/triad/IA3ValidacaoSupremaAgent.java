package com.tcc.pjb.backend.ai.triad;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.CognitiveAgent;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.core.enums.CognitiveStage;
import com.tcc.pjb.backend.ai.core.model.CognitiveContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IA3ValidacaoSupremaAgent implements CognitiveAgent {

    @SuppressWarnings("unused") 
    public static final List<String> IDEIAS = List.of(
            "Validar consistência final: separar o que é 'dado do sistema' vs 'inferência'.",
            "Gerar resposta final em linguagem compatível com o solicitante (cidadão/advogado/servidor).",
            "Incluir trilha de transparência: por que o sistema chegou naquele resumo.",
            "Aplicar política de segurança: não criar decisões, não prometer resultado, não inventar jurisprudência.",
            "Produzir lista de fontes oficiais recomendadas (CNJ, tribunais, Planalto, STF, STJ, TST, TSE, INSS, Receita).",
            "Se a ação é consulta de movimentação: retornar status, última movimentação e próximos marcos possíveis.",
            "Se a ação é prazo: retornar cálculo estimado + alertar necessidade de conferir intimação oficial.",
            "Se a ação é documento: orientar checklist e organização em dossiê digital.",
            "Se a ação é acordo: gerar minuta estruturada com campos e cláusulas mínimas, sem impor mérito.",
            "Se houver sigilo: recomendar nível e justificar (LGPD, menores, saúde etc.).",
            "Se houver óbito: aplicar protocolo (suspensão, habilitação, trava financeira) e explicar com clareza.",
            "Gerar 'resumo auditável' com metadados para armazenamento no processo (sem dados sensíveis).",
            "Gerar versão curta (1 parágrafo) e versão detalhada (com seções) no mesmo retorno.",
            "Incluir alertas críticos como itens separados, com prioridade e ação recomendada.",
            "Incluir um 'plano de conferência' (passos para o usuário validar no portal oficial).",
            "Se há divergências nos dados: apontar divergência e pedir conferência objetiva.",
            "Se há lacuna de prova: apontar lacuna e quais documentos supririam.",
            "Normalizar linguagem: termos padronizados, sem jargão inútil, mas tecnicamente correta.",
            "Se necessário, direcionar para atendimento humano (cartório/defensoria/advogado) com motivo.",
            "Gerar rastro de auditoria: traceId, estágio, módulos acionados, flags aplicadas.",
            "Incluir recomendações de segurança da conta GOV e autenticação (sem coletar credenciais).",
            "Evitar exposição de dados pessoais na resposta: mascarar CPF/telefone automaticamente.",
            "Gerar saída estruturada para front-end: campos (status, movimentacao, prazos, alertas).",
            "Gerar explicação do rito aplicável e por que ele é relevante para a resposta.",
            "Checar se o pedido é impossível sem consulta externa e explicar limites com precisão.",
            "Preparar resposta pronta para ser enviada por secretaria ao cliente final (linguagem clara).",
            "Preparar resposta pronta para juntada interna (linguagem técnica, objetiva).",
            "Se processo inexistente: sugerir correção do número e caminhos de busca.",
            "Se processo encontrado mas sem movimentações: explicar e orientar atualização/cadastro.",
            "Incluir recomendação de notificação futura (quando sistema permitir) para novas movimentações.",
            "Se houver risco de fraude: recomendar travas (óbito, suspeita de dados, sigilo).",
            "Gerar sugestão de próximos atos processuais prováveis (sem afirmar como certo).",
            "Gerar checklist de compliance (assinatura digital, poderes, procuração, cadeia documental).",
            "Gerar orientação de assinatura eletrônica (e-CPF/e-CNPJ/GOV) sem instruir bypass.",
            "Se houver valores: retornar estimativa com nota de cautela e base do cálculo.",
            "Incluir notas de 'auditoria de cálculo' (base, componentes, fórmula) quando financeiro usado.",
            "Se houver recurso: explicar requisitos mínimos e prazos típicos (com ressalva de conferência).",
            "Se audiência: explicar confirmação, preparo de prova oral, rol de testemunhas e prazo.",
            "Se diligência: explicar acompanhamento e canais de confirmação.",
            "Se delegacia/BO: orientar dados mínimos e anexos, e o que esperar.",
            "Se MP: orientar fluxo institucional (sem expor estratégia sigilosa).",
            "Se juiz/servidor: destacar fila, priorização, e sugestões de gestão processual.",
            "Gerar “FAQ do caso” baseado no pedido para reduzir retrabalho.",
            "Aplicar validação cruzada de campos: rito x classe x fase x status.",
            "Gerar ‘to-do list’ priorizada (top 5) para o solicitante.",
            "Evitar estrangeirismos desnecessários: “insights” → “apontamentos”/“indícios”.",
            "Explicar com exemplos práticos (curtos) quando o solicitante for cidadão.",
            "Explicar com estrutura técnica quando o solicitante for advogado.",
            "Gerar anexos textuais: minuta, checklist, resumo (se solicitado) em seções distintas.",
            "Se houver contradição legal: apontar e recomendar consulta humana.",
            "Conferir se há elementos de segredo de justiça e sugerir fluxo de tarja/versão pública.",
            "Gerar recomendação de indexação (etiquetas) para busca interna futura (Elastic).",
            "Fechar com nota de responsabilidade: conferência em fontes oficiais e limites do assistente.",
            "Encapsular resultado em JSON opcional para API (sem quebrar compatibilidade).",
            "Registrar no log de auditoria a entrega final (sem guardar dados sensíveis).",
            "Incluir 'fontes recomendadas' como lista (sem links diretos se não configurado).",
            "Fornecer a resposta final como '3 camadas': leiga, técnica, e auditoria/metadados."
    );

    @Override
    public CognitiveStage stage() {
        return CognitiveStage.VALIDACAO_SUPREMA;
    }

    @Override
    public void process(CognitiveContext context) {

        
        Object traceObj = context.memory().get("traceId");
        String traceId = traceObj != null ? String.valueOf(traceObj) : UUID.randomUUID().toString();

        Object intentObj = context.memory().get("intent");
        String intent = intentObj != null ? String.valueOf(intentObj) : "GERAL";

        StringBuilder sb = new StringBuilder();
        sb.append("PJB | Assistente Jurídico (Tríade IA)\n");
        sb.append("TraceId: ").append(traceId).append("\n\n");

        if ("CONSULTA_MOVIMENTACAO".equals(intent)) {

            Object resumo = context.memory().get("movimentacao_resumo");
            if (resumo instanceof Map<?, ?> m) {

                sb.append("Andamento (base interna do PJB):\n");
                sb.append("- Processo: ").append(getStr(m, "numero", "(sem número)")).append("\n");
                sb.append("- Status: ").append(getStr(m, "status", "(sem status)")).append("\n");
                sb.append("- Fase: ").append(getStr(m, "fase", "(sem fase)")).append("\n");
                sb.append("- Rito: ").append(getStr(m, "rito", "(sem rito)")).append("\n");
                sb.append("- Última movimentação registrada: ")
                        .append(getStr(m, "ultimaMovimentacao", "(sem data)"))
                        .append("\n\n");

                sb.append("O que fazer agora:\n");
                sb.append("1) Se houver prazo/intimação, confirmar no portal oficial do tribunal e/ou Diário da Justiça.\n");
                sb.append("2) Se o número estiver correto e o PJB não encontrar, ativar integração com o sistema do tribunal correspondente (futuro).\n\n");

            } else {
                sb.append("Não foi possível localizar o processo no banco interno do PJB.\n");
                sb.append("Sugestão: conferir o número CNJ e tentar novamente.\n\n");
            }

        } else {
            sb.append("Pedido recebido e analisado.\n");
            sb.append("O assistente organiza fatos, fundamentos e próximos passos, mas não emite decisão nem substitui atuação profissional.\n\n");
        }

        
        if (!context.fundamentos().isEmpty()) {
            sb.append("Fundamentos/Notas (organização):\n");
            for (String f : context.fundamentos()) {
                sb.append("- ").append(f).append("\n");
            }
            sb.append("\n");
        }

        if (!context.alertas().isEmpty()) {
            sb.append("Alertas:\n");
            for (String a : context.alertas()) {
                sb.append("- ").append(a).append("\n");
            }
            sb.append("\n");
        }

        
        sb.append("Fontes oficiais recomendadas para validação:\n");
        sb.append("- CNJ (consultas e padrões CNJ)\n");
        sb.append("- Portais oficiais dos Tribunais (consulta pública/andamentos)\n");
        sb.append("- STF/STJ/TST/TSE/STM (jurisprudência oficial)\n");
        sb.append("- Planalto (legislação federal)\n");
        sb.append("- INSS/Receita Federal (quando aplicável)\n\n");

        Object plano = context.memory().get("plano_validacao_oficial");
        if (plano instanceof List<?> list && !list.isEmpty()) {
            sb.append("Plano de conferência (passo a passo):\n");
            for (Object p : list) {
                sb.append("- ").append(String.valueOf(p)).append("\n");
            }
            sb.append("\n");
        }

        IAResponse resposta = IAResponse.builder()
                .origem("TRIAD_IA_V3")
                .status(context.alertas().isEmpty() ? IAResponse.StatusIA.SUCESSO : IAResponse.StatusIA.ALERTA)
                .texto(sb.toString())
                .confianca(context.alertas().isEmpty() ? 0.92 : 0.78)
                .dataGeracao(Instant.now())
                .build();

        context.memory().put("finalResponse", resposta);
    }

    private static String getStr(Map<?, ?> map, String key, String fallback) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : fallback;
    }
}
