# Universal Digital Core & Zero-Error Triage

O PJB trata Juizados Especiais, Núcleos 4.0 e demais ritos digitais como contexto de distribuição e de painel, não como módulos isolados. A plataforma preserva um único fluxo de processo e aplica `RitoContext` para prazos, movimentos, nomenclaturas, validações, atalhos e permissões de interface.

## Decisão de arquitetura

A evolução usa os pacotes existentes de distribuição, sustentação digital, preflight, peticionamento, frontend público, custas, segurança e observabilidade. Nenhum módulo paralelo de Juizado, PJe, e-SAJ, eproc, Creta ou Projudi foi criado.

## Núcleos Digitais Universais

A hierarquia operacional é:

```text
Rito > Unidade/Vara > Comarca > Tribunal
```

O roteamento considera classe, assunto, valor da causa, disponibilidade de Juízo 100% Digital, aceite da parte autora, oposição tempestiva da parte demandada, complexidade probatória, vulnerabilidade e urgência.

## Painel contextual

O painel é único. O backend entrega um conjunto de capacidades compatível com o rito:

```text
prazos
movimentos visíveis
movimentos ocultos
atalhos
políticas de custas
validações prévias
alertas de revisão humana
```

## Triagem Zero Erros

A triagem opera em três camadas:

```text
regras determinísticas
inteligência documental assistiva
sugestões revisáveis por humano
```

A plataforma bloqueia erro estrutural, alerta divergência semântica, sinaliza competência territorial provável, impede custas iniciais indevidas no Juizado de primeiro grau e prioriza urgências por heatmap auditável.

## IA e OCR

A decisão arquitetural é manter o domínio em Java 21. Recursos de OCR, semântica documental e IA devem entrar por ports/adapters Java e eventos assíncronos, sem criar microsserviço obrigatório em outra linguagem.

## Redistribuição fluida

Quando a causa deixa de ser compatível com o rito digital, o PJB altera o `RitoContext`, registra a razão, notifica as partes e encaminha o processo para a unidade ordinária correspondente sem exigir novo protocolo.
