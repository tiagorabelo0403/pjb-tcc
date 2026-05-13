# R67 — jornada inteligente de baixo overhead

Entrou uma malha nova de inteligência operacional leve no peticionamento.

## O que entrou

- `PeticionamentoJourneyIntelligenceService`
- `PeticionamentoJourneyIntelligenceAssembler`
- `PeticionamentoJourneyPayloadSupport`
- `PeticionamentoProtocolProgressSupport`
- `PeticionamentoJourneyIntelligenceResponse`
- `PeticionamentoJourneyStepResponse`
- `PeticionamentoJourneyActionResponse`

## Superfícies

- `POST /api/v1/peticionamento/studio/jornada-inteligente`
- `POST /api/v1/peticionamento/studio/wizard-protocolo-simples` agora retorna também `journeyIntelligence`

## O que a leitura inteligente entrega

- fase atual da jornada
- pulso operacional
- score de completude
- sinais observados
- domínios faltantes
- passos com status e automabilidade
- próximas ações com razão operacional
- métricas compactas de execução

## Como ficou leve

- cálculo apenas sob demanda
- sem retenção de sessão
- sem scheduler
- sem polling
- sem trilha contínua em memória
- sem recomputação dupla quando o wizard já montou o contexto

## Resultado

O PJB passa a enxergar em que passo o usuário está, sugerir a próxima ação útil e abrir avanço assistido sem criar custo permanente de CPU ou heap.
