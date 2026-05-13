from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
ENTITY_ROOT = ROOT / "pjb-api" / "src" / "main" / "java" / "com" / "tcc" / "pjb" / "backend" / "model" / "entity"

MODULE_BY_SEGMENT = {
    "security": "IDENTIDADE_SEGURANCA",
    "identity": "IDENTIDADE_SEGURANCA",
    "api": "INTEGRACOES_EXTERNAS",
    "gov": "INTEGRACOES_EXTERNAS",
    "judicial": "INTEGRACOES_EXTERNAS",
    "federalismo": "INTEGRACOES_EXTERNAS",
    "atlas": "INTEGRACOES_EXTERNAS",
    "institucional": "COMUNICACOES",
    "secretariat": "COMUNICACOES",
    "notification": "COMUNICACOES",
    "calendar": "PRAZOS_AGENDA",
    "recursalmesh": "PRAZOS_AGENDA",
    "ai": "INTELIGENCIA_APLICADA",
    "intelligence": "INTELIGENCIA_APLICADA",
    "jurisprudencia": "INTELIGENCIA_APLICADA",
    "radar": "INTELIGENCIA_APLICADA",
    "document": "DOCUMENTOS",
    "upload": "DOCUMENTOS",
    "icp": "DOCUMENTOS",
    "pericia": "DOCUMENTOS",
    "digitalizacao": "DOCUMENTOS",
    "competencia": "COMPETENCIA_ROTEAMENTO",
    "forum": "COMPETENCIA_ROTEAMENTO",
    "auditoria": "AUDITORIA_OBSERVABILIDADE",
    "infra": "AUDITORIA_OBSERVABILIDADE",
    "outbox": "AUDITORIA_OBSERVABILIDADE",
    "governance": "AUDITORIA_OBSERVABILIDADE",
    "painel": "AUDITORIA_OBSERVABILIDADE",
    "executionmesh": "AUDITORIA_OBSERVABILIDADE",
}

BASE_OVERRIDES = {
    "AuditoriaAcesso.java": "AUDITORIA_OBSERVABILIDADE",
    "AuditoriaEvento.java": "AUDITORIA_OBSERVABILIDADE",
    "MetadadosSistema.java": "AUDITORIA_OBSERVABILIDADE",
    "KernelDecisionEvent.java": "AUDITORIA_OBSERVABILIDADE",
    "EventoInstitucional.java": "COMUNICACOES",
    "NotificationHistory.java": "COMUNICACOES",
    "ChatMensagem.java": "COMUNICACOES",
    "Equipe.java": "COMUNICACOES",
    "MembroEquipe.java": "COMUNICACOES",
    "InstitutionalPolicyProfile.java": "COMUNICACOES",
    "OrgaoJudiciario.java": "COMPETENCIA_ROTEAMENTO",
    "Estados.java": "COMPETENCIA_ROTEAMENTO",
    "Municipios.java": "COMPETENCIA_ROTEAMENTO",
    "Jurisdicao.java": "COMPETENCIA_ROTEAMENTO",
    "Usuario.java": "IDENTIDADE_SEGURANCA",
    "Profile.java": "IDENTIDADE_SEGURANCA",
    "CredencialAcesso.java": "IDENTIDADE_SEGURANCA",
    "ModeloContrato.java": "DOCUMENTOS",
    "EspecialidadePericial.java": "DOCUMENTOS",
    "ProcessIntelligenceSnapshot.java": "INTELIGENCIA_APLICADA",
    "NegotiationRoundSnapshot.java": "INTELIGENCIA_APLICADA",
}

IMPORTS = (
    "import com.tcc.pjb.backend.core.modularity.PjbModuleId;\n"
    "import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;\n"
    "import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;\n"
)


def module_for(path: Path) -> str:
    relative = path.relative_to(ENTITY_ROOT)
    if len(relative.parts) == 1 and path.name in BASE_OVERRIDES:
        return BASE_OVERRIDES[path.name]
    for part in relative.parts[:-1]:
        module = MODULE_BY_SEGMENT.get(part)
        if module:
            return module
    lowered = path.name.lower()
    if any(token in lowered for token in ("audit", "auditoria", "ledger", "telemetria", "checkpoint", "outbox")):
        return "AUDITORIA_OBSERVABILIDADE"
    if any(token in lowered for token in ("usuario", "security", "credencial", "perfil", "avatar", "auth", "webauthn", "passkey")):
        return "IDENTIDADE_SEGURANCA"
    if any(token in lowered for token in ("document", "documento", "upload", "pdf", "certidao")):
        return "DOCUMENTOS"
    return "PROCESSO_LIFECYCLE"


def ensure_imports(text: str) -> str:
    if "import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;" in text:
        return text
    return re.sub(r"(package [^;]+;\n)", r"\1\n" + IMPORTS, text, count=1)


def main() -> None:
    changed = 0
    scanned = 0
    for path in sorted(ENTITY_ROOT.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        if "@Entity" not in text:
            continue
        scanned += 1
        if "@PjbDataOwnership" in text:
            continue
        module = module_for(path)
        updated = ensure_imports(text)
        annotation = f"@PjbDataOwnership(module = PjbModuleId.{module}, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)\n"
        updated = re.sub(r"(?m)^@Entity\b", annotation + "@Entity", updated, count=1)
        if updated != text:
            path.write_text(updated, encoding="utf-8")
            changed += 1
    print(f"entities_scanned={scanned}")
    print(f"entities_changed={changed}")


if __name__ == "__main__":
    main()
