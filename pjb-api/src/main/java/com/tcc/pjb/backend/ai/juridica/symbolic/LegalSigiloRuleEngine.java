package com.tcc.pjb.backend.ai.juridica.symbolic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class LegalSigiloRuleEngine implements LegalDeterministicRuleEngine {

    private static final Pattern CPF_PATTERN = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");

    @Override
    public String engineCode() {
        return LegalSymbolicValidationCatalog.ENGINE_SIGILO;
    }

    @Override
    public LegalSymbolicValidationOutcome evaluate(LegalSymbolicValidationContext context) {
        String text = context == null ? "" : context.normalizedText();
        boolean relevant = context != null && (!context.normalizedSigilo().isBlank()
                || context.textContains("sigilo", "sigiloso", "restrito", "confidencial", "segredo de justica", "segredo de justiça"));
        boolean sensitiveDataDetected = CPF_PATTERN.matcher(text).find() || (context != null && context.textContains("cpf", "cnpj", "dados medicos", "dados médicos", "endereco", "endereço", "filiacao", "filiação"));
        boolean maskingDetected = context != null && context.textContains("anonimiz", "mascar", "tarja", "ocult");
        List<LegalSymbolicValidationIssue> issues = new ArrayList<>();
        if (context != null && context.sigiloContains("publico", "público")
                && context.textContains("sigiloso", "restrito", "confidencial", "segredo de justica", "segredo de justiça")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Contexto marcado como público com texto que exige tratamento reservado ou sigiloso.",
                    Map.of("sigilo", context.normalizedSigilo())
            ));
        }
        if (context != null && context.sigiloContains("restrito", "sigiloso", "segredo")
                && context.textContains("consulta publica irrestrita", "consulta pública irrestrita", "divulgacao ampla", "divulgação ampla")) {
            issues.add(LegalSymbolicValidationIssue.contradiction(
                    engineCode(),
                    "Contexto sigiloso com orientação textual de exposição pública ampla.",
                    Map.of("sigilo", context.normalizedSigilo())
            ));
        }
        if (relevant && sensitiveDataDetected && !maskingDetected) {
            issues.add(LegalSymbolicValidationIssue.missingEvidence(
                    engineCode(),
                    "Conteúdo com indício de dado sensível sem política explícita de mascaramento, anonimização ou tarja.",
                    Map.of("sigilo", context == null ? "" : context.normalizedSigilo())
            ));
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("relevant", relevant);
        diagnostics.put("sensitiveDataDetected", sensitiveDataDetected);
        diagnostics.put("maskingDetected", maskingDetected);
        return LegalSymbolicValidationOutcome.of(engineCode(), issues, diagnostics);
    }
}
