# Round 116 — MCP doctor e transcript real

## Entradas materiais
- `LegalMcpExecutionTranscript`
- `LegalMcpDoctorCheck`
- `LegalMcpDoctorReport`
- `LegalMcpExecutionTranscriptService`
- `LegalMcpDoctorService`

## Conexões feitas
- `LegalMcpExecutionPlan` ampliado com `transcript` e `doctor`
- `JuridicaMcpServerCatalogService` passou a capturar transcript e executar doctor checks
- `JuridicaUnifiedMeshProfileService` publica sinais de transcript/doctor no bloco `mcp`
- `LegalToolScopePolicy` transporta diagnóstico MCP aprofundado até a conversa
- `LegalAiConversationResponseComposerService` expõe doctor/transcript nos safeguards

## Provas adicionadas
- `LegalMcpDoctorServiceTest`
- `LegalMcpExecutionTranscriptServiceTest`
- ajustes em `JuridicaMcpServerCatalogServiceTest`
- ajustes em `JuridicaMcpRound115ArchitectureTest`
- ajustes em `JuridicaUnifiedMeshProfileServiceTest`

## Validação honesta
- guards Python passaram
- compilação dirigida do lote principal passou
- compilação dirigida dos testes alterados passou
- `repository_layout_guard.py` exigiu limpeza real da raiz
