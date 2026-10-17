package com.tcc.pjb.backend.core.security.access;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class ProcessoVinculoResolver {

    private static final Set<LaianeProcuracaoStatus> PROCURACAO_ENCERRADA = EnumSet.of(
            LaianeProcuracaoStatus.INDEFERIDA, LaianeProcuracaoStatus.REVOGADA,
            LaianeProcuracaoStatus.EXPIRADA, LaianeProcuracaoStatus.SUSPENSA);
    private static final Set<VinculoProcessualDoUsuario> INSTITUCIONAIS = EnumSet.of(
            VinculoProcessualDoUsuario.ORGAO_NO_POLO, VinculoProcessualDoUsuario.PROCESSO_CONCLUSO,
            VinculoProcessualDoUsuario.TAREFA_ATRIBUIDA, VinculoProcessualDoUsuario.OFICIAL_COM_MANDADO);

    private final PoloProcessualRepository poloProcessualRepository;
    private final LaianeProcuracaoRepository procuracaoRepository;
    private final PeritoNomeacaoRepository peritoNomeacaoRepository;
    private final ProcessoRepository processoRepository;
    private final VinculoInstitucionalResolver institucionalResolver;
    private final VinculoFuncionalResolver funcionalResolver;

    public ProcessoVinculoResolver(PoloProcessualRepository poloProcessualRepository,
                                   LaianeProcuracaoRepository procuracaoRepository,
                                   PeritoNomeacaoRepository peritoNomeacaoRepository,
                                   ProcessoRepository processoRepository,
                                   VinculoInstitucionalResolver institucionalResolver,
                                   VinculoFuncionalResolver funcionalResolver) {
        this.poloProcessualRepository = poloProcessualRepository;
        this.procuracaoRepository = procuracaoRepository;
        this.peritoNomeacaoRepository = peritoNomeacaoRepository;
        this.processoRepository = processoRepository;
        this.institucionalResolver = institucionalResolver;
        this.funcionalResolver = funcionalResolver;
    }

    public Optional<VinculoProcessualDoUsuario> vinculo(Usuario usuario, Processo processo, Set<VinculoProcessualDoUsuario> aceitos) {
        if (!identificado(usuario) || processo == null || processo.getId() == null) {
            return Optional.empty();
        }
        for (VinculoProcessualDoUsuario candidato : VinculoProcessualDoUsuario.values()) {
            if (aceitos.contains(candidato) && possui(candidato, usuario, processo)) {
                return Optional.of(candidato);
            }
        }
        return Optional.empty();
    }

    public Optional<VinculoProcessualDoUsuario> vinculoInstitucional(Usuario usuario, Long processoId) {
        if (!identificado(usuario) || processoId == null) {
            return Optional.empty();
        }
        for (VinculoProcessualDoUsuario candidato : INSTITUCIONAIS) {
            if (possuiInstitucional(candidato, usuario, processoId, () -> processoRepository.findUfById(processoId).orElse(null))) {
                return Optional.of(candidato);
            }
        }
        return Optional.empty();
    }

    private boolean possui(VinculoProcessualDoUsuario vinculo, Usuario usuario, Processo processo) {
        return switch (vinculo) {
            case PARTE -> ehParte(usuario, processo);
            case ADVOGADO_DO_AJUIZAMENTO -> ajuizouSemProcuracaoEncerrada(usuario, processo);
            case ADVOGADO_COM_PROCURACAO -> usuario.getTipoUsuario().isAdvocacia()
                    && procuracaoRepository.existsByAdvogadoIdAndProcessoIdAndStatus(usuario.getId(), processo.getId(), LaianeProcuracaoStatus.ATIVA);
            case FUNCAO_NA_UNIDADE -> funcionalResolver.funcaoNaUnidade(usuario, processo);
            case DESIGNACAO_FORMAL -> funcionalResolver.designacaoFormal(usuario, processo);
            case PERITO_NOMEADO -> usuario.getTipoUsuario().isPerito() && peritoNomeado(usuario.getId(), processo.getId());
            case ORGAO_NO_POLO, PROCESSO_CONCLUSO, TAREFA_ATRIBUIDA, OFICIAL_COM_MANDADO ->
                    possuiInstitucional(vinculo, usuario, processo.getId(), processo::getUf);
        };
    }

    private boolean possuiInstitucional(VinculoProcessualDoUsuario vinculo, Usuario usuario, Long processoId, Supplier<String> ufDoProcesso) {
        return switch (vinculo) {
            case ORGAO_NO_POLO -> institucionalResolver.orgaoNoPolo(usuario, processoId, ufDoProcesso);
            case PROCESSO_CONCLUSO -> institucionalResolver.processoConcluso(usuario, processoId);
            case TAREFA_ATRIBUIDA -> institucionalResolver.tarefaAtribuida(usuario, processoId);
            case OFICIAL_COM_MANDADO -> institucionalResolver.oficialComMandado(usuario, processoId);
            default -> false;
        };
    }

    private boolean ehParte(Usuario usuario, Processo processo) {
        if (usuario.getTipoUsuario() != TipoUsuario.CIDADAO) {
            return false;
        }
        String cpf = usuario.getCpf();
        boolean temCpf = cpf != null && !cpf.isBlank();
        if (temCpf && (cpf.equals(processo.getParteAutoraCpf()) || cpf.equals(processo.getParteReuCpf()))) {
            return true;
        }
        if (processo.getUsuario() != null && Objects.equals(processo.getUsuario().getId(), usuario.getId())) {
            return true;
        }
        return temCpf && poloProcessualRepository.findFirstByProcessoIdAndDocumentoAndAtivoTrueOrderByIdAsc(processo.getId(), cpf).isPresent();
    }

    private boolean ajuizouSemProcuracaoEncerrada(Usuario usuario, Processo processo) {
        return usuario.getTipoUsuario().isAdvocacia()
                && processo.getUsuario() != null && Objects.equals(processo.getUsuario().getId(), usuario.getId())
                && !procuracaoRepository.existsByAdvogado_IdAndProcessoIdAndStatusIn(usuario.getId(), processo.getId(), PROCURACAO_ENCERRADA);
    }

    private boolean peritoNomeado(Long peritoId, Long processoId) {
        return peritoNomeacaoRepository.existsByProcessoIdAndPeritoIdAndStatus(processoId, peritoId, PeritoNomeacaoStatus.NOMEADO)
                || peritoNomeacaoRepository.existsByProcessoIdAndPeritoIdAndStatus(processoId, peritoId, PeritoNomeacaoStatus.ACEITO);
    }

    private static boolean identificado(Usuario usuario) {
        return usuario != null && usuario.getId() != null && usuario.getTipoUsuario() != null;
    }
}
