package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiJuridicalLineageDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalAiJuridicalLineageRegistry {

    private final List<LineageRule> rules = List.of(
            rule("CONSTITUCIONAL", "Direito Constitucional", "constitucionalismo e controle do poder",
                    List.of("Hans Kelsen", "Konrad Hesse", "Robert Alexy", "Ronald Dworkin"),
                    List.of("José Afonso da Silva", "Paulo Bonavides", "Gilmar Ferreira Mendes", "Luís Roberto Barroso"),
                    List.of("força normativa", "máxima efetividade", "proporcionalidade", "integridade constitucional"),
                    List.of("Constituição vigente", "controle de constitucionalidade", "precedente vinculante", "competência constitucional"),
                    List.of("constit", "stf", "adpf", "adi", "direito fundamental", "proporcionalidade", "liberdade", "dignidade")),
            rule("ADMINISTRATIVO", "Direito Administrativo", "regime jurídico-administrativo e supremacia condicionada pelo interesse público",
                    List.of("Otto Mayer", "Maurice Hauriou", "Léon Duguit"),
                    List.of("Celso Antônio Bandeira de Mello", "Maria Sylvia Zanella Di Pietro", "Hely Lopes Meirelles", "Marçal Justen Filho"),
                    List.of("legalidade administrativa", "motivação", "proporcionalidade", "controle do desvio de finalidade"),
                    List.of("competência do agente", "motivação do ato", "processo administrativo", "controle de legalidade"),
                    List.of("administr", "licita", "improb", "servidor", "ato administrativo", "poder de policia", "concurso")),
            rule("CIVIL", "Direito Civil", "romanística privada e codificação civil",
                    List.of("Caio", "Justiniano", "Savigny", "Pothier"),
                    List.of("Clóvis Beviláqua", "Pontes de Miranda", "Miguel Reale", "Orlando Gomes", "Gustavo Tepedino"),
                    List.of("autonomia privada funcionalizada", "boa-fé objetiva", "função social", "fato-valor-norma"),
                    List.of("negócio jurídico", "responsabilidade civil", "prova do dano", "nexo causal"),
                    List.of("civil", "contrat", "obriga", "dano", "lucro cessante", "família", "sucess", "posse", "propriedade")),
            rule("PROCESSUAL_CIVIL", "Direito Processual Civil", "processo como garantia e técnica de tutela jurisdicional",
                    List.of("Giuseppe Chiovenda", "Francesco Carnelutti", "Enrico Tullio Liebman", "Mauro Cappelletti"),
                    List.of("Humberto Theodoro Júnior", "Cândido Rangel Dinamarco", "Fredie Didier Jr.", "Teresa Arruda Alvim"),
                    List.of("devido processo", "contraditório substancial", "primazia do mérito", "cooperação processual"),
                    List.of("competência", "cabimento", "prazo", "interesse processual", "ônus da prova"),
                    List.of("processo civil", "cpc", "recurso", "agravo", "apela", "embargos", "tutela", "prazo", "prova")),
            rule("PENAL", "Direito Penal", "limitação do poder punitivo estatal",
                    List.of("Cesare Beccaria", "Franz von Liszt", "Claus Roxin", "Luigi Ferrajoli"),
                    List.of("Nelson Hungria", "Heleno Cláudio Fragoso", "Cezar Roberto Bitencourt", "Juarez Tavares"),
                    List.of("legalidade estrita", "culpabilidade", "intervenção mínima", "garantismo"),
                    List.of("tipicidade", "ilicitude", "culpabilidade", "prova da autoria", "cadeia de custódia"),
                    List.of("penal", "crime", "dolo", "culpa", "homic", "feminic", "júri", "prisão", "flagrante")),
            rule("PROCESSUAL_PENAL", "Direito Processual Penal", "garantia contra o arbítrio na persecução penal",
                    List.of("James Goldschmidt", "Francesco Carnelutti", "Luigi Ferrajoli"),
                    List.of("Aury Lopes Jr.", "Ada Pellegrini Grinover", "Gustavo Badaró", "Eugênio Pacelli"),
                    List.of("presunção de inocência", "paridade de armas", "cadeia de custódia", "juiz natural"),
                    List.of("competência", "prova lícita", "prisão cautelar", "nulidade", "rito do júri"),
                    List.of("cpp", "processo penal", "denúncia", "queixa", "prisão preventiva", "habeas", "interrogatório")),
            rule("TRABALHISTA", "Direito do Trabalho", "proteção social do trabalho humano",
                    List.of("Hugo Sinzheimer", "Mario de la Cueva", "Américo Plá Rodriguez"),
                    List.of("Arnaldo Süssekind", "Maurício Godinho Delgado", "Vólia Bomfim Cassar", "Alice Monteiro de Barros"),
                    List.of("proteção", "primazia da realidade", "continuidade", "indisponibilidade relativa"),
                    List.of("vínculo", "verbas", "jornada", "ônus probatório", "prescrição trabalhista"),
                    List.of("trabalho", "clt", "emprego", "verbas", "jornada", "rescis", "fgts", "reclamação trabalhista")),
            rule("TRIBUTARIO", "Direito Tributário", "legalidade fiscal e limitação constitucional ao poder de tributar",
                    List.of("Albert Hensel", "Dino Jarach", "Sainz de Bujanda"),
                    List.of("Aliomar Baleeiro", "Geraldo Ataliba", "Paulo de Barros Carvalho", "Roque Antonio Carrazza"),
                    List.of("legalidade tributária", "tipicidade cerrada", "capacidade contributiva", "segurança jurídica"),
                    List.of("regra-matriz", "lançamento", "decadência", "prescrição", "competência tributária"),
                    List.of("tribut", "imposto", "icms", "iss", "ipi", "execução fiscal", "ctn", "lançamento")),
            rule("AMBIENTAL", "Direito Ambiental", "responsabilidade intergeracional e prevenção do dano ecológico",
                    List.of("Hans Jonas", "Michel Prieur", "Alexandre Kiss"),
                    List.of("Paulo Affonso Leme Machado", "Édis Milaré", "José Afonso da Silva", "Antônio Herman Benjamin"),
                    List.of("precaução", "prevenção", "poluidor-pagador", "desenvolvimento sustentável"),
                    List.of("licenciamento", "nexo ambiental", "risco integral", "reparação integral"),
                    List.of("ambient", "licenciamento", "ibama", "dano ambiental", "app", "reserva legal", "poluição")),
            rule("ELEITORAL", "Direito Eleitoral", "legitimidade democrática e igualdade de disputa",
                    List.of("Hans Kelsen", "Giovanni Sartori", "Robert Dahl"),
                    List.of("José Jairo Gomes", "Adriano Soares da Costa", "Joel José Cândido"),
                    List.of("soberania popular", "lisura eleitoral", "paridade de armas", "normalidade do pleito"),
                    List.of("competência eleitoral", "prazos eleitorais", "inelegibilidade", "prova da captação ilícita"),
                    List.of("eleitoral", "tse", "tre", "ineleg", "propaganda eleitoral", "registro de candidatura", "ai je", "aije"))
    );

    public List<LegalAiJuridicalLineageDescriptor> resolve(LegalAiConversationRequest request, String capability) {
        String evidence = normalize((request == null ? "" : request.message()) + " " + capability + " " + contextText(request));
        List<LegalAiJuridicalLineageDescriptor> selected = new ArrayList<>();
        for (LineageRule rule : rules) {
            if (rule.matches(evidence)) {
                selected.add(rule.descriptor());
            }
        }
        if (selected.isEmpty()) {
            selected.add(rules.getFirst().descriptor());
            selected.add(generalTheory());
        }
        return selected.stream().limit(4).toList();
    }

    private LegalAiJuridicalLineageDescriptor generalTheory() {
        return new LegalAiJuridicalLineageDescriptor(
                "TEORIA_GERAL",
                "Teoria Geral do Direito",
                "validade, justiça, eficácia e interpretação institucional",
                List.of("Aristóteles", "Tomás de Aquino", "Hans Kelsen", "H. L. A. Hart", "Norberto Bobbio", "Ronald Dworkin"),
                List.of("Miguel Reale", "Tércio Sampaio Ferraz Jr.", "Paulo Nader"),
                List.of("fato-valor-norma", "validade normativa", "integridade", "segurança jurídica"),
                List.of("fonte normativa", "hierarquia", "vigência", "competência", "coerência sistêmica")
        );
    }

    private static LineageRule rule(String code, String name, String tradition, List<String> founders, List<String> brazilian, List<String> lenses, List<String> checks, List<String> tokens) {
        return new LineageRule(new LegalAiJuridicalLineageDescriptor(code, name, tradition, founders, brazilian, lenses, checks), tokens);
    }

    private static String contextText(LegalAiConversationRequest request) {
        Map<String, Object> context = request == null || request.context() == null ? Map.of() : request.context();
        return String.join(" ", context.values().stream().map(String::valueOf).toList());
    }

    private static String normalize(String value) {
        String safe = value == null ? "" : value.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(safe, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.trim();
    }

    private record LineageRule(LegalAiJuridicalLineageDescriptor descriptor, List<String> tokens) {
        boolean matches(String text) {
            return tokens.stream().map(LegalAiJuridicalLineageRegistry::normalize).anyMatch(text::contains);
        }
    }
}
