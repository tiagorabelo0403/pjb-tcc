# Modularização de `custas` — Design

## Contexto

`docs/architecture/monolith_to_modular_strategy.md` já define a estratégia (strangler fig interno) e a estrutura-alvo de 5 camadas para o PJB. Só um módulo (`modules.acordo`) tem a estrutura completa hoje; `modules.prazos` tem uma migração parcial em andamento. Esta fatia leva um segundo bounded context — `custas` — para a estrutura completa, replicando o padrão já validado por `acordo` e pelo teste de arquitetura já existente para `prazos`.

**Por que `custas` e não outro candidato**: investigação comparou `custas`, `prazos`, `citacao`, `intimacao`. `custas` tem o menor raio de explosão (só 2 arquivos externos reais consomem algo do pacote, e a superfície cruzada é um único método), já tem `domain/` com 44 arquivos livres de Spring/JPA (confirmado por grep), repositório próprio, e 13 testes já cobrindo o comportamento. Crucialmente, **`custas` não está plugado no fluxo real de ajuizamento** (`D-motor-custas-nao-integrado-ao-ajuizamento`, dívida aberta por decisão de negócio, não tocada aqui) — mover esse código não arrisca quebrar nenhum caminho de produção ativo. `citacao`/`intimacao` foram descartados por estarem entrelaçados num motor compartilhado sem separação clara e com quase nenhum teste; `prazos` foi descartado por já estar fragmentado em 7+ pacotes legados, raio de explosão grande demais pra uma única fatia.

## Achado que muda a abordagem: `@PjbDataOwnership` é metadado morto

Existe uma anotação `@PjbDataOwnership(module=PjbModuleId.X, mode=PjbOwnershipMode.Y)` em 225 entidades do projeto, incluindo `CustaJudicial` (`module=PROCESSO_LIFECYCLE, mode=PUBLISHED_VIEW`). Investigação confirmou que essa tag é **puramente decorativa hoje**:

- O único teste que a validaria (`PjbArchitectureTest.entities_devem_ter_anotacao_ownership`) está `@Disabled` desde sempre.
- Nenhum guard Python nem outro teste lê essa anotação.
- O valor atual em `CustaJudicial` é o *fallback* mecânico de `scripts/round27a_apply_ownership.py` (linha 85: `return "PROCESSO_LIFECYCLE"` quando nenhuma regra de nome de arquivo/pacote casa) — não é uma decisão deliberada de que "processo" é dono do ciclo de vida de custas.
- Nem `modules.acordo` nem `modules.prazos` — os dois módulos já modularizados — usam essa anotação em nenhuma de suas entidades.

**Decisão: não mexer em `@PjbDataOwnership` nesta fatia.** Seguir o precedente real (`acordo`/`prazos` não a usam) em vez de investir trabalho numa governança que não está conectada a nada que rode de verdade. Isso é consistente com "não fazer refactor gigante" — resolver a inconsistência entre os dois sistemas de governança (`@PjbDataOwnership` vs. `modular_monolith_guard.py`) é uma decisão arquitetural própria, fora do escopo de mover um módulo.

**A governança que realmente importa aqui é `scripts/modular_monolith_guard.py`** — roda com `maxErrors: 0` (bloqueia build) e orçamentos de warning por regra (`docs/architecture/modular_monolith_guard_baseline.json`). As regras relevantes: domain não pode importar Spring/JPA/web/repository nem ser `@Entity`/`@Repository` (ERROR); application não pode importar web nem repository (ERROR); infrastructure não pode importar web (ERROR); classes `*Port` em `api` não podem importar entity legada (ERROR); qualquer camada de um módulo importando repository legado fora de `infrastructure` é ERROR. A migração inteira precisa terminar com 0 erros novos deste guard.

## Estrutura alvo

Mesmo padrão de `com.tcc.pjb.backend.modules.acordo` (já existe, 54 arquivos, domain/application/infrastructure/api sem framework vazando entre camadas).

```
com.tcc.pjb.backend.modules.custas
├── domain/          44 records/enum já existentes (Command/Result/View/Snapshot/Query) +
│                    CustaIsencaoPolicy (interface pura) + PixPayloadGenerator (porta interna,
│                    sem consumidor fora do módulo, implementada em infrastructure)
├── application/     CustaJudicialService, CustasApplicationService, CustaIsencaoPorRitoPolicy
├── infrastructure/  entidade CustaJudicial + CustaJudicialRepository (persistence/), CustasConfiguration
├── web/             AdminCustasController
└── api/             GruCodigoBarrasGenerator — a ÚNICA superfície hoje cruzada por outro módulo
                      (WorkflowTrabalhistaService/TrabalhistaWorkflowConfiguration, em
                      core/financeiro/trabalhista/, não modularizado, fora de escopo)
```

