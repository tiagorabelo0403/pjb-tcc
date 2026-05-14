package com.tcc.pjb.backend.service.processual.celerity;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessoDestravavelRadarService {

    public record ProcessoDestravavel(
            UUID processoId,
            String numeroProcesso,
            RitoProcessual rito,
            Duration tempoParado,
            ProcessoAceleracaoAcaoTipo acaoDestravadora,
            String motivoParalisacao
    ) {}

    public record RadarInput(
            UUID processoId,
            String numeroProcesso,
            RitoProcessual rito,
            Duration tempoParado,
            boolean temExpedientePendente,
            boolean temDocumentoSemAssinatura,
            boolean temMandadoDevolvido,
            boolean temDecursoNaoCertificado
    ) {}

    public boolean eDestravavel(RadarInput input) {
        return input.temExpedientePendente()
                || input.temDocumentoSemAssinatura()
                || input.temMandadoDevolvido()
                || input.temDecursoNaoCertificado();
    }

    public List<ProcessoDestravavel> filtrarDestraveis(List<RadarInput> processos) {
        List<ProcessoDestravavel> resultado = new ArrayList<>();
        for (RadarInput input : processos) {
            if (!eDestravavel(input)) continue;
            ProcessoAceleracaoAcaoTipo acao = resolverAcao(input);
            String motivo = resolverMotivo(input);
            resultado.add(new ProcessoDestravavel(
                    input.processoId(), input.numeroProcesso(),
                    input.rito(), input.tempoParado(), acao, motivo));
        }
        return resultado;
    }

    private ProcessoAceleracaoAcaoTipo resolverAcao(RadarInput input) {
        if (input.temDecursoNaoCertificado()) return ProcessoAceleracaoAcaoTipo.CERTIFICAR_DECURSO_PRAZO;
        if (input.temExpedientePendente()) return ProcessoAceleracaoAcaoTipo.VERIFICAR_AR;
        if (input.temDocumentoSemAssinatura()) return ProcessoAceleracaoAcaoTipo.EXPEDIR_OFICIO;
        if (input.temMandadoDevolvido()) return ProcessoAceleracaoAcaoTipo.EXPEDIR_MANDADO;
        return ProcessoAceleracaoAcaoTipo.CERTIFICAR_DECURSO_PRAZO;
    }

    private String resolverMotivo(RadarInput input) {
        if (input.temDecursoNaoCertificado()) return "Decurso de prazo não certificado";
        if (input.temExpedientePendente()) return "Expediente sem ciência registrada";
        if (input.temDocumentoSemAssinatura()) return "Documento aguardando assinatura";
        if (input.temMandadoDevolvido()) return "Mandado devolvido sem cumprimento";
        return "Paralisação sem causa imediata identificada";
    }
}
