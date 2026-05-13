# Round 51 — hardening de paths e assertions em testes

- testes escaneados: 1050
- referências inválidas a `.java` em `Path.of(...)`: 0
- arquivos de teste usando `assert*` sem import estático de JUnit: 0

## Correções aplicadas
- normalização de `pjb-api/src/main/java/...` para `src/main/java/...` em testes do módulo `pjb-api`;
- atualização de caminhos movidos do eixo `core/processo/juizado/procedural`;
- atualização de caminhos movidos de DTOs de `processual/comunicacao/institutional/*`;
- atualização do caminho de `FederalismoRedistribuicaoService` para `service/ajuizamento/federal`;
- correção de referência residual antiga do controller intertribunal em `PjbSurfaceRound87ProcessualMarketplaceResidualDisciplineTest`;
- inclusão de `import static org.junit.jupiter.api.Assertions.*;` onde o teste usava `assert*` sem qualificação.
