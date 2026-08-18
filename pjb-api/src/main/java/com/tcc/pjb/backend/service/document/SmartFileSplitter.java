package com.tcc.pjb.backend.service.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.tcc.pjb.backend.model.dto.AnexoDeclarado;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SmartFileSplitter {

    
    private static final long LIMITE_BYTES = 5 * 1024 * 1024;

    public List<Attachment> processarArquivos(List<MultipartFile> arquivos, List<AnexoDeclarado> declaracoes) {
        List<AnexoDeclarado> decls = declaracoes == null ? List.of() : declaracoes;
        List<Attachment> processados = new ArrayList<>();

        if (arquivos == null || arquivos.isEmpty()) {
            if (!decls.isEmpty()) {
                throw new ErroDeValidacaoException(TipoErroValidacao.CORRELACAO_ARQUIVO_TIPO,
                        "Tipos declarados mas nenhum arquivo enviado.")
                        .addMetadado("declaracoes_sem_arquivo",
                                decls.stream().map(AnexoDeclarado::nomeArquivo).toList());
            }
            return processados;
        }

        for (MultipartFile arquivo : arquivos) {
            String nome = arquivo.getOriginalFilename();
            if (nome == null || nome.isBlank()) {
                throw new ErroDeValidacaoException(TipoErroValidacao.NOME_ARQUIVO_AUSENTE,
                        "Todo arquivo deve ter um nome. Renomeie antes de enviar.");
            }
        }

        Set<String> vistos = new LinkedHashSet<>();
        Set<String> duplicados = new LinkedHashSet<>();
        for (MultipartFile arquivo : arquivos) {
            String nome = arquivo.getOriginalFilename();
            if (!vistos.add(nome)) {
                duplicados.add(nome);
            }
        }
        if (!duplicados.isEmpty()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.NOME_ARQUIVO_DUPLICADO,
                    "Anexos com nome duplicado: " + String.join(", ", duplicados) + " — renomeie antes de enviar.")
                    .addMetadado("nomes_duplicados", duplicados);
        }

        Map<String, TipoDocumento> tipoPorNome = new LinkedHashMap<>();
        if (!decls.isEmpty()) {
            for (AnexoDeclarado d : decls) {
                tipoPorNome.put(d.nomeArquivo(), d.tipo());
            }
            for (String nome : vistos) {
                if (!tipoPorNome.containsKey(nome)) {
                    throw new ErroDeValidacaoException(TipoErroValidacao.CORRELACAO_ARQUIVO_TIPO,
                            "Arquivo '" + nome + "' enviado sem tipo declarado.")
                            .addMetadado("arquivo_sem_declaracao", nome);
                }
            }
            for (String nomeDeclarado : tipoPorNome.keySet()) {
                if (!vistos.contains(nomeDeclarado)) {
                    throw new ErroDeValidacaoException(TipoErroValidacao.CORRELACAO_ARQUIVO_TIPO,
                            "Tipo declarado para '" + nomeDeclarado + "' sem arquivo correspondente.")
                            .addMetadado("declaracao_sem_arquivo", nomeDeclarado);
                }
            }
        }

        for (MultipartFile arquivo : arquivos) {
            String nomeOriginal = arquivo.getOriginalFilename();
            try {
                if (arquivo.getSize() > LIMITE_BYTES) {
                    throw new ErroDeValidacaoException(TipoErroValidacao.TAMANHO_EXCEDIDO, nomeOriginal)
                            .addMetadado("arquivo", nomeOriginal)
                            .addMetadado("tamanho_atual", arquivo.getSize())
                            .addMetadado("tamanho_limite", LIMITE_BYTES);
                }
                if (!isPdf(arquivo)) {
                    throw new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, nomeOriginal)
                            .addMetadado("tipo_recebido", arquivo.getContentType())
                            .addMetadado("tipo_esperado", "application/pdf");
                }
                validarIntegridadePdf(arquivo);

                Attachment.AttachmentBuilder builder = Attachment.builder()
                        .name(nomeOriginal)
                        .contentType(arquivo.getContentType())
                        .size(arquivo.getSize())
                        .content(arquivo.getBytes());
                if (tipoPorNome.containsKey(nomeOriginal)) {
                    builder.tipoDocumento(tipoPorNome.get(nomeOriginal));
                }
                processados.add(builder.build());
            } catch (IOException e) {
                throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nomeOriginal, e)
                        .addMetadado("erro_tecnico", e.getClass().getSimpleName());
            }
        }
        return processados;
    }

    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        String nome = file.getOriginalFilename();
        return (contentType != null && contentType.equals("application/pdf")) ||
                (nome != null && nome.toLowerCase().endsWith(".pdf"));
    }

    private void validarIntegridadePdf(MultipartFile file) throws IOException {
        
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {

            
            if (doc.isEncrypted()) {
                throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_PROTEGIDO, file.getOriginalFilename())
                        .addMetadado("motivo", "Documento possui senha de abertura ou edição");
            }

            
            if (doc.getNumberOfPages() == 0) {
                throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, "PDF vazio (0 páginas)");
            }

        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_PROTEGIDO, "Senha requerida", e);
        }
    }
}