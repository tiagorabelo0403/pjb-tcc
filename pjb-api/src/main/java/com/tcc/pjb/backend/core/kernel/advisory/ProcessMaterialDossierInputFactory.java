package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class ProcessMaterialDossierInputFactory {

    private final ProcessMaterialDossierTextSupport textSupport;

    ProcessMaterialDossierInputFactory(ProcessMaterialDossierTextSupport textSupport) {
        this.textSupport = Objects.requireNonNull(textSupport);
    }

    ProcessMaterialDossierInput fromProcess(Processo processo, List<String> riskSignals) {
        Objects.requireNonNull(processo, "processo");
        return new ProcessMaterialDossierInput(
                "PROCESS",
                phaseLabel(processo.getFaseAtual()),
                ProcessMaterialDossierTextSupport.firstNonBlank(processo.getObjetoProcessual(), processo.getAssunto(), processo.getClasseProcessual(), processo.getResumoIA()),
                ProcessMaterialDossierTextSupport.firstNonBlank(processo.getPedidoPrincipal(), textSupport.firstBullet(processo.getPedidosConsolidados()), processo.getObjetoProcessual(), processo.getAssunto()),
                textSupport.joinText(processo.getAssunto(), processo.getObjetoProcessual(), processo.getPedidoPrincipal(), processo.getPedidosConsolidados(), processo.getResumoIA()),
                textSupport.joinText(processo.getMaterialProbatorioResumo(), processo.getPedidosConsolidados(), processo.getResumoIA()),
                textSupport.splitStructured(processo.getPedidosConsolidados()),
                textSupport.splitStructured(processo.getMaterialProbatorioResumo()),
                processo.getMaterialProbatorioScore(),
                processo.getPotencialAcordoScore(),
                processo.getValorCausa(),
                processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
                processo.getRito() == null ? null : processo.getRito().name(),
                !textSupport.blank(processo.getParteAutoraCpf()),
                !textSupport.blank(processo.getParteReuCpf()),
                riskSignals == null ? List.of() : List.copyOf(riskSignals)
        );
    }

    ProcessMaterialDossierInput fromRequest(LaianePeticaoAssistRequest request,
                                            CanonicalContext canonical,
                                            String ritoName) {
        Objects.requireNonNull(request, "request");
        List<String> pedidos = textSupport.splitStructured(ProcessMaterialDossierTextSupport.asString(request.getCtx() != null ? request.getCtx().get("pedidos") : null));
        if (pedidos.isEmpty() && request.getCtx() != null) {
            pedidos = textSupport.normalizeListObject(request.getCtx().get("pedidos"));
        }
        List<String> provas = ProcessMaterialDossierTextSupport.mergeOrderedLists(
                textSupport.normalizeList(request.getDocumentosAnexados()),
                textSupport.normalizeListObject(request.getCtx() != null ? request.getCtx().get("provas") : null),
                textSupport.splitStructured(ProcessMaterialDossierTextSupport.asString(request.getCtx() != null ? request.getCtx().get("material_probatorio_resumo") : null))
        );
        String texto = textSupport.joinText(
                request.getTextoFatosResumido(),
                ProcessMaterialDossierTextSupport.asString(request.getCtx() != null ? request.getCtx().get("fatos") : null),
                ProcessMaterialDossierTextSupport.asString(request.getCtx() != null ? request.getCtx().get("fundamentos") : null),
                request.getAssuntoTpu(),
                request.getMateriaPrincipal(),
                ProcessMaterialDossierTextSupport.asString(request.getCtx() != null ? request.getCtx().get("objeto_processual") : null),
                ProcessMaterialDossierTextSupport.asString(request.getCtx() != null ? request.getCtx().get("pedido_principal") : null)
        );
        return new ProcessMaterialDossierInput(
                "PETITION_ASSIST",
                textSupport.truthy(request.getAtoJurisdicionalAnterior()) ? "RECURSAL" : "AJUIZAMENTO",
                ProcessMaterialDossierTextSupport.firstNonBlank(
                        ProcessMaterialDossierTextSupport.asString(request.getCtx() != null ? request.getCtx().get("objeto_processual") : null),
                        request.getAssuntoTpu(),
                        request.getMateriaPrincipal(),
                        request.getClasseTpu(),
                        texto
                ),
                ProcessMaterialDossierTextSupport.firstNonBlank(
                        ProcessMaterialDossierTextSupport.asString(request.getCtx() != null ? request.getCtx().get("pedido_principal") : null),
                        textSupport.firstItem(pedidos),
                        request.getAssuntoTpu(),
                        request.getMateriaPrincipal()
                ),
                texto,
                textSupport.joinText(String.join(System.lineSeparator(), provas), ProcessMaterialDossierTextSupport.asString(request.getCtx() != null ? request.getCtx().get("material_probatorio_resumo") : null)),
                pedidos,
                provas,
                scoreRequestEvidence(texto, provas, request),
                scoreRequestNegotiation(texto, request, canonical),
                request.getValorCausa(),
                ProcessMaterialDossierTextSupport.firstNonBlank(canonical != null ? canonical.ramoDireito() : null, request.getRamoDireito()),
                ProcessMaterialDossierTextSupport.firstNonBlank(ritoName, request.getRitoSugerido()),
                !textSupport.blank(request.getCpfCnpjAutor()),
                !textSupport.blank(request.getCpfCnpjReu()),
                buildRequestRiskSignals(request, canonical)
        );
    }

    private Integer scoreRequestEvidence(String text,
                                         List<String> provas,
                                         LaianePeticaoAssistRequest request) {
        int score = Math.min(45, (provas == null ? 0 : provas.size()) * 8);
        if (textSupport.containsAny(textSupport.normalize(text), "LAUDO", "ATESTADO", "CONTRATO", "COMPROVANTE", "RECIBO", "EXTRATO")) {
            score += 25;
        }
        if (request.getDocumentosAnexados() != null && request.getDocumentosAnexados().size() >= 4) {
            score += 20;
        }
        if (textSupport.truthy(request.getRequerLiminar())) {
            score -= 5;
        }
        return textSupport.clamp(score, 0, 100);
    }

    private Integer scoreRequestNegotiation(String text,
                                            LaianePeticaoAssistRequest request,
                                            CanonicalContext canonical) {
        int score = 40;
        String normalized = textSupport.normalize(text);
        if (textSupport.containsAny(normalized, "ACORDO", "NEGOCI", "PARCEL", "CONCILI")) {
            score += 20;
        }
        if (request.getValorCausa() != null && request.getValorCausa().signum() > 0) {
            score += 10;
        }
        if (canonical != null && !textSupport.blank(canonical.ramoDireito()) && textSupport.containsAny(textSupport.normalize(canonical.ramoDireito()), "CIVIL", "CONSUMIDOR", "TRABALHISTA")) {
            score += 10;
        }
        if (textSupport.truthy(request.getAtoJurisdicionalAnterior())) {
            score -= 10;
        }
        return textSupport.clamp(score, 0, 100);
    }

    private List<String> buildRequestRiskSignals(LaianePeticaoAssistRequest request,
                                                 CanonicalContext canonical) {
        List<String> out = new ArrayList<>();
        if (textSupport.truthy(request.getRequerLiminar())) {
            out.add("Fluxo contém tutela de urgência e exige prova imediata da plausibilidade e do perigo de dano.");
        }
        if (textSupport.truthy(request.getRequerJuizadoEspecial())) {
            out.add("Checar teto econômico e compatibilidade do caso com o microssistema do juizado.");
        }
        if (canonical != null && !textSupport.blank(canonical.classeTpuCodigo())) {
            out.add("Classe TPU consolidada: " + canonical.classeTpuCodigo());
        }
        return List.copyOf(out);
    }

    private String phaseLabel(FaseProcessual fase) {
        return fase == null ? "PROCESSO" : fase.name();
    }
}
