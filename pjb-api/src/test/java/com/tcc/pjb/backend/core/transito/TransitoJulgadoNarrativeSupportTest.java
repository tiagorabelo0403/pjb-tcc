package com.tcc.pjb.backend.core.transito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransitoJulgadoNarrativeSupportTest {

    private TransitoJulgadoNarrativeSupport support() {
        return new TransitoJulgadoNarrativeSupport(
                mock(ExecutionIncidentResolver.class),
                mock(ExecutionEnforcementResolver.class),
                mock(PatrimonialConstrictionResolver.class),
                mock(ExternalConstrictionResolver.class),
                mock(ExpropriationGovernanceResolver.class),
                mock(ExpropriationAuctionCycleResolver.class),
                mock(ExternalConstrictionContingencyResolver.class),
                mock(ExternalConstrictionReconciliationResolver.class),
                mock(ExpropriationHomologationResolver.class),
                mock(ExpropriationSettlementResolver.class),
                mock(ExecutionClosureGovernanceResolver.class),
                mock(ExecutionSatisfactionResolver.class),
                mock(TerminalArchiveLinkResolver.class)
        );
    }

    @Test
    void deveResolverTipoDeWorkItemParaExpropriacao() {
        ExpropriationGovernanceProfile profile = new ExpropriationGovernanceProfile(
                "hasta publica",
                "imovel",
                "eletronica",
                "sessao",
                "edital",
                "depositario",
                "leiloeiro",
                "preco",
                "mesa",
                "fraude",
                "fila",
                "inbox",
                TipoUsuario.LEILOEIRO_JUDICIAL,
                10,
                true,
                2,
                ChronoUnit.DAYS,
                "base legal",
                "liquidacao",
                "entrega",
                List.of(),
                List.of(),
                List.of(),
                new LinkedHashMap<>()
        );

        assertThat(support().resolveWorkItemTypeForExpropriation(profile)).isEqualTo(WorkItemType.EXPEDICAO);
    }

    @Test
    void deveMontarDescricaoDeAtoExecutivoComValor() {
        PostJudgmentOperationalProfile operational = new PostJudgmentOperationalProfile(
                "cumprimento",
                "fila",
                "inbox",
                TipoUsuario.SERVIDOR_FORUM,
                10,
                true,
                2,
                ChronoUnit.DAYS,
                "base legal",
                "coisa julgada",
                "trilha",
                "arquivo",
                "mesa",
                "retencao",
                "satisfacao",
                List.of(),
                List.of(),
                List.of(),
                new LinkedHashMap<>()
        );
        ExecutionEnforcementProfile enforcement = new ExecutionEnforcementProfile(
                "PENHORA",
                "modo",
                "OBRIGACAO_PAGAR",
                "constricao",
                "expropriacao",
                "satisfacao",
                "fila",
                "inbox",
                TipoUsuario.SERVIDOR_FORUM,
                10,
                true,
                2,
                ChronoUnit.DAYS,
                "base legal",
                "prova",
                "mesa",
                "impacto",
                "ledger",
                List.of(),
                List.of(),
                List.of(),
                new LinkedHashMap<>()
        );

        String description = support().buildEnforcementDescription("detalhe", operational, enforcement, 1200D);

        assertThat(description).contains("detalhe", "R$ 1200.00", "cumprimento", "PENHORA");
    }

    @Test
    void deveResolverFaseDeAtoExecutivoPenhora() {
        Processo processo = Processo.builder().numeroProcesso("0001").faseAtual(FaseProcessual.EXECUCAO).build();
        ExecutionEnforcementProfile enforcement = new ExecutionEnforcementProfile(
                "PENHORA",
                "modo",
                "OBRIGACAO_PAGAR",
                "constricao",
                "expropriacao",
                "satisfacao",
                "fila",
                "inbox",
                TipoUsuario.SERVIDOR_FORUM,
                10,
                true,
                2,
                ChronoUnit.DAYS,
                "base legal",
                "prova",
                "mesa",
                "impacto",
                "ledger",
                List.of(),
                List.of(),
                List.of(),
                new LinkedHashMap<>()
        );

        assertThat(support().resolveExecutionPhaseForAct(processo, enforcement)).isEqualTo(FaseProcessual.PENHORA);
    }
}
