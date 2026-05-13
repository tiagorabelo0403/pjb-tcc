# Fechamento final do PDF — estado honesto

## O que foi corrigido nesta rodada

- duplicidade real de migration Flyway corrigida:
  - `V149__user_calendar_preferences_and_contexts.sql` foi renumerada para `V202__user_calendar_preferences_and_contexts.sql`
- matriz final item por item do PDF gerada em:
  - `docs/PDF_FINAL_MATRIX.md`
  - `docs/reports/pdf_final_matrix.json`
- alinhamento entre versões do PDF e numeração real das migrations documentado em:
  - `docs/PDF_MIGRATION_ALIGNMENT.md`
  - `docs/reports/migration_alignment_report.json`
- sweep estático final atualizado para detectar duplicidade de versions Flyway

## Estado final mais preciso

- Macroblocos do PDF estruturalmente presentes: **25/25**
- Itens marcados como implementados sem adaptação: **17**
- Itens marcados como implementados com adaptação: **8**
- Itens ausentes como macrobloco principal: **0**

## O que ainda não foi provado neste ambiente

- build Maven global
- teste unitário global
- teste de integração global

## Arquivos-chave

- `docs/PDF_FINAL_MATRIX.md`
- `docs/PDF_MIGRATION_ALIGNMENT.md`
- `docs/reports/pdf_final_matrix.json`
- `docs/reports/migration_alignment_report.json`
- `docs/reports/final_static_sweep_report.json`
