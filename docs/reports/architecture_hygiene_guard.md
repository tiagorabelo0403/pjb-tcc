# Architecture Hygiene Guard

- Base analisada: `pjb-api\src\main\java\com\tcc\pjb\backend`
- Arquivos Java: **7722**
- Raízes de pacote: **25**
- Classes acima de 1000 linhas: **19**
- Services/engines acima de 900 linhas: **31**
- Controllers acima de 600 linhas: **0**

## Raízes semânticas duplicadas

- Nenhuma raiz duplicada detectada neste critério.

## Pacotes mais espalhados

- `core.procedural` -> 159 arquivos

## Maiores hotspots de classe

- `pjb-api\src\main\java\com\tcc\pjb\backend\core\comunicacao\judicial\CitacaoIntimacaoEngine.java` -> 1186 linhas (service_or_engine)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\processual\comunicacao\flow\NationalCommunicationFlowFacade.java` -> 1170 linhas (service_or_engine)
- `pjb-api\src\main\java\com\tcc\pjb\backend\platform\jusos\v2\notificacao\NotificacaoInteligentePJB.java` -> 1161 linhas (other)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\professional\ProfessionalInstitutionalAccessGrantAdminService.java` -> 1160 linhas (service_or_engine)
- `pjb-api\src\main\java\com\tcc\pjb\backend\modules\laiane\service\LaianePeticaoAssistService.java` -> 1145 linhas (service_or_engine)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\processual\recursal\ia\RecursalIaPlannerService.java` -> 1119 linhas (service_or_engine)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\secretariat\oficial\SecretariaOficialCumprimentoRoutingService.java` -> 1107 linhas (service_or_engine)
- `pjb-api\src\main\java\com\tcc\pjb\backend\platform\jusos\v2\cooperacao\CooperacaoJuridicaEngine.java` -> 1106 linhas (service_or_engine)
- `pjb-api\src\main\java\com\tcc\pjb\backend\service\processual\peticionamento\PeticionamentoEditorBlueprintCatalogService.java` -> 1098 linhas (service_or_engine)
- `pjb-api\src\main\java\com\tcc\pjb\backend\core\identidade\grafo\application\IdentidadeJuridicaGraphApplicationService.java` -> 1084 linhas (service_or_engine)

## Ações recomendadas

- Concentrar resolução de contexto e montagem de snapshot fora das facades gigantes.
- Extrair assemblers ou projections de controllers e query services acima de 1000 linhas.
- Evitar DTO aninhado sob controller; preferir model.dto ou api.contract por bounded context.
- Usar guardas estáticas no pipeline para impedir regressão de tamanho e espalhamento.
