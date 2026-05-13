# Platform Structural Decomposition — Round 116

## Objetivo
Atacar o `TribunalRuleEngine` como hotspot estrutural restante, removendo a concentração simultânea de resolução de regra, adaptação para `NationalRulePack`, sincronização de buckets por tribunal e análise/cobertura no mesmo arquivo.

## O que entrou
- `TribunalRuleResolutionSupport`
- `TribunalRulePackSynchronizationSupport`
- `TribunalRuleAnalyticsSupport`
- `TribunalRuleEngineRefinementArchitectureTest`
- `TribunalRuleEngineBehaviorTest`

## O que foi extraído do engine
### Resolução e snapshots
- resolução de regra e fallback
- resolução em lote
- snapshots de triagem, prazo e distribuição
- enriquecimento de contexto para `NationalRulePack`
- sincronização de configuração do motor nacional de prazo
- composição de extras para integração
- cadeia aplicável, merge/estender/restringir e auditoria

### Sincronização e adaptação para rule pack
- descoberta de tribunais customizados
- sincronização de buckets adaptados por tribunal/ramo/grau
- adaptação de `RegraResolvida` para `NationalRulePackEngine.Regra`
- trim do registry de sincronização
- inferência de ramo de rule pack por código do tribunal

### Analytics e cobertura
- análise de desvios entre regra nacional e regra local
- relatório de cobertura por tribunal
- leitura de log de resoluções
- contagem de fallback
- listagem por tribunal
- listagem de expiradas
- total de regras ativas

## Resultado objetivo
- `TribunalRuleEngine`: `1442 -> 897` linhas
- saiu do hotspot do `architecture_hygiene_guard`
- suporte de resolução ficou abaixo de 500 linhas
- suporte de sincronização ficou abaixo de 200 linhas
- suporte analítico ficou abaixo de 200 linhas

## Travas adicionadas
- `TribunalRuleEngineRefinementArchitectureTest`
  - impede regressão estrutural do engine e valida a permanência dos suportes extraídos
- `TribunalRuleEngineBehaviorTest`
  - valida override por tribunal, desvio em relação ao baseline nacional e cobertura personalizada

## Continuidade recomendada
Depois desta rodada, os próximos eixos com melhor retorno alinhados ao diagnóstico sênior são:
1. Pact provider verification real para autenticação, peticionamento e consulta pública
2. mais `*IT.java` por bounded context com PostgreSQL real
3. catálogo explícito de classificação LGPD + aplicação inicial de RLS em processos sigilosos
4. decomposição de facades institucionais ainda extensas (`PeticionamentoSessaoFacadeService`, superfícies nacionais de comunicação)
