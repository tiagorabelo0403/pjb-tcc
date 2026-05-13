package com.tcc.pjb.backend.ai.core;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;
import com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB;

@Service
public class LegalAiService {

    private static final Logger log = LoggerFactory.getLogger(LegalAiService.class);

    public IAResponse analisarCliente(String nome, String cpfCnpj) {
        return minimalResponse(
                "Análise jurídica ainda não disponível (modo compat).",
                IAResponse.StatusIA.INDETERMINADO,
                0.0
        );
    }

    public IAResponse analisarHistoricoCliente(Long clienteId) {
        if (clienteId == null) {
            return minimalResponse("Cliente inválido.", IAResponse.StatusIA.ERRO, 0.0);
        }
        return minimalResponse(
                "Histórico do cliente ainda não disponível (modo compat).",
                IAResponse.StatusIA.INDETERMINADO,
                0.0
        );
    }

    public IAResponse analisarProcesso(String numeroProcesso) {
        if (numeroProcesso == null || numeroProcesso.isBlank()) {
            return minimalResponse("Número do processo inválido.", IAResponse.StatusIA.ERRO, 0.0);
        }
        return minimalResponse(
                "Análise do processo ainda não disponível (modo compat).",
                IAResponse.StatusIA.INDETERMINADO,
                0.0
        );
    }

    public IAResponse gerarResumo(String texto) {
        if (texto == null || texto.isBlank()) {
            return minimalResponse("Texto vazio.", IAResponse.StatusIA.ERRO, 0.0);
        }
        return minimalResponse(
                "Resumo ainda não disponível (modo compat).",
                IAResponse.StatusIA.INDETERMINADO,
                0.0
        );
    }

    protected IAResponse safeCall(String source, String text, Runnable call) {
        try {
            if (call != null) {
                call.run();
            }
            return minimalResponse(text, IAResponse.StatusIA.SUCESSO, 0.75).toBuilder().origem(source).build();
        } catch (Exception e) {
            log.warn("[{}] IA call failed: {}", source, e.toString());
            return minimalResponse("Análise não disponível (fallback).", IAResponse.StatusIA.INDETERMINADO, 0.0)
                    .toBuilder()
                    .origem(source)
                    .alertasCriticos(List.of("IA indisponível: fallback acionado"))
                    .build();
        }
    }

    private static IAResponse minimalResponse(String texto, IAResponse.StatusIA status, double confianca) {
        return IAResponse.builder()
                .origem("LEGAL_AI")
                .texto(texto)
                .status(status == null ? IAResponse.StatusIA.INDETERMINADO : status)
                .confianca(confianca)
                .dataGeracao(Instant.now())
                .alertasCriticos(Collections.emptyList())
                .metadados(Collections.emptyMap())
                .evidencias(Collections.<EvidenceItem>emptyList())
                .build();
    }

    public IAResponse analisarCadastroCliente(String nome, String cpfCnpj, Instant instante) {
        String n = (nome == null || nome.isBlank()) ? "(sem nome)" : nome.trim();
        String doc = maskDoc(cpfCnpj);
        if (log.isDebugEnabled()) {
            log.debug("[LEGAL_AI] analisarCadastroCliente nome={}, doc={}, ts={}", n, doc, instante);
        }
        return minimalResponse(
                "Triagem de cadastro registrada (modo compat).",
                IAResponse.StatusIA.INDETERMINADO,
                0.0
        );
    }

    public IAResponse auditarBuscaClientes(Object query, int page, int size) {
        if (log.isDebugEnabled()) {
            log.debug("[LEGAL_AI] auditarBuscaClientes page={}, size={}", page, size);
        }
        return minimalResponse("Auditoria de busca registrada (modo compat).", IAResponse.StatusIA.SUCESSO, 0.1);
    }

    public IAResponse verificarCoerenciaAtualizacao(Long clienteId, String nomeAtualizado) {
        if (log.isDebugEnabled()) {
            log.debug("[LEGAL_AI] verificarCoerenciaAtualizacao id={}", clienteId);
        }
        return minimalResponse("Verificação de coerência registrada (modo compat).", IAResponse.StatusIA.SUCESSO, 0.1);
    }

    public IAResponse auditarExclusaoCliente(Long clienteId) {
        if (log.isDebugEnabled()) {
            log.debug("[LEGAL_AI] auditarExclusaoCliente id={}", clienteId);
        }
        return minimalResponse("Auditoria de exclusão registrada (modo compat).", IAResponse.StatusIA.SUCESSO, 0.1);
    }

    private static String maskDoc(String doc) {
        if (doc == null || doc.isBlank()) return "(sem doc)";
        String digits = CriptografiaPJB.normalizarDocumentoNumerico(doc);
        if (digits == null) return "(sem doc)";
        if (digits.length() == 11) {
            return digits.replaceAll("(\\d{3})\\d{5}(\\d{3})", "$1*****$2");
        }
        if (digits.length() == 14) {
            return digits.replaceAll("(\\d{3})\\d{8}(\\d{3})", "$1********$2");
        }
        return "(doc invalido)";
    }
}
