package com.tcc.pjb.backend.core.protocolo.completude;

import com.tcc.pjb.backend.core.protocolo.completude.domain.FundamentoNormativo;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ResultadoValidacao;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoDocumentoProcessual;
import com.tcc.pjb.backend.model.entity.protocolo.RequisitoDocumental;
import com.tcc.pjb.backend.model.entity.protocolo.RequisitoDocumentalEquivalencia;
import com.tcc.pjb.backend.model.repository.protocolo.RequisitoDocumentalEquivalenciaRepository;
import com.tcc.pjb.backend.model.repository.protocolo.RequisitoDocumentalRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProtocoloCompletudeValidator {

    private final RequisitoDocumentalRepository requisitoRepository;
    private final RequisitoDocumentalEquivalenciaRepository equivalenciaRepository;

    public ProtocoloCompletudeValidator(RequisitoDocumentalRepository requisitoRepository,
                                        RequisitoDocumentalEquivalenciaRepository equivalenciaRepository) {
        this.requisitoRepository = Objects.requireNonNull(requisitoRepository);
        this.equivalenciaRepository = Objects.requireNonNull(equivalenciaRepository);
    }

    @Transactional(readOnly = true)
    public ResultadoValidacao validar(ContextoValidacaoCompletude contexto) {
        List<RequisitoDocumental> requisitos = requisitoRepository
                .findVigentesNaData(contexto.rito(), contexto.dataProtocolo());

        List<ViolacaoCompletude> violacoes = new ArrayList<>();
        String versao = requisitos.isEmpty() ? "v1.0"
                : requisitos.stream().map(RequisitoDocumental::getVersao).findFirst().orElse("v1.0");

        for (RequisitoDocumental requisito : requisitos) {
            if (!aplicavel(requisito, contexto)) continue;

            if (contexto.possuiDocumento(requisito.getTipoDocumento())) continue;

            Optional<RequisitoDocumentalEquivalencia> equivalente = equivalenciaRepository
                    .findByRequisitoId(requisito.getId())
                    .stream()
                    .filter(eq -> contexto.possuiDocumento(eq.getTipoDocumentoAceito()))
                    .findFirst();

            if (equivalente.isPresent()) {
                RequisitoDocumentalEquivalencia eq = equivalente.get();
                if (eq.getSeveridadeSeSubstituto() != com.tcc.pjb.backend.model.entity.enums.processual.completude.SeveridadeCompletude.BLOQUEANTE) {
                    violacoes.add(new ViolacaoCompletude.DocumentoTipoIncorreto(
                            requisito.getTipoDocumento(),
                            eq.getTipoDocumentoAceito().name(),
                            eq.getSeveridadeSeSubstituto(),
                            fundamento(requisito)));
                }
                continue;
            }

            violacoes.add(new ViolacaoCompletude.DocumentoObrigatorioAusente(
                    requisito.getTipoDocumento(), fundamento(requisito), requisito.getNivelSensibilidade()));
        }

        boolean temBloqueante = violacoes.stream()
                .anyMatch(v -> v.severidade() == com.tcc.pjb.backend.model.entity.enums.processual.completude.SeveridadeCompletude.BLOQUEANTE);

        ProtocoloCompletudeStatus status = temBloqueante
                ? ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO
                : ProtocoloCompletudeStatus.COMPLETO;

        return new ResultadoValidacao(status, List.copyOf(violacoes), versao);
    }

    private boolean aplicavel(RequisitoDocumental req, ContextoValidacaoCompletude ctx) {
        if (req.getAplicaATipoParte() != null
                && ctx.tipoPartePrincipal() != null
                && req.getAplicaATipoParte() != ctx.tipoPartePrincipal()) {
            return false;
        }
        if (req.getAplicaARepresentante() != null
                && ctx.representante() != null
                && req.getAplicaARepresentante() != ctx.representante()) {
            return false;
        }
        return ctx.condicaoPresente(req.getTipoCondicao());
    }

    private FundamentoNormativo fundamento(RequisitoDocumental req) {
        return new FundamentoNormativo(
                req.getFonteNormativaTipo(),
                req.getFonteNormativaIdentificador(),
                req.getFundamentoResumido(),
                req.getGrauExigibilidade(),
                req.getAutoridadeOrigem());
    }
}
