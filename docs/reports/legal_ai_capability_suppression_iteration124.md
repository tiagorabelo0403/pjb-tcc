# Round 124 — Supressão adaptativa por classe processual e sigilo

## Artefatos
- `LegalAiConversationCapabilitySuppressionService`
- `LegalAiConversationCapabilitySuppressionSnapshot`
- enriquecimento em `LegalToolScopePolicy`
- gates adicionais em `LegalSensitiveActionApprovalService`
- safeguards adicionais em `LegalAiConversationResponseComposerService`

## Resultado material
- capability pode ser monitorada, escalada ou travada conforme classe processual, sigilo e reincidência
- ramos estritos não contaminam a malha inteira
- tools, skills e examples deixam de ser reaproveitados ingenuamente em fluxo sensível

## Validação honesta
- guards Python: OK
- compilação dirigida do lote principal com `javac` e stubs transitórios locais: OK
- compilação dirigida dos testes novos e ajustados: OK
