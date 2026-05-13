# Build Performance do PJB

## Diagnóstico objetivo

O tempo de compilação local na faixa de 1m40s é compatível com o tamanho atual do projeto:
- mais de 4000 classes Java em `src/main/java`
- mais de 300 classes de teste
- annotation processing com Lombok e MapStruct
- projeto único, grande e altamente acoplado em tempo de compilação

Isso significa que o tempo está alto, mas não necessariamente indica erro estrutural.

## Otimizações seguras aplicadas

### 1. Annotation processing desativado em `testCompile`
Os testes atuais não usam Lombok nem geram mappers MapStruct próprios. Por isso, a fase `testCompile` não precisa rodar annotation processors.

Efeito esperado:
- menos custo de compilação incremental
- menos custo em rebuild local
- menor risco do que mexer na compilação principal

### 2. IntelliJ com heap de build explícito e compilação paralela
O workspace agora fixa:
- `BUILD_PROCESS_HEAP_SIZE=4096`
- `PARALLEL_COMPILATION=true`

Efeito esperado:
- menos pressão de memória no compilador da IDE
- melhor aproveitamento de múltiplos núcleos no desktop

## O que não foi mexido por cautela

- Não houve modularização do projeto nesta rodada
- Não foi desligado annotation processing da compilação principal
- Não foi removido MapStruct/Lombok da cadeia principal
- Não foi alterado o modelo DDD
- Não foi forçado skip de testes como padrão

## Comandos recomendados

Para iteração local:

```bash
./mvnw -DskipTests compile
```

Para validação completa:

```bash
./mvnw clean test
```

Para empacotamento:

```bash
./mvnw clean package
```

## Próximas otimizações possíveis, mas mais invasivas

1. dividir o projeto em módulos Maven
2. separar testes pesados dos rápidos
3. reduzir o número de classes recompiladas por feature slice
4. revisar mappers e DTOs antigos que provocam cascata de recompilação

Essas medidas podem reduzir mais o tempo, mas têm risco maior de efeito colateral do que a rodada atual.
