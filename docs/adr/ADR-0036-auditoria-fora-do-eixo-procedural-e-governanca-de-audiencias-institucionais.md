# ADR-0036 — auditoria fora do eixo procedural e governança de audiências institucionais

## Contexto

Depois do fechamento estrutural do eixo procedural, a próxima rodada de valor passou a ser a caça de incoerências reais de compilação e de concentração estrutural fora dele. A auditoria do fluxo de governança de audiências institucionais mostrou dois problemas materiais:

- o `InstitutionalHearingSchedulingGovernanceApplicationService` acumulava capability resolution, análise de escopo, filas, guardas de segregação, catálogo de atores e construção integral das governanças por rito;
- o arquivo original chamava `firstNonBlank(...)` com cadeias de 4 e 5 argumentos em pontos como `resolveUnitCluster(...)` e `resolveJurisdictionAxis(...)`, mas só mantinha sobrecargas de 2 e 3 argumentos, criando risco real de falha de compilação quando o ruído de dependências deixasse de mascarar o problema.

## Decisão

A governança de audiências institucionais passou a operar com separação explícita por responsabilidade:

- `InstitutionalHearingSchedulingCapabilityResolver` concentra perfil operacional, capacidades e gates principais;
- `InstitutionalHearingSchedulingScopeSupport` concentra escopo, chaves, filas, segregação e fallbacks territoriais/unidade;
- `InstitutionalHearingSchedulingActorCatalog` concentra os catálogos de atores por função operacional;
- `InstitutionalHearingRiteGovernanceResolver` concentra a construção das governanças por rito;
- `InstitutionalHearingSchedulingGovernanceApplicationService` permanece como orquestrador curto.

A correção do risco de compilação foi absorvida dentro de `InstitutionalHearingSchedulingScopeSupport`, com sobrecargas explícitas de `firstNonBlank(...)` para 4 e 5 argumentos.

## Consequências

### Positivas

- o serviço principal ficou menor, mais previsível e mais coerente com a disciplina já aplicada ao eixo procedural;
- capability, scope e rite governance passaram a ser testáveis e endurecíveis de forma isolada;
- o risco real de compilação por fallback expandido deixou de ficar escondido no arquivo monolítico;
- a extração prepara melhor a malha institucional para novas trilhas sem recontaminar o orquestrador principal.

### Custos

- houve aumento do número de colaboradores no pacote de `panel.application`;
- parte da densidade estrutural migrou para `InstitutionalHearingRiteGovernanceResolver`, que poderá ser o próximo alvo se o eixo institucional continuar sendo endurecido.
