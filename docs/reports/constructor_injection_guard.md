# Constructor Injection Guard

- Base analisada: `C:\PJB\pjb-api\src\main\java\com\tcc\pjb\backend`
- Construtores analisados: **2068**
- Hotspots detectados: **46**

## Hotspots

- `pjb-api\src\main\java\com\tcc\pjb\backend\core\plataforma\substituicao\application\PjbArquiteturaSubstituicaoNacionalApplicationService.java` -> 40 dependências, 523 linhas (service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\processual\comunicacao\institutional\surface\NationalCommunicationInstitutionalSurfaceFacadeService.java` -> 38 dependências, 726 linhas (facade_service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\oficial_justica\OficialJusticaPainelService.java` -> 34 dependências, 812 linhas (service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\processual\comunicacao\institutional\governance\NationalCommunicationInstitutionalGovernanceSurfaceFacadeService.java` -> 33 dependências, 762 linhas (facade_service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\core\kernel\twin\ProcessDigitalTwinService.java` -> 33 dependências, 569 linhas (service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\model\dto\processual\comunicacao\institutional\panel\NationalCommunicationInstitutionalInboxItemResponse.java` -> 31 dependências, 104 linhas (other)
- `pjb-api\src\main\java\com\tcc\pjb\backend\modules\laiane\service\LaianePeticaoAssistService.java` -> 29 dependências, 1145 linhas (service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\core\processo\unificado\domain\ProcessoUnificadoCompetencia.java` -> 27 dependências, 85 linhas (other)
- `pjb-api\src\main\java\com\tcc\pjb\backend\ai\juridica\conversation\LegalAiConversationOrchestrator.java` -> 26 dependências, 274 linhas (other)
- `pjb-api\src\main\java\com\tcc\pjb\backend\core\frontend\app\application\PjbFrontendAppApplicationService.java` -> 25 dependências, 832 linhas (service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\model\dto\processual\recursal\automation\RecursalAutomationRequest.java` -> 25 dependências, 200 linhas (other)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\processual\surface\ProcessoSurfaceFacadeService.java` -> 24 dependências, 728 linhas (facade_service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\delegado\DelegadoPainelService.java` -> 24 dependências, 429 linhas (service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\processual\comunicacao\flow\NationalCommunicationFlowService.java` -> 24 dependências, 281 linhas (service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\integration\judicial\JudicialConnectorPolicyOverlay.java` -> 24 dependências, 99 linhas (other)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\AcordoSuggestionPipelineAsyncService.java` -> 23 dependências, 597 linhas (service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\core\kernel\recursal\mesh\RecursalStateSnapshot.java` -> 23 dependências, 301 linhas (other)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\processual\peticionamento\PeticionamentoSessaoFacadeService.java` -> 22 dependências, 1063 linhas (facade_service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\infra\ScaleArchitectureService.java` -> 22 dependências, 983 linhas (service)
- `pjb-api\src\main\java\com\tcc\pjb\backend\modules\atendimento\service\AtendimentoChatService.java` -> 21 dependências, 586 linhas (service)

## Ações recomendadas

- Extrair collaborators especializados quando facades/services ultrapassarem 12 dependências com mais de 900 linhas.
- Concentrar gateways documentais, drafting e projections em assemblers dedicados.
- Travar regressão com testes de arquitetura focados nos hotspots mais críticos.
