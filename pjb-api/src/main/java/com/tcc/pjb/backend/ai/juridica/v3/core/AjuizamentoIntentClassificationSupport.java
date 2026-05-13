package com.tcc.pjb.backend.ai.juridica.v3.core;

import com.tcc.pjb.backend.core.procedural.ProceduralRitoNames;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class AjuizamentoIntentClassificationSupport {
String inferirEsfera(Map<String, Object> ctx, String texto) {
String esferaTxt = str(ctx.get("esfera"));
if (!esferaTxt.isBlank()) return normalizeEnumKey(esferaTxt);
boolean isFederal = contains(texto,
"uniao federal", "inss", "receita federal", "ibama", "anatel", "anp",
"cvm", "bacen", "banco central", "justica federal", "jef", "trf",
"ministerio da fazenda", "ministerio da saude federal", "anvisa",
"dnit", "incra", "funai", "caixa economica federal", "bndes",
"banco do brasil sa", "petrobras", "correios ect", "embrapa",
"ministerio publico federal", "mpf", "pge federal", "agu",
"crime federal", "trafic", "terroris", "lavagem dinheiro federais") ||
bool(ctx.get("federal")) || bool(ctx.get("jef"));
boolean isEleitoral = contains(texto, "tse", "tre", "zona eleitoral", "eleicao",
"candidato", "partido politico", "propaganda eleitoral") || bool(ctx.get("eleitoral"));
boolean isMilitar = contains(texto, "justica militar", "stm", "tjm",
"conselho justica militar", "ipm", "cppm", "cpm") || bool(ctx.get("militar"));
boolean isTrabalhista = contains(texto, "clt", "vara do trabalho", "trt", "tst",
"reclamante trabalhista", "reclamada trabalhista") || bool(ctx.get("trabalhista"));
boolean isEstadual = contains(texto, "estado de", "governo estadual", "secretaria estadual",
"pm estadual", "tribunal de justica") || bool(ctx.get("estadual"));
boolean isMunicipal = contains(texto, "prefeitura", "municipio", "camara municipal",
"iss", "iptu", "alvara", "guarda municipal") || bool(ctx.get("municipal"));
if (isEleitoral) return "ELEITORAL";
if (isMilitar) return "MILITAR";
if (isTrabalhista) return "TRABALHISTA";
if (isFederal) return "FEDERAL";
if (isEstadual) return "ESTADUAL";
if (isMunicipal) return "MUNICIPAL";
return "ESTADUAL";
}
String inferirRamo(Map<String, Object> ctx, String texto) {
String ramoPre = str(ctx.get("ramo_direito"));
if (!ramoPre.isBlank()) return normalizeEnumKey(ramoPre);
String areaDir = normalizeEnumKey(str(ctx.get("area_direito")));
if (!areaDir.isBlank()) return mapAreaToRamo(areaDir);
String ambito = normalizeEnumKey(str(ctx.get("ambito_direito")));
if (!ambito.isBlank()) return mapAmbitoToRamo(ambito);
String natureza = normalizeEnumKey(str(ctx.get("natureza_acao")));
if (!natureza.isBlank()) return mapNaturezaToRamo(natureza);
if (contains(texto,
"eca", "crianca", "adolescente", "menor infrator", "ato infracional",
"conselho tutelar", "acolhimento institucional", "vara infancia",
"destituicao poder familiar", "adocao menor", "internacao eca")) {
return "INFANCIA_JUVENTUDE";
}
if (contains(texto,
"agrario", "reforma agraria", "incra", "assentamento", "desapropriacao rural",
"funrural", "propriedade rural", "terra", "posse terra rural",
"usucapiao rural", "lote rural", "assentado", "mst", "sem terra")) {
return "AGRARIO";
}
if (contains(texto,
"penal", "criminal", "crime", "inquerito", "denuncia penal", "cpm",
"trafico", "furto", "roubo", "homicid", "estelionato", "corrup",
"lesao corporal", "ameaca", "violencia", "drogas", "11343", "cppm",
"habeas corpus", "revisao criminal", "execucao penal", "lep",
"organizacao criminosa", "lavagem", "peculato", "prevaricacao",
"extorsao", "sequestro", "falsa identidade", "documento falso",
"crime informatico", "cybercrime", "racismo", "injuria racial",
"feminicidio", "genocidio", "tortura", "terrorismo")) {
return "PENAL";
}
if (contains(texto,
"clt", "reclamante", "reclamada", "empregado", "empregador",
"salario", "fgts", "rescisao", "aviso previo", "horas extras",
"vale transporte", "adicional", "tst", "trt", "vara do trabalho",
"assedio moral", "acidente trabalho", "doenca ocupacional",
"greve", "dissidio coletivo", "convencao coletiva", "terceirizacao",
"banco horas", "adicional noturno", "adicional periculosidade",
"adicional insalubridade", "estabilidade gestante")) {
return "TRABALHISTA";
}
if (contains(texto,
"previdenciario", "inss", "cnis", "beneficio previdenciario",
"bpc", "loas", "aposentadoria", "auxilio doenca", "auxilio acidente",
"pensao por morte", "salario maternidade", "carencia",
"tempo de contribuicao", "rpps", "rgps", "ipca previdenciario",
"revisao beneficio", "restabelecimento beneficio", "desaposentacao",
"aposentadoria especial", "atividade rural previdenciaria",
"salario de beneficio", "dic previdenciaria")) {
return "PREVIDENCIARIO";
}
if (contains(texto,
"tributario", "fiscal", "tributo", "imposto", "taxa", "contribuicao fiscal",
"icms", "ipi", "pis", "cofins", "csll", "irpf", "irpj", "iss", "itbi",
"cda", "execucao fiscal", "parcelamento", "refis", "simples nacional",
"auto de infracao", "embargo execucao fiscal", "repetir indebito",
"anulatoria debito", "compensacao tributaria", "decadencia tributaria",
"prescricao tributaria", "substituicao tributaria", "base calculo",
"aliquota", "fato gerador", "lancamento tributario")) {
return "TRIBUTARIO";
}
if (contains(texto,
"eleitoral", "tse", "tre", "zona eleitoral", "eleicao", "voto",
"candidato", "partido", "propaganda eleitoral", "financiamento campanha",
"impugnacao candidatura", "diploma", "airc", "aije", "aime", "rced",
"ficha limpa", "inelegibilidade", "abuso poder economico", "captacao sufragio")) {
return "ELEITORAL";
}
if (contains(texto,
"militar", "ipm", "exercito", "marinha", "aeronautica",
"conselho justica militar", "cppm", "cpm", "auditoria militar",
"oficialato", "disciplinar militar", "insubordinacao",
"abandono posto", "desertor", "crime militar")) {
return "MILITAR";
}
if (contains(texto,
"adi", "adpf", "adc", "ade", "inconstitucionalidade", "mandado injuncao",
"acao direta", "supremo tribunal", "stf",
"controle difuso", "controle concentrado", "clausula petrea",
"competencia legislativa", "poder constituinte")) {
return "CONSTITUCIONAL";
}
if (contains(texto,
"administrativo", "improbidade", "ato administrativo", "licitacao",
"pregao", "contrato administrativo", "servidor publico", "concurso publico",
"ato nulo", "anulacao ato", "mandado seguranca", "acao popular",
"impugnacao edital", "pad", "processo disciplinar", "responsabilidade estado",
"concessao servico publico", "permissao", "parceria publico privada",
"desapropriacao urbana", "tombamento")) {
return "ADMINISTRATIVO";
}
if (contains(texto,
"consumidor", "cdc", "codigo defesa", "produto defeituoso",
"servico deficiente", "procon", "pratica abusiva", "recall",
"dano moral consumidor", "propaganda enganosa", "clausula abusiva",
"comercio eletronico", "direito arrependimento", "cobranca indevida",
"negativacao indevida", "banco consumidor", "plano saude cdc")) {
return "CONSUMIDOR";
}
if (contains(texto,
"ambiental", "meio ambiente", "ibama", "licenca ambiental",
"area protegida", "poluicao", "desmatamento", "lei flora", "sisnama",
"dano ambiental", "apa", "snuc", "crime ambiental", "degradacao",
"recuperacao area", "reserva legal", "app", "car ambiental",
"codigo florestal", "transgenico", "biosseguranca")) {
return "AMBIENTAL";
}
if (contains(texto,
"familia", "divorcio", "guarda", "alimentos", "tutela menor",
"curatela", "adocao", "uniao estavel", "heranca", "inventario",
"arrolamento", "testamento", "partilha", "casamento", "separacao",
"pensao alimenticia", "paternidade", "maternidade", "regime bens",
"alienacao parental", "reconhecimento filho", "investigacao paternidade",
"dissolucao uniao estavel", "formal partilha")) {
return "FAMILIA_SUCESSOES";
}
if (contains(texto,
"propriedade intelectual", "patente", "marca", "direito autoral",
"software direito", "programa computador", "inpi", "violacao autoral",
"concorrencia desleal", "segredo industrial", "know how",
"design industrial", "indicacao geografica", "licenciamento tecnologia")) {
return "PROPRIEDADE_INTELECTUAL";
}
if (contains(texto,
"falencia", "recuperacao judicial", "recuperacao extrajudicial",
"societario", "dissolucao empresa", "penhora quotas", "desconsideracao",
"contrato empresarial", "titulo credito", "nota promissoria",
"cheque sem fundo", "duplicata", "cram down", "habilitacao credito",
"administrador judicial", "massa falida", "assembleia credores")) {
return "EMPRESARIAL";
}
if (contains(texto,
"internacional", "extraditar", "carta rogatoria", "cooperacao juridica",
"tratado", "stj internacional", "mla", "convencao haia",
"sentenca estrangeira", "homologacao estrangeira", "deportacao",
"expulsao estrangeiro", "refugiado", "asilo politico")) {
return "INTERNACIONAL";
}
if (contains(texto,
"saude sus", "plano de saude", "anvisa", "medicamento sus",
"tratamento custeio", "internacao recusada", "cirurgia negada",
"erro medico", "responsabilidade medica", "plano de saude negar",
"leito uti", "saude mental", "internacao psiquiatrica",
"transplante orgao", "doacao orgao")) {
return "SAUDE";
}
if (contains(texto,
"educacao", "mec", "cota vestibular", "enade", "vestibular",
"matricula escolar", "universidade federal", "prouni", "fies",
"diploma homologacao", "educacao especial", "inclusao escolar",
"bolsa pesquisa", "cnpq", "capes", "faculdade particular contrato")) {
return "EDUCACAO";
}
if (contains(texto,
"habitacao", "minha casa minha vida", "moradia", "despejo locacao",
"locacao residencial", "contrato locacao", "aluguel", "fiador",
"fgts habitacional", "sfi", "cri imobiliario", "alienacao fiduciaria imovel",
"retificacao area", "incorporacao imobiliaria", "condominio",
"usucapiao urbana", "reintegracao posse imovel")) {
return "IMOBILIARIO";
}
return "CIVIL";
}
private String mapAreaToRamo(String area) {
return switch (area) {
case "INFANCIA", "INFANCIA_JUVENTUDE", "ECA" -> "INFANCIA_JUVENTUDE";
case "AGRARIO", "FUNDIARIO", "RURAL" -> "AGRARIO";
default -> mapAmbitoToRamo(area);
};
}
private String mapAmbitoToRamo(String ambito) {
return switch (ambito) {
case "PENAL", "CRIMINAL" -> "PENAL";
case "TRABALHISTA", "TRABALHO" -> "TRABALHISTA";
case "PREVID", "PREVIDENCIARIO", "PREVIDENCIA" -> "PREVIDENCIARIO";
case "TRIBUT", "TRIBUTARIO", "FISCAL" -> "TRIBUTARIO";
case "ELEITORAL" -> "ELEITORAL";
case "MILITAR" -> "MILITAR";
case "ADMIN", "ADMINISTRATIVO" -> "ADMINISTRATIVO";
case "CONSTIT", "CONSTITUCIONAL" -> "CONSTITUCIONAL";
case "CONSUMIDOR", "CDC" -> "CONSUMIDOR";
case "AMBIENTAL" -> "AMBIENTAL";
case "FAMILIA", "FAMILIA_SUCESSOES", "SUCESSOES" -> "FAMILIA_SUCESSOES";
case "EMPRESARIAL", "SOCIETARIO" -> "EMPRESARIAL";
case "PI", "PROPRIEDADE_INTELECTUAL" -> "PROPRIEDADE_INTELECTUAL";
case "INTERNACIONAL" -> "INTERNACIONAL";
case "SAUDE" -> "SAUDE";
case "EDUCACAO" -> "EDUCACAO";
case "IMOBILIARIO", "LOCACAO" -> "IMOBILIARIO";
case "INFANCIA", "ECA" -> "INFANCIA_JUVENTUDE";
case "AGRARIO" -> "AGRARIO";
default -> "CIVIL";
};
}
private String mapNaturezaToRamo(String natureza) {
return switch (natureza) {
case "PENAL", "CRIMINAL", "INFRACIONAL" -> "PENAL";
case "TRABALHISTA", "LABORAL" -> "TRABALHISTA";
case "PREVIDENCIARIO" -> "PREVIDENCIARIO";
case "TRIBUTARIO", "FAZENDARIO" -> "TRIBUTARIO";
case "ELEITORAL" -> "ELEITORAL";
case "MILITAR" -> "MILITAR";
case "ADMINISTRATIVO", "PUBLICO" -> "ADMINISTRATIVO";
case "FAMILIAR", "FAMILIA" -> "FAMILIA_SUCESSOES";
default -> "CIVIL";
};
}
String inferirSubRamo(String ramo, Map<String, Object> ctx, String texto) {
return switch (ramo) {
case "PENAL" -> inferirSubRamoPenal(texto);
case "CIVIL" -> inferirSubRamoCivil(texto);
case "FAMILIA_SUCESSOES" -> inferirSubRamoFamilia(texto);
case "TRIBUTARIO" -> inferirSubRamoTributario(texto);
case "PREVIDENCIARIO" -> inferirSubRamoPrevidenciario(texto);
case "ADMINISTRATIVO" -> inferirSubRamoAdministrativo(texto);
case "ELEITORAL" -> inferirSubRamoEleitoral(texto);
case "TRABALHISTA" -> inferirSubRamoTrabalhista(texto);
case "EMPRESARIAL" -> inferirSubRamoEmpresarial(texto);
case "AMBIENTAL" -> inferirSubRamoAmbiental(texto);
case "CONSUMIDOR" -> inferirSubRamoConsumidor(texto);
case "INFANCIA_JUVENTUDE" -> inferirSubRamoInfanciaJuventude(texto);
case "AGRARIO" -> inferirSubRamoAgrario(texto);
case "CONSTITUCIONAL" -> inferirSubRamoConstitucional(texto);
case "SAUDE" -> inferirSubRamoSaude(texto);
case "IMOBILIARIO" -> inferirSubRamoImobiliario(texto);
case "PROPRIEDADE_INTELECTUAL" -> inferirSubRamoPI(texto);
case "INTERNACIONAL" -> inferirSubRamoInternacional(texto);
default -> "GERAL";
};
}
private String inferirSubRamoPenal(String t) {
if (contains(t, "homicid", "feminicid", "genocidio")) return "CRIMES_DOLOSOS_VIDA";
if (contains(t, "trafico", "drogas", "11343", "entorpecente")) return "LEI_DROGAS";
if (contains(t, "maria da penha", "violencia domestica", "11340")) return "VIOLENCIA_DOMESTICA";
if (contains(t, "transito", "ctb", "embriaguez volante")) return "CRIMES_TRANSITO";
if (contains(t, "corrupcao", "peculato", "prevaricacao", "lavagem")) return "CRIMES_CONTRA_ADMINISTRACAO";
if (contains(t, "estelionato", "fraude", "golpe")) return "CRIMES_PATRIMONIO";
if (contains(t, "racismo", "injuria racial", "discriminacao racial")) return "RACISMO_DISCRIMINACAO";
if (contains(t, "tortura", "maus tratos")) return "TORTURA";
if (contains(t, "terrorismo", "financiamento terrorismo")) return "TERRORISMO";
if (contains(t, "organizacao criminosa", "12850", "orcrim")) return "ORGANIZACAO_CRIMINOSA";
if (contains(t, "cyb", "crime informatico", "invasao dispositivo")) return "CRIMES_CIBERNETICOS";
if (contains(t, "violencia politica", "candidata", "Lei 14.197")) return "VIOLENCIA_POLITICA";
if (contains(t, "habeas corpus", "prisao ilegal", "flagrante", "preventiva")) return "GARANTIAS_PROCESSUAIS";
if (contains(t, "execucao penal", "lep", "progressao regime", "livramento")) return "EXECUCAO_PENAL";
if (contains(t, "revisao criminal", "revisao sentenca penal")) return "REVISAO_CRIMINAL";
if (contains(t, "extorsao", "sequestro", "carcere privado")) return "CRIMES_LIBERDADE_PESSOAL";
if (contains(t, "abuso sexual", "estupro", "assedio sexual penal")) return "CRIMES_DIGNIDADE_SEXUAL";
if (contains(t, "crimes ambientais", "lei 9605")) return "CRIMES_AMBIENTAIS";
return "PENAL_COMUM";
}
private String inferirSubRamoCivil(String t) {
if (contains(t, "dano moral", "responsabilidade civil", "indenizacao")) return "RESPONSABILIDADE_CIVIL";
if (contains(t, "contrato", "inadimplencia", "rescisao contratual")) return "CONTRATOS";
if (contains(t, "locacao", "aluguel", "despejo", "inquilino")) return "LOCACAO";
if (contains(t, "posse", "propriedade", "reintegracao", "imissao")) return "DIREITOS_REAIS";
if (contains(t, "titulo extrajudicial", "nota promissoria", "execucao civil")) return "EXECUCAO_CIVIL";
if (contains(t, "tutela urgencia", "liminar", "antecipacao tutela")) return "TUTELA_URGENCIA";
if (contains(t, "obrigacao fazer", "obrigacao nao fazer")) return "OBRIGACAO_FAZER";
if (contains(t, "usucapiao", "prescricao aquisitiva")) return "USUCAPIAO";
if (contains(t, "acao monitoria", "monitoria")) return "MONITORIA";
if (contains(t, "acao civil publica", "acp")) return "ACAO_CIVIL_PUBLICA";
return "OBRIGACOES";
}
private String inferirSubRamoFamilia(String t) {
if (contains(t, "alimentos", "pensao alimenticia", "alimentos gravid")) return "ALIMENTOS";
if (contains(t, "divorcio", "separacao", "dissolver casamento")) return "DIVORCIO";
if (contains(t, "guarda", "visita", "alienacao parental")) return "GUARDA_VISITAS";
if (contains(t, "inventario", "arrolamento", "heranca", "espolio")) return "SUCESSOES";
if (contains(t, "adocao", "adotar")) return "ADOCAO";
if (contains(t, "uniao estavel", "companheiro")) return "UNIAO_ESTAVEL";
if (contains(t, "tutela", "curatela", "interditar")) return "TUTELA_CURATELA";
if (contains(t, "investigacao paternidade", "reconhecimento filho")) return "PATERNIDADE";
if (contains(t, "testamento", "codicilo")) return "TESTAMENTO";
return "FAMILIA_GERAL";
}
private String inferirSubRamoTributario(String t) {
if (contains(t, "execucao fiscal", "cda", "penhora fiscal")) return "EXECUCAO_FISCAL";
if (contains(t, "repetir indebito", "restituicao", "compensacao")) return "REPETICAO_INDEBITO";
if (contains(t, "anulatoria", "anular debito fiscal", "auto infracao")) return "ANULATORIA_DEBITO";
if (contains(t, "mandado seguranca tributario")) return "MANDADO_SEGURANCA_TRIBUTARIO";
if (contains(t, "embargos execucao fiscal")) return "EMBARGOS_EXECUCAO_FISCAL";
if (contains(t, "imunidade", "isencao", "nao incidencia")) return "IMUNIDADE_ISENCAO";
if (contains(t, "planejamento tributario", "elusao", "transacao")) return "PLANEJAMENTO_TRIBUTARIO";
if (contains(t, "declaratoria tributaria")) return "DECLARATORIA_TRIBUTARIA";
if (contains(t, "cautelar fiscal")) return "CAUTELAR_FISCAL";
return "TRIBUTARIO_GERAL";
}
private String inferirSubRamoPrevidenciario(String t) {
if (contains(t, "bpc", "loas", "assistencia social")) return "BPC_LOAS";
if (contains(t, "aposentadoria especial", "atividade especial")) return "APOSENTADORIA_ESPECIAL";
if (contains(t, "aposentadoria", "aposent")) return "APOSENTADORIA";
if (contains(t, "auxilio doenca", "incapacidade", "pericia medica")) return "AUXILIO_INCAPACIDADE";
if (contains(t, "pensao por morte", "beneficio morte")) return "PENSAO_MORTE";
if (contains(t, "revisao beneficio", "revisao vida toda", "teto", "correc")) return "REVISAO_BENEFICIO";
if (contains(t, "salario maternidade", "maternidade prev")) return "SALARIO_MATERNIDADE";
if (contains(t, "rpps", "servidor publico previdencia", "iprev")) return "RPPS";
if (contains(t, "restabelecimento", "restabelecer beneficio")) return "RESTABELECIMENTO";
if (contains(t, "acidentario", "acidente trabalho prev")) return "ACIDENTARIO";
if (contains(t, "tempo rural", "trabalhador rural", "segurado especial")) return "TRABALHADOR_RURAL";
return "PREVIDENCIARIO_GERAL";
}
private String inferirSubRamoAdministrativo(String t) {
if (contains(t, "improbidade", "8429", "ato de improbidade")) return "IMPROBIDADE_ADMINISTRATIVA";
if (contains(t, "licitacao", "pregao", "tomada preco", "concorrencia")) return "LICITACAO_CONTRATOS";
if (contains(t, "concurso publico", "edital", "nomeacao", "classificacao")) return "CONCURSO_PUBLICO";
if (contains(t, "servidor publico", "estabilidade", "cargo publico")) return "SERVIDOR_PUBLICO";
if (contains(t, "acao popular", "4717", "patrimonio publico")) return "ACAO_POPULAR";
if (contains(t, "mandado seguranca", "ato coator", "autoridade")) return "MANDADO_SEGURANCA";
if (contains(t, "pad", "processo disciplinar", "sindicancia")) return "PROCESSO_DISCIPLINAR";
if (contains(t, "responsabilidade estado", "dano estado")) return "RESPONSABILIDADE_ESTADO";
if (contains(t, "desapropriacao", "utilidade publica", "necessidade publica")) return "DESAPROPRIACAO";
if (contains(t, "concessao", "permissao", "autorizacao servico publico")) return "CONCESSAO_PERMISSAO";
return "ADMINISTRATIVO_GERAL";
}
private String inferirSubRamoEleitoral(String t) {
if (contains(t, "registro candidatura", "rrc", "candidato")) return "REGISTRO_CANDIDATURA";
if (contains(t, "airc", "impugnacao registro")) return "AIRC";
if (contains(t, "aije", "investigacao judicial eleitoral")) return "AIJE";
if (contains(t, "aime", "impugnacao mandato")) return "AIME";
if (contains(t, "rced", "expedicao diploma")) return "RCED";
if (contains(t, "propaganda eleitoral", "horario gratuito")) return "PROPAGANDA";
if (contains(t, "prestacao contas", "contas eleitorais")) return "PRESTACAO_CONTAS";
if (contains(t, "direito resposta")) return "DIREITO_RESPOSTA";
if (contains(t, "inelegibilidade", "ficha limpa", "lc 64")) return "INELEGIBILIDADE";
if (contains(t, "captacao sufragio", "captacao ilicita")) return "CAPTACAO_ILICITA_SUFRAGIO";
return "ELEITORAL_GERAL";
}
private String inferirSubRamoTrabalhista(String t) {
if (contains(t, "rescisao indireta", "justa causa", "demissao")) return "RESCISAO_CONTRATO";
if (contains(t, "horas extras", "banco horas", "adicional noturno")) return "JORNADA_REMUNERACAO";
if (contains(t, "assedio moral", "assedio sexual", "dano moral trabalho")) return "DANOS_MORAIS_LABORAIS";
if (contains(t, "acidente trabalho", "doenca ocupacional", "nexo causal")) return "ACIDENTE_TRABALHO";
if (contains(t, "fgts", "multa fgts", "seguro desemprego")) return "FGTS_VERBAS";
if (contains(t, "terceirizacao", "contrato temporario", "cooperativa")) return "TERCEIRIZACAO";
if (contains(t, "greve", "dissidio coletivo", "convencao coletiva")) return "COLETIVO_SINDICAL";
if (contains(t, "gestante", "estabilidade gestante")) return "ESTABILIDADE_GESTANTE";
if (contains(t, "teletrabalho", "home office", "sobreaviso")) return "TELETRABALHO";
return "TRABALHISTA_GERAL";
}
private String inferirSubRamoEmpresarial(String t) {
if (contains(t, "falencia", "concordata", "insolvencia")) return "FALENCIA";
if (contains(t, "recuperacao judicial", "recuperacao extrajudicial")) return "RECUPERACAO_JUDICIAL";
if (contains(t, "dissolucao sociedade", "apuracao haveres", "exclusao socio")) return "SOCIETARIO";
if (contains(t, "desconsideracao personalidade juridica", "confusao patrimonial")) return "DESCONSIDERACAO";
if (contains(t, "titulo credito", "nota promissoria", "cheque", "duplicata")) return "TITULOS_CREDITO";
return "EMPRESARIAL_GERAL";
}
private String inferirSubRamoAmbiental(String t) {
if (contains(t, "licenca ambiental", "lip", "lai", "lao")) return "LICENCIAMENTO";
if (contains(t, "crime ambiental", "lei 9605")) return "CRIME_AMBIENTAL";
if (contains(t, "area protegida", "app", "reserva legal", "car")) return "AREA_PROTEGIDA";
if (contains(t, "acao civil publica ambiental")) return "ACP_AMBIENTAL";
if (contains(t, "dano ambiental", "reparacao ambiental")) return "DANO_AMBIENTAL";
return "AMBIENTAL_GERAL";
}
private String inferirSubRamoConsumidor(String t) {
if (contains(t, "produto defeituoso", "vicio produto")) return "VICIO_PRODUTO";
if (contains(t, "banco", "financeiro", "contrato bancario")) return "BANCARIO_FINANCEIRO";
if (contains(t, "plano saude cdc", "seguradora")) return "SEGURO_SAUDE_PLANO";
if (contains(t, "cobranca indevida", "negativacao", "spc serasa")) return "NEGATIVACAO_INDEVIDA";
if (contains(t, "arrependimento", "7 dias", "compra online")) return "COMERCIO_ELETRONICO";
if (contains(t, "propaganda enganosa", "publicidade abusiva")) return "PUBLICIDADE_ABUSIVA";
return "CONSUMIDOR_GERAL";
}
private String inferirSubRamoInfanciaJuventude(String t) {
if (contains(t, "ato infracional", "menor infrator", "internacao eca")) return "ECA_INFRACIONAL";
if (contains(t, "adocao", "adotar menor")) return "ADOCAO_ECA";
if (contains(t, "destituicao poder familiar")) return "DESTITUICAO_PODER_FAMILIAR";
if (contains(t, "acolhimento institucional", "lar substituto")) return "ACOLHIMENTO";
if (contains(t, "guarda menor", "tutela menor")) return "GUARDA_TUTELA_MENOR";
return "INFANCIA_GERAL";
}
private String inferirSubRamoAgrario(String t) {
if (contains(t, "desapropriacao rural", "reforma agraria", "incra")) return "DESAPROPRIACAO_RURAL";
if (contains(t, "usucapiao rural", "usucapiao pro labore")) return "USUCAPIAO_RURAL";
if (contains(t, "assentamento", "lote rural", "mst")) return "ASSENTAMENTO";
if (contains(t, "posse terra rural", "conflito fundiario")) return "CONFLITO_FUNDIARIO";
return "AGRARIO_GERAL";
}
private String inferirSubRamoConstitucional(String t) {
if (contains(t, "adi", "acao direta inconstitucionalidade")) return "ADI";
if (contains(t, "adpf")) return "ADPF";
if (contains(t, "adc", "acao declaratoria constitucionalidade")) return "ADC";
if (contains(t, "mandado injuncao")) return "MANDADO_INJUNCAO";
if (contains(t, "controle difuso")) return "CONTROLE_DIFUSO";
return "CONSTITUCIONAL_GERAL";
}
private String inferirSubRamoSaude(String t) {
if (contains(t, "medicamento sus", "fornecimento medicamento")) return "FORNECIMENTO_MEDICAMENTO";
if (contains(t, "plano saude", "cobertura plano", "negar tratamento")) return "PLANO_SAUDE";
if (contains(t, "erro medico", "responsabilidade hospitalar")) return "ERRO_MEDICO";
if (contains(t, "saude mental", "internacao psiquiatrica")) return "SAUDE_MENTAL";
if (contains(t, "leito uti", "internacao urgencia")) return "INTERNACAO_URGENTE";
return "SAUDE_GERAL";
}
private String inferirSubRamoImobiliario(String t) {
if (contains(t, "despejo", "falta pagamento aluguel")) return "DESPEJO";
if (contains(t, "usucapiao urbana")) return "USUCAPIAO_URBANA";
if (contains(t, "reintegracao posse imovel")) return "POSSE_IMOVEL";
if (contains(t, "compra venda imovel", "rescisao compra")) return "COMPRA_VENDA";
if (contains(t, "condominio")) return "CONDOMINIO";
if (contains(t, "alienacao fiduciaria imovel", "consolidacao")) return "ALIENACAO_FIDUCIARIA";
return "IMOBILIARIO_GERAL";
}
private String inferirSubRamoPI(String t) {
if (contains(t, "patente", "modelo utilidade")) return "PATENTES";
if (contains(t, "marca", "nome empresarial pi")) return "MARCAS";
if (contains(t, "direito autoral", "violacao autoral")) return "DIREITO_AUTORAL";
if (contains(t, "software", "programa computador")) return "SOFTWARE";
if (contains(t, "concorrencia desleal")) return "CONCORRENCIA_DESLEAL";
return "PI_GERAL";
}
private String inferirSubRamoInternacional(String t) {
if (contains(t, "homologacao sentenca estrangeira")) return "HOMOLOGACAO_SENTENCA";
if (contains(t, "carta rogatoria")) return "CARTA_ROGATORIA";
if (contains(t, "extraditar", "extradição")) return "EXTRADICAO";
if (contains(t, "convencao haia menor", "sequestro internacional")) return "HAIA_MENOR";
if (contains(t, "refugiado", "asilo politico")) return "REFUGIADO_ASILO";
return "INTERNACIONAL_GERAL";
}
String inferirRito(String ramo, String subRamo, Map<String, Object> ctx, String texto) {
String ritoExplicito = str(ctx.get("rito"));
if (!ritoExplicito.isBlank()) {
return ProceduralRitoNames.canonicalName(ritoExplicito);
}
ritoExplicito = str(ctx.get("rito_processual"));
if (!ritoExplicito.isBlank()) {
return ProceduralRitoNames.canonicalName(ritoExplicito);
}
return switch (ramo) {
case "PENAL" -> mapRitoPenal(subRamo, texto);
case "TRABALHISTA" -> mapRitoTrabalhista(ctx, texto);
case "PREVIDENCIARIO" -> mapRitoPrevidenciario(subRamo, texto);
case "TRIBUTARIO" -> mapRitoTributario(subRamo, texto);
case "ELEITORAL" -> mapRitoEleitoral(subRamo);
case "MILITAR" -> mapRitoMilitar(subRamo, texto);
case "ADMINISTRATIVO" -> mapRitoAdministrativo(subRamo, texto);
case "FAMILIA_SUCESSOES" -> mapRitoFamilia(subRamo, texto);
case "CONSTITUCIONAL" -> mapRitoConstitucional(subRamo);
case "AMBIENTAL" -> mapRitoAmbiental(subRamo, texto);
case "EMPRESARIAL" -> mapRitoEmpresarial(subRamo, texto);
case "INFANCIA_JUVENTUDE" -> mapRitoInfanciaJuventude(subRamo, texto);
case "AGRARIO" -> mapRitoAgrario(subRamo, texto);
case "INTERNACIONAL" -> mapRitoInternacional(subRamo);
default -> mapRitoCivil(subRamo, ctx, texto);
};
}
private String mapRitoPenal(String sub, String texto) {
return switch (sub) {
case "CRIMES_DOLOSOS_VIDA" -> "TRIBUNAL_JURI";
case "LEI_DROGAS" -> "PENAL_LEI_DROGAS";
case "VIOLENCIA_DOMESTICA" -> "PENAL_MARIA_DA_PENHA";
case "CRIMES_TRANSITO" -> "PENAL_CRIMES_TRANSITO";
case "GARANTIAS_PROCESSUAIS" -> "ESPECIAL_HABEAS_CORPUS";
case "EXECUCAO_PENAL" -> "EXECUCAO_PENAL";
case "REVISAO_CRIMINAL" -> "PENAL_REVISAO_CRIMINAL";
case "ORGANIZACAO_CRIMINOSA" -> "PENAL_ORGANIZACAO_CRIMINOSA";
case "CRIMES_CIBERNETICOS" -> "PENAL_CRIMES_CIBERNETICOS";
case "RACISMO_DISCRIMINACAO" -> "PENAL_RACISMO";
case "TORTURA" -> "PENAL_TORTURA";
case "TERRORISMO" -> "PENAL_TERRORISMO";
case "VIOLENCIA_POLITICA" -> "PENAL_VIOLENCIA_POLITICA";
default -> {
if (contains(texto, "sumario", "pena 2 a 4")) yield "PROCEDIMENTO_PENAL_SUMARIO";
if (contains(texto, "jecrim", "juizado especial criminal")) yield "JUIZADO_ESPECIAL_CRIMINAL";
yield "PROCEDIMENTO_PENAL_COMUM";
}
};
}
private String mapRitoTrabalhista(Map<String, Object> ctx, String texto) {
Double valor = toDouble(ctx.get("valor_causa"));
if (contains(texto, "inquerito judicial", "falta grave", "art. 853", "art 853")) return "TRABALHISTA_INQUERITO_FALTA_GRAVE";
if (contains(texto, "acao de cumprimento", "ação de cumprimento", "art. 872", "art 872")) return "TRABALHISTA_ACAO_CUMPRIMENTO";
if (contains(texto, "dissidio coletivo")) return "TRABALHISTA_DISSIDIO_COLETIVO";
if (contains(texto, "sumario", "alcada", "alçada", "lei 5.584", "lei 5584")) return "TRABALHISTA_SUMARIO_ALCADA";
if (valor != null && valor <= 2.0 * 1621.0) return "TRABALHISTA_SUMARIO_ALCADA";
if (valor != null && valor <= 40.0 * 1621.0 && !contains(texto, "autarquia", "fundacao publica", "fundação pública", "administracao publica", "administração pública", "municipio", "prefeitura", "estado", "uniao", "união")) return "TRABALHISTA_SUMARISSIMO";
if (contains(texto, "sumarissimo")) return "TRABALHISTA_SUMARISSIMO";
if (contains(texto, "acao rescisoria")) return "TRABALHISTA_ACAO_RESCISORIA";
if (contains(texto, "mandado seguranca trabalhista")) return "TRABALHISTA_MANDADO_SEGURANCA";
if (contains(texto, "acidente trabalho", "doenca ocupacional", "doença ocupacional")) return "TRABALHISTA_ACIDENTE_TRABALHO";
return "TRABALHISTA_ORDINARIO";
}
private String mapRitoPrevidenciario(String sub, String texto) {
if (contains(texto, "jef", "juizado especial federal")) return "PREVIDENCIARIO_JEF";
return switch (sub) {
case "BPC_LOAS" -> "PREVIDENCIARIO_BPC_LOAS";
case "APOSENTADORIA" -> "PREVIDENCIARIO_APOSENTADORIA";
case "APOSENTADORIA_ESPECIAL" -> "PREVIDENCIARIO_ESPECIAL";
case "AUXILIO_INCAPACIDADE" -> "PREVIDENCIARIO_AUXILIO_INCAPACIDADE";
case "REVISAO_BENEFICIO" -> "PREVIDENCIARIO_REVISAO_BENEFICIO";
case "RESTABELECIMENTO" -> "PREVIDENCIARIO_RESTABELECIMENTO";
case "SALARIO_MATERNIDADE" -> "PREVIDENCIARIO_SALARIO_MATERNIDADE";
case "PENSAO_MORTE" -> "PREVIDENCIARIO_PENSAO_MORTE";
case "TRABALHADOR_RURAL" -> "PREVIDENCIARIO_RURAL";
case "RPPS" -> "PREVIDENCIARIO_RPPS";
case "ACIDENTARIO" -> "PREVIDENCIARIO_ACIDENTARIO";
default -> "PREVIDENCIARIO_COMUM";
};
}
private String mapRitoTributario(String sub, String texto) {
return switch (sub) {
case "EXECUCAO_FISCAL" -> "EXECUCAO_FISCAL";
case "REPETICAO_INDEBITO" -> "TRIBUTARIO_REPETICAO_INDEBITO";
case "ANULATORIA_DEBITO" -> "TRIBUTARIO_ANULATORIA_DEBITO";
case "MANDADO_SEGURANCA_TRIBUTARIO" -> "TRIBUTARIO_MANDADO_SEGURANCA";
case "EMBARGOS_EXECUCAO_FISCAL" -> "TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL";
case "DECLARATORIA_TRIBUTARIA" -> "TRIBUTARIO_DECLARATORIA";
case "CAUTELAR_FISCAL" -> "TRIBUTARIO_CAUTELAR_FISCAL";
default -> "FAZENDA_PUBLICA_CONHECIMENTO";
};
}
private String mapRitoEleitoral(String sub) {
return switch (sub) {
case "REGISTRO_CANDIDATURA" -> "ELEITORAL_REGISTRO_CANDIDATURA";
case "AIRC" -> "ELEITORAL_AIRC";
case "AIJE" -> "ELEITORAL_AIJE";
case "AIME" -> "ELEITORAL_AIME";
case "RCED" -> "ELEITORAL_RCED";
case "PROPAGANDA" -> "ELEITORAL_PROPAGANDA";
case "PRESTACAO_CONTAS" -> "ELEITORAL_PRESTACAO_CONTAS";
case "DIREITO_RESPOSTA" -> "ELEITORAL_DIREITO_RESPOSTA";
case "INELEGIBILIDADE" -> "ELEITORAL_INELEGIBILIDADE";
case "CAPTACAO_ILICITA_SUFRAGIO" -> "ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO";
default -> "ELEITORAL";
};
}
private String mapRitoMilitar(String sub, String texto) {
if (contains(texto, "ipm", "inquerito policial militar")) return "MILITAR_IPM";
if (contains(texto, "conselho justica")) return "MILITAR_CONSELHO_JUSTICA";
if (contains(texto, "pad", "disciplinar militar")) return "MILITAR_PAD";
if (contains(texto, "cppm", "processo penal militar")) return "MILITAR_PROCESSO_PENAL_MILITAR";
if (contains(texto, "habeas corpus militar")) return "MILITAR_HABEAS_CORPUS_MILITAR";
return "MILITAR";
}
private String mapRitoAdministrativo(String sub, String texto) {
return switch (sub) {
case "IMPROBIDADE_ADMINISTRATIVA" -> "IMPROBIDADE_ADMINISTRATIVA";
case "ACAO_POPULAR" -> "ESPECIAL_ACAO_POPULAR";
case "MANDADO_SEGURANCA" -> "ESPECIAL_MANDADO_SEGURANCA";
case "PROCESSO_DISCIPLINAR" -> "ADMINISTRATIVO_PAD";
case "CONCURSO_PUBLICO" -> "ADMINISTRATIVO_CONCURSO_PUBLICO";
case "SERVIDOR_PUBLICO" -> "ADMINISTRATIVO_SERVIDORES";
default -> "FAZENDA_PUBLICA_CONHECIMENTO";
};
}
private String mapRitoFamilia(String sub, String texto) {
return switch (sub) {
case "ALIMENTOS" -> "CIVIL_FAMILIA_ALIMENTOS";
case "DIVORCIO", "GUARDA_VISITAS", "UNIAO_ESTAVEL" -> "CIVIL_FAMILIA_DIVORCIO";
case "SUCESSOES" -> "CIVIL_INVENTARIO_ARROLAMENTO";
case "ADOCAO" -> "CIVIL_ADOCAO";
case "TUTELA_CURATELA" -> "CIVIL_TUTELA_CURATELA";
case "PATERNIDADE" -> "CIVIL_INVESTIGACAO_PATERNIDADE";
default -> {
if (contains(texto, "tutela urgencia", "liminar familia", "medida protetiva")) {
yield "CIVIL_TUTELA_URGENTE";
}
yield "CIVIL_FAMILIA_DIVORCIO";
}
};
}
private String mapRitoConstitucional(String sub) {
return switch (sub) {
case "ADI" -> "ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE";
case "ADPF" -> "ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL";
case "ADC" -> "ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE";
case "MANDADO_INJUNCAO" -> "ESPECIAL_MANDADO_INJUNCAO";
default -> "ESPECIAL_MANDADO_SEGURANCA";
};
}
private String mapRitoAmbiental(String sub, String texto) {
if ("CRIME_AMBIENTAL".equals(sub)) return "AMBIENTAL_CRIMINAL";
if ("ACP_AMBIENTAL".equals(sub)) return "AMBIENTAL_ACP";
if (contains(texto, "tutela urgencia ambiental")) return "AMBIENTAL_TUTELA_URGENTE";
return "CIVIL_ACAO_CIVIL_PUBLICA";
}
private String mapRitoEmpresarial(String sub, String texto) {
return switch (sub) {
case "FALENCIA" -> "FALENCIA";
case "RECUPERACAO_JUDICIAL" -> "RECUPERACAO_JUDICIAL";
case "DESCONSIDERACAO" -> "INCIDENTE_DESCONSIDERACAO_PERSONALIDADE_JURIDICA";
default -> "COMUM_ORDINARIO";
};
}
private String mapRitoInfanciaJuventude(String sub, String texto) {
return switch (sub) {
case "ECA_INFRACIONAL" -> "INFANCIA_JUVENTUDE_INFRACIONAL";
case "ADOCAO_ECA" -> "INFANCIA_JUVENTUDE_ADOCAO";
case "DESTITUICAO_PODER_FAMILIAR" -> "INFANCIA_JUVENTUDE_ECA";
case "GUARDA_TUTELA_MENOR" -> "INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR";
default -> "INFANCIA_JUVENTUDE_ECA";
};
}
private String mapRitoAgrario(String sub, String texto) {
return switch (sub) {
case "DESAPROPRIACAO_RURAL" -> "AGRARIO_DESAPROPRIACAO";
case "USUCAPIAO_RURAL" -> "AGRARIO_USUCAPIAO_RURAL";
case "CONFLITO_FUNDIARIO" -> "AGRARIO_ACP_AGRARIA";
default -> "AGRARIO_POSSE_TERRA";
};
}
private String mapRitoInternacional(String sub) {
return switch (sub) {
case "HOMOLOGACAO_SENTENCA" -> "HOMOLOGACAO_SENTENCA_ESTRANGEIRA";
case "CARTA_ROGATORIA" -> "CARTA_ROGATORIA";
default -> "COOPERACAO_JURIDICA_INTERNACIONAL";
};
}
private String mapRitoCivil(String sub, Map<String, Object> ctx, String texto) {
if (contains(texto, "mandado seguranca")) return "ESPECIAL_MANDADO_SEGURANCA";
if (contains(texto, "habeas data")) return "ESPECIAL_HABEAS_DATA";
if (contains(texto, "acao civil publica", "acp")) return "CIVIL_ACAO_CIVIL_PUBLICA";
if (contains(texto, "tutela urgencia", "antecipacao tutela", "liminar")) return "CIVIL_TUTELA_URGENTE";
if (contains(texto, "titulo extrajudicial")) return "EXECUCAO_TITULO_EXTRAJUDICIAL";
if (contains(texto, "usucapiao")) return "CIVIL_USUCAPIAO";
if (contains(texto, "reintegracao", "imissao", "nunciacao")) return "CIVIL_POSSESSORIA";
if (contains(texto, "monitoria")) return "CIVIL_ACAO_MONITORIA";
if ("EXECUCAO_CIVIL".equals(sub)) return "EXECUCAO_TITULO_EXTRAJUDICIAL";
if ("TUTELA_URGENCIA".equals(sub)) return "CIVIL_TUTELA_URGENTE";
if ("USUCAPIAO".equals(sub)) return "CIVIL_USUCAPIAO";
if ("MONITORIA".equals(sub)) return "CIVIL_ACAO_MONITORIA";
Double valor = toDouble(ctx.get("valor_causa"));
if (valor != null) {
if (valor <= 20.0 * 1760.0) return "JUIZADO_ESPECIAL_CIVEL";
}
if (contains(texto, "juizado especial", "jec", "jecrim")) return "JUIZADO_ESPECIAL_CIVEL";
if (contains(texto, "fazenda publica", "ente publico")) return "FAZENDA_PUBLICA_CONHECIMENTO";
return "COMUM_ORDINARIO";
}

private static String normalizeEnumKey(String s) {
if (s == null) return "";
String x = normalizeTexto(s).toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_').replace('/', '_');
while (x.contains("__")) x = x.replace("__", "_");
return x;
}

private static String normalizeTexto(String s) {
if (s == null) return "";
return s.toLowerCase(Locale.ROOT)
.replace('á','a').replace('à','a').replace('ã','a').replace('â','a')
.replace('é','e').replace('ê','e')
.replace('í','i')
.replace('ó','o').replace('ô','o').replace('õ','o')
.replace('ú','u')
.replace('ç','c');
}

private static boolean contains(String texto, String... tokens) {
String normalized = normalizeTexto(texto);
for (String token : tokens) if (normalized.contains(normalizeTexto(token))) return true;
return false;
}

private static String str(Object v) {
return v == null ? "" : String.valueOf(v).trim();
}

private static boolean bool(Object v) {
if (v instanceof Boolean b) return b;
if (v == null) return false;
String s = str(v).toLowerCase(Locale.ROOT);
return "true".equals(s) || "1".equals(s) || "sim".equals(s) || "yes".equals(s);
}

private static Double toDouble(Object v) {
if (v instanceof Number n) return n.doubleValue();
String s = str(v).replace('.', '#').replace(',', '.').replace('#', ',');
if (s.isBlank()) return null;
try {
return Double.parseDouble(s.replace(",", ""));
} catch (NumberFormatException ignored) {
return null;
}
}
}
