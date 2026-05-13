package com.tcc.pjb.backend.core.catalog;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public enum TpuClasseCnj {

    PROCEDIMENTO_COMUM(1, "Procedimento Comum", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_DE_ALIMENTOS(2, "Ação de Alimentos", RamoDireito.FAMILIA,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_FAMILIA,
            ParteCanonica.ALIMENTANTE_ALIMENTANDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    DIVORCIO_CONSENSUAL(3, "Divórcio Consensual", RamoDireito.FAMILIA,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_FAMILIA,
            ParteCanonica.REQUERENTE_CONJUGE,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    DIVORCIO_LITIGIOSO(4, "Divórcio Litigioso", RamoDireito.FAMILIA,
            RamoJustica.ESTADUAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    INVESTIGACAO_PATERNIDADE(5, "Investigação de Paternidade", RamoDireito.FAMILIA,
            RamoJustica.ESTADUAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    ADOCAO(6, "Adoção", RamoDireito.FAMILIA,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_FAMILIA,
            ParteCanonica.REQUERENTE_ADOTANDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    GUARDA_CUSTODIA(7, "Guarda e Responsabilidade", RamoDireito.FAMILIA,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_FAMILIA,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    INTERDITO_PROIBITORIO(8, "Interdito Proibitório", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_POSSESSORIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    REINTEGRACAO_POSSE(9, "Reintegração de Posse", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ESPECIAL_POSSESSORIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    MANUTENCAO_POSSE(10, "Manutenção de Posse", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ESPECIAL_POSSESSORIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    USUCAPIAO(11, "Usucapião", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    USUCAPIAO_ESPECIAL_URBANO(12, "Usucapião Especial Urbano (Moradia)", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    USUCAPIAO_ESPECIAL_RURAL(13, "Usucapião Especial Rural (Pro Labore)", RamoDireito.AGRARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    RESCISAO_CONTRATUAL(14, "Rescisão Contratual", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    INDENIZACAO_DANO_MATERIAL(15, "Indenização por Dano Material", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    INDENIZACAO_DANO_MORAL(16, "Indenização por Dano Moral", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_MONITORIA(17, "Ação Monitória", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_MONITORIA,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    CONSIGNACAO_PAGAMENTO(18, "Ação de Consignação em Pagamento", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ESPECIAL_CONSIGNACAO,
            ParteCanonica.CONSIGNANTE_CONSIGNADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    NUNCIACAO_OBRA_NOVA(19, "Nunciação de Obra Nova", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_POSSESSORIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    INVENTARIO(20, "Inventário", RamoDireito.FAMILIA,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_FAMILIA,
            ParteCanonica.INVENTARIANTE_HERDEIROS,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    ARROLAMENTO_SUMARIO(21, "Arrolamento Sumário", RamoDireito.FAMILIA,
            RamoJustica.ESTADUAL, FaixaProcedimental.SUMARISSIMO,
            ParteCanonica.INVENTARIANTE_HERDEIROS,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    RETIFICACAO_REGISTRO(22, "Retificação de Registro Público", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_REGISTRO,
            ParteCanonica.REQUERENTE_MP,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    TUTELA_CURATELA(23, "Tutela e Curatela", RamoDireito.FAMILIA,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_FAMILIA,
            ParteCanonica.REQUERENTE_CURATELADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    INTERDICAO(24, "Ação de Interdição", RamoDireito.FAMILIA,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_FAMILIA,
            ParteCanonica.REQUERENTE_INTERDITANDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    DISSOLUCAO_UNIAO_ESTAVEL(25, "Dissolução de União Estável", RamoDireito.FAMILIA,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_FAMILIA,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    TUTELA_CAUTELAR_ANTECEDENTE(200, "Tutela Cautelar Antecedente", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.TUTELA_URGENCIA,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    TUTELA_ANTECIPADA_ANTECEDENTE(201, "Tutela Antecipada Antecedente (art. 303 CPC)", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.TUTELA_URGENCIA,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    MEDIDA_CAUTELAR(202, "Medida Cautelar Incidental", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.TUTELA_URGENCIA,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    JUIZADO_ESPECIAL_CIVEL(250, "Ação no Juizado Especial Cível (Lei 9.099/95)", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL, FaixaProcedimental.JUIZADO_SUMARISSIMO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    JUIZADO_ESPECIAL_FEDERAL(251, "Ação no Juizado Especial Federal (Lei 10.259/01)", RamoDireito.CIVIL,
            RamoJustica.FEDERAL, FaixaProcedimental.JUIZADO_SUMARISSIMO,
            ParteCanonica.AUTOR_UNIAO_INSS_CAIXA,
            Set.of(TribunalAlcada.QUALQUER_FEDERAL)),

    JUIZADO_ESPECIAL_FAZENDA_PUBLICA(252, "Ação no Juizado Especial da Fazenda Pública (Lei 12.153/09)",
            RamoDireito.ADMINISTRATIVO,
            RamoJustica.ESTADUAL, FaixaProcedimental.JUIZADO_SUMARISSIMO,
            ParteCanonica.AUTOR_ENTE_PUBLICO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    JUIZADO_ESPECIAL_CRIMINAL(253, "Procedimento Sumaríssimo Penal (JEC — Lei 9.099/95)", RamoDireito.PENAL,
            RamoJustica.ESTADUAL, FaixaProcedimental.SUMARISSIMO_PENAL,
            ParteCanonica.MP_ACUSADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    MANDADO_SEGURANCA_INDIVIDUAL(330, "Mandado de Segurança Individual", RamoDireito.CONSTITUCIONAL,
            RamoJustica.ESTADUAL_FEDERAL_SUPERIOR, FaixaProcedimental.MANDADO_SEGURANCA,
            ParteCanonica.IMPETRANTE_AUTORIDADE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.SUPERIOR)),

    MANDADO_SEGURANCA_COLETIVO(331, "Mandado de Segurança Coletivo", RamoDireito.CONSTITUCIONAL,
            RamoJustica.ESTADUAL_FEDERAL_SUPERIOR, FaixaProcedimental.MANDADO_SEGURANCA,
            ParteCanonica.IMPETRANTE_AUTORIDADE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.SUPERIOR)),

    MANDADO_INJUNCAO(332, "Mandado de Injunção Individual", RamoDireito.CONSTITUCIONAL,
            RamoJustica.ESTADUAL_FEDERAL_SUPERIOR, FaixaProcedimental.MANDADO_SEGURANCA,
            ParteCanonica.IMPETRANTE_AUTORIDADE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.SUPERIOR)),

    MANDADO_INJUNCAO_COLETIVO(333, "Mandado de Injunção Coletivo", RamoDireito.CONSTITUCIONAL,
            RamoJustica.ESTADUAL_FEDERAL_SUPERIOR, FaixaProcedimental.MANDADO_SEGURANCA,
            ParteCanonica.IMPETRANTE_AUTORIDADE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.SUPERIOR)),

    HABEAS_CORPUS(334, "Habeas Corpus", RamoDireito.PENAL,
            RamoJustica.ESTADUAL_FEDERAL_SUPERIOR, FaixaProcedimental.HABEAS_CORPUS,
            ParteCanonica.IMPETRANTE_PACIENTE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.SUPERIOR)),

    HABEAS_DATA(335, "Habeas Data", RamoDireito.CONSTITUCIONAL,
            RamoJustica.ESTADUAL_FEDERAL_SUPERIOR, FaixaProcedimental.HABEAS_CORPUS,
            ParteCanonica.REQUERENTE_DETENTOR_INFORMACOES,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.SUPERIOR)),

    ACAO_POPULAR(336, "Ação Popular", RamoDireito.CONSTITUCIONAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.CIDADAO_REU_FAZENDA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_CIVIL_PUBLICA(400, "Ação Civil Pública (Lei 7.347/85)", RamoDireito.AMBIENTAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.MP_OU_LEGITIMADO_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_DIRETA_INCONSTITUCIONALIDADE(401, "ADI — Ação Direta de Inconstitucionalidade", RamoDireito.CONSTITUCIONAL,
            RamoJustica.SUPERIOR, FaixaProcedimental.CONTROLE_CONCENTRADO,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.STF, TribunalAlcada.TJESTADUAL_PLENO)),

    ACAO_DECLARATORIA_CONSTITUCIONALIDADE(402, "ADC — Ação Declaratória de Constitucionalidade", RamoDireito.CONSTITUCIONAL,
            RamoJustica.SUPERIOR, FaixaProcedimental.CONTROLE_CONCENTRADO,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.STF)),

    ARGUICAO_DESCUMPRIMENTO_PRECEITO(403, "ADPF — Arguição de Descumprimento de Preceito Fundamental",
            RamoDireito.CONSTITUCIONAL,
            RamoJustica.SUPERIOR, FaixaProcedimental.CONTROLE_CONCENTRADO,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.STF)),

    ACAO_RESCISORIA(404, "Ação Rescisória", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL_SUPERIOR, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.SUPERIOR)),

    CUMPRIMENTO_SENTENCA(450, "Cumprimento de Sentença (art. 513 CPC)", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.EXECUCAO,
            ParteCanonica.EXEQUENTE_EXECUTADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    CUMPRIMENTO_PROVISORIO(451, "Cumprimento Provisório de Sentença", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.EXECUCAO,
            ParteCanonica.EXEQUENTE_EXECUTADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    EXECUCAO_TITULO_EXTRAJUDICIAL(452, "Execução por Título Extrajudicial (Livro III CPC)", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL, FaixaProcedimental.EXECUCAO,
            ParteCanonica.EXEQUENTE_EXECUTADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    EXECUCAO_FISCAL(453, "Execução Fiscal (Lei 6.830/80)", RamoDireito.TRIBUTARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.EXECUCAO_FISCAL,
            ParteCanonica.FAZENDA_EXECUTADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    EMBARGOS_EXECUCAO_FISCAL(454, "Embargos à Execução Fiscal", RamoDireito.TRIBUTARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.EXECUCAO_FISCAL,
            ParteCanonica.EMBARGANTE_EMBARGADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    EMBARGOS_EXECUCAO(455, "Embargos do Executado / à Execução", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL, FaixaProcedimental.EXECUCAO,
            ParteCanonica.EMBARGANTE_EMBARGADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    ACAO_IMPROBIDADE_ADMINISTRATIVA(550, "Ação de Improbidade Administrativa (Lei 8.429/92, pós-Lei 14.230/21)",
            RamoDireito.ADMINISTRATIVO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.MP_OU_ENTE_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    PAD_PROCESSO_ADMINISTRATIVO(551, "PAD — Processo Administrativo Disciplinar (judicial)",
            RamoDireito.ADMINISTRATIVO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    MANDADO_SEGURANCA_SERVIDOR(552, "Mandado de Segurança em Matéria de Servidor", RamoDireito.ADMINISTRATIVO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.MANDADO_SEGURANCA,
            ParteCanonica.IMPETRANTE_AUTORIDADE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_CONCURSO_PUBLICO(553, "Ação sobre Concurso Público", RamoDireito.ADMINISTRATIVO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_ORGAO_PUBLICO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_ANULATORIA_DEBITO_FISCAL(600, "Ação Anulatória de Débito Fiscal", RamoDireito.TRIBUTARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.CONTRIBUINTE_FAZENDA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_REPETICAO_INDEBITO(601, "Ação de Repetição de Indébito Tributário", RamoDireito.TRIBUTARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.CONTRIBUINTE_FAZENDA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_DECLARATORIA_TRIBUTO(602, "Ação Declaratória em Matéria Tributária", RamoDireito.TRIBUTARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.CONTRIBUINTE_FAZENDA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    MANDADO_SEGURANCA_TRIBUTARIO(603, "Mandado de Segurança Tributário", RamoDireito.TRIBUTARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.MANDADO_SEGURANCA,
            ParteCanonica.IMPETRANTE_AUTORIDADE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    CAUTELAR_FISCAL(604, "Medida Cautelar Fiscal (Lei 8.397/92)", RamoDireito.TRIBUTARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.TUTELA_URGENCIA,
            ParteCanonica.FAZENDA_EXECUTADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    DENUNCIA_CRIME_DOLOSO_PENA_RECLUSAO(650, "Denúncia — Crime Doloso com Pena de Reclusão", RamoDireito.PENAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.PENAL_ORDINARIO,
            ParteCanonica.MP_ACUSADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    DENUNCIA_CRIME_PENA_DETENCAO(651, "Denúncia — Crime com Pena de Detenção", RamoDireito.PENAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.PENAL_SUMARIO,
            ParteCanonica.MP_ACUSADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    QUEIXA_CRIME(652, "Queixa-crime (ação penal privada)", RamoDireito.PENAL,
            RamoJustica.ESTADUAL, FaixaProcedimental.PENAL_ORDINARIO,
            ParteCanonica.QUERELANTE_QUERELADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    DENUNCIA_JURI(653, "Denúncia — Crime de Competência do Júri", RamoDireito.PENAL,
            RamoJustica.ESTADUAL, FaixaProcedimental.TRIBUNAL_JURI,
            ParteCanonica.MP_ACUSADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    ACAO_PENAL_LEI_DROGAS(654, "Ação Penal — Tráfico de Entorpecentes (Lei 11.343/06)", RamoDireito.PENAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.PENAL_ORDINARIO,
            ParteCanonica.MP_ACUSADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_PENAL_MARIA_PENHA(655, "Ação Penal — Violência Doméstica/Familiar (Lei 11.340/06)", RamoDireito.PENAL,
            RamoJustica.ESTADUAL, FaixaProcedimental.PENAL_ORDINARIO,
            ParteCanonica.MP_ACUSADO_VITIMA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    ACAO_PENAL_LAVAGEM(656, "Ação Penal — Lavagem de Capitais (Lei 9.613/98)", RamoDireito.PENAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.PENAL_ORDINARIO,
            ParteCanonica.MP_ACUSADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_PENAL_ORGANIZACAO_CRIMINOSA(657, "Ação Penal — Crime Organizado (Lei 12.850/13)", RamoDireito.PENAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.PENAL_ORDINARIO,
            ParteCanonica.MP_ACUSADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_PENAL_CRIMES_INFORMATICOS(658, "Ação Penal — Crimes Cibernéticos (Lei 12.737/12, Marco Civil)",
            RamoDireito.PENAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.PENAL_ORDINARIO,
            ParteCanonica.MP_ACUSADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    REVISAO_CRIMINAL(659, "Revisão Criminal", RamoDireito.PENAL,
            RamoJustica.ESTADUAL_FEDERAL_SUPERIOR, FaixaProcedimental.PENAL_REVISAO,
            ParteCanonica.CONDENADO_MINISTERIO_PUBLICO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.SUPERIOR)),

    EXECUCAO_PENAL(660, "Execução Penal (LEP — Lei 7.210/84)", RamoDireito.PENAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.EXECUCAO_PENAL,
            ParteCanonica.EXECUTADO_MP_VEP,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    HABEAS_CORPUS_PENAL(661, "Habeas Corpus Penal (preventivo ou liberatório)", RamoDireito.PENAL,
            RamoJustica.ESTADUAL_FEDERAL_SUPERIOR, FaixaProcedimental.HABEAS_CORPUS,
            ParteCanonica.IMPETRANTE_PACIENTE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.SUPERIOR)),

    ACAO_PENAL_ECA(662, "Ação por Ato Infracional — ECA (Lei 8.069/90)", RamoDireito.INFANCIA_JUVENTUDE,
            RamoJustica.ESTADUAL, FaixaProcedimental.PENAL_ECA,
            ParteCanonica.MP_ADOLESCENTE_REPRESENTANTE,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    ACAO_PENAL_CRIMES_AMBIENTAIS(663, "Ação Penal — Crimes Ambientais (Lei 9.605/98)", RamoDireito.AMBIENTAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.PENAL_ORDINARIO,
            ParteCanonica.MP_ACUSADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    RECLAMACAO_TRABALHISTA_INDIVIDUAL(750, "Reclamação Trabalhista Individual", RamoDireito.TRABALHISTA,
            RamoJustica.TRABALHO, FaixaProcedimental.TRABALHISTA_ORDINARIO,
            ParteCanonica.RECLAMANTE_RECLAMADA,
            Set.of(TribunalAlcada.QUALQUER_TRABALHO)),

    RECLAMACAO_TRABALHISTA_PLRV(751, "Reclamação Trabalhista — Participação nos Lucros/Resultados",
            RamoDireito.TRABALHISTA,
            RamoJustica.TRABALHO, FaixaProcedimental.TRABALHISTA_ORDINARIO,
            ParteCanonica.RECLAMANTE_RECLAMADA,
            Set.of(TribunalAlcada.QUALQUER_TRABALHO)),

    DISSIDIO_COLETIVO_ECONOMICO(752, "Dissídio Coletivo Econômico", RamoDireito.TRABALHISTA,
            RamoJustica.TRABALHO, FaixaProcedimental.DISSIDIO_COLETIVO,
            ParteCanonica.SUSCITANTE_SUSCITADO_MPT,
            Set.of(TribunalAlcada.QUALQUER_TRT, TribunalAlcada.TST)),

    DISSIDIO_COLETIVO_JURIDICO(753, "Dissídio Coletivo Jurídico", RamoDireito.TRABALHISTA,
            RamoJustica.TRABALHO, FaixaProcedimental.DISSIDIO_COLETIVO,
            ParteCanonica.SUSCITANTE_SUSCITADO_MPT,
            Set.of(TribunalAlcada.QUALQUER_TRT, TribunalAlcada.TST)),

    ACAO_RESCISORIA_TRABALHISTA(754, "Ação Rescisória Trabalhista", RamoDireito.TRABALHISTA,
            RamoJustica.TRABALHO, FaixaProcedimental.TRABALHISTA_ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_TRT, TribunalAlcada.TST)),

    MANDADO_SEGURANCA_TRABALHISTA(755, "Mandado de Segurança Trabalhista", RamoDireito.TRABALHISTA,
            RamoJustica.TRABALHO, FaixaProcedimental.MANDADO_SEGURANCA,
            ParteCanonica.IMPETRANTE_AUTORIDADE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_TRT, TribunalAlcada.TST)),

    EXECUCAO_TRABALHISTA(756, "Execução Trabalhista (CLT)", RamoDireito.TRABALHISTA,
            RamoJustica.TRABALHO, FaixaProcedimental.EXECUCAO,
            ParteCanonica.EXEQUENTE_EXECUTADO,
            Set.of(TribunalAlcada.QUALQUER_TRABALHO)),

    CUMPRIMENTO_ACORDO_TRABALHISTA(757, "Cumprimento de Acordo Trabalhista", RamoDireito.TRABALHISTA,
            RamoJustica.TRABALHO, FaixaProcedimental.EXECUCAO,
            ParteCanonica.EXEQUENTE_EXECUTADO,
            Set.of(TribunalAlcada.QUALQUER_TRABALHO)),

    ACAO_TRABALHISTA_ACIDENTE_TRABALHO(758, "Ação Trabalhista — Acidente do Trabalho/Doença Ocupacional",
            RamoDireito.TRABALHISTA,
            RamoJustica.TRABALHO, FaixaProcedimental.TRABALHISTA_ORDINARIO,
            ParteCanonica.RECLAMANTE_RECLAMADA,
            Set.of(TribunalAlcada.QUALQUER_TRABALHO)),

    HABEAS_CORPUS_TRABALHISTA(759, "Habeas Corpus Trabalhista (art. 5º LXVIII CF — dívida trabalhista)",
            RamoDireito.TRABALHISTA,
            RamoJustica.TRABALHO, FaixaProcedimental.HABEAS_CORPUS,
            ParteCanonica.IMPETRANTE_PACIENTE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_TRT, TribunalAlcada.TST)),

    CONCESSAO_BENEFICIO_PREVIDENCIARIO(850, "Concessão/Restabelecimento de Benefício Previdenciário (INSS)",
            RamoDireito.PREVIDENCIARIO,
            RamoJustica.FEDERAL, FaixaProcedimental.PREVIDENCIARIO_JEF,
            ParteCanonica.SEGURADO_INSS,
            Set.of(TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.QUALQUER_JEF)),

    REVISAO_BENEFICIO_PREVIDENCIARIO(851, "Revisão de Benefício Previdenciário", RamoDireito.PREVIDENCIARIO,
            RamoJustica.FEDERAL, FaixaProcedimental.PREVIDENCIARIO_JEF,
            ParteCanonica.SEGURADO_INSS,
            Set.of(TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.QUALQUER_JEF)),

    BPC_LOAS(852, "Benefício de Prestação Continuada — BPC/LOAS (Lei 8.742/93)",
            RamoDireito.PREVIDENCIARIO,
            RamoJustica.FEDERAL, FaixaProcedimental.PREVIDENCIARIO_JEF,
            ParteCanonica.SEGURADO_INSS,
            Set.of(TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.QUALQUER_JEF)),

    AUXILIO_INCAPACIDADE(853, "Auxílio por Incapacidade Temporária ou Permanente", RamoDireito.PREVIDENCIARIO,
            RamoJustica.FEDERAL, FaixaProcedimental.PREVIDENCIARIO_JEF,
            ParteCanonica.SEGURADO_INSS,
            Set.of(TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.QUALQUER_JEF)),

    PENSAO_MORTE_PREVIDENCIARIA(854, "Pensão por Morte Previdenciária", RamoDireito.PREVIDENCIARIO,
            RamoJustica.FEDERAL, FaixaProcedimental.PREVIDENCIARIO_JEF,
            ParteCanonica.DEPENDENTE_INSS,
            Set.of(TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.QUALQUER_JEF)),

    SALARIO_MATERNIDADE(855, "Salário-Maternidade Rural/Urbano", RamoDireito.PREVIDENCIARIO,
            RamoJustica.FEDERAL, FaixaProcedimental.PREVIDENCIARIO_JEF,
            ParteCanonica.SEGURADO_INSS,
            Set.of(TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.QUALQUER_JEF)),

    APOSENTADORIA(856, "Aposentadoria por Idade, Tempo ou Invalidez", RamoDireito.PREVIDENCIARIO,
            RamoJustica.FEDERAL, FaixaProcedimental.PREVIDENCIARIO_JEF,
            ParteCanonica.SEGURADO_INSS,
            Set.of(TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.QUALQUER_JEF)),

    APOSENTADORIA_ESPECIAL(857, "Aposentadoria Especial (atividade especial)", RamoDireito.PREVIDENCIARIO,
            RamoJustica.FEDERAL, FaixaProcedimental.PREVIDENCIARIO_JEF,
            ParteCanonica.SEGURADO_INSS,
            Set.of(TribunalAlcada.QUALQUER_FEDERAL, TribunalAlcada.QUALQUER_JEF)),

    RPPS_REGIME_PROPRIO(858, "Regime Próprio de Previdência Social (RPPS)", RamoDireito.PREVIDENCIARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.SEGURADO_ENTE_PUBLICO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    REGISTRO_CANDIDATURA(900, "Pedido de Registro de Candidatura (DRAP)", RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.ELEITORAL_REGISTRO,
            ParteCanonica.CANDIDATO_PARTIDO_MPE,
            Set.of(TribunalAlcada.QUALQUER_ZONA_ELEITORAL, TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    AIRC_IMPUGNACAO_REGISTRO(901, "AIRC — Ação de Impugnação de Registro de Candidatura",
            RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.ELEITORAL_REGISTRO,
            ParteCanonica.IMPUGNANTE_IMPUGNADO_MPE,
            Set.of(TribunalAlcada.QUALQUER_ZONA_ELEITORAL, TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    AIJE_ABUSO_PODER(902, "AIJE — Ação de Investigação Judicial Eleitoral (abuso de poder)", RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.ELEITORAL_INVESTIGACAO,
            ParteCanonica.AUTOR_INVESTIGADO_MPE,
            Set.of(TribunalAlcada.QUALQUER_ZONA_ELEITORAL, TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    AIME_MANDATO_ELETIVO(903, "AIME — Ação de Impugnação de Mandato Eletivo", RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.ELEITORAL_INVESTIGACAO,
            ParteCanonica.AUTOR_IMPUGNADO_MPE,
            Set.of(TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    RCED_RECURSO_DIPLOMA(904, "RCED — Recurso Contra a Expedição do Diploma", RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.ELEITORAL_RECURSO,
            ParteCanonica.AUTOR_IMPUGNADO_MPE,
            Set.of(TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    REPRESENTACAO_PROPAGANDA(905, "Representação por Propaganda Eleitoral Irregular (art. 96 Lei 9.504/97)",
            RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.ELEITORAL_REPRESENTACAO,
            ParteCanonica.REPRESENTANTE_REPRESENTADO_MPE,
            Set.of(TribunalAlcada.QUALQUER_ZONA_ELEITORAL, TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    DIREITO_RESPOSTA_ELEITORAL(906, "Pedido de Direito de Resposta Eleitoral (art. 58 Lei 9.504/97)",
            RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.ELEITORAL_REPRESENTACAO,
            ParteCanonica.REPRESENTANTE_REPRESENTADO_MPE,
            Set.of(TribunalAlcada.QUALQUER_ZONA_ELEITORAL, TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    CAPTACAO_ILICITA_SUFRAGIO(907, "Captação Ilícita de Sufrágio (art. 41-A Lei 9.504/97)", RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.ELEITORAL_INVESTIGACAO,
            ParteCanonica.AUTOR_INVESTIGADO_MPE,
            Set.of(TribunalAlcada.QUALQUER_ZONA_ELEITORAL, TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    REPRESENTACAO_ABUSO_FINANCEIRO(908, "Representação — Abuso de Poder Econômico em Campanha",
            RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.ELEITORAL_INVESTIGACAO,
            ParteCanonica.AUTOR_INVESTIGADO_MPE,
            Set.of(TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    PRESTACAO_CONTAS_ELEITORAL(909, "Prestação de Contas de Campanha / Partido", RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.ELEITORAL_CONTAS,
            ParteCanonica.PRESTADOR_MPE_ORGAO_TECNICO,
            Set.of(TribunalAlcada.QUALQUER_ZONA_ELEITORAL, TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    HABEAS_CORPUS_ELEITORAL(910, "Habeas Corpus Eleitoral (CE art. 370)", RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.HABEAS_CORPUS,
            ParteCanonica.IMPETRANTE_PACIENTE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_ZONA_ELEITORAL, TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    MANDADO_SEGURANCA_ELEITORAL(911, "Mandado de Segurança Eleitoral", RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.MANDADO_SEGURANCA,
            ParteCanonica.IMPETRANTE_AUTORIDADE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    INELEGIBILIDADE_LCP64(912, "Ação de Declaração de Inelegibilidade (LC 64/90)", RamoDireito.ELEITORAL,
            RamoJustica.ELEITORAL, FaixaProcedimental.ELEITORAL_INVESTIGACAO,
            ParteCanonica.AUTOR_INELEGIVEL_MPE,
            Set.of(TribunalAlcada.QUALQUER_TRE, TribunalAlcada.TSE)),

    IPM_INQUERITO_POLICIAL_MILITAR(1000, "IPM — Inquérito Policial Militar (CPPM art. 9º)", RamoDireito.MILITAR,
            RamoJustica.MILITAR, FaixaProcedimental.MILITAR_IPM,
            ParteCanonica.AUTORIDADE_MILITAR_INVESTIGADO_MPM,
            Set.of(TribunalAlcada.QUALQUER_AUDITORIA_MILITAR, TribunalAlcada.STM, TribunalAlcada.QUALQUER_TJM)),

    ACAO_PENAL_MILITAR(1001, "Ação Penal Militar (CPM / CPPM)", RamoDireito.MILITAR,
            RamoJustica.MILITAR, FaixaProcedimental.MILITAR_PENAL,
            ParteCanonica.MPM_ACUSADO_MILITAR_OFENDIDO,
            Set.of(TribunalAlcada.QUALQUER_AUDITORIA_MILITAR, TribunalAlcada.STM, TribunalAlcada.QUALQUER_TJM)),

    HABEAS_CORPUS_MILITAR(1002, "Habeas Corpus Militar (CPPM art. 466)", RamoDireito.MILITAR,
            RamoJustica.MILITAR, FaixaProcedimental.HABEAS_CORPUS,
            ParteCanonica.IMPETRANTE_PACIENTE_COATORA,
            Set.of(TribunalAlcada.QUALQUER_AUDITORIA_MILITAR, TribunalAlcada.STM, TribunalAlcada.QUALQUER_TJM)),

    MANDADO_SEGURANCA_MILITAR(1003, "Mandado de Segurança Militar (matéria funcional militar)", RamoDireito.MILITAR,
            RamoJustica.MILITAR, FaixaProcedimental.MANDADO_SEGURANCA,
            ParteCanonica.IMPETRANTE_AUTORIDADE_COATORA,
            Set.of(TribunalAlcada.STM, TribunalAlcada.QUALQUER_TJM)),

    PAD_MILITAR(1004, "PAD Militar — Processo Administrativo Disciplinar Militar", RamoDireito.MILITAR,
            RamoJustica.MILITAR, FaixaProcedimental.MILITAR_PAD,
            ParteCanonica.AUTORIDADE_MILITAR_INVESTIGADO_DEFESA,
            Set.of(TribunalAlcada.QUALQUER_AUDITORIA_MILITAR, TribunalAlcada.STM, TribunalAlcada.QUALQUER_TJM)),

    REVISAO_CRIMINAL_MILITAR(1005, "Revisão Criminal Militar (CPPM art. 634)", RamoDireito.MILITAR,
            RamoJustica.MILITAR, FaixaProcedimental.PENAL_REVISAO,
            ParteCanonica.CONDENADO_MPM,
            Set.of(TribunalAlcada.STM, TribunalAlcada.QUALQUER_TJM)),

    EXECUCAO_PENAL_MILITAR(1006, "Execução de Sentença Penal Militar (CPPM art. 585)", RamoDireito.MILITAR,
            RamoJustica.MILITAR, FaixaProcedimental.EXECUCAO_PENAL,
            ParteCanonica.CONDENADO_MPM_VEP,
            Set.of(TribunalAlcada.QUALQUER_AUDITORIA_MILITAR, TribunalAlcada.STM, TribunalAlcada.QUALQUER_TJM)),

    RECUPERACAO_JUDICIAL(1100, "Recuperação Judicial (Lei 11.101/05)", RamoDireito.EMPRESARIAL,
            RamoJustica.ESTADUAL, FaixaProcedimental.RECUPERACAO_FALENCIA,
            ParteCanonica.DEVEDOR_CREDOR_ADMINISTRADOR,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    RECUPERACAO_EXTRAJUDICIAL(1101, "Recuperação Extrajudicial (Lei 11.101/05 art. 161)", RamoDireito.EMPRESARIAL,
            RamoJustica.ESTADUAL, FaixaProcedimental.RECUPERACAO_FALENCIA,
            ParteCanonica.DEVEDOR_CREDOR_ADMINISTRADOR,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    FALENCIA(1102, "Falência (Lei 11.101/05 art. 75)", RamoDireito.EMPRESARIAL,
            RamoJustica.ESTADUAL, FaixaProcedimental.RECUPERACAO_FALENCIA,
            ParteCanonica.REQUERENTE_FALIDO_ADMINISTRADOR,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    INCIDENTE_DESCONSIDERACAO(1103, "Incidente de Desconsideração da Personalidade Jurídica (art. 133 CPC)",
            RamoDireito.EMPRESARIAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    DESAPROPRIACAO_REFORMA_AGRARIA(1150, "Desapropriação para Reforma Agrária (LC 76/93)", RamoDireito.AGRARIO,
            RamoJustica.FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.UNIAO_INCRA_PROPRIETARIO,
            Set.of(TribunalAlcada.QUALQUER_FEDERAL)),

    DESAPROPRIACAO_INTERESSE_SOCIAL(1151, "Desapropriação por Utilidade/Interesse Social (Dec-Lei 3.365/41)",
            RamoDireito.AGRARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.PODER_PUBLICO_PROPRIETARIO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    USUCAPIAO_RURAL_ESPECIAL(1152, "Usucapião Especial Rural Pro Labore (art. 191 CF)", RamoDireito.AGRARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.AUTOR_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACP_AGRARIA(1153, "Ação Civil Pública Agrária", RamoDireito.AGRARIO,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.MP_OU_LEGITIMADO_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_ECA_MEDIDA_SOCIOEDUC(1200, "Ação ECA — Medida Socioeducativa (art. 112 ECA)", RamoDireito.INFANCIA_JUVENTUDE,
            RamoJustica.ESTADUAL, FaixaProcedimental.PENAL_ECA,
            ParteCanonica.MP_ADOLESCENTE_REPRESENTANTE,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    ADOCAO_NACIONAL(1201, "Adoção Nacional (ECA arts. 39-52A)", RamoDireito.INFANCIA_JUVENTUDE,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_FAMILIA,
            ParteCanonica.REQUERENTE_ADOTANDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    ADOCAO_INTERNACIONAL(1202, "Adoção Internacional (ECA arts. 51-52)", RamoDireito.INFANCIA_JUVENTUDE,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_FAMILIA,
            ParteCanonica.REQUERENTE_ADOTANDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    GUARDA_ECA(1203, "Guarda de Menor (ECA art. 33)", RamoDireito.INFANCIA_JUVENTUDE,
            RamoJustica.ESTADUAL, FaixaProcedimental.ESPECIAL_FAMILIA,
            ParteCanonica.REQUERENTE_MENOR_MP,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL)),

    ACP_AMBIENTAL(1250, "Ação Civil Pública Ambiental (Lei 7.347/85)", RamoDireito.AMBIENTAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.ORDINARIO,
            ParteCanonica.MP_OU_LEGITIMADO_REU,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ACAO_PENAL_AMBIENTAL(1251, "Ação Penal Ambiental (Lei 9.605/98)", RamoDireito.AMBIENTAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.PENAL_ORDINARIO,
            ParteCanonica.MP_ACUSADO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    TUTELA_AMBIENTAL_URGENTE(1252, "Tutela Ambiental de Urgência (art. 300 CPC c/c Lei 7.347/85)",
            RamoDireito.AMBIENTAL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.TUTELA_URGENCIA,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    HOMOLOGACAO_SENTENCA_ESTRANGEIRA(1300, "Homologação de Sentença Estrangeira (STJ — RISTJ art. 216-A)",
            RamoDireito.CIVIL,
            RamoJustica.SUPERIOR, FaixaProcedimental.HOMOLOGACAO,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.STJ)),

    CARTA_ROGATORIA(1301, "Carta Rogatória", RamoDireito.CIVIL,
            RamoJustica.SUPERIOR, FaixaProcedimental.HOMOLOGACAO,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.STJ)),

    COOPERACAO_JURIDICA_INTERNACIONAL(1302, "Cooperação Jurídica Internacional (MLAT / Convenções)",
            RamoDireito.CIVIL,
            RamoJustica.SUPERIOR, FaixaProcedimental.HOMOLOGACAO,
            ParteCanonica.AUTORIDADE_CENTRAL_REQUERIDO,
            Set.of(TribunalAlcada.STJ)),

    MEDIACAO_JUDICIAL(1350, "Mediação Judicial Pré-Processual / Processual (Lei 13.140/15)", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.AUTOCOMPOSITIVO,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    CONCILIACAO_JUDICIAL(1351, "Conciliação Judicial (art. 165 CPC)", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL_FEDERAL, FaixaProcedimental.AUTOCOMPOSITIVO,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL, TribunalAlcada.QUALQUER_FEDERAL)),

    ARBITRAGEM(1352, "Procedimento Arbitral (Lei 9.307/96)", RamoDireito.CIVIL,
            RamoJustica.ESTADUAL, FaixaProcedimental.AUTOCOMPOSITIVO,
            ParteCanonica.REQUERENTE_REQUERIDO,
            Set.of(TribunalAlcada.QUALQUER_ESTADUAL));

    
    public enum RamoJustica {
        ESTADUAL, FEDERAL, SUPERIOR, TRABALHO, ELEITORAL, MILITAR,
        ESTADUAL_FEDERAL, ESTADUAL_FEDERAL_SUPERIOR;
    }

    
    public enum FaixaProcedimental {
        ORDINARIO, SUMARIO, SUMARISSIMO, SUMARISSIMO_PENAL, JUIZADO_SUMARISSIMO,
        PENAL_ORDINARIO, PENAL_SUMARIO, PENAL_REVISAO, PENAL_ECA,
        TRIBUNAL_JURI, HABEAS_CORPUS, MANDADO_SEGURANCA,
        ESPECIAL_FAMILIA, ESPECIAL_POSSESSORIO, ESPECIAL_MONITORIA,
        ESPECIAL_CONSIGNACAO, ESPECIAL_REGISTRO,
        TUTELA_URGENCIA, EXECUCAO, EXECUCAO_FISCAL, EXECUCAO_PENAL,
        TRABALHISTA_ORDINARIO, DISSIDIO_COLETIVO,
        PREVIDENCIARIO_JEF,
        ELEITORAL_REGISTRO, ELEITORAL_INVESTIGACAO, ELEITORAL_REPRESENTACAO,
        ELEITORAL_RECURSO, ELEITORAL_CONTAS,
        MILITAR_IPM, MILITAR_PENAL, MILITAR_PAD,
        RECUPERACAO_FALENCIA, CONTROLE_CONCENTRADO, HOMOLOGACAO, AUTOCOMPOSITIVO;
    }

    
    public enum ParteCanonica {

        AUTOR_REU,
        REQUERENTE_REQUERIDO,
        ALIMENTANTE_ALIMENTANDO,
        REQUERENTE_CONJUGE,
        REQUERENTE_ADOTANDO,
        INVENTARIANTE_HERDEIROS,
        REQUERENTE_CURATELADO,
        REQUERENTE_INTERDITANDO,
        CONSIGNANTE_CONSIGNADO,
        REQUERENTE_MP,
        EXEQUENTE_EXECUTADO,
        EMBARGANTE_EMBARGADO,
        FAZENDA_EXECUTADO,
        CONTRIBUINTE_FAZENDA,

        IMPETRANTE_AUTORIDADE_COATORA,
        IMPETRANTE_PACIENTE_COATORA,
        REQUERENTE_DETENTOR_INFORMACOES,
        CIDADAO_REU_FAZENDA,
        MP_OU_LEGITIMADO_REU,
        MP_OU_ENTE_REU,
        AUTOR_ORGAO_PUBLICO,
        AUTOR_INELEGIVEL_MPE,

        MP_ACUSADO,
        MP_ACUSADO_VITIMA,
        QUERELANTE_QUERELADO,
        CONDENADO_MINISTERIO_PUBLICO,
        EXECUTADO_MP_VEP,
        MP_ADOLESCENTE_REPRESENTANTE,

        RECLAMANTE_RECLAMADA,
        SUSCITANTE_SUSCITADO_MPT,

        SEGURADO_INSS,
        DEPENDENTE_INSS,
        SEGURADO_ENTE_PUBLICO,

        CANDIDATO_PARTIDO_MPE,
        IMPUGNANTE_IMPUGNADO_MPE,
        AUTOR_INVESTIGADO_MPE,
        AUTOR_IMPUGNADO_MPE,
        REPRESENTANTE_REPRESENTADO_MPE,
        PRESTADOR_MPE_ORGAO_TECNICO,

        AUTORIDADE_MILITAR_INVESTIGADO_MPM,
        MPM_ACUSADO_MILITAR_OFENDIDO,
        AUTORIDADE_MILITAR_INVESTIGADO_DEFESA,
        CONDENADO_MPM,
        CONDENADO_MPM_VEP,

        DEVEDOR_CREDOR_ADMINISTRADOR,
        REQUERENTE_FALIDO_ADMINISTRADOR,

        UNIAO_INCRA_PROPRIETARIO,
        PODER_PUBLICO_PROPRIETARIO,

        REQUERENTE_MENOR_MP,

        AUTORIDADE_CENTRAL_REQUERIDO,

        AUTOR_UNIAO_INSS_CAIXA,
        AUTOR_ENTE_PUBLICO,
        SEGURADO_INSS_DEPENDENTE
    }

    
    public enum TribunalAlcada {
        QUALQUER_ESTADUAL,
        QUALQUER_FEDERAL,
        QUALQUER_JEF,
        QUALQUER_TRT,
        QUALQUER_TRABALHO,
        QUALQUER_ZONA_ELEITORAL,
        QUALQUER_TRE,
        QUALQUER_AUDITORIA_MILITAR,
        QUALQUER_TJM,
        TJESTADUAL_PLENO,
        SUPERIOR,
        STF, STJ, TST, TSE, STM;
    }

    private final int codigoTpu;
    private final String descricao;
    private final RamoDireito ramoDireito;
    private final RamoJustica ramoJustica;
    private final FaixaProcedimental faixaProcedimental;
    private final ParteCanonica parteCanonica;
    private final Set<TribunalAlcada> tribunaisHabilitados;

    private static final Map<Integer, TpuClasseCnj> BY_CODIGO = new ConcurrentHashMap<>(256);
    private static final Map<String, TpuClasseCnj> BY_NORMALIZED_NAME = new ConcurrentHashMap<>(256);
    private static final Map<RamoDireito, List<TpuClasseCnj>> BY_RAMO = new ConcurrentHashMap<>(16);
    private static final Map<RamoJustica, List<TpuClasseCnj>> BY_RAMO_JUSTICA = new ConcurrentHashMap<>(8);

    static {
        for (TpuClasseCnj c : values()) {
            BY_CODIGO.put(c.codigoTpu, c);
            BY_NORMALIZED_NAME.put(normalize(c.name()), c);
            BY_NORMALIZED_NAME.put(normalize(c.descricao), c);
            BY_RAMO.computeIfAbsent(c.ramoDireito, k -> new java.util.ArrayList<>()).add(c);
            BY_RAMO_JUSTICA.computeIfAbsent(c.ramoJustica, k -> new java.util.ArrayList<>()).add(c);
        }

        BY_RAMO.replaceAll((k, v) -> List.copyOf(v));
        BY_RAMO_JUSTICA.replaceAll((k, v) -> List.copyOf(v));
    }

    TpuClasseCnj(int codigoTpu,
                 String descricao,
                 RamoDireito ramoDireito,
                 RamoJustica ramoJustica,
                 FaixaProcedimental faixaProcedimental,
                 ParteCanonica parteCanonica,
                 Set<TribunalAlcada> tribunaisHabilitados) {
        this.codigoTpu = codigoTpu;
        this.descricao = descricao;
        this.ramoDireito = ramoDireito;
        this.ramoJustica = ramoJustica;
        this.faixaProcedimental = faixaProcedimental;
        this.parteCanonica = parteCanonica;
        this.tribunaisHabilitados = Set.copyOf(tribunaisHabilitados);
    }

    public int codigoTpu() { return codigoTpu; }
    public String descricao() { return descricao; }
    public RamoDireito ramoDireito() { return ramoDireito; }
    public RamoJustica ramoJustica() { return ramoJustica; }
    public FaixaProcedimental faixaProcedimental() { return faixaProcedimental; }
    public ParteCanonica parteCanonica() { return parteCanonica; }
    public Set<TribunalAlcada> tribunaisHabilitados() { return tribunaisHabilitados; }

    public boolean isEleitoral() { return ramoJustica == RamoJustica.ELEITORAL; }
    public boolean isMilitar()   { return ramoJustica == RamoJustica.MILITAR; }
    public boolean isTrabalhista(){ return ramoJustica == RamoJustica.TRABALHO; }
    public boolean isFederal()   { return ramoJustica == RamoJustica.FEDERAL
                                       || ramoJustica == RamoJustica.ESTADUAL_FEDERAL
                                       || ramoJustica == RamoJustica.ESTADUAL_FEDERAL_SUPERIOR; }
    public boolean exigeMP()     { return ramoDireito.exigeAtuacaoMP(); }
    public boolean exigeSigilo() { return ramoDireito.geraSigiloAutomatico(); }

    public boolean habilitadoPara(TribunalAlcada alcada) {
        return tribunaisHabilitados.contains(alcada);
    }

    public static Optional<TpuClasseCnj> porCodigo(int codigo) {
        return Optional.ofNullable(BY_CODIGO.get(codigo));
    }

    public static Optional<TpuClasseCnj> porNome(String nome) {
        if (nome == null || nome.isBlank()) return Optional.empty();
        return Optional.ofNullable(BY_NORMALIZED_NAME.get(normalize(nome)));
    }

    public static List<TpuClasseCnj> porRamo(RamoDireito ramo) {
        return BY_RAMO.getOrDefault(ramo, List.of());
    }

    public static List<TpuClasseCnj> porRamoJustica(RamoJustica ramoJustica) {
        return BY_RAMO_JUSTICA.getOrDefault(ramoJustica, List.of());
    }

    public static List<TpuClasseCnj> todasEmOrdemCodigo() {
        return Arrays.stream(values())
                .sorted(java.util.Comparator.comparingInt(TpuClasseCnj::codigoTpu))
                .toList();
    }

    public static Map<String, Object> toMap(TpuClasseCnj c) {
        Map<String, Object> m = new LinkedHashMap<>(12);
        m.put("codigoTpu", c.codigoTpu);
        m.put("nome", c.name());
        m.put("descricao", c.descricao);
        m.put("ramoDireito", c.ramoDireito.name());
        m.put("ramoJustica", c.ramoJustica.name());
        m.put("faixaProcedimental", c.faixaProcedimental.name());
        m.put("parteCanonica", c.parteCanonica.name());
        m.put("tribunaisHabilitados", c.tribunaisHabilitados.stream()
                .map(Enum::name).sorted().toList());
        m.put("isEleitoral", c.isEleitoral());
        m.put("isMilitar", c.isMilitar());
        m.put("exigeMP", c.exigeMP());
        m.put("exigeSigilo", c.exigeSigilo());
        return m;
    }

    private static String normalize(String v) {
        if (v == null) return "";
        return Normalizer.normalize(v, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9_]", "_")
                .replaceAll("_+", "_")
                .toUpperCase(Locale.ROOT)
                .strip();
    }
}
