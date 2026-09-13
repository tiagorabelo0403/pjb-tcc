package com.tcc.pjb.backend.service.document;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processo.ProcessoAccessApplicationService;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongConsumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entrega o PDF de um documento processual a quem tem direito de vê-lo.
 *
 * <p>A autorização é sempre contra o <b>sigilo efetivo</b> do processo, calculado pela malha recursal
 * e possivelmente mais restritivo que o gravado no processo, e acontece duas vezes: no processo e no
 * documento. O conteúdo só é resolvido depois de as duas passarem — resolver antes significaria ler o
 * documento do armazenamento para descartá-lo em seguida.
 */
@Service
public class DocumentoPdfDownloadService {

    private final DocumentoProcessualRepository documentoRepository;
    private final ProcessoAccessApplicationService processoAccessApplicationService;
    private final PjbAuthorizationService authorizationService;
    private final DocumentContentService contentService;
    private final RecursalEffectiveSecrecyService secrecyService;

    public DocumentoPdfDownloadService(DocumentoProcessualRepository documentoRepository,
                                       ProcessoAccessApplicationService processoAccessApplicationService,
                                       PjbAuthorizationService authorizationService,
                                       DocumentContentService contentService,
                                       RecursalEffectiveSecrecyService secrecyService) {
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.processoAccessApplicationService = Objects.requireNonNull(processoAccessApplicationService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.contentService = Objects.requireNonNull(contentService);
        this.secrecyService = Objects.requireNonNull(secrecyService);
    }

    /** Processo a que o documento pertence, para o chamador registrar o contexto da requisição. */
    public record PdfAutorizado(Long processoId, DocumentContentService.ResolvedDocumentContent conteudo) {
    }

    /**
     * @param registrarContexto chamado assim que o processo do documento e conhecido e <b>antes</b> da
     *     autorizacao. O orcamento de download (`DownloadBudgetContextResolver`) le o contexto da
     *     requisicao, inclusive quando o acesso e negado: registrar so depois de autorizar perderia o
     *     contexto exatamente na negativa, que e quando ele mais importa.
     */
    @Transactional(readOnly = true)
    public PdfAutorizado resolver(UUID documentoId, LongConsumer registrarContexto) {
        DocumentoProcessual documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento", documentoId));

        Long processoId = documento.getProcesso() != null ? documento.getProcesso().getId() : null;
        if (processoId == null) {
            throw new RecursoNaoEncontradoException("Processo", "(n/a)");
        }

        if (registrarContexto != null) {
            registrarContexto.accept(processoId);
        }

        Processo processo = processoAccessApplicationService.load(processoId);
        NivelSigilo efetivo = secrecyService.effectiveSecrecyForProcesso(processoId);
        authorizationService.requireReadProcessoAtSecrecy(processo, efetivo);
        authorizationService.requireReadDocumentoAtSecrecy(processo, documento, efetivo);

        return new PdfAutorizado(processoId, contentService.resolvePdf(documento));
    }
}
