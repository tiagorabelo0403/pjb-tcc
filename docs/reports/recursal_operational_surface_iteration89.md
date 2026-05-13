# Round 89 — surface operacional recursal

## O que entrou
- materialização de uma surface operacional recursal agregada
- resposta dedicada com seções de advogado, institucional, documental e inteligência
- resposta dedicada com lacunas remanescentes explícitas
- novo endpoint canônico em `RecursalRoutes.SURFACES_OPERATIONAL`
- controller e service próprios no eixo recursal
- teste unitário da service
- teste arquitetural de pacotes

## Estrutura criada
- `model/dto/processual/recursal/surface/RecursalOperationalSurfaceResponse`
- `model/dto/processual/recursal/surface/RecursalOperationalSurfaceSectionView`
- `model/dto/processual/recursal/surface/RecursalOperationalSurfaceGapView`
- `service/processual/recursal/surface/RecursalOperationalSurfaceService`
- `controller/processual/recursal/surface/RecursalOperationalSurfaceController`

## O que a surface passou a devolver
- rota prioritária ativa
- nomenclatura ativa do recurso
- bloqueio de poder de recorrer, quando existir
- quatro seções operacionais organizadas:
  - advogado, partes e peticionamento
  - institucional, caixas e secretaria
  - autos digitais, certidões e colaboração documental
  - observabilidade, indexação e avisos
- backlog restante explícito da malha recursal

## Validação honesta
- compilação dirigida com `javac` e stubs mínimos para Spring passou
- runner local da `RecursalOperationalSurfaceService` passou
- `runtime_concurrency_guard.py` passou
- não há afirmação de build Maven global verde
- não há afirmação de compile total do `pjb-api`
- não há afirmação de Docker estável

## O que ainda falta
1. boundaries HTTP especializados por eixo recursal, além da surface agregada
2. provider contracts e ITs dedicados para cada nova superfície
3. viewer/autenticidade/assinatura documental soberanos mais finos
4. governança de avisos móveis sem scheduler paralelo
5. continuidade da recuperação de compile global do `pjb-api`
