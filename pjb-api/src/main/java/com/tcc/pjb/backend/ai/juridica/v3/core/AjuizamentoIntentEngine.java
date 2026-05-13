package com.tcc.pjb.backend.ai.juridica.v3.core;

import com.tcc.pjb.backend.ai.academy.CurriculumKnowledgeService;
import com.tcc.pjb.backend.ai.academy.CurriculumSnapshot;
import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.CanonicalRitoSelector.SelectedRito;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.procedural.ProceduralRitoNames;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
@Service
public class AjuizamentoIntentEngine {
private static final double CONFIDENCE_HIGH = 0.93;
private static final double CONFIDENCE_MEDIUM = 0.74;
private static final double CONFIDENCE_LOW = 0.51;
private final CurriculumKnowledgeService curriculumKnowledgeService;
private final CanonicalRitoSelector canonicalRitoSelector;
private final NationalProceduralRoutingService nationalProceduralRoutingService;
private final AjuizamentoIntentClassificationSupport classificationSupport;
public AjuizamentoIntentEngine(CurriculumKnowledgeService curriculumKnowledgeService, CanonicalRitoSelector canonicalRitoSelector, NationalProceduralRoutingService nationalProceduralRoutingService, AjuizamentoIntentClassificationSupport classificationSupport) {
this.curriculumKnowledgeService = curriculumKnowledgeService;
this.canonicalRitoSelector = canonicalRitoSelector;
this.nationalProceduralRoutingService = nationalProceduralRoutingService;
this.classificationSupport = classificationSupport;
}
public AjuizamentoIntent inferir(Map<String, Object> ctx) {
Objects.requireNonNull(ctx, "ctx");
Map<String, Object> safe = sanitize(ctx);
String texto = buildTextoConsolidado(safe);
String esferaInferida = inferirEsfera(safe, texto);
String ramoInferido = inferirRamo(safe, texto);
String ramoBase = BrazilianLegalKnowledgeBase.resolveRamoCodigo(ramoInferido);
String subRamo = inferirSubRamo(ramoBase, safe, texto);
SelectedRito selectedRito = canonicalRitoSelector.select(
        buildCanonicalPayload(safe, ramoBase, esferaInferida),
        inferirRito(ramoBase, subRamo, safe, texto),
        "ajuizamento_intent_engine"
);
CanonicalContext canonical = selectedRito.canonicalContext();
String rito = selectedRito.rito() != null ? selectedRito.rito().name() : null;
String ramo = canonical.ramoDireito() != null && !canonical.ramoDireito().isBlank()
? BrazilianLegalKnowledgeBase.resolveRamoCodigo(canonical.ramoDireito())
: ramoBase;
String esfera = canonical.ramoJusticaNacional() != null && !canonical.ramoJusticaNacional().isBlank() ? canonical.ramoJusticaNacional() : esferaInferida;
String tipoAcao = inferirTipoAcao(rito, ramo, subRamo, texto);
String competencia = refineCompetencia(firstNonBlank(canonical.tribunalCodigo(), inferirCompetencia(rito, esfera, ramo, safe, texto)), ramo, subRamo, esfera, safe);
String fundamento = refineFundamento(inferirFundamento(rito, ramo, subRamo, texto), ramo, subRamo, rito);
double confianca = recalibrarConfianca(calcularConfianca(safe, rito, ramo), safe, texto, ramo, subRamo, rito);
boolean segredo = inferirSegredo(ramo, rito);
boolean exigeMP = inferirExigeMP(ramo, subRamo, rito);
boolean conciliar = inferirConciliacao(ramo, rito);
CurriculumSnapshot curriculum = curriculumKnowledgeService.snapshot(ramo, subRamo, ProceduralRitoNames.parse(rito));
ProceduralRoutingReport proceduralRouting = nationalProceduralRoutingService.analyzeContext(safe);
List<String> campos = mergeDistinct(
        camposObrigatoriosPara(rito, ramo),
        derivedCamposObrigatorios(safe, ramo, subRamo, rito),
        canonical.requiredPartyRoles().stream().map(this::fieldAliasForRole).toList()
);
List<String> alertas = mergeDistinct(
        alertasPara(rito, ramo, safe, texto),
        derivedAlertas(safe, ramo, subRamo, rito, esfera, confianca),
        canonicalAlerts(canonical, safe),
        selectionAlerts(selectedRito),
        proceduralRouting != null ? proceduralRouting.alerts() : List.of(),
        proceduralRouting != null ? proceduralRouting.reasons() : List.of()
);
List<String> documentos = mergeDistinct(
        documentosEssenciaisPara(rito, ramo, texto),
        derivedDocumentos(ramo, subRamo, safe),
        canonical.requiredDocuments()
);
List<String> proximosPassos = mergeDistinct(
        proximosPassosPara(rito, ramo, esfera),
        derivedPassos(ramo, subRamo, rito, curriculum, safe),
        canonicalPassos(canonical),
        proceduralRouting != null ? proceduralRouting.reviewChecklist() : List.of()
);
return new AjuizamentoIntent(
        rito, ramo, subRamo, esfera, competencia, tipoAcao, fundamento, confianca,
        List.copyOf(campos),
        List.copyOf(alertas),
        List.copyOf(documentos),
        List.copyOf(proximosPassos),
        segredo, exigeMP, conciliar,
        proceduralRouting
);
}

private Map<String, Object> buildCanonicalPayload(Map<String, Object> safe, String ramo, String esfera) {
Map<String, Object> payload = new LinkedHashMap<>(safe);
payload.putIfAbsent("ramoDireito", ramo);
payload.putIfAbsent("esfera", esfera);
payload.putIfAbsent("competencia", esfera);
payload.putIfAbsent("materia", ramo);
return payload;
}
private List<String> canonicalAlerts(CanonicalContext canonical, Map<String, Object> safe) {
List<String> out = new ArrayList<>();
if (canonical.requiredDocuments().isEmpty()) {
return out;
}
for (String doc : canonical.requiredDocuments()) {
String normalized = normalizeEnumKey(doc);
if (!safe.containsKey(doc) && !safe.containsKey(normalized.toLowerCase(Locale.ROOT))) {
out.add("Documento canônico relevante ao rito: " + normalized);
}
}
if (canonical.tribunalCodigo() == null || canonical.tribunalCodigo().isBlank()) {
out.add("Tribunal não resolvido de forma canônica; revisão de competência recomendada.");
}
return out;
}
private List<String> canonicalPassos(CanonicalContext canonical) {
List<String> out = new ArrayList<>();
if (canonical.tribunalCodigo() != null && !canonical.tribunalCodigo().isBlank()) {
out.add("Conferir protocolo e distribuição no tribunal " + canonical.tribunalCodigo() + '.');
}
if (!canonical.requiredDocuments().isEmpty()) {
out.add("Consolidar anexos obrigatórios do catálogo procedural antes do protocolo.");
}
if (!canonical.requiredPartyRoles().isEmpty()) {
out.add("Validar qualificação das partes canônicas exigidas pelo rito selecionado.");
}
return out;
}
private List<String> selectionAlerts(SelectedRito selectedRito) {
List<String> out = new ArrayList<>();
if (selectedRito == null) {
return out;
}
if (selectedRito.heuristicUsed()) {
out.add("Rito final fechado por compatibilidade heurística controlada: " + selectedRito.rito().name());
}
if (selectedRito.fallbackApplied()) {
out.add("Rito final entrou em compatibilidade mínima; revisão procedural obrigatória.");
}
if (selectedRito.status() != null && !selectedRito.status().isBlank() && !"CANONICAL_RITO_RESOLVED".equals(selectedRito.status())) {
out.add("Estado procedural atual: " + selectedRito.status());
}
return out;
}
private String fieldAliasForRole(String role) {
String normalized = normalizeEnumKey(role);
return switch (normalized) {
case "AUTOR", "IMPETRANTE", "REPRESENTANTE", "REQUERENTE", "RECLAMANTE", "EXEQUENTE" -> "parteAutoraNome";
case "REU", "REPRESENTADO", "IMPUGNADO", "RECLAMADA", "EXECUTADO", "AUTORIDADE_COATORA", "PACIENTE", "ACUSADO" -> "parteReuNome";
default -> "parte_" + normalized.toLowerCase(Locale.ROOT);
};
}
private String firstNonBlank(String... values) {
for (String value : values) {
if (value != null && !value.isBlank()) return value;
}
return null;
}
private String buildTextoConsolidado(Map<String, Object> ctx) {
String[] campos = {
"assunto", "resumo", "narrativa", "fatos", "pedido", "pedidos",
"descricao", "tipo_acao", "classe", "classe_cnj", "materia",
"ambito_direito", "ramo_direito", "rito", "rito_processual",
"comarca", "vara", "tribunal", "observacoes", "ementa",
"causa_pedir", "qualificacao_partes", "valor_causa_descricao",
"area_direito", "especialidade", "natureza_acao"
};
StringBuilder sb = new StringBuilder();
for (String c : campos) {
Object v = ctx.get(c);
if (v != null) sb.append(' ').append(v);
}
return normalizeTexto(sb.toString());
}
private String inferirEsfera(Map<String, Object> ctx, String texto) {
return classificationSupport.inferirEsfera(ctx, texto);
}
private String inferirRamo(Map<String, Object> ctx, String texto) {
return classificationSupport.inferirRamo(ctx, texto);
}
private String inferirSubRamo(String ramo, Map<String, Object> ctx, String texto) {
return classificationSupport.inferirSubRamo(ramo, ctx, texto);
}
private String inferirRito(String ramo, String subRamo, Map<String, Object> ctx, String texto) {
return classificationSupport.inferirRito(ramo, subRamo, ctx, texto);
}
private String inferirTipoAcao(String rito, String ramo, String subRamo, String texto) {
if (rito == null) return "ACAO_COMUM";
return switch (rito) {
case "ESPECIAL_MANDADO_SEGURANCA", "ESPECIAL_MANDADO_SEGURANCA_COLETIVO" -> "MANDADO_DE_SEGURANCA";
case "ESPECIAL_HABEAS_CORPUS" -> "HABEAS_CORPUS";
case "ESPECIAL_HABEAS_DATA" -> "HABEAS_DATA";
case "ESPECIAL_ACAO_POPULAR", "ADMINISTRATIVO_ACAO_POPULAR" -> "ACAO_POPULAR";
case "CIVIL_ACAO_CIVIL_PUBLICA", "AMBIENTAL_ACP", "ADMINISTRATIVO_ACAO_CIVIL_PUBLICA_ADM" -> "ACAO_CIVIL_PUBLICA";
case "CIVIL_TUTELA_URGENTE", "CIVIL_TUTELA_CAUTELAR_ANTECEDENTE", "CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE" -> "TUTELA_CAUTELAR_ANTECIPADA";
case "CIVIL_FAMILIA_ALIMENTOS" -> "ACAO_ALIMENTOS";
case "CIVIL_FAMILIA_DIVORCIO" -> "ACAO_DIVORCIO_DISSOLUCAO";
case "CIVIL_DISSOLUCAO_CASAMENTO" -> "DISSOLUCAO_CASAMENTO";
case "CIVIL_INVENTARIO_ARROLAMENTO" -> "INVENTARIO_ARROLAMENTO";
case "CIVIL_ADOCAO" -> "ACAO_ADOCAO";
case "CIVIL_TUTELA_CURATELA" -> "TUTELA_CURATELA";
case "CIVIL_INVESTIGACAO_PATERNIDADE" -> "INVESTIGACAO_PATERNIDADE";
case "CIVIL_USUCAPIAO" -> "ACAO_USUCAPIAO";
case "CIVIL_POSSESSORIA" -> "ACAO_POSSESSORIA";
case "CIVIL_ACAO_MONITORIA" -> "ACAO_MONITORIA";
case "EXECUCAO_FISCAL", "FAZENDA_PUBLICA_EXECUCAO" -> "EXECUCAO_FISCAL";
case "EXECUCAO_TITULO_EXTRAJUDICIAL" -> "EXECUCAO_TITULO_EXTRAJUDICIAL";
case "EXECUCAO_TITULO_JUDICIAL", "CUMPRIMENTO_SENTENCA" -> "CUMPRIMENTO_SENTENCA";
case "IMPROBIDADE_ADMINISTRATIVA" -> "ACAO_IMPROBIDADE_ADMINISTRATIVA";
case "ADMINISTRATIVO_PAD" -> "PROCESSO_DISCIPLINAR_ADMINISTRATIVO";
case "ADMINISTRATIVO_CONCURSO_PUBLICO" -> "ACAO_CONCURSO_PUBLICO";
case "TRIBUNAL_JURI" -> "PROCEDIMENTO_JURI";
case "PENAL_MARIA_DA_PENHA" -> "ACAO_PENAL_LEI_11340";
case "PENAL_LEI_DROGAS" -> "ACAO_PENAL_LEI_11343";
case "PENAL_REVISAO_CRIMINAL" -> "REVISAO_CRIMINAL";
case "EXECUCAO_PENAL" -> "EXECUCAO_PENAL";
case "TRABALHISTA_ORDINARIO", "TRABALHISTA_SUMARISSIMO", "TRABALHISTA_SUMARIO_ALCADA" -> "RECLAMACAO_TRABALHISTA";
case "TRABALHISTA_DISSIDIO_COLETIVO" -> "DISSIDIO_COLETIVO";
case "TRABALHISTA_INQUERITO_FALTA_GRAVE" -> "INQUERITO_JUDICIAL_FALTA_GRAVE";
case "TRABALHISTA_ACAO_CUMPRIMENTO" -> "ACAO_CUMPRIMENTO_TRABALHISTA";
case "TRABALHISTA_ACAO_RESCISORIA" -> "ACAO_RESCISORIA_TRABALHISTA";
case "TRABALHISTA_ACIDENTE_TRABALHO" -> "RECLAMACAO_ACIDENTE_TRABALHO";
case "JUIZADO_ESPECIAL_CIVEL" -> "ACAO_JUIZADO_ESPECIAL_CIVEL";
case "JUIZADO_ESPECIAL_FEDERAL" -> "ACAO_JUIZADO_ESPECIAL_FEDERAL";
case "JUIZADO_ESPECIAL_FAZENDA_PUBLICA" -> "ACAO_JUIZADO_ESPECIAL_FAZENDA";
case "PREVIDENCIARIO_JEF", "PREVIDENCIARIO_COMUM" -> "ACAO_PREVIDENCIARIA";
case "PREVIDENCIARIO_BPC_LOAS" -> "ACAO_BPC_LOAS";
case "PREVIDENCIARIO_REVISAO_BENEFICIO" -> "REVISAO_BENEFICIO_PREVIDENCIARIO";
case "PREVIDENCIARIO_RESTABELECIMENTO" -> "RESTABELECIMENTO_BENEFICIO";
case "TRIBUTARIO_ANULATORIA_DEBITO" -> "ACAO_ANULATORIA_DEBITO_FISCAL";
case "TRIBUTARIO_REPETICAO_INDEBITO" -> "REPETICAO_INDEBITO";
case "TRIBUTARIO_MANDADO_SEGURANCA" -> "MANDADO_SEGURANCA_TRIBUTARIO";
case "TRIBUTARIO_EMBARGOS_EXECUCAO_FISCAL" -> "EMBARGOS_EXECUCAO_FISCAL";
case "ELEITORAL_AIRC" -> "AIRC";
case "ELEITORAL_AIJE" -> "AIJE";
case "ELEITORAL_AIME" -> "AIME";
case "ELEITORAL_RCED" -> "RCED";
case "RECUPERACAO_JUDICIAL" -> "RECUPERACAO_JUDICIAL";
case "FALENCIA" -> "ACAO_FALENCIA";
case "INCIDENTE_DESCONSIDERACAO_PERSONALIDADE_JURIDICA" -> "INCIDENTE_DESCONSIDERACAO_PJ";
case "ESPECIAL_MANDADO_INJUNCAO", "ESPECIAL_MANDADO_INJUNCAO_COLETIVO" -> "MANDADO_INJUNCAO";
case "ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE" -> "ACAO_DIRETA_INCONSTITUCIONALIDADE";
case "ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE" -> "ACAO_DECLARATORIA_CONSTITUCIONALIDADE";
case "ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL" -> "ADPF";
case "HOMOLOGACAO_SENTENCA_ESTRANGEIRA" -> "HOMOLOGACAO_SENTENCA_ESTRANGEIRA";
case "CARTA_ROGATORIA" -> "CARTA_ROGATORIA";
case "INFANCIA_JUVENTUDE_INFRACIONAL" -> "ACAO_ECA_INFRACIONAL";
case "INFANCIA_JUVENTUDE_ADOCAO" -> "ACAO_ADOCAO_ECA";
case "AGRARIO_DESAPROPRIACAO" -> "ACAO_DESAPROPRIACAO_RURAL";
case "ARBITRAGEM" -> "ARBITRAGEM";
default -> "ACAO_COMUM_" + ramo;
};
}
private String inferirCompetencia(String rito, String esfera, String ramo, Map<String, Object> ctx, String texto) {
if ("FEDERAL".equals(esfera)) return inferirCompetenciaFederal(rito, ctx, texto);
if ("ELEITORAL".equals(esfera) || (rito != null && rito.startsWith("ELEITORAL"))) {
return inferirCompetenciaEleitoral(ctx);
}
if ("MILITAR".equals(esfera) || (rito != null && rito.startsWith("MILITAR"))) {
return "JUSTICA_MILITAR";
}
if (rito != null && rito.startsWith("TRABALHISTA")) return "JUSTICA_TRABALHO";
if (ProceduralRitoNames.isOneOf(rito, "ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE", "ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL", "ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE")) {
return "STF";
}
if (ProceduralRitoNames.isOneOf(rito, "HOMOLOGACAO_SENTENCA_ESTRANGEIRA", "CARTA_ROGATORIA")) {
return "STJ";
}
return inferirCompetenciaEstadual(rito, ramo, ctx, texto);
}
private String inferirCompetenciaFederal(String rito, Map<String, Object> ctx, String texto) {
String estado = str(ctx.get("estado")).toUpperCase();
if (ProceduralRitoNames.isOneOf(rito, "JUIZADO_ESPECIAL_FEDERAL", "PREVIDENCIARIO_JEF", "PREVIDENCIARIO_BPC_LOAS")) {
return estado.isBlank() ? "JEF" : "JEF_" + estado;
}
return estado.isBlank() ? "JUSTICA_FEDERAL" : "JUSTICA_FEDERAL_SECAO_" + estado;
}
private String inferirCompetenciaEleitoral(Map<String, Object> ctx) {
String cargo = str(ctx.get("cargo"));
if (contains(cargo, "presidente", "senador", "governador", "federal")) return "TRE_TSE";
String municipio = str(ctx.get("municipio")).toUpperCase();
return municipio.isBlank() ? "ZONA_ELEITORAL" : "ZONA_ELEITORAL_" + municipio;
}
private String inferirCompetenciaEstadual(String rito, String ramo, Map<String, Object> ctx, String texto) {
String vara = str(ctx.get("vara")).toUpperCase();
if (!vara.isBlank()) return vara;
if (rito == null) return "COMARCA_" + str(ctx.get("comarca")).toUpperCase();
return switch (rito) {
case "CIVIL_FAMILIA_ALIMENTOS", "CIVIL_FAMILIA_DIVORCIO", "CIVIL_ADOCAO",
"CIVIL_INVESTIGACAO_PATERNIDADE", "CIVIL_TUTELA_CURATELA" -> "VARA_FAMILIA_SUCESSOES";
case "CIVIL_INVENTARIO_ARROLAMENTO" -> "VARA_ORFAOS_SUCESSOES";
case "PENAL_MARIA_DA_PENHA" -> "VARA_VIOLENCIA_DOMESTICA";
case "TRIBUNAL_JURI" -> "TRIBUNAL_JURI";
case "IMPROBIDADE_ADMINISTRATIVA", "FAZENDA_PUBLICA_CONHECIMENTO",
"FAZENDA_PUBLICA_EXECUCAO" -> "VARA_FAZENDA_PUBLICA";
case "EXECUCAO_FISCAL" -> "VARA_EXECUCAO_FISCAL";
case "INFANCIA_JUVENTUDE_ECA", "INFANCIA_JUVENTUDE_INFRACIONAL",
"INFANCIA_JUVENTUDE_ADOCAO", "INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR" -> "VARA_INFANCIA_JUVENTUDE";
case "AGRARIO_DESAPROPRIACAO", "AGRARIO_USUCAPIAO_RURAL",
"AGRARIO_ACP_AGRARIA", "AGRARIO_POSSE_TERRA" -> "VARA_AGRARIA";
default -> "VARA_CIVEL_" + str(ctx.get("comarca")).toUpperCase();
};
}
private String inferirFundamento(String rito, String ramo, String subRamo, String texto) {
List<String> normas = new ArrayList<>();
normas.add("CF/1988 art. 5º (acesso à justiça, devido processo legal, contraditório e ampla defesa)");
switch (ramo) {
case "CIVIL" -> {
normas.add("CPC/2015 — Lei 13.105/2015");
if ("RESPONSABILIDADE_CIVIL".equals(subRamo)) normas.add("CC/2002 art. 186, 187, 927-954");
if ("CONTRATOS".equals(subRamo)) normas.add("CC/2002 art. 421-480; Princípio da boa-fé objetiva");
if ("USUCAPIAO".equals(subRamo)) normas.add("CC/2002 art. 1.238-1.244; CF art. 183");
if ("LOCACAO".equals(subRamo)) normas.add("Lei 8.245/91 (Lei do Inquilinato)");
}
case "PENAL" -> {
normas.add("CP — Dec.-Lei 2.848/40; CPP — Dec.-Lei 3.689/41");
if ("LEI_DROGAS".equals(subRamo)) normas.add("Lei 11.343/2006");
if ("VIOLENCIA_DOMESTICA".equals(subRamo)) normas.add("Lei 11.340/2006 (Lei Maria da Penha)");
if ("CRIMES_TRANSITO".equals(subRamo)) normas.add("Lei 9.503/97 (CTB) arts. 302-308");
if ("ECA_PENAL".equals(subRamo)) normas.add("Lei 8.069/90 (ECA)");
if ("ORGANIZACAO_CRIMINOSA".equals(subRamo)) normas.add("Lei 12.850/2013");
if ("LAVAGEM_DINHEIRO".equals(subRamo)) normas.add("Lei 9.613/98");
if ("RACISMO_DISCRIMINACAO".equals(subRamo)) normas.add("Lei 7.716/89; CF art. 5º, XLII");
if ("CRIMES_CIBERNETICOS".equals(subRamo)) normas.add("Lei 12.737/2012; Lei 14.155/2021");
}
case "TRABALHISTA" -> {
normas.add("CLT — Dec.-Lei 5.452/43; CF/1988 art. 7º-11; Lei 13.467/2017 (Reforma)");
normas.add("Lei 9.029/95 (práticas discriminatórias nas relações de trabalho)");
}
case "PREVIDENCIARIO" -> {
normas.add("Lei 8.213/91 (Planos de Benefícios); Lei 8.212/91 (Custeio); EC 103/2019");
if ("BPC_LOAS".equals(subRamo)) normas.add("Lei 8.742/93 (LOAS) art. 20");
if ("APOSENTADORIA_ESPECIAL".equals(subRamo)) normas.add("Lei 8.213/91 art. 57-58; Decreto 3.048/99");
if ("TRABALHADOR_RURAL".equals(subRamo)) normas.add("Lei 8.213/91 art. 11, VII; TNU Súmula 83");
}
case "TRIBUTARIO" -> {
normas.add("CTN — Lei 5.172/66; CF/1988 art. 145-162");
if ("EXECUCAO_FISCAL".equals(subRamo)) normas.add("Lei 6.830/80 (LEF)");
if ("CAUTELAR_FISCAL".equals(subRamo)) normas.add("Lei 8.397/92 (Cautelar Fiscal)");
}
case "ADMINISTRATIVO" -> {
normas.add("Lei 14.133/2021 (NLLC); Lei 9.784/99 (PAF); CF art. 37");
if ("IMPROBIDADE_ADMINISTRATIVA".equals(subRamo)) normas.add("Lei 8.429/92 com redação da Lei 14.230/2021 (dolo específico)");
if ("MANDADO_SEGURANCA".equals(subRamo)) normas.add("Lei 12.016/2009 (Lei do MS); CF art. 5º, LXIX");
if ("DESAPROPRIACAO".equals(subRamo)) normas.add("Dec.-Lei 3.365/41; LC 76/93");
}
case "ELEITORAL" -> normas.add("CE — Lei 4.737/65; LC 64/90 (LAEP); LC 135/10 (Ficha Limpa); Resoluções TSE");
case "MILITAR" -> { normas.add("CPM — Dec.-Lei 1.001/69"); normas.add("CPPM — Dec.-Lei 1.002/69"); }
case "FAMILIA_SUCESSOES" -> {
normas.add("CC/2002 art. 1.511-1.783 (Família) e art. 1.784-2.046 (Sucessões)");
if ("ALIMENTOS".equals(subRamo)) normas.add("Lei 5.478/68 (Alimentos); Lei 11.804/08 (Alimentos Gravídicos)");
if ("GUARDA_VISITAS".equals(subRamo)) normas.add("Lei 13.058/14 (Guarda compartilhada); Lei 12.318/10 (Alienação parental)");
if ("ADOCAO".equals(subRamo)) normas.add("ECA Lei 8.069/90 arts. 39-52; Lei 13.509/17");
}
case "CONSUMIDOR" -> normas.add("Lei 8.078/90 (CDC); CF art. 5º, XXXII");
case "AMBIENTAL" -> {
normas.add("Lei 6.938/81 (PNMA); Lei 9.605/98 (Crimes Ambientais); Lei 12.651/12 (Código Florestal)");
normas.add("Lei 7.347/85 (ACP ambiental); CF art. 225");
}
case "EMPRESARIAL" -> {
normas.add("CC/2002 art. 966-1.195");
if ("FALENCIA".equals(subRamo) || "RECUPERACAO_JUDICIAL".equals(subRamo)) {
normas.add("Lei 11.101/2005 (LRF)");
}
}
case "CONSTITUCIONAL" -> normas.add("CF/1988; STF — competência originária (CF art. 102)");
case "SAUDE" -> { normas.add("CF art. 196 (saúde como direito); Lei 8.080/90 (SUS)"); normas.add("Lei 9.656/98 (Planos de Saúde); RN ANS 465/2021"); }
case "INFANCIA_JUVENTUDE" -> normas.add("Lei 8.069/90 (ECA); CF art. 227; Lei 13.509/17");
case "AGRARIO" -> normas.add("Lei 4.504/64 (Estatuto da Terra); Dec.-Lei 3.365/41; LC 76/93; CF art. 184-186");
case "INTERNACIONAL" -> normas.add("LINDB (Dec.-Lei 4.657/42); CPC art. 960-965; Convenções de Haia; Tratados bilaterais");
case "IMOBILIARIO" -> normas.add("CC/2002 art. 1.196-1.510; Lei 8.245/91 (Locações); Lei 9.514/97 (SFI)");
case "PROPRIEDADE_INTELECTUAL" -> normas.add("Lei 9.279/96 (LPI); Lei 9.610/98 (Direito Autoral); Lei 9.609/98 (Software)");
case "EDUCACAO" -> normas.add("CF arts. 205-214; LDB — Lei 9.394/96; Lei 12.711/12 (Cotas)");
default -> normas.add("Legislação específica aplicável ao caso");
}
return String.join("; ", normas);
}
private double calcularConfianca(Map<String, Object> ctx, String rito, String ramo) {
int score = 0;
if (!str(ctx.get("assunto")).isBlank()) score += 20;
if (!str(ctx.get("resumo")).isBlank() || !str(ctx.get("narrativa")).isBlank()) score += 15;
if (!str(ctx.get("ramo_direito")).isBlank() || !str(ctx.get("ambito_direito")).isBlank()) score += 20;
if (!str(ctx.get("rito")).isBlank() || !str(ctx.get("rito_processual")).isBlank()) score += 20;
if (ctx.get("valor_causa") != null) score += 10;
if (!str(ctx.get("tribunal")).isBlank() || !str(ctx.get("vara")).isBlank()) score += 10;
if (!str(ctx.get("comarca")).isBlank()) score += 5;
if (rito != null && !"CIVIL".equals(ramo)) score += 5;
if (score >= 80) return CONFIDENCE_HIGH;
if (score >= 40) return CONFIDENCE_MEDIUM;
return CONFIDENCE_LOW;
}
private boolean inferirSegredo(String ramo, String rito) {
return switch (ramo) {
case "FAMILIA_SUCESSOES", "PENAL", "SAUDE", "INFANCIA_JUVENTUDE" -> true;
default -> ProceduralRitoNames.isOneOf(rito, "PENAL_MARIA_DA_PENHA");
};
}
private boolean inferirExigeMP(String ramo, String subRamo, String rito) {
return switch (ramo) {
case "PENAL", "ELEITORAL", "INFANCIA_JUVENTUDE", "AMBIENTAL", "ADMINISTRATIVO" -> true;
case "FAMILIA_SUCESSOES" -> true;
default -> false;
};
}
private boolean inferirConciliacao(String ramo, String rito) {
return switch (ramo) {
case "CIVIL", "CONSUMIDOR", "TRABALHISTA", "IMOBILIARIO", "EDUCACAO", "SAUDE",
"FAMILIA_SUCESSOES", "EMPRESARIAL" -> true;
case "PENAL", "TRIBUTARIO" -> false;
default -> true;
};
}
private List<String> camposObrigatoriosPara(String rito, String ramo) {
List<String> base = new ArrayList<>(List.of(
"autor_nome", "autor_cpf_cnpj", "reu_nome",
"advogado_nome", "advogado_oab", "doc_procuracao",
"valor_causa", "pedido_principal", "fundamento_juridico",
"comarca", "vara_ou_unidade", "tribunal"
));
if (rito == null) return base;
switch (ramo) {
case "PREVIDENCIARIO" -> base.addAll(List.of("nb_beneficio", "cpf_segurado", "cnis", "carencia", "der_data"));
case "TRIBUTARIO" -> base.addAll(List.of("cda_numero", "tributo", "periodo_competencia", "valor_debito"));
case "ELEITORAL" -> base.addAll(List.of("pleito_ano", "cargo_pretendido", "municipio_pleito", "numero_candidato"));
case "MILITAR" -> base.addAll(List.of("organizacao_militar", "posto_graduacao_acusado", "fato_data", "bo_militar"));
case "TRABALHISTA" -> base.addAll(List.of("ctps", "data_admissao", "data_demissao", "ultimo_salario", "motivo_rescisao", "nit_pis"));
case "PENAL" -> base.addAll(List.of("fato_descricao", "data_fato", "local_fato", "tipificacao_penal", "bo_numero"));
case "FAMILIA_SUCESSOES" -> base.addAll(List.of("certidao_casamento_nascimento", "cpf_partes", "partilha_bens"));
case "AMBIENTAL" -> base.addAll(List.of("area_descricao", "coordenadas_geo", "laudo_tecnico", "orgao_ambiental_notificado"));
case "INFANCIA_JUVENTUDE" -> base.addAll(List.of("nome_menor", "cpf_responsavel", "certidao_nascimento_menor", "relatorio_conselho_tutelar"));
case "AGRARIO" -> base.addAll(List.of("itr_documento", "matricula_imovel", "ccir", "car_numero"));
default -> {}
}
return Collections.unmodifiableList(base);
}
private List<String> alertasPara(String rito, String ramo, Map<String, Object> ctx, String texto) {
List<String> alertas = new ArrayList<>();
if (rito == null) {
alertas.add("Rito processual não identificado — revise o ramo do direito e tipo de ação antes do protocolo.");
}
switch (ramo) {
case "PENAL" -> {
alertas.add("Área penal: verifique prescrição (CP arts. 109-117), decadência e condição de procedibilidade.");
if (contains(texto, "habeas corpus")) alertas.add("HC: prazo é urgente — verifique situação de flagrante ou preventiva.");
}
case "ELEITORAL" -> alertas.add("Prazo eleitoral contado em dias corridos — verificar calendário TSE.");
case "TRABALHISTA" -> alertas.add("Prescrição trabalhista: 2 anos pós-rescisão (CF art. 7º, XXIX); créditos: 5 anos retroativos.");
case "TRIBUTARIO" -> alertas.add("Verificar prescrição e decadência tributária (CTN arts. 173 e 174) antes de qualquer protocolo.");
case "PREVIDENCIARIO" -> alertas.add("Checar DER (Data do Requerimento Administrativo) no INSS — condiciona retroativos e competência do JEF.");
case "AMBIENTAL" -> alertas.add("Dano ambiental: imprescritível (STJ REsp 1.120.117). ACP obrigatória se dano difuso.");
case "INFANCIA_JUVENTUDE" -> alertas.add("Prioridade absoluta ECA (CF art. 227). Segredo de justiça automático. Prazo: urgência imediata.");
case "AGRARIO" -> alertas.add("Ação agrária: verificar fase de mediação fundiária obrigatória (Lei 13.465/17) antes do ajuizamento.");
default -> {}
}
if (ProceduralRitoNames.isOneOf(rito, "CIVIL_TUTELA_URGENTE", "CIVIL_TUTELA_CAUTELAR_ANTECEDENTE", "CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE")) {
alertas.add("Tutela de urgência: exige probabilidade do direito + perigo de dano ou risco ao resultado útil (CPC art. 300). Fundamente detalhadamente.");
}
if (ProceduralRitoNames.isOneOf(rito, "IMPROBIDADE_ADMINISTRATIVA")) {
alertas.add("LIA redação 2021 (Lei 14.230): exige dolo específico — culpa não é suficiente. Demonstrar elemento subjetivo.");
}
Double valor = toDouble(ctx.get("valor_causa"));
if (valor == null || valor <= 0.0) {
alertas.add("CRÍTICO: Valor da causa ausente ou zero — obrigatório por CPC art. 292. Sistema não pode calcular custas nem rito automaticamente.");
}
return Collections.unmodifiableList(alertas);
}
private List<String> documentosEssenciaisPara(String rito, String ramo, String texto) {
List<String> docs = new ArrayList<>(List.of(
"Procuração ad judicia (CPC art. 105)",
"Documentos de identificação do(s) autor(es) — CPF/CNPJ, RG",
"Comprovante de residência",
"Documentos probatórios do fato narrado"
));
if (rito == null) return docs;
switch (ramo) {
case "PENAL" -> docs.addAll(List.of("Boletim de ocorrência", "Laudos periciais", "Prontuário/ATJLD se aplicável", "Representação da vítima se necessário"));
case "TRABALHISTA" -> docs.addAll(List.of("CTPS (física ou digital)", "Contrato de trabalho", "Holerites (6+ meses)", "Termo de rescisão (TRCT)", "Extrato FGTS", "PPP se doença ocupacional", "CAGED"));
case "PREVIDENCIARIO" -> docs.addAll(List.of("Extrato CNIS completo", "Carta de indeferimento/concessão INSS", "Laudos médicos/perícias", "Comprovação atividade especial se aplicável"));
case "TRIBUTARIO" -> docs.addAll(List.of("CDA ou Auto de Infração", "Nota fiscal/documentos contábeis", "DARF/DARE de recolhimento", "Impugnação administrativa (se houver)", "Certidão de dívida ativa"));
case "ELEITORAL" -> docs.addAll(List.of("Certidão de quitação eleitoral", "Documentos do partido", "Mídia probatória da propaganda irregular", "Prestação de contas anterior"));
case "FAMILIA_SUCESSOES" -> docs.addAll(List.of("Certidão de casamento/nascimento", "Documentos de bens (matrículas, DUTs)", "Acordo extrajudicial (se houver)", "Laudo de avaliação de bens"));
case "AMBIENTAL" -> docs.addAll(List.of("Licença Ambiental", "Laudo do IBAMA/IMA/Órgão Estadual", "ART do responsável técnico", "Geo-referenciamento da área"));
case "INFANCIA_JUVENTUDE" -> docs.addAll(List.of("Certidão de nascimento do menor", "Relatório do Conselho Tutelar", "Laudo psicossocial", "Estudo social do CRAS/CREAS"));
case "AGRARIO" -> docs.addAll(List.of("Matrícula do imóvel rural", "CCIR (INCRA)", "CAR — Cadastro Ambiental Rural", "ITR últimos 5 anos", "Georeferenciamento"));
default -> {}
}
return Collections.unmodifiableList(docs);
}
private List<String> proximosPassosPara(String rito, String ramo, String esfera) {
List<String> passos = new ArrayList<>(List.of(
"1. Confirmar competência: esfera (" + esfera + "), rito (" + (rito != null ? rito : "INDEFINIDO") + ") e vara especializada.",
"2. Verificar condições de admissibilidade: legitimidade ativa/passiva, interesse de agir e possibilidade jurídica.",
"3. Mapear prescrição/decadência e calcular prazo com dias úteis/corridos conforme rito.",
"4. Organizar documentação probatória: documentos, testemunhas, peritos.",
"5. Calcular valor da causa com metodologia correta (CPC art. 292).",
"6. Redigir petição inicial: qualificação → fatos → direito → pedidos → valor.",
"7. Avaliar pedido de tutela provisória (urgência ou evidência) se cabível.",
"8. Verificar necessidade de recolhimento de custas e se há isenção/gratuidade aplicável.",
"9. Protocolar no sistema correto: " + sistemaProtocolo(rito, esfera),
"10. Aguardar distribuição, monitorar intimações e cadastrar prazos."
));
return Collections.unmodifiableList(passos);
}
private String sistemaProtocolo(String rito, String esfera) {
if ("FEDERAL".equals(esfera)) return "PJe (JF) ou e-Proc TRF";
if (rito == null) return "VERIFICAR_MANUALMENTE";
if (rito.startsWith("TRABALHISTA")) return "PJe TRT";
if (rito.startsWith("ELEITORAL")) return "PJe TSE/TRE";
if (rito.startsWith("MILITAR")) return "Sistema Justiça Militar";
return "PJe Estadual / ESAJ / e-SAJ / e-Proc (conforme estado)";
}

public Map<String, Object> inferIntent(Map<String, Object> ctx) {
AjuizamentoIntent intent = inferir(ctx);
Map<String, Object> out = new LinkedHashMap<>();
out.put("rito", intent.rito());
out.put("ramoDireito", intent.ramoDireito());
out.put("ramoProjeto", BrazilianLegalKnowledgeBase.toProjetoRamo(intent.ramoDireito(), intent.subRamo(), intent.esfera()).name());
out.put("subRamo", intent.subRamo());
out.put("esfera", intent.esfera());
out.put("competencia", intent.competencia());
out.put("tipoAcao", intent.tipoAcao());
out.put("fundamento", intent.fundamento());
out.put("confianca", intent.confianca());
out.put("camposObrigatorios", intent.camposObrigatorios());
out.put("alertas", intent.alertas());
out.put("documentosEssenciais", intent.documentosEssenciais());
out.put("proximosPassos", intent.proximosPassos());
out.put("segredoJustica", intent.segredoJustica());
out.put("exigeMP", intent.exigeMP());
out.put("admiteConciliacao", intent.admiteConciliacao());
return Collections.unmodifiableMap(out);
}

private String refineCompetencia(String competencia, String ramo, String subRamo, String esfera, Map<String, Object> ctx) {
List<String> hints = BrazilianLegalKnowledgeBase.competenceHints(ramo, esfera, subRamo, toDouble(ctx.get("valor_causa")));
if (hints.isEmpty()) return competencia;
if (competencia == null || competencia.isBlank()) return hints.get(0);
return competencia + " | " + hints.get(0);
}

private String refineFundamento(String fundamento, String ramo, String subRamo, String rito) {
RamoDescriptor descriptor = BrazilianLegalKnowledgeBase.resolve(ramo);
StringBuilder sb = new StringBuilder();
if (fundamento != null && !fundamento.isBlank()) sb.append(fundamento);
if (descriptor != null) {
if (sb.length() > 0) sb.append(" | ");
sb.append(descriptor.regraPrincipal());
}
if (rito != null) {
if (sb.length() > 0) sb.append(" | ");
sb.append("Rito ").append(rito);
}
return sb.toString();
}

private double recalibrarConfianca(double base, Map<String, Object> ctx, String texto, String ramo, String subRamo, String rito) {
double confidence = Math.max(CONFIDENCE_LOW, base);
if (rito != null) confidence += 0.05;
if (!"GERAL".equals(subRamo) && !subRamo.endsWith("_GERAL")) confidence += 0.04;
if (ctx.containsKey("classe") || ctx.containsKey("classe_cnj")) confidence += 0.03;
if (ctx.containsKey("assunto") || ctx.containsKey("materia")) confidence += 0.03;
List<RamoDescriptor> matches = BrazilianLegalKnowledgeBase.search(texto);
if (!matches.isEmpty() && matches.get(0).codigo().equals(ramo)) confidence += 0.05;
if (matches.size() > 1 && matches.get(0).codigo().equals(ramo) && matches.get(1).codigo().equals(ramo)) confidence += 0.02;
return Math.min(0.99, Math.round(confidence * 100.0) / 100.0);
}

private List<String> derivedCamposObrigatorios(Map<String, Object> ctx, String ramo, String subRamo, String rito) {
LinkedHashSet<String> campos = new LinkedHashSet<>();
campos.add("descricaoCaso");
campos.add("assunto");
campos.add("pedido");
if (!hasAny(ctx, "classe", "classe_cnj", "classe_tpu")) campos.add("classeTPU");
if (!hasAny(ctx, "valor_causa", "valorCausa")) campos.add("valorCausa");
if (BrazilianLegalKnowledgeBase.toProjetoRamo(ramo, subRamo, null) == RamoDireito.FAMILIA) campos.add("qualificacaoMenoresOuDependentes");
if (rito != null && ProceduralRitoNames.isPrevidenciario(rito)) campos.add("protocoloAdministrativoOuDER");
if (rito != null && ProceduralRitoNames.isTrabalhista(rito)) campos.add("vinculoEmpregaticio");
if (rito != null && ProceduralRitoNames.isTribFazenda(rito)) campos.add("entePublicoEnvolvido");
return new ArrayList<>(campos);
}

private List<String> derivedAlertas(Map<String, Object> ctx, String ramo, String subRamo, String rito, String esfera, double confianca) {
LinkedHashSet<String> alertas = new LinkedHashSet<>();
if (confianca < 0.60) alertas.add("Confiança heurística reduzida. Revise assunto, classe TPU, pedidos e competência.");
if (rito == null) alertas.add("Rito não determinado com segurança. Revise o pedido principal e a classe processual.");
if (!hasAny(ctx, "tribunal", "comarca", "vara")) alertas.add("Competência territorial incompleta. Informe tribunal, comarca ou vara para roteamento mais preciso.");
if ("FAMILIA_SUCESSOES".equals(ramo)) alertas.add("Família e sucessões exigem atenção ao segredo de justiça e eventual atuação obrigatória do MP.");
if ("SAUDE".equals(ramo) && !"FEDERAL".equals(esfera)) alertas.add("Direito à saúde pode deslocar competência entre fazenda pública e vara cível conforme polo passivo.");
if ("PROPRIEDADE_INTELECTUAL".equals(ramo)) alertas.add("Propriedade intelectual costuma exigir competência federal quando houver ato do INPI.");
if (rito != null && ProceduralRitoNames.requiresSegredoByDefault(rito)) alertas.add("O rito identificado sugere tratamento reforçado de sigilo e controle de acesso.");
return new ArrayList<>(alertas);
}

private List<String> derivedDocumentos(String ramo, String subRamo, Map<String, Object> ctx) {
LinkedHashSet<String> docs = new LinkedHashSet<>();
RamoDescriptor descriptor = BrazilianLegalKnowledgeBase.resolve(ramo);
descriptor.classesComuns().stream().limit(3).forEach(docs::add);
if ("SAUDE".equals(ramo)) {
docs.add("Receita, laudo e negativa administrativa ou contratual");
docs.add("Comprovante de urgência clínica");
}
if ("EDUCACAO".equals(ramo)) {
docs.add("Edital, histórico escolar e ato denegatório");
}
if ("IMOBILIARIO".equals(ramo)) {
docs.add("Matrícula, contrato e notificação extrajudicial");
}
if ("PROPRIEDADE_INTELECTUAL".equals(ramo)) {
docs.add("Certidão do INPI, prova de anterioridade e elementos de confusão");
}
return new ArrayList<>(docs);
}

private List<String> derivedPassos(String ramo, String subRamo, String rito, CurriculumSnapshot curriculum, Map<String, Object> ctx) {
LinkedHashSet<String> passos = new LinkedHashSet<>();
passos.add("11. Validar aderência do caso ao ramo " + curriculum.ramoCodigo() + " e revisar a trilha prioritária indicada.");
curriculum.materiasPrioritarias().stream().limit(3).forEach(m -> passos.add("Trilha prioritária: " + m));
curriculum.legislacaoChave().stream().limit(2).forEach(l -> passos.add("Revisar base legal: " + l));
curriculum.prazosCriticos().stream().limit(2).forEach(p -> passos.add("Prazo crítico: " + p));
if (rito != null) passos.add("Sistema recomendado: " + ProceduralRitoNames.suggestedProtocolSystem(rito, str(ctx.get("esfera"))));
if ("INTERNACIONAL".equals(ramo)) passos.add("Verificar cooperação internacional, carta rogatória ou homologação no STJ conforme o pedido.");
return new ArrayList<>(passos);
}

@SafeVarargs
private static List<String> mergeDistinct(List<String>... values) {
LinkedHashSet<String> out = new LinkedHashSet<>();
for (List<String> list : values) {
if (list == null) continue;
for (String item : list) {
if (item == null) continue;
String v = item.trim();
if (!v.isBlank()) out.add(v);
}
}
return new ArrayList<>(out);
}

private static boolean hasAny(Map<String, Object> ctx, String... keys) {
for (String key : keys) {
Object value = ctx.get(key);
if (value != null && !String.valueOf(value).isBlank()) return true;
}
return false;
}

private static Map<String, Object> sanitize(Map<String, Object> ctx) {
Map<String, Object> safe = new LinkedHashMap<>(ctx.size());
for (Map.Entry<String, Object> e : ctx.entrySet()) {
if (e.getKey() != null) safe.put(String.valueOf(e.getKey()), e.getValue());
}
return safe;
}
private static String normalizeEnumKey(String s) {
if (s == null || s.isBlank()) return "";
return s.trim().toUpperCase(Locale.ROOT)
.replace('Ç','C').replace('Ã','A').replace('Á','A').replace('Â','A')
.replace('É','E').replace('Ê','E').replace('Í','I').replace('Ó','O')
.replace('Ô','O').replace('Õ','O').replace('Ú','U').replace('Ü','U')
.replaceAll("[^A-Z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
}
private static String normalizeTexto(String s) {
if (s == null) return "";
return s.toLowerCase(Locale.ROOT)
.replace('ç','c').replace('ã','a').replace('á','a').replace('â','a')
.replace('é','e').replace('ê','e').replace('í','i').replace('ó','o')
.replace('ô','o').replace('õ','o').replace('ú','u').replace('ü','u')
.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
}
private static boolean contains(String texto, String... tokens) {
if (texto == null) return false;
for (String t : tokens) { if (texto.contains(t)) return true; }
return false;
}
private static String str(Object v) {
return v == null ? "" : String.valueOf(v).trim();
}
private static boolean bool(Object v) {
if (v == null) return false;
if (v instanceof Boolean b) return b;
String s = str(v).toLowerCase(Locale.ROOT);
return "true".equals(s) || "1".equals(s) || "sim".equals(s) || "yes".equals(s);
}
private static Double toDouble(Object v) {
if (v == null) return null;
if (v instanceof Number n) return n.doubleValue();
try {
String s = str(v).replace(",", ".").replace("R$", "").replace(" ", "");
return s.isBlank() ? null : Double.parseDouble(s);
} catch (Exception e) {
return null;
}
}
}
