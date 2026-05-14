package com.tcc.pjb.backend.service.processual.paralisacao;

public sealed interface ProcessoParalisacaoReason
        permits ProcessoParalisacaoReason.ExpedienteSemCiencia,
                ProcessoParalisacaoReason.DocumentoAguardandoAssinatura,
                ProcessoParalisacaoReason.TarefaSemResponsavel,
                ProcessoParalisacaoReason.PendenciaVencida,
                ProcessoParalisacaoReason.AguardandoResposta,
                ProcessoParalisacaoReason.SobrestamentoPorDependencia,
                ProcessoParalisacaoReason.CausaNaoIdentificada {

    String descricao();

    record ExpedienteSemCiencia(String expedienteId) implements ProcessoParalisacaoReason {
        public String descricao() { return "Expediente " + expedienteId + " sem ciência registrada."; }
    }

    record DocumentoAguardandoAssinatura(String documentoTitulo) implements ProcessoParalisacaoReason {
        public String descricao() { return "Documento '" + documentoTitulo + "' aguardando assinatura."; }
    }

    record TarefaSemResponsavel(String tarefaDescricao) implements ProcessoParalisacaoReason {
        public String descricao() { return "Tarefa '" + tarefaDescricao + "' sem responsável designado."; }
    }

    record PendenciaVencida(String pendencia) implements ProcessoParalisacaoReason {
        public String descricao() { return "Pendência vencida sem resolução: " + pendencia + "."; }
    }

    record AguardandoResposta(String entidade) implements ProcessoParalisacaoReason {
        public String descricao() { return "Aguardando resposta de: " + entidade + "."; }
    }

    record SobrestamentoPorDependencia(String processoReferencia) implements ProcessoParalisacaoReason {
        public String descricao() { return "Sobrestado aguardando processo " + processoReferencia + "."; }
    }

    record CausaNaoIdentificada() implements ProcessoParalisacaoReason {
        public String descricao() { return "Causa da paralisação não identificada automaticamente."; }
    }
}
