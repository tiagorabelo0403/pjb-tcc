package com.tcc.pjb.backend.service.publicacao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PublicacaoReadinessService {

    public record PublicacaoInput(
            UUID processoId,
            UUID documentoId,
            String tipoAto,
            MeioPublicacao meio,
            boolean documentoAssinado,
            boolean sigiloVerificado,
            boolean parteRepresentadaAdvogado,
            boolean enderecoEletronicoValidado,
            boolean minutaConferidaServidor
    ) {}

    public record PublicacaoReadiness(
            UUID processoId,
            boolean apta,
            List<String> bloqueios,
            List<String> alertas,
            MeioPublicacao meioSugerido
    ) {}

    public PublicacaoReadiness avaliar(PublicacaoInput input) {
        List<String> bloqueios = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        if (!input.documentoAssinado()) {
            bloqueios.add("Documento não assinado — assinar antes de publicar.");
        }
        if (!input.sigiloVerificado()) {
            bloqueios.add("Sigilo processual não verificado.");
        }
        if (!input.minutaConferidaServidor()) {
            bloqueios.add("Minuta não conferida por servidor antes da publicação.");
        }
        if (input.meio().eEletronico() && !input.enderecoEletronicoValidado()) {
            alertas.add("Endereço eletrônico não validado — verificar domicílio judicial.");
        }
        if (!input.parteRepresentadaAdvogado()) {
            alertas.add("Parte sem advogado constituído — verificar necessidade de intimação pessoal.");
        }
        MeioPublicacao sugerido = input.parteRepresentadaAdvogado()
                ? MeioPublicacao.DIARIO_ELETRONICO_JUSTICA
                : MeioPublicacao.DOMICILIO_ELETRONICO_JUDICIAL;
        return new PublicacaoReadiness(input.processoId(), bloqueios.isEmpty(),
                List.copyOf(bloqueios), List.copyOf(alertas), sugerido);
    }
}
