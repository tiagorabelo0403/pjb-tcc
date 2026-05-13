# Round 117 — MCP evidence promotion e approval lane por replay

## Entradas materiais
- `LegalMcpEvidencePromotionDecision`
- `LegalMcpEvidencePromotionService`
- integração em `LegalMcpExecutionPlan`
- integração em `JuridicaMcpServerCatalogService`
- propagação em mesh, tool scope, approval e safeguards da conversa

## O que mudou de verdade
1. O MCP agora decide quais examples podem ser promovidos a partir do transcript benchmarkado.
2. A lane de approval do turno passa a considerar a evidência operacional do próprio plano MCP.
3. Prompt injection e contexto em quarentena congelam promoção e escalam a lane para revisão humana.
4. Contextos sigilosos ou approval-linked escalam para step-up mesmo quando o doctor não bloqueia todo o plano.

## Guardrails preservados
- zero executor novo
- zero scheduler novo
- zero `CompletableFuture` cru
- sem mistura indevida de síncrono e assíncrono
- sem banco novo
- sem surface paralela fora da IA jurídica existente
