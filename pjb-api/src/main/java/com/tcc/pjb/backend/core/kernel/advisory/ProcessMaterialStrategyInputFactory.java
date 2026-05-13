package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.util.List;
import java.util.Objects;

final class ProcessMaterialStrategyInputFactory {

    private final ProcessMaterialStrategyTextSupport textSupport;
    private final ProcessMaterialStrategyScoringPolicy scoringPolicy;

    ProcessMaterialStrategyInputFactory(ProcessMaterialStrategyTextSupport textSupport,
                                        ProcessMaterialStrategyScoringPolicy scoringPolicy) {
        this.textSupport = Objects.requireNonNull(textSupport);
        this.scoringPolicy = Objects.requireNonNull(scoringPolicy);
    }

    ProcessMaterialStrategyInput fromProcess(Processo processo,
                                             ProcessMaterialDossierReport dossier,
                                             List<String> externalSignals) {
        Objects.requireNonNull(processo, "processo");
        return new ProcessMaterialStrategyInput(
                "PROCESS",
                processo.getObjetoProcessual(),
                processo.getPedidoPrincipal(),
                processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
                processo.getRito() == null ? null : processo.getRito().name(),
                processo.getValorCausa(),
                processo.getMaterialProbatorioScore(),
                processo.getPotencialAcordoScore(),
                processo.getParteAutoraCpf(),
                processo.getParteReuCpf(),
                false,
                false,
                dossier,
                externalSignals,
                null,
                processo.getMaterialProbatorioResumo(),
                processo.getResumoIA()
        );
    }

    ProcessMaterialStrategyInput fromRequest(LaianePeticaoAssistRequest request,
                                             CanonicalContext canonical,
                                             String ritoName,
                                             ProcessMaterialDossierReport dossier,
                                             double readinessScore,
                                             List<String> externalSignals) {
        LaianePeticaoAssistRequest safe = request == null ? new LaianePeticaoAssistRequest() : request;
        return new ProcessMaterialStrategyInput(
                "REQUEST",
                textSupport.firstNonBlank(
                        textSupport.stringFromCtx(safe, "objeto", "objeto_processual", "objetoProcessual"),
                        safe.getAssuntoTpu(),
                        safe.getMateriaPrincipal()
                ),
                textSupport.firstNonBlank(
                        textSupport.stringFromCtx(safe, "pedido", "pedido_principal", "pedidoPrincipal", "pedidos"),
                        safe.getProtocolTitle()
                ),
                canonical != null ? canonical.ramoDireito() : safe.getRamoDireito(),
                textSupport.firstNonBlank(ritoName, safe.getRitoSugerido()),
                safe.getValorCausa(),
                scoringPolicy.inferScore(dossier != null ? dossier.evidentiaryBracket() : null),
                scoringPolicy.inferScore(dossier != null ? dossier.negotiationBracket() : null),
                safe.getCpfCnpjAutor(),
                safe.getCpfCnpjReu(),
                Boolean.TRUE.equals(safe.getCasoUrgente()),
                Boolean.TRUE.equals(safe.getRequerJuizadoEspecial()),
                dossier,
                externalSignals,
                readinessScore,
                textSupport.joinStructured(safe.getDocumentosAnexados()),
                textSupport.firstNonBlank(safe.getDraftMarkdown(), safe.getTextoFatosResumido())
        );
    }
}
