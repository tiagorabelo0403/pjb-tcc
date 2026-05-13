# Round 83 — taxonomia processual unificada recursal

## Objetivo
Trazer para a malha recursal a disciplina de classes, assuntos, movimentos e tipos de petição observada nos manuais do PJe e das Tabelas Processuais Unificadas, sem criar taxonomia paralela.

## Artefatos
- `RecursalProceduralTaxonomyBlueprint`
- `RecursalProceduralTaxonomyTrackFactory`
- novos labels formais de taxonomia em `RecursalFormalSectionLabels`

## Resultado
- nova trilha `TAXONOMIA_PROCESSUAL_UNIFICADA_RECURSAL`;
- novo passo de playbook `ALINHAR_TAXONOMIA_CNJ_E_TIPO_PETICAO`;
- alinhamento explícito entre classe CNJ, assunto material/processual, movimentação real e tipo de petição da espécie recursal;
- reaproveitamento do `de-para` entre legado e shell recursal sem drift taxonômico.
