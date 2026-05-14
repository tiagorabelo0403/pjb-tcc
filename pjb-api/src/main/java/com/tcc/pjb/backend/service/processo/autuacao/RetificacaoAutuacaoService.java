package com.tcc.pjb.backend.service.processo.autuacao;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RetificacaoAutuacaoService {

    public record RetificacaoRequest(
            UUID processoId,
            RitoProcessual ritoAtual,
            RitoProcessual ritoSolicitado,
            String classeAtual,
            String classeSolicitada,
            boolean processoJaCitado,
            boolean processoComSentenca,
            String motivoRetificacao,
            String operadorId
    ) {}

    public record RetificacaoResponse(
            UUID processoId,
            RetificacaoAutuacaoStatus status,
            boolean permitida,
            RetificacaoAutuacaoDiffService.DiffResultado diff,
            String mensagem
    ) {}

    private final RetificacaoAutuacaoPolicy policy;
    private final RetificacaoAutuacaoDiffService diffService;

    public RetificacaoAutuacaoService(
            RetificacaoAutuacaoPolicy policy,
            RetificacaoAutuacaoDiffService diffService) {
        this.policy = policy;
        this.diffService = diffService;
    }

    public RetificacaoResponse iniciar(RetificacaoRequest request) {
        RetificacaoAutuacaoPolicy.RetificacaoInput policyInput =
                new RetificacaoAutuacaoPolicy.RetificacaoInput(
                        request.ritoAtual(), request.ritoSolicitado(),
                        request.classeAtual(), request.classeSolicitada(),
                        request.processoJaCitado(), request.processoComSentenca(),
                        request.motivoRetificacao());

        RetificacaoAutuacaoPolicy.RetificacaoDecisao decisao = policy.avaliar(policyInput);

        RetificacaoAutuacaoDiffService.DiffInput diffInput =
                new RetificacaoAutuacaoDiffService.DiffInput(
                        request.processoId(), request.ritoAtual(), request.ritoSolicitado(),
                        request.classeAtual(), request.classeSolicitada(), null, null);

        RetificacaoAutuacaoDiffService.DiffResultado diff = diffService.calcular(diffInput);

        RetificacaoAutuacaoStatus status = decisao.permitida()
                ? RetificacaoAutuacaoStatus.AGUARDANDO_CONFERENCIA
                : RetificacaoAutuacaoStatus.RASCUNHO;

        return new RetificacaoResponse(request.processoId(), status, decisao.permitida(), diff, decisao.orientacao());
    }
}
