package com.tcc.pjb.backend.service;

import java.text.NumberFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.EssenceResult;

@Service
public class EssenceFilter {

    private static final Logger logger = LoggerFactory.getLogger(EssenceFilter.class);

    public boolean detectAbusiveClauses(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }
        List<String> abusivos = List.of("renúncia de direitos", "cláusula abusiva", "obrigações desproporcionais");
        return abusivos.stream().anyMatch(t -> html.toLowerCase().contains(t));
    }

    
    public EssenceResult evaluate(String original, String suggested) {
        if (original == null || suggested == null) {
            logger.warn("Avaliação de essência ignorada devido a parâmetros nulos.");
            return new EssenceResult(true, 0.0, 0.0, Collections.emptyList(), "Texto inválido para avaliação.");
        }

        logger.info("Analisando essência | OriginalLength={} | SuggestedLength={}",
                original.length(), suggested.length());

        Set<String> originalTokens = tokenize(original);
        Set<String> suggestedTokens = tokenize(suggested);

        double similarity = jaccardSimilarity(originalTokens, suggestedTokens);
        double difference = 1 - similarity;
        List<String> divergences = detectDivergences(original, suggested);

        boolean essencePreserved = similarity >= 0.40 && divergences.isEmpty();

        
        String suggestion;
        if (!essencePreserved) {
            NumberFormat nf = NumberFormat.getPercentInstance(Locale.forLanguageTag("pt-BR"));
            suggestion = "Revisar cláusulas divergentes. Similaridade atual: " + nf.format(similarity);
        } else {
            suggestion = "Essência preservada. Prosseguir com homologação.";
        }

        return new EssenceResult(essencePreserved, similarity, difference, divergences, suggestion);
    }

    

    private Set<String> tokenize(String text) {
        String cleaned = text.toLowerCase().replaceAll("[^a-zà-ú0-9 ]", " ");
        return new HashSet<>(Arrays.asList(cleaned.split("\\s+")));
    }

    private double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return inter.size() / (double) union.size();
    }

    private List<String> detectDivergences(String original, String suggested) {
        List<String> diffs = new ArrayList<>();
        List<String> termosEssenciais = List.of(
                "responsabilidade", "obrigação", "culpa", "dolo",
                "prazo", "valor", "pedido", "requer",
                "determina", "indeferido", "procedente", "improcedente",
                "indenização", "tutela", "urgência"
        );

        for (String termo : termosEssenciais) {
            boolean o = original.toLowerCase().contains(termo);
            boolean s = suggested.toLowerCase().contains(termo);

            if (o && !s) {
                diffs.add("Termo essencial removido: " + termo);
            } else if (!o && s) {
                diffs.add("Termo essencial adicionado: " + termo);
            }
        }
        return diffs;
    }
}