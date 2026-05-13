package com.tcc.pjb.backend.service.processual.participacao.submission;

import com.tcc.pjb.backend.service.processual.participacao.SignaturePolicy;
import com.tcc.pjb.backend.service.processual.participacao.workspace.DeadlineGuardView;
import com.tcc.pjb.backend.service.processual.participacao.workspace.RepresentationGuardView;
import com.tcc.pjb.backend.service.processual.participacao.workspace.SecurityGuardView;

import java.util.List;

public record SubmissionResponse(Long processoId,
                                 Long eventoSeq,
                                 String eventoId,
                                 Long workItemRecepcaoId,
                                 String inboxKey,
                                 String queueCode,
                                 String acaoCodigo,
                                 String acaoLabel,
                                 List<SubmissionDocumentView> documentos,
                                 SignaturePolicy signaturePolicy,
                                 RepresentationGuardView representacao,
                                 SecurityGuardView seguranca,
                                 DeadlineGuardView prazo,
                                 SubmissionAuditView auditoria,
                                 List<String> diferencias) {
}
