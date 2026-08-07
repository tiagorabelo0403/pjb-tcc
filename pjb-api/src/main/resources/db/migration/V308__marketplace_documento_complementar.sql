-- V308 - Marketplace completude documental, Fase 2: novo suporte a tipagem de documento
-- e representacao resolvida em procesos para o complemento documental.
-- Ambas colunas nullable — serviços que populam ficam a cargo de Task 10.

alter table tb_documento_processual
    add column if not exists tipo_documento varchar(60);

alter table tb_processo
    add column if not exists instrumento_representacao_resolvido varchar(60);
