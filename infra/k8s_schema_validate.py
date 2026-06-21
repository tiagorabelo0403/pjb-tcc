import subprocess
import sys
import yaml
import kubernetes_validate

# K8s version: nenhum pino declarado em cd.yml (azure/setup-kubectl@v4 sem versão)
# nem nos manifestos. 1.30 é o mínimo que suporta todos os recursos usados:
#   - ValidatingAdmissionPolicy/Binding: stable em 1.28+
#   - FlowSchema/PriorityLevelConfiguration: stable em 1.29+
#   - HPA autoscaling/v2: stable em 1.23+
# Atualizar quando o cluster alvo declarar versão explícita.
K8S_VERSION = "1.30"

def validate_overlay(label, path):
    print(f"\n{'='*60}")
    print(f"  {label}  ({path})")
    print(f"{'='*60}")

    result = subprocess.run(
        ["kubectl", "kustomize", path],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        print(f"  BUILD FAIL: {result.stderr.strip()}")
        return False

    docs = [d for d in yaml.safe_load_all(result.stdout) if d]
    validated = 0
    skipped = []
    errors = []

    for doc in docs:
        api_version = doc.get("apiVersion", "")
        kind = doc.get("kind", "")
        name = doc.get("metadata", {}).get("name", "?")
        label_doc = f"{kind}/{name}"

        try:
            kubernetes_validate.validate(doc, K8S_VERSION, strict=True)
            validated += 1
        except kubernetes_validate.SchemaNotFoundError:
            skipped.append(f"{label_doc}  [{api_version} — schema ausente, pulado]")
        except kubernetes_validate.ValidationError as e:
            errors.append(f"{label_doc}: {str(e)[:250]}")
        except Exception as e:
            errors.append(f"{label_doc}: {type(e).__name__}: {str(e)[:150]}")

    print(f"  Validados : {validated}")
    print(f"  Pulados   : {len(skipped)}")
    for s in skipped:
        print(f"    - {s}")
    if errors:
        print(f"  ERROS ({len(errors)}):")
        for err in errors:
            print(f"    ! {err}")
        return False
    else:
        print(f"  Schema    : OK")
        return True

overlays = [
    ("base", "infra/k8s/base"),
    ("prod", "infra/k8s/overlays/prod"),
    ("prod-sovereign-fapi-gateway", "infra/k8s/overlays/prod-sovereign-fapi-gateway"),
    ("prod-sovereign-opa-ext-authz", "infra/k8s/overlays/prod-sovereign-opa-ext-authz"),
]

all_ok = True
for label, path in overlays:
    ok = validate_overlay(label, path)
    if not ok:
        all_ok = False

print(f"\n{'='*60}")
print(f"  Tool: kubernetes-validate {kubernetes_validate.latest_version()}  K8s target: {K8S_VERSION}")
print(f"  RESULTADO: {'SCHEMA OK' if all_ok else 'SCHEMA FALHOU'}")
print(f"{'='*60}")
sys.exit(0 if all_ok else 1)
