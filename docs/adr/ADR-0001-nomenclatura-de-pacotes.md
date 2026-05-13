# ADR-0001 — Nomenclatura de pacotes para novas adições

Status: Aceita
Data: 2026-03-25

## Contexto

O PJB possui histórico misto de nomenclatura em português e inglês. A ausência de uma decisão explícita aumenta a fragmentação entre camadas, dificulta busca global, revisão arquitetural e aplicação consistente de regras de importação.

## Decisão

Novos pacotes devem usar inglês como padrão.

Exceções permitidas:
- termos jurídicos brasileiros cujo nome normativo ou institucional seja melhor preservado em português
- integrações cujo contrato externo já use nomenclatura oficial em português
- pacotes legados já estabilizados, que não serão renomeados por causa desta decisão

## Diretrizes

- novos bounded contexts, adapters, facades, orchestrators e policies usam inglês
- nomes de classes podem permanecer em português quando representam conceito jurídico material do domínio brasileiro
- não haverá reescrita massiva retroativa apenas para uniformização nominal
- quando houver dúvida entre clareza técnica e fidelidade normativa, prevalece a clareza arquitetural com documentação local do conceito

## Consequências

- reduz crescimento desordenado da mistura PT/EN
- permite evolução incremental sem renomeação em massa
- preserva compatibilidade com o legado já consolidado
- facilita futuras regras de governança arquitetural e revisão automática
