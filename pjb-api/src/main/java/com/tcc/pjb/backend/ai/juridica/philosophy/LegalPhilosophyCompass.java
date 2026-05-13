package com.tcc.pjb.backend.ai.juridica.philosophy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalPhilosophyCompass {

    public List<LegalPhilosopher> suggest(Map<String, Object> payload) {
        String tema = "";
        if (payload != null) {
            tema = String.valueOf(payload.getOrDefault("tema", payload.getOrDefault("assunto", payload.getOrDefault("area", ""))));
        }
        tema = tema == null ? "" : tema.toLowerCase(Locale.ROOT);

        List<LegalPhilosopher> out = new ArrayList<>();

        
        if (containsAny(tema, "constit", "direitos fundamentais", "controle", "stf", "principio", "proporcional")) {
            out.add(new LegalPhilosopher("Konrad Hesse", "teoria constitucional", "força normativa da Constituição"));
            out.add(new LegalPhilosopher("Robert Alexy", "teoria dos direitos fundamentais", "regras x princípios e ponderação"));
            out.add(new LegalPhilosopher("José Afonso da Silva", "constitucional", "sistematização e efetividade"));
            out.add(new LegalPhilosopher("Paulo Bonavides", "constitucional", "estado democrático e direitos"));
        }

        
        if (containsAny(tema, "administr", "licita", "improb", "servidor", "ato administrativo", "poder de policia")) {
            out.add(new LegalPhilosopher("Celso Antônio Bandeira de Mello", "direito administrativo", "regime jurídico-administrativo e princípios"));
            out.add(new LegalPhilosopher("Maria Sylvia Zanella Di Pietro", "direito administrativo", "conceituação e aplicação prática"));
        }

        
        if (containsAny(tema, "obriga", "contrat", "responsabilidade civil", "dano", "inadimplemento")) {
            out.add(new LegalPhilosopher("Miguel Reale", "teoria do direito / civil", "tridimensionalidade (fato, valor, norma)"));
            out.add(new LegalPhilosopher("Pontes de Miranda", "civil", "sistematização rigorosa e categorias"));
            out.add(new LegalPhilosopher("Carlos Roberto Gonçalves", "civil", "didática aplicada em obrigações"));
        }

        
        if (containsAny(tema, "penal", "crime", "culp", "dolo", "tipicidade", "ilicitude")) {
            out.add(new LegalPhilosopher("Eugenio Raúl Zaffaroni", "penal", "garantismo e crítica do sistema penal"));
            out.add(new LegalPhilosopher("Claus Roxin", "penal", "domínio do fato e teoria do delito"));
        }

        
        if (containsAny(tema, "processo", "prova", "recurso", "tutela de urgencia", "procedimento")) {
            out.add(new LegalPhilosopher("Humberto Theodoro Júnior", "processual civil", "tutelas e estrutura processual"));
            out.add(new LegalPhilosopher("Fredie Didier Jr.", "processual civil", "sistematização contemporânea e precedentes"));
        }

        
        if (out.isEmpty()) {
            out.add(new LegalPhilosopher("Miguel Reale", "teoria do direito", "tridimensionalidade e integração norma/valor/fato"));
            out.add(new LegalPhilosopher("Norberto Bobbio", "filosofia do direito", "positivismo, validade e eficácia"));
        }

        return out;
    }

    public List<String> lenses() {
        return LegalHermeneuticsLens.defaultLenses();
    }

    private static boolean containsAny(String text, String... tokens) {
        if (text == null) return false;
        for (String t : tokens) {
            if (t == null || t.isBlank()) continue;
            if (text.contains(t.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
