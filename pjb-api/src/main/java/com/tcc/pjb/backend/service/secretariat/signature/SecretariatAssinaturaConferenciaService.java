package com.tcc.pjb.backend.service.secretariat.signature;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SecretariatAssinaturaConferenciaService {

    public record DocumentoConferencia(
            UUID documentoId,
            String titulo,
            String hashSha256,
            boolean renderizado,
            boolean sigiloCompativel,
            boolean signatarioHabilitado,
            boolean papelValido
    ) {}

    public record ConferenciaResultado(
            UUID documentoId,
            String titulo,
            boolean aprovado,
            List<String> problemas
    ) {}

    private final SecretariatAssinaturaRiskPolicy riskPolicy;

    public SecretariatAssinaturaConferenciaService(SecretariatAssinaturaRiskPolicy riskPolicy) {
        this.riskPolicy = riskPolicy;
    }

    public ConferenciaResultado conferir(DocumentoConferencia doc) {
        SecretariatAssinaturaRiskPolicy.AssinaturaRiskInput input =
                new SecretariatAssinaturaRiskPolicy.AssinaturaRiskInput(
                        doc.hashSha256(),
                        doc.renderizado(),
                        doc.signatarioHabilitado(),
                        doc.papelValido(),
                        doc.sigiloCompativel(),
                        true);
        SecretariatAssinaturaRiskPolicy.AssinaturaRiskResultado resultado = riskPolicy.avaliar(input);
        return new ConferenciaResultado(
                doc.documentoId(), doc.titulo(), resultado.aprovado(), resultado.riscos());
    }

    public List<ConferenciaResultado> conferirLote(List<DocumentoConferencia> documentos) {
        List<ConferenciaResultado> resultados = new ArrayList<>();
        for (DocumentoConferencia doc : documentos) {
            resultados.add(conferir(doc));
        }
        return resultados;
    }

    public boolean loteApto(List<ConferenciaResultado> resultados) {
        return resultados.stream().allMatch(ConferenciaResultado::aprovado);
    }
}
