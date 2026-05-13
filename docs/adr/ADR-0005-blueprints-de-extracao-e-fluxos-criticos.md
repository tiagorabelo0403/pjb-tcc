# ADR-0005 — blueprints de extração e fluxos críticos

## Status
Aceito

## Contexto
O PJB já passou a medir hotspots do core e trilhas internas de extração, mas ainda faltava converter essa leitura em duas saídas mais operacionais:

1. blueprints mínimos para começar a separar uma trilha sem refactor cego;
2. leitura explícita da cobertura institucional dos fluxos críticos ponta a ponta.

A ausência desse segundo eixo era especialmente grave porque o projeto ainda possui poucos `*IT.java` quando comparado ao volume do backend. Sem medir fluxos críticos, o time enxerga quantidade de testes, mas não enxerga quais jornadas processuais continuam desprotegidas.

## Decisão
O relatório de aprendizado estrutural passa a incluir:

- `blueprintsExtracao`, com:
  - fatia
  - trilha
  - prontidão
  - score de prioridade
  - pacote-alvo sugerido
  - fachada sugerida
  - porta sugerida
  - contrato inicial de integração
  - bloqueios
  - primeiras ações
- `fluxosCriticos`, com leitura heurística sobre fluxos institucionais como:
  - `peticao-triagem-secretaria-gabinete-decisao-publicacao`
  - `protocolo-24x7-integridade`
  - `intimacao-multicanal-ciencia`
  - `prazo-painel-alerta`

## Consequências
Positivas:

- a extração do core deixa de ser só diagnóstico e passa a virar trilha de execução
- a dívida de testes de integração deixa de ser abstrata e passa a ser percebida por jornada institucional
- a priorização das próximas ondas fica mais aderente ao produto real do Judiciário

Custos:

- a leitura de fluxos críticos é heurística e depende da nomenclatura dos testes
- os blueprints são sugestões de governança e não substituem revisão arquitetural humana

## Diretriz de uso
A primeira onda de separação deve privilegiar trilhas `PRONTA` e `PREPARAR` com blueprint explícito e, ao mesmo tempo, aumentar o lastro dos fluxos críticos que estejam `AUSENTE` ou `PARCIAL` antes de promover quebras estruturais mais amplas.
