#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "pjb-api" / "src" / "main" / "java"


def read(rel: str) -> str:
    path = ROOT / rel
    if not path.exists():
        raise SystemExit(f"MISSING FILE: {rel}")
    return path.read_text(encoding="utf-8")


def require_contains(rel: str, fragments: list[str]) -> list[str]:
    text = read(rel)
    issues: list[str] = []
    for fragment in fragments:
        if fragment not in text:
            issues.append(f"{rel}: missing fragment -> {fragment}")
    return issues


def require_absent(rel: str, fragments: list[str]) -> list[str]:
    text = read(rel)
    issues: list[str] = []
    for fragment in fragments:
        if fragment in text:
            issues.append(f"{rel}: forbidden fragment -> {fragment}")
    return issues


def require_type_exists(simple_name: str) -> list[str]:
    matches = list(SRC.rglob(f"{simple_name}.java"))
    if matches:
        return []
    return [f"missing type declaration: {simple_name}"]


def main() -> int:
    issues: list[str] = []

    office = "pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceModeService.java"
    issues += require_absent(office, ["->"])
    issues += require_contains(office, [
        "OfficeWorkspaceMode mode = forcedMode != null ? forcedMode : effectiveMode(",
        "return view;",
    ])

    ops = "pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/operations/NationalCommunicationInstitutionalOperationsController.java"
    issues += require_contains(ops, [
        "import org.springframework.web.bind.annotation.RestController;",
        "import org.springframework.web.bind.annotation.RequestMapping;",
        "import org.springframework.web.bind.annotation.GetMapping;",
        "import org.springframework.web.bind.annotation.PostMapping;",
        "import org.springframework.web.bind.annotation.RequestBody;",
        "import org.springframework.web.bind.annotation.RequestParam;",
    ])

    panel = "pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/panel/NationalCommunicationInstitutionalFinalController.java"
    issues += require_contains(panel, [
        "import org.springframework.web.bind.annotation.RestController;",
        "import org.springframework.web.bind.annotation.RequestMapping;",
        "import org.springframework.web.bind.annotation.GetMapping;",
        "import org.springframework.web.bind.annotation.PostMapping;",
        "import org.springframework.web.bind.annotation.RequestBody;",
        "import org.springframework.web.bind.annotation.RequestParam;",
    ])

    juizado_resolver = "pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoDecisionResolver.java"
    juizado_context = "pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoDecisionContext.java"
    issues += require_contains(juizado_resolver, [
        "import com.tcc.pjb.backend.core.procedural.NationalProceduralActionProfile;",
        "import com.tcc.pjb.backend.core.procedural.NationalProceduralPartyProfile;",
    ])
    issues += require_contains(juizado_context, [
        "import com.tcc.pjb.backend.core.procedural.NationalProceduralActionProfile;",
        "import com.tcc.pjb.backend.core.procedural.NationalProceduralPartyProfile;",
    ])
    issues += require_type_exists("NationalProceduralActionProfile")
    issues += require_type_exists("NationalProceduralPartyProfile")

    audiencia = "pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/AudienciaContexto.java"
    issues += require_contains(audiencia, [
        "import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;",
        "import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;",
        "import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;",
        "import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;",
    ])
    issues += require_type_exists("RitoProcessual")
    issues += require_type_exists("FaseProcessual")
    issues += require_type_exists("EsferaJurisdicao")
    issues += require_type_exists("MateriaJurisdicao")

    root_pom = "pom.xml"
    issues += require_contains(root_pom, [
        "<artifactId>maven-compiler-plugin</artifactId>",
        "<maven.compiler.plugin.version>3.14.1</maven.compiler.plugin.version>",
        "<version>${maven.compiler.plugin.version}</version>",
        "<artifactId>maven-enforcer-plugin</artifactId>",
    ])

    sobrestamento = "pjb-api/src/main/java/com/tcc/pjb/backend/core/judicial/sobrestamento/SobrestamentoTemaService.java"
    issues += require_contains(sobrestamento, [
        "import com.tcc.pjb.backend.model.entity.Processo;",
        "private List<Processo> collectEligibleProcesses(TemaRepercussaoGeral tema)",
    ])


    forum_seed = "pjb-api/src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumAllocationSeedResolver.java"
    forum_profile = "pjb-api/src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumAllocationProfileResolver.java"
    issues += require_contains(forum_seed, [
        "NationalProceduralForumAllocationClassSeed classSeed = classSeedResolver.resolve(context);",
        "NationalProceduralForumAllocationBaseSeed baseSeed = baseSeedResolver.resolve(context);",
        "return profileResolver.resolve(context, classSeed, baseSeed);",
    ])
    issues += require_absent(forum_seed, [
        "profileResolver.resolve(\n                context,\n                classSeed.classeTpu(),",
        "profileResolver.resolve(\n                context,\n                baseSeed.territorial(),",
    ])
    issues += require_contains(forum_profile, [
        "ConfiguracaoDistribuicaoVaraService.PerfilVara resolve(NationalProceduralForumAllocationContext context,",
        "NationalProceduralForumAllocationClassSeed classSeed,",
        "NationalProceduralForumAllocationBaseSeed baseSeed)",
    ])

    wrapper = ".mvn/wrapper/maven-wrapper.properties"
    issues += require_contains(wrapper, [
        "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip",
    ])

    if issues:
        for issue in issues:
            print(f"FAIL: {issue}")
        return 1

    print("OK: uploaded compile regressions are not present in the current snapshot")
    return 0


if __name__ == "__main__":
    sys.exit(main())
