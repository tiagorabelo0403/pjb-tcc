package com.tcc.pjb.backend.service.secretariat.query.operational;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SecretariatOperationalTransactionModelService {

    public OperationalTransactionCatalogView catalog() {
        return new OperationalTransactionCatalogView(List.of(
                row("FIRST_INSTANCE_SECRETARIAT", "Transações operacionais da secretaria de 1º grau", List.of(
                        tx("VISUALIZAR_SECAO_CREDENCIAL_FUNCIONAL", "Abre a seção da credencial funcional da secretaria", "GET", OperationalApiRoutes.SECRETARIAT_CREDENTIAL_SECURITY_BASE, "SEM_PAYLOAD", "USUARIO", "CREDENCIAL_FUNCIONAL_VISUALIZADA"),
                        tx("EMITIR_OTP_CREDENCIAL_FUNCIONAL", "Emite OTP institucional para criar ou redefinir a senha funcional da secretaria", "POST", OperationalApiRoutes.SECRETARIAT_CREDENTIAL_SECURITY_BASE + OperationalApiRoutes.PATH_SECRETARIAT_CREDENTIAL_CHALLENGE, "SEM_PAYLOAD", "USUARIO", "OTP_CREDENCIAL_EMITIDO"),
                        tx("DEFINIR_SENHA_CREDENCIAL_FUNCIONAL", "Cria ou redefine a senha funcional da secretaria", "POST", OperationalApiRoutes.SECRETARIAT_CREDENTIAL_SECURITY_BASE + OperationalApiRoutes.PATH_SECRETARIAT_CREDENTIAL_PASSWORD, "OperationalCredentialPasswordSetRequest", "USUARIO", "SENHA_FUNCIONAL_DEFINIDA"),
                        tx("DESBLOQUEAR_ATO_PROCESSUAL", "Gera token efêmero da credencial funcional para ato sensível da secretaria", "POST", OperationalApiRoutes.SECRETARIAT_CREDENTIAL_SECURITY_BASE + OperationalApiRoutes.PATH_SECRETARIAT_CREDENTIAL_UNLOCK, "OperationalCredentialUnlockRequest", "USUARIO", "TOKEN_EFEMERO_EMITIDO"),
                        tx("REALIZAR_JUNTADA", "Realiza juntada operacional no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_PROCESS_JUNTADA, "SecretariaJuntadaRequest", "PROCESSO", "JUNTADA_MATERIALIZADA"),
                        tx("EXPEDIR_INTIMACAO", "Expede intimação operacional no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_PROCESS_INTIMACAO, "SecretariaIntimacaoRequest", "PROCESSO", "INTIMACAO_EXPEDIDA"),
                        tx("CONCLUSAO_PARA_DESPACHO", "Encaminha processo para conclusão", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_PROCESS_CONCLUSAO, "SecretariaConclusaoRequest", "PROCESSO", "CONCLUSAO_REGISTRADA"),
                        tx("SANEAMENTO_FILA", "Executa saneamento em lote da fila", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_QUEUE_SANEAMENTO, "query(queueCode,limite)", "FILA", "FILA_SANEADA"),
                        tx("CONFIRMAR_LOCAL_AGENDA", "Confirma sala, local ou link do item operacional", "POST", OperationalApiRoutes.SECRETARIAT_BASE + OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_VENUE_CONFIRMATION, "SecretariatQueueVenueConfirmationRequest", "ITEM_FILA", "LOCAL_CONFIRMADO"),
                        tx("SOLICITAR_STEP_UP_INTIMACAO", "Emite OTP institucional de dois fatores para assinatura da confirmação de intimação", "POST", OperationalApiRoutes.SECRETARIAT_BASE + OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_PARTICIPANT_NOTIFICATION_CHALLENGE, "SEM_PAYLOAD", "ITEM_FILA", "OTP_EMITIDO"),
                        tx("CONFIRMAR_INTIMACAO_PARTICIPANTES", "Confirma a intimação dos participantes do item operacional", "POST", OperationalApiRoutes.SECRETARIAT_BASE + OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_PARTICIPANT_NOTIFICATION, "SecretariatQueueParticipantNotificationRequest", "ITEM_FILA", "PARTICIPANTES_INTIMADOS"),
                        tx("REGISTRAR_COMPARECIMENTO", "Registra presença ou ausência vinculada ao item operacional", "POST", OperationalApiRoutes.SECRETARIAT_BASE + OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_ATTENDANCE, "SecretariatQueueAttendanceRequest", "ITEM_FILA", "COMPARECIMENTO_REGISTRADO"),
                        tx("REGISTRAR_EVENTO_REAL", "Registra evento real de conclusão operacional", "POST", OperationalApiRoutes.SECRETARIAT_BASE + OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_COMPLETION_EVENT, "SecretariatQueueCompletionEventRequest", "ITEM_FILA", "EVENTO_REAL_REGISTRADO"),
                        tx("EXECUTAR_RETORNO_PROCESSO", "Executa retorno automático auditável ao processo", "POST", OperationalApiRoutes.SECRETARIAT_BASE + OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_PROCESS_RETURN, "SecretariatQueueProcessReturnRequest", "ITEM_FILA", "RETORNO_EXECUTADO")
                )),
                row("TRIBUNAL_COLLEGIATE_SECRETARIAT", "Transações operacionais da secretaria colegiada do PJB", List.of(
                        tx("INCLUIR_EM_PAUTA", "Inclui processo em pauta colegiada", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_PAUTA, "SecretariaPautaColegiadaRequest", "PROCESSO", "PAUTA_REGISTRADA"),
                        tx("PUBLICAR_PAUTA", "Publica pauta colegiada", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_PUBLICATION, "SecretariaPublicacaoPautaRequest", "JULGAMENTO", "PAUTA_PUBLICADA"),
                        tx("REGISTRAR_SUSTENTACAO_ORAL", "Registra sustentação oral colegiada", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_SUSTENTACAO, "SecretariaSustentacaoOralRequest", "JULGAMENTO", "SUSTENTACAO_REGISTRADA"),
                        tx("PUBLICAR_ACORDAO", "Publica acórdão colegiado", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_ACORDAO, "SecretariaPublicacaoAcordaoRequest", "JULGAMENTO", "ACORDAO_PUBLICADO"),
                        tx("BAIXAR_ORIGEM", "Registra baixa e retorno à origem", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_BAIXA, "SecretariaBaixaOrigemRequest", "JULGAMENTO", "BAIXA_REGISTRADA")
                )),
                row("ELECTORAL_JUDICIAL_SECRETARIAT", "Transações operacionais da secretaria eleitoral do PJB", List.of(
                        tx("INCLUIR_EM_PAUTA", "Inclui feito colegiado eleitoral em pauta no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_PAUTA, "SecretariaPautaColegiadaRequest", "PROCESSO", "PAUTA_REGISTRADA"),
                        tx("PUBLICAR_PAUTA", "Publica pauta eleitoral no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_PUBLICATION, "SecretariaPublicacaoPautaRequest", "JULGAMENTO", "PAUTA_PUBLICADA"),
                        tx("INSTAURAR_CORREGEDORIA_ELEITORAL", "Instaura procedimento de corregedoria eleitoral no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_CORREGEDORIA, "SecretariaCorregedoriaEleitoralRequest", "PROCESSO", "CORREGEDORIA_INSTAURADA"),
                        tx("REGISTRAR_INSPECAO_CORREGEDORIA", "Registra inspeção de corregedoria eleitoral no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_INSPECAO, "SecretariaInspecaoCorregedoriaRequest", "PROCESSO", "INSPECAO_REGISTRADA"),
                        tx("VALIDAR_PESQUISA_ELEITORAL", "Valida pesquisa eleitoral no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_PESQUISA, "SecretariaPesquisaEleitoralRequest", "PROCESSO", "PESQUISA_PROCESSADA")
                )),
                row("LABOUR_JUDICIAL_SECRETARIAT", "Transações operacionais da secretaria trabalhista do PJB", List.of(
                        tx("EXPEDIR_INTIMACAO", "Expede intimação no PJB trabalhista", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_PROCESS_INTIMACAO, "SecretariaIntimacaoRequest", "PROCESSO", "INTIMACAO_EXPEDIDA"),
                        tx("PUBLICAR_ACORDAO", "Publica acórdão trabalhista colegiado", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_ACORDAO, "SecretariaPublicacaoAcordaoRequest", "JULGAMENTO", "ACORDAO_PUBLICADO"),
                        tx("RECEBER_MIDIA_PROCESSUAL", "Recebe mídia processual trabalhista no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_LABOUR_MIDIA_RECEBIMENTO, "SecretariaMidiaProcessualRequest", "PROCESSO", "MIDIA_RECEBIDA"),
                        tx("DISPONIBILIZAR_MIDIA_PROCESSUAL", "Disponibiliza mídia processual trabalhista no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_LABOUR_MIDIA_DISPONIBILIZACAO, "SecretariaMidiaProcessualRequest", "PROCESSO", "MIDIA_DISPONIBILIZADA"),
                        tx("IMPULSIONAR_EXECUCAO_TRABALHISTA", "Impulsiona execução trabalhista no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_LABOUR_EXECUCAO, "SecretariaExecucaoTrabalhistaRequest", "PROCESSO", "EXECUCAO_IMPULSIONADA")
                )),
                row("MILITARY_JUDICIAL_SECRETARIAT", "Transações operacionais da secretaria militar do PJB", List.of(
                        tx("PUBLICAR_PAUTA", "Publica pauta militar colegiada", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_PUBLICATION, "SecretariaPublicacaoPautaRequest", "JULGAMENTO", "PAUTA_PUBLICADA"),
                        tx("REGISTRAR_SUSTENTACAO_ORAL", "Registra sustentação oral no PJB militar", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_SUSTENTACAO, "SecretariaSustentacaoOralRequest", "JULGAMENTO", "SUSTENTACAO_REGISTRADA"),
                        tx("BAIXAR_ORIGEM", "Registra baixa de feito militar no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_BAIXA, "SecretariaBaixaOrigemRequest", "JULGAMENTO", "BAIXA_REGISTRADA"),
                        tx("RECEBER_PLANTAO_MILITAR", "Recebe urgência de plantão militar no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_MILITARY_PLANTAO, "SecretariaPlantaoMilitarRequest", "PROCESSO", "PLANTAO_ATIVADO"),
                        tx("REGISTRAR_BALCAO_VIRTUAL_MILITAR", "Registra atendimento de balcão virtual militar no PJB", "POST", OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE + OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_MILITARY_BALCAO, "SecretariaBalcaoVirtualMilitarRequest", "PROCESSO", "ATENDIMENTO_VIRTUAL_REGISTRADO")
                ))
        ));
    }

    public OperationalTransactionSnapshot resolve(String journeyMode) {
        String normalized = normalize(journeyMode);
        OperationalTransactionCatalogRow row = catalog().rows().stream()
                .filter(candidate -> normalize(candidate.journeyMode()).equals(normalized))
                .findFirst()
                .orElseGet(() -> catalog().rows().stream().filter(candidate -> normalize(candidate.journeyMode()).equals("FIRST_INSTANCE_SECRETARIAT")).findFirst().orElseThrow());
        long collegiateTransactions = row.transactions().stream().filter(transaction -> "JULGAMENTO".equals(transaction.scope())).count();
        long specializedTransactions = row.transactions().stream().filter(transaction -> "PROCESSO".equals(transaction.scope()) && !transaction.actionCode().startsWith("REALIZAR_") && !transaction.actionCode().startsWith("EXPEDIR_") && !transaction.actionCode().startsWith("CONCLUSAO_") && !transaction.actionCode().startsWith("SANEAMENTO")).count();
        return new OperationalTransactionSnapshot(
                row.journeyMode(),
                row.transactions(),
                List.of("ENDPOINTS_PJB_ATIVOS", collegiateTransactions > 0 ? "TRANSACOES_COLEGIADAS" : "TRANSACOES_1G", specializedTransactions > 0 ? "TRANSACOES_ESPECIALIZADAS" : "TRANSACOES_BASE"),
                new OperationalTransactionDiagnostics(
                        row.transactions().size(),
                        collegiateTransactions,
                        specializedTransactions,
                        row.transactions().stream().map(OperationalTransactionView::method).distinct().toList(),
                        row.transactions().stream().map(OperationalTransactionView::scope).distinct().toList()
                )
        );
    }

    private OperationalTransactionCatalogRow row(String journeyMode, String descriptor, List<OperationalTransactionView> transactions) {
        return new OperationalTransactionCatalogRow(journeyMode, descriptor, List.copyOf(transactions));
    }

    private OperationalTransactionView tx(String actionCode,
                                          String descriptor,
                                          String method,
                                          String route,
                                          String payloadType,
                                          String scope,
                                          String outcome) {
        return new OperationalTransactionView(actionCode, descriptor, method, route, payloadType, scope, outcome);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "FIRST_INSTANCE_SECRETARIAT";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public record OperationalTransactionCatalogView(List<OperationalTransactionCatalogRow> rows) {
        public OperationalTransactionCatalogView {
            rows = List.copyOf(Objects.requireNonNull(rows));
        }
    }

    public record OperationalTransactionCatalogRow(String journeyMode,
                                                   String descriptor,
                                                   List<OperationalTransactionView> transactions) {
    }

    public record OperationalTransactionView(String actionCode,
                                             String descriptor,
                                             String method,
                                             String route,
                                             String payloadType,
                                             String scope,
                                             String outcome) {
    }

    public record OperationalTransactionSnapshot(String journeyMode,
                                                 List<OperationalTransactionView> transactions,
                                                 List<String> labels,
                                                 OperationalTransactionDiagnostics diagnostics) {
    }

    public record OperationalTransactionDiagnostics(long transactionCount,
                                                    long collegiateTransactionCount,
                                                    long specializedTransactionCount,
                                                    List<String> methods,
                                                    List<String> scopes) {
    }
}
