package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ExecutionClosureGovernanceResolver {

    public ExecutionClosureGovernanceProfile resolve(Processo processo,
                                                     String modoFechamento,
                                                     String preferencia,
                                                     String subrogacao,
                                                     double percentualSatisfeito,
                                                     double saldoRemanescente,
                                                     String motivo) {
        String closingToken = normalize(modoFechamento);
        String preferenceToken = normalize(preferencia);
        String subrogationToken = normalize(subrogacao);
        String reasonToken = normalize(motivo);
        boolean sigilo = processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean integral = percentualSatisfeito >= 99.999D && saldoRemanescente <= 0.009D;
        boolean residual = saldoRemanescente > 0.009D;
        boolean frustrated = closingToken.contains("FRUSTR") || reasonToken.contains("SEM BENS") || reasonToken.contains("INSUFICIEN");
        boolean archiveReady = integral && !frustrated;

        String closureMode = integral ? "FECHAMENTO_EXECUTIVO_INTEGRAL_COM_BAIXA_PLENA"
                : frustrated ? "FECHAMENTO_EXECUTIVO_FRUSTRADO_COM_RESERVA_DE_REATIVACAO"
                : residual ? "FECHAMENTO_EXECUTIVO_PARCIAL_COM_SALDO_REMANESCENTE"
                : "FECHAMENTO_EXECUTIVO_CONTROLADO_EM_VALIDACAO";
        String closureConsistencyStatus = archiveReady ? "CONSISTENTE_COM_BAIXA_E_ARQUIVO"
                : residual ? "CONSISTENTE_COM_BAIXA_PARCIAL_E_RESERVA_EXECUTIVA"
                : frustrated ? "CONSISTENTE_COM_SUSPENSAO_E_REATIVACAO_POTENCIAL"
                : "REVISAO_COMPLEMENTAR_DE_CONSISTENCIA";
        String archiveReadiness = archiveReady ? "ARQUIVO_ELEGIVEL_APOS_CONFERENCIA_FINAL"
                : residual ? "ARQUIVO_PARCIAL_COM_RESTRICAO_DE_SALDO"
                : frustrated ? "ARQUIVO_CONDICIONADO_A_GUARDA_E_REATIVACAO"
                : "ARQUIVO_AINDA_NAO_ELEGIVEL";
        String residualDispositionMode = residual ? "SALDO_REMANESCENTE_MANTIDO_EM_TRILHA_EXECUTIVA"
                : frustrated ? "SALDO_FRUSTRADO_COM_REATIVACAO_CONDICIONADA"
                : "SALDO_EXTINTO_SEM_REMANESCENTE";
        String preferenceClosureMode = preferenceToken.contains("TRABALH") ? "PREFERENCIA_FINAL_TRABALHISTA_QUITADA_OU_RESERVADA"
                : preferenceToken.contains("FISCAL") ? "PREFERENCIA_FINAL_FISCAL_RESOLVIDA"
                : preferenceToken.contains("HIPOTEC") || preferenceToken.contains("GARANT") ? "PREFERENCIA_GARANTIDA_COM_FECHAMENTO_DE_ONUS"
                : "PREFERENCIA_ORDINARIA_COM_FECHAMENTO_LINEAR";
        String subrogationClosureMode = subrogationToken.contains("SIM") || subrogationToken.contains("SUBROG")
                ? "SUBROGACAO_FINAL_CONFIRMADA_NO_FECHAMENTO"
                : "SUBROGACAO_FINAL_AFASTADA_OU_JA_SANEADA";
        String settlementClosureMode = integral ? "LIQUIDACAO_FINAL_COMPATIVEL_COM_EXTINCAO"
                : residual ? "LIQUIDACAO_FINAL_COMPATIVEL_COM_CONTINUIDADE_RESIDUAL"
                : "LIQUIDACAO_FINAL_DEPENDENTE_DE_REVISAO";
        String closureDesk = residual || frustrated ? "MESA_FECHAMENTO_EXECUTIVO_REVISADO" : "MESA_FECHAMENTO_EXECUTIVO_SUMARIO";
        String queueCode = "EXECUCAO_FECHAMENTO_FINAL" + (sigilo ? "_SIGILO" : "");
        String inboxKey = "inbox.execucao.fechamento.final";
        TipoUsuario assignedRole = archiveReady ? TipoUsuario.SERVIDOR_FORUM : residual ? TipoUsuario.CONTADOR_JUDICIAL : TipoUsuario.JUIZ;
        int priority = Math.min(99, 86 + (residual ? 5 : 0) + (frustrated ? 4 : 0) + (sigilo ? 3 : 0));
        boolean blocking = true;
        long dueAmount = residual || frustrated ? 2L : 1L;
        ChronoUnit dueUnit = ChronoUnit.DAYS;
        String baseLegal = "Arts. 924, 925 e 943 do CPC";
        String terminalDispositionHint = archiveReady ? "BAIXA_TERMINAL_INTEGRAL" : residual ? "BAIXA_PARCIAL_COM_SALDO" : frustrated ? "BAIXA_FRUSTRADA_COM_REATIVACAO" : "BAIXA_CONDICIONADA_A_REVISAO";

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (residual) {
            warnings.add("Fechamento com saldo remanescente exige compatibilizar baixa parcial, preferência e reativação do saldo executivo.");
        }
        if (frustrated) {
            warnings.add("Fechamento frustrado exige trilha formal de guarda, retenção mínima e reativação controlada por localização futura de bens.");
        }
        if (subrogationClosureMode.contains("CONFIRMADA")) {
            warnings.add("Sub-rogação final confirmada exige validar cadeia de ônus, preferência e baixa registral compatível com o produto da expropriação.");
        }
        reviewChecklist.add("Conferir coerência entre homologação, liquidação do produto, satisfação terminal e vínculo de arquivamento.");
        reviewChecklist.add("Validar saldo remanescente, excedente, preferência final, sub-rogação e pista de baixa definitiva ou parcial.");
        reviewChecklist.add("Checar se o fechamento autoriza arquivamento imediato, guarda condicionada ou manutenção da trilha executiva ativa.");
        fundamentos.add(baseLegal);
        fundamentos.add("Fechamento executivo: " + closureMode.replace('_', ' '));
        fundamentos.add("Consistência terminal: " + closureConsistencyStatus.replace('_', ' '));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("percentualSatisfeito", percentualSatisfeito);
        metadata.put("saldoRemanescente", saldoRemanescente);
        metadata.put("archiveReady", archiveReady);
        metadata.put("residual", residual);
        metadata.put("frustrated", frustrated);
        metadata.put("sigilo", sigilo);
        metadata.put("motivo", motivo);
        metadata.put("tribunalCodigo", processo != null ? processo.getTribunalCodigoRoteado() : null);

        return new ExecutionClosureGovernanceProfile(
                closureMode,
                closureConsistencyStatus,
                archiveReadiness,
                residualDispositionMode,
                preferenceClosureMode,
                subrogationClosureMode,
                settlementClosureMode,
                closureDesk,
                queueCode,
                inboxKey,
                assignedRole,
                priority,
                blocking,
                dueAmount,
                dueUnit,
                baseLegal,
                terminalDispositionHint,
                warnings.stream().toList(),
                fundamentos.stream().toList(),
                reviewChecklist.stream().toList(),
                metadata);
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
