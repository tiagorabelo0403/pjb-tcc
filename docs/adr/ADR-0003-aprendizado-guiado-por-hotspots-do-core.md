# ADR-0003 — Aprendizado guiado por hotspots do core

Status: Aceita
Data: 2026-04-03

## Contexto

O PJB já possui cobertura e gates de higiene acoplados ao ciclo Maven, além do isolamento formal do legado `judge`. Mesmo assim, a base ainda corre o risco de cair em um padrão clássico de monólito sem governança de crescimento: a equipe percebe que `core` está grande, mas não mede quais fatias concentram mais pressão estrutural, mais consumo por controllers e mais dívida de testes.

Sem esse aprendizado operacional, a próxima rodada tende a errar a prioridade:
- gasta energia em renomeação cosmética
- tenta extrair blocos pequenos demais antes dos hotspots reais
- rompe contratos HTTP antes de estabilizar a borda
- mexe no legado `judge` mesmo ele já estando contido, enquanto `core/comunicacao`, `core/processo` e `core/kernel` seguem carregando o peso maior

## Decisão

- o projeto passa a expor um analisador interno de aprendizado estrutural do core
- a unidade de leitura passa a ser a fatia `primeiro-segmento/segundo-segmento`, priorizando hotspots como `core/comunicacao`, `core/processo`, `core/kernel`, `core/security` e correlatos
- a priorização não será feita apenas por quantidade de arquivos, mas também por dependências entrantes, dependências de saída, pressão de controllers e razão de testes
- a saída dessa leitura será usada para definir ondas de decomposição, começando por contratos de borda e fachadas canônicas antes de qualquer quebra profunda

## Diretrizes

- `controller` continua sendo tratado como pressão de surface HTTP, não como primeiro alvo de extração
- a primeira onda deve mirar os hotspots com maior pressão composta e menor segurança de testes
- a segunda onda só avança depois que a primeira reduzir risco na borda, na fachada e nos contratos internos
- mensagens e aprendizados operacionais devem ficar centralizados em classes próprias, evitando espalhamento de literais
- o legado `judge` segue fora da disputa de prioridade enquanto permanecer contido na ponte canônica

## Consequências

- a modularização futura deixa de ser intuitiva e passa a ser guiada por dados do próprio repositório
- refactors do núcleo ganham trilha de aprendizado reexecutável dentro do projeto
- a equipe reduz a chance de atacar o pacote errado na ordem errada
- o PJB passa a distinguir melhor entre problema de nome, problema de acoplamento e problema de dívida de testes
