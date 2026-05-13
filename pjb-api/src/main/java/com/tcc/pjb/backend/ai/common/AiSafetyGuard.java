package com.tcc.pjb.backend.ai.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AiSafetyGuard {

    public record GuardResult(boolean permitido, List<String> alertas, List<String> bloqueios) {
        public boolean bloqueado() {
            return !bloqueios.isEmpty();
        }
    }

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\u0000-\\u001F\\u007F]", Pattern.MULTILINE);

    
    private static final Pattern INJECTION = Pattern.compile(
            "(?i)(ignore|disregard|bypass)\\s+(all\\s+)?(previous|prior|system)\\s+(instructions|rules)|" +
            "(?i)system\\s+prompt|developer\\s+message|reveal\\s+your\\s+instructions|" +
            "(?i)jailbreak|dan\\b|prompt\\s+injection"
    );

    
    private static final Pattern PII = Pattern.compile(
            "(?i)senha|password|cpf|rg|cart[aã]o\\s+de\\s+cr[eé]dito|cvv|token|chave\\s+pix"
    );

    
    public String sanitizar(String texto) {
        if (texto == null) return "";
        String s = CONTROL_CHARS.matcher(texto).replaceAll(" ");
        s = s.replace('\u00A0', ' ');
        return s.trim();
    }

    
    public GuardResult avaliarEntrada(String entrada) {
        String s = sanitizar(entrada);
        List<String> alertas = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();

        String lower = s.toLowerCase(Locale.ROOT);

        if (INJECTION.matcher(lower).find()) {
            alertas.add("Possível prompt injection detectado.");
        }
        if (PII.matcher(lower).find()) {
            alertas.add("Entrada pode conter pedido/menção a dados sensíveis (PII/credenciais).");
        }
        
        if (lower.contains("reveal") && lower.contains("instructions")) {
            bloqueios.add("Solicitação de divulgação de instruções internas.");
        }

        return new GuardResult(bloqueios.isEmpty(), List.copyOf(alertas), List.copyOf(bloqueios));
    }

    
    public GuardResult avaliarSaida(String saida) {
        String s = sanitizar(saida);
        List<String> alertas = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();

        String lower = s.toLowerCase(Locale.ROOT);
        if (INJECTION.matcher(lower).find()) {
            alertas.add("Saída contém padrões suspeitos de injection/jailbreak.");
        }
        if (lower.contains("system prompt") || lower.contains("developer message")) {
            bloqueios.add("Saída aparenta conter instruções internas.");
        }
        return new GuardResult(bloqueios.isEmpty(), List.copyOf(alertas), List.copyOf(bloqueios));
    }
}
