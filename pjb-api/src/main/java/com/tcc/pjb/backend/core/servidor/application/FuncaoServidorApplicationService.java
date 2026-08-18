package com.tcc.pjb.backend.core.servidor.application;

import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncaoServidorApplicationService {

    private final FuncaoServidorJudiciarioRepository funcaoRepository;

    @Inject
    public FuncaoServidorApplicationService(FuncaoServidorJudiciarioRepository funcaoRepository) {
        this.funcaoRepository = funcaoRepository;
    }

    @Transactional
    public FuncaoServidorJudiciarioEntity designar(Long usuarioId, Long unidadeId,
                                                    FuncaoServidorJudiciario funcao,
                                                    LocalDate dataInicio, Long designadoPorId,
                                                    String portaria) {
        funcaoRepository.findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(usuarioId, unidadeId, funcao, true)
                .ifPresent(f -> {
                    throw new DataIntegrityViolationException(
                            "Já existe função ativa para o usuário na unidade: " + funcao);
                });
        FuncaoServidorJudiciarioEntity entity = new FuncaoServidorJudiciarioEntity(
                usuarioId, unidadeId, funcao, dataInicio, designadoPorId, portaria);
        return funcaoRepository.save(entity);
    }

    @Transactional
    public void encerrar(Long funcaoId, LocalDate dataFim, Long operadorId) {
        FuncaoServidorJudiciarioEntity entity = funcaoRepository.findById(funcaoId)
                .orElseThrow(() -> new EntityNotFoundException("Função não encontrada: " + funcaoId));
        entity.encerrar(dataFim);
        funcaoRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<FuncaoServidorJudiciarioEntity> listarPorUnidade(Long unidadeId) {
        return funcaoRepository.findByUnidadeIdAndAtivo(unidadeId, true);
    }

    @Transactional(readOnly = true)
    public Optional<FuncaoServidorJudiciarioEntity> funcaoAtiva(Long usuarioId, Long unidadeId,
                                                                  FuncaoServidorJudiciario funcao) {
        return funcaoRepository.findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(usuarioId, unidadeId, funcao, true);
    }

    public boolean podeExecutar(Long usuarioId, Long unidadeId,
                                 FuncaoServidorJudiciario funcao, String acao) {
        return funcaoAtiva(usuarioId, unidadeId, funcao)
                .map(f -> verificarPermissao(f.getFuncao(), acao))
                .orElse(false);
    }

    public boolean temPermissaoEmQualquerUnidade(Long usuarioId, String acao) {
        return funcaoRepository.findAll().stream()
                .filter(f -> f.getUsuarioId().equals(usuarioId) && f.isAtivo())
                .anyMatch(f -> verificarPermissao(f.getFuncao(), acao));
    }

    private boolean verificarPermissao(FuncaoServidorJudiciario funcao, String acao) {
        return switch (acao.toLowerCase()) {
            case "proferir" -> funcao.podeProferir();
            case "concluir" -> funcao.podeConcluir();
            case "intimar" -> funcao.podeIntimar();
            case "distribuir" -> funcao.podeDistribuir();
            case "arquivar" -> funcao.podeArquivar();
            default -> false;
        };
    }
}
