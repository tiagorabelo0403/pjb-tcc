# ADR-0009 — modularização do catálogo de painéis institucionais

## Contexto

`InstitutionalPanelBlueprintApplicationService` concentrava todo o catálogo institucional de painéis em uma única classe com centenas de linhas, misturando composição de catálogo, filtro, ordenação e dependência direta das rotas canônicas. Isso tornava a manutenção mais frágil e deixava testes de qualidade acoplados ao texto bruto de um único arquivo.

## Decisão

O catálogo foi separado em peças menores sob `panel.application.catalog`:

- `ForumInstitutionalPanelBlueprintCatalog`
- `MinisterioPublicoInstitutionalPanelBlueprintCatalog`
- `DefensoriaEProcuradoriaInstitutionalPanelBlueprintCatalog`
- `ApoioInstitucionalPanelBlueprintCatalog`
- `InstitutionalPanelBlueprintCatalog`

`InstitutionalPanelBlueprintApplicationService` passou a atuar como orquestrador curto, combinando os catálogos, aplicando filtro de escopo/painel e ordenação final.

A construção manual permanece disponível no construtor padrão para testes puros e uso fora do container Spring. No runtime Spring, a montagem preferencial passa pelo construtor com `List<InstitutionalPanelBlueprintCatalog>`.

## Consequências

- o catálogo fica organizado por trilha institucional, e não por arquivo monolítico
- novas expansões entram por catálogo específico, reduzindo colisão entre áreas
- a camada de aplicação volta a focar em orquestração, não em armazenamento massivo de dados estáticos
- os testes de qualidade deixam de depender da permanência artificial de todo o catálogo em um único arquivo
