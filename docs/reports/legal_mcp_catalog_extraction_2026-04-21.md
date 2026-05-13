# Extração de textos operacionais MCP para catálogo externo

## Objetivo
Remover textos operacionais e exemplos MCP do Java de execução, concentrando esses dados em `resources/catalog` com carregamento fail-fast e validação dedicada.

## Entradas adicionadas
- `catalog/legal_mcp_text_2026.json`
- `catalog/legal_mcp_tool_examples_2026.json`
- `scripts/legal_mcp_catalog_guard.py`

## Classes adicionadas
- `LegalMcpResourcePaths`
- `LegalMcpTextCatalogService`
- `LegalMcpToolExampleCatalogService`

## Classes ajustadas
- `LegalMcpDeliberationCheckpointService`
- `LegalMcpToolExampleRegistry`
- `JuridicaMcpServerCatalogService`
- `README.md`

## Resultado estrutural
- razões deliberativas saíram do código de orquestração MCP
- modos de seleção e salvaguardas MCP saíram do código de orquestração MCP
- exemplos de tools MCP passaram a ser carregados de catálogo JSON
- o repositório ganhou validação específica para o catálogo MCP

## Validações executadas
- `python scripts/legal_mcp_catalog_guard.py`
- `python scripts/legal_knowledge_catalog_guard.py`
- `python scripts/import_sanity_probe.py`
- `python scripts/repository_layout_guard.py`
- `python scripts/runtime_concurrency_guard.py`
