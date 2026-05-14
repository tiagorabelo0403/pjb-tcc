package com.tcc.pjb.backend.service.processual.celerity;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RetrabalhoEvitavelDetectionService {

    public record PadraoRetrabalho(
            String descricao,
            String causaProvavel,
            String prevencaoSugerida,
            boolean critico
    ) {}

    public record RetrabalhoInput(
            RitoProcessual rito,
            boolean partesSemDocumentoValido,
            boolean valorCausaConflitanteComRito,
            boolean competenciaDuvidosa,
            boolean citacaoNulaProvavel,
            boolean classeIncompativelComPedido,
            boolean liminaresSemFundamento,
            boolean acordoNaoAssinadoPorTodas
    ) {}

    public List<PadraoRetrabalho> detectar(RetrabalhoInput input) {
        List<PadraoRetrabalho> padroes = new ArrayList<>();

        if (input.partesSemDocumentoValido()) {
            padroes.add(new PadraoRetrabalho(
                    "Parte sem documento válido nos autos",
                    "CPF/CNPJ ou RG ausente ou ilegível",
                    "Intimar para regularizar antes de citar — evita nulidade posterior",
                    true));
        }

        if (input.valorCausaConflitanteComRito()) {
            padroes.add(new PadraoRetrabalho(
                    "Valor da causa incompatível com o rito declarado",
                    "Juizado com valor acima do limite ou ordinário com valor de sumaríssimo",
                    "Corrigir classe/rito antes da autuação formal",
                    false));
        }

        if (input.competenciaDuvidosa()) {
            padroes.add(new PadraoRetrabalho(
                    "Competência territorial ou material duvidosa",
                    "Localização da parte ré ou matéria não confere com o juízo",
                    "Verificar competência antes de citar — evita remessa posterior",
                    true));
        }

        if (input.citacaoNulaProvavel()) {
            padroes.add(new PadraoRetrabalho(
                    "Citação com alto risco de nulidade",
                    "Endereço desatualizado ou meio de comunicação impróprio para o rito",
                    "Atualizar endereço antes de expedir mandado citatório",
                    true));
        }

        if (input.classeIncompativelComPedido()) {
            padroes.add(new PadraoRetrabalho(
                    "Classe processual incompatível com os pedidos formulados",
                    "Pedido principal não corresponde à classe CNJ selecionada",
                    "Retificar autuação antes de avançar — evita extinção sem mérito",
                    false));
        }

        if (input.acordoNaoAssinadoPorTodas()) {
            padroes.add(new PadraoRetrabalho(
                    "Acordo firmado sem assinatura de todas as partes",
                    "Advogado ou parte não assinou o termo de acordo",
                    "Não homologar até que todas as partes assinem digitalmente",
                    false));
        }

        return padroes;
    }
}
