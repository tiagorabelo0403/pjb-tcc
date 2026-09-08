package com.tcc.pjb.backend.ai.juridica.v2;

import com.tcc.pjb.backend.ai.juridica.v2.dto.JudexGenerateMinutaRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.ai.common.AiModelClient;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.query.ProcessoQueryModel;
import com.tcc.pjb.backend.query.ProcessoQueryRepository;
import com.tcc.pjb.backend.service.semantic.SemanticPrecedentSearchService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/ia/judex")
@Validated
@PreAuthorize("isAuthenticated()")
@Slf4j
public class JudexOnDemandController {

    private static final int PRECEDENTES_TOP_K = 3;

    private final AiModelClient aiModelV2;
    private final ObjectProvider<ProcessoQueryRepository> queryRepository;
    private final ObjectProvider<SemanticPrecedentSearchService> precedentSearch;

    public JudexOnDemandController(@Qualifier("aiModelV2") AiModelClient aiModelV2,
                                  ObjectProvider<ProcessoQueryRepository> queryRepository,
                                  ObjectProvider<SemanticPrecedentSearchService> precedentSearch) {
        this.aiModelV2 = aiModelV2;
        this.queryRepository = queryRepository;
        this.precedentSearch = precedentSearch;
    }

    @PostMapping("/analise-minuta")
    public ResponseEntity<String> gerarMinuta(@Valid @RequestBody(required = false) JudexGenerateMinutaRequest request) {
        JudexGenerateMinutaRequest body = Objects.requireNonNullElseGet(request, () -> new JudexGenerateMinutaRequest(null, null, null, null));
        Long processoId = body.processoId();
        String promptAdicional = body.promptAdicional();

        String analiseV1Req = body.analiseV1();
        String peticaoInicialReq = body.peticaoInicialText();

        log.info("Agente V2 (Judex) recebendo pedido de minuta para processo {}", processoId);

        String analiseV1 = analiseV1Req;
        String peticaoInicial = peticaoInicialReq;
        String ritoProcessual = null;

        ProcessoQueryRepository repo = queryRepository.getIfAvailable();
        if (repo != null && processoId != null) {
            ProcessoQueryModel processo = repo.findById(processoId)
                    .orElseThrow(() -> new RuntimeException("Processo não encontrado no índice de leitura"));
            analiseV1 = processo.getAnaliseTriagemV1();
            peticaoInicial = processo.getPeticaoInicialText();
            ritoProcessual = processo.getRitoProcessual();
        }

        if ((analiseV1 == null || analiseV1.isBlank()) && (peticaoInicial == null || peticaoInicial.isBlank())) {
            return ResponseEntity.badRequest().body(
                    "Dados insuficientes. Informe processoId (com pjb.search.enabled=true) ou envie analiseV1/peticaoInicialText no body."
            );
        }

        String contextoPrecedentes = buildContextoPrecedentes(ritoProcessual, peticaoInicial, analiseV1);

        String prompt = String.format(
                "Você é um Juiz de Direito (IA V2 - Judex). " +
                        "Análise de Triagem V1: %s. " +
                        "Texto da Petição Inicial: %s. " +
                        "Instruções Adicionais do Magistrado: %s. " +
                        "%s" +
                        "Tarefa: Gere uma minuta de decisão completa e fundamentada.",
                analiseV1, peticaoInicial, promptAdicional, contextoPrecedentes
        );

        String minuta = aiModelV2.generate(prompt);
        return ResponseEntity.ok(minuta);
    }

    private String buildContextoPrecedentes(String ritoProcessual, String peticaoInicial, String analiseV1) {
        SemanticPrecedentSearchService service = precedentSearch.getIfAvailable();
        if (service == null) {
            return "";
        }

        String query = (peticaoInicial != null && !peticaoInicial.isBlank()) ? peticaoInicial : analiseV1;
        if (query == null || query.isBlank()) {
            return "";
        }

        List<Precedente> precedentes;
        try {
            precedentes = service.semanticSearch(null, ritoProcessual, query, PRECEDENTES_TOP_K);
        } catch (RuntimeException e) {
            log.warn("Busca de precedentes indisponível, minuta seguirá sem contexto de jurisprudência: {}", e.getMessage());
            return "";
        }
        if (precedentes.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(
                "Precedentes relacionados recuperados da base (cite apenas os que forem pertinentes ao caso; "
                        + "nunca cite jurisprudência que não esteja nesta lista): ");
        for (Precedente p : precedentes) {
            String titulo = p.getTitulo() != null ? p.getTitulo() : p.getIdentificador();
            String tese = p.getTese() != null ? p.getTese() : p.getEmentaResumo();
            if (titulo == null && tese == null) {
                continue;
            }
            sb.append("[").append(titulo != null ? titulo : "sem título").append("] ")
                    .append(tese != null ? tese : "").append(" ");
        }
        sb.append(". ");
        return sb.toString();
    }
}