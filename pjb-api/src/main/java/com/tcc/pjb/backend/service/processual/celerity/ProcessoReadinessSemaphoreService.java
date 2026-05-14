package com.tcc.pjb.backend.service.processual.celerity;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProcessoReadinessSemaphoreService {

    public enum Semaforo {
        PRONTO("Processo apto para a próxima movimentação"),
        COM_PENDENCIA_LEVE("Processo com pendência que não impede movimentação"),
        COM_RISCO_RETRABALHO("Processo com risco de retorno antes de concluído"),
        INCOMPLETO("Processo com pendência que deve ser resolvida antes de prosseguir");

        private final String descricao;
        Semaforo(String descricao) { this.descricao = descricao; }
        public String getDescricao() { return descricao; }
    }

    public record SemaforoResultado(
            Semaforo semaforo,
            List<String> pendencias,
            List<String> riscos,
            String mensagem
    ) {}

    public record SemaforoInput(
            RitoProcessual rito,
            boolean temDocumentosNaoLidos,
            boolean temPecaSemClassificacao,
            boolean temCustasDevidas,
            boolean temExpedientePendente,
            boolean temPendenciaVencida,
            boolean partesSemRepresentacao
    ) {}

    public SemaforoResultado avaliar(SemaforoInput input) {
        List<String> pendencias = new ArrayList<>();
        List<String> riscos = new ArrayList<>();

        if (input.temCustasDevidas()) {
            riscos.add("Custas processuais devidas — risco de extinção sem resolução de mérito");
        }
        if (input.partesSemRepresentacao()) {
            riscos.add("Parte sem representação válida — risco de nulidade de atos futuros");
        }
        if (input.temDocumentosNaoLidos()) {
            pendencias.add("Documentos juntados sem leitura registrada");
        }
        if (input.temPecaSemClassificacao()) {
            pendencias.add("Peça processual sem classificação de tipo");
        }
        if (input.temExpedientePendente()) {
            pendencias.add("Expediente pendente de ciência");
        }
        if (input.temPendenciaVencida()) {
            riscos.add("Pendência interna vencida sem resolução");
        }

        Semaforo semaforo = determinarSemaforo(pendencias, riscos);

        return new SemaforoResultado(semaforo, pendencias, riscos, buildMensagem(semaforo));
    }

    private Semaforo determinarSemaforo(List<String> pendencias, List<String> riscos) {
        if (!riscos.isEmpty() && riscos.size() >= 2) return Semaforo.INCOMPLETO;
        if (!riscos.isEmpty()) return Semaforo.COM_RISCO_RETRABALHO;
        if (!pendencias.isEmpty()) return Semaforo.COM_PENDENCIA_LEVE;
        return Semaforo.PRONTO;
    }

    private String buildMensagem(Semaforo semaforo) {
        return switch (semaforo) {
            case PRONTO -> "Processo aparentemente apto para a próxima movimentação.";
            case COM_PENDENCIA_LEVE -> "Processo pode avançar, mas há pendências leves para regularizar.";
            case COM_RISCO_RETRABALHO -> "Atenção: há risco de retorno antes de concluído. Verifique antes de assinar.";
            case INCOMPLETO -> "Processo com pendências que devem ser resolvidas antes de prosseguir.";
        };
    }
}
