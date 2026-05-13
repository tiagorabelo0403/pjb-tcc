package com.tcc.pjb.backend.core.security.sigilo;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.Locale;

@Service
public class DocumentoSigiloClassifier {

    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    private static final Pattern CPF_RAW = Pattern.compile("\\b\\d{11}\\b");
    private static final Pattern CEP = Pattern.compile("\\b\\d{5}-?\\d{3}\\b");
    private static final Pattern RG = Pattern.compile("\\b\\d{1,2}\\.?\\d{3}\\.?\\d{3}-?[0-9Xx]\\b");
    private static final Pattern SUS = Pattern.compile("\\b\\d{15}\\b");
    private static final Pattern DATA_NASC = Pattern.compile("(data\\s+de\\s+nasc|nascimento|nasc\\.)");
    private static final Pattern CID = Pattern.compile("\\bCID[- ]?10\\b|\\bCID\\b|\\b[ABCEFGHIJKLMNOPQRSTUVZ]\\d{2}\\.?\\d?\\b");

    private static final Set<String> FN_ANCHORS = Set.of(
            "cpf", "rg", "cnh", "carteira", "identidade", "titulo_eleitor", "comprovante",
            "holerite", "contracheque", "extrato", "fatura", "endereco", "residencia",
            "prontuario", "receita", "atestado", "laudo", "exame", "relatorio", "internacao",
            "hospital", "upa", "clinica"
    );

    public record Classification(
            DocumentoCategoria suggestedCategoria,
            NivelSigilo minSigilo,
            double confidence,
            List<String> reasons
    ) {
    }

    
    public Classification classify(String filename, String sampleText) {
        String fn = norm(filename);
        String tx = norm(sampleText);

        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        if (!fn.isBlank()) {
            int hits = 0;
            for (String a : FN_ANCHORS) {
                if (fn.contains(a)) hits++;
            }
            if (hits > 0) {
                double s = Math.min(0.35, 0.10 * hits);
                score += s;
                reasons.add("Nome do arquivo contém marcadores pessoais/saúde (hits=" + hits + ")");
            }
        }

        if (!tx.isBlank()) {
            boolean cpf = CPF.matcher(tx).find() || CPF_RAW.matcher(tx).find();
            boolean rg = RG.matcher(tx).find();
            boolean cep = CEP.matcher(tx).find();
            boolean sus = SUS.matcher(tx).find();
            boolean nasc = DATA_NASC.matcher(tx).find();
            boolean cid = CID.matcher(tx).find();

            if (cpf) {
                score += 0.45;
                reasons.add("Sinal forte: CPF detectado no texto");
            }
            if (rg) {
                score += 0.20;
                reasons.add("Sinal: RG/identidade detectado no texto");
            }
            if (sus) {
                score += 0.15;
                reasons.add("Sinal: número do Cartão SUS (15 dígitos) detectado");
            }
            if (nasc) {
                score += 0.10;
                reasons.add("Sinal: referência a data de nascimento detectada");
            }
            if (cep) {
                score += 0.08;
                reasons.add("Sinal: CEP/endereço detectado");
            }
            if (cid) {
                score += 0.12;
                reasons.add("Sinal: CID/diagnóstico detectado");
            }
        }

        score = Math.max(0.0, Math.min(1.0, score));
        DocumentoCategoria cat = (score >= 0.65) ? DocumentoCategoria.PESSOAL : null;
        NivelSigilo min = (score >= 0.65) ? NivelSigilo.SIGILO_N2 : NivelSigilo.PUBLICO;

        if (cat != null) {
            reasons.add("Política: conteúdo pessoal/PHI -> categoria PESSOAL e mínimo SIGILO_N2");
        }

        return new Classification(cat, min, round2(score), List.copyOf(reasons));
    }

    private static String norm(String raw) {
        if (raw == null) return "";
        String s = raw.toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
        return s;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
