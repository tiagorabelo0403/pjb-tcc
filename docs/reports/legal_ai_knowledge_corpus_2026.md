# Legal AI Knowledge Corpus — Governança Inicial

## Objetivo

Estruturar a IA jurídica do PJB para trabalhar com um corpus hierarquizado e auditável, capaz de combinar:

- Constituição e legislação oficial consolidada
- ritos e fluxos processuais internos do PJB
- jurisprudência oficial dos tribunais superiores
- súmulas, OJs e enunciados oficiais
- doutrina e livros apenas por licença, upload controlado ou acervo institucional

## Lanes de armazenamento e ranking

- `NORMATIVE_TEXT`
- `PRECEDENT`
- `BINDING_STATEMENT`
- `GOVERNANCE_NORM`
- `DOCTRINE`

## Fontes oficiais catalogadas na base inicial

- Constituição Federal de 1988 — Planalto
- Portal da Legislação / Códigos — Planalto
- Jurisprudência do STF
- Súmulas Vinculantes do STF
- Jurisprudência do STJ
- Jurisprudência, Súmulas, OJs e Precedentes Normativos do TST
- Súmulas e Jurisprudência do TSE
- Atos Normativos do CNJ

## Política de doutrina

Doutrina não deve ser tratada como fonte primária automática. O PJB só deve promover livros, comentários e manuais para grounding quando houver:

- licença válida
- comprovação de titularidade
- acervo institucional controlado
- upload autorizado com metadados completos

## Superfícies adicionadas

- `GET /api/ai/legal/knowledge/sources`
- `POST /api/ai/legal/knowledge/coverage`

## Integração conversacional

A conversa jurídica passa a carregar `conversationKnowledgeCoverage` no contexto retornado, permitindo:

- saber quais ramos foram inferidos
- ver quais fontes oficiais estão priorizadas
- separar lane oficial de lane doutrinária
- orientar próximos passos de grounding e ingestão
