package com.tcc.pjb.backend.service.magistratura.acts;

import com.tcc.pjb.backend.model.dto.juiz.CertidaoTJResponse;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCode;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest;
import com.tcc.pjb.backend.model.dto.pericia.PeritoNomeacaoRequest;
import com.tcc.pjb.backend.model.dto.pericia.PeritoNomeacaoResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizOrdemCumprimentoOficialRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.juiz.CertidaoTransitoJulgadoService;
import com.tcc.pjb.backend.service.juiz.decision.JuizGabineteDecisionalService;
import com.tcc.pjb.backend.service.juiz.decision.JuizOficialCumprimentoOrderService;
import com.tcc.pjb.backend.service.pericia.PeritoNomeacaoService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MagistraturaJudicialActExecutionSupport {

    private final JuizGabineteDecisionalService juizGabineteDecisionalService;
    private final JuizOficialCumprimentoOrderService juizOficialCumprimentoOrderService;
    private final CertidaoTransitoJulgadoService certidaoTransitoJulgadoService;
    private final PeritoNomeacaoService peritoNomeacaoService;
    private final MagistraturaJudicialActRelatoriaFormalizationSupport relatoriaFormalizationSupport;
    private final MagistraturaJudicialActPanelExecutionSupport panelExecutionSupport;
    private final MagistraturaJudicialActProjectionSupport projectionSupport;

    public MagistraturaJudicialActExecutionSupport(JuizGabineteDecisionalService juizGabineteDecisionalService,
                                                   JuizOficialCumprimentoOrderService juizOficialCumprimentoOrderService,
                                                   CertidaoTransitoJulgadoService certidaoTransitoJulgadoService,
                                                   PeritoNomeacaoService peritoNomeacaoService,
                                                   MagistraturaJudicialActRelatoriaFormalizationSupport relatoriaFormalizationSupport,
                                                   MagistraturaJudicialActPanelExecutionSupport panelExecutionSupport,
                                                   MagistraturaJudicialActProjectionSupport projectionSupport) {
        this.juizGabineteDecisionalService = Objects.requireNonNull(juizGabineteDecisionalService);
        this.juizOficialCumprimentoOrderService = Objects.requireNonNull(juizOficialCumprimentoOrderService);
        this.certidaoTransitoJulgadoService = Objects.requireNonNull(certidaoTransitoJulgadoService);
        this.peritoNomeacaoService = Objects.requireNonNull(peritoNomeacaoService);
        this.relatoriaFormalizationSupport = Objects.requireNonNull(relatoriaFormalizationSupport);
        this.panelExecutionSupport = Objects.requireNonNull(panelExecutionSupport);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public Map<String, Object> execute(Processo processo,
                                       Usuario usuario,
                                       Long processoId,
                                       MagistraturaJudicialActCode code,
                                       MagistraturaJudicialActCommandRequest request) {
        return switch (code) {
            case DESPACHO -> juizGabineteDecisionalService.assinarDespacho(processoId, required(request.conteudo(), "conteudo"), request.fundamentacao());
            case DECISAO_INTERLOCUTORIA -> juizGabineteDecisionalService.proferirDecisaoInterlocutoria(processoId, required(request.dispositivo(), "dispositivo"), request.fundamentacao(), request.tipo());
            case SENTENCA -> juizGabineteDecisionalService.proferirSentenca(processoId, required(request.dispositivo(), "dispositivo"), request.fundamentacao(), request.tipo());
            case DESIGNAR_AUDIENCIA -> juizGabineteDecisionalService.designarAudiencia(processoId, request.dataHora(), request.tipo(), request.local());
            case ORDEM_CUMPRIMENTO_OFICIAL -> juizOficialCumprimentoOrderService.ordenarCumprimento(processoId, new JuizOrdemCumprimentoOficialRequest(
                    request.oficialId(),
                    required(projectionSupport.firstNonBlank(request.fundamentacao(), request.observacao()), "fundamentacao"),
                    request.conteudo(),
                    request.tipo(),
                    request.prioridade(),
                    request.dataHora(),
                    null,
                    null,
                    null,
                    Boolean.TRUE,
                    Boolean.TRUE,
                    request.observacao()
            ));
            case CERTIDAO_TRANSITO_JULGADO -> toMap(certidaoTransitoJulgadoService.gerarAutomatica(processoId));
            case NOMEACAO_PERITO -> toMap(peritoNomeacaoService.nomear(processoId, PeritoNomeacaoRequest.builder()
                    .peritoId(required(request.peritoId(), "peritoId"))
                    .observacao(request.observacao())
                    .build()));
            case DESPACHO_RELATOR -> relatoriaFormalizationSupport.registrarDespachoRelatoria(processo, usuario, required(request.conteudo(), "conteudo"), request.fundamentacao());
            case DECISAO_MONOCRATICA -> panelExecutionSupport.executarDecisaoMonocratica(
                    processo,
                    usuario,
                    processoId,
                    request,
                    required(request.relatorio(), "relatorio"),
                    request.fundamentacao(),
                    required(request.dispositivo(), "dispositivo")
            );
            case VOTO_COLEGIADO -> panelExecutionSupport.proferirVoto(processoId, required(request.voto(), "voto"), request.fundamentacao(), required(request.decisao(), "decisao"));
            case ACORDAO -> panelExecutionSupport.lavrarAcordao(processoId, required(request.ementa(), "ementa"), required(request.dispositivo(), "dispositivo"), request.fundamentacao());
            case PEDIDO_VISTA -> panelExecutionSupport.pedirVista(processoId, request.diasVista());
            case DESTAQUE -> panelExecutionSupport.registrarDestaque(processoId, required(request.observacao(), "observacao"));
            case INCLUSAO_PAUTA -> panelExecutionSupport.incluirPauta(processoId, request.dataHora(), request.orgao());
            case DECISAO_PLENARIA -> panelExecutionSupport.registrarDecisaoPlenaria(processoId, required(request.votacao(), "votacao"), required(request.ementa(), "ementa"), required(request.dispositivo(), "dispositivo"));
        };
    }

    private Map<String, Object> toMap(CertidaoTJResponse response) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "CERTIDAO_TRANSITO_JULGADO_GERADA");
        out.put("processoId", response.processoId());
        out.put("processoNumero", response.processoNumero());
        out.put("documentoId", response.documentoId());
        out.put("hashDocumento", response.hashDocumento());
        out.put("generatedAt", response.generatedAt());
        out.put("assinaturaQualificada", response.assinaturaQualificada());
        out.put("validacaoSoberana", response.validacaoSoberana());
        return projectionSupport.safeMap(out);
    }

    private Map<String, Object> toMap(PeritoNomeacaoResponse response) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", response.getStatus() == null ? "NOMEACAO_REGISTRADA" : response.getStatus().name());
        out.put("nomeacaoId", response.getId());
        out.put("processoId", response.getProcessoId());
        out.put("peritoId", response.getPeritoId());
        out.put("peritoNome", response.getPeritoNome());
        out.put("nomeadoEm", response.getNomeadoEm());
        out.put("observacao", response.getObservacao());
        return projectionSupport.safeMap(out);
    }

    private <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        if (value instanceof String text && text.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value;
    }
}
