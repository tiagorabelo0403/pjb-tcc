# DataJud API Key — Nota de Diagnóstico de Segurança

## Situação

Uma credencial de API do DataJud/CNJ foi encontrada no histórico Git:

- **Commit onde apareceu**: `921fc0f` (`chore: initial secure repository baseline`)
- **Commit onde foi removida**: `ca51411` (`fix(security): remover API key CNJ DataJud hardcoded do application.yml`)
- **Localização original**: `pjb-api/src/main/resources/application.yml` (linha 137, valor default)
- **Localização atual**: variável de ambiente `${PJB_INSTITUTIONAL_CNJ_DATAJUD_API_KEY:}` — sem fallback

## Classificação: Cenário B — Credencial Privada de Serviço

O valor `cDZHYzlZa0JadVREZDJCendQbXY6SkJlTzNjLV9TRENyQk1RdnFKZGRQdw==` é uma
string Base64 que decodifica para `<api_key>:<api_secret>` — formato de
autenticação Basic Auth usado pelo DataJud REST API (CNJ).

Trata-se de **credencial privada de serviço**, não de chave pública criptográfica.
Qualquer parte que detenha essa string pode autenticar-se na API DataJud com as
permissões associadas a ela.

## Estado atual do código

O `application.yml` atual contém:

```yaml
pjb:
  institutional:
    cnj:
      datajud:
        api-key: ${PJB_INSTITUTIONAL_CNJ_DATAJUD_API_KEY:}
```

Não há valor hardcoded. A integração só funciona se a variável de ambiente for
fornecida em tempo de execução.

## Ação obrigatória — Requer intervenção humana

**Antes de qualquer limpeza de histórico Git**, o titular da credencial deve:

1. Acessar o portal DataJud/CNJ: https://datajud-wiki.cnj.jus.br/
2. Revogar/regenerar a chave cujo prefixo decodificado começa com `p6Gc9Yk...`
3. Confirmar que a chave foi rotacionada.

Enquanto a chave não for rotacionada, ela permanece ativa mesmo removida do código.

## Plano de limpeza de histórico (aguardando confirmação)

Assim que o titular confirmar "chave rotacionada", executar:

```bash
# 1. Criar backup antes do purge
git branch backup-before-datajud-history-purge
git tag backup-before-datajud-history-purge

# 2. Criar arquivo de substituições
echo "cDZHYzlZa0JadVREZDJCendQbXY6SkJlTzNjLV9TRENyQk1RdnFKZGRQdw==>REMOVED_DATAJUD_API_KEY" > /tmp/replacements.txt

# 3. Instalar git-filter-repo se não estiver disponível
#    pip install git-filter-repo

# 4. Reescrever histórico
git filter-repo --replace-text /tmp/replacements.txt

# 5. Verificar que não resta nenhuma ocorrência
git log --all -S "cDZHYzlZa0JadVREZDJCendQbXY6SkJlTzNjLV9TRENyQk1RdnFKZGRQdw" --oneline
git grep -n "cDZHYzlZa0JadVREZDJCendQbXY6SkJlTzNjLV9TRENyQk1RdnFKZGRQdw" $(git rev-list --all)

# 6. Force push SOMENTE após autorização explícita do titular
git push --force-with-lease
```

**Nota**: `git filter-repo` não está instalado neste ambiente.
Instalar via `pip install git-filter-repo` antes de executar o purge.

## Status

| Item | Estado |
|------|--------|
| Credencial removida do código atual | ✅ Sim (commit `ca51411`) |
| Integração usa variável de ambiente | ✅ Sim |
| Credencial ainda no histórico Git | ⚠️ Sim (commit `921fc0f`) |
| Chave rotacionada no portal DataJud | ❓ Aguardando confirmação do titular |
| Histórico Git limpo | ⏳ Pendente — aguarda rotação confirmada |
