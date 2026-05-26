package com.tcc.pjb.backend.integration.serpro.datavalid;

import jakarta.inject.Inject;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CpfValidacaoService {

    private static final Logger log = LoggerFactory.getLogger(CpfValidacaoService.class);

    private final CpfValidacaoPort port;

    @Inject
    public CpfValidacaoService(CpfValidacaoPort port) {
        this.port = Objects.requireNonNull(port, "cpfValidacaoPort");
    }

    public void validarParaPeticionamento(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return;
        }
        CpfValidacaoResult result;
        try {
            result = port.consultar(cpf);
        } catch (Exception ex) {
            log.warn("Falha na consulta de situacao cadastral — peticionamento bloqueado por segurança: {}", ex.getMessage());
            throw new CpfSituacaoBloqueadaException("PJB-CPF-002",
                    "Peticionamento não permitido: validação cadastral indisponível no momento.");
        }
        if (result == null || result.situacao() == null) {
            throw new CpfSituacaoBloqueadaException("PJB-CPF-002",
                    "Peticionamento não permitido: situação cadastral não pôde ser determinada.");
        }
        if (result.situacao() == CpfSituacao.CANCELADA_POR_OBITO) {
            throw new CpfSituacaoBloqueadaException("PJB-CPF-001",
                    "Peticionamento não permitido para CPF com situação impeditiva.");
        }
        if (result.situacao().bloqueante()) {
            throw new CpfSituacaoBloqueadaException("PJB-CPF-002",
                    "Peticionamento não permitido para CPF com situação cadastral irregular.");
        }
    }
}
