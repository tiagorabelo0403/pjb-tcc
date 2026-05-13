#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parent.parent
TEST_ROOT = ROOT / "pjb-api" / "src" / "test" / "java"

violations: list[str] = []
conversation_route = 'post("/api/ai/legal/conversation")'

for path in TEST_ROOT.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    if "surfaceFacadeService.converse(" in text:
        violations.append(f"{path.relative_to(ROOT)} usa surfaceFacadeService.converse para a rota de conversation")
    if conversation_route in text and "LegalAiConversationController" not in text and path.name != "LegalAiSignedOriginTestSupport.java":
        violations.append(f"{path.relative_to(ROOT)} usa /conversation sem montar LegalAiConversationController")

required = [
    TEST_ROOT / "com/tcc/pjb/backend/ai/juridica/api/LegalAiConversationControllerIT.java",
    TEST_ROOT / "com/tcc/pjb/backend/ai/juridica/api/LegalAiKnowledgeControllerIT.java",
    TEST_ROOT / "com/tcc/pjb/backend/contracts/provider/LegalAiControllerProviderContractTest.java",
]
for path in required:
    if not path.exists():
        violations.append(f"arquivo obrigatório ausente: {path.relative_to(ROOT)}")

provider = TEST_ROOT / "com/tcc/pjb/backend/contracts/provider/LegalAiControllerProviderContractTest.java"
if provider.exists():
    text = provider.read_text(encoding="utf-8")
    if "conversationController" not in text or "target.setControllers(controller, conversationController);" not in text:
        violations.append("LegalAiControllerProviderContractTest não está montando a surface dedicada de conversation")

if violations:
    print("LEGAL AI SURFACE SPLIT GUARD: FAIL")
    for violation in violations:
        print(f" - {violation}")
    sys.exit(1)

print("LEGAL AI SURFACE SPLIT GUARD: OK")
