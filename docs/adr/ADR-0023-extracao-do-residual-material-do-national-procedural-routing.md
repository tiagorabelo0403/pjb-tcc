# ADR-0023 — extração do residual material do NationalProceduralRouting

## Contexto

Após as rodadas anteriores, o `NationalProceduralRoutingService` já havia perdido payload factories, territorialidade, party profile, heurística de rito, classificação, labels, forum allocation e síntese decisória final. Ainda assim, o arquivo principal continuava carregando quatro blocos materiais relevantes:

- classificação do perfil probatório
- fechamento da banda de complexidade
- inferência e fallback de `tipoJustica`
- integração direta com a distribuição dinâmica do `MapaCompetenciaDinamicoEngine`

Esses pontos mantinham regra material, fallback estrutural e decisão operacional dentro do orquestrador, contrariando a diretriz de deixar services curtos e colaboradores dedicados.

## Decisão

O eixo residual foi separado em colaboradores próprios:

- `NationalProceduralProbatoryProfileResolver`
- `NationalProceduralComplexityBandResolver`
- `NationalProceduralTipoJusticaResolver`
- `NationalProceduralDistributionResolver`

Também foram introduzidos contratos internos explícitos para dar forma ao contexto transferido entre o orquestrador e os novos resolvers:

- `NationalProceduralComplexityContext`
- `NationalProceduralDistributionContext`

O `NationalProceduralRoutingService` permanece como coordenador do fluxo procedural, mas deixa de carregar essas regras diretamente.

## Consequências

### Positivas

- o orquestrador principal fica mais curto e mais coerente com o papel de costura do fluxo
- a heurística probatória e a banda de complexidade deixam de ficar escondidas no meio da análise procedural
- a resolução de `tipoJustica` passa a ter fallback explícito e centralizado, reduzindo transições com `null` desnecessário
- a ponte com a distribuição dinâmica passa a ser testável sem contaminar o serviço principal
- reduz-se o risco de retorno de regra material residual ao `NationalProceduralRoutingService`

### Custos

- aumento do número de colaboradores internos do eixo procedural
- necessidade de manter testes de governança para impedir regressão estrutural

## Proteções adotadas

- testes dedicados para perfil probatório, complexidade, tipo de justiça e distribuição dinâmica
- teste de governança para garantir que os métodos residuais não retornem ao serviço principal
- preservação do contrato externo de `ProceduralRoutingReport`
