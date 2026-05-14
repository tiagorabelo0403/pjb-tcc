package com.tcc.pjb.backend.service.processual.celerity;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CeleridadeBottleneckDiagnosisService {

    public record DiagnosticoGargalo(
            ProcessoGargaloTipo tipo,
            String descricaoHumana,
            String solucaoSugerida,
            boolean acaoImediataCartoraria,
            ProcessoAceleracaoAcaoTipo acaoRecomendada
    ) {}

    public record DiagnosticoInput(
            RitoProcessual rito,
            FaseProcessual fase,
            Duration tempoParado,
            boolean temExpedientePendente,
            boolean temDocumentoSemAssinatura,
            boolean temTarefaSemResponsavel,
            boolean temMandadoDevolvido,
            boolean reuPreso
    ) {}

    public List<DiagnosticoGargalo> diagnosticar(DiagnosticoInput input) {
        List<DiagnosticoGargalo> gargalos = new ArrayList<>();

        if (input.reuPreso() && input.tempoParado().toDays() > 5) {
            gargalos.add(new DiagnosticoGargalo(
                    ProcessoGargaloTipo.PROCESSO_SEM_MOVIMENTACAO,
                    "Réu preso — processo parado há " + input.tempoParado().toDays() + " dias.",
                    "Urgência constitucional. Designar audiência de instrução imediatamente.",
                    false, ProcessoAceleracaoAcaoTipo.DESIGNAR_AUDIENCIA));
        }

        if (input.temExpedientePendente()) {
            gargalos.add(new DiagnosticoGargalo(
                    ProcessoGargaloTipo.EXPEDIENTE_SEM_CIENCIA,
                    "Expediente pendente de ciência há " + input.tempoParado().toDays() + " dias.",
                    "Conferir meio de comunicação utilizado e verificar AR ou publicação no DJe.",
                    true, ProcessoAceleracaoAcaoTipo.VERIFICAR_AR));
        }

        if (input.temDocumentoSemAssinatura()) {
            gargalos.add(new DiagnosticoGargalo(
                    ProcessoGargaloTipo.DOCUMENTO_SEM_ASSINATURA,
                    "Documento aguardando assinatura digital.",
                    "Notificar responsável pela assinatura — verificar delegação ou ausência.",
                    true, ProcessoAceleracaoAcaoTipo.EXPEDIR_OFICIO));
        }

        if (input.temTarefaSemResponsavel()) {
            gargalos.add(new DiagnosticoGargalo(
                    ProcessoGargaloTipo.TAREFA_SEM_RESPONSAVEL,
                    "Tarefa sem servidor designado.",
                    "Designar responsável ou redistribuir workitem para a fila da unidade.",
                    true, ProcessoAceleracaoAcaoTipo.REDISTRIBUIR_PROCESSO));
        }

        if (input.temMandadoDevolvido()) {
            gargalos.add(new DiagnosticoGargalo(
                    ProcessoGargaloTipo.MANDADO_DEVOLVIDO_SEM_CUMPRIMENTO,
                    "Mandado devolvido sem cumprimento.",
                    "Verificar motivação da devolução e expedir novo mandado com endereço atualizado.",
                    true, ProcessoAceleracaoAcaoTipo.EXPEDIR_MANDADO));
        }

        if (gargalos.isEmpty() && input.tempoParado().toDays() > 30) {
            gargalos.add(new DiagnosticoGargalo(
                    ProcessoGargaloTipo.PROCESSO_SEM_MOVIMENTACAO,
                    "Processo parado há " + input.tempoParado().toDays() + " dias sem causa identificada.",
                    "Verificar fila da unidade e pendências internas.",
                    true, ProcessoAceleracaoAcaoTipo.CERTIFICAR_DECURSO_PRAZO));
        }

        return gargalos;
    }
}
