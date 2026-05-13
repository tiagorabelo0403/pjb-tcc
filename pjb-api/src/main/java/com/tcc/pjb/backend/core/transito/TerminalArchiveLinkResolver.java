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
public class TerminalArchiveLinkResolver {

    public TerminalArchiveLinkProfile resolve(Processo processo,
                                              String operationType,
                                              String terminalDisposition,
                                              String motivo,
                                              double percentualSatisfeito,
                                              double saldoRemanescente) {
        String op = normalize(operationType);
        String terminal = normalizeTerminalDisposition(terminalDisposition, percentualSatisfeito, saldoRemanescente, motivo);
        boolean archived = processo != null && processo.getStatusProcesso() == StatusProcesso.ARQUIVADO;
        String archiveEligibility = resolveArchiveEligibility(op, terminal, saldoRemanescente, archived);
        String archiveLinkMode = resolveArchiveLinkMode(op, terminal, saldoRemanescente);
        String archiveQueue = resolveArchiveQueue(op, archiveEligibility, terminal);
        String archiveInbox = op.equals("DESARQUIVAR") ? "inbox.execucao.arquivo.reativacao" : archiveEligibility.equals("INELEGIVEL") ? "inbox.execucao.arquivo.revisao" : "inbox.execucao.arquivo.terminal";
        String archiveReviewDesk = archiveEligibility.equals("INELEGIVEL") ? "MESA_REVIEW_BAIXA_E_ARQUIVO" : op.equals("DESARQUIVAR") ? "MESA_REATIVACAO_ACERVO" : "MESA_GUARDA_EXECUTIVA";
        String retentionClass = terminal.contains("FRUSTRADA") || terminal.contains("SUSPENS") ? "RETENCAO_LONGA_COM_REATIVACAO" : terminal.contains("PARCIAL") ? "RETENCAO_INTERMEDIARIA_RESIDUAL" : "RETENCAO_TERMINAL_DEFINITIVA";
        String accessMode = archived || terminal.contains("SIGILO") ? "ACESSO_RESTRITO_AUDITAVEL" : "ACESSO_CONTROLADO_DE_ACERVO";
        String reactivationMode = terminal.contains("FRUSTRADA") || terminal.contains("SUSPENS") ? "REATIVACAO_POR_LOCALIZACAO_DE_BENS" : terminal.contains("PARCIAL") ? "REATIVACAO_POR_SALDO_RESIDUAL" : "REATIVACAO_EXCEPCIONAL";
        TipoUsuario assignedRole = op.equals("DESARQUIVAR") ? TipoUsuario.JUIZ : TipoUsuario.SERVIDOR_FORUM;
        int priority = archiveEligibility.equals("INELEGIVEL") ? 95 : op.equals("DESARQUIVAR") ? 90 : 82;
        boolean blocking = archiveEligibility.equals("INELEGIVEL") || op.equals("DESARQUIVAR");
        long dueAmount = op.equals("DESARQUIVAR") ? 24L : archiveEligibility.equals("ELEGIVEL_COM_RESERVA") ? 3L : 5L;
        ChronoUnit dueUnit = op.equals("DESARQUIVAR") ? ChronoUnit.HOURS : ChronoUnit.DAYS;
        String baseLegal = op.equals("DESARQUIVAR") ? "Arts. 921, 924 e 925 do CPC" : terminal.contains("SUSPENS") ? "Arts. 921, 924 e 925 do CPC" : "Arts. 924, 925 e 1000 do CPC";

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (archiveEligibility.equals("INELEGIVEL")) {
            warnings.add("Baixa e arquivamento ainda não elegíveis para o estado terminal e saldo atualmente persistidos.");
        }
        if (saldoRemanescente > 0D) {
            warnings.add("Há saldo remanescente; conferir se o arquivamento será provisório, parcial ou com reserva de reativação.");
        }
        if (op.equals("DESARQUIVAR") && !archived) {
            warnings.add("Pedido de desarquivamento em processo não marcado como arquivado exige conferência de consistência do estado do acervo.");
        }
        reviewChecklist.add("Conferir correspondência entre satisfação terminal, baixa, retenção documental e possibilidade de reabertura futura.");
        reviewChecklist.add("Validar índices de sigilo, status do acervo, pendências executivas e comunicação final de baixa ou reativação.");
        fundamentos.add(baseLegal);
        fundamentos.add("Disposição terminal: " + terminal.replace('_', ' '));
        fundamentos.add("Modo de vínculo com arquivo: " + archiveLinkMode.replace('_', ' '));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("operationType", op);
        metadata.put("terminalDisposition", terminal);
        metadata.put("archiveEligibility", archiveEligibility);
        metadata.put("archiveLinkMode", archiveLinkMode);
        metadata.put("archiveReviewDesk", archiveReviewDesk);
        metadata.put("retentionClass", retentionClass);
        metadata.put("accessMode", accessMode);
        metadata.put("reactivationMode", reactivationMode);
        metadata.put("archived", archived);
        metadata.put("saldoRemanescente", saldoRemanescente);
        metadata.put("percentualSatisfeito", percentualSatisfeito);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);

