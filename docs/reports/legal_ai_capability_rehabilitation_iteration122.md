# Round 122 — capability rehabilitation stability window

## Objetivo
Liberar a volta de capability somente quando a sessão acumular estabilidade vencedora suficiente por turno após recovery e cooldown.

## O que entrou
- `LegalAiConversationCapabilityRehabilitationSnapshot`
- `LegalAiConversationCapabilityRehabilitationService`
- integração em `LegalAiConversationOrchestrator`
- integração em `LegalToolScopePolicy`
- integração em `LegalSensitiveActionApprovalService`
- integração em `LegalAiConversationResponseComposerService`

## Resultado material
- janela formal de reabilitação por capability
- contagem de estabilidade vencedora por turno
- liberação monitorada apenas após convergência mínima
- safeguards explícitos para release lane e turns remaining

## Validação honesta
- guards Python executadas com sucesso
- compilação dirigida do lote principal com `javac`: passou
- compilação dirigida dos testes novos e ajustados: passou
- sem afirmação de build Maven global verde
- sem afirmação de compile total do `pjb-api`
- sem afirmação de Docker estável
