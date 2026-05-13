# Architecture Hygiene Round 104

Esta rodada priorizou extrações de baixo risco em classes gigantes, sem reescrever regra de negócio nem alterar a superfície HTTP.

## Eixos tratados

### Secretaria judicial
- `SecretariatQueuePanelRow` passou a materializar o snapshot interno do painel/agenda.
- `SecretariatQueueAgendaFilter` passou a materializar o filtro normalizado da agenda.
- `SecretariatQueueQueryService` deixou de carregar esses value objects como tipos aninhados.

### Tribunal / plugins
- `PluginResolucaoTribunalService` teve seus manifests, specs, snapshots e resultados extraídos para arquivos dedicados.
- `TribunalRuleEngine` teve snapshots e estados auxiliares extraídos para arquivos dedicados.

### Assinatura qualificada
- `QualifiedSignatureIdentityContextService` deixou de concentrar também os objetos resolvidos do contexto de assinatura.

## Ganho estrutural
- redução de classes gigantes sem duplicar fluxo
- melhora de legibilidade para IDE, revisão e manutenção
- preparação mais segura para próximas extrações por bounded context
- menor risco de regressão semântica do que uma quebra grande por pacote em uma única rodada

## Limite assumido
A rodada não tenta redesenhar toda a regra de negócio desses serviços. Ela reduz acoplamento estrutural e concentra os próximos hotspots para extrações futuras mais seguras.
