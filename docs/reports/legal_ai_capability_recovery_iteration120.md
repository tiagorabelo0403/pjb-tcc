# Round 120 — capability recovery lane

Entrou uma capability recovery lane material para reabrir capability bloqueada somente quando replay, benchmark, doctor, fence documental e cobertura mínima de skills/examples convergirem.

Artefatos principais:
- `LegalAiConversationCapabilityRecoverySnapshot`
- `LegalAiConversationCapabilityRecoveryService`
- ajuste do `LegalAiConversationOrchestrator`
- `LegalToolScopePolicy.enrichWithCapabilityRecovery(...)`
- integração de `CAPABILITY_RECOVERY_*` no approval
- safeguards novos na resposta conversacional

Saída esperada:
- `DENIED` quando perfil jurídico ou sigilo permanecem estruturalmente bloqueados
- `PENDING` quando a sessão ainda aguarda replay vencedor, doctor estável ou recomposição mínima
- `RECOVERED` quando a capability pode voltar em modo monitorado com step-up e recovery candidates explícitos
