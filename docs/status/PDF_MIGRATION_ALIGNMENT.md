# Alinhamento de migrations do PDF

O conteúdo do PDF foi materializado, mas a numeração das migrations acabou consolidada com adaptação na base atual.

## Resumo

- Entradas esperadas do PDF: **11**
- Entradas resolvidas na base: **11**
- Versões duplicadas de migration detectadas no sweep: **1**

## Mapa PDF → base atual

| PDF | Item | Migration resolvida na base | Status |
|---|---|---|---|
| V178 | ICP-Brasil | `V197__icp_brasil_certificate_chain.sql ` | implementado com adaptação |
| V179 | MNI | `V192__mni_remessa.sql ` | implementado com adaptação |
| V180 | DataJud | `V193__datajud_feed_checkpoint.sql ` | implementado com adaptação |
| V181 | Workflow criminal | `V195__criminal_workflow.sql ` | implementado com adaptação |
| V182 | Integrações financeiras sensíveis | `V198__integracao_judicial_financeira.sql ` | implementado com adaptação |
| V183 | Custas judiciais | `V196__custas_judiciais.sql ` | implementado com adaptação |
| V184 | Workflow trabalhista | `V199__workflow_trabalhista.sql ` | implementado com adaptação |
| V185 | Workflow eleitoral | `V191__workflow_eleitoral.sql ` | implementado com adaptação |
| V186 | DJe | `V189__dje_publicacao.sql ` | implementado com adaptação |
| V187 | Digitalização de acervo | `V194__digitalizacao_acervo.sql ` | implementado com adaptação |
| V188 | Sobrestamento por tema | `V190__sobrestamento_tema.sql ` | implementado com adaptação |

## Duplicidades corrigidas/monitoradas

- `V100`: V100__download_budget_scope.sql, V100__operational_function_credentials.sql, V100__secretariat_queue_row_version.sql
