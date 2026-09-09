# ADR-0063 — Fachadas de superfície não serão achatadas pela métrica de pass-through

## Contexto

O "Plano de Melhoria — PJB v3" da coorientação mede 25 beans em que ao menos 80% dos métodos públicos têm corpo de uma linha no formato `return outro.metodo(args)`, e propõe removê-los pelo teste de deleção: se apagar o módulo não concentra complexidade em lugar nenhum, ele não pagava seu custo.

A medição foi reproduzida contra o HEAD atual e devolveu 51 beans pelo mesmo critério. A leitura do código, porém, mostra que a contagem responde a uma pergunta sintática, não à pergunta arquitetural.

## Medição

Duas definições foram aplicadas à mesma base:

| Definição | Resultado |
|---|---|
| Frouxa — corpo de uma linha terminando em `.metodo(args);` | 51 beans |
| Estrita — `return campo.metodo(<apenas parâmetros do método>);` | 23 beans |

A diferença de 28 beans são falsos positivos de uma classe específica: corpos de uma linha que **compõem** duas chamadas e injetam uma chave canônica.

```java
return projectionSupport.snapshot("lgpd.processual.classificacao", engine.classificar(processoId));
```

A chave (`lgpd.processual.classificacao`) é identificador de contrato da superfície REST, e `snapshot(...)` constrói o envelope padronizado. Apagar a fachada não remove um salto: espalha chave de contrato e construção de envelope pelos controllers.

Na mesma família, `AdminJudicialConnectorHubSurfaceFacadeService.resolveHorizon` aplica default de 24 horas com piso de 60 segundos — política de negócio dentro de um método classificado como repasse.

## Disposição dos 23 repasses estritos

| Situação | Beans | Por que não achatar |
|---|---|---|
| Criados por fatias de F6 | 16 | Existem para reduzir dependências de construtor. Achatar devolve as dependências ao bean raiz e reabre o god service que F6 fechou |
| Seam compartilhado real | 4 | `InstitutionalPainelSurfaceFacadeService` tem 12 chamadores, `NationalCommunicationInstitutionalSurfaceFacadeService` tem 17. Deletar distribui a delegação por todos eles — a complexidade reaparece, que é exatamente o que o teste de deleção reprova |
| Ponto de injeção de fachada encapsulada | 1 | `ProcessReadingWorkspaceService` monta `ProcessReadingWorkspaceFacade`, que é package-private. Achatar exigiria tornar pública uma classe interna ou mover a montagem para o controller |
| Adapter de porta hexagonal | 1 | `CustaJudicialStoreAdapter` implementa `CustaJudicialStorePort`. A contagem por nome de classe dá zero chamadores porque os consumidores injetam a interface e o Spring resolve por tipo |
| Guarda estrutural preexistente | 1 | `NationalCommunicationFlowService` é verificado por `NationalCommunicationFlowServiceStructuralSeparationTest` |

## Decisão

F4 não será executada como especificada. Nenhuma fachada de superfície será removida com base na proporção de métodos de uma linha.

Duas regras passam a valer para qualquer reavaliação futura:

1. A métrica de pass-through só é aplicável na definição estrita — `return campo.metodo(<apenas parâmetros>)`. Corpo de uma linha que compõe chamadas, aplica default ou anexa chave de contrato é adaptador, não repasse.
2. Um repasse estrito só é candidato a remoção se tiver **um único chamador** e não existir para reduzir a superfície de injeção de outro bean. Com múltiplos chamadores a complexidade reaparece distribuída, e o próprio teste de deleção reprova a remoção.

## Conflito registrado entre duas frentes do mesmo plano

F6 reduz dependências de construtor extraindo sub-orquestradores, e todo sub-orquestrador extraído é, por construção, um delegador de uma linha. F4 propõe remover delegadores de uma linha. Executadas juntas e literalmente, uma desfaz a outra: 16 dos 23 repasses estritos medidos hoje são resultado direto de fatias de F6 já mescladas.

Onde as duas frentes colidirem, F6 prevalece — reduzir a superfície de injeção de um bean é ganho verificável por contagem de dependências e por custo de teste, enquanto remover um salto de delegação com chamador único é ganho de legibilidade.

## Consequências

- A métrica "fachadas ≥80% pass-through" do plano permanece alta e **não** deve ser lida como dívida pendente.
- Fica preservada a convenção `*SurfaceFacadeService` como envelope REST com chave canônica de projeção.
- Se a superfície de projeção deixar de usar chave canônica no futuro, esta decisão precisa ser reavaliada — é a premissa que sustenta os 28 falsos positivos.
