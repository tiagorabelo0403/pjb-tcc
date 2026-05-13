package com.tcc.pjb.backend.ai.academy;

import com.tcc.pjb.backend.ai.juridica.v3.core.BrazilianLegalKnowledgeBase;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class CurriculumKnowledgeService {
public CurriculumModule getModule(String ramo) {
return CURRICULUM.getOrDefault(resolveCurriculumKey(ramo), CURRICULUM.get("CIVIL"));
}
public CurriculumModule getModule(RamoDireito ramo) {
return getModule(BrazilianLegalKnowledgeBase.fromProjetoRamo(ramo));
}
public Set<String> getRamosDisponiveis() {
return Collections.unmodifiableSet(CURRICULUM.keySet());
}
public List<CurriculumProgram> programs() {
return PROGRAMS;
}
public CurriculumProgram program(String id) {
if (PROGRAMS.isEmpty()) {
return null;
}
if (id == null || id.isBlank()) {
return PROGRAMS.getFirst();
}
String key = id.trim();
for (CurriculumProgram program : PROGRAMS) {
if (program.getId() != null && program.getId().equalsIgnoreCase(key)) {
return program;
}
}
return PROGRAMS.getFirst();
}
public List<CurriculumModule> search(String termo, int limit) {
String q = termo == null ? "" : termo.toLowerCase(Locale.ROOT).trim();
if (q.isBlank()) return List.of(getModule("CIVIL"));
List<CurriculumModule> ranked = new ArrayList<>(CURRICULUM.values());
ranked.sort((a, b) -> Integer.compare(score(q, b), score(q, a)));
List<CurriculumModule> out = new ArrayList<>();
for (CurriculumModule m : ranked) {
if (score(q, m) <= 0) continue;
out.add(m);
if (out.size() >= Math.max(1, limit)) break;
}
if (out.isEmpty()) out.add(getModule("CIVIL"));
return Collections.unmodifiableList(out);
}
public CurriculumSnapshot snapshot(String ramo, String subRamo, String ritoName) {
return snapshot(ramo, subRamo, com.tcc.pjb.backend.core.procedural.ProceduralRitoNames.parse(ritoName));
}
public CurriculumSnapshot snapshot(String ramo, String subRamo, RitoProcessual rito) {
CurriculumModule module = getModule(ramo);
String knowledgeKey = BrazilianLegalKnowledgeBase.resolveRamoCodigo(ramo);
List<String> ritos = new ArrayList<>();
if (rito != null) {
ritos.add(rito.name());
} else {
BrazilianLegalKnowledgeBase.relatedRitos(knowledgeKey).stream().limit(6).forEach(ritos::add);
}
LinkedHashSet<String> materias = new LinkedHashSet<>();
module.materias().stream().limit(6).forEach(materias::add);
if (subRamo != null && !subRamo.isBlank()) {
String needle = subRamo.toLowerCase(Locale.ROOT).replace('_', ' ');
module.materias().stream()
        .filter(m -> m.toLowerCase(Locale.ROOT).contains(needle))
        .limit(3)
        .forEach(materias::add);
}
List<String> prazos = BrazilianLegalKnowledgeBase.resolve(knowledgeKey).prazosPrincipais().stream().limit(4).toList();
return new CurriculumSnapshot(
        knowledgeKey,
        module.nome(),
        List.copyOf(materias),
        module.legislacaoPrincipal().stream().limit(5).toList(),
        module.principiosGerais().stream().limit(4).toList(),
        prazos,
        List.copyOf(ritos)
);
}
public Map<String, Object> describe(String ramo, String subRamo, String ritoName) {
return describe(ramo, subRamo, com.tcc.pjb.backend.core.procedural.ProceduralRitoNames.parse(ritoName));
}
public Map<String, Object> describe(String ramo, String subRamo, RitoProcessual rito) {
CurriculumSnapshot snapshot = snapshot(ramo, subRamo, rito);
Map<String, Object> out = new LinkedHashMap<>();
out.put("ramoCodigo", snapshot.ramoCodigo());
out.put("nome", snapshot.nome());
out.put("materiasPrioritarias", snapshot.materiasPrioritarias());
out.put("legislacaoChave", snapshot.legislacaoChave());
out.put("principiosChave", snapshot.principiosChave());
out.put("prazosCriticos", snapshot.prazosCriticos());
out.put("ritosRelacionados", snapshot.ritosRelacionados());
return Collections.unmodifiableMap(out);
}
private static final List<CurriculumProgram> PROGRAMS = List.of();
private static List<CurriculumProgram> loadPrograms() {
return List.of();
}
private static final Map<String, CurriculumModule> CURRICULUM = Map.ofEntries(
Map.entry("CIVIL", new CurriculumModule(
"CIVIL", "Direito Civil e Processual Civil",
List.of("Teoria geral do negócio jurídico", "Obrigações", "Contratos em espécie",
"Responsabilidade civil (contratual e extracontratual)", "Posse e propriedade",
"Direitos reais de gozo (usufruto, uso, habitação, servidão)",
"Direitos reais de garantia (penhor, hipoteca, anticrese)",
"Direito de família (regime de bens, alimentos, guarda)",
"Direito das sucessões (inventário, partilha, testamento)",
"Teoria geral do processo", "Pressupostos processuais",
"Tutelas provisórias de urgência e evidência (CPC 294-311)",
"Execução civil e cumprimento de sentença",
"Procedimentos especiais (CPC 539-770)",
"Recursos cíveis (apelação, agravo, embargos, REsp, RE)",
"Ação popular e ação civil pública"),
List.of("CC — Lei 10.406/2002", "CPC — Lei 13.105/2015",
"LINDB — Dec.-Lei 4.657/42", "Lei 8.009/90 (bem de família)",
"Lei 7.347/85 (ação civil pública)", "Lei 9.307/96 (arbitragem)",
"Lei 13.140/15 (mediação)"),
List.of("Autonomia privada e função social do contrato (CC art. 421)",
"Boa-fé objetiva (CC art. 422)", "Vedação ao enriquecimento sem causa (CC art. 884)",
"Responsabilidade objetiva por atividade de risco (CC art. 927 parágrafo único)",
"Contraditório e ampla defesa (CF art. 5º, LV)",
"Razoável duração do processo (CF art. 5º, LXXVIII)",
"Cooperação processual (CPC art. 6º)"),
List.of("STJ Súmula 37 (cumulação dano moral e material)",
"STJ Súmula 54 (juros moratórios — illícito extracontratual)",
"STJ Súmula 362 (juros remuneratórios — contrato bancário)",
"STJ Súmula 412 (ação de cobrança — prazo prescrição)",
"STJ Súmula 477 (independência das instâncias — dano moral)"),
List.of("RE 631.240 (responsabilidade civil solidária Estado/particular)",
"RE 586.224 (competência — complementação aposentadoria)",
"RE 852.475 (imprescritibilidade — pretensão reparatória contra Estado)"),
List.of("Competência (CPC 42-66)", "Litisconsórcio e intervenção de terceiros",
"Petição inicial e emenda (CPC 319-331)", "Contestação e reconvenção",
"Audiências (conciliação e instrução — CPC 334-368)",
"Sentença e coisa julgada (CPC 485-508)", "Embargos de declaração",
"Liquidação de sentença", "Cumprimento provisório e definitivo",
"Penhora, avaliação e expropriação", "Tutela de evidência (CPC 311)"),
List.of("Abuso de direito (CC art. 187)", "Dano moral in re ipsa",
"Solidariedade passiva (CC art. 942)", "Danos extrapatrimoniais (CC art. 944)",
"Prescrição e decadência (CC arts. 189-211)", "Vícios do negócio jurídico")
)),
Map.entry("PENAL", new CurriculumModule(
"PENAL", "Direito Penal e Processual Penal",
List.of("Teoria do crime (tipicidade, ilicitude, culpabilidade)",
"Teoria da imputação objetiva", "Tentativa, consumação e desistência",
"Concurso de pessoas (autoria e participação)",
"Concurso de crimes (material, formal, crime continuado)",
"Extinção da punibilidade (prescrição, anistia, graça, indulto, perdão)",
"Crimes em espécie — Parte Especial CP",
"Crimes hediondos (Lei 8.072/90)", "Lei de drogas (Lei 11.343/2006)",
"Lei Maria da Penha (Lei 11.340/2006)", "ECA penal (Lei 8.069/90)",
"Crimes de trânsito (Lei 9.503/97)", "Crimes contra a ordem tributária",
"Lavagem de dinheiro (Lei 9.613/98)", "Organização criminosa (Lei 12.850/2013)",
"Crimes cibernéticos (Lei 12.737/2012; Lei 14.155/2021)",
"Inquérito policial e investigação criminal",
"Ação penal pública e privada", "Prisões cautelares e liberdade provisória",
"Nulidades processuais penais", "Tribunal do Júri (CPP art. 406-497)",
"Execução penal (LEP — Lei 7.210/84)", "Habeas corpus e revisão criminal"),
List.of("CP — Dec.-Lei 2.848/40", "CPP — Dec.-Lei 3.689/41",
"LEP — Lei 7.210/84", "CF art. 5º (garantias penais)",
"Lei 8.072/90 (crimes hediondos)", "Lei 11.343/2006 (drogas)",
"Lei 11.340/2006 (Maria da Penha)", "Lei 12.850/2013 (organizações criminosas)",
"Lei 9.613/98 (lavagem)", "Lei 14.155/2021 (crimes cibernéticos)"),
List.of("Princípio da legalidade — nullum crimen sine lege (CF art. 5º, XXXIX)",
"Princípio da anterioridade (CP art. 1º)", "Princípio da lesividade",
"Princípio da culpabilidade (vedação da responsabilidade objetiva penal)",
"Princípio da individualização da pena (CF art. 5º, XLVI)",
"Princípio da presunção de inocência (CF art. 5º, LVII)",
"Princípio do in dubio pro reo", "Vedação da dupla incriminação (ne bis in idem)"),
List.of("STF Súmula Vinculante 11 (uso de algemas)",
"STF Súmula Vinculante 14 (acesso a inquérito)",
"STJ Súmula 444 (vedação agravantes genéricas sem fundamentação)",
"STJ Súmula 231 (reincidência — quantum)",
"STJ Súmula 630 (acordo de não persecução penal — hipóteses)"),
List.of("ADC 43/44 (execução antecipada da pena — inconstitucionalidade)",
"RE 600.817 (tráfico privilegiado — não hediondo)",
"HC 143.988 (internação ECA — medida excepcional)"),
List.of("Inquérito policial — prazo (CPP 10)", "Denúncia ou queixa (CPP 41-46)",
"Resposta à acusação (CPP 396-A)", "Instrução criminal — audiências",
"Alegações finais orais e memoriais", "Pronúncia e desaforamentos (Júri)",
"Recursos penais (RESE, apelação, embargos infringentes)",
"Habeas corpus (CPP 647-667)", "Revisão criminal (CPP 621-631)",
"Execução penal — progressão de regime, saídas temporárias"),
List.of("Dosimetria da pena (CP 59; método trifásico)", "Penas alternativas (CP 43-48)",
"Sursis processual (Lei 9.099/95 art. 89)", "Acordo de não persecução penal",
"Colaboração premiada (Lei 12.850/2013 art. 4º)")
)),
Map.entry("TRABALHISTA", new CurriculumModule(
"TRABALHISTA", "Direito do Trabalho e Processual do Trabalho",
List.of("Contrato de trabalho: formação, espécies e extinção",
"Jornada de trabalho e horas extras", "Remuneração e salário",
"FGTS e verbas rescisórias", "Estabilidade no emprego",
"Responsabilidade civil e danos morais laborais",
"Acidente de trabalho e doença ocupacional",
"Direito coletivo: greve, sindicato, negociação coletiva",
"Terceirização (Lei 13.429/2017; STF ADC 48)",
"Teletrabalho e home office (CLT art. 75-A a 75-E)",
"Reforma trabalhista (Lei 13.467/2017) — alterações relevantes",
"Processo trabalhista: reclamação, dissídio coletivo, ação rescisória",
"Tutela de urgência no processo trabalhista",
"Execução trabalhista e penhora de bens"),
List.of("CLT — Dec.-Lei 5.452/43", "CF art. 7º-11",
"Lei 13.467/17 (Reforma Trabalhista)", "Lei 9.029/95 (atos discriminatórios)",
"Lei 13.467/17 art. 507-A (cláusula compromissória de arbitragem)"),
List.of("Princípio da proteção (in dubio pro operario, norma mais favorável, condição mais benéfica)",
"Princípio da primazia da realidade (contrato-realidade)",
"Princípio da irrenunciabilidade dos direitos trabalhistas",
"Princípio da continuidade da relação de emprego"),
List.of("TST Súmula 331 (terceirização — responsabilidade subsidiária)",
"TST Súmula 85 (compensação de jornada — requisitos)",
"TST Súmula 428 (sobreaviso — uso contínuo de celular)",
"TST Súmula 443 (dispensa discriminatória — presunção)",
"TST Súmula 291 (horas extras habituais — rescisão indireta)"),
List.of("STF ADC 48 (terceirização lícita — ampla)", "STF ADPF 324 (pejotização)"),
List.of("Audiência una ou bifurcada", "Instrução e depoimento das partes",
"Testemunhas (CLT 820-828)", "Sentença e liquidação trabalhista",
"Recurso ordinário (8 dias — CLT 895)", "Recurso de revista",
"Agravo de instrumento", "Embargos no TST",
"Execução trabalhista — penhora, hasta pública"),
List.of("Vínculo empregatício — requisitos (CLT art. 3º)",
"Grupo econômico trabalhista (CLT art. 2º §2º)",
"Sucessão trabalhista (CLT art. 10 e 448)",
"Poder diretivo do empregador x direitos fundamentais do empregado")
)),
Map.entry("PREVIDENCIARIO", new CurriculumModule(
"PREVIDENCIARIO", "Direito Previdenciário",
List.of("Seguridade social: saúde, previdência e assistência",
"Segurados e dependentes do RGPS", "Carência e período de graça",
"Aposentadorias pós-EC 103/2019 (por pontos progressivos)",
"Aposentadoria especial — atividade de risco e insalubridade",
"Aposentadoria do trabalhador rural — segurado especial",
"Auxílio por incapacidade temporária (ex-auxílio-doença)",
"Aposentadoria por incapacidade permanente (ex-invalidez)",
"BPC/LOAS — benefício de prestação continuada",
"Pensão por morte e auxílio-reclusão",
"Salário-maternidade", "Revisão do benefício — tese da vida toda",
"Tempo especial — conversão e PPP",
"RPPS — regime próprio de servidor público",
"Previdência complementar (EFPC)",
"Processo previdenciário no JEF e na Vara Federal"),
List.of("Lei 8.213/91 (Benefícios do RGPS)", "Lei 8.212/91 (Custeio)",
"EC 103/2019 (Reforma Previdenciária)", "Lei 8.742/93 (LOAS)",
"Decreto 3.048/99 (Regulamento Previdência)",
"Lei 9.099/95 + Lei 10.259/2001 (JEF)"),
List.of("Universalidade da cobertura", "Uniformidade dos benefícios",
"Seletividade e distributividade na prestação",
"Irredutibilidade do valor dos benefícios",
"Precedência de custeio (CF art. 195 §5º)"),
List.of("TNU Súmula 83 (trabalho rural — início de prova material)",
"TNU Súmula 54 (atividade especial — PPP — laudo por profissional)",
"STJ Súmula 568 (competência JEF — até 60 SM)"),
List.of("STF RE 661.256 (desaposentação — inconstitucional)",
"STJ Tema 692 (revisão vida toda — constitucional)",
"STF ARE 664.335 (aposentadoria especial — exposição habitual)"),
List.of("Processo no JEF — Lei 10.259/01", "DER e DIB — relevância jurídica",
"Recurso inominado JEF (10 dias)", "Pedido de uniformização TNU",
"Execução no JEF — RPV e precatório"),
List.of("Cálculo do salário-de-benefício", "PBC — período básico de cálculo",
"Teto do RGPS e benefícios acima do teto", "CNIS — central de informações",
"DER — data do requerimento administrativo como marco de retroativos")
)),
Map.entry("TRIBUTARIO", new CurriculumModule(
"TRIBUTARIO", "Direito Tributário",
List.of("Competência tributária e limitações ao poder de tributar",
"Imunidades tributárias (CF art. 150, VI)", "Espécies tributárias",
"Fato gerador, base de cálculo, alíquota",
"Obrigação tributária principal e acessória",
"Sujeito ativo e passivo; solidariedade; responsabilidade tributária",
"Crédito tributário: lançamento, suspensão, extinção, exclusão",
"Execução fiscal (LEF — Lei 6.830/80)",
"Embargos à execução fiscal", "Exceção de pré-executividade",
"Repetição de indébito e compensação tributária",
"Ação anulatória de débito fiscal",
"Mandado de segurança tributário",
"Processo administrativo fiscal (Dec. 70.235/72)",
"CARF e CSRF — julgamento administrativo",
"Parcelamentos e transação tributária (Lei 13.988/20)"),
List.of("CTN — Lei 5.172/66", "CF arts. 145-162",
"Lei 6.830/80 (LEF)", "Dec. 70.235/72 (PAF)",
"LC 116/2003 (ISS)", "LC 87/96 (ICMS)",
"Lei 13.988/2020 (Transação tributária)"),
List.of("Estrita legalidade tributária (CF art. 150, I)",
"Anterioridade (CF art. 150, III, b)", "Noventena/anterioridade nonagesimal",
"Vedação ao confisco (CF art. 150, IV)",
"Isonomia tributária (CF art. 150, II)",
"Capacidade contributiva (CF art. 145 §1º)"),
List.of("STF RE 574.706 (ICMS fora da base PIS/COFINS — Tema 69)",
"STJ Súmula 436 (confissão de dívida — dispensa lançamento)",
"STJ Súmula 555 (prazo decadencial — tributo sujeito a homologação)",
"STJ Súmula 621 (redirecionamento fiscal — dissolução irregular)"),
List.of("Tema 69 STF (ICMS na base PIS/COFINS)",
"Tema 379 STJ (prescrição intercorrente execução fiscal)",
"Tema 971 STJ (SELIC no indébito tributário)"),
List.of("Execução fiscal — citação, penhora, substituição", "Embargos — prazo 30 dias",
"Exceção de pré-executividade — vícios objetivos", "CDA — título executivo",
"Redirecionamento ao sócio-gerente (CTN art. 135)",
"Prescrição intercorrente (Lei 11.051/04)"),
List.of("ICMS — não-cumulatividade; substituição tributária",
"PIS/COFINS — regime cumulativo x não-cumulativo",
"IRPJ — lucro real, presumido e arbitrado",
"Simples Nacional — LC 123/2006")
)),
Map.entry("ADMINISTRATIVO", new CurriculumModule(
"ADMINISTRATIVO", "Direito Administrativo",
List.of("Princípios da Administração Pública (CF art. 37 — LIMPE)",
"Ato administrativo: requisitos, validade e nulidade",
"Poderes administrativos: vinculado, discricionário, regulamentar, hierárquico, disciplinar, de polícia",
"Licitações e contratos administrativos (Lei 14.133/2021)",
"Servidores públicos — regime estatutário (Lei 8.112/90)",
"Processo administrativo disciplinar (PAD)",
"Responsabilidade civil do Estado (CF art. 37 §6º — teoria do risco administrativo)",
"Improbidade administrativa (Lei 8.429/92 — dolo específico após Lei 14.230/2021)",
"Controle da administração: interno, externo (TCU/TCE), judicial",
"Mandado de segurança (Lei 12.016/2009)",
"Ação popular (Lei 4.717/65)",
"Concessões, permissões e autorizações de serviços públicos",
"Desapropriação (Dec.-Lei 3.365/41)",
"Parcerias público-privadas (Lei 11.079/2004)",
"Bens públicos — classificação, afetação, alienação"),
List.of("CF art. 37-43", "Lei 8.112/90 (EPEP — servidores federais)",
"Lei 14.133/2021 (NLLC)", "Lei 9.784/99 (PAF Federal)",
"Lei 8.429/92 (LIA) com alterações da Lei 14.230/2021",
"Lei 12.016/09 (MS)", "Lei 4.717/65 (AP)",
"Dec.-Lei 3.365/41 (Desapropriação)"),
List.of("Legalidade", "Impessoalidade", "Moralidade", "Publicidade", "Eficiência",
"Supremacia do interesse público", "Indisponibilidade do interesse público",
"Autotutela administrativa (Súmulas STF 346 e 473)"),
List.of("STF Súmula 346 (anulação pelo próprio ato)",
"STF Súmula 473 (autotutela — nulidade e conveniência)",
"STJ Súmula 510 (concurso público — direito à nomeação dentro da validade)",
"STJ Súmula 635 (prescrição quinquenal — indenização por atos do Estado)",
"STJ Súmula 212 (MS — prazo 120 dias)"),
List.of("RE 848.826 (responsabilidade eleitoral e Estado)",
"ADI 6.138 (improbidade — dolo específico)"),
List.of("Mandado de segurança — prazo 120 dias (Lei 12.016 art. 23)",
"PAD — prazo 60 dias prorrogáveis", "Processo administrativo — contraditório",
"Controle judicial dos atos discricionários (mérito x legalidade)"),
List.of("Poder de polícia — atributos e limites", "Serviço público — classificação e regime jurídico",
"Empresa pública e sociedade de economia mista",
"Parceria público-privada — riscos compartilhados")
)),
Map.entry("ELEITORAL", new CurriculumModule(
"ELEITORAL", "Direito Eleitoral",
List.of("Sistema eleitoral brasileiro: proporcional, majoritário, misto",
"Partidos políticos — criação, fidelidade, democracia interna",
"Registro de candidatura (RRC) e condições de elegibilidade",
"Inelegibilidades (LC 64/90; LC 135/10 — Ficha Limpa)",
"Propaganda eleitoral — proibições e ilicitudes (Lei 9.504/97)",
"Financiamento de campanhas (pós-ADI 4.650 — vedação PJ)",
"Abuso do poder econômico e político",
"Captação ilícita de sufrágio (CE art. 41-A)",
"AIRC, AIJE, AIME e RCED",
"Prestação de contas de campanha",
"Direito de resposta eleitoral",
"Recurso eleitoral — prazo de 3 dias"),
List.of("CE — Lei 4.737/65", "LAEP — LC 64/90",
"Lei da Ficha Limpa — LC 135/10", "Lei das Eleições — Lei 9.504/97",
"Resoluções TSE vigentes"),
List.of("Soberania popular (CF art. 1º, parágrafo único)",
"Pluralismo político (CF art. 1º, V)",
"Anualidade eleitoral (CF art. 16)",
"Moralidade eleitoral e combate ao abuso de poder"),
List.of("TSE Súmula 37 (cassação mandato por captação ilícita)",
"TSE Súmula 45 (representação por propaganda irregular — prazo)"),
List.of("ADI 4.650 (financiamento privado — vedação pessoas jurídicas)"),
List.of("AIRC — 5 dias pós-publicação edital", "AIJE — até 15 dias após diplomação",
"AIME — até 15 dias após diplomação", "RCED — prazo específico TSE",
"Recurso eleitoral: 3 dias", "Prestação de contas: 30 dias"),
List.of("Quociente eleitoral e quociente partidário",
"Cláusula de barreira (Lei 9.504/97 art. 17)",
"Crimes eleitorais (CE arts. 289-354)")
)),
Map.entry("AMBIENTAL", new CurriculumModule(
"AMBIENTAL", "Direito Ambiental",
List.of("Princípios do direito ambiental (prevenção, precaução, poluidor-pagador, in dubio pro natura)",
"Licenciamento ambiental (CONAMA 237/97; Lei Complementar 140/2011)",
"Responsabilidade civil objetiva ambiental (CF art. 225 §3º; STJ Súmula 613)",
"Imprescritibilidade do dano ambiental",
"Crimes ambientais (Lei 9.605/98) — pessoas físicas e jurídicas",
"Código Florestal (Lei 12.651/12) — APP, reserva legal, CAR",
"SNUC — Sistema Nacional de Unidades de Conservação (Lei 9.985/00)",
"Ação civil pública ambiental (Lei 7.347/85)",
"SISNAMA — estrutura institucional",
"Direitos das gerações futuras e tutela coletiva ambiental"),
List.of("CF art. 225", "Lei 6.938/81 (PNMA)", "Lei 9.605/98 (Crimes Ambientais)",
"Lei 12.651/12 (Código Florestal)", "Lei 7.347/85 (ACP)",
"Lei 9.985/00 (SNUC)", "LC 140/2011 (competências ambientais)"),
List.of("Prevenção", "Precaução", "Poluidor-pagador", "Reparação integral",
"In dubio pro natura", "Função socioambiental da propriedade"),
List.of("STJ Súmula 613 (responsabilidade ambiental — adquirente do imóvel)",
"STJ Súmula 618 (aplicação de multa e reparação — cumulação)"),
List.of("ADPF 708 (inércia Fundo Clima — omissão inconstitucional)",
"STF ADI 4.983 (vaquejada — meio ambiente)"),
List.of("ACP — prazo — imprescritível (dano difuso)",
"Ação penal ambiental — prazo prescricional por pena máxima",
"Tutela inibitória — urgente — in dubio pro natura"),
List.of("Responsabilidade administrativa, civil e penal — independência das instâncias",
"Reparação integral — reconstituição, indenização, compensação ecológica",
"PGA — plano de gerenciamento ambiental")
)),
Map.entry("FAMILIA_SUCESSOES", new CurriculumModule(
"FAMILIA_SUCESSOES", "Direito de Família e Sucessões",
List.of("Casamento — requisitos, impedimentos, causas suspensivas",
"Regimes de bens (separação, comunhão parcial, comunhão universal, participação final nos aquestos)",
"Divórcio direto e consensual extrajudicial",
"União estável — reconhecimento, dissolução e direitos",
"Alimentos: obrigação alimentar, fixação, revisão, execução e extinção",
"Alimentos gravídicos (Lei 11.804/08)",
"Guarda compartilhada (Lei 13.058/14) e unilateral",
"Alienação parental (Lei 12.318/10)",
"Adoção nacional e internacional (ECA arts. 39-52; Lei 13.509/17)",
"Tutela, curatela e tomada de decisão apoiada (Lei 13.146/15)",
"Investigação e reconhecimento de paternidade",
"Inventário judicial e extrajudicial",
"Arrolamento sumário e comum",
"Partilha, colação e sonegação de bens",
"Testamento público, cerrado e particular; codicilo",
"Herdeiros necessários e legítima (CC art. 1.845)",
"Bem de família legal e voluntário (Lei 8.009/90)"),
List.of("CC/2002 arts. 1.511-2.046", "ECA — Lei 8.069/90",
"Lei 13.058/14 (guarda compartilhada)", "Lei 12.318/10 (alienação parental)",
"Lei 11.804/08 (alimentos gravídicos)", "Lei 8.009/90 (bem de família)",
"Lei 13.146/15 (LBI — curatela)", "Lei 13.509/17 (adoção)"),
List.of("Princípio da afetividade (base das relações familiares)",
"Proteção integral da criança e do adolescente (CF art. 227)",
"Melhor interesse do menor", "Solidariedade familiar",
"Igualdade entre cônjuges e companheiros (CF art. 226 §5º)"),
List.of("STJ Súmula 301 (DNA — presunção absoluta de paternidade)",
"STJ Súmula 364 (bem de família — único imóvel — impenhorabilidade)",
"STJ Súmula 596 (alimentos gravídicos — ônus da prova)",
"STJ Súmula 522 (alimentos avoengos — subsidiários)"),
List.of("RE 878.694 (herança — igualdade cônjuge e companheiro)",
"RE 898.060 (filiação socioafetiva — multiparentalidade)"),
List.of("Divórcio extrajudicial: imediato (cartório) — sem filhos menores",
"Alimentos provisionais: tutela urgente imediata (CPC 300)",
"Inventário: 60 dias do óbito (CPC 611)", "Arrolamento: qualquer valor — consenso"),
List.of("Multiparentalidade e paternidade socioafetiva",
"Direito das famílias homoafetivas (ADI 4.277)",
"Família monoparental e reconstituída",
"Herança digital — tratamento dos bens digitais")
)),
Map.entry("CONSTITUCIONAL", new CurriculumModule(
"CONSTITUCIONAL", "Direito Constitucional",
List.of("Teoria da Constituição — conceito, classificação, poder constituinte",
"Princípios fundamentais (CF art. 1º-4º)", "Direitos fundamentais (CF art. 5º-17)",
"Organização do Estado — federalismo e repartição de competências",
"Organização dos Poderes (CF art. 44-135)",
"Controle difuso de constitucionalidade",
"Controle concentrado: ADI, ADC, ADPF, ADO",
"Mandado de injunção individual e coletivo",
"Intervenção federal e estadual",
"Defesa do Estado e das instituições democráticas",
"Ordem econômica e financeira (CF art. 170-192)",
"Ordem social (CF art. 193-232)",
"Emendas constitucionais — cláusulas pétreas (CF art. 60 §4º)",
"Repercussão geral e súmula vinculante"),
List.of("CF/1988 — integral", "Lei 9.868/99 (ADI/ADC)", "Lei 9.882/99 (ADPF)",
"Lei 12.063/09 (ADO)", "RISTF — Regimento Interno STF"),
List.of("Dignidade da pessoa humana (CF art. 1º, III)",
"Separação dos poderes (CF art. 2º)",
"Estado Democrático de Direito", "Supremacia da Constituição",
"Força normativa da Constituição (Hesse)",
"Proporcionalidade e razoabilidade",
"Eficácia horizontal dos direitos fundamentais"),
List.of("STF Súmula Vinculante 10 (clausula de reserva de plenário)",
"STF Súmula Vinculante 28 (inconstitucionalidade tributária — depósito)"),
List.of("RE 593.727 (investigação por MP — validade)",
"RE 848.826 (responsabilidade eleitoral pelo Estado)",
"ADPF 54 (anencefalia — antecipação de parto — atípica)"),
List.of("ADI/ADC/ADPF — sem prazo fixo (ação abstrata)",
"Mandado de injunção: urgente (omissão inconstitucional)",
"RE — tempestividade 15 dias + repercussão geral"),
List.of("Bloco de constitucionalidade — tratados DDHH",
"Controle preventivo e repressivo",
"Modulação dos efeitos das decisões em controle concentrado")
)),
Map.entry("INFANCIA_JUVENTUDE", new CurriculumModule(
"INFANCIA_JUVENTUDE", "Direito da Criança e do Adolescente",
List.of("Doutrina da proteção integral (CF art. 227; ECA)",
"Criança (0-12) e adolescente (12-18) — tratamento diferenciado",
"Medidas protetivas (ECA art. 101) — tipos e aplicação",
"Ato infracional e medidas socioeducativas (SINASE — Lei 12.594/12)",
"Internação — requisitos estritos (ECA art. 122) — proporcionalidade",
"Adoção — lista única, estágio de convivência, habilitação",
"Destituição do poder familiar — requisitos e processo",
"Acolhimento institucional e familiar — temporalidade",
"Conselho Tutelar — competência e medidas",
"Trabalho infantil — proibição e aprendiz",
"Violência doméstica contra criança e adolescente",
"Direito à convivência familiar e comunitária"),
List.of("ECA — Lei 8.069/90", "CF art. 227-228",
"Lei 12.594/12 (SINASE)", "Lei 13.509/17 (adoção — prazo 120 dias)",
"Lei 12.010/09 (adoção — convivência)"),
List.of("Prioridade absoluta (CF art. 227; ECA art. 4º)",
"Melhor interesse da criança", "Proteção integral",
"Brevidade, excepcionalidade e respeito à condição peculiar do adolescente"),
List.of("STJ Súmula 605 (dano moral coletivo — criança e adolescente)"),
List.of("HC 143.988 (internação ECA — medida excepcional)"),
List.of("Internação provisória: máximo 45 dias (ECA art. 108)",
"Internação: máximo 3 anos (ECA art. 121)",
"Prazo adoção: 120 dias com renovação (Lei 13.509/17)"),
List.of("Inimputabilidade do menor de 18 anos (CF art. 228)",
"Redução da maioridade penal — proposta e vedação (cláusula pétrea)",
"Responsabilização civil dos pais por atos do filho (CC art. 932)")
)),
Map.entry("AGRARIO", new CurriculumModule(
"AGRARIO", "Direito Agrário e Fundiário",
List.of("Estatuto da Terra (Lei 4.504/64) — função social da propriedade rural",
"Reforma agrária — competência da União (CF art. 184-186)",
"Desapropriação por interesse social para reforma agrária",
"Imunidade do imóvel produtivo à desapropriação (CF art. 185)",
"CCIR, ITR, CAR, SNCR — cadastros rurais",
"Usucapião pro labore (CF art. 191; CC art. 1.239)",
"Arrendamento rural (Dec. 59.566/66)",
"Parceria rural (Dec. 59.566/66)",
"Posse agrária e conflitos fundiários coletivos",
"Regularização fundiária rural (Lei 13.465/17)",
"Violência no campo e proteção de defensores de direitos",
"Propriedade quilombola (CF art. 68 ADCT)"),
List.of("Lei 4.504/64 (Estatuto da Terra)", "CF arts. 184-191",
"LC 76/93 (rito desapropriação agrária)", "Dec.-Lei 3.365/41",
"Lei 13.465/17 (regularização fundiária)", "Lei 8.629/93 (desapropriação)"),
List.of("Função social da propriedade rural (CF art. 186)",
"Justa e prévia indenização em dinheiro (CF art. 184)",
"Propriedade produtiva — vedação à desapropriação",
"Moradia e sustento familiares — usucapião especial rural"),
List.of("STJ Súmula 354 (cálculo indenização desapropriação — base de cálculo)"),
List.of("MS 25.284 STF (imóvel produtivo — imunidade — comprovação)"),
List.of("Desapropriação agrária — rito urgente LC 76/93",
"Usucapião rural: 5 anos de posse mansa (CF art. 191)",
"Reintegração de posse rural: audiência prévia obrigatória — conflitos coletivos"),
List.of("Índices de produtividade (GUT e GEE) — critério desapropriação",
"Terras devolutas — titularidade e regularização",
"RECA — regularização de assentamentos")
))
);
private static String resolveCurriculumKey(String ramo) {
return switch (BrazilianLegalKnowledgeBase.resolveRamoCodigo(ramo)) {
case "FAMILIA_SUCESSOES" -> "FAMILIA_SUCESSOES";
case "SAUDE", "IMOBILIARIO" -> "CIVIL";
case "PROPRIEDADE_INTELECTUAL" -> "EMPRESARIAL";
case "EDUCACAO", "INTERNACIONAL" -> "CONSTITUCIONAL";
default -> BrazilianLegalKnowledgeBase.resolveRamoCodigo(ramo);
};
}

private static int score(String q, CurriculumModule module) {
int score = 0;
String needle = q.replace('_', ' ');
if (module.ramo().toLowerCase(Locale.ROOT).contains(q)) score += 30;
if (module.nome().toLowerCase(Locale.ROOT).contains(needle)) score += 25;
for (String s : module.materias()) if (s.toLowerCase(Locale.ROOT).contains(needle)) score += 8;
for (String s : module.legislacaoPrincipal()) if (s.toLowerCase(Locale.ROOT).contains(needle)) score += 6;
for (String s : module.topicosProcessuais()) if (s.toLowerCase(Locale.ROOT).contains(needle)) score += 5;
for (String s : module.topicosSubstantivos()) if (s.toLowerCase(Locale.ROOT).contains(needle)) score += 4;
return score;
}

}
