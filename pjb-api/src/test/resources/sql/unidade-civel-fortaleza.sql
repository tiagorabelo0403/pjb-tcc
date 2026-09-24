insert into tb_unidade_judiciaria_competencia (codigo, tribunal_id, comarca_id, comarca, uf, tipo_justica, ramo_direito, tipo_vara, processos_ativos, capacidade_maxima, capacidade_reserva_percentual, indice_congestionamento, aceita_distribuicao, permite_juizado_especial, permite_urgencia, permite_distribuicao_automatica, somente_digital, status_operacional, modo_operacao, endereco_fisico, endereco_digital, valor_causa_minimo, valor_causa_maximo, prioridade_estrategica, distribuicoes_ultimas_24h)
select 'TJCE-CIVEL-CE-CAP', t.id, c.id, 'Fortaleza', 'CE', 'ESTADUAL', 'CIVIL', 'CIVEL_GERAL', 0, 50000, 10, 0.1500, true, false, true, true, true, 'ATIVA', 'DIGITAL', 'Fortaleza - CE', 'https://tjce.pjb.local/tjce-civel-ce-cap', 0, null, 80, 0
from tb_tribunal t
left join tb_comarca c on c.uf = 'CE' and upper(c.nome) = 'FORTALEZA'
where t.sigla = 'TJCE'
on conflict (codigo) do nothing;
insert into tb_unidade_judiciaria_especialidade (unidade_id, especialidade) select id, 'CIVEL' from tb_unidade_judiciaria_competencia where codigo = 'TJCE-CIVEL-CE-CAP' on conflict do nothing;
insert into tb_unidade_judiciaria_especialidade (unidade_id, especialidade) select id, 'OBRIGACIONAL' from tb_unidade_judiciaria_competencia where codigo = 'TJCE-CIVEL-CE-CAP' on conflict do nothing;
insert into tb_unidade_judiciaria_especialidade (unidade_id, especialidade) select id, 'RESPONSABILIDADE_CIVIL' from tb_unidade_judiciaria_competencia where codigo = 'TJCE-CIVEL-CE-CAP' on conflict do nothing;
insert into tb_unidade_judiciaria_classe_tpu (unidade_id, classe_tpu) select id, 'PROCEDIMENTO_COMUM_CIVEL' from tb_unidade_judiciaria_competencia where codigo = 'TJCE-CIVEL-CE-CAP' on conflict do nothing;
insert into tb_unidade_judiciaria_classe_tpu (unidade_id, classe_tpu) select id, 'TUTELA_ANTECIPADA_ANTECEDENTE' from tb_unidade_judiciaria_competencia where codigo = 'TJCE-CIVEL-CE-CAP' on conflict do nothing;
insert into tb_unidade_judiciaria_classe_tpu (unidade_id, classe_tpu) select id, 'CUMPRIMENTO_DE_SENTENCA' from tb_unidade_judiciaria_competencia where codigo = 'TJCE-CIVEL-CE-CAP' on conflict do nothing;
insert into tb_unidade_judiciaria_assunto_tpu (unidade_id, assunto_tpu) select id, 'CONTRATOS' from tb_unidade_judiciaria_competencia where codigo = 'TJCE-CIVEL-CE-CAP' on conflict do nothing;
insert into tb_unidade_judiciaria_assunto_tpu (unidade_id, assunto_tpu) select id, 'RESPONSABILIDADE_CIVIL' from tb_unidade_judiciaria_competencia where codigo = 'TJCE-CIVEL-CE-CAP' on conflict do nothing;
insert into tb_unidade_judiciaria_assunto_tpu (unidade_id, assunto_tpu) select id, 'OBRIGACOES' from tb_unidade_judiciaria_competencia where codigo = 'TJCE-CIVEL-CE-CAP' on conflict do nothing;
