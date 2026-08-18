package com.tcc.pjb.backend.core.protocolo.completude;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.id.PjbUuidV7Generator;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.GrauExigibilidade;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.OrigemValidacao;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloPendencia;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloValidacaoHistorico;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloPendenciaRepository;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloValidacaoHistoricoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProtocoloCompletudeOverrideService {

    private final ProtocoloPendenciaRepository pendenciaRepository;
    private final ProtocoloValidacaoHistoricoRepository historicoRepository;
    private final ProtocoloCompletudeStateMachine stateMachine;
    private final ObjectMapper objectMapper;

    public ProtocoloCompletudeOverrideService(ProtocoloPendenciaRepository pendenciaRepository,
                                               ProtocoloValidacaoHistoricoRepository historicoRepository,
                                               ObjectMapper objectMapper) {
        this.pendenciaRepository = Objects.requireNonNull(pendenciaRepository);
        this.historicoRepository = Objects.requireNonNull(historicoRepository);
        this.stateMachine = null;
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public ProtocoloPendencia dispensar(Long protocoloId, String justificativa, Long servidorId) {
        ProtocoloPendencia pendencia = pendenciaRepository.findAtivaByProtocoloId(protocoloId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ProtocoloPendencia ativa", protocoloId));

        if (pendencia.getStatus() != ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO) {
            throw new RegraNegocioException("Override só é aplicável a pendências no status PENDENTE_DOCUMENTACAO");
        }

        validarGrauExigibilidadePermiteOverride(pendencia);

        ProtocoloCompletudeStateMachine.validar(
                ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO,
                ProtocoloCompletudeStatus.DISPENSADO);

        pendencia.setStatus(ProtocoloCompletudeStatus.DISPENSADO);
        pendencia.setDispensadoPor(servidorId);
        pendencia.setDispensaJustificativa(justificativa);

        ProtocoloPendencia salva = pendenciaRepository.save(pendencia);

        ProtocoloValidacaoHistorico historico = ProtocoloValidacaoHistorico.builder()
                .uuid(PjbUuidV7Generator.generate())
                .protocoloId(protocoloId)
                .statusResultante(ProtocoloCompletudeStatus.DISPENSADO)
                .versaoRegraAplicada(pendencia.getRequisitosVersao())
                .violacoesJson(pendencia.getViolacoesJson())
                .documentosHash(pendencia.getDocumentosHash())
                .origemValidacao(OrigemValidacao.OVERRIDE)
                .executadoPor(servidorId)
                .build();
        historicoRepository.save(historico);

        return salva;
    }

    @SuppressWarnings("unchecked")
    private void validarGrauExigibilidadePermiteOverride(ProtocoloPendencia pendencia) {
        try {
            List<Map<String, Object>> violacoes = objectMapper.readValue(
                    pendencia.getViolacoesJson(), new TypeReference<>() {});
            for (Map<String, Object> v : violacoes) {
                Map<String, Object> fund = (Map<String, Object>) v.get("fundamento");
                if (fund == null) continue;
                String grau = (String) fund.get("grau");
                if (GrauExigibilidade.ABSOLUTO.name().equals(grau)) {
                    String campo = (String) v.getOrDefault("campo", "desconhecido");
                    throw new RegraNegocioException(
                            "Override não permitido: exigência ABSOLUTA em campo '" + campo
                                    + "'. Apenas requisitos DISPENSAVEL_COM_JUSTIFICATIVA ou RELATIVO podem ser dispensados.");
                }
            }
        } catch (RegraNegocioException ex) {
            throw ex;
        } catch (Exception ignored) {
        }
    }
}
