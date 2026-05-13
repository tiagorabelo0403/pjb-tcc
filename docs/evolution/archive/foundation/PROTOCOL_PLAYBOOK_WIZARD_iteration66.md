Round 66 consolidou três blocos estruturais no mesmo zip completo:

1. playbook operacional nacional por rito e competência
- NationalProceduralOperationalPlaybookService
- snapshot e detalhe por rito
- etapas operacionais tipadas para triagem, competência, partes, prova, pedidos, protocolo e pós-protocolo
- checklist pré-protocolo, âncoras de unidade, documentos mínimos e garantias

2. malha de variações por tribunal e unidade
- NationalProceduralTribunalVariationService
- snapshot de tribunais de referência
- detalhe por tribunal, unidade, rito e justiça
- resolução orientada por conector, exigência de step-up, certificado, canais de protocolo e regras locais

3. wizard simples de protocolo
- PeticionamentoSimpleProtocolWizardService
- endpoint POST /api/v1/peticionamento/studio/wizard-protocolo-simples
- devolve status, próximos passos, prévia de protocolo, playbook e variação local do tribunal/unidade

Superfícies novas:
- GET /api/v1/admin/scale-architecture/judicial-procedural-playbooks
- GET /api/v1/admin/scale-architecture/judicial-procedural-playbooks/{rito}
- GET /api/v1/admin/scale-architecture/judicial-tribunal-variations
- GET /api/v1/admin/scale-architecture/judicial-tribunal-variations/{tribunalCodigo}/{rito}
- GET /api/v1/processual/unificado/playbook-operacional-ritos
- GET /api/v1/processual/unificado/playbook-operacional-ritos/{rito}
- GET /api/v1/processual/unificado/variacoes-tribunal-unidade/{tribunalCodigo}/{rito}
- POST /api/v1/peticionamento/studio/wizard-protocolo-simples

Testes adicionados:
- NationalProceduralOperationalPlaybookServiceTest
- NationalProceduralTribunalVariationServiceTest
