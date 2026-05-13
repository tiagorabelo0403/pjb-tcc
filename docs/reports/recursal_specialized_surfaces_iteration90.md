# Round 90 — surfaces recursais especializadas

## O que entrou
- materialização de surfaces recursais especializadas por eixo
- resposta dedicada `RecursalSpecializedSurfaceResponse`
- catálogo central de eixos, trilhas e gaps
- suporte central de projeção para aggregate e specialized surfaces
- services próprios para advogado, institucional, documental e inteligência
- controllers próprios para advogado, institucional, documental e inteligência
- novas rotas canônicas no `RecursalRoutes`
- atualização dos testes da surface agregada
- novo teste das surfaces especializadas
- novo teste arquitetural de pacotes

## Estrutura criada
- `model/dto/processual/recursal/surface/RecursalSpecializedSurfaceResponse`
- `service/processual/recursal/surface/RecursalOperationalSurfaceAxisDefinition`
- `service/processual/recursal/surface/RecursalOperationalSurfaceCatalog`
- `service/processual/recursal/surface/RecursalOperationalSurfaceProjectionSupport`
- `service/processual/recursal/surface/RecursalAttorneySurfaceService`
- `service/processual/recursal/surface/RecursalInstitutionalSurfaceService`
- `service/processual/recursal/surface/RecursalDocumentalSurfaceService`
- `service/processual/recursal/surface/RecursalIntelligenceSurfaceService`
- `controller/processual/recursal/surface/RecursalAttorneySurfaceController`
- `controller/processual/recursal/surface/RecursalInstitutionalSurfaceController`
- `controller/processual/recursal/surface/RecursalDocumentalSurfaceController`
- `controller/processual/recursal/surface/RecursalIntelligenceSurfaceController`

## O que mudou na organização
- a lógica de montagem da surface agregada deixou de carregar catálogo local de trilhas e gaps
- o catálogo recursal de surfaces ficou centralizado
- a projeção comum de sections/alerts/gaps passou a ser reutilizável
- cada eixo ganhou boundary HTTP fino próprio

## Validação honesta
- compilação dirigida com `javac` e stubs mínimos para Spring passou
- runner local da surface agregada e da surface do advogado passou
- `runtime_concurrency_guard.py` passou
- não há afirmação de build Maven global verde
- não há afirmação de compile total do `pjb-api`
- não há afirmação de Docker estável

## O que ainda falta
1. provider contracts e ITs por surface especializada
2. viewer/autenticidade/assinatura documental soberanos mais finos
3. governança de avisos móveis sem scheduler paralelo
4. continuidade da recuperação de compile global do `pjb-api`
