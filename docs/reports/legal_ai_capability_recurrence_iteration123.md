# Round 123 — capability recurrence registry

## Objetivo
Endurecer a IA jurídica conversacional com um registry de reincidência por capability e processo, usando apenas a memória multi-turn já governada, sem executor paralelo, sem scheduler e sem armazenamento solto.

## Artefatos
- `LegalAiConversationCapabilityRecurrenceService`
- `LegalAiConversationCapabilityRecurrenceSnapshot`
- integração no orquestrador, tool scope, approval e response composer

## Materialização
- contagem determinística de reincidência na mesma capability/processo
- score de risco por recorrência operacional
- escalonamento automático por step-up, human review ou hard lock
- bloqueio de tools quando a mesma capability oscila repetidamente

## Validação
- guards Python passaram
- compilação dirigida com `javac` do lote principal passou
- testes novos e ajustados passaram
