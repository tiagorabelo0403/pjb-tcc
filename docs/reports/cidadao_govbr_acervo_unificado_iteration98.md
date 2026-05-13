# Round 98 — acervo soberano do cidadão por login gov.br

## Objetivo
Transformar o PJB na porta de entrada soberana do cidadão para o seu acervo processual consolidado por CPF canônico, usando o estado do vínculo gov.br e reaproveitando a malha processual nacional já existente.

## Entradas materiais
- `model/dto/cidadao/govbr/CidadaoGovBrAcervoUnificadoResponse.java`
- `service/cidadao/govbr/GovBrCitizenPanelLabels.java`
- `service/cidadao/govbr/CidadaoGovBrAcervoUnificadoService.java`
- `controller/cidadao/CidadaoGovBrAcervoUnificadoController.java`
- `CidadaoGovBrAcervoUnificadoServiceTest`
- `GovBrCitizenPanelLabelsTest`
- `PjbCidadaoGovBrAcervoSurfaceArchitectureTest`

## Resultado funcional
- painel consolidado por CPF canônico;
- leitura organizada por sistema de origem, papel processual e rito;
- semântica de cores centralizada;
- uso do estado gov.br vinculado à identidade nacional;
- endpoint soberano `/api/v1/cidadao/govbr/acervo-unificado`.

## Restrições preservadas
- sem módulo satélite;
- sem scheduler paralelo;
- sem executor paralelo;
- sem fila nova;
- sem strings institucionais espalhadas.
