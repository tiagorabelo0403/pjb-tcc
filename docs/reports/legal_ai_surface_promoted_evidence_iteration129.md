# Round 129 — assembler soberano de evidência promovida nas superfícies estruturadas

## Objetivo
Endurecer `/api/ai/legal/minuta` e `/api/ai/legal/grounding/check` para que a montagem do contexto material consuma apenas evidência promovida, sem tratar a cadeia inteira de descritores como lastro elegível.

## Artefatos adicionados
- `LegalAiStructuredSurfaceEvidenceBundle`
- `LegalAiStructuredSurfaceEvidenceAssemblerService`
- `LegalAiStructuredSurfaceEvidenceAssemblerServiceTest`
- `JuridicaLegalAiSurfaceRound129ArchitectureTest`

## Ligações realizadas
- `LegalAiSurfaceFacadeService` passou a montar bundle promovido para minuta e grounding.
- O prompt de minuta agora recebe apenas `promotedEvidenceIds` e `promotedEvidenceDescriptors`.
- O trace/safeguards do grounding agora recebe o bundle promovido materialmente.
- A surface endurece o fluxo quando o status de promoção é `PROMOTED` mas não há âncora promovida.

## Riscos evitados
- promoção ingênua de descritor derivado/não elegível para prompt de minuta
- grounding tratado como estável sem âncora material promovida
- bypass futuro da montagem promovida por consumo direto da lista ampla de descritores

## Validação honesta
- guards Python passaram
- compilação dirigida do lote principal passou
- compilação dirigida dos testes novos passou
- sem build Maven global verde
- sem compile total do `pjb-api`
- sem Docker estável
