# Round 138 — varredura de testes, postura de banco e revisão de blindagem

## O que foi corrigido

- imports reais corrigidos em testes do eixo procedural/juizado:
  - `NationalProceduralJuizadoExclusionResolverTest`
  - `NationalProceduralJuizadoTrackClassifierTest`
  - `NationalProceduralJuizadoTrackResolverTest`
  - `CompatibilidadeEnumsLegadosTest`
- `scripts/import_sanity_probe.py` foi endurecida para:
  - considerar annotations de teste comuns;
  - ignorar falsos positivos vindos de string literals ao verificar tipos conhecidos;
  - servir tanto para `src/main/java` quanto para `src/test/java`
- nova sonda reutilizável: `scripts/test_db_posture_probe.py`
- novo guard: `PjbDatasourceTransportPostureGuardTest`

## Banco de dados e configuração

Nesta rodada eu **não inventei migration**. O motivo é simples: do round 130 ao 138 o que entrou foi governança, filtros, contracts, tests, DTOs e endurecimento de configuração. Não apareceu nova entidade persistente obrigatória ou alteração material de domínio que justificasse migration honesta.

O que eu modernizei de verdade foi a **postura do transporte e do pool**:

- `application.yml`
  - `ApplicationName`
  - `assumeMinServerVersion`
  - `connectTimeout`
  - `sslmode`
  - `sslrootcert`
  - `targetServerType`
- réplica de leitura em `application.yml`
  - `ApplicationName` próprio
  - `sslmode`
  - `sslrootcert`
  - `targetServerType=preferSecondary`
  - `loadBalanceHosts=true`
- `application-docker.yml`
  - hostname interno `postgres`
  - `sslmode` explícito
  - `ApplicationName`
- `application-prod.yml`
  - `sslmode=verify-full`
  - `sslrootcert`
  - `targetServerType=primary`
  - `ApplicationName`

## Leitura honesta de segurança, estabilidade e performance

### Segurança

Está mais forte do que antes porque:
- a borda das APIs já vinha endurecida;
- agora o transporte do datasource também ficou com postura explícita, em vez de depender de defaults implícitos do driver;
- produção passa a declarar `verify-full` por padrão.

### Estabilidade

Está mais previsível porque:
- os testes agora também entram na varredura de imports/visibilidade;
- o datasource ficou mais explícito em `connectTimeout`, `targetServerType` e identificação por `ApplicationName`.

### Performance

Melhorou em observabilidade e roteamento porque:
- leitura e escrita ficam mais fáceis de diferenciar no banco;
- a réplica declara preferência por secundária e pode balancear hosts de leitura;
- isso ajuda troubleshooting sem abrir executor, scheduler ou fila paralelos.

## O que ainda pode melhorar depois

- continuar ampliando `EntityGraph` e specification tests nos bounded contexts mais quentes;
- endurecer `statement_timeout`/`read-only` por lane **se** isso já estiver materializado no builder real do datasource, sem inventar configuração cosmética;
- rodar novo compile real do usuário para pegar o próximo lote honesto de quebras remanescentes.

## Validação honesta desta rodada

- `runtime_concurrency_guard.py`: passou
- `architecture_hygiene_guard.py`: passou
- `constructor_injection_guard.py`: passou
- `config_taxonomy_guard.py`: passou
- `transactional_hotspot_guard.py --fail-on-missing-budgets`: passou
- `repository_layout_guard.py`: passou
- `import_sanity_probe.py` em `src/main/java`: passou
- `import_sanity_probe.py` em `src/test/java`: passou
- `test_db_posture_probe.py`: passou

## Honestidade

- não estou afirmando build Maven global verde
- não estou afirmando compile total do `pjb-api`
- não estou afirmando Docker estável
- não houve validação Git real porque o ZIP continua sem `.git`
