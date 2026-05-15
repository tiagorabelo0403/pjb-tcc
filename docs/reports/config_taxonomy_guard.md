# Config Taxonomy Guard

- Base analisada: `pjb-api\src\main\java\com\tcc\pjb\backend`
- Raiz canônica: `configs`
- Arquivos Java analisados: **7693**
- Arquivos na raiz canônica: **104**
- Arquivos em raízes legadas: **0**

## Contagem por raiz

- `configs` -> 104 arquivos (canônica)

## Arquivos ainda em raízes legadas

- Nenhum arquivo Java remanescente nas raízes legadas.

## Diretriz

- Usar `configs` como raiz canônica para configurações Spring e infraestrutura do runtime.
- Evitar novos arquivos Java em `config` e `configuracao` para impedir deriva semântica.
- Reservar `config` de nível de repositório apenas para toolchain e análise estática fora da árvore Java.
