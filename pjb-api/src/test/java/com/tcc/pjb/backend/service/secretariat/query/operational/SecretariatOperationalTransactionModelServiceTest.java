package com.tcc.pjb.backend.service.secretariat.query.operational;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretariatOperationalTransactionModelServiceTest {

    private final SecretariatOperationalTransactionModelService service = new SecretariatOperationalTransactionModelService();

    @Test
    void shouldExposeCollegiateTransactionsWithPjbRoutes() {
        SecretariatOperationalTransactionModelService.OperationalTransactionSnapshot snapshot = service.resolve("TRIBUNAL_COLLEGIATE_SECRETARIAT");

        assertThat(snapshot.transactions())
                .extracting(SecretariatOperationalTransactionModelService.OperationalTransactionView::actionCode)
                .contains("INCLUIR_EM_PAUTA", "PUBLICAR_PAUTA", "REGISTRAR_SUSTENTACAO_ORAL", "PUBLICAR_ACORDAO", "BAIXAR_ORIGEM");
        assertThat(snapshot.transactions())
                .extracting(SecretariatOperationalTransactionModelService.OperationalTransactionView::route)
                .allMatch(route -> route.startsWith("/api/v1/secretariat/operacional/"));
        assertThat(snapshot.diagnostics().collegiateTransactionCount()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void shouldExposeSpecializedTransactionsForElectoralLabourAndMilitarySecretariats() {
        SecretariatOperationalTransactionModelService.OperationalTransactionSnapshot electoral = service.resolve("ELECTORAL_JUDICIAL_SECRETARIAT");
        SecretariatOperationalTransactionModelService.OperationalTransactionSnapshot labour = service.resolve("LABOUR_JUDICIAL_SECRETARIAT");
        SecretariatOperationalTransactionModelService.OperationalTransactionSnapshot military = service.resolve("MILITARY_JUDICIAL_SECRETARIAT");

        assertThat(electoral.transactions())
                .extracting(SecretariatOperationalTransactionModelService.OperationalTransactionView::actionCode)
                .contains("INSTAURAR_CORREGEDORIA_ELEITORAL", "REGISTRAR_INSPECAO_CORREGEDORIA", "VALIDAR_PESQUISA_ELEITORAL");
        assertThat(labour.transactions())
                .extracting(SecretariatOperationalTransactionModelService.OperationalTransactionView::actionCode)
                .contains("RECEBER_MIDIA_PROCESSUAL", "DISPONIBILIZAR_MIDIA_PROCESSUAL", "IMPULSIONAR_EXECUCAO_TRABALHISTA");
        assertThat(military.transactions())
                .extracting(SecretariatOperationalTransactionModelService.OperationalTransactionView::actionCode)
                .contains("RECEBER_PLANTAO_MILITAR", "REGISTRAR_BALCAO_VIRTUAL_MILITAR");
        assertThat(electoral.diagnostics().specializedTransactionCount()).isGreaterThan(0);
        assertThat(labour.diagnostics().specializedTransactionCount()).isGreaterThan(0);
        assertThat(military.diagnostics().specializedTransactionCount()).isGreaterThan(0);
    }

    @Test
    void shouldFallbackToFirstInstanceJourneyModeWhenUnknown() {
        SecretariatOperationalTransactionModelService.OperationalTransactionSnapshot snapshot = service.resolve("DESCONHECIDO");

        assertThat(snapshot.journeyMode()).isEqualTo("FIRST_INSTANCE_SECRETARIAT");
        assertThat(snapshot.transactions())
                .extracting(SecretariatOperationalTransactionModelService.OperationalTransactionView::actionCode)
                .contains("REALIZAR_JUNTADA", "EXPEDIR_INTIMACAO", "CONCLUSAO_PARA_DESPACHO");
    }
}
