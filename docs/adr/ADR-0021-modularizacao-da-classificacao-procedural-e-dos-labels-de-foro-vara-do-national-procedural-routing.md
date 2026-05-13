# ADR-0021 — modularização da classificação procedural e dos labels de foro/vara do NationalProceduralRouting

## Contexto

Após a extração do perfil material, da decisão de juizados, do party profile e da heurística de rito, o `NationalProceduralRoutingService` ainda mantinha um bloco residual sensível demais para um orquestrador:

- definição do regime procedural
- definição da trilha procedural
- construção do label de foro sugerido
- construção do label de vara sugerida

Esses pontos já não eram o núcleo da regra de competência ou da canonicalização, mas continuavam acoplados ao fluxo principal e faziam o serviço carregar classificação e apresentação territorial/judiciária ao mesmo tempo.

## Decisão

Separar esses subeixos em dois colaboradores próprios, preservando o contrato público do serviço principal:

- `NationalProceduralClassificationResolver`
- `NationalProceduralForumLabelFactory`

O `NationalProceduralRoutingService` continua responsável por coordenar a análise procedural nacional, mas deixa de conter diretamente os fechamentos de regime/trilha e a montagem de labels de foro/vara.

## Consequências

### Positivas

- o serviço principal fica mais curto no trecho que combina rito, juizado, distribuição e unidade sugerida
- a classificação procedural passa a ter ponto dedicado para crescer sem recontaminar o orquestrador
- os labels territoriais/judiciais ficam concentrados em fábrica própria, reduzindo mistura entre regra de decisão e apresentação
- a base ganha mais uma trava de governança para impedir regressão dessa concentração

### Custos

- aumento controlado do número de colaboradores do eixo procedural
- necessidade de manter testes de governança para proteger a disciplina de modularização

## Status

Aceito.
