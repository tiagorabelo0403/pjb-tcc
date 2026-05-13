package com.tcc.pjb.backend.ai.juridica.v3.core;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
public final class BrazilianLegalKnowledgeBase {
private BrazilianLegalKnowledgeBase() {}
public static RamoDescriptor getRamo(String ramo) {
return RAMOS.getOrDefault(ramo, RAMOS.get("CIVIL"));
}
public static Set<String> getRamosDisponiveis() {
return Collections.unmodifiableSet(RAMOS.keySet());
}
private static final Map<String, RamoDescriptor> RAMOS = Map.ofEntries(
Map.entry("CIVIL", new RamoDescriptor(
"CIVIL", "Direito Civil e Processual Civil",
"Código Civil — Lei 10.406/2002; CPC — Lei 13.105/2015",
"CPC/2015 (Lei 13.105/2015)",
List.of("Obrigações e contratos", "Responsabilidade civil extracontratual e contratual",
"Posse e propriedade", "Direitos reais de gozo e garantia",
"Tutelas de urgência e evidência", "Execução civil e cumprimento de sentença",
"Procedimentos especiais", "Ação civil pública",
"Usucapião", "Ação monitória", "Retificação de registro público"),
List.of("TJXX (Varas Cíveis)", "TRF (se federal)", "STJ", "STF"),
List.of("PROCEDIMENTO_COMUM", "TUTELA_URGENCIA", "EXECUCAO_TITULO_EXTRAJUDICIAL",
"EMBARGOS_EXECUCAO", "CUMPRIMENTO_SENTENCA", "ACAO_MONITORIA",
"CONSIGNACAO_PAGAMENTO", "RETIFICACAO_REGISTRO", "USUCAPIAO",
"ACAO_POSSESSORIA", "NUNCIACAO_OBRA_NOVA"),
List.of("STJ Súmula 37 (dano moral + material — cumulação)",
"STJ Súmula 248 (dano moral — PJ)",
"STJ REsp 1.445.240 (responsabilidade civil objetiva)",
"STF RE 631.240 (condenação solidária Estado + particular)"),
List.of("Tutela de urgência: imediata (CPC 300)",
"Resposta contestação: 15 dias úteis (CPC 335)",
"Recurso apelação: 15 dias úteis (CPC 1.003)",
"Prescrição geral: 3 anos (CC 206-A) ou 10 anos (CC 205)",
"Usucapião ordinária: 10 anos (CC 1.242)"),
false, false, true
)),
Map.entry("PENAL", new RamoDescriptor(
"PENAL", "Direito Penal e Processual Penal",
"Código Penal — Dec.-Lei 2.848/40; CPP — Dec.-Lei 3.689/41",
"CPP (Dec.-Lei 3.689/41)",
List.of("Crimes contra a pessoa (homicídio, lesão, ameaça)",
"Crimes contra o patrimônio (furto, roubo, extorsão, estelionato)",
"Crimes contra a dignidade sexual",
"Crimes contra a administração pública (peculato, corrupção, prevaricação)",
"Crimes de trânsito — CTB (Lei 9.503/97)",
"Crimes ambientais (Lei 9.605/98)",
"Lei de Drogas (Lei 11.343/2006)",
"Violência doméstica (Lei 11.340/2006 — Maria da Penha)",
"ECA penal (Lei 8.069/90)",
"Lavagem de dinheiro (Lei 9.613/98)",
"Organização criminosa (Lei 12.850/2013)",
"Crimes cibernéticos (Lei 12.737/2012; Lei 14.155/2021)",
"Racismo e injúria racial (Lei 7.716/89)",
"Tortura (Lei 9.455/97)",
"Terrorismo (Lei 13.260/2016)",
"Violência política (Lei 14.197/2021)",
"Execução penal (LEP — Lei 7.210/84)",
"Revisão criminal"),
List.of("Vara Criminal Estadual", "Vara Federal Criminal", "TRF", "STJ", "STF",
"Tribunal do Júri (dolosos contra a vida)", "JECRIM"),
List.of("INQUERITO_POLICIAL", "ACAO_PENAL_PUBLICA_INCONDICIONADA",
"ACAO_PENAL_PUBLICA_CONDICIONADA", "ACAO_PENAL_PRIVADA",
"HABEAS_CORPUS", "REVISAO_CRIMINAL", "EXECUCAO_PENAL",
"PROCEDIMENTO_JURI", "RECLAMACAO_CORRECIONAL",
"MEDIDA_CAUTELAR_PENAL", "ACORDO_NAO_PERSECUCAO_PENAL"),
List.of("STF Súmula Vinculante 11 (algemas)",
"STJ Súmula 444 (vedação de circunstâncias negativas genéricas)",
"STF HC 126.292 / STF ADC 43 e 44 (execução antecipada)",
"STJ Súmula 231 (reincidência — cálculo pena)",
"STF RE 600.817 (tráfico privilegiado)"),
List.of("Defesa preliminar: 10 dias (CPP 396-A)",
"HC: urgente — sem prazo fixo",
"Recurso em sentido estrito: 5 dias (CPP 586)",
"Apelação criminal: 5 dias (CPP 593)",
"Prescrição: conforme pena máxima (CP 109)",
"Queixa crime: 6 meses da ciência do autor (CPP 38)"),
true, true, false
)),
Map.entry("TRABALHISTA", new RamoDescriptor(
"TRABALHISTA", "Direito do Trabalho e Processual do Trabalho",
"CLT — Dec.-Lei 5.452/1943; CF art. 7º-11; Lei 13.467/2017 (Reforma); Lei 14.020/2020 (RJET)",
"CLT arts. 837-910; CPC subsidiário",
List.of("Rescisão de contrato (justa causa, sem justa causa, indireta)",
"Horas extras, banco de horas e sobreaviso",
"Adicional noturno, periculosidade e insalubridade",
"FGTS, multa de 40% e seguro-desemprego",
"Assédio moral e sexual; dano moral laboral",
"Acidente de trabalho e doença ocupacional",
"Estabilidade gestante (ADCT art. 10)",
"Teletrabalho e home office (CLT art. 75-A)",
"Terceirização (Lei 13.429/17; STF ADC 48)",
"Greve e dissídio coletivo",
"Convenção e acordo coletivo",
"Ação rescisória trabalhista",
"Competência material — danos morais de relação de trabalho"),
List.of("Vara do Trabalho", "TRT (14 regionais)", "TST", "STF"),
List.of("RECLAMACAO_TRABALHISTA", "DISSIDIO_COLETIVO", "MANDADO_SEGURANCA_TRABALHISTA",
"ACAO_RESCISORIA_TRABALHISTA", "EMBARGOS_EXECUCAO_TRABALHISTA",
"TUTELA_CAUTELAR_TRABALHISTA", "ACAO_ACIDENTE_TRABALHO_TRT"),
List.of("TST Súmula 331 (terceirização — responsabilidade subsidiária)",
"TST Súmula 428 (sobreaviso teletrabalho)",
"STF ADC 48 (terceirização — ampla licitude)",
"TST Súmula 85 (compensação de jornada)",
"TST Súmula 443 (dispensa discriminatória — reversão presumida)"),
List.of("Reclamação: 2 anos pós-rescisão (CF art. 7º, XXIX)",
"Crédito: 5 anos retroativos (CF art. 7º, XXIX)",
"Contestação: audiência (CLT 847)",
"Recurso ordinário: 8 dias (CLT 895)",
"Pré-executividade: 5 dias"),
false, false, true
)),
Map.entry("PREVIDENCIARIO", new RamoDescriptor(
"PREVIDENCIARIO", "Direito Previdenciário e da Seguridade Social",
"Lei 8.213/91 (Benefícios); Lei 8.212/91 (Custeio); EC 103/2019 (Reforma Previdenciária)",
"CPC/2015 subsidiário; Lei 9.099/95 e Lei 10.259/2001 (JEF)",
List.of("Aposentadoria por tempo de contribuição (EC 103/2019 — pontos progressivos)",
"Aposentadoria por incapacidade permanente (ex-invalidez)",
"Aposentadoria especial (atividade de risco — PPP)",
"Aposentadoria rural — trabalhador rural / segurado especial",
"Auxílio por incapacidade temporária (ex-auxílio-doença)",
"BPC/LOAS (Lei 8.742/93) — deficiente e idoso",
"Pensão por morte",
"Salário-maternidade",
"Revisão de benefício (tese da vida toda — STJ Tema 692)",
"Restabelecimento de benefício cessado indevidamente",
"Reconhecimento de tempo especial — aposentadoria especial",
"RPPS — regime próprio de servidor público",
"Previdência complementar (EFPC / entidade fechada)",
"Acidentário — seguro DPVAT e INSS",
"DIP — Data de Início do Pagamento e retroativos"),
List.of("JEF (até 60 salários mínimos)", "Vara Federal Previdenciária",
"TRF (5 regiões)", "STJ", "STF", "TNU — Turma Nacional de Uniformização"),
List.of("CONCESSAO_BENEFICIO", "REVISAO_BENEFICIO", "RESTABELECIMENTO_BENEFICIO",
"BPC_LOAS", "APOSENTADORIA_ESPECIAL", "ACAO_ACIDENTARIA",
"REVISAO_VIDA_TODA", "RECONHECIMENTO_TEMPO_RURAL",
"RECONHECIMENTO_TEMPO_ESPECIAL", "SALARIO_MATERNIDADE_JEF"),
List.of("STF RE 661.256 (desaposentação — vedada)",
"STF ARE 664.335 (aposentadoria especial — exposição ao risco)",
"STJ Tema 692 (revisão da vida toda — constitucional)",
"TNU Súmula 83 (trabalho rural — início de prova material)",
"TNU Súmula 54 (atividade especial — PPP)"),
List.of("Prescrição quinquenal (Dec. 20.910/32)",
"Contestação INSS: 30 dias (Lei 10.259/01)",
"Recurso inominado JEF: 10 dias",
"Pedido de uniformização TNU: 15 dias",
"DER — prazo de requerimento administrativo (obrigatório)"),
false, false, true
)),
Map.entry("TRIBUTARIO", new RamoDescriptor(
"TRIBUTARIO", "Direito Tributário e Financeiro",
"CTN — Lei 5.172/1966; CF arts. 145-162; Lei 6.830/80 (LEF)",
"CPC/2015 + LEF (Lei 6.830/80) + Dec. 70.235/72 (PAF)",
List.of("ICMS — imposto sobre circulação de mercadorias e serviços",
"IPI — imposto sobre produtos industrializados",
"PIS/COFINS — contribuições sociais",
"CSLL — contribuição social sobre lucro líquido",
"IRPF / IRPJ — imposto de renda",
"ISS — imposto sobre serviços",
"IPTU — imposto sobre propriedade urbana",
"IPVA — imposto sobre veículos",
"ITBI — imposto sobre transmissão de bens imóveis",
"ITCMD — imposto sobre herança e doação",
"Contribuição previdenciária patronal — RGPS/RPPS",
"Simples Nacional",
"Refis / Parcelamentos (PGFN)",
"Execução fiscal e embargos (LEF)",
"Repetição de indébito e compensação tributária",
"Imunidades e isenções tributárias",
"Planejamento tributário, elisão e evasão fiscal",
"Exclusão do ICMS da base PIS/COFINS — tese do século",
"Cautelar fiscal (Lei 8.397/92)"),
List.of("Vara da Fazenda Pública", "TRF (tributos federais)",
"TJ (tributos estaduais/municipais)", "CARF (administrativo)",
"CSRF", "STJ", "STF"),
List.of("EXECUCAO_FISCAL", "EMBARGOS_EXECUCAO_FISCAL", "ACAO_ANULATORIA_DEBITO",
"REPETICAO_INDEBITO", "MANDADO_SEGURANCA_TRIBUTARIO",
"ACAO_DECLARATORIA_TRIBUTARIA", "CAUTELAR_FISCAL",
"CONSIGNACAO_PAGAMENTO_TRIBUTARIA"),
List.of("STF RE 574.706 (ICMS fora da base PIS/COFINS)",
"STJ Súmula 621 (redirecionamento fiscal — dissolução irregular)",
"STF ADI 2.588 (JCP — juros sobre capital próprio)",
"STJ Tema 971 (SELIC no indébito tributário)",
"STJ Súmula 436 (admissão confissão dívida — lançamento tributário)"),
List.of("Decadência: 5 anos (CTN art. 173)",
"Prescrição execução fiscal: 5 anos (CTN art. 174)",
"Embargos execução fiscal: 30 dias (LEF art. 16)",
"Impugnação administrativa: 30 dias (Dec. 70.235/72)",
"Mandado de segurança tributário: 120 dias do ato coator"),
false, false, false
)),
Map.entry("ADMINISTRATIVO", new RamoDescriptor(
"ADMINISTRATIVO", "Direito Administrativo e Público",
"Lei 14.133/2021 (NLLC); Lei 9.784/99 (PAF); Lei 8.429/92 (LIA); CF art. 37",
"CPC/2015; Lei 12.016/2009 (MS); Lei 4.717/65 (AP); Lei 7.347/85 (ACP)",
List.of("Licitações e contratos administrativos (Lei 14.133/2021)",
"Improbidade administrativa (Lei 8.429/92 — redação Lei 14.230/2021)",
"Servidor público: estabilidade, remoção, penalidades, acumulação",
"Concurso público: nulidade, nomeação, preterição",
"Mandado de segurança contra ato administrativo (Lei 12.016/09)",
"Ação popular (Lei 4.717/65)",
"Ato administrativo: nulidade e anulação",
"Concessões e permissões de serviço público",
"Processo administrativo disciplinar (PAD — Lei 8.112/90)",
"Responsabilidade civil do Estado (CF art. 37 §6º)",
"Parcerias público-privadas (Lei 11.079/2004)",
"Desapropriação urbana (Dec.-Lei 3.365/41)",
"Tombamento e bens públicos"),
List.of("Vara da Fazenda Pública", "TRF (atos federais)",
"TJ (atos estaduais/municipais)", "TCU", "TCE", "STJ", "STF"),
List.of("MANDADO_SEGURANCA", "ACAO_POPULAR", "ACAO_IMPROBIDADE",
"ACAO_CIVIL_PUBLICA", "MANDADO_INJUNCAO",
"AGRAVO_REGIMENTAL_ADM", "ACAO_DECLARATORIA_ADM"),
List.of("STF Súmula 473 (anulabilidade atos administrativos)",
"STJ Súmula 212 (mandado de segurança — prazo 120 dias)",
"STF ADI 6.138 (improbidade — dolo específico após Lei 14.230/2021)",
"STJ Súmula 635 (prazo prescrição pretensão indenização Estado — 5 anos)",
"STJ Súmula 510 (prazo concurso público — nomeação dentro da validade)"),
List.of("Mandado de segurança: 120 dias do ato coator (Lei 12.016 art. 23)",
"PAD: 60 dias prorrogáveis (Lei 8.112 art. 152)",
"Resposta do poder público: 10 dias (MS Lei 12.016 art. 7º)",
"Prescrição ação popular: 5 anos (Lei 4.717 art. 21)",
"Prescrição danos contra Fazenda: 5 anos (Dec. 20.910/32)"),
false, true, false
)),
Map.entry("ELEITORAL", new RamoDescriptor(
"ELEITORAL", "Direito Eleitoral",
"Código Eleitoral — Lei 4.737/1965; Resoluções TSE; LC 64/90 (LAEP); LC 135/10 (Ficha Limpa)",
"CE + Regimentos TSE/TRE",
List.of("Registro de candidatura (RRC)",
"AIRC — ação de impugnação de registro de candidatura",
"AIJE — investigação judicial eleitoral (abuso econômico/político)",
"AIME — ação de impugnação de mandato eletivo",
"RCED — recurso contra expedição de diploma",
"Propaganda eleitoral irregular",
"Prestação de contas de campanha",
"Inelegibilidade (LC 64/90 e LC 135/10)",
"Abuso do poder econômico e político",
"Captação ilícita de sufrágio (CE art. 41-A)",
"Financiamento de campanha",
"Direito de resposta eleitoral"),
List.of("Zona Eleitoral", "TRE (27 tribunais)", "TSE", "STF"),
List.of("AIRC", "AIJE", "AIME", "RCED",
"REGISTRO_CANDIDATURA", "RECURSO_ELEITORAL",
"PROPAGANDA_ELEITORAL", "PRESTACAO_CONTAS",
"REPRESENTACAO_ELEITORAL"),
List.of("TSE Res. 23.609 (candidaturas e cotas de gênero)",
"TSE Res. 23.610 (propaganda eleitoral)",
"STF ADI 4.650 (financiamento privado de campanhas — vedação PJ)",
"TSE Súmula 37 (cassação mandato por captação ilícita)",
"TSE Res. 23.600 (prestação de contas)"),
List.of("AIRC: 5 dias após publicação do edital",
"AIJE: até 15 dias após diplomação",
"AIME: até 15 dias após diplomação",
"Recurso eleitoral: 3 dias",
"Prestação de contas: 30 dias após eleição",
"RRC: conforme calendário TSE"),
false, true, false
)),
Map.entry("MILITAR", new RamoDescriptor(
"MILITAR", "Direito Militar",
"CPM — Dec.-Lei 1.001/1969; CPPM — Dec.-Lei 1.002/1969",
"CPPM (Dec.-Lei 1.002/1969)",
List.of("Crimes militares próprios e impróprios",
"Insubordinação, deserção e abandono de posto",
"Violência contra superior e inferior hierárquico",
"Crimes contra a hierarquia e disciplina militar",
"Processo administrativo disciplinar militar",
"IPM — Inquérito Policial Militar",
"Conselho de Justiça Permanente e Especial",
"Lesão corporal leve praticada por militar (competência Justiça Comum — STF)",
"Crimes militares com resultado morte — competência Tribunal do Júri (STF)"),
List.of("Auditoria Militar", "TJM (SP/MG/RS)", "STM", "STF"),
List.of("IPM", "ACAO_PENAL_MILITAR", "CONSELHO_JUSTICA",
"HABEAS_CORPUS_MILITAR", "RECURSO_CRIMINAL_MILITAR"),
List.of("STF HC 122.694 (competência Justiça Militar — critério ratione personae)",
"STM Súmula 5 (crime militar impróprio — ausência hierarquia)",
"STF RHC 116.474 (violência doméstica militar — Justiça Comum)",
"STF AP 937 QO (foro privilegiado — restrição ao exercício do mandato)"),
List.of("Defesa na AIDH: 3 dias",
"Contestação Conselho: 3 dias",
"Recurso criminal militar: 5 dias (CPPM art. 529)",
"Habeas corpus militar: urgente — sem prazo fixo"),
true, true, false
)),
Map.entry("FAMILIA_SUCESSOES", new RamoDescriptor(
"FAMILIA_SUCESSOES", "Direito de Família e Sucessões",
"CC/2002 arts. 1.511-1.783 (Família); arts. 1.784-2.046 (Sucessões)",
"CPC/2015 + arts. 693-699 (família) + arts. 610-673 (inventário) + arts. 746-763 (interdição)",
List.of("Casamento: regime de bens, separação e divórcio",
"União estável (CC art. 1.723) e suas dissolução",
"Guarda compartilhada (Lei 13.058/2014)",
"Alienação parental (Lei 12.318/2010)",
"Alimentos: fixação, revisão, execução e exoneração",
"Alimentos gravídicos (Lei 11.804/2008)",
"Tutela, curatela e tomada de decisão apoiada (Lei 13.146/2015)",
"Adoção nacional e internacional (ECA arts. 39-52; Lei 13.509/17)",
"Inventário judicial e extrajudicial; arrolamento",
"Partilha de bens e colações",
"Testamento, codicilo e herança digital",
"Reconhecimento de paternidade/maternidade; investigação de paternidade",
"Herdeiros necessários e legítima",
"Bem de família (Lei 8.009/90)"),
List.of("Vara de Família e Sucessões", "TJ", "STJ", "STF"),
List.of("DIVORCIO", "SEPARACAO_JUDICIAL", "GUARDA_VISITA", "ALIMENTOS",
"INVENTARIO", "ARROLAMENTO", "ADOCAO", "RECONHECIMENTO_PATERNIDADE",
"INVESTIGACAO_PATERNIDADE", "UNIAO_ESTAVEL", "TUTELA_CURATELA"),
List.of("STJ Súmula 596 (alimentos gravídicos — presunção paternidade)",
"STF RE 878.694 (herança — direitos iguais cônjuge e companheiro)",
"STJ Súmula 301 (DNA — presunção absoluta de paternidade)",
"STJ REsp 1.088.681 (alienação parental — síndrome da alienação)",
"STJ Súmula 364 (bem de família — único imóvel — proteção)"),
List.of("Divórcio consensual extrajudicial: imediato (cartório — sem filhos menores)",
"Alimentos provisionais: imediatos (CPC 300)",
"Inventário: 60 dias do óbito (CPC 611)",
"Prescrição ação de alimentos: imprescritível (capital); 2 anos (prestações vencidas)",
"Arrolamento sumário: qualquer valor — consenso entre partes"),
true, true, true
)),
Map.entry("CONSTITUCIONAL", new RamoDescriptor(
"CONSTITUCIONAL", "Direito Constitucional",
"CF/1988; Regimento Interno do STF",
"CF art. 102 (competência STF); Lei 9.868/99 (ADI/ADC); Lei 9.882/99 (ADPF)",
List.of("ADI — ação direta de inconstitucionalidade",
"ADC — ação declaratória de constitucionalidade",
"ADPF — arguição de descumprimento de preceito fundamental",
"Mandado de injunção (individual e coletivo)",
"Controle difuso de constitucionalidade (qualquer juízo)",
"Repercussão geral — RE no STF",
"Cláusulas pétreas (CF art. 60 §4º)",
"Competências legislativas: privativa, concorrente, suplementar",
"Direitos fundamentais e proporcionalidade",
"Controle concentrado — efeito erga omnes e vinculante"),
List.of("STF (controle concentrado)", "Qualquer juízo (controle difuso)", "STJ", "TRF"),
List.of("ADI", "ADC", "ADPF", "MANDADO_INJUNCAO", "RECURSO_EXTRAORDINARIO",
"RECLAMACAO_CONSTITUCIONAL"),
List.of("STF ADPF 54 (anencefalia — antecipação de parto)",
"STF ADI 3.510 (pesquisas com células-tronco)",
"STF RE 586.224 (complementação aposentadoria — competência)",
"STF ADC 43 e 44 (execução provisória — prisão em 2º grau)"),
List.of("ADI/ADC/ADPF: sem prazo definido (permanente)",
"RE com repercussão geral: sistemática de julgamento em lote",
"Reclamação constitucional: 10 dias — não há prazo fixo"),
false, false, false
)),
Map.entry("CONSUMIDOR", new RamoDescriptor(
"CONSUMIDOR", "Direito do Consumidor",
"CDC — Lei 8.078/1990",
"CPC/2015 + Lei 9.099/95 (JEC)",
List.of("Vício de produto e serviço",
"Publicidade enganosa e abusiva",
"Práticas comerciais abusivas",
"Cláusulas contratuais abusivas (CDC art. 51)",
"Recall e responsabilidade objetiva do fabricante",
"Serviços bancários e financeiros (CDC x bancos — STJ Súmula 297)",
"Saúde suplementar no CDC — planos de saúde",
"Comércio eletrônico (Dec. 7.962/2013)",
"Cobranças indevidas e negativação indevida",
"Cadastro de inadimplentes — SPC/Serasa",
"Direito de arrependimento (7 dias — fora do estabelecimento)",
"Dano moral do consumidor",
"Ação coletiva de consumo"),
List.of("PROCON (extrajudicial)", "JEC (até 40 SM)", "Vara Cível", "TJ", "STJ"),
List.of("ACAO_INDENIZATORIA_CONSUMIDOR", "ACAO_COLETIVA_CONSUMIDOR",
"MANDADO_SEGURANCA_CONSUMIDOR", "TUTELA_URGENCIA_CDC",
"ACAO_DECLARATORIA_CONTRATO"),
List.of("STJ Súmula 297 (CDC aplicável às instituições financeiras)",
"STJ Súmula 470 (vício produto — responsabilidade solidária)",
"STJ Súmula 381 (correção monetária bancária — independe de cláusula expressa)",
"STF RE 661.256 (CDC x previdência complementar)",
"STJ Súmula 543 (prescrição conta corrente — 5 anos)"),
List.of("Vício: 30 dias (produtos não duráveis) / 90 dias (duráveis) — CDC art. 26",
"Arrependimento: 7 dias corridos do recebimento — CDC art. 49",
"Prescrição dano ao consumidor: 5 anos — CDC art. 27",
"Tutela coletiva: imprescritível a pretensão difusa"),
false, false, true
)),
Map.entry("AMBIENTAL", new RamoDescriptor(
"AMBIENTAL", "Direito Ambiental",
"Lei 6.938/81 (PNMA); Lei 9.605/98 (Crimes Ambientais); Lei 12.651/12 (Código Florestal)",
"CPC/2015 + Lei 7.347/85 (ACP ambiental)",
List.of("Licenciamento ambiental (LAP/LAI/LAO — CONAMA 237/97)",
"Áreas de preservação permanente (APP) e reserva legal",
"Cadastro ambiental rural (CAR) e regularização",
"Crimes ambientais (Lei 9.605/98)",
"Dano ambiental e reparação integral (imprescritível)",
"Poluição hídrica, atmosférica, sonora e do solo",
"Unidades de conservação (SNUC — Lei 9.985/00)",
"Transgênicos e biossegurança (Lei 11.105/05)",
"Mudanças climáticas (Lei 12.187/09)",
"Responsabilidade civil objetiva ambiental (CF art. 225 §3º)",
"Ação civil pública ambiental (Lei 7.347/85)"),
List.of("Vara Ambiental", "IBAMA", "TRF (crimes federais)", "TJ", "STJ", "STF"),
List.of("ACAO_CIVIL_PUBLICA_AMBIENTAL", "ACAO_PENAL_AMBIENTAL",
"MANDADO_SEGURANCA_AMBIENTAL", "ACAO_POPULAR_AMBIENTAL",
"EMBARGO_AMBIENTAL", "TUTELA_URGENCIA_AMBIENTAL"),
List.of("STF ADI 4.983 (vaquejada — violação à dignidade dos animais)",
"STF ADPF 708 (Fundo Clima — obrigação de manutenção)",
"STJ Súmula 613 (responsabilidade ambiental — adquirente do imóvel)",
"STJ REsp 1.356.207 (responsabilidade objetiva — dano ambiental)"),
List.of("Imprescritibilidade do dano ambiental (STJ REsp 1.120.117)",
"Ação penal ambiental: prescrição pela pena máxima (CP art. 109)",
"Licença ambiental: prazo conforme resolução CONAMA",
"Liminar ambiental: in dubio pro natura"),
false, true, false
)),
Map.entry("EMPRESARIAL", new RamoDescriptor(
"EMPRESARIAL", "Direito Empresarial e Societário",
"CC/2002 arts. 966-1.195; Lei 6.404/76 (SA); Lei 11.101/05 (LRF); Lei 9.279/96 (LPI)",
"CPC/2015 + Lei 11.101/05",
List.of("Constituição e dissolução de sociedades",
"Recuperação judicial e extrajudicial (Lei 11.101/05)",
"Falência e incidente de desconsideração da personalidade jurídica",
"Responsabilidade dos sócios e administradores",
"Apuração de haveres e exclusão de sócio",
"Contrato de sociedade simples e empresária",
"Títulos de crédito: cheque, nota promissória, duplicata, CCB",
"Propriedade industrial e marcas (Lei 9.279/96)",
"Contratos empresariais e inadimplência",
"Desconsideração da personalidade jurídica (CC art. 50; CDC art. 28)",
"Fusão, incorporação, cisão e transformação societária"),
List.of("Vara Empresarial", "TJ", "TRF (crimes empresariais)", "STJ", "STF"),
List.of("RECUPERACAO_JUDICIAL", "FALENCIA", "DISSOLUCAO_EMPRESA",
"APURACAO_HAVERES", "ACAO_MONITORIA_TITULO",
"INCIDENTE_DESCONSIDERACAO", "CAUTELAR_EMPRESARIAL"),
List.of("STJ Súmula 435 (sócio responsável — CNPJ cancelado irregularmente)",
"STJ Súmula 430 (responsabilidade tributária sócio-gerente — ato com excesso)",
"STF RE 598.296 (recuperação judicial — crédito tributário — preferência Fisco)",
"STJ REsp 1.315.166 (cram down — aprovação judicial do plano)",
"STJ Súmula 600 (NR 2021 — exclusão sócio — EI)"),
List.of("Pedido de recuperação: 60 dias pós-protestos relevantes (LRF art. 51)",
"Plano de recuperação: 60 dias pós-deferimento (LRF art. 53)",
"Habilitação crédito falência: 15 dias (LRF art. 7º)",
"Prescrição cheque: 6 meses após protesto; 2 anos ação direta (Lei 7.357/85)",
"Prescrição duplicata: 3 anos (CC art. 206)"),
false, false, true
)),
Map.entry("PROPRIEDADE_INTELECTUAL", new RamoDescriptor(
"PROPRIEDADE_INTELECTUAL", "Direito de Propriedade Intelectual",
"Lei 9.279/96 (LPI); Lei 9.610/98 (Direito Autoral); Lei 9.609/98 (Software)",
"CPC/2015",
List.of("Patentes de invenção e modelo de utilidade",
"Marcas e nomes empresariais",
"Desenho industrial",
"Indicações geográficas",
"Direito autoral e conexos (músicas, obras literárias, audiovisual)",
"Software e programas de computador",
"Concorrência desleal",
"Segredo industrial e know-how",
"Licenciamento e transferência de tecnologia",
"Domínio de internet (NIC.br)",
"Criações em relação de emprego (LPI art. 88-93)"),
List.of("INPI (administrativo)", "Vara Federal (competência exclusiva — marcas/patentes)",
"TRF", "STJ", "STF"),
List.of("ACAO_NULIDADE_MARCA", "ACAO_CONTRAFACAO", "ACAO_INDENIZATORIA_PI",
"CAUTELAR_APREENSAO", "ACAO_CONCORRENCIA_DESLEAL"),
List.of("STJ REsp 1.657.974 (marca de alto renome — proteção ampla)",
"STJ REsp 1.188.105 (patente pipeline — alcance)",
"STF RE 352.036 (autoria intelectual — direito à imagem)",
"STJ Súmula 221 (concorrência desleal — nome comercial)"),
List.of("Patente invenção: validade 20 anos (LPI art. 40)",
"Marca: validade 10 anos renováveis (LPI art. 133)",
"Prescrição concorrência desleal: 5 anos (LPI art. 225)",
"Ação nulidade marca: imprescritível (LPI art. 174)"),
false, false, true
)),
Map.entry("INTERNACIONAL", new RamoDescriptor(
"INTERNACIONAL", "Direito Internacional Privado e Público",
"LINDB (Dec.-Lei 4.657/42); Convenção de Haia; Tratados bilaterais",
"CPC/2015 (arts. 960-965 — cooperação internacional); Regimento STJ",
List.of("Carta rogatória e auxílio direto",
"Homologação de sentença estrangeira",
"Extradição e deportação",
"Cooperação jurídica internacional (MLAT)",
"Sequestro e retenção ilícita de menor (Conv. Haia 1980)",
"Conflito de leis no espaço",
"Tratados internacionais de direitos humanos",
"Arbitragem internacional (Lei 9.307/96)",
"Imunidade de jurisdição de Estado estrangeiro",
"Refúgio e asilo (Lei 9.474/97)"),
List.of("STJ (homologação e carta rogatória)", "STF (extradição)", "TRF", "AGU"),
List.of("HOMOLOGACAO_SENTENCA_ESTRANGEIRA", "CARTA_ROGATORIA",
"EXEQUATUR", "EXTRADICAO", "REPATRIACAO_MENOR"),
List.of("STF Ext. 1.085 (extradição — vedações constitucionais)",
"STJ SEC 9.412 (homologação — ofensa à ordem pública)",
"STF RE 466.343 (depositário infiel — inconvencionalidade)",
"STJ CC 136.130 (cooperação direta — MLA)"),
List.of("Carta rogatória: 6 meses para cumprimento (CPC 962)",
"Homologação STJ: manifestação 15 dias",
"Convenção Haia menor: 6 semanas preferencial",
"Extradição: prazo de prisão preventiva 40 dias (Lei 6.815 art. 82)"),
false, false, false
)),
Map.entry("SAUDE", new RamoDescriptor(
"SAUDE", "Direito à Saúde",
"CF art. 196; Lei 8.080/90 (SUS); Lei 9.656/98 (Planos de Saúde); RN ANS 465/2021",
"CPC/2015 + Lei 12.016/09 (MS) + Lei 9.099/95 (JEC)",
List.of("Fornecimento de medicamentos pelo SUS (CF art. 196)",
"Custeio de tratamento pelo plano de saúde",
"Internação compulsória e leito de UTI",
"Erro médico e responsabilidade civil hospitalar",
"Negativa de cobertura de plano de saúde",
"Cancelamento ilegal de plano de saúde",
"Saúde mental e internação psiquiátrica (Lei 10.216/01)",
"Medicamentos off-label e experimentais",
"Transplante e doação de órgãos (Lei 9.434/97)",
"Responsabilidade solidária dos entes federativos em saúde (STF Tema 6)"),
List.of("JEF (SUS + INSS)", "Vara da Fazenda (SUS estadual/municipal)",
"Vara Cível (plano privado)", "TJ", "STJ", "STF"),
List.of("ACAO_FORNECIMENTO_MEDICAMENTO", "MANDADO_SEGURANCA_SAUDE",
"ACAO_INDENIZATORIA_SAUDE", "ACAO_COLETIVA_SAUDE",
"TUTELA_URGENCIA_SAUDE"),
List.of("STF Tema 6 (fornecimento medicamentos — responsabilidade solidária)",
"STF RE 855.178 (solidariedade entes federativos — saúde)",
"STJ Súmula 609 (plano saúde — doença preexistente — cobertura)",
"STJ Súmula 302 (prazo prescrição plano saúde — 1 ano)",
"STJ Súmula 469 (CDC aplica-se planos de saúde)"),
List.of("Urgência médica: tutela imediata",
"Prazo plano resposta autorização: 5 dias (urgência) / 10 dias (eletivo) — ANS",
"Prescrição erro médico: 3 anos (CC 206)",
"Ação mandado de segurança saúde: 120 dias do ato denegatório"),
true, false, true
)),
Map.entry("IMOBILIARIO", new RamoDescriptor(
"IMOBILIARIO", "Direito Imobiliário",
"CC/2002 arts. 1.196-1.510; Lei 8.245/91 (Locação); Lei 9.514/97 (SFI)",
"CPC/2015 + Lei 8.245/91",
List.of("Locação residencial e comercial",
"Despejo por falta de pagamento e por denúncia vazia",
"Compra e venda de imóvel",
"Alienação fiduciária de imóvel (Lei 9.514/97)",
"Minha Casa Minha Vida / MCMV",
"Condomínio edilício (CC arts. 1.331-1.358)",
"Usucapião: urbana, rural, familiar, extrajudicial",
"Posse e reintegração de posse",
"Retificação de área e matrículas",
"Incorporação imobiliária (Lei 4.591/64)",
"Hipoteca e anticrese",
"Regularização fundiária (Lei 13.465/17)"),
List.of("Vara Cível (Locação/Posse)", "Vara da Fazenda (IPTU)", "TJ", "STJ"),
List.of("DESPEJO", "REINTEGRACAO_POSSE", "USUCAPIAO",
"ACAO_CONSIGNACAO_ALUGUEL", "RETIFICACAO_REGISTRO",
"RESCISAO_COMPRA_VENDA", "ACAO_DIVISAO_DEMARCACAO"),
List.of("STJ Súmula 335 (cláusula de vigência locatícia — averbação — oponibilidade)",
"STJ Súmula 307 (FGTS Habitacional e hipoteca — coexistência)",
"STJ REsp 1.433.031 (distrato imobiliário — devolução percentuais)",
"STF RE 93.256 (usucapião especial urbana — requisitos)"),
List.of("Desocupação voluntária locação: 30 dias (Lei 8.245 art. 46)",
"Notificação denúncia comercial: 30 dias (Lei 8.245 art. 57)",
"Usucapião ordinária: 10 anos posse mansa (CC 1.242)",
"Usucapião especial urbana: 5 anos (CF art. 183)",
"Usucapião especial rural: 5 anos (CF art. 191)"),
false, false, true
)),
Map.entry("EDUCACAO", new RamoDescriptor(
"EDUCACAO", "Direito à Educação",
"CF arts. 205-214; LDB — Lei 9.394/96; Lei 12.711/12 (Cotas)",
"CPC/2015 + Lei 12.016/09 (MS)",
List.of("Acesso a vagas em universidades federais (cotas — Lei 12.711/12)",
"FIES — financiamento estudantil",
"ProUni — programa universidade para todos",
"ENADE e avaliação INEP",
"Reconhecimento de diploma estrangeiro (MEC)",
"Matrícula compulsória de criança/adolescente",
"Educação especial e inclusiva (Lei 13.146/15 — LBI)",
"Contratos com faculdades particulares",
"Bolsas de pesquisa (CNPq/FAPESP/CAPES)",
"Cancelamento unilateral de matrícula — STJ Súmula 514"),
List.of("JEF (questões federais)", "Vara Federal", "Vara da Fazenda Estadual",
"JEC (contratos faculdades privadas até 40 SM)", "TJ", "STJ"),
List.of("MANDADO_SEGURANCA_EDUCACAO", "ACAO_OBRIGACAO_FAZER_EDUCACAO",
"ACAO_INDENIZATORIA_EDUCACAO", "TUTELA_URGENCIA_MATRICULA"),
List.of("STF ADPF 186 (cotas raciais — constitucionalidade)",
"STJ Súmula 514 (cancelamento unilateral de matrícula — vedação)",
"STF RE 597.285 (FIES — equivalência de diplomas)",
"STJ Súmula 473 (acesso à educação — obrigação de fazer estatal)"),
List.of("Matrícula urgência: tutela imediata",
"Reconhecimento diploma: 60 dias MEC",
"Prescrição contratos educação: 5 anos (CC 206-A)"),
false, false, true
)),
Map.entry("INFANCIA_JUVENTUDE", new RamoDescriptor(
"INFANCIA_JUVENTUDE", "Infância, Juventude e Direito da Criança",
"Lei 8.069/90 (ECA); CF art. 227; Lei 13.509/17 (Adoção)",
"ECA + CPC/2015 (subsidiário); Resolução CNJ 71/2009",
List.of("Ato infracional e medidas socioeducativas (internação, semiliberdade, liberdade assistida)",
"Adoção nacional e internacional (Lei 13.509/17)",
"Destituição do poder familiar",
"Acolhimento institucional e familiar",
"Guarda e tutela do menor",
"Proteção contra abuso, maus-tratos e negligência",
"Trabalho infantil (proibição — CF art. 7º, XXXIII)",
"Direito à convivência familiar e comunitária",
"Conselho Tutelar — medidas protetivas (ECA art. 101)",
"SINASE — Sistema Nacional de Atendimento Socioeducativo (Lei 12.594/12)"),
List.of("Vara da Infância e Juventude", "TJ", "STJ", "STF"),
List.of("MEDIDA_SOCIOEDUCATIVA", "DESTITUICAO_PODER_FAMILIAR",
"HABILITACAO_ADOCAO", "ACAO_ADOCAO_ECA",
"MEDIDA_PROTETIVA_ECA", "ACOLHIMENTO_INSTITUCIONAL"),
List.of("STF HC 143.988 (internação — medida excepcional — ECA art. 122)",
"STJ Súmula 605 (dano moral coletivo — criança e adolescente)",
"STJ REsp 1.293.875 (convivência familiar — prioridade absoluta)",
"STJ AgRg RE 1.274.745 (medida socioeducativa — proporcionalidade)"),
List.of("Prazo de internação máxima: 3 anos (ECA art. 121)",
"Internação provisória: 45 dias (ECA art. 108)",
"Adoção: prazo indeterminado — priorizando o melhor interesse",
"Destituição poder familiar: urgente — prioridade absoluta"),
true, true, false
)),
Map.entry("AGRARIO", new RamoDescriptor(
"AGRARIO", "Direito Agrário e Fundiário",
"Lei 4.504/64 (Estatuto da Terra); Dec.-Lei 3.365/41; LC 76/93; CF arts. 184-186",
"CPC/2015 + LC 76/93 (rito sumário especial desapropriação agrária)",
List.of("Desapropriação para reforma agrária (INCRA — CF art. 184)",
"Assentamento rural e regularização fundiária",
"Conflitos fundiários e possessórios rurais",
"Usucapião pro labore — rural (CF art. 191; CC art. 1.239)",
"Propriedade produtiva — imunidade à desapropriação (CF art. 185)",
"ITR — imposto territorial rural e exceções",
"CCIR — certificado de cadastro de imóvel rural",
"CAR — cadastro ambiental rural e regularização",
"Violência no campo — crimes praticados contra trabalhadores rurais",
"Arrendamento e parceria rural (Dec. 59.566/66)"),
List.of("Vara Agrária", "Vara Federal (INCRA)", "TRF", "STJ", "STF"),
List.of("DESAPROPRIACAO_REFORMA_AGRARIA", "USUCAPIAO_RURAL",
"ACAO_REINTEGRACAO_POSSE_RURAL", "ASSENTAMENTO",
"ACAO_DIVISAO_DEMARCACAO_RURAL"),
List.of("STF MS 25.284 (desapropriação — imóvel produtivo — imunidade)",
"STJ Súmula 354 (fixação indenização desapropriação)",
"STF RE 140.521 (usucapião rural — requisito pessoalidade)",
"STJ REsp 1.442.840 (conflito fundiário — mediação obrigatória)"),
List.of("Desapropriação agrária — rito especial (LC 76/93): urgente",
"Usucapião rural: 5 anos de posse mansa (CF art. 191)",
"Reintegração de posse: urgente em conflitos coletivos",
"INCRA — prazo vistoria após denúncia: 60 dias"),
false, false, false
))
);

private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
private static String norm(String raw) {
if (raw == null) return "";
String n = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
n = n.toLowerCase(Locale.ROOT);
n = NON_ALNUM.matcher(n).replaceAll(" ").trim();
return n;
}
public static List<RamoDescriptor> search(String texto) {
String q = norm(texto);
if (q.isBlank()) return List.of(getRamo("CIVIL"));
List<Map.Entry<String, RamoDescriptor>> ranked = new ArrayList<>(RAMOS.entrySet());
ranked.sort((a, b) -> Integer.compare(scoreRamo(q, b.getValue()), scoreRamo(q, a.getValue())));
List<RamoDescriptor> out = new ArrayList<>();
for (Map.Entry<String, RamoDescriptor> e : ranked) {
int s = scoreRamo(q, e.getValue());
if (s <= 0) continue;
out.add(e.getValue());
if (out.size() >= 8) break;
}
if (out.isEmpty()) out.add(getRamo("CIVIL"));
return Collections.unmodifiableList(out);
}
private static int scoreRamo(String q, RamoDescriptor d) {
int score = 0;
String nome = norm(d.nome());
String regra = norm(d.regraPrincipal());
String cod = norm(d.codigoProcessual());
if (!nome.isBlank() && containsAll(nome, q)) score += 40;
if (!regra.isBlank() && containsAny(regra, q)) score += 15;
if (!cod.isBlank() && containsAny(cod, q)) score += 10;
for (String s : d.subRamos()) {
String ns = norm(s);
if (containsAny(ns, q)) score += 8;
}
for (String c : d.classesComuns()) {
String nc = norm(c);
if (containsAny(nc, q)) score += 5;
}
for (String p : d.precedentesEstruturantes()) {
String np = norm(p);
if (containsAny(np, q)) score += 3;
}
return score;
}
private static boolean containsAny(String hay, String q) {
if (hay.isBlank() || q.isBlank()) return false;
for (String t : q.split("\\s+")) {
if (t.length() < 3) continue;
if (hay.contains(t)) return true;
}
return false;
}
private static boolean containsAll(String hay, String q) {
if (hay.isBlank() || q.isBlank()) return false;
int ok = 0;
int total = 0;
for (String t : q.split("\\s+")) {
if (t.length() < 3) continue;
total++;
if (hay.contains(t)) ok++;
}
return total > 0 && ok == total;
}
public static Flags inferFlags(String ramo, String subRamo, String rito) {
RamoDescriptor d = getRamo(ramo == null ? "CIVIL" : ramo.toUpperCase(Locale.ROOT));
boolean segredo = d.segredoJustica();
boolean mp = d.exigeMP();
boolean conciliacao = d.admiteConciliacao();
String sr = subRamo == null ? "" : subRamo.toUpperCase(Locale.ROOT);
String rt = rito == null ? "" : rito.toUpperCase(Locale.ROOT);
if (rt.contains("MARIA_DA_PENHA") || sr.contains("VIOLENCIA") || sr.contains("ADOCAO") || sr.contains("INFANCIA")) segredo = true;
if (sr.contains("IMPROBIDADE") || sr.contains("AMBIENTAL") || sr.contains("ELEITORAL") || sr.contains("PENAL") || rt.startsWith("PENAL")) mp = true;
if (rt.startsWith("PENAL") || rt.contains("EXECUCAO_PENAL") || rt.contains("TRIBUNAL_JURI")) conciliacao = false;
return new Flags(segredo, mp, conciliacao);
}
public static List<String> competenceHints(String ramo, String esfera, String subRamo, Double valorCausa) {
String r = ramo == null ? "CIVIL" : ramo.toUpperCase(Locale.ROOT);
String e = esfera == null ? "" : esfera.toUpperCase(Locale.ROOT);
String s = subRamo == null ? "" : subRamo.toUpperCase(Locale.ROOT);
List<String> hints = new ArrayList<>();
if ("TRABALHISTA".equals(r)) hints.add("Competência: Vara do Trabalho (CLT; CF art. 114).");
if ("ELEITORAL".equals(r)) hints.add("Competência: Zona Eleitoral/TRE/TSE, conforme a classe (Código Eleitoral; Resoluções TSE).");
if ("MILITAR".equals(r)) hints.add("Competência: Justiça Militar da União (STM) ou Estadual (TJM), conforme a força e o fato.");
if ("PREVIDENCIARIO".equals(r) || s.contains("INSS")) {
hints.add("Competência: Justiça Federal; verificar JEF se valor até 60 salários mínimos.");
if (valorCausa != null && valorCausa > 0 && valorCausa <= 60.0) hints.add("Indício: JEF (até 60 SM) se o valor da causa estiver em salários mínimos.");
}
if ("TRIBUTARIO".equals(r)) hints.add("Competência: Vara da Fazenda Pública (estadual/municipal) ou Justiça Federal (tributo federal).");
if ("ADMINISTRATIVO".equals(r) || "FAZENDA".equals(e) || "ESTADUAL".equals(e) || "MUNICIPAL".equals(e)) hints.add("Competência: Vara da Fazenda Pública quando houver ente público como parte.");
if ("FEDERAL".equals(e)) hints.add("Esfera federal detectada: TRF/JF/JEF conforme matéria e valor.");
if ("INFANCIA_JUVENTUDE".equals(r)) hints.add("Competência: Vara da Infância e Juventude; prioridade absoluta (CF art. 227; ECA).");
if ("AGRARIO".equals(r)) hints.add("Competência: Vara Agrária/Varas Cíveis com competência agrária (conforme organização judiciária local).");
if (hints.isEmpty()) {
RamoDescriptor d = getRamo(r);
if (d != null && d.tribunaisCompetentes() != null && !d.tribunaisCompetentes().isEmpty()) {
hints.add("Tribunais/órgãos típicos: " + String.join(", ", d.tribunaisCompetentes()));
}
}
return Collections.unmodifiableList(hints);
}


private static final Map<String, String> RAMO_ALIASES = Map.ofEntries(
Map.entry("CIVEL", "CIVIL"),
Map.entry("CIVIL", "CIVIL"),
Map.entry("CRIMINAL", "PENAL"),
Map.entry("PENAL", "PENAL"),
Map.entry("TRABALHO", "TRABALHISTA"),
Map.entry("TRABALHISTA", "TRABALHISTA"),
Map.entry("PREVIDENCIA", "PREVIDENCIARIO"),
Map.entry("PREVID", "PREVIDENCIARIO"),
Map.entry("PREVIDENCIARIO", "PREVIDENCIARIO"),
Map.entry("TRIBUTARIA", "TRIBUTARIO"),
Map.entry("TRIBUTARIO", "TRIBUTARIO"),
Map.entry("FAZENDARIO", "TRIBUTARIO"),
Map.entry("ADMINISTRATIVO", "ADMINISTRATIVO"),
Map.entry("CONSTITUCIONAL", "CONSTITUCIONAL"),
Map.entry("CONSUMIDOR", "CONSUMIDOR"),
Map.entry("AMBIENTAL", "AMBIENTAL"),
Map.entry("EMPRESA", "EMPRESARIAL"),
Map.entry("EMPRESARIAL", "EMPRESARIAL"),
Map.entry("MILITAR", "MILITAR"),
Map.entry("ELEITORAL", "ELEITORAL"),
Map.entry("FAMILIA", "FAMILIA_SUCESSOES"),
Map.entry("SUCESSOES", "FAMILIA_SUCESSOES"),
Map.entry("FAMILIA_E_SUCESSOES", "FAMILIA_SUCESSOES"),
Map.entry("FAMILIA_SUCESSOES", "FAMILIA_SUCESSOES"),
Map.entry("INFANCIA", "INFANCIA_JUVENTUDE"),
Map.entry("INFANCIA_E_JUVENTUDE", "INFANCIA_JUVENTUDE"),
Map.entry("INFANCIA_JUVENTUDE", "INFANCIA_JUVENTUDE"),
Map.entry("ECA", "INFANCIA_JUVENTUDE"),
Map.entry("AGRARIO", "AGRARIO"),
Map.entry("PROPRIEDADE_INTELECTUAL", "PROPRIEDADE_INTELECTUAL"),
Map.entry("PI", "PROPRIEDADE_INTELECTUAL"),
Map.entry("INTERNACIONAL", "INTERNACIONAL"),
Map.entry("SAUDE", "SAUDE"),
Map.entry("IMOBILIARIO", "IMOBILIARIO"),
Map.entry("EDUCACAO", "EDUCACAO")
);

public static String resolveRamoCodigo(String ramo) {
String token = normalizeRamoInput(ramo);
if (token.isBlank()) return "CIVIL";
String alias = RAMO_ALIASES.get(token);
if (alias != null && RAMOS.containsKey(alias)) return alias;
return RAMOS.containsKey(token) ? token : "CIVIL";
}

public static RamoDescriptor resolve(String ramo) {
return getRamo(resolveRamoCodigo(ramo));
}

public static String fromProjetoRamo(RamoDireito ramo) {
if (ramo == null) return "CIVIL";
return switch (ramo) {
case FAMILIA -> "FAMILIA_SUCESSOES";
default -> ramo.name();
};
}

public static RamoDireito toProjetoRamo(String ramo) {
String codigo = resolveRamoCodigo(ramo);
return switch (codigo) {
case "FAMILIA_SUCESSOES" -> RamoDireito.FAMILIA;
case "SAUDE" -> RamoDireito.CIVIL;
case "EDUCACAO" -> RamoDireito.ADMINISTRATIVO;
case "IMOBILIARIO" -> RamoDireito.CIVIL;
case "PROPRIEDADE_INTELECTUAL" -> RamoDireito.EMPRESARIAL;
case "INTERNACIONAL" -> RamoDireito.ADMINISTRATIVO;
default -> RamoDireito.fromString(codigo);
};
}

public static RamoDireito toProjetoRamo(String ramo, String subRamo, String esfera) {
String codigo = resolveRamoCodigo(ramo);
String sub = normalizeRamoInput(subRamo);
String esf = normalizeRamoInput(esfera);
if ("SAUDE".equals(codigo)) {
if (sub.contains("PLANO") || sub.contains("ERRO_MEDICO")) return RamoDireito.CONSUMIDOR;
if ("FEDERAL".equals(esf) || "ESTADUAL".equals(esf) || "MUNICIPAL".equals(esf)) return RamoDireito.ADMINISTRATIVO;
return RamoDireito.CIVIL;
}
if ("INTERNACIONAL".equals(codigo) && sub.contains("EXTRAD")) return RamoDireito.PENAL;
return toProjetoRamo(codigo);
}

public static List<String> relatedRitos(String ramo) {
RamoDescriptor d = resolve(ramo);
if (d == null || d.classesComuns() == null || d.classesComuns().isEmpty()) return List.of("COMUM_ORDINARIO");
LinkedHashSet<String> out = new LinkedHashSet<>();
for (String c : d.classesComuns()) {
String normalized = normalizeClasseComum(c);
if (normalized != null) out.add(normalized);
}
if (out.isEmpty()) out.add("COMUM_ORDINARIO");
return List.copyOf(out);
}

private static String normalizeClasseComum(String raw) {
if (raw == null || raw.isBlank()) return null;
String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
return normalized.isBlank() ? null : normalized;
}

public static List<String> summarize(String ramo, int maxClasses, int maxPrecedentes) {
RamoDescriptor d = resolve(ramo);
List<String> out = new ArrayList<>();
out.add(d.nome());
out.add(d.regraPrincipal());
d.classesComuns().stream().limit(Math.max(0, maxClasses)).forEach(out::add);
d.precedentesEstruturantes().stream().limit(Math.max(0, maxPrecedentes)).forEach(out::add);
return Collections.unmodifiableList(out);
}

public static Map<String, Object> describeForRouter(String ramo, String subRamo, String esfera, Double valorCausa) {
String codigo = resolveRamoCodigo(ramo);
RamoDescriptor d = getRamo(codigo);
RamoDireito projeto = toProjetoRamo(codigo, subRamo, esfera);
Map<String, Object> out = new LinkedHashMap<>();
out.put("ramoCodigo", codigo);
out.put("ramoProjeto", projeto != null ? projeto.name() : null);
out.put("nome", d.nome());
out.put("regraPrincipal", d.regraPrincipal());
out.put("tribunaisCompetentes", d.tribunaisCompetentes());
out.put("classesComuns", d.classesComuns());
out.put("prazosPrincipais", d.prazosPrincipais());
out.put("relatedRitos", relatedRitos(codigo));
out.put("competenceHints", competenceHints(codigo, esfera, subRamo, valorCausa));
return Collections.unmodifiableMap(out);
}

private static String normalizeRamoInput(String raw) {
if (raw == null || raw.isBlank()) return "";
return norm(raw).replace(' ', '_').toUpperCase(Locale.ROOT);
}

}