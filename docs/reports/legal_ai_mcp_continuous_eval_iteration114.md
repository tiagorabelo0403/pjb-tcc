# Round 114 — MCP jurídico com benchmark contínuo e replay

## O que entrou
- `ai/juridica/eval/LegalBenchmarkCatalog`
- `ai/juridica/eval/LegalEvalReplayRunner`
- `ai/juridica/eval/LegalMcpPlanScorer`
- `ai/juridica/eval/LegalMcpServerPromotionPolicy`
- `ai/juridica/eval/LegalMcpServerDemotionPolicy`
- `model/dto/ai/legal/eval/LegalEvalMetric`
- `model/dto/ai/legal/eval/LegalEvalCase`
- `model/dto/ai/legal/eval/LegalEvalSuite`
- `model/dto/ai/legal/eval/LegalEvalReplayArtifact`
- `model/dto/ai/legal/eval/LegalEvalReplayResult`
- `LegalMcpExecutionPlan` ampliado com `evaluation`

## Materialização
- benchmark suite por scope jurídico e estado de risco;
- replay determinístico do plano MCP;
- score de qualidade do plano;
- políticas de promoção e rebaixamento por evidência;
- publication do score e adaptation hints na malha MCP;
- readiness para loop de melhoria contínua do planner.

## Hints estratégicos materializados
- `DISCOVER_THEN_PIN`
- `LOAD_CANONICAL_TOOL_EXAMPLES`
- `SLIDING_COMPACTION`
- `TRANSCRIPT_CAPTURE_AND_REPLAY`
- `AUTO_READONLY_MONITORED`
- `STRICT_REVIEW`

## Validação honesta
- `runtime_concurrency_guard.py`
- `architecture_hygiene_guard.py`
- `constructor_injection_guard.py`
- `config_taxonomy_guard.py`
- `transactional_hotspot_guard.py --fail-on-missing-budgets`
- `repository_layout_guard.py`
- compilação dirigida do lote principal com `javac` e stubs transitórios locais
- compilação dirigida dos testes do lote `eval` e `mcp`
