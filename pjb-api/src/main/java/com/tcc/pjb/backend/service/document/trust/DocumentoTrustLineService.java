package com.tcc.pjb.backend.service.document.trust;

import com.tcc.pjb.backend.model.entity.enums.document.DocumentoEstadoOperacional;
import com.tcc.pjb.backend.model.entity.enums.document.DocumentoOrigemSistema;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DocumentoTrustLineService {

    public record TrustLineInput(
            UUID documentoId,
            DocumentoOrigemSistema origem,
            String hashSha256,
            boolean validadoFormato,
            boolean classificadoSigilo,
            boolean conferidoConteudo,
            boolean assinado,
            boolean protocolado
    ) {}

    public record TrustLineEtapa(
            String etapa,
            boolean concluida,
            String descricao
    ) {}

    public record TrustLineResultado(
            UUID documentoId,
            DocumentoEstadoOperacional estadoAtual,
            List<TrustLineEtapa> etapas,
            boolean integridadeConfirmada
    ) {}

    public TrustLineResultado avaliar(TrustLineInput input) {
        List<TrustLineEtapa> etapas = new ArrayList<>();
        etapas.add(new TrustLineEtapa("UPLOAD", true, "Documento recebido de " + input.origem().name()));
        etapas.add(new TrustLineEtapa("HASH_SHA256", input.hashSha256() != null && !input.hashSha256().isBlank(),
                "Integridade verificada por SHA-256"));
        etapas.add(new TrustLineEtapa("VALIDACAO_FORMATO", input.validadoFormato(), "Formato validado"));
        etapas.add(new TrustLineEtapa("CLASSIFICACAO_SIGILO", input.classificadoSigilo(), "Nível de sigilo classificado"));
        etapas.add(new TrustLineEtapa("CONFERENCIA", input.conferidoConteudo(), "Conteúdo conferido pelo servidor"));
        etapas.add(new TrustLineEtapa("ASSINATURA", input.assinado(), "Assinado digitalmente"));
        etapas.add(new TrustLineEtapa("PROTOCOLO", input.protocolado(), "Protocolado nos autos digitais"));

        DocumentoEstadoOperacional estado = resolverEstado(input);
        boolean integro = input.hashSha256() != null && !input.hashSha256().isBlank();
        return new TrustLineResultado(input.documentoId(), estado, etapas, integro);
    }

    private DocumentoEstadoOperacional resolverEstado(TrustLineInput input) {
        if (input.protocolado()) return DocumentoEstadoOperacional.PROTOCOLADO;
        if (input.assinado()) return DocumentoEstadoOperacional.ASSINADO;
        if (input.conferidoConteudo()) return DocumentoEstadoOperacional.AGUARDANDO_ASSINATURA;
        if (input.classificadoSigilo()) return DocumentoEstadoOperacional.FINALIZADO;
        return DocumentoEstadoOperacional.SALVO;
    }
}
