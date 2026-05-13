package com.tcc.pjb.backend.platform.text;

import java.util.Set;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.shared.text.TextTokenUtils;

@Component
public class JaccardScorer {

    public double score(String query, String document) {
        Set<String> a = TextTokenUtils.tokens(query);
        Set<String> b = TextTokenUtils.tokens(document);
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        if (a.size() > b.size()) {
            Set<String> tmp = a;
            a = b;
            b = tmp;
        }
        int intersection = 0;
        for (String token : a) {
            if (b.contains(token)) {
                intersection++;
            }
        }
        int union = a.size() + b.size() - intersection;
        if (union <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) intersection / (double) union));
    }
}
