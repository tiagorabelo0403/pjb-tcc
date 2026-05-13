from pathlib import Path
ROOT = Path.cwd()

def replace_once(relative_path: str, old: str, new: str) -> None:
    path = ROOT / relative_path
    if not path.exists():
        raise FileNotFoundError(f"Arquivo não encontrado: {relative_path}")
    text = path.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise RuntimeError(f"Trecho esperado não encontrado em {relative_path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")

replace_once("pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/document/identity/QualifiedSignatureCertificateContextSupport.java",
'map.put("presente", certificate != null || !fingerprint.isBlank() || !subjectRfc2253.isBlank() || !serial.isBlank());',
'map.put("presente", certificate != null || !defaultString(fingerprint).isBlank() || !defaultString(subjectRfc2253).isBlank() || !defaultString(serial).isBlank());')

replace_once("pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/document/identity/QualifiedSignatureInstitutionalAssignmentSupport.java",
'''        String secretariaEspecializada = firstNonBlank(\n                secretariaEmbargos,\n                secretariaRecursal,\n                segundaInstancia,\n                instanciaSuperior,\n                juizadoEspecial,\n                trabalhista,\n                eleitoral,\n                militar,\n                secretariaBase,\n                buildSpecializedSecretariatName(ramoJustica, instancia, tribunal)\n        );''',
'''        String secretariaEspecializada = firstNonBlank(\n                secretariaEmbargos,\n                secretariaRecursal,\n                eleitoral,\n                militar,\n                trabalhista,\n                juizadoEspecial,\n                segundaInstancia,\n                instanciaSuperior,\n                secretariaBase,\n                buildSpecializedSecretariatName(ramoJustica, instancia, tribunal)\n        );''')

replace_once("pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/document/identity/QualifiedSignatureInstitutionalAssignmentSupport.java",
'''        if ("SEGUNDO_GRAU".equals(instancia) || "SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)) {\n            return firstNonBlank(trimToNull(tribunal), "Secretaria Eleitoral de Corte");\n        }''',
'''        if ("SEGUNDO_GRAU".equals(instancia) || "SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)) {\n            String corte = trimToNull(tribunal);\n            return corte == null ? "Secretaria Judiciária Eleitoral" : "Secretaria Judiciária Eleitoral - " + corte;\n        }''')

replace_once("pjb-api/src/main/java/com/tcc/pjb/backend/tribunal/perfil/PerfilInstanciaTribunalService.java",
'''        } else if (value.length() == 3) {\n            value = "" + value.charAt(0) + value.charAt(0) + value.charAt(1) + value.charAt(1) + value.charAt(2) + value.charAt(2);\n        } else if (value.length() < 6) {''',
'''        } else if (value.length() == 3) {\n            return "#" + value.toUpperCase(Locale.ROOT);\n        } else if (value.length() < 6) {''')

readme = ROOT / "README.md"
if readme.exists():
    marker = "## Round 27-F — Estabilização preventiva dos blocos remanescentes"
    text = readme.read_text(encoding="utf-8", errors="ignore")
    if marker not in text:
        readme.write_text(text.rstrip() + "\n\n" + marker + "\n- Blindagem de assinatura qualificada contra certificado de entrada parcial.\n- Priorização de secretaria especializada por ramo antes da secretaria genérica de instância.\n- Preservação de cor institucional curta válida em manifesto de resolução de tribunal.\n- Rodada preventiva sem novo executor, sem scheduler paralelo, sem `CompletableFuture` solto e sem comentários no código.\n", encoding="utf-8")
print("Round 27-F aplicado com sucesso.")
