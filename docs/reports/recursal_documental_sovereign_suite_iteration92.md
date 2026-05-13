# Round 92 — suíte documental soberana recursal

Nesta rodada eu aprofundei o eixo documental do recursal sem abrir cadeia paralela fora do PJB.

Entrou:
- `core/processo/recursal/domain/foundation/RecursalDocumentalLabels`
- `model/dto/processual/recursal/documental/RecursalDocumentalArtifactRequest`
- `model/dto/processual/recursal/documental/RecursalDocumentViewerResponse`
- `model/dto/processual/recursal/documental/RecursalDocumentAuthenticityResponse`
- `model/dto/processual/recursal/documental/RecursalDocumentSignatureEvidenceResponse`
- `service/processual/recursal/documental/RecursalDocumentalSovereignSuiteService`
- `controller/processual/recursal/documental/RecursalDocumentalSovereignSuiteController`

Rotas materializadas:
- `/api/v1/processual/recursal/document-viewer`
- `/api/v1/processual/recursal/document-authenticity`
- `/api/v1/processual/recursal/document-signature-evidence`

A suíte nova faz três coisas de forma coesa:
1. projeta visualização documental com hash, política de acesso, modo de visualização e rotas relacionadas;
2. projeta autenticidade com envelope de prova, modo de validação, rota pública de conferência e linha de assinatura;
3. projeta evidência de assinatura com modo de assinatura, temporalidade, LTV e cadeia mínima de certificados.

Conexões preservadas:
- reutiliza a surface documental recursal já existente;
- reutiliza o catálogo de surfaces do recursal;
- mantém a mesma cadeia documental do PJB;
- não cria scheduler, executor ou pipeline documental paralelo.

Validação honesta:
- compilação dirigida com `javac` e stubs mínimos passou para service e controller novos;
- runner local da suíte documental passou;
- `runtime_concurrency_guard.py` passou;
- não há afirmação de build Maven global verde;
- não há afirmação de compile total do `pjb-api`;
- não há afirmação de Docker estável.

O que ainda falta depois desta rodada:
1. validação externa federada mais fina para assinatura institucional em fronteiras soberanas;
2. governança mobile/notificacional sem scheduler paralelo;
3. continuidade da recuperação de compile global do `pjb-api`.
