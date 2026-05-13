# Git Safe Sync

Este projeto usa uma política local para evitar que segredos, tokens, chaves privadas, arquivos sensíveis e dados pessoais sejam enviados ao GitHub.

## Fluxo recomendado

Use sincronização manual segura quando terminar uma mudança:

```powershell
.\scripts\git-sync-safe.ps1 "descrição objetiva da mudança"
```

Para rodar testes antes de enviar:

```powershell
.\scripts\git-sync-safe.ps1 "descrição objetiva da mudança" -RunTests
```

## Sincronização automática segura

Se quiser que o repositório observe alterações enquanto você trabalha, deixe este comando aberto:

```powershell
.\scripts\git-auto-sync-safe.ps1
```

Ele só cria commit e push quando:

- há poucas mudanças por ciclo;
- os arquivos não estão ignorados por `.gitignore`;
- o guard não encontra segredo, token, chave, CPF, CNPJ, e-mail real ou telefone real;
- o hook de pre-commit passa.

Se encontrar risco, a automação bloqueia o envio, desfaz apenas o stage e mantém seus arquivos locais.

## O que nunca deve ser versionado

- `.env` real;
- senha, token, cookie, chave de API ou credencial;
- CPF, CNPJ, telefone, e-mail pessoal ou dados de partes reais;
- certificado, keystore, kubeconfig, dumps, planilhas ou exportações de banco;
- artefatos gerados como `target`, `node_modules` e logs.

## Arquivos de segurança

- `.gitignore`: exclui artefatos, ambientes locais e arquivos sensíveis.
- `.gitattributes`: normaliza texto e marca binários.
- `.githooks/pre-commit`: executa a barreira antes de cada commit.
- `scripts/git_secret_guard.ps1`: guard principal no Windows.
- `scripts/git_secret_guard.py`: guard equivalente para ambientes com Python.
