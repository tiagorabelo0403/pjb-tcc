package com.tcc.pjb.backend.core.protocolo.completude;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.id.PjbUuidV7Generator;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ResultadoValidacao;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.OrigemValidacao;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloPendencia;
import com.tcc.pjb.backend.model.entity.protocolo.ProtocoloValidacaoHistorico;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloPendenciaRepository;
import com.tcc.pjb.backend.model.repository.protocolo.ProtocoloValidacaoHistoricoRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProtocoloPendenciaApplicationService {

    private static final int PRAZO_REGULARIZACAO_DIAS = 10;

    private final ProtocoloPendenciaRepository pendenciaRepository;
    private final ProtocoloValidacaoHistoricoRepository historicoRepository;
    private final ObjectMapper objectMapper;

    public ProtocoloPendenciaApplicationService(ProtocoloPendenciaRepository pendenciaRepository,
                                                ProtocoloValidacaoHistoricoRepository historicoRepository,
                                                ObjectMapper objectMapper) {
        this.pendenciaRepository = Objects.requireNonNull(pendenciaRepository);
        this.historicoRepository = Objects.requireNonNull(historicoRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public ProtocoloPendencia registrarPendencia(Long protocoloId,
                                                  ResultadoValidacao resultado,
                                                  List<String> documentosAnexados,
                                                  OrigemValidacao origem,
                                                  Long executadoPor) {
        String docHash = computeHash(documentosAnexados);
        String violacoesJson = serializarViolacoes(resultado.violacoes());

        Optional<ProtocoloPendencia> existente = pendenciaRepository.findAtivaByProtocoloId(protocoloId);

        ProtocoloPendencia pendencia;
        if (existente.isPresent()) {
            pendencia = existente.get();
            if (pendencia.getDocumentosHash().equals(docHash)) {
                return pendencia;
            }
            pendencia.setViolacoesJson(violacoesJson);
            pendencia.setDocumentosHash(docHash);
            pendencia.setStatus(resultado.statusResultante());
            pendencia.setRequisitosVersao(resultado.versaoRegraAplicada());
        } else {
            pendencia = ProtocoloPendencia.builder()
                    .uuid(PjbUuidV7Generator.generate())
                    .protocoloId(protocoloId)
                    .status(resultado.statusResultante())
                    .prazoRegularizacao(LocalDate.now().plusDays(PRAZO_REGULARIZACAO_DIAS))
                    .violacoesJson(violacoesJson)
                    .requisitosVersao(resultado.versaoRegraAplicada())
                    .documentosHash(docHash)
                    .version(0L)
                    .build();
        }

        ProtocoloPendencia salva;
        try {
            salva = pendenciaRepository.save(pendencia);
        } catch (DataIntegrityViolationException e) {
            salva = pendenciaRepository.findAtivaByProtocoloId(protocoloId).orElseThrow();
        }

        gravarHistorico(protocoloId, resultado, docHash, origem, executadoPor);
        return salva;
    }

    @Transactional
    public void registrarCompleto(Long protocoloId,
                                   ResultadoValidacao resultado,
                                   List<String> documentosAnexados,
                                   OrigemValidacao origem,
                                   Long executadoPor) {
        String docHash = computeHash(documentosAnexados);
        gravarHistorico(protocoloId, resultado, docHash, origem, executadoPor);
    }

    private void gravarHistorico(Long protocoloId, ResultadoValidacao resultado,
                                  String docHash, OrigemValidacao origem, Long executadoPor) {
        ProtocoloValidacaoHistorico historico = ProtocoloValidacaoHistorico.builder()
                .uuid(PjbUuidV7Generator.generate())
                .protocoloId(protocoloId)
                .statusResultante(resultado.statusResultante())
                .versaoRegraAplicada(resultado.versaoRegraAplicada())
                .violacoesJson(serializarViolacoes(resultado.violacoes()))
                .documentosHash(docHash)
                .origemValidacao(origem)
                .executadoPor(executadoPor)
                .build();
        historicoRepository.save(historico);
    }

    public static String computeHash(List<String> documentosAnexados) {
        if (documentosAnexados == null || documentosAnexados.isEmpty()) {
            return Hashes.sha256Hex("EMPTY");
        }
        String joined = documentosAnexados.stream().sorted().reduce("", (a, b) -> a + "|" + b);
        return Hashes.sha256Hex(joined);
    }

    private String serializarViolacoes(List<ViolacaoCompletude> violacoes) {
        List<Map<String, Object>> lista = violacoes.stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            boolean sensivel = isSensivel(v);
            m.put("codigo", v.codigo());
            m.put("severidade", v.severidade().name());
            m.put("campo", sensivel ? "DADO_SENSIVEL" : v.campo());
            m.put("acaoCorretiva", sensivel
                    ? "Contacte a secretaria para orientações sobre este documento."
                    : v.acaoCorretiva());
            if (v.fundamento() != null) {
                Map<String, Object> fund = new LinkedHashMap<>();
                fund.put("tipo", v.fundamento().tipo() != null ? v.fundamento().tipo().name() : null);
                fund.put("identificador", v.fundamento().identificador());
                fund.put("resumo", v.fundamento().resumo());
                fund.put("grau", v.fundamento().grau() != null ? v.fundamento().grau().name() : null);
                m.put("fundamento", fund);
            }
            return m;
        }).toList();

        try {
            return objectMapper.writeValueAsString(lista);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private boolean isSensivel(ViolacaoCompletude v) {
        return v.nivelSensibilidade() != null && v.nivelSensibilidade().exigeCredencial();
    }
}
