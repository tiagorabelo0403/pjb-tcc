package com.tcc.pjb.backend.service.processual.acceleration.juizado;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.springframework.stereotype.Service;

@Service
public class JuizadoSentencaModeloResolver {

    public record ModeloInput(
            RitoProcessual rito,
            boolean procedente,
            boolean homologacaoAcordo,
            boolean extincaoInaptidao
    ) {}

    public record ModeloSentenca(
            String tipoSentenca,
            String estrutura,
            boolean simplificada
    ) {}

    public ModeloSentenca resolver(ModeloInput input) {
        if (input.homologacaoAcordo()) {
            return new ModeloSentenca(
                    "HOMOLOGACAO_ACORDO",
                    "Relatório sumário → Homologação → Dispositivo — sem recurso ordinário",
                    true);
        }
        if (input.extincaoInaptidao()) {
            return new ModeloSentenca(
                    "EXTINCAO_SEM_MERITO",
                    "Relatório → Fundamento legal → Extinção — intimação simplificada",
                    true);
        }
        String tipo = input.procedente() ? "PROCEDENCIA" : "IMPROCEDENCIA";
        return new ModeloSentenca(
                "SENTENCA_" + tipo,
                "Relatório sumário → Fundamentos → Dispositivo — JEC art. 38 Lei 9.099/95",
                true);
    }
}
