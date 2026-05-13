# Round 131 — contracts e IT das superfícies jurídicas soberanas

## Escopo
- `/api/ai/legal/minuta`
- `/api/ai/legal/grounding/check`

## Entregas
- Provider contract test do `LegalAiController`
- Pact do provider `PjbLegalAiProvider`
- IT HTTP das duas superfícies
- Guard arquitetural do round 131
- Guard de cobertura do pact
- Regra específica `legal-ai-governed-surfaces` no `application.yml`

## Estados provados
### Minuta
- `PROMOTED`
- `STEP_UP_REQUIRED`
- `BLOCKED`

### Grounding check
- `ALIGNED` com `groundingPromotionStatus=PROMOTED`
- `REVIEW_REQUIRED` com `groundingPromotionStatus=STEP_UP_REQUIRED`
- `BLOCKED` com `groundingPromotionStatus=BLOCKED`

## Hardening adicional
- Rota específica e mais restrita para as duas superfícies
- Content-Type estrito `application/json`
- Rate limit por `ip_user`
- `no-store-response` ativo

## Validação honesta
- Guards Python: passou
- Compilação dirigida do lote novo com stubs transitórios: passou
- Sem alegar Maven global verde
- Sem alegar compile total do `pjb-api`
- Sem alegar Docker estável
