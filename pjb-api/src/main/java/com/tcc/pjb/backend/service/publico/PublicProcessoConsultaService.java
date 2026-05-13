package com.tcc.pjb.backend.service.publico;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.sigilo.SigiloUiMapper;
import com.tcc.pjb.backend.model.dto.publico.*;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicProcessoConsultaService {

    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final DocumentoPaginaRepository paginaRepository;

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "consulta-publica.processo-resumo.read", maxMillis = 1200, critical = false)
    public PublicProcessoConsultaResponse consultarPorNumero(String numero) {
        String key = numero != null ? numero.trim() : "";
        if (key.isBlank()) {
            throw new RecursoNaoEncontradoException("Processo", "numero_vazio");
        }

        Optional<Processo> opt = processoRepository.findByNumeroUnificado(key);
        if (opt.isEmpty()) {
            opt = processoRepository.findByNumeroProcesso(key);
        }
        Processo p = opt.orElseThrow(() -> new RecursoNaoEncontradoException("Processo", key));

        NivelSigilo sigilo = p.getNivelSigilo() != null ? p.getNivelSigilo() : NivelSigilo.PUBLICO;

        
        if (sigilo.getNivel() >= 4) {
            throw new RecursoNaoEncontradoException("Processo", key);
        }

        boolean restrito = sigilo.exigeCredencial();
        String aviso = restrito ? (sigilo.mensagemPublica() != null && !sigilo.mensagemPublica().isBlank()
                ? sigilo.mensagemPublica()
                : "Processo em segredo de justiça") : null;

        String orientacao = restrito
                ? "Acesso restrito: a íntegra fica disponível apenas para as partes e advogados habilitados (procuração/certificado digital), " +
                "bem como perfis institucionais (magistratura, MP, Defensoria, Procuradorias e servidores do fórum)."
                : null;

        
        if (restrito) {
            return new PublicProcessoConsultaResponse(
                    p.getId(),
                    safeNumero(p),
                    p.getTipoJustica() != null ? p.getTipoJustica().name() : null,
                    p.getRamoDireito() != null ? p.getRamoDireito().name() : null,
                    p.getClasseProcessual(),
                    p.getAssunto(),
                    p.getDataUltimaMovimentacao(),
                    SigiloUiMapper.toUi(sigilo),
                    true,
                    aviso,
                    orientacao,
                    null,
                    List.of(),
                    List.of()
            );
        }

        
        PublicPartesDTO partes = new PublicPartesDTO(p.getParteAutoraNome(), p.getParteReuNome());

        List<MovimentacaoProcessual> movs = movimentacaoRepository
                .findTop200ByProcesso_IdOrderByDataMovimentacaoDesc(p.getId());
        List<PublicMovimentacaoDTO> movDtos = movs.stream().map(m -> new PublicMovimentacaoDTO(
                m.getId(),
                m.getDataMovimentacao() != null ? java.time.LocalDateTime.ofInstant(m.getDataMovimentacao(), java.time.ZoneId.systemDefault()) : null,
                m.getFaseDe() != null ? m.getFaseDe().name() : null,
                m.getFasePara() != null ? m.getFasePara().name() : null,
                m.getDescricao()
        )).toList();

        List<PublicDocumentoDTO> docDtos = List.of();
        if (!movDtos.isEmpty() && movDtos.size() > 12) {
            movDtos = movDtos.subList(0, 12);
        }

        return new PublicProcessoConsultaResponse(
                p.getId(),
                safeNumero(p),
                p.getTipoJustica() != null ? p.getTipoJustica().name() : null,
                p.getRamoDireito() != null ? p.getRamoDireito().name() : null,
                p.getClasseProcessual(),
                p.getAssunto(),
                p.getDataUltimaMovimentacao(),
                SigiloUiMapper.toUi(sigilo),
                false,
                null,
                null,
                partes,
                movDtos,
                docDtos
        );
    }

    private static String safeNumero(Processo p) {
        if (p.getNumeroUnificado() != null && !p.getNumeroUnificado().isBlank()) return p.getNumeroUnificado();
        return p.getNumeroProcesso();
    }
}
