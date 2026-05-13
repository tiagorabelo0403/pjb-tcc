# PLATFORM STRUCTURAL DECOMPOSITION ROUND110

## Escopo da rodada

Esta rodada continuou a correção dos pontos críticos já mapeados para o PJB:
- saturação e deriva de concorrência fora da espinha oficial
- budgets transacionais e pressão silenciosa sobre pool de conexão
- classes gigantes com concentração indevida de fluxo operacional
- necessidade de deixar o histórico técnico explícito para continuidade em novos chats

## Decisão de priorização

Entre os próximos hotspots apontados na rodada anterior:
- `OficialJusticaPainelService`
- `SecretariatQueueQueryService`
- `QualifiedSignatureIdentityContextService`
- `ProceduralCatalogSupport`

foi priorizado `OficialJusticaPainelService` nesta rodada porque combinava:
- linha total acima do limiar do guard de arquitetura
- fluxo operacional crítico de ofício/resposta com lógica de montagem concentrada
- superfície com risco de regressão funcional se continuasse crescendo dentro do painel principal

## Mudanças implementadas

### 1. Extração do workflow de ofícios do painel do oficial de justiça

Foi criado o collaborator dedicado:
- `OficialJusticaOficioWorkflowSupport`

Responsabilidades extraídas do painel principal:
- resolução de destinatário processual para ofício institucional
- montagem do mapa de destinatário resolvido
- criação da juntada direta do ofício original no processo
- composição da topologia de protocolo direto no processo
- composição formal da descrição do ofício
- normalização do fundamento institucional

### 2. Redução do tamanho do hotspot

Resultado objetivo:
- `OficialJusticaPainelService`: `1022 -> 804` linhas

### 3. Remoção de código morto

Foi removida a trilha privada não utilizada de encaminhamento cartorário auxiliar que não era invocada pelo serviço após a consolidação do protocolo direto no processo.

### 4. Travas de regressão

Testes adicionados:
- `OficialJusticaOficioWorkflowSupportTest`
- `OficialJusticaPainelRefinementArchitectureTest`

Esses testes fixam:
- materialização da juntada direta com canal e envelope corretos
- propagação da resolução institucional do destinatário
- manutenção do painel abaixo do limiar de hotspot de service
- permanência da lógica extraída fora do `OficialJusticaPainelService`

## Continuidade recomendada

Próximos alvos com melhor retorno:
1. `QualifiedSignatureIdentityContextService`
2. `SecretariatQueueQueryService`
3. `ProceduralCatalogSupport`

A lógica de decisão para a próxima rodada continua sendo:
- reduzir classes acima de 900/1000 linhas
- evitar reintrodução de fronteiras assíncronas cruas
- manter budgets transacionais explícitos nos hotspots
- não reabrir risco de saturação de pool, memory leak silencioso ou race-condition por fluxo paralelo mal governado
