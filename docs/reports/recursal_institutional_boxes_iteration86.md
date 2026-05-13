# Round 86 — representação institucional, caixas e cobertura recursal

## Objetivo
Levar para a malha recursal a organização institucional descrita nos manuais: gestor, distribuidor, representante padrão, caixas de organização, filtros, histórico, cobertura, agenda e pré-pauta.

## Artefatos
- `RecursalInstitutionalOrganizationBlueprint`
- `RecursalInstitutionalOrganizationTrackFactory`
- novos labels formais institucionais em `RecursalFormalSectionLabels`

## Resultado
- nova trilha `ORGANIZACAO_INSTITUCIONAL_RECURSAL`;
- novo passo de playbook `ORQUESTRAR_REPRESENTACAO_E_CAIXAS_INSTITUCIONAIS`;
- Procuradoria e Defensoria passaram a ser tratadas de forma distinta no recursal, respeitando entidade representada x vínculo por processo;
- conexão explícita entre caixas, filtros, cobertura, agenda, pré-pauta e institutional workbench.
