package com.tcc.pjb.backend.service.processual.protocolo;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProtocoloReciboService {

    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final DocumentoPaginaRepository documentoPaginaRepository;

    public ProtocoloReciboService(DocumentoProcessualRepository documentoProcessualRepository,
                                  DocumentoPaginaRepository documentoPaginaRepository) {
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.documentoPaginaRepository = Objects.requireNonNull(documentoPaginaRepository);
    }

    @Transactional
    public DocumentoProcessual emitirReciboPeticaoInicial(Processo processo, Usuario usuario, String hashConteudo) {
        Objects.requireNonNull(processo);
        Instant agora = Instant.now();
        String referencia = firstNonBlank(processo.getConnectorProtocolReference(), "PJB-PROTOCOLO:" + processo.getId());
        String conteudo = conteudoRecibo(processo, usuario, hashConteudo, referencia, agora);
        byte[] bytes = conteudo.getBytes(StandardCharsets.UTF_8);
        String sha256 = Hashes.sha256Hex(bytes);
        DocumentoProcessual documento = DocumentoProcessual.builder()
                .processo(processo)
                .titulo("Recibo de protocolo " + processo.getNumeroProcesso())
                .nomeOriginal("recibo-protocolo-" + safeFileToken(processo.getNumeroProcesso()) + ".txt")
                .sha256(sha256)
                .sha384(Hashes.sha384Hex(conteudo))
                .contentType("text/plain; charset=UTF-8")
                .tamanhoBytes((long) bytes.length)
                .pdf(bytes)
                .origemSistema("PJB_PROTOCOLO")
                .categoria(DocumentoCategoria.PESSOAL)
                .nivelSigilo(processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo())
                .visibilityScope("PARTES_E_PROCURADORES")
                .criadoPor(usuario == null ? null : usuario.getId())
                .criadoEm(LocalDateTime.ofInstant(agora, ZoneOffset.UTC))
                .build();
        documento.setEstadoOperacional("RECIBO_PROTOCOLO_EMITIDO");
        documento.setProtocoloExterno(referencia);
        documento.setQuantidadePaginas(1);
        DocumentoProcessual salvo = documentoProcessualRepository.save(documento);
        documentoPaginaRepository.save(DocumentoPagina.builder()
                .documento(salvo)
                .pageNumber(1)
                .pageId("recibo-" + salvo.getId())
                .fingerprint(sha256)
                .textoExtraido(conteudo)
                .criadoEm(LocalDateTime.ofInstant(agora, ZoneOffset.UTC))
                .build());
        return salvo;
    }

    private String conteudoRecibo(Processo processo, Usuario usuario, String hashConteudo, String referencia, Instant agora) {
        return String.join(System.lineSeparator(),
                "RECIBO DE PROTOCOLO",
                "Numero CNJ: " + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso()),
                "Data/hora oficial UTC: " + agora,
                "Usuario: " + usuarioLabel(usuario),
                "Tipo de peticao: PETICAO_INICIAL",
                "Parte autora: " + firstNonBlank(processo.getParteAutoraNome(), "NAO_INFORMADA"),
                "Parte re: " + firstNonBlank(processo.getParteReuNome(), "NAO_INFORMADA"),
                "Hash do conteudo: " + firstNonBlank(hashConteudo, "NAO_INFORMADO"),
                "Referencia do protocolo: " + referencia,
                "Origem: " + firstNonBlank(processo.getConnectorSystem(), "PJB"),
                "Status: " + firstNonBlank(processo.getConnectorSubmissionStatus(), "PROTOCOLO_REALIZADO")
        );
    }

    private String usuarioLabel(Usuario usuario) {
        if (usuario == null) {
            return "NAO_INFORMADO";
        }
        String id = usuario.getId() == null ? "SEM_ID" : String.valueOf(usuario.getId());
        return id + " - " + firstNonBlank(usuario.getNome(), usuario.getEmail(), "NAO_INFORMADO");
    }

    private String safeFileToken(String value) {
        String normalized = firstNonBlank(value, "sem-numero");
        return normalized.replaceAll("[^0-9A-Za-z._-]+", "_");
    }

    private String firstNonBlank(String first, String second) {
        String a = trimToNull(first);
        return a == null ? trimToNull(second) : a;
    }

    private String firstNonBlank(String first, String second, String third) {
        String a = firstNonBlank(first, second);
        return a == null ? trimToNull(third) : a;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
