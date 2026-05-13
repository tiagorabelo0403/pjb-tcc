# Round 121 — capability cooldown lock registry

## Objetivo
Evitar abre-fecha instável de capability na conversa jurídica quando a mesma sessão ou o mesmo processo acumulam drift operacional, review repetido ou recovery prematura.

## O que entrou
- `LegalAiConversationCapabilityCooldownSnapshot`
- `LegalAiConversationCapabilityCooldownService`
- integração em `LegalAiConversationOrchestrator`
- integração em `LegalToolScopePolicy`
- integração em `LegalSensitiveActionApprovalService`
- integração em `LegalAiConversationResponseComposerService`

## Resultado material
- lock por capability com escopo de sessão ou sessão+processo
- congelamento de recovery candidates em cenário de flapping
- monitoramento assistido antes da reabertura estável
- safeguards explícitos para cooldown e turns remaining

## Validação honesta
- guards Python executadas com sucesso
- compilação dirigida do lote principal com `javac`: passou
- compilação dirigida dos testes novos e ajustados: passou
- sem afirmação de build Maven global verde
- sem afirmação de compile total do `pjb-api`
- sem afirmação de Docker estável
