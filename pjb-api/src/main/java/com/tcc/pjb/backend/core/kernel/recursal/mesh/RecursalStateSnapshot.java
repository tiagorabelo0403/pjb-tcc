package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.time.Instant;
import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;

public record RecursalStateSnapshot(
        String recursoId,
        RecursalLifecycleState state,
        int revision,
        RecursalTribunal tribunalAtual,
        RecursalTribunalDetalhado tribunalDetalhadoAtual,
        InstanceLevel instanciaAtual,
        RecursalAuthority autoridadeAtual,
        boolean preparoSatisfeito,
        boolean admissibilidadePositiva,
        boolean remetido,
        boolean autuadoDestino,
        boolean distribuidoDestino,
        boolean preparoEmComplementacao,
        boolean diligenciaPendente,
        boolean multaEmbargosProtelatoriosAplicada,
        boolean sobrestadoPorPrecedente,
        boolean efeitoSuspensivoAtivo,
        boolean efeitoAtivoConcedido,
        boolean conhecimentoParcial,
        int iteracoesEmbargosDeclaracao,
        boolean remessaNecessaria,
        boolean requisicaoPublicaPagamento,
        RecursalRemessaTrace remessaTrace,
        RecursalSustentacaoOralTrace sustentacaoOralTrace,
        RecursalPrecedentTrace precedentTrace,
        RecursalCompetenciaTrace competenciaTrace,
        RecursalPublicPaymentTrace publicPaymentTrace,
        Instant atualizadoEm) {

    public RecursalStateSnapshot(
            String recursoId,
            RecursalLifecycleState state,
            int revision,
            RecursalTribunal tribunalAtual,
            RecursalTribunalDetalhado tribunalDetalhadoAtual,
            InstanceLevel instanciaAtual,
            RecursalAuthority autoridadeAtual,
            boolean preparoSatisfeito,
            boolean admissibilidadePositiva,
            boolean remetido,
            boolean autuadoDestino,
            boolean distribuidoDestino,
            boolean preparoEmComplementacao,
            boolean diligenciaPendente,
            boolean multaEmbargosProtelatoriosAplicada,
            boolean sobrestadoPorPrecedente,
            boolean efeitoSuspensivoAtivo,
            boolean efeitoAtivoConcedido,
            boolean conhecimentoParcial,
            int iteracoesEmbargosDeclaracao,
            boolean remessaNecessaria,
            boolean requisicaoPublicaPagamento,
            Instant atualizadoEm) {
        this(
                recursoId,
                state,
                revision,
                tribunalAtual,
                tribunalDetalhadoAtual,
                instanciaAtual,
                autoridadeAtual,
                preparoSatisfeito,
                admissibilidadePositiva,
                remetido,
                autuadoDestino,
                distribuidoDestino,
                preparoEmComplementacao,
                diligenciaPendente,
                multaEmbargosProtelatoriosAplicada,
                sobrestadoPorPrecedente,
                efeitoSuspensivoAtivo,
                efeitoAtivoConcedido,
                conhecimentoParcial,
                iteracoesEmbargosDeclaracao,
                remessaNecessaria,
                requisicaoPublicaPagamento,
                RecursalRemessaTrace.empty(),
                RecursalSustentacaoOralTrace.empty(),
                RecursalPrecedentTrace.empty(),
                RecursalCompetenciaTrace.empty(),
                requisicaoPublicaPagamento ? RecursalPublicPaymentTrace.empty().ativar() : RecursalPublicPaymentTrace.empty(),
                atualizadoEm
        );
    }

    public RecursalStateSnapshot(
            String recursoId,
            RecursalLifecycleState state,
            int revision,
            RecursalTribunal tribunalAtual,
            RecursalTribunalDetalhado tribunalDetalhadoAtual,
            InstanceLevel instanciaAtual,
            RecursalAuthority autoridadeAtual,
            boolean preparoSatisfeito,
            boolean admissibilidadePositiva,
            boolean remetido,
            boolean autuadoDestino,
            boolean distribuidoDestino,
            boolean preparoEmComplementacao,
            boolean diligenciaPendente,
            boolean multaEmbargosProtelatoriosAplicada,
            boolean sobrestadoPorPrecedente,
            boolean efeitoSuspensivoAtivo,
            boolean efeitoAtivoConcedido,
            boolean conhecimentoParcial,
            int iteracoesEmbargosDeclaracao,
            Instant atualizadoEm) {
        this(
                recursoId,
                state,
                revision,
                tribunalAtual,
                tribunalDetalhadoAtual,
                instanciaAtual,
                autoridadeAtual,
                preparoSatisfeito,
                admissibilidadePositiva,
                remetido,
                autuadoDestino,
                distribuidoDestino,
                preparoEmComplementacao,
                diligenciaPendente,
                multaEmbargosProtelatoriosAplicada,
                sobrestadoPorPrecedente,
                efeitoSuspensivoAtivo,
                efeitoAtivoConcedido,
                conhecimentoParcial,
                iteracoesEmbargosDeclaracao,
                false,
                false,
                atualizadoEm
        );
    }

    public RecursalStateSnapshot(
            String recursoId,
            RecursalLifecycleState state,
            int revision,
            RecursalTribunal tribunalAtual,
            RecursalTribunalDetalhado tribunalDetalhadoAtual,
            InstanceLevel instanciaAtual,
            RecursalAuthority autoridadeAtual,
            boolean preparoSatisfeito,
            boolean admissibilidadePositiva,
            boolean remetido,
            boolean autuadoDestino,
            boolean distribuidoDestino,
            boolean preparoEmComplementacao,
            boolean diligenciaPendente,
            boolean multaEmbargosProtelatoriosAplicada,
            boolean sobrestadoPorPrecedente,
            boolean efeitoSuspensivoAtivo,
            boolean efeitoAtivoConcedido,
            boolean conhecimentoParcial,
            int iteracoesEmbargosDeclaracao,
            boolean remessaNecessaria,
            Instant atualizadoEm) {
        this(
                recursoId,
                state,
                revision,
                tribunalAtual,
                tribunalDetalhadoAtual,
                instanciaAtual,
                autoridadeAtual,
                preparoSatisfeito,
                admissibilidadePositiva,
                remetido,
                autuadoDestino,
                distribuidoDestino,
                preparoEmComplementacao,
                diligenciaPendente,
                multaEmbargosProtelatoriosAplicada,
                sobrestadoPorPrecedente,
                efeitoSuspensivoAtivo,
                efeitoAtivoConcedido,
                conhecimentoParcial,
                iteracoesEmbargosDeclaracao,
                remessaNecessaria,
                false,
                atualizadoEm
        );
    }

    public RecursalStateSnapshot {
        Objects.requireNonNull(recursoId, "recursoId");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(tribunalAtual, "tribunalAtual");
        Objects.requireNonNull(tribunalDetalhadoAtual, "tribunalDetalhadoAtual");
        Objects.requireNonNull(instanciaAtual, "instanciaAtual");
        Objects.requireNonNull(autoridadeAtual, "autoridadeAtual");
        remessaTrace = remessaTrace == null ? RecursalRemessaTrace.empty() : remessaTrace;
        sustentacaoOralTrace = sustentacaoOralTrace == null ? RecursalSustentacaoOralTrace.empty() : sustentacaoOralTrace;
        precedentTrace = precedentTrace == null ? RecursalPrecedentTrace.empty() : precedentTrace;
        competenciaTrace = competenciaTrace == null ? RecursalCompetenciaTrace.empty() : competenciaTrace;
        publicPaymentTrace = publicPaymentTrace == null
                ? requisicaoPublicaPagamento ? RecursalPublicPaymentTrace.empty().ativar() : RecursalPublicPaymentTrace.empty()
                : publicPaymentTrace;
        atualizadoEm = atualizadoEm == null ? Instant.now() : atualizadoEm;
    }

    public static RecursalStateSnapshot newDraft(String recursoId, RecursalCaseContext context) {
        return new RecursalStateSnapshot(
                recursoId,
                RecursalLifecycleState.RASCUNHO,
                0,
                context.tribunalOrigem(),
                context.tribunalDetalhadoOrigem(),
                context.instanciaAtual(),
                context.autoridadeAtual(),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                0,
                context.remessaNecessaria(),
                context.demandaRequisicaoPublicaPagamento(),
                RecursalRemessaTrace.empty(),
                RecursalSustentacaoOralTrace.empty(),
                RecursalPrecedentTrace.empty(),
                RecursalCompetenciaTrace.empty(),
                context.demandaRequisicaoPublicaPagamento() ? RecursalPublicPaymentTrace.empty().ativar() : RecursalPublicPaymentTrace.empty(),
                Instant.now()
        );
    }

    public RecursalStateSnapshot advance(
            RecursalLifecycleState nextState,
            RecursalTribunal tribunalAtual,
            RecursalTribunalDetalhado tribunalDetalhadoAtual,
            InstanceLevel instanciaAtual,
            RecursalAuthority autoridadeAtual,
            boolean preparoSatisfeito,
            boolean admissibilidadePositiva,
            boolean remetido,
            boolean autuadoDestino,
            boolean distribuidoDestino,
            boolean preparoEmComplementacao,
            boolean diligenciaPendente,
            boolean multaEmbargosProtelatoriosAplicada,
            boolean sobrestadoPorPrecedente,
            boolean efeitoSuspensivoAtivo,
            boolean efeitoAtivoConcedido,
            boolean conhecimentoParcial,
            int iteracoesEmbargosDeclaracao,
            boolean remessaNecessaria,
            boolean requisicaoPublicaPagamento,
            RecursalRemessaTrace remessaTrace,
            RecursalSustentacaoOralTrace sustentacaoOralTrace,
            RecursalPrecedentTrace precedentTrace,
            RecursalCompetenciaTrace competenciaTrace,
            RecursalPublicPaymentTrace publicPaymentTrace,
            Instant atualizadoEm) {
        return new RecursalStateSnapshot(
                recursoId,
                nextState,
                revision + 1,
                tribunalAtual,
                tribunalDetalhadoAtual,
                instanciaAtual,
                autoridadeAtual,
                preparoSatisfeito,
                admissibilidadePositiva,
                remetido,
                autuadoDestino,
                distribuidoDestino,
                preparoEmComplementacao,
                diligenciaPendente,
                multaEmbargosProtelatoriosAplicada,
                sobrestadoPorPrecedente,
                efeitoSuspensivoAtivo,
                efeitoAtivoConcedido,
                conhecimentoParcial,
                iteracoesEmbargosDeclaracao,
                remessaNecessaria,
                requisicaoPublicaPagamento,
                remessaTrace,
                sustentacaoOralTrace,
                precedentTrace,
                competenciaTrace,
                publicPaymentTrace,
                atualizadoEm
        );
    }
}
