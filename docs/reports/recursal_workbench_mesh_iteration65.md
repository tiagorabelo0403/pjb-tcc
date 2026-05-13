# Round 65 — malha real de painéis e workbenches por órgão julgador

## Objetivo
Conectar o handoff recursal às superfícies reais já existentes no PJB, evitando dashboard recursal paralelo e duplicação de workbench.

## Entradas principais
- `RecursalWorkbenchSurfaceCatalog`
- `RecursalAdjudicationWorkbenchBlueprint`
- `RecursalAdjudicationWorkbenchTrackFactory`
- `RecursalAutomationPlaybookService`
- `RecursalAutomationWorkspaceService`

## Superfícies reutilizadas
- `/api/v1/magistratura/atos`
- `/api/v1/distribuicao/processual/workbench`
- `/api/v1/institucional/workbench`
- painéis de gabinete, colegiado e corte superior expostos por `OperationalApiRoutes`
- superfícies operacionais de secretaria/colegiado já existentes

## Resultado estrutural
O recursal agora consegue dizer:
- qual família de órgão recebe a caneta jurisdicional;
- em qual painel real do PJB o processo precisa aparecer;
- qual workbench de magistratura, distribuição, secretaria e institucional precisa ser reutilizado;
- quando a rota excepcional precisa também reaparecer na borda de corte superior.

## Honestidade operacional
- guards Python executadas sem novos achados materiais;
- compilação dirigida com `javac` nas classes alteradas: `SMOKE_OK`;
- sem afirmação de build Maven global verde;
- sem afirmação de compile total do `pjb-api`.
