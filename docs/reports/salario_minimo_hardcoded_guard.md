# Salario Minimo Hardcoded Guard

- Base analisada: `C:\PJB\pjb-api\src\main\java\com\tcc\pjb\backend`
- Arquivo canonico excluido do scan: `SalarioMinimoNacionalService.java`
- Arquivos escaneados: **8070**
- Arquivos com achados: **5**
- Total de ocorrencias: **10**

## Achados

### `pjb-api/src/main/java/com/tcc/pjb/backend/platform/jusos/v2/rules/NationalRulePackEngine.java` (2 ocorrencia(s))

- linha 418 — padrao `localdate_now_inline_in_service_call`, match `multiplicar`
  - snippet: `if (valorCausa != null && valorCausa.compareTo(salarioMinimoNacionalService.multiplicar(new BigDecimal("40"), LocalDate.now())) <= 0) {`
  - acao: Passar data de referencia do dominio (data do pedido, data do ajuizamento, etc.), nao LocalDate.now() inline.
- linha 430 — padrao `localdate_now_inline_in_service_call`, match `multiplicar`
  - snippet: `&& valorCausa.compareTo(salarioMinimoNacionalService.multiplicar(new BigDecimal("60"), LocalDate.now())) <= 0) {`
  - acao: Passar data de referencia do dominio (data do pedido, data do ajuizamento, etc.), nao LocalDate.now() inline.

### `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/calculo/CalculoJudicialEconomicReferenceService.java` (2 ocorrencia(s))

- linha 34 — padrao `valor_por_ano_literal`, match `2025`
  - snippet: `salarioMinimoNacionalService.valorPorAno(2025),`
  - acao: Derivar o ano de LocalDate (ex.: hoje.getYear() - 1 ou getYear()) em vez de literal.
- linha 35 — padrao `valor_por_ano_literal`, match `2026`
  - snippet: `salarioMinimoNacionalService.valorPorAno(2026),`
  - acao: Derivar o ano de LocalDate (ex.: hoje.getYear() - 1 ou getYear()) em vez de literal.

### `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/calculo/CalculoJudicialFrontendCatalogService.java` (2 ocorrencia(s))

- linha 466 — padrao `map_entry_literal_with_salario_minimo_key`, match `salarioMinimoReferencia="1518.00"`
  - snippet: `"salarioMinimoReferencia", "1518.00",`
  - acao: Substituir literal por chamada ao SalarioMinimoNacionalService.
- linha 589 — padrao `map_entry_literal_with_salario_minimo_key`, match `salarioMinimoReferencia="1518.00"`
  - snippet: `"salarioMinimoReferencia", "1518.00",`
  - acao: Substituir literal por chamada ao SalarioMinimoNacionalService.

### `pjb-api/src/main/java/com/tcc/pjb/backend/service/recuperacaojudicial/FalenciaDecretacaoService.java` (2 ocorrencia(s))

- linha 12 — padrao `bigdecimal_literal_near_identifier`, match `1412.00`
  - snippet: `private static final BigDecimal VALOR_SALARIO_MINIMO = new BigDecimal("1412.00");`
  - acao: Substituir literal por chamada ao SalarioMinimoNacionalService com data de referencia explicita.
- linha 12 — padrao `constant_declaration_salario_minimo`, match `VALOR_SALARIO_MINIMO`
  - snippet: `private static final BigDecimal VALOR_SALARIO_MINIMO = new BigDecimal("1412.00");`
  - acao: Remover a constante local; injetar SalarioMinimoNacionalService e usar valorEm/multiplicar com data de referencia.

### `pjb-api/src/main/java/com/tcc/pjb/backend/service/recuperacaojudicial/QuadroGeralCredoresAssemblerService.java` (2 ocorrencia(s))

- linha 40 — padrao `bigdecimal_literal_near_identifier`, match `1412.00`
  - snippet: `private static final BigDecimal SALARIO_MINIMO = new BigDecimal("1412.00");`
  - acao: Substituir literal por chamada ao SalarioMinimoNacionalService com data de referencia explicita.
- linha 40 — padrao `constant_declaration_salario_minimo`, match `SALARIO_MINIMO`
  - snippet: `private static final BigDecimal SALARIO_MINIMO = new BigDecimal("1412.00");`
  - acao: Remover a constante local; injetar SalarioMinimoNacionalService e usar valorEm/multiplicar com data de referencia.

## Acoes recomendadas (transversais)

- Todo calculo baseado em salario minimo deve receber data de referencia do dominio (data do pedido, ajuizamento, decretacao, fato) e chamar SalarioMinimoNacionalService.valorEm(data) ou multiplicar(qtdSm, data).
- Constantes locais com valor monetario do salario minimo sao proibidas em src/main; a fonte canonica e SalarioMinimoNacionalService.
- valorPorAno(ano) so deve receber ano derivado de LocalDate no dominio, nunca literal.
- LocalDate.now() dentro da chamada ao service canonico e equivalente a hardcode: mascara falta de data de referencia real do dominio.
