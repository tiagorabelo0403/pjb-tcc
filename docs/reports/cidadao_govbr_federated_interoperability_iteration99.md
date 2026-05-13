# Round 99 — interoperabilidade federada soberana do cidadão gov.br

## Escopo
Materializar, no eixo do cidadão gov.br, a camada que faltava entre acervo unificado e acesso federado seguro a processos de outros sistemas dentro do PJB.

## Entradas reais da rodada
- `CidadaoGovBrInteroperabilidadeFederadaResponse`
- `CidadaoGovBrAcessoFederadoRequest`
- `CidadaoGovBrAcessoFederadoResponse`
- `GovBrFederatedInteropLabels`
- `CidadaoGovBrInteroperabilidadeFederadaService`
- `CidadaoGovBrInteroperabilidadeFederadaController`
- `CidadaoGovBrInteroperabilidadeFederadaServiceTest`
- `PjbCidadaoGovBrFederatedInteropArchitectureTest`

## O que foi materializado
1. Panorama por sistema judicial mostrando:
   - descoberta por CPF canônico
   - acesso a capa/timeline
   - ponte documental
   - modo de sincronização
   - postura runtime
   - envelope criptográfico
2. Política de acesso por processo/fonte para distinguir:
   - descoberta
   - capa
   - timeline
   - documento
   - mídia
   - necessidade de step-up
   - necessidade de credencial institucional adicional
3. Seleção controlada entre:
   - link federado controlado
   - proxy soberano controlado
   - espelho autorizado soberano
4. Gaps explícitos por fonte para orientar a continuação da interoperabilidade real.

## Segurança aplicada
- reuso do `JudicialConnectorSecurityPackService` como fonte de verdade do envelope criptográfico
- bloqueio quando o runtime do conector está em `BLOCKED` ou `QUARANTINED`
- exigência de step-up quando a fonte ou o processo pedirem assurance maior
- degradação para leitura/control link quando o transporte seguro/document bridge ainda não fecharem
- nenhuma fila paralela, executor paralelo ou scheduler paralelo foi criado

## Estado honesto
- compile dirigido e testes locais do lote novo
- `runtime_concurrency_guard.py` como validação obrigatória
- sem afirmar build Maven global verde
- sem afirmar compile total do `pjb-api`
- sem afirmar Docker estável
