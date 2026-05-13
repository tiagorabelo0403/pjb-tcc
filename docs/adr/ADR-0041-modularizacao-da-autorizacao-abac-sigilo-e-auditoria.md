# ADR-0041 — modularização da autorização ABAC, sigilo e auditoria

## Status
Aceito

## Contexto

`PjbAuthorizationService` havia se tornado um concentrador excessivo de responsabilidades críticas. No mesmo arquivo coexistiam:

- decisão ABAC de leitura processual e documental
- elevação de sigilo documental
- exigência de credencial forte para sigilo alto
- auditoria de leitura e escrita
- matriz de acesso contextual a integrações externas

Essa concentração aumentava o risco de regressão em uma fronteira de segurança sensível e dificultava governança estrutural, testes específicos e futura evolução para um modelo ainda mais forte de zero trust contextual.

## Decisão

O serviço principal foi mantido como superfície pública estável, mas o miolo foi separado em colaboradores dedicados:

- `PjbAuthorizationDecisionContextResolver`
- `PjbAuthorizationPolicyFacade`
- `PjbAuthorizationSigiloResolver`
- `PjbAuthorizationAuditFacade`
- `PjbAuthorizationExternalSystemAccessPolicy`

Também foi introduzido o teste estrutural `PjbAuthorizationServiceStructuralSeparationTest` para impedir o retorno da lógica de sigilo, auditoria e acesso externo ao corpo central do serviço.

## Consequências

### Positivas

- a fronteira ABAC fica mais legível e auditável
- step-up por sigilo alto passa a ter eixo próprio de endurecimento
- auditoria de leitura e escrita deixa de ficar espalhada
- a matriz de acesso a integrações externas fica preparada para evolução por capacidade
- o serviço principal fica mais próximo do padrão de orquestrador curto adotado no restante do monólito modular

### Negativas

- a complexidade total continua existindo, agora explicitada em mais arquivos
- futuras mudanças no contrato interno exigirão atenção coordenada entre colaboradores especializados
