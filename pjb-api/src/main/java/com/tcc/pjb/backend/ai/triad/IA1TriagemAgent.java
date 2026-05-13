package com.tcc.pjb.backend.ai.triad;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.CognitiveAgent;
import com.tcc.pjb.backend.ai.core.enums.CognitiveStage;
import com.tcc.pjb.backend.ai.core.model.CognitiveContext;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IA1TriagemAgent implements CognitiveAgent {

    private final ProcessoRepository processoRepository;

    public static final List<String> IDEIAS = List.of(
                        "Normalizar número CNJ e validar dígitos verificadores antes de consultar bases.",
                        "Identificar intenção do usuário (consulta, prazo, modelo de peça, acordo, impugnação, recurso).",
                        "Classificar o papel do solicitante (cidadão, advogado, MP, juiz, servidor) e aplicar permissões.",
                        "Extrair entidades do pedido (processo, partes, valor, rito, foro, vara, instância, tribunal).",
                        "Detectar se o pedido envolve sigilo (família, infância, saúde, bancário) e exigir reforço.",
                        "Checar se o pedido pede decisão/sentença e aplicar política: nunca decidir, apenas auxiliar.",
                        "Mapear o ramo/matéria presumida com base em palavras-chave e metadados do processo.",
                        "Selecionar trilha de análise (CPC, CPP, CLT, JE/EF, execuções, mandado de segurança).",
                        "Identificar urgência (liminar, tutela, plantão, risco de perecimento) e disparar alerta.",
                        "Localizar o processo no banco local por número unificado e por número antigo (compat).",
                        "Conferir se há movimentações recentes e capturar a última data conhecida.",
                        "Determinar se a dúvida é de 'andamento' (consulta) ou de 'estratégia' (orientação).",
                        "Separar fatos fornecidos pelo usuário de inferências do sistema (auditabilidade).",
                        "Gerar checklist de dados faltantes sem travar: tentar responder com o que existe.",
                        "Aplicar sanitização LGPD no texto do usuário (CPF, telefone, e-mail) em memória.",
                        "Classificar o nível de confiabilidade do pedido: dados oficiais vs relato informal.",
                        "Selecionar módulos internos relevantes (prazo, financeiro, documentos, auditoria, chat).",
                        "Construir 'contexto mínimo' para a IA2: objetivo, fatos, rito provável, riscos.",
                        "Detectar conflito de datas (ex.: prazo vencido) e sinalizar como possível erro humano.",
                        "Checar se envolve pessoa falecida (óbito) e acionar 'lifecycle' para travas preventivas.",
                        "Analisar se o pedido é de 'movimentação' e preparar resposta com status + próximos passos.",
                        "Validar linguagem ofensiva/ameaçadora e aplicar guardrails de segurança.",
                        "Resolver timezone e datas de referência (America/Fortaleza) para prazos e agendas.",
                        "Triar se envolve tribunal superior (STF/STJ/TST/TSE/STM) e ajustar trilha.",
                        "Triar se envolve execução/penhora/leilão e separar fase executiva.",
                        "Triar se envolve audiência e preparar consulta de pauta/agenda.",
                        "Triar se envolve diligência de oficial de justiça e preparar rota e confirmação.",
                        "Triar se envolve delegacia/BO e preparar fluxo de atendimento cidadão.",
                        "Triar se envolve MP e preparar trilha de manifestação/parecer sem substituir atuação.",
                        "Triar se envolve Defensoria e preparar orientação de documentos e triagem social.",
                        "Detectar se o usuário quer um 'resumo do processo' e preparar sumarização segura.",
                        "Detectar se o usuário quer 'modelo de petição' e preparar escopo sem automatizar mérito.",
                        "Analisar se há pedido de 'cálculo' e encaminhar para engine financeira apropriada.",
                        "Detectar divergência de jurisdição (estadual x federal) e sinalizar caminhos.",
                        "Detectar conflito de competência e sugerir checagens objetivas.",
                        "Preparar 'plano de perguntas' para coletar prova essencial (documentos-chave).",
                        "Classificar documentos citados (contrato, NF, conversas, laudo) e orientar organização.",
                        "Detectar se há risco de nulidade (intimação, citação, competência) e marcar alerta.",
                        "Detectar se é Juizado Especial e sinalizar limites (valor, complexidade, recursos).",
                        "Detectar se é rito trabalhista e sinalizar audiência una, prova, prazos de CLT.",
                        "Detectar se é penal e sinalizar cautela com presunção de inocência e sigilo.",
                        "Detectar se é família e sinalizar prioridade/segredo e proteção de menores.",
                        "Detectar se é previdenciário e preparar trilha CNIS/benefício/carência.",
                        "Detectar se é tributário e preparar trilha execução fiscal/defesa/parcelamento.",
                        "Detectar se é consumidor e preparar trilha prova/CDC/competência.",
                        "Gerar 'resumo estruturado' (tópicos) sem perder fluidez para alimentar IA2.",
                        "Adicionar rastreio (traceId) e registrar evento de triagem na auditoria.",
                        "Marcar quais regras internas foram aplicadas (policy flags) para inspeção posterior.",
                        "Preparar 'contexto de rito' com fase processual provável (cognitiva/instrutória/executória).",
                        "Preparar 'contexto de sigilo' com nível recomendado e justificativa.",
                        "Preparar 'contexto de compliance' (LGPD/ética/segurança) e limites do assistente.",
                        "Selecionar 'template de resposta' conforme finalidade (andamento, prazo, orientação documental).",
                        "Validar consistência do request payload (chaves, tipos) e corrigir tolerância.",
                        "Extrair 'pergunta final' em uma frase para orientar IA2.",
                        "Catalogar erros de entrada comuns (número incompleto, vara errada) e sugerir correção."
                );

    @Override
    public CognitiveStage stage() {
        return CognitiveStage.TRIAGEM;
    }

    @Override
    public void process(CognitiveContext context) {
        String acao = safe(context.request().getAcao()).toUpperCase(Locale.ROOT).trim();
        context.memory().put("acao_normalizada", acao);

        
        context.memory().putIfAbsent("intent", "GERAL");

        if (acao.contains("MOVIMENT") || acao.contains("ANDAMENTO") || acao.contains("CONSULTAR_PROCESSO")) {
            context.memory().put("intent", "CONSULTA_MOVIMENTACAO");
        } else if (acao.contains("PRAZO")) {
            context.memory().put("intent", "CALCULO_PRAZO");
        } else if (acao.contains("ACORDO")) {
            context.memory().put("intent", "APOIO_ACORDO");
        }

        
        Object num = context.request().getPayload().getOrDefault("numeroProcesso",
                context.request().getPayload().get("numeroUnificado"));

        if (num instanceof String s && !s.isBlank()) {
            String numero = s.trim();
            context.memory().put("numero_informado", numero);

            
            Optional<?> p = processoRepository.findByNumeroUnificado(numero)
                    .map(x -> x)
                    .or(() -> processoRepository.findByNumeroProcesso(numero).map(x -> x));

            if (p.isPresent()) {
                context.memory().put("processo_encontrado", true);
                context.memory().put("processo_obj", p.get());
            } else {
                context.memory().put("processo_encontrado", false);
            }
        }

        
        if (acao.contains("SENTENCA") || acao.contains("DECISAO")) {
            context.alertas().add("Assistente não emite decisão/sentença: apenas organiza fatos, fundamentos e próximos passos.");
            context.memory().put("guardrail_decisao", true);
        }

        
        context.memory().put("resumo_triagem", Map.of(
                "intent", context.memory().get("intent"),
                "acao", acao,
                "dominio", String.valueOf(context.dominioPrimario()),
                "papel", String.valueOf(context.papelSolicitante()),
                "temNumero", context.memory().containsKey("numero_informado"),
                "processoEncontrado", context.memory().getOrDefault("processo_encontrado", false)
        ));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
