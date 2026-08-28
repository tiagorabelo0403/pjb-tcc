-- Remove 32 tabelas sem uso: 0 referencia em codigo Java (producao ou teste), 0 FK vinda de
-- tabela viva, 0 view dependente. Cada uma foi classificada individualmente antes do DROP:
--   DUPLICADA/SUPERADA (29) - ja existe entidade JPA viva cobrindo o mesmo conceito, indicada
--     no comentario ao lado de cada tabela.
--   OBSOLETA (3) - pjb_gigs_activity/pjb_gigs_comment (nomenclatura destoante, conceito coberto
--     por MovimentacaoProcessual/EventoProcessual) e tb_usuario_especialidade (juncao do modelo
--     antigo de perito, substituida por campo String direto em PeritoDisponibilidade).
--
-- Deliberadamente NAO incluidas nesta migration (16 tabelas): representam desenho de feature
-- real, muitas com o servico de logica ja implementado e funcionando, faltando so persistencia
-- (ex: PrecatorioRpvService, ConflitodeCompetenciaDetectorService, TutelaUrgenciaReadinessService).
-- Ficam registradas como pendencia de produto, nao removidas.
--
-- Ordem filha->mae nos pares com FK interna ao proprio grupo removido, sem CASCADE: qualquer
-- dependencia nao mapeada faz o Flyway falhar alto em vez de apagar algo em silencio.

-- pjb_gigs_comment / pjb_gigs_activity (OBSOLETA - MovimentacaoProcessual/EventoProcessual cobrem)
DROP TABLE IF EXISTS pjb_gigs_comment;
DROP TABLE IF EXISTS pjb_gigs_activity;

-- pjb_unidade_judiciaria / pjb_instituicao_judicial (DUPLICADA - Tribunal/UnidadeJudiciariaCompetencia)
DROP TABLE IF EXISTS pjb_unidade_judiciaria;
DROP TABLE IF EXISTS pjb_instituicao_judicial;

-- pjb_pericia / pjb_perito (DUPLICADA - PeritoNomeacao/PeritoDisponibilidade)
DROP TABLE IF EXISTS pjb_pericia;
DROP TABLE IF EXISTS pjb_perito;

-- pjb_prazo_evento / pjb_prazo_processual (DUPLICADA - core/prazos/* + PainelAlertaPrazo)
DROP TABLE IF EXISTS pjb_prazo_evento;
DROP TABLE IF EXISTS pjb_prazo_processual;

-- pjb_ciencia_publicacao / pjb_publicacao (DUPLICADA - CienciaProcessual/DjePublicacao)
DROP TABLE IF EXISTS pjb_ciencia_publicacao;
DROP TABLE IF EXISTS pjb_publicacao;

-- tb_peticionamento_parte, tb_peticionamento_anexo / tb_peticionamento_intermediario
-- (DUPLICADA - service/processual/peticionamento/* reconstruido + PoloProcessual)
DROP TABLE IF EXISTS tb_peticionamento_parte;
DROP TABLE IF EXISTS tb_peticionamento_anexo;
DROP TABLE IF EXISTS tb_peticionamento_intermediario;

-- tb_sessao_voto / tb_sessao_plenaria_item / tb_sessao_plenaria
-- (DUPLICADA - JulgamentoColegiado/PlenarioVirtualSessao/VotoColegiado)
DROP TABLE IF EXISTS tb_sessao_voto;
DROP TABLE IF EXISTS tb_sessao_plenaria_item;
DROP TABLE IF EXISTS tb_sessao_plenaria;

-- Restante, sem FK cruzada entre si (DUPLICADA salvo indicacao em contrario)
DROP TABLE IF EXISTS pjb_distribuicao_snapshot; -- ProcessoDistribuicaoCompetencia
DROP TABLE IF EXISTS pjb_assinatura_pendente; -- service/secretariat/signature/*
DROP TABLE IF EXISTS pjb_assinatura_lote; -- service/secretariat/signature/*
DROP TABLE IF EXISTS pjb_expediente_painel; -- SecretariatQueueItem
DROP TABLE IF EXISTS pjb_processo_externo_carga; -- MniRecepcao (pipeline MNI)
DROP TABLE IF EXISTS pjb_fase_transicao_log; -- MovimentacaoProcessual
DROP TABLE IF EXISTS pjb_ato_processual_log; -- EventoProcessual/MovimentacaoProcessual
DROP TABLE IF EXISTS pjb_citacao; -- CitacaoIntimacaoEngine/ExpedicaoJudicial
DROP TABLE IF EXISTS pjb_custas_processual; -- modules/custas/*
DROP TABLE IF EXISTS pjb_datajud_movimentacao; -- integration/datajud/feed/* + DataJudFeedCheckpoint
DROP TABLE IF EXISTS tb_cadastro_central_pessoa; -- IdentidadeJuridicaNacional
DROP TABLE IF EXISTS tb_certidao_template; -- CertidaoDigital + CertidaoBatchEmissionService
DROP TABLE IF EXISTS tb_certidao_emitida; -- DiligenciaOperadorCertidao
DROP TABLE IF EXISTS tb_publicacao_oficial; -- DjePublicacao
DROP TABLE IF EXISTS tb_knowledge_card; -- LegalKnowledgeCorpusArtifact
DROP TABLE IF EXISTS tb_usuario_especialidade; -- OBSOLETA - PeritoDisponibilidade usa String direto
