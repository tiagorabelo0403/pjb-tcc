package com.tcc.pjb.backend.ai.common.clients.local;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.common.AiModelClient;
import java.util.Locale;

public class LocalHeuristicAiModelClient implements AiModelClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*?\\}");
    private static final Pattern INPUT_BLOCK = Pattern.compile("Entrada:\\s*(\\{[\\s\\S]*?\\})", Pattern.CASE_INSENSITIVE);
    private static final Set<String> STOP = Set.of(
            "de","da","do","das","dos","a","o","as","os","e","em","no","na","nos","nas",
            "para","por","com","sem","art","artigo","lei","cpc","cpp","cf","stf","stj"
    );

    private final String version;
    private long timeoutMillis = 10_000;

    public LocalHeuristicAiModelClient(String version) {
        this.version = version == null ? "v1" : version;
    }

    @Override
    public String generate(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt não pode ser vazio");
        }
        boolean wantsJson = prompt.toLowerCase(Locale.ROOT).contains("json")
                && (prompt.toLowerCase(Locale.ROOT).contains("exclus") || prompt.toLowerCase(Locale.ROOT).contains("retorne"));

        if (wantsJson) {
            return generateJson(prompt);
        }
        return "[LOCAL-" + version.toUpperCase(Locale.ROOT) + "] "
                + "Resposta heurística gerada em " + LocalDateTime.now() + ".\n"
                + "Resumo: " + summarize(prompt);
    }

    @Override
    public double[] embed(String text) {
        int hash = (text == null ? 0 : text).hashCode();
        int a = Math.floorMod(hash, 100);
        int b = Math.floorMod(Math.floorDiv(hash, 100), 100);
        int c = Math.floorMod(Math.floorDiv(hash, 10_000), 100);
        return new double[]{a / 100.0, b / 100.0, c / 100.0};
    }

    @Override
    public void setTimeout(long millis) {
        this.timeoutMillis = millis;
    }

    private String generateJson(String prompt) {
        Map<String, Object> input = tryParseInput(prompt);
        if (prompt.contains("\"classificacao\"")
                && prompt.contains("\"keywords\"")
                && prompt.contains("documentosFaltantes")) {

            String assunto = Objects.toString(input.getOrDefault("assunto", ""), "");
            String materia = Objects.toString(input.getOrDefault("materia", ""), "");
            String contexto = Objects.toString(input.getOrDefault("contextoJuridico", ""), "");

            String classificacao = classify(assunto, materia, contexto);
            List<String> keywords = keywordsFrom(assunto + " " + materia);
            List<String> docs = suggestDocuments(materia, assunto);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("classificacao", classificacao);
            out.put("keywords", keywords);
            out.put("documentosFaltantesSugeridos", docs);
            return toJson(out);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "LOCAL_" + version.toUpperCase(Locale.ROOT));
        out.put("generatedAt", LocalDateTime.now().toString());
        out.put("note", "Modo local: heurísticas (sem LLM). Para respostas mais ricas, habilite pjb.ai.provider=openai e configure OPENAI_API_KEY.");
        out.put("inputKeys", new ArrayList<>(input.keySet()));
        out.put("keywords", keywordsFrom(String.valueOf(input)));
        return toJson(out);
    }

    private Map<String, Object> tryParseInput(String prompt) {
        try {
            Matcher m = INPUT_BLOCK.matcher(prompt);
            if (m.find()) {
                String json = m.group(1);
                return MAPPER.readValue(json, new TypeReference<>() {});
            }
        } catch (Exception ignored) {
        }
        try {
            Matcher m2 = JSON_BLOCK.matcher(prompt);
            if (m2.find()) {
                String json = m2.group();
                return MAPPER.readValue(json, new TypeReference<>() {});
            }
        } catch (Exception ignored) {
        }

        return new LinkedHashMap<>();
    }

    private String classify(String assunto, String materia, String contexto) {
        String s = (assunto + " " + materia + " " + contexto).toLowerCase(Locale.ROOT);

        if (containsAny(s, "habeas", "pris", "flagrante", "denuncia", "inquerito", "crime")) {
            return "Penal";
        }
        if (containsAny(s, "fgts", "verbas", "rescis", "justa causa", "ctps", "salario")) {
            return "Trabalhista";
        }
        if (containsAny(s, "contrato", "indeniza", "dano moral", "dano material", "consumidor", "cdc")) {
            return "Cível";
        }
        if (containsAny(s, "servidor", "licita", "improbidade", "prefeitura", "estado", "união", "administrativo")) {
            return "Administrativo";
        }
        if (containsAny(s, "divorcio", "guarda", "alimentos", "paternidade", "familia")) {
            return "Família";
        }
        if (containsAny(s, "inss", "aposentadoria", "auxilio", "beneficio", "bpc")) {
            return "Previdenciário";
        }
        return materia != null && !materia.isBlank() ? materia : "Triagem Geral";
    }

    private List<String> suggestDocuments(String materia, String assunto) {
        String s = (materia + " " + assunto).toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (containsAny(s, "trabalh", "rescis", "verbas")) {
            out.add("CTPS (todas as páginas)");
            out.add("Holerites/contracheques");
            out.add("TRCT e comprovantes de pagamento");
            out.add("Cartões de ponto / escala / registros");
        }
        if (containsAny(s, "penal", "crime", "pris")) {
            out.add("Boletim de ocorrência / Auto de prisão / Termos");
            out.add("Decisão que decretou a prisão (se houver)");
            out.add("Documentos pessoais do acusado e endereço");
            out.add("Provas documentais e rol de testemunhas" );
        }
        if (containsAny(s, "familia", "guarda", "alimentos")) {
            out.add("Certidão de nascimento/casamento");
            out.add("Comprovantes de renda e despesas");
            out.add("Comprovante de residência");
        }
        if (containsAny(s, "consumidor", "cdc", "produto", "servico")) {
            out.add("Contrato, nota fiscal, prints e protocolos");
            out.add("Comprovação de tentativa de solução administrativa");
        }

        if (out.isEmpty()) {
            out.add("Documentos básicos: RG/CPF, comprovante de residência");
            out.add("Procuração/contrato de honorários (quando aplicável)");
            out.add("Provas documentais do fato (prints, e-mails, boletins)");
        }
        return out;
    }

    private List<String> keywordsFrom(String text) {
        if (text == null) return List.of();
        String norm = normalize(text);
        String[] parts = norm.split("[^\\p{L}\\p{Nd}]+");
        Map<String, Integer> freq = new HashMap<>();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (p.length() <= 2) continue;
            if (STOP.contains(p)) continue;
            freq.merge(p, 1, Integer::sum);
        }
        return freq.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(12)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String summarize(String prompt) {
        String s = prompt.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (s.length() <= 360) return s;
        return s.substring(0, 360) + "...";
    }

    private boolean containsAny(String hay, String... needles) {
        if (hay == null) return false;
        for (String n : needles) {
            if (n != null && !n.isBlank() && hay.contains(n.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private String normalize(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ç", "c");
    }

    private String toJson(Object o) {
        try { return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"json_serialization_failed\"}"; }
    }
}
