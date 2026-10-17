UPDATE tb_unidade_judiciaria_competencia
   SET exclusivo_juizado_especial = TRUE,
       permite_juizado_especial = TRUE,
       valor_causa_maximo = NULL
 WHERE codigo LIKE '%-JEF-%';
