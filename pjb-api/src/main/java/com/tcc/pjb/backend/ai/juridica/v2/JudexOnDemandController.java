package com.tcc.pjb.backend.ai.juridica.v2;

import com.tcc.pjb.backend.ai.juridica.v2.dto.JudexGenerateMinutaRequest;
import jakarta.validation.Valid;
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
import com.tcc.pjb.backend.query.ProcessoQueryModel;
import com.tcc.pjb.backend.query.ProcessoQueryRepository;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/ia/judex")
@Validated
@PreAuthorize("isAuthenticated()")
@Slf4j
public class JudexOnDemandController {

    private final AiModelClient aiModelV2;
    private final ObjectProvider<ProcessoQueryRepository> queryRepository;

    public JudexOnDemandController(@Qualifier("aiModelV2") AiModelClient aiModelV2,
                                  ObjectProvider<ProcessoQueryRepository> queryRepository) {
        this.aiModelV2 = aiModelV2;
        this.queryRepository = queryRepository;
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

        ProcessoQueryRepository repo = queryRepository.getIfAvailable();
        if (repo != null && processoId != null) {
            ProcessoQueryModel processo = repo.findById(processoId)
                    .orElseThrow(() -> new RuntimeException("Processo não encontrado no índice de leitura"));
            analiseV1 = processo.getAnaliseTriagemV1();
            peticaoInicial = processo.getPeticaoInicialText();
        }

        if ((analiseV1 == null || analiseV1.isBlank()) && (peticaoInicial == null || peticaoInicial.isBlank())) {
            return ResponseEntity.badRequest().body(
                    "Dados insuficientes. Informe processoId (com pjb.search.enabled=true) ou envie analiseV1/peticaoInicialText no body."
            );
        }

        String prompt = String.format(
                "Você é um Juiz de Direito (IA V2 - Judex). " +
                        "Análise de Triagem V1: %s. " +
                        "Texto da Petição Inicial: %s. " +
                        "Instruções Adicionais do Magistrado: %s. " +
                        "Tarefa: Gere uma minuta de decisão completa e fundamentada.",
                analiseV1, peticaoInicial, promptAdicional
        );

        String minuta = aiModelV2.generate(prompt);
        return ResponseEntity.ok(minuta);
    }
}