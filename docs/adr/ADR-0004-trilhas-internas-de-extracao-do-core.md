# ADR-0004 — Trilhas internas de extração do core

## Status
Aceito

## Contexto

Os hotspots do `core` já estavam sendo medidos por tamanho, dependências e pressão de borda, mas o relatório ainda não respondia a pergunta operacional seguinte: por onde começar a quebra de cada hotspot sem cair em renomeação cosmética ou refactor destrutivo.

Sem essa camada intermediária, a equipe continua vendo que `core/comunicacao` e `core/processo` estão pressionados, mas não enxerga quais recortes internos oferecem a melhor primeira fronteira de separação.

## Decisão

- o analisador de aprendizado estrutural passa a gerar trilhas internas de extração por hotspot do `core`
- cada trilha é derivada da primeira subfatia canônica abaixo do hotspot, preservando a linguagem já usada no repositório
- cada trilha informa:
  - volume principal
  - volume de testes
  - razão de testes
  - prontidão para separação (`PRONTA`, `PREPARAR`, `ENDURECER`)
  - sinais operacionais
  - ações iniciais
- o aprendizado deixa de ficar restrito ao endpoint administrativo e passa a ser exposto também em surface processual integrada

## Consequências

- a primeira onda de decomposição pode começar por trilhas internas concretas, e não só por pacotes amplos
- hotspots com pouca cobertura deixam explícito se precisam endurecer antes da extração
- a malha processual ganha leitura arquitetural mais útil para priorização institucional
- o projeto reduz o risco de quebrar `core` por intuição em vez de por dado reexecutável
