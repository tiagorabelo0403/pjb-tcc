from pathlib import Path

root = Path(__file__).resolve().parents[1]

def read(relative):
    return (root / relative).read_text(encoding="utf-8")

def require(condition, message):
    if not condition:
        raise SystemExit(message)

route_files = list((root / "pjb-api/src/main/java").rglob("NationalCommunicationInstitutionalHttpRoutes.java"))
require(len(route_files) == 1, f"NationalCommunicationInstitutionalHttpRoutes duplicado: {route_files}")
require("service/processual/comunicacao/institutional/surface" in route_files[0].as_posix(), "registry HTTP institucional fora da surface")

controllers_root = root / "pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional"
for source in controllers_root.rglob("*Controller.java"):
    content = source.read_text(encoding="utf-8")
    require("controller.processual.comunicacao.institutional.NationalCommunicationInstitutionalHttpRoutes" not in content, f"controller usando registry duplicado: {source}")
    require("service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes" in content, f"controller institucional sem registry HTTP de surface: {source}")
    require("@RequestMapping(NationalCommunicationInstitutionalHttpRoutes.CANONICAL_BASE)" in content, f"controller institucional fora da base canônica: {source}")
    require("LEGACY_BASE" not in content, f"controller institucional ainda expõe alias legado: {source}")

state_machine = read("pjb-api/src/main/java/com/tcc/pjb/backend/core/kernel/recursal/mesh/NationalRecursalStateMachine.java")
require("case SUSTENTAR -> RecursalLifecycleState.JULGAMENTO_COLEGIADO;" in state_machine, "SUSTENTAR não retorna ao julgamento colegiado")

hearing = read("pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/InstitutionalHearingSchedulingCapabilityResolver.java")
require("nominationRole != InstitutionalNominationRole.TITULAR_INSTITUCIONAL" in hearing, "titular institucional não pode virar finding de coordenação de secretaria")

pdf_ltv = read("pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/recursal/pdf/RecursalPdfLongTermValidationService.java")
require("EMBEDDED_RFC3161_DOCUMENT_TIMESTAMP_MOCK" in pdf_ltv, "timestamp documental mockado não materializado")
require(pdf_ltv.count("markEmbeddedMockDocumentTimestamp(") >= 2, "helper de timestamp documental mockado ausente")
require("documentTimestampEmbedded" in pdf_ltv, "timestamp documental não declarado")

pdf_validation = read("pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/recursal/pdf/RecursalPdfArtifactValidationService.java")
require("REAL_CERTIFICATE_SIGNATURE_REQUIRED" in pdf_validation, "validação PDF não preserva exigência de certificado real")

data_plane = read("pjb-api/src/main/java/com/tcc/pjb/backend/configs/datasource/PjbInstitutionalDataPlaneFilter.java")
require("stateBundle.entryActivationBundle()" in data_plane, "data plane deve reutilizar ativação materializada no state bundle")

print("round28ab_regression_probe OK")
