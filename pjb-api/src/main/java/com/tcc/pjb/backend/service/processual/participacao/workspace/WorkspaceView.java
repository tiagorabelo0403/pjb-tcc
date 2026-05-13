package com.tcc.pjb.backend.service.processual.participacao.workspace;

import com.tcc.pjb.backend.service.processual.participacao.ActionProfile;
import com.tcc.pjb.backend.service.processual.participacao.SignaturePolicy;
import com.tcc.pjb.backend.service.processual.participacao.submission.SubmissionView;

import java.time.Instant;
import java.util.List;

public record WorkspaceView(ProcessIdentityView processo,
                            String persona,
                            String personaLabel,
                            String perfilUsuario,
                            List<String> capacities,
                            List<ActionProfile> actions,
                            SignaturePolicy signaturePolicy,
                            RepresentationGuardView representacao,
                            SecurityGuardView seguranca,
                            DeadlineGuardView prazo,
                            RoutingView routing,
                            List<PendingView> pendencias,
                            List<SubmissionView> minhasSubmissoes,
                            ExperienceDifferentialView diferencial,
                            Instant geradoEm) {
}
