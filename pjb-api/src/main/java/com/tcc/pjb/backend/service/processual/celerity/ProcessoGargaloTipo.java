package com.tcc.pjb.backend.service.processual.celerity;

public enum ProcessoGargaloTipo {

    EXPEDIENTE_SEM_CIENCIA("Expediente expedido sem confirmação de ciência pela parte"),
    DOCUMENTO_SEM_ASSINATURA("Documento aguardando assinatura digital do responsável"),
    TAREFA_SEM_RESPONSAVEL("Tarefa ou workitem sem servidor designado"),
    PENDENCIA_VENCIDA("Pendência interna vencida sem resolução"),
    PERICIA_SEM_LAUDO("Perícia requerida sem laudo depositado"),
    CITACAO_FRUSTRADA("Tentativa de citação frustrada — meio de comunicação inválido"),
    RECURSO_PENDENTE_PROCESSAMENTO("Recurso interposto aguardando processamento cartorário"),
    OFICIO_SEM_RESPOSTA("Ofício expedido sem resposta dentro do prazo"),
    PENHORA_SEM_AVALIACAO("Penhora realizada sem avaliação do bem"),
    MANDADO_DEVOLVIDO_SEM_CUMPRIMENTO("Mandado devolvido sem cumprimento registrado"),
    AUDIENCIA_SEM_DESIGNACAO("Audiência determinada sem data designada"),
    PROCESSO_SEM_MOVIMENTACAO("Processo sem qualquer movimentação por período superior ao SLA"),
    CUSTAS_PENDENTES("Custas processuais pendentes de recolhimento"),
    PERICIA_SEM_QUESITOS("Perícia requerida sem quesitos formulados pelas partes"),
    ACORDO_NAO_HOMOLOGADO("Acordo firmado não homologado");

    private final String descricao;

    ProcessoGargaloTipo(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }

    public boolean eCartorario() {
        return switch (this) {
            case EXPEDIENTE_SEM_CIENCIA, DOCUMENTO_SEM_ASSINATURA, TAREFA_SEM_RESPONSAVEL,
                 MANDADO_DEVOLVIDO_SEM_CUMPRIMENTO, OFICIO_SEM_RESPOSTA,
                 CUSTAS_PENDENTES, RECURSO_PENDENTE_PROCESSAMENTO -> true;
            default -> false;
        };
    }
}
