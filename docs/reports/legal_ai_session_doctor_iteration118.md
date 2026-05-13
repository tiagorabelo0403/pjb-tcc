# Round 118 — Legal AI Session Doctor

## Objetivo
Ligar replay/benchmark/doctor do MCP ao histórico retido da conversa para bloquear automaticamente a surface quando houver drift operacional.

## Entradas materiais
- memória multi-turn retida
- status de grounding
- status de execução simbólica
- benchmark/replay/doctor do MCP
- pressure de contradição e evidência faltante
- fence documental do turno

## Saídas materiais
- `status`: READY, DEGRADED ou BLOCKED
- `blockedSurface`
- `blockedSkillIds`
- `blockedToolExampleIds`
- `operationalMode`
- `diagnostics` auditáveis por sessão

## Efeitos de runtime
- o tool scope passa a poder congelar a surface inteira por sessão
- a approval lane sobe automaticamente quando o doctor contínuo detectar deriva
- reuse de skill/example promovido pelo MCP é congelado até replay vencedor
