#!/usr/bin/env python3
from __future__ import annotations
import json
from pathlib import Path
import re
import hashlib

from project_roots import ROOT, SRC_MAIN, SRC_TEST, CORE_MAIN, RES_MAIN

API_POM = ROOT / 'pjb-api' / 'pom.xml'
ROOT_POM = ROOT / 'pom.xml'
CORE_POM = ROOT / 'pjb-core' / 'pom.xml'
RUNTIME_GUARD_SCRIPT = ROOT / 'scripts' / 'runtime_concurrency_guard.py'
RUNTIME_GUARD_REPORT = ROOT / 'docs' / 'reports' / 'runtime_concurrency_guard.json'

JAVA_GLOB = '**/*.java'


def file_hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rels(base: Path, paths: list[Path]) -> list[str]:
    return sorted(str(p.relative_to(base)) for p in paths)


def text(path: Path) -> str:
    return path.read_text(encoding='utf-8') if path.exists() else ''


def main() -> None:
    root_pom = text(ROOT_POM)
    api_pom = text(API_POM)
    core_pom = text(CORE_POM)

    root_java = list(SRC_MAIN.glob(JAVA_GLOB))
    test_java = list(SRC_TEST.glob(JAVA_GLOB))
    core_java = list(CORE_MAIN.glob(JAVA_GLOB))

    extracted_pkg_root = list((SRC_MAIN / 'com' / 'tcc' / 'pjb' / 'backend' / 'core' / 'modularity').glob('*.java')) if (SRC_MAIN / 'com' / 'tcc' / 'pjb' / 'backend' / 'core' / 'modularity').exists() else []
    extracted_pkg_core = list((CORE_MAIN / 'com' / 'tcc' / 'pjb' / 'backend' / 'core' / 'modularity').glob('*.java')) if (CORE_MAIN / 'com' / 'tcc' / 'pjb' / 'backend' / 'core' / 'modularity').exists() else []

    root_names = {p.name: p for p in extracted_pkg_root}
    core_names = {p.name: p for p in extracted_pkg_core}
    aligned = []
    mismatched = []
    for name, p in core_names.items():
        if name in root_names and file_hash(root_names[name]) == file_hash(p):
            aligned.append(name)
        elif name in root_names:
            mismatched.append(name)

    autowired_hits = []
    for p in root_java:
        t = p.read_text(encoding='utf-8', errors='ignore')
        if any(line.strip().startswith('@Autowired') for line in t.splitlines()):
            autowired_hits.append(str(p.relative_to(ROOT)))

    controller_paths = sorted(SRC_MAIN.glob('**/*Controller.java'))
    generic_controller_smoke = (SRC_TEST / 'com' / 'tcc' / 'pjb' / 'backend' / 'ControllerSurfaceSmokeTest.java').exists()
    controller_tests = []
    missing_controller_tests = []
    for c in controller_paths:
        base = c.stem
        candidates = [
            SRC_TEST / c.relative_to(SRC_MAIN).parent / f'{base}Test.java',
            SRC_TEST / c.relative_to(SRC_MAIN).parent / f'{base}Tests.java',
        ]
        found = [p for p in candidates if p.exists()]
        if found:
            controller_tests.append(str(found[0].relative_to(ROOT)))
        else:
            missing_controller_tests.append(str(c.relative_to(ROOT)))

    migration_dir = RES_MAIN / 'db' / 'migration'
    migration_versions = {}
    for migration in migration_dir.glob('V*__*.sql'):
        version = migration.name.split('__', 1)[0]
        migration_versions.setdefault(version, []).append(migration.name)
    duplicate_migration_versions = {
        version: sorted(files) for version, files in migration_versions.items() if len(files) > 1
    }

    report = {
        'summary': {
            'rootPomPackagingPom': '<packaging>pom</packaging>' in root_pom,
            'rootModulesDeclared': all(m in root_pom for m in ['<module>pjb-core</module>', '<module>pjb-api</module>']),
            'pjbCorePomExists': CORE_POM.exists(),
            'pjbApiPomExists': API_POM.exists(),
            'pjbApiDependsOnPjbCore': '<artifactId>pjb-core</artifactId>' in api_pom,
            'pjbApiUsesOwnSourceTree': '<artifactId>pjb-api</artifactId>' in api_pom and '../src/main/java' not in api_pom,
            'pjbCoreIsolationOwnedByModule': 'com/tcc/pjb:pjb-core' in api_pom or '<artifactId>pjb-core</artifactId>' in api_pom,
            'springBootPluginOwnedByApiModule': 'spring-boot-maven-plugin' not in root_pom or '<inherited>false</inherited>' in root_pom,
            'rootJavaFiles': len(root_java),
            'rootTestFiles': len(test_java),
            'pjbCoreJavaFiles': len(core_java),
            'controllerCount': len(controller_paths),
            'controllerTestsFound': len(controller_tests),
            'controllerTestsMissing': len(missing_controller_tests),
            'genericControllerSmokePresent': generic_controller_smoke,
            'autowiredInProduction': len(autowired_hits),
            'duplicateMigrationVersions': len(duplicate_migration_versions),
            'runtimeConcurrencyGuardPresent': RUNTIME_GUARD_SCRIPT.exists(),
            'runtimeConcurrencyGuardReportPresent': RUNTIME_GUARD_REPORT.exists(),
        },
        'multiModuleActivation': {
            'rootArtifactPackaging': 'pom' if '<packaging>pom</packaging>' in root_pom else 'non-pom',
            'modules': ['pjb-core', 'pjb-api'],
            'parentCoordinates': {
                'groupId': 'com.tcc.pjb',
                'artifactId': 'pjb-backend-core',
                'version': '1.0.0-RELEASE',
            },
        },
        'realExtraction': {
            'extractedPackage': 'com.tcc.pjb.backend.core.modularity',
            'presentInRoot': rels(SRC_MAIN, extracted_pkg_root),
            'presentInPjbCore': rels(CORE_MAIN, extracted_pkg_core),
            'rootPackageRemoved': len(extracted_pkg_root) == 0,
            'pjbCorePackageCount': len(extracted_pkg_core),
            'alignedMirrors': aligned,
            'mismatchedMirrors': mismatched,
        },
        'driftSweep': {
            'autowiredHits': autowired_hits,
            'genericControllerSmokePresent': generic_controller_smoke,
            'duplicateMigrationVersions': duplicate_migration_versions,
            'missingControllerTests': missing_controller_tests[:200],
        },
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))


if __name__ == '__main__':
    main()
