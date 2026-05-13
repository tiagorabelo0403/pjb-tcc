package com.tcc.pjb.backend.core.comunicacao.institucional.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.ResolucaoDestinoInstitucionalResult;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.core.util.Hashes;

@Service
public class MotorRoteamentoComunicacaoInstitucional {

    private final UnitResolutionService unitResolutionService;
    private final PoliticaEntregaInstitucionalService politicaEntregaInstitucionalService;
    private final PrazoEntregaInstitucionalResolver prazoEntregaInstitucionalResolver;

    public MotorRoteamentoComunicacaoInstitucional(UnitResolutionService unitResolutionService,
                                                   PoliticaEntregaInstitucionalService politicaEntregaInstitucionalService,
                                                   PrazoEntregaInstitucionalResolver prazoEntregaInstitucionalResolver) {
        this.unitResolutionService = Objects.requireNonNull(unitResolutionService);
        this.politicaEntregaInstitucionalService = Objects.requireNonNull(politicaEntregaInstitucionalService);
        this.prazoEntregaInstitucionalResolver = Objects.requireNonNull(prazoEntregaInstitucionalResolver);
    }

    public ResolucaoRoteamentoInstitucionalResult resolver(ResolucaoRoteamentoInstitucionalRequest request) {
        Objects.requireNonNull(request, "request");
        ResolucaoDestinoInstitucionalResult destino = unitResolutionService.resolver(request);
        TipoComunicacaoJudicial tipoComunicacaoEfetiva = request.tipoComunicacaoSolicitada();
        PlanoEntregaInstitucional planoEntrega = politicaEntregaInstitucionalService.resolver(request, destino.alvo().unidade(), tipoComunicacaoEfetiva);
        PrazoEntregaInstitucionalResolver.PrazosResolverResultado prazos = prazoEntregaInstitucionalResolver.resolver(request, tipoComunicacaoEfetiva, planoEntrega.canalPrincipal());
        List<String> justificativas = new ArrayList<>();
        justificativas.addAll(destino.justificativas());
        justificativas.addAll(planoEntrega.justificativas());
        justificativas.add("slaCienciaHoras=" + prazos.slaCienciaHoras());
        justificativas.add("slaRespostaHoras=" + prazos.slaRespostaHoras());
        if (request.atoCanonico() != null) {
            justificativas.add("atoCanonico=" + request.atoCanonico().name());
            if ("ABRIR_VISTA_MP_INTERESSE_INCAPAZ".equals(request.atoCanonico().name())) {
                justificativas.add("Ministério Público deve intervir como fiscal da ordem jurídica em caso de incapaz ou criança/adolescente");
            }
        }
        boolean bloqueiaFluxo = request.bloqueioFluxoSensivel() || request.papelProcessual().bloqueiaMarcoProcessualSensivel();
        String gateCode = bloqueiaFluxo
                ? request.atoCanonico() != null && request.atoCanonico().gateCode() != null
                    ? request.atoCanonico().gateCode()
                    : "GATE-INSTITUCIONAL-" + request.destinatarioKind().name()
                : null;
        String hash = Hashes.sha256Hex(String.join("|", List.of(
                String.valueOf(request.processoId()),
                String.valueOf(request.processoNumero()),
                request.destinatarioKind().name(),
                request.papelProcessual().name(),
                tipoComunicacaoEfetiva.name(),
                destino.alvo().unidade().codigo(),
                destino.alvo().caixa().codigo(),
                planoEntrega.canalPrincipal().canal().name(),
                String.valueOf(prazos.slaCienciaHoras()),
                String.valueOf(prazos.slaRespostaHoras()),
                String.valueOf(gateCode)
        )));
        return new ResolucaoRoteamentoInstitucionalResult(
                destino.alvo(),
                tipoComunicacaoEfetiva,
                planoEntrega,
                prazos.slaCienciaHoras(),
                prazos.slaRespostaHoras(),
                gateCode,
                bloqueiaFluxo,
                List.copyOf(justificativas),
                hash,
                destino.catalogVersion()
        );
    }
}
