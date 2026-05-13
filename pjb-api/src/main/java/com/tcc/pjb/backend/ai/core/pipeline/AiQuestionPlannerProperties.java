package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.ai.questions")
public class AiQuestionPlannerProperties {

    private final Map<String, DomainRules> domains = new HashMap<>();

    public Map<String, DomainRules> getDomains() {
        return domains;
    }

    public static final class DomainRules {

        private int maxQuestions = 5;
        private double noveltyBonus = 0.75;

        
        private Map<String, Double> categoryWeights = new HashMap<>();

        
        private Map<String, List<String>> categoryKeywords = new HashMap<>();

        public int getMaxQuestions() {
            return maxQuestions;
        }

        public void setMaxQuestions(int maxQuestions) {
            this.maxQuestions = maxQuestions;
        }

        public double getNoveltyBonus() {
            return noveltyBonus;
        }

        public void setNoveltyBonus(double noveltyBonus) {
            this.noveltyBonus = noveltyBonus;
        }

        public Map<String, Double> getCategoryWeights() {
            return categoryWeights;
        }

        public void setCategoryWeights(Map<String, Double> categoryWeights) {
            this.categoryWeights = (categoryWeights == null) ? new HashMap<>() : new HashMap<>(categoryWeights);
        }

        public Map<String, List<String>> getCategoryKeywords() {
            return categoryKeywords;
        }

        public void setCategoryKeywords(Map<String, List<String>> categoryKeywords) {
            if (categoryKeywords == null) {
                this.categoryKeywords = new HashMap<>();
                return;
            }
            Map<String, List<String>> out = new HashMap<>();
            for (Map.Entry<String, List<String>> e : categoryKeywords.entrySet()) {
                out.put(e.getKey(), e.getValue() == null ? new ArrayList<>() : new ArrayList<>(e.getValue()));
            }
            this.categoryKeywords = out;
        }
    }
}
