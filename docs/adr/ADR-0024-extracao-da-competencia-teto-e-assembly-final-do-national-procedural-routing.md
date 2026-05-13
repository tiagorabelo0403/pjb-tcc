# ADR-0024 — extração da competência, teto e assembly final do NationalProceduralRouting

## Contexto

Após as rodadas anteriores, o `NationalProceduralRoutingService` já havia perdido a maior parte das regras materiais de rito, perfil, juizado, distribuição, classificação e síntese final. Ainda assim, três blocos estruturais relevantes permaneciam no arquivo principal:

- montagem do `CompetenceResolveRequest`
- acionamento direto do diagnóstico de teto processual
- instanciação final do `ProceduralRoutingReport`

Esses pontos mantinham tradução de contrato externo, costura operacional com serviço transversal e apresentação final do relatório dentro do mesmo orquestrador. Isso contrariava a diretriz de manter services curtos, com colaboradores dedicados e contratos internos explícitos.

## Decisão

Os blocos foram separados em colaboradores próprios:

- `NationalProceduralCompetenceRequestFactory`
- `NationalProceduralTetoDiagnosticResolver`
- `NationalProceduralRoutingReportAssembler`

Também foi introduzido contexto explícito para o eixo de teto e para o assembly final do relatório:

- `NationalProceduralTetoDiagnosticContext`
- `NationalProceduralRoutingReportAssemblyContext`

O `NationalProceduralRoutingService` continua como coordenador da análise procedural nacional, mas deixa de traduzir diretamente o request de competência, de costurar ele mesmo o teto processual e de instanciar o relatório final.

## Consequências

### Positivas

- o serviço principal fica mais próximo de uma orquestração pura
- o contrato do resolvedor nacional de competência passa a ter tradução centralizada e testável
- a ponte entre payload procedural, canonical, competência e `TetoProcessualService` fica isolada e reaproveitável
- a montagem final do relatório deixa de misturar apresentação e fluxo dentro do orquestrador principal
- reduz-se o risco de regressão cosmética em futuras rodadas do eixo procedural

### Custos

- aumento do número de colaboradores internos do módulo procedural
- necessidade de manter testes de governança adicionais para impedir regressão estrutural

## Proteções adotadas

- testes dedicados para factory de competência, resolver de teto e assembler do relatório
- teste de governança para garantir que esses blocos não retornem ao `NationalProceduralRoutingService`
- preservação do contrato externo de `ProceduralRoutingReport`
