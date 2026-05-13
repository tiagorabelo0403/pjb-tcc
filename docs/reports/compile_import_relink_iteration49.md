# Compile import relink — round 49

## Objetivo
Fechar a primeira leva de erros reais de compile apontados no log local do `pjb-api`, sem abrir feature nova.

## Famílias tratadas
- enums de jurisdição e processo em `model/entity`, `model/dto` e jurisprudência
- decisões de juizado deslocadas para `core/processo/juizado/procedural`
- referências a `TribunalRuleEngine` em `tribunal/regras/snapshot`
- espécies de embargos na malha recursal
- `PreclusaoTipo` em admissibilidade recursal
- nested type `PericiaEvidenceReport` em peticionamento de mídia
- DTOs de completude em infraestrutura soberana
- imports de domínio institucional na surface de comunicação

## Arquivos tocados
- 31 arquivos Java
- 1 README

## Limite honesto
Sem cadeia Maven completa no ambiente atual. A validação deste round ficou em religação estrutural, busca focal de símbolos e guards do repositório.
