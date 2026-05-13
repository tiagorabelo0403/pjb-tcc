# Round 63 — Matriz de capacidades da secretaria multigrau

## Escopo

Reforço do eixo recursal para garantir que o núcleo funcional da secretaria de primeiro grau seja reaplicado no órgão recursal competente, inclusive em rota excepcional com presidência/vice e corte superior, sem perder a simetria com a secretaria institucional.

## Entradas principais

- `RecursalSecretariatCapabilityMatrixBlueprint`
- `RecursalSecretariatCapabilityMatrixTrackFactory`
- ampliação de `RecursalAutomationWorkspaceService`
- ampliação de `RecursalAutomationPlaybookService`
- expansão de `RecursalFormalSectionLabels`
- reforço de `RecursalAutomationWorkspaceServiceTest`

## Capacidades nucleares preservadas

- agenda e calendário
- comunicação e intimação
- mesa de exceções
- cobertura e substituição
- catálogo formal
- pós-julgamento e nova janela recursal
- espelho institucional para MP, Defensoria e Procuradoria

## Validação honesta

- guards Python executadas sem findings novos
- compilação dirigida de arquivos principais alterados com `javac` local
- smoke local: `ACORDAO -> RECURSO_ESPECIAL`, com presença da trilha `MATRIZ_CAPACIDADES_SECRETARIA_MULTIGRAU`
- sem afirmação de build Maven global verde
