# ADR-0002 — Isolamento do legado judge e gates de qualidade executáveis

Status: Aceita
Data: 2026-04-03

## Contexto

O PJB já possui uma diretriz para novas adições usarem inglês como padrão, com exceções controladas para termos jurídicos brasileiros e pacotes legados estabilizados. Mesmo assim, a base ainda mantém uma ponte histórica em `service.judge`, enquanto a evolução real do domínio judicial ocorreu majoritariamente em `service.juiz`.

Sem uma decisão complementar, o risco não é apenas estético:
- novas classes podem voltar a crescer dentro do legado `judge`
- imports transitórios podem se espalhar para fora do roteador canônico
- cobertura e fronteiras arquiteturais podem continuar existindo apenas como intenção documental, não como verificação executável

Ao mesmo tempo, o PJB precisa endurecer sua governança sem depender de features preview do Java 21 para o fluxo principal de build. A linha estável deve privilegiar virtual threads centralizadas, observabilidade e gates reproduzíveis no Maven.

## Decisão

- `service.judge` passa a ser tratado formalmente como legado transitório isolado.
- o único ponto canônico autorizado a importar `com.tcc.pjb.backend.service.judge.*` é `JudgeDocketController`
- novas capacidades judiciais devem seguir em `service.juiz` ou em bounded contexts explícitos do domínio
- o crescimento do pacote legado `judge` passa a ser bloqueado por testes de governança e pela auditoria estrutural do código
- o build Maven passa a acoplar cobertura por JaCoCo em testes unitários e de integração
- o endurecimento arquitetural por Checkstyle fica governado por profile explícito `quality-gates`, preservando adoção incremental sem quebrar o fluxo base de build local

## Diretrizes

- não haverá renomeação massiva retroativa apenas para apagar a palavra `judge`
- toda ponte residual entre `judge` e `juiz` deve ser curta, auditável e justificada
- strings de governança, mensagens operacionais e sinais de qualidade devem preferir classes dedicadas ou catálogos específicos, evitando espalhamento de literais pela superfície oficial
- cobertura mínima e fronteiras de importação devem ser tratadas como ativos operacionais do projeto, não como documentação opcional
- quando houver conflito entre limpeza nominal imediata e estabilidade do domínio, prevalece a estabilidade com isolamento governado

## Consequências

- reduz a chance de bifurcação semântica entre `judge` e `juiz`
- transforma JaCoCo e Checkstyle em instrumentos mensuráveis de qualidade, e não em recomendação solta
- prepara a base para evoluções futuras com métricas reais antes de modularizações maiores
- evita correções cosméticas que escondem o problema em vez de governá-lo
- melhora a legibilidade arquitetural para quem entra no projeto e precisa identificar rapidamente o que é legado, o que é canônico e o que é gate de proteção
