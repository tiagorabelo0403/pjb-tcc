package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ExecutionSatisfactionResolver {

    public ExecutionSatisfactionProfile resolve(Processo processo,
                                                String modo,
                                                double percentualSatisfeito,
                                                double saldoRemanescente,
                                                String fundamento) {
        String normalizedMode = resolveMode(modo, percentualSatisfeito, saldoRemanescente, fundamento);
        boolean archived = processo != null && processo.getStatusProcesso() == StatusProcesso.ARQUIVADO;
        boolean integral = normalizedMode.equals("SATISFACAO_INTEGRAL") || normalizedMode.equals("EXTINCAO_POR_PAGAMENTO") || normalizedMode.equals("EXTINCAO_POR_ACORDO_CUMPRIDO");
        boolean frustrada = normalizedMode.equals("SATISFACAO_FRUSTRADA") || normalizedMode.equals("SUSPENSAO_SEM_BENS");
        String closureMode = resolveClosureMode(normalizedMode, integral, frustrada);
        String satisfactionMode = resolveSatisfactionMode(normalizedMode, percentualSatisfeito, saldoRemanescente);
        String terminalDisposition = resolveTerminalDisposition(normalizedMode, integral, frustrada);
        String residualMode = saldoRemanescente > 0D ? "SALDO_REMANESCENTE_ATIVO" : "SEM_SALDO_REMANESCENTE";
        String retentionMode = integral ? "RETENCAO_TERMINAL_CONTROLADA" : frustrada ? "RETENCAO_PARA_REATIVACAO_FUTURA" : "RETENCAO_INTERMEDIARIA";
        String reopenMode = frustrada ? "REABERTURA_POR_LOCALIZACAO_DE_BENS" : integral ? "REABERTURA_EXCEPCIONAL" : "REABERTURA_CONDICIONADA";
        String queueCode = resolveQueueCode(terminalDisposition, frustrada, archived);
        String inboxKey = resolveInboxKey(terminalDisposition, frustrada);
        TipoUsuario assignedRole = integral ? TipoUsuario.JUIZ : TipoUsuario.SERVIDOR_FORUM;
        int priority = integral ? 94 : frustrada ? 81 : 88;
        boolean blocking = integral || frustrada;
        long dueAmount = integral ? 2L : frustrada ? 5L : 3L;
        ChronoUnit dueUnit = ChronoUnit.DAYS;
        String baseLegal = resolveBaseLegal(normalizedMode, integral, frustrada);
        String saldoMode = saldoRemanescente > 0D ? "BAIXA_COM_SALDO" : "BAIXA_SEM_SALDO";
        String baixaMode = resolveBaixaMode(terminalDisposition, integral, frustrada);

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (archived) {
            warnings.add("Processo arquivado exige revalidação do ciclo terminal antes de consolidar satisfação ou extinção executiva.");
        }
        if (saldoRemanescente > 0D) {
            warnings.add("Há saldo remanescente; avaliar baixa parcial, suspensão ou manutenção do ciclo executivo residual.");
        }
        if (percentualSatisfeito < 100D && integral) {
            warnings.add("Modo terminal integral informado com percentual inferior ao total; conferir coerência do fechamento executivo.");
        }
        reviewChecklist.add("Conferir pagamentos, levantamentos, custas finais, honorários, constrições remanescentes e saldo residual.");
        reviewChecklist.add("Validar fundamento terminal, retenção documental, hipótese de reabertura e comunicação às partes.");
        if (frustrada) {
            reviewChecklist.add("Marcar bens não localizados, registrar diligências negativas e parametrizar eventual reativação futura.");
        }
        fundamentos.add(baseLegal);
        fundamentos.add("Disposição terminal: " + terminalDisposition.replace('_', ' '));
        fundamentos.add("Modo de satisfação: " + satisfactionMode.replace('_', ' '));
        if (fundamento != null && !fundamento.isBlank()) {
            fundamentos.add(fundamento.trim());
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("closureMode", closureMode);
        metadata.put("satisfactionMode", satisfactionMode);
        metadata.put("terminalDisposition", terminalDisposition);
        metadata.put("residualMode", residualMode);
        metadata.put("retentionMode", retentionMode);
        metadata.put("reopenMode", reopenMode);
        metadata.put("saldoMode", saldoMode);
        metadata.put("baixaMode", baixaMode);
        metadata.put("percentualSatisfeito", percentualSatisfeito);
        metadata.put("saldoRemanescente", saldoRemanescente);
        metadata.put("archived", archived);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);

        return new ExecutionSatisfactionProfile(
                closureMode,
                satisfactionMode,
                terminalDisposition,
                residualMode,
                retentionMode,
                reopenMode,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                saldoMode,
                baixaMode,
                warnings.stream().toList(),
                fundamentos.stream().toList(),
                reviewChecklist.stream().toList(),
                metadata);
    }

    private String resolveMode(String modo, double percentualSatisfeito, double saldoRemanescente, String fundamento) {
        String token = normalize(modo) + ' ' + normalize(fundamento);
        if (token.contains("ACORDO")) {
            return "EXTINCAO_POR_ACORDO_CUMPRIDO";
        }
        if (token.contains("PERDA") || token.contains("SUPERVENIENTE")) {
            return "EXTINCAO_POR_PERDA_SUPERVENIENTE";
        }
        if (token.contains("PAGAMENTO") || token.contains("QUITACAO")) {
            return "EXTINCAO_POR_PAGAMENTO";
        }
        if (token.contains("FRUSTRADA") || token.contains("FRUSTRADO")) {
            return "SATISFACAO_FRUSTRADA";
        }
        if (token.contains("SUSPENSAO") || token.contains("SEM BENS") || token.contains("SEM_BENS")) {
            return "SUSPENSAO_SEM_BENS";
        }
        if (percentualSatisfeito >= 100D && saldoRemanescente <= 0D) {
            return "SATISFACAO_INTEGRAL";
        }
        if (percentualSatisfeito > 0D || saldoRemanescente > 0D) {
            return "SATISFACAO_PARCIAL";
        }
        return "SATISFACAO_FRUSTRADA";
    }

    private String resolveClosureMode(String normalizedMode, boolean integral, boolean frustrada) {
        if (integral) {
            return "FECHAMENTO_TERMINAL_INTEGRAL";
        }
        if (frustrada) {
            return normalizedMode.equals("SUSPENSAO_SEM_BENS") ? "FECHAMENTO_SUSPENSIVO" : "FECHAMENTO_FRUSTRADO";
        }
        return "FECHAMENTO_PARCIAL_COM_RESERVA";
    }

    private String resolveSatisfactionMode(String normalizedMode, double percentualSatisfeito, double saldoRemanescente) {
        if (normalizedMode.equals("SUSPENSAO_SEM_BENS") || normalizedMode.equals("SATISFACAO_FRUSTRADA")) {
            return "SATISFACAO_INSUFICIENTE";
        }
        if (normalizedMode.equals("SATISFACAO_PARCIAL") || saldoRemanescente > 0D || percentualSatisfeito < 100D) {
            return "SATISFACAO_PARCIAL";
        }
        return "SATISFACAO_TOTAL";
    }

    private String resolveTerminalDisposition(String normalizedMode, boolean integral, boolean frustrada) {
        if (normalizedMode.equals("EXTINCAO_POR_PAGAMENTO")) {
            return "EXTINCAO_EXECUTIVA_POR_PAGAMENTO";
        }
        if (normalizedMode.equals("EXTINCAO_POR_ACORDO_CUMPRIDO")) {
            return "EXTINCAO_EXECUTIVA_POR_ACORDO";
        }
        if (normalizedMode.equals("EXTINCAO_POR_PERDA_SUPERVENIENTE")) {
            return "EXTINCAO_EXECUTIVA_POR_PERDA_SUPERVENIENTE";
        }
        if (frustrada) {
            return normalizedMode.equals("SUSPENSAO_SEM_BENS") ? "BAIXA_SUSPENSIVA_SEM_BENS" : "BAIXA_FRUSTRADA";
        }
        if (integral) {
            return "BAIXA_TERMINAL_INTEGRAL";
        }
        return "BAIXA_PARCIAL_COM_SALDO";
    }

    private String resolveQueueCode(String terminalDisposition, boolean frustrada, boolean archived) {
        String suffix = frustrada ? "FRUSTRADA" : terminalDisposition.contains("PARCIAL") ? "PARCIAL" : "TERMINAL";
        return "SATISFACAO_EXECUTIVA_" + suffix + (archived ? "_REVIEW" : "");
    }

    private String resolveInboxKey(String terminalDisposition, boolean frustrada) {
        if (frustrada) {
            return "inbox.execucao.satisfacao.frustrada";
        }
        if (terminalDisposition.contains("PARCIAL")) {
            return "inbox.execucao.satisfacao.parcial";
        }
        return "inbox.execucao.satisfacao.terminal";
    }

    private String resolveBaseLegal(String normalizedMode, boolean integral, boolean frustrada) {
        if (integral) {
            return "Arts. 924, 925 e 526 do CPC";
        }
        if (frustrada) {
            return normalizedMode.equals("SUSPENSAO_SEM_BENS") ? "Arts. 921 e 924 do CPC" : "Arts. 797, 921 e 924 do CPC";
        }
        return "Arts. 523, 524, 831 e 924 do CPC";
    }

    private String resolveBaixaMode(String terminalDisposition, boolean integral, boolean frustrada) {
        if (integral) {
            return "BAIXA_COM_EXTINCAO_E_LIBERACAO_TOTAL";
        }
        if (frustrada) {
            return "BAIXA_COM_RESERVA_DE_REATIVACAO";
        }
        if (terminalDisposition.contains("PARCIAL")) {
            return "BAIXA_PARCIAL_COM_PENDENCIA_RESIDUAL";
        }
        return "BAIXA_TERMINAL_CONTROLADA";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }
}
