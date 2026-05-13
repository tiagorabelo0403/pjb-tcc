package com.tcc.pjb.backend.service.document;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.tcc.pjb.backend.core.engine.document.EvidenceScoringEngine;
import com.tcc.pjb.backend.core.engine.document.LgpdRedactionEngine;
import com.tcc.pjb.backend.model.dto.DocumentoEnriquecidoDTO;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartIngestionService {

    private static final Duration INGESTION_TIMEOUT = Duration.ofSeconds(10);

    private final LgpdRedactionEngine lgpdEngine;
    private final EvidenceScoringEngine evidenceEngine;
    private final TriagemNacionalIAEngine triagemNacionalIAEngine;
    private final PjbExecutionOrchestrator executionOrchestrator;

    public CompletableFuture<DocumentoEnriquecidoDTO> processarUploadInteligente(MultipartFile arquivo) {
        return executionOrchestrator.supply(PjbExecutionDescriptor.io("smart-ingestion.processar-upload-inteligente", INGESTION_TIMEOUT), () -> {
            try {
                String nome = arquivo == null ? null : arquivo.getOriginalFilename();
                String nameHash = nome == null ? null : Integer.toHexString(nome.hashCode());
                log.info("Ingestao inteligente iniciada: nameHash={}", nameHash);

                String textoExtraido = "";
                if (arquivo != null) {
                    
                    textoExtraido = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
                }

                var lgpd = lgpdEngine.analisarRisco(textoExtraido);
                var prova = evidenceEngine.classificarProva(nome, textoExtraido, arquivo != null ? arquivo.getContentType() : null);
                TriagemNacionalIAEngine.ResultadoTriagem triagem = triagemNacionalIAEngine.triarERegistrar(
                        new TriagemNacionalIAEngine.PedidoTriagem(
                                nome != null ? Integer.toHexString(nome.hashCode()) : "UPLOAD_SEM_NOME",
                                null,
                                null,
                                null,
                                BigDecimal.ZERO,
                                textoExtraido,
                                null,
                                null,
                                null,
                                null,
                                nome != null ? List.of(nome) : List.of(),
                                null,
                                false,
                                false,
                                null
                        )
                );

                DocumentoEnriquecidoDTO dto = DocumentoEnriquecidoDTO.builder()
                        .nomeOriginal(nome)
                        .conteudoPublico(lgpd.getConteudoSanitizado())
                        .tipoProvaDetectado(prova.getTipoProva())
                        .forcaProbatoria(prova.descricaoForca())
                        .contemDadosSensiveis(lgpd.getRiscoScore() > 0)
                        .sugestaoSistema(lgpd.isSugereSegredoJustica() ? "RECOMENDADO_SIGILO" : "PUBLICO")
                        .urgenciaCalculada(triagem != null && triagem.classificacao() != null ? (int) Math.round(triagem.confiancaGeral() * 100d) : null)
                        .classeInferida(triagem != null && triagem.classificacao() != null ? triagem.classificacao().classeTpu() : null)
                        .ramoInferido(triagem != null && triagem.competencia() != null ? triagem.competencia().tipoJusticaSugerido() : null)
                        .build();

                log.info("Ingestao concluida: tipo={} riscoLGPD={}", dto.getTipoProvaDetectado(), lgpd.getRiscoScore());
                return dto;

            } catch (IOException e) {
                throw new RuntimeException("Falha na leitura do arquivo", e);
            }
        });
    }
}
