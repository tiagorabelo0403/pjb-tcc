package com.tcc.pjb.backend.service.secretariat.signature;

import com.tcc.pjb.backend.service.secretariat.batch.LoteInteligenteSafetyPolicy;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SecretariatAssinaturaLoteApplicator {

    public record LoteAssinaturaRequest(
            String operadorId,
            List<SecretariatAssinaturaConferenciaService.DocumentoConferencia> documentos
    ) {}

    public record LoteAssinaturaResultado(
            int aprovados,
            int rejeitados,
            List<SecretariatAssinaturaConferenciaService.ConferenciaResultado> detalhes,
            String mensagem
    ) {}

    private final SecretariatAssinaturaConferenciaService conferenciaService;
    private final LoteInteligenteSafetyPolicy safetyPolicy;

    public SecretariatAssinaturaLoteApplicator(
            SecretariatAssinaturaConferenciaService conferenciaService,
            LoteInteligenteSafetyPolicy safetyPolicy) {
        this.conferenciaService = conferenciaService;
        this.safetyPolicy = safetyPolicy;
    }

    public LoteAssinaturaResultado aplicar(LoteAssinaturaRequest request) {
        LoteInteligenteSafetyPolicy.LoteSafetyInput safetyInput =
                new LoteInteligenteSafetyPolicy.LoteSafetyInput(
                        "ASSINAR",
                        false,
                        true,
                        true,
                        request.documentos().size(),
                        false);

        LoteInteligenteSafetyPolicy.LoteSafetyResultado safety = safetyPolicy.avaliar(safetyInput);
        if (!safety.aprovado()) {
            return new LoteAssinaturaResultado(
                    0, request.documentos().size(), List.of(), safety.mensagem());
        }

        List<SecretariatAssinaturaConferenciaService.ConferenciaResultado> conferencias =
                conferenciaService.conferirLote(request.documentos());

        long aprovados = conferencias.stream()
                .filter(SecretariatAssinaturaConferenciaService.ConferenciaResultado::aprovado).count();
        long rejeitados = conferencias.size() - aprovados;

        String mensagem = rejeitados == 0
                ? aprovados + " documento(s) conferido(s) e prontos para assinatura."
                : aprovados + " aprovado(s), " + rejeitados + " rejeitado(s). Revisar antes de assinar.";

        return new LoteAssinaturaResultado((int) aprovados, (int) rejeitados, conferencias, mensagem);
    }
}