**Por que `GruCodigoBarrasGenerator` vai pra `api/` e `PixPayloadGenerator` fica em `domain/`**: os dois são interfaces funcionais puras (zero Spring), implementadas como beans lambda em `CustasConfiguration`. A diferença é quem consome: `GruCodigoBarrasGenerator` é importado por `core/financeiro/trabalhista/WorkflowTrabalhistaService.java` e `TrabalhistaWorkflowConfiguration.java` (2 arquivos fora do módulo) — é contrato cross-module de verdade, pertence a `api/`. `PixPayloadGenerator` não tem nenhum consumidor fora de `core/financeiro/custas/` — é porta interna (padrão hexagonal clássico: interface em domain, implementação da infraestrutura injetada), fica em `domain/`.

## Escopo exato (verificado por grep, não estimado)

**54 arquivos principais movem:**
- 44 em `core/financeiro/custas/domain/*.java` → `modules/custas/domain/`
- `CustaIsencaoPolicy.java`, `PixPayloadGenerator.java` → `modules/custas/domain/`
- `CustaJudicialService.java`, `CustasApplicationService.java`, `CustaIsencaoPorRitoPolicy.java` → `modules/custas/application/`
- `CustasConfiguration.java` → `modules/custas/infrastructure/`
- `model/entity/financeiro/CustaJudicial.java` → `modules/custas/infrastructure/persistence/`
- `model/repository/CustaJudicialRepository.java` → `modules/custas/infrastructure/persistence/`
- `controller/admin/AdminCustasController.java` → `modules/custas/web/`
- `GruCodigoBarrasGenerator.java` → `modules/custas/api/`

**13 arquivos de teste movem/atualizam import junto** (mesma árvore de pacote espelhada em `src/test`):
`CustaIsencaoPorRitoPolicyTest`, `CustaJudicialFlowIT`, `CustaJudicialRepositoryIT`, `CustaJudicialServiceCommandHelpersTest`, `CustaJudicialServiceConsultaTest`, `CustaJudicialServiceIsencaoTest`, `CustaJudicialServicePagamentoTest`, `CustaJudicialServiceSnapshotsTest`, `CustaJudicialServiceTest`, `CustaJudicialServiceViewsTest`, `CustasApplicationServiceTest`, `domain/TipoCustaTest`, `controller/admin/AdminCustasControllerTest`.

**2 arquivos externos (fora do módulo, não movem) só trocam import:**
`core/financeiro/trabalhista/WorkflowTrabalhistaService.java`, `core/financeiro/trabalhista/TrabalhistaWorkflowConfiguration.java` — trocam `import com.tcc.pjb.backend.core.financeiro.custas.GruCodigoBarrasGenerator` por `import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator`, e `import com.tcc.pjb.backend.core.financeiro.custas.domain.GruResult` por `import com.tcc.pjb.backend.modules.custas.domain.GruResult` (`GruResult` fica em `domain/`, não move pra `api/` — `api/` pode depender de `domain/` normalmente, é a direção permitida pelo guard).

**1 arquivo novo:** `CustasArchitectureTest.java`, espelhando `pjb-api/src/test/java/com/tcc/pjb/backend/modules/prazos/PrazosArchitectureTest.java` linha por linha, adaptado pro pacote `modules.custas`.

Confirmado por grep exaustivo (não estimativa): nenhum outro arquivo no repositório referencia `CustaJudicial`, `CustaJudicialRepository`, `core.financeiro.custas.*` fora da lista acima.

## Método de execução — por que `git mv`, não recriação manual

Dado o volume (54 arquivos principais), a forma seguraṇa é `git mv` (preserva histórico, é atômico, elimina risco de erro de transcrição) seguido de edição cirúrgica só da linha `package` e dos imports que mudam — nunca recriar o corpo dos arquivos do zero. Cada tarefa do plano de implementação compila e roda os testes daquela camada antes de prosseguir pra próxima, para que um erro fique isolado à camada onde ele foi introduzido.

## Zero mudança de comportamento

Nenhuma regra de negócio muda. É mover arquivos + corrigir `package`/`import` + adicionar 1 teste de arquitetura novo. Os 13 testes existentes devem continuar passando com as mesmas asserções, só em pacote novo.

## Critério de fechamento

- `./mvnw test-compile -pl pjb-api` limpo.
- Os 13 testes de `custas` verdes na nova localização.
- `python scripts/modular_monolith_guard.py` — 0 errors, nenhum warning novo acima do orçamento do baseline atual (`docs/architecture/modular_monolith_guard_baseline.json`).
- `CustasArchitectureTest` novo, verde.
- Suíte unitária completa (`./mvnw test -pl pjb-api`) sem regressão.
- `docs/architecture/monolith_to_modular_strategy.md` atualizado pra registrar `custas` como segundo módulo com estrutura completa (ao lado de `acordo`).