        return new TerminalArchiveLinkProfile(
                op,
                terminal,
                archiveEligibility,
                archiveLinkMode,
                archiveQueue,
                archiveInbox,
                archiveReviewDesk,
                retentionClass,
                accessMode,
                reactivationMode,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                warnings.stream().toList(),
                fundamentos.stream().toList(),
                reviewChecklist.stream().toList(),
                metadata);
    }

    private String normalizeTerminalDisposition(String terminalDisposition,
                                                double percentualSatisfeito,
                                                double saldoRemanescente,
                                                String motivo) {
        String token = normalize(terminalDisposition) + ' ' + normalize(motivo);
        if (token.contains("SUSPENS")) {
            return "BAIXA_SUSPENSIVA_SEM_BENS";
        }
        if (token.contains("FRUSTR")) {
            return "BAIXA_FRUSTRADA";
        }
        if (token.contains("ACORDO")) {
            return "EXTINCAO_EXECUTIVA_POR_ACORDO";
        }
        if (token.contains("PAGAMENTO")) {
            return "EXTINCAO_EXECUTIVA_POR_PAGAMENTO";
        }
        if (token.contains("PARCIAL") || saldoRemanescente > 0D || percentualSatisfeito < 100D) {
            return "BAIXA_PARCIAL_COM_SALDO";
        }
        return token.isBlank() ? "BAIXA_TERMINAL_INTEGRAL" : token.replace(' ', '_');
    }

    private String resolveArchiveEligibility(String op, String terminal, double saldoRemanescente, boolean archived) {
        if (op.equals("DESARQUIVAR")) {
            return archived ? "ELEGIVEL" : "ELEGIVEL_COM_RESERVA";
        }
        if (terminal.contains("PARCIAL") && saldoRemanescente > 0D) {
            return "ELEGIVEL_COM_RESERVA";
        }
        if (terminal.contains("FRUSTRADA") || terminal.contains("SUSPENS")) {
            return "ELEGIVEL_COM_RESERVA";
        }
        if (terminal.contains("TERMINAL") || terminal.contains("EXTINCAO")) {
            return "ELEGIVEL";
        }
        return "INELEGIVEL";
    }

    private String resolveArchiveLinkMode(String op, String terminal, double saldoRemanescente) {
        if (op.equals("DESARQUIVAR")) {
            return "REATIVACAO_CONTROLADA_DO_ACERVO_EXECUTIVO";
        }
        if (terminal.contains("PARCIAL") || saldoRemanescente > 0D) {
            return "ARQUIVAMENTO_COM_RESERVA_DE_SALDO_E_REATIVACAO";
        }
        if (terminal.contains("FRUSTRADA") || terminal.contains("SUSPENS")) {
            return "ARQUIVAMENTO_SUSPENSIVO_COM_BUSCA_FUTURA_DE_BENS";
        }
        return "ARQUIVAMENTO_TERMINAL_DEFINITIVO";
    }

    private String resolveArchiveQueue(String op, String eligibility, String terminal) {
        if (op.equals("DESARQUIVAR")) {
            return "DESARQUIVAMENTO_EXECUTIVO_CONTROLADO";
        }
        if (eligibility.equals("INELEGIVEL")) {
            return "ARQUIVAMENTO_EXECUTIVO_REVIEW";
        }
        if (terminal.contains("PARCIAL") || terminal.contains("SUSPENS") || terminal.contains("FRUSTRADA")) {
            return "ARQUIVAMENTO_EXECUTIVO_COM_RESERVA";
        }
        return "ARQUIVAMENTO_EXECUTIVO_TERMINAL";
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
