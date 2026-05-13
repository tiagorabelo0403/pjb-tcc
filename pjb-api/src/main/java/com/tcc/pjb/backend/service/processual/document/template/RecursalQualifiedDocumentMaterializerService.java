package com.tcc.pjb.backend.service.processual.document.template;

import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderRequest;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RecursalQualifiedDocumentMaterializerService {

    private final OfficialDocumentTemplateService officialDocumentTemplateService;

    public RecursalQualifiedDocumentMaterializerService(OfficialDocumentTemplateService officialDocumentTemplateService) {
        this.officialDocumentTemplateService = Objects.requireNonNull(officialDocumentTemplateService);
    }

    public Map<String, Object> materializarVotoColegiado(Long processoId,
                                                         String titulo,
                                                         String voto,
                                                         String fundamentacao,
                                                         String decisao,
                                                         String orgaoJulgador,
                                                         String nivelInstancia) {
        return render(
                processoId,
                TemplateDocumentoOficial.DECISAO,
                titulo,
                Map.of(
                        "fundamentacao", safe(fundamentacao),
                        "dispositivo", safe(decisao),
                        "votoColegiado", safe(voto),
                        "orgaoJulgador", safe(orgaoJulgador),
                        "nivelInstancia", safe(nivelInstancia),
                        "especieRecursal", "VOTO_COLEGIADO"
                )
        );
    }

    public Map<String, Object> materializarAcordao(Long processoId,
                                                   String titulo,
                                                   String ementa,
                                                   String fundamentacao,
                                                   String dispositivo,
                                                   String orgaoJulgador,
                                                   String nivelInstancia,
                                                   String resultadoJulgamento) {
        return render(
                processoId,
                TemplateDocumentoOficial.ACORDAO,
                titulo,
                Map.of(
                        "ementa", safe(ementa),
                        "fundamentacao", safe(fundamentacao),
                        "dispositivo", safe(dispositivo),
                        "orgaoJulgador", safe(orgaoJulgador),
                        "nivelInstancia", safe(nivelInstancia),
                        "resultadoJulgamento", safe(resultadoJulgamento)
                )
        );
    }

    public Map<String, Object> materializarDecisaoMonocratica(Long processoId,
                                                              String titulo,
                                                              String relatorio,
                                                              String fundamentacao,
                                                              String dispositivo,
                                                              String orgaoJulgador,
                                                              String nivelInstancia) {
        return render(
                processoId,
                TemplateDocumentoOficial.DECISAO,
                titulo,
                Map.of(
                        "fundamentacao", composeMonocraticFoundation(relatorio, fundamentacao),
                        "dispositivo", safe(dispositivo),
                        "relatorio", safe(relatorio),
                        "orgaoJulgador", safe(orgaoJulgador),
                        "nivelInstancia", safe(nivelInstancia),
                        "especieRecursal", "DECISAO_MONOCRATICA_RECURSAL"
                )
        );
    }

    public Map<String, Object> materializarPronunciamentoRelatoria(Long processoId,
                                                                   String titulo,
                                                                   String fundamentacao,
                                                                   String dispositivo,
                                                                   String orgaoJulgador,
                                                                   String nivelInstancia,
                                                                   String especieAto,
                                                                   Map<String, String> extras) {
        LinkedHashMap<String, String> variaveis = new LinkedHashMap<>();
        variaveis.put("fundamentacao", safe(fundamentacao));
        variaveis.put("dispositivo", safe(dispositivo));
        variaveis.put("orgaoJulgador", safe(orgaoJulgador));
        variaveis.put("nivelInstancia", safe(nivelInstancia));
        variaveis.put("especieRecursal", safe(especieAto));
        if (extras != null && !extras.isEmpty()) {
            extras.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                    variaveis.put(key, value.trim());
                }
            });
        }
        return render(processoId, TemplateDocumentoOficial.DECISAO, titulo, variaveis);
    }

    public Map<String, Object> materializarPauta(Long processoId,
                                                 String titulo,
                                                 String destinatario,
                                                 String conteudoPublicacao,
                                                 String prazoPublicacao,
                                                 String orgaoJulgador,
                                                 String nivelInstancia) {
        return render(
                processoId,
                TemplateDocumentoOficial.EDITAL,
                titulo,
                Map.of(
                        "destinatario", safe(destinatario),
                        "conteudoPublicacao", safe(conteudoPublicacao),
                        "prazoPublicacao", safe(prazoPublicacao),
                        "orgaoJulgador", safe(orgaoJulgador),
                        "nivelInstancia", safe(nivelInstancia),
                        "dataGeracaoPauta", Instant.now().toString()
                )
        );
    }

    private Map<String, Object> render(Long processoId,
                                       TemplateDocumentoOficial template,
                                       String titulo,
                                       Map<String, String> variaveis) {
        OfficialDocumentTemplateRenderResponse response = officialDocumentTemplateService.renderizar(
                new OfficialDocumentTemplateRenderRequest(
                        processoId,
                        template,
                        titulo,
                        variaveis,
                        Boolean.TRUE,
                        Boolean.TRUE
                )
        );
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", response.processoId());
        out.put("numeroProcesso", response.numeroProcesso());
        out.put("templateDocumentoOficial", response.template().name());
        out.put("tituloDocumento", response.tituloDocumento());
        out.put("conteudoAssinado", response.conteudoRenderizado());
        out.put("hashSha256", response.hashSha256());
        out.put("documentoId", response.documentoId());
        out.put("categoria", response.categoria() == null ? null : response.categoria().name());
        out.put("nivelSigilo", response.nivelSigilo() == null ? null : response.nivelSigilo().name());
        out.put("persistido", response.persistido());
        out.put("selado", response.selado());
        out.put("variaveisAusentes", response.variaveisAusentes());
        out.put("alertas", response.alertas());
        out.put("assinaturaQualificada", response.assinaturaQualificada());
        out.put("validacaoSoberana", response.validacaoSoberana());
        return Collections.unmodifiableMap(out);
    }

    private String composeMonocraticFoundation(String relatorio, String fundamentacao) {
        String relatorioSafe = safe(relatorio);
        String fundamentacaoSafe = safe(fundamentacao);
        if (relatorioSafe.equals("NAO_INFORMADO")) {
            return fundamentacaoSafe;
        }
        return "RELATORIO: " + relatorioSafe + System.lineSeparator() + System.lineSeparator() + "FUNDAMENTACAO: " + fundamentacaoSafe;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "NAO_INFORMADO" : value.trim();
    }
}
