# DataJud API Key — Nota de Diagnóstico de Segurança

## Situação

Uma credencial de API do DataJud/CNJ foi encontrada no histórico Git:

- **Commit onde apareceu**: `921fc0f` (`chore: initial secure repository baseline`)
- **Commit onde foi removida**: `ca51411` (`fix(security): remover API key CNJ DataJud hardcoded do application.yml`)
- **Localização original**: `pjb-api/src/main/resources/application.yml` (linha 137, valor default)
- **Localização atual**: variável de ambiente `${PJB_INSTITUTIONAL_CNJ_DATAJUD_API_KEY:}` — sem fallback

## Classificação: Cenário B — Credencial Privada de Serviço

O valor removido é uma string Base64 redigida neste relatório. A evidência
histórica indica formato `<api_key>:<api_secret>`, compatível com autenticação
Basic Auth usada pelo DataJud REST API (CNJ).

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

## Ação obrigatória — Requer intervenção externa

**Antes de qualquer limpeza de histórico Git**, o titular da credencial deve
revogar ou regenerar a chave no canal administrativo do DataJud/CNJ e confirmar
que a chave anterior foi invalidada.

Enquanto a chave não for rotacionada, ela permanece ativa mesmo removida do código.

## Limpeza de histórico

Não executar purge de histórico nesta auditoria. A limpeza depende primeiro de
rotação ou revogação externa da credencial e de autorização operacional
explícita, pois envolve reescrita de histórico e coordenação de branches remotas.

## Status

| Item | Estado |
|------|--------|
| Credencial removida do código atual | ✅ Sim (commit `ca51411`) |
| Integração usa variável de ambiente | ✅ Sim |
| Credencial ainda no histórico Git | ⚠️ Sim (commit `921fc0f`) |
| Chave rotacionada no portal DataJud | ❓ Aguardando confirmação do titular |
| Histórico Git limpo | ⏳ Pendente — aguarda rotação confirmada |
